from datetime import date, datetime, time, timedelta
import json
import os
from pathlib import Path
import random
from urllib.parse import parse_qsl, quote_plus, urlencode, urlsplit, urlunsplit

import bcrypt
import click
from dotenv import load_dotenv
from flask import Flask, abort, flash, redirect, render_template, request, url_for
from flask_login import LoginManager, current_user, login_required, login_user, logout_user
from flask_wtf.csrf import CSRFProtect
from markupsafe import escape
from sqlalchemy import func, or_
from werkzeug.utils import secure_filename

from calculations import (
    analyze_complaint,
    analyze_legacy_complaint,
    evaluate_weather_disruption,
    legacy_predict_crowd,
    legacy_predict_flight,
    legacy_predict_medical,
    legacy_predict_taxi,
    legacy_predict_weather,
    predict_flight_delay,
    predict_passenger_flow,
    predict_taxi_demand,
)
from legacy_data import (
    LegacyDataManager,
    generate_legacy_alerts,
    generate_legacy_recommendations,
    legacy_analytics,
    legacy_dashboard_metrics,
)
from models import (
    Alert,
    Airport,
    Complaint,
    ComplaintAnalysis,
    Flight,
    FlightPredictionRecord,
    Notification,
    PassengerFlowRecord,
    Taxi,
    TaxiAvailability,
    TaxiBooking,
    TaxiDemandForecast,
    TaxiOperator,
    User,
    WeatherRecord,
    db,
)


ROOT = Path(__file__).resolve().parent

# On serverless environments (Vercel, AWS Lambda), the deployment filesystem is read-only.
# Fallback to /tmp/instance when running on Vercel or when the local directory is not writable.
if os.getenv("VERCEL") or os.getenv("AWS_LAMBDA_FUNCTION_NAME"):
    INSTANCE_DIR = Path("/tmp/instance")
else:
    INSTANCE_DIR = ROOT / "instance"

try:
    INSTANCE_DIR.mkdir(parents=True, exist_ok=True)
except Exception:
    INSTANCE_DIR = Path("/tmp/instance")
    try:
        INSTANCE_DIR.mkdir(parents=True, exist_ok=True)
    except Exception:
        INSTANCE_DIR = ROOT

load_dotenv(ROOT / ".env")


def _database_uri():
    configured = os.getenv("DATABASE_URL") or os.getenv("DB_URL")
    if not configured:
        db_path = (INSTANCE_DIR / "airwise.db").as_posix()
        return f"sqlite:///{db_path}"

    if configured.startswith("jdbc:"):
        configured = configured[len("jdbc:"):]
        parsed = urlsplit(configured)
        username = os.getenv("DB_USERNAME") or parsed.username
        password = os.getenv("DB_PASSWORD") or parsed.password
        host = parsed.hostname or "localhost"
        port = f":{parsed.port}" if parsed.port else ""
        database = parsed.path
        query = urlencode([
            (key, value) for key, value in parse_qsl(parsed.query)
            if key in {"charset", "connectTimeout", "readTimeout"}
        ])
        credentials = ""
        if username:
            credentials = quote_plus(username)
            if password:
                credentials += ":" + quote_plus(password)
            credentials += "@"
        configured = urlunsplit(("mysql+pymysql", f"{credentials}{host}{port}", database, query, ""))
    elif configured.startswith("mysql://"):
        configured = "mysql+pymysql://" + configured[len("mysql://"):]
    elif configured.startswith("postgres://"):
        configured = "postgresql://" + configured[len("postgres://"):]
    return configured


def create_app(test_config=None):
    app = Flask(
        __name__,
        instance_path=str(INSTANCE_DIR),
        template_folder=str(ROOT / "templates"),
        static_folder=str(ROOT / "static"),
    )
    app.config.update(
        SECRET_KEY=os.getenv("SECRET_KEY", "airwise-local-development-key"),
        SQLALCHEMY_DATABASE_URI=_database_uri(),
        SQLALCHEMY_TRACK_MODIFICATIONS=False,
        SQLALCHEMY_ENGINE_OPTIONS={"pool_pre_ping": True},
        AIRWISE_DEMO_MODE=os.getenv("AIRWISE_DEMO_MODE", "false").lower() in {"1", "true", "yes"},
        MAX_CONTENT_LENGTH=10 * 1024 * 1024,
        WTF_CSRF_TIME_LIMIT=None,
    )
    if test_config:
        app.config.update(test_config)

    db.init_app(app)
    csrf = CSRFProtect(app)
    login_manager = LoginManager(app)
    login_manager.login_view = "login"
    login_manager.login_message = "Please sign in to access this page."
    login_manager.login_message_category = "warning"

    @login_manager.user_loader
    def load_user(user_id):
        try:
            return db.session.get(User, int(user_id))
        except (TypeError, ValueError):
            return None

    @app.context_processor
    def shared_template_values():
        unread_count = 0
        if current_user.is_authenticated:
            unread_count = Notification.query.filter_by(user_id=current_user.id, is_read=False).count()
        active_alerts = Alert.query.filter_by(active=True).order_by(Alert.created_at.desc()).limit(5).all()
        return {
            "unread_notif_count": unread_count,
            "global_active_alerts": active_alerts,
            "demo_mode": app.config["AIRWISE_DEMO_MODE"],
            "now": datetime.now(),
        }

    @app.template_filter("tojson")
    def json_for_data_attributes(value):
        return escape(json.dumps(value, separators=(",", ":")))

    @app.after_request
    def add_security_headers(response):
        response.headers.setdefault("X-Content-Type-Options", "nosniff")
        response.headers.setdefault("X-Frame-Options", "SAMEORIGIN")
        response.headers.setdefault("Referrer-Policy", "strict-origin-when-cross-origin")
        return response

    def role_required(role):
        def decorator(view):
            from functools import wraps

            @wraps(view)
            @login_required
            def wrapped(*args, **kwargs):
                if current_user.role != role:
                    abort(403)
                return view(*args, **kwargs)
            return wrapped
        return decorator

    def get_latest_weather(airport):
        return WeatherRecord.query.filter_by(airport_id=airport.id).order_by(WeatherRecord.recorded_at.desc()).first()

    def get_latest_flow(airport):
        return PassengerFlowRecord.query.filter_by(airport_id=airport.id).order_by(PassengerFlowRecord.recorded_at.desc()).first()

    def predict_for_flight(flight):
        recent = FlightPredictionRecord.query.filter_by(flight_id=flight.id).order_by(
            FlightPredictionRecord.predicted_at.desc()
        ).first()
        if recent and recent.predicted_at > datetime.now() - timedelta(hours=2):
            return recent

        weather = get_latest_weather(flight.arrival_airport)
        if weather:
            weather_risk = 0.75 if weather.disruption_level.upper() in {"HIGH", "CRITICAL"} else 0.45 if weather.disruption_level.upper() == "MEDIUM" else 0.12
        else:
            weather_risk = 0.15
        flow = get_latest_flow(flight.departure_airport)
        if flow:
            congestion = {"CRITICAL": 0.85, "HIGH": 0.60, "MODERATE": 0.35}.get(flow.congestion_level.upper(), 0.10)
        else:
            congestion = 0.20
        result = predict_flight_delay(
            flight.previous_delay_minutes or 0,
            flight.turnaround_minutes or 45,
            flight.passenger_count or 180,
            weather_risk,
            flight.scheduled_departure,
            congestion,
        )
        record = FlightPredictionRecord(
            flight=flight,
            delay_probability=result.probability,
            expected_delay_minutes=result.expected_delay_minutes,
            risk_level=result.risk_level,
            contributing_factors="; ".join(result.contributing_factors),
            weather_risk_score=result.weather_risk_score,
            congestion_risk_score=result.congestion_risk_score,
            demo_data=True,
        )
        db.session.add(record)
        db.session.commit()
        return record

    def notify(user, title, message, kind, link_url):
        db.session.add(Notification(user=user, title=title, message=message, type=kind, link_url=link_url))

    def flights_query():
        return Flight.query.order_by(Flight.scheduled_departure.desc())

    def update_flight_status(flight, status, delay_minutes, reason):
        flight.status = status
        flight.delay_minutes = delay_minutes
        if reason and reason.strip():
            flight.confirmed_delay_reason = reason.strip()
        if status == "DELAYED" and delay_minutes > 0:
            flight.estimated_arrival = flight.scheduled_arrival + timedelta(minutes=delay_minutes)
            flight.estimated_departure = flight.scheduled_departure + timedelta(minutes=delay_minutes)
            active_bookings = TaxiBooking.query.filter(
                TaxiBooking.flight_id == flight.id,
                TaxiBooking.booking_status != "CANCELLED",
            ).all()
            for booking in active_bookings:
                booking.flight_delayed_flag = True
                booking.booking_status = "RESCHEDULE_REQUIRED"
                booking.flight_delay_notice = (
                    f"Flight {flight.flight_number} is delayed by {delay_minutes} min. "
                    f"New estimated landing: {flight.effective_arrival}"
                )
                notify(
                    booking.user,
                    "Flight Delayed — Taxi Reschedule Needed",
                    f"Flight {flight.flight_number} is delayed by {delay_minutes} min. Please review and confirm your new taxi pickup time.",
                    "FLIGHT",
                    url_for("booking_confirmation", reference=booking.booking_reference),
                )
        elif status == "ON_TIME":
            flight.estimated_arrival = flight.scheduled_arrival
            flight.estimated_departure = flight.scheduled_departure
            flight.delay_minutes = 0

    @app.get("/health")
    def health():
        return {"status": "ok", "application": "AIRWISE Flask"}

    @app.get("/")
    def index():
        recent_flights = flights_query().limit(6).all()
        weather_records = WeatherRecord.query.order_by(WeatherRecord.recorded_at.desc()).limit(4).all()
        return render_template(
            "index.html",
            recent_flights=recent_flights,
            weather_records=weather_records,
            total_flights=Flight.query.count(),
            delayed_flights=Flight.query.filter_by(status="DELAYED").count(),
        )

    @app.get("/access-denied")
    def access_denied():
        return render_template("access_denied.html"), 403

    @app.route("/login", methods=["GET", "POST"])
    def login():
        if request.method == "POST":
            username = request.form.get("username", "").strip()
            password = request.form.get("password", "")
            user = User.query.filter(func.lower(User.username) == username.lower()).first()
            valid_password = False
            if user:
                try:
                    valid_password = bcrypt.checkpw(password.encode("utf-8"), user.password.encode("utf-8"))
                except (ValueError, TypeError):
                    valid_password = False
            if not user or not user.enabled or not valid_password:
                flash("Invalid username or password. Please try again.", "error")
                return render_template("login.html", error=True, username=username)
            login_user(user, remember=request.form.get("rememberMe") == "on")
            destinations = {
                "ROLE_AIRPORT_ADMIN": "admin_dashboard",
                "ROLE_TAXI_OPERATOR": "operator_dashboard",
                "ROLE_PASSENGER": "passenger_dashboard",
            }
            return redirect(url_for(destinations.get(user.role, "index")))
        return render_template("login.html", demo=request.args.get("demo"), error=False)

    @app.post("/logout")
    @login_required
    def logout():
        logout_user()
        flash("You have been logged out successfully.", "success")
        return redirect(url_for("login", logout="true"))

    @app.route("/register", methods=["GET", "POST"])
    def register():
        if request.method == "POST":
            username = request.form.get("username", "").strip()
            password = request.form.get("password", "")
            confirm = request.form.get("confirmPassword", "")
            full_name = request.form.get("fullName", "").strip()
            phone = request.form.get("phone", "").strip() or None
            role = request.form.get("role", "ROLE_PASSENGER")
            error = None
            if not username or not password:
                error = "Username and password cannot be empty."
            elif len(username) > 100 or len(password) < 6 or len(password) > 72:
                error = "Username must be 100 characters or fewer; password must be 6 to 72 characters."
            elif password != confirm:
                error = "Passwords do not match."
            elif User.query.filter(func.lower(User.username) == username.lower()).first():
                error = "Username or email is already registered."
            if error:
                flash(error, "error")
                return render_template("register.html", form=request.form)
            role = role if role == "ROLE_TAXI_OPERATOR" else "ROLE_PASSENGER"
            user = User(
                username=username,
                password=bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8"),
                full_name=full_name or username,
                phone=phone,
                role=role,
            )
            db.session.add(user)
            db.session.commit()
            flash("Registration successful! You can now log in.", "success")
            return redirect(url_for("login", registered="true"))
        return render_template("register.html", form={})

    @app.get("/flights/search")
    def search_flights():
        query = flights_query()
        flight_number = request.args.get("flightNumber", "").strip()
        airline = request.args.get("airline", "").strip()
        dep_airport_id = request.args.get("depAirportId", type=int)
        arr_airport_id = request.args.get("arrAirportId", type=int)
        travel_date = request.args.get("travelDate", "").strip()
        if flight_number:
            query = query.filter(Flight.flight_number.ilike(f"%{flight_number}%"))
        if airline:
            query = query.filter(Flight.airline.ilike(f"%{airline}%"))
        if dep_airport_id:
            query = query.filter_by(departure_airport_id=dep_airport_id)
        if arr_airport_id:
            query = query.filter_by(arrival_airport_id=arr_airport_id)
        if travel_date:
            try:
                day = date.fromisoformat(travel_date)
                query = query.filter(Flight.scheduled_departure >= datetime.combine(day, time.min))
                query = query.filter(Flight.scheduled_departure <= datetime.combine(day, time.max))
            except ValueError:
                flash("Travel date must be a valid date.", "error")
        flights = query.order_by(Flight.scheduled_departure.asc()).all()
        airports = Airport.query.order_by(Airport.code.asc()).all()
        return render_template(
            "flight_search.html", flights=flights, airports=airports,
            flight_number=flight_number, airline=airline,
            dep_airport_id=dep_airport_id, arr_airport_id=arr_airport_id,
            travel_date=travel_date,
        )

    @app.get("/flights/<int:flight_id>")
    def flight_details(flight_id):
        flight = db.get_or_404(Flight, flight_id)
        prediction = predict_for_flight(flight)
        departure_weather = get_latest_weather(flight.departure_airport)
        arrival_weather = get_latest_weather(flight.arrival_airport)
        return render_template(
            "flight_details.html", flight=flight, prediction=prediction,
            dep_weather=departure_weather, arr_weather=arrival_weather,
        )

    @app.get("/weather")
    def weather_dashboard():
        records = WeatherRecord.query.order_by(WeatherRecord.recorded_at.desc()).all()
        return render_template("weather_dashboard.html", weather_records=records)

    @app.get("/taxis/demand")
    def taxi_demand_dashboard():
        predictions = TaxiDemandForecast.query.order_by(TaxiDemandForecast.time_slot_start.asc()).all()
        chart_series = [
            {"label": "Predicted demand", "data": [item.predicted_demand for item in predictions]},
            {"label": "Available taxi fleet", "data": [item.available_taxis for item in predictions]},
            {"label": "Reserved bookings", "data": [item.reserved_taxis for item in predictions]},
        ]
        chart_labels = [item.time_slot_start.strftime("%H:%M") for item in predictions]
        return render_template(
            "taxi_demand.html",
            predictions=predictions,
            chart_series=chart_series,
            chart_labels=chart_labels,
        )

    @app.get("/passenger/dashboard")
    @role_required("ROLE_PASSENGER")
    def passenger_dashboard():
        bookings = TaxiBooking.query.filter_by(user_id=current_user.id).order_by(TaxiBooking.pickup_time.desc()).all()
        complaints = Complaint.query.filter_by(user_id=current_user.id).order_by(Complaint.created_at.desc()).all()
        active_booking_statuses = {"CONFIRMED", "DRIVER_ASSIGNED", "RESCHEDULE_REQUIRED"}
        open_complaints = [item for item in complaints if item.status not in {"RESOLVED", "CLOSED"}]
        upcoming_flights = flights_query().limit(2).all()
        notifications = Notification.query.filter_by(user_id=current_user.id).order_by(Notification.created_at.desc()).limit(5).all()
        active_alerts = Alert.query.filter_by(active=True).order_by(Alert.created_at.desc()).limit(5).all()
        stats = {
            "bookings_count": len(bookings),
            "active_bookings_count": sum(item.booking_status in active_booking_statuses for item in bookings),
            "complaints_count": len(complaints),
            "open_complaints_count": len(open_complaints),
            "recent_bookings": bookings[:3],
            "recent_complaints": complaints[:3],
        }
        return render_template(
            "passenger_dashboard.html", stats=stats,
            upcoming_flights=upcoming_flights, notifications=notifications,
            active_alerts=active_alerts,
        )

    @app.route("/taxis/book", methods=["GET", "POST"])
    @login_required
    def book_taxi():
        flight = None
        flight_id = request.values.get("flightId", type=int)
        if flight_id:
            flight = db.session.get(Flight, flight_id)
        suggested_pickup = flight.effective_arrival + timedelta(minutes=30) if flight else datetime.now() + timedelta(hours=1)
        if request.method == "POST":
            try:
                taxi_type = request.form.get("taxiType", "SEDAN")
                terminal = request.form.get("terminal", "Terminal 2").strip()
                destination = request.form.get("destination", "").strip()
                passengers = int(request.form.get("passengers", "1"))
                luggage = int(request.form.get("luggage", "1"))
                pickup_time = datetime.fromisoformat(request.form.get("pickupTime", ""))
                notes = request.form.get("notes", "").strip() or None
                if taxi_type not in {"SEDAN", "SUV", "PREMIUM", "ELECTRIC", "VAN"}:
                    raise ValueError("Select a valid taxi category.")
                if not destination or len(destination) > 200:
                    raise ValueError("Enter a destination of 1 to 200 characters.")
                if passengers < 1 or luggage < 0 or passengers > 15 or luggage > 20:
                    raise ValueError("Passenger or luggage count is outside the supported range.")
                if pickup_time < datetime.now() - timedelta(minutes=10):
                    raise ValueError("Pickup time cannot be in the past.")

                window_start = pickup_time - timedelta(minutes=30)
                window_end = pickup_time + timedelta(minutes=30)
                active_bookings = TaxiBooking.query.filter(
                    TaxiBooking.pickup_time >= window_start,
                    TaxiBooking.pickup_time <= window_end,
                    TaxiBooking.booking_status != "CANCELLED",
                ).count()
                total_fleet = Taxi.query.filter_by(status="AVAILABLE").count()
                if active_bookings >= max(5, total_fleet):
                    raise ValueError(
                        f"Capacity limit reached for this pickup window ({pickup_time:%H:%M}). Please choose an alternative slot or taxi type."
                    )

                base_rates = {"SEDAN": 24.0, "SUV": 34.0, "PREMIUM": 52.0, "ELECTRIC": 28.0, "VAN": 45.0}
                distance = 22.0 + random.randrange(15)
                fare = round((distance * base_rates[taxi_type] + 150.0) * 100) / 100
                reference = None
                while not reference or TaxiBooking.query.filter_by(booking_reference=reference).first():
                    reference = f"AW-TX-{10000 + random.randrange(90000)}"
                booking = TaxiBooking(
                    booking_reference=reference,
                    user=current_user,
                    flight=flight,
                    requested_taxi_type=taxi_type,
                    pickup_terminal=terminal or "Terminal 2",
                    destination=destination,
                    passenger_count=passengers,
                    luggage_count=luggage,
                    pickup_time=pickup_time,
                    estimated_fare=fare,
                    booking_status="CONFIRMED",
                    notes=notes,
                )
                db.session.add(booking)
                available_taxi = Taxi.query.filter_by(taxi_type=taxi_type, status="AVAILABLE").first()
                if available_taxi:
                    booking.taxi = available_taxi
                    booking.booking_status = "DRIVER_ASSIGNED"
                db.session.flush()
                notify(
                    current_user,
                    f"Taxi Booking Confirmed: {reference}",
                    f"Your ride to {destination} is booked for {pickup_time}. Reference: {reference}",
                    "TAXI",
                    url_for("booking_confirmation", reference=reference),
                )
                db.session.commit()
                flash(f"Taxi advance reservation confirmed! Reference: {reference}", "success")
                return redirect(url_for("booking_confirmation", reference=reference))
            except (ValueError, TypeError) as error:
                db.session.rollback()
                flash(str(error) or "Enter a valid pickup date and time.", "error")
                flight = db.session.get(Flight, flight_id) if flight_id else None
        return render_template("taxi_book.html", flight=flight, suggested_pickup=suggested_pickup)

    @app.get("/taxis/bookings")
    @login_required
    def taxi_history():
        bookings = TaxiBooking.query.filter_by(user_id=current_user.id).order_by(TaxiBooking.pickup_time.desc()).all()
        return render_template("taxi_history.html", bookings=bookings)

    @app.get("/taxis/bookings/<reference>")
    @login_required
    def booking_confirmation(reference):
        booking = TaxiBooking.query.filter_by(booking_reference=reference).first_or_404()
        if current_user.role == "ROLE_PASSENGER" and booking.user_id != current_user.id:
            abort(403)
        if current_user.role == "ROLE_TAXI_OPERATOR":
            operator = TaxiOperator.query.filter_by(user_id=current_user.id).first()
            if not operator or not booking.taxi or booking.taxi.operator_id != operator.id:
                abort(403)
        return render_template(
            "taxi_confirmation.html",
            booking=booking,
            reschedule_time=booking.pickup_time + timedelta(minutes=40),
        )

    @app.post("/taxis/bookings/<int:booking_id>/reschedule")
    @login_required
    def reschedule_booking(booking_id):
        booking = db.get_or_404(TaxiBooking, booking_id)
        _require_booking_access(booking)
        try:
            pickup_time = datetime.fromisoformat(request.form.get("newPickupTime", ""))
            if pickup_time < datetime.now() - timedelta(minutes=10):
                raise ValueError("Pickup time cannot be in the past.")
            booking.pickup_time = pickup_time
            booking.booking_status = "CONFIRMED"
            booking.flight_delayed_flag = False
            booking.flight_delay_notice = f"Rescheduled successfully to {pickup_time}"
            notify(booking.user, f"Taxi Pickup Rescheduled: {booking.booking_reference}", f"Your updated pickup time is confirmed for {pickup_time}", "TAXI", url_for("booking_confirmation", reference=booking.booking_reference))
            db.session.commit()
            flash(f"Pickup slot rescheduled successfully to {pickup_time}!", "success")
            return redirect(url_for("booking_confirmation", reference=booking.booking_reference))
        except ValueError as error:
            db.session.rollback()
            flash(str(error) or "Enter a valid pickup date and time.", "error")
            return redirect(url_for("taxi_history"))

    @app.post("/taxis/bookings/<int:booking_id>/cancel")
    @login_required
    def cancel_booking(booking_id):
        booking = db.get_or_404(TaxiBooking, booking_id)
        _require_booking_access(booking)
        booking.booking_status = "CANCELLED"
        notify(booking.user, "Taxi Booking Cancelled", f"Your taxi reservation {booking.booking_reference} has been cancelled.", "TAXI", url_for("taxi_history"))
        db.session.commit()
        flash("Booking cancelled successfully.", "success")
        return redirect(url_for("taxi_history"))

    def _require_booking_access(booking):
        if current_user.role == "ROLE_PASSENGER" and booking.user_id != current_user.id:
            abort(403)
        if current_user.role == "ROLE_TAXI_OPERATOR":
            operator = TaxiOperator.query.filter_by(user_id=current_user.id).first()
            if not operator or not booking.taxi or booking.taxi.operator_id != operator.id:
                abort(403)

    @app.get("/operator/dashboard")
    @role_required("ROLE_TAXI_OPERATOR")
    def operator_dashboard():
        operator = TaxiOperator.query.filter_by(user_id=current_user.id).first()
        taxis = operator.taxis if operator else Taxi.query.order_by(Taxi.vehicle_number.asc()).all()
        bookings_query = TaxiBooking.query.join(Taxi, isouter=True)
        if operator:
            bookings_query = bookings_query.filter(Taxi.operator_id == operator.id)
        bookings = bookings_query.order_by(TaxiBooking.pickup_time.desc()).all()
        forecasts = TaxiDemandForecast.query.order_by(TaxiDemandForecast.time_slot_start.asc()).all()
        return render_template(
            "operator_dashboard.html", operator=operator, taxis=taxis,
            bookings=bookings[:8], demand_predictions=forecasts,
            available_count=sum(taxi.status == "AVAILABLE" for taxi in taxis),
            busy_count=sum(taxi.status == "BUSY" for taxi in taxis),
        )

    @app.route("/operator/taxis", methods=["GET", "POST"])
    @role_required("ROLE_TAXI_OPERATOR")
    def operator_taxis():
        operator = TaxiOperator.query.filter_by(user_id=current_user.id).first()
        if request.method == "POST":
            if not operator:
                flash("Operator profile not configured.", "error")
                return redirect(url_for("operator_taxis"))
            try:
                vehicle_number = request.form.get("vehicleNumber", "").strip().upper()
                model_name = request.form.get("model", "").strip()
                taxi_type = request.form.get("taxiType", "SEDAN")
                passengers = int(request.form.get("passengers", "4"))
                luggage = int(request.form.get("luggage", "3"))
                fare = float(request.form.get("farePerKm", "25"))
                terminal = request.form.get("terminal", "Terminal 2")
                if not vehicle_number or not model_name or taxi_type not in {"SEDAN", "SUV", "PREMIUM", "ELECTRIC", "VAN"}:
                    raise ValueError("Enter a vehicle number, model, and valid vehicle category.")
                if passengers < 1 or passengers > 15 or luggage < 1 or luggage > 20 or fare < 10:
                    raise ValueError("Vehicle capacity or fare is outside the supported range.")
                if Taxi.query.filter_by(vehicle_number=vehicle_number).first():
                    raise ValueError("That vehicle number is already registered.")
                db.session.add(Taxi(operator=operator, vehicle_number=vehicle_number, model=model_name, taxi_type=taxi_type, passenger_capacity=passengers, luggage_capacity=luggage, base_fare_per_km=fare, status="AVAILABLE", current_terminal=terminal))
                db.session.commit()
                flash(f"Vehicle {vehicle_number} registered to fleet!", "success")
            except (ValueError, TypeError) as error:
                db.session.rollback()
                flash(str(error), "error")
            return redirect(url_for("operator_taxis"))
        taxis = operator.taxis if operator else Taxi.query.order_by(Taxi.vehicle_number.asc()).all()
        return render_template("operator_taxis.html", taxis=taxis)

    @app.post("/operator/taxis/<int:taxi_id>/status")
    @role_required("ROLE_TAXI_OPERATOR")
    def update_taxi_status(taxi_id):
        taxi = db.get_or_404(Taxi, taxi_id)
        operator = TaxiOperator.query.filter_by(user_id=current_user.id).first()
        if operator and taxi.operator_id != operator.id:
            abort(403)
        status = request.form.get("status")
        if status not in {"AVAILABLE", "BUSY", "MAINTENANCE"}:
            abort(400)
        taxi.status = status
        terminal = request.form.get("terminal", "").strip()
        if terminal:
            taxi.current_terminal = terminal
        db.session.commit()
        flash(f"Taxi status updated to {status}.", "success")
        return redirect(url_for("operator_taxis"))

    @app.get("/operator/bookings")
    @role_required("ROLE_TAXI_OPERATOR")
    def operator_bookings():
        operator = TaxiOperator.query.filter_by(user_id=current_user.id).first()
        query = TaxiBooking.query.join(Taxi, isouter=True)
        if operator:
            query = query.filter(Taxi.operator_id == operator.id)
        bookings = query.order_by(TaxiBooking.pickup_time.desc()).all()
        return render_template("operator_bookings.html", bookings=bookings)

    @app.post("/operator/bookings/<int:booking_id>/status")
    @role_required("ROLE_TAXI_OPERATOR")
    def operator_update_booking(booking_id):
        booking = db.get_or_404(TaxiBooking, booking_id)
        _require_booking_access(booking)
        status = request.form.get("status")
        if status not in {"DRIVER_ASSIGNED", "COMPLETED", "CANCELLED"}:
            abort(400)
        booking.booking_status = status
        notify(booking.user, f"Taxi Booking Update: {booking.booking_reference}", f"Your booking status is now: {status}", "TAXI", url_for("booking_confirmation", reference=booking.booking_reference))
        db.session.commit()
        flash(f"Booking status updated to {status}.", "success")
        return redirect(url_for("operator_bookings"))

    @app.route("/complaints/submit", methods=["GET", "POST"])
    @login_required
    def submit_complaint():
        if request.method == "POST":
            title = request.form.get("title", "").strip()
            description = request.form.get("description", "").strip()
            category = request.form.get("category", "Other")
            airport_id = request.form.get("airportId", type=int)
            airport = db.session.get(Airport, airport_id) if airport_id else None
            if not title or len(title) > 150 or not description or len(description) > 3000:
                flash("Enter a subject and description within the supported lengths.", "error")
                return render_template("complaint_submit.html", airports=Airport.query.order_by(Airport.code).all(), form=request.form)
            analysis = analyze_complaint(title, description, category)
            reference = None
            while not reference or Complaint.query.filter_by(reference_no=reference).first():
                reference = f"AW-CP-{10000 + random.randrange(90000)}"
            complaint = Complaint(
                reference_no=reference,
                user=current_user,
                title=title,
                description=description,
                category=analysis.category,
                priority=analysis.priority,
                status="SUBMITTED",
                assigned_department=analysis.recommended_department,
                airport=airport,
                terminal=request.form.get("terminal", "").strip() or "Terminal 2",
                flight_number=request.form.get("flightNumber", "").strip() or None,
                booking_reference=request.form.get("bookingReference", "").strip() or None,
                preferred_contact=request.form.get("preferredContact", "Email"),
                attachment_name=secure_filename(request.files["attachment"].filename) if request.files.get("attachment") and request.files["attachment"].filename else None,
            )
            db.session.add(complaint)
            db.session.flush()
            db.session.add(ComplaintAnalysis(
                complaint=complaint,
                detected_category=analysis.category,
                sentiment=analysis.sentiment,
                priority=analysis.priority,
                extracted_keywords=analysis.extracted_keywords,
                summary_reason=analysis.summary_reason,
                recommended_department=analysis.recommended_department,
                urgent_safety=analysis.urgent_safety,
            ))
            if analysis.urgent_safety or analysis.priority == "URGENT":
                db.session.add(Alert(
                    title=f"URGENT SAFETY COMPLAINT: {reference}",
                    message=f"High-priority passenger report received: '{title}'. Auto-assigned to: {analysis.recommended_department}",
                    severity="CRITICAL", category="COMPLAINT", airport=airport,
                ))
            notify(current_user, f"Complaint Registered: {reference}", f"Your report '{title}' has been routed to {analysis.recommended_department}. Priority assigned: {analysis.priority}", "COMPLAINT", url_for("complaint_detail", reference=reference))
            db.session.commit()
            flash(f"Complaint submitted successfully! Reference: {reference}", "success")
            return redirect(url_for("complaint_detail", reference=reference))
        return render_template(
            "complaint_submit.html",
            airports=Airport.query.order_by(Airport.code).all(),
            form={"flightNumber": request.args.get("flightNumber", ""), "bookingReference": request.args.get("bookingRef", "")},
        )

    @app.get("/complaints/track")
    @login_required
    def complaint_track():
        search_ref = request.args.get("searchRef", "").strip()
        if search_ref:
            found = Complaint.query.filter_by(reference_no=search_ref).first()
            if found:
                return redirect(url_for("complaint_detail", reference=found.reference_no))
            flash(f"No complaint record found for reference: {search_ref}", "error")
        complaints = Complaint.query.filter_by(user_id=current_user.id).order_by(Complaint.created_at.desc()).all()
        return render_template("complaint_track.html", complaints=complaints, search_ref=search_ref)

    @app.get("/complaints/view/<reference>")
    @login_required
    def complaint_detail(reference):
        complaint = Complaint.query.filter_by(reference_no=reference).first_or_404()
        if current_user.role == "ROLE_PASSENGER" and complaint.user_id != current_user.id:
            abort(403)
        return render_template("complaint_details.html", complaint=complaint, analysis=complaint.analysis)

    @app.get("/profile")
    @login_required
    def profile():
        return render_template("profile.html", user=current_user)

    @app.post("/profile")
    @login_required
    def update_profile():
        full_name = request.form.get("fullName", "").strip()
        phone = request.form.get("phone", "").strip()
        new_password = request.form.get("newPassword", "")
        if not full_name or len(full_name) > 120 or len(phone) > 30 or (new_password and not 6 <= len(new_password) <= 72):
            flash("Check the name, phone, or new password length and try again.", "error")
            return redirect(url_for("profile"))
        current_user.full_name = full_name
        current_user.phone = phone or None
        if new_password:
            current_user.password = bcrypt.hashpw(new_password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
        db.session.commit()
        flash("Profile updated successfully!", "success")
        return redirect(url_for("profile"))

    @app.get("/notifications")
    @login_required
    def notifications_page():
        notifications = Notification.query.filter_by(user_id=current_user.id).order_by(Notification.created_at.desc()).all()
        return render_template("notifications.html", notifications=notifications)

    @app.post("/notifications/<int:notification_id>/read")
    @login_required
    def mark_notification_read(notification_id):
        notification = db.get_or_404(Notification, notification_id)
        if notification.user_id != current_user.id:
            abort(403)
        notification.is_read = True
        db.session.commit()
        return redirect(url_for("notifications_page"))

    @app.post("/notifications/read-all")
    @login_required
    def mark_all_notifications_read():
        Notification.query.filter_by(user_id=current_user.id, is_read=False).update({"is_read": True})
        db.session.commit()
        return redirect(url_for("notifications_page"))

    @app.get("/admin/dashboard")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_dashboard():
        flights = flights_query().limit(5).all()
        complaints = Complaint.query.order_by(Complaint.created_at.desc()).limit(5).all()
        alerts = Alert.query.filter_by(active=True).order_by(Alert.created_at.desc()).all()
        stats = {
            "total_flights": Flight.query.count(),
            "delayed_flights": Flight.query.filter_by(status="DELAYED").count(),
            "on_time_flights": Flight.query.filter_by(status="ON_TIME").count(),
            "cancelled_flights": Flight.query.filter_by(status="CANCELLED").count(),
            "total_bookings": TaxiBooking.query.count(),
            "active_bookings": TaxiBooking.query.filter(TaxiBooking.booking_status.in_(["CONFIRMED", "DRIVER_ASSIGNED"])).count(),
            "available_taxis": Taxi.query.filter_by(status="AVAILABLE").count(),
            "total_complaints": Complaint.query.count(),
            "open_complaints": Complaint.query.filter(Complaint.status.in_(["SUBMITTED", "UNDER_REVIEW", "ASSIGNED", "IN_PROGRESS"])).count(),
            "urgent_complaints": Complaint.query.filter_by(priority="URGENT").count(),
            "active_alerts": len(alerts),
            "critical_alerts": sum(alert.severity == "CRITICAL" for alert in alerts),
        }
        return render_template("admin_dashboard.html", stats=stats, recent_flights=flights, recent_complaints=complaints, alerts=alerts)

    @app.get("/admin/flights")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_flights():
        return render_template("admin_flights.html", flights=flights_query().all())

    @app.post("/admin/flights/<int:flight_id>/status")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_update_flight(flight_id):
        flight = db.get_or_404(Flight, flight_id)
        status = request.form.get("status")
        try:
            delay_minutes = int(request.form.get("delayMinutes", "0"))
            if status not in {"ON_TIME", "DELAYED", "CANCELLED", "LANDED"} or not 0 <= delay_minutes <= 600:
                raise ValueError("Choose a valid status and delay between 0 and 600 minutes.")
            update_flight_status(flight, status, delay_minutes, request.form.get("confirmedReason"))
            db.session.commit()
            flash(f"Flight {flight.flight_number} updated to {status} ({delay_minutes} min delay). Connected taxi bookings updated!", "success")
        except ValueError as error:
            db.session.rollback()
            flash(str(error), "error")
        return redirect(url_for("admin_flights"))

    @app.get("/admin/complaints")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_complaints():
        complaints = Complaint.query.order_by(Complaint.created_at.desc()).all()
        return render_template("admin_complaints.html", complaints=complaints)

    @app.get("/admin/complaints/<int:complaint_id>")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_complaint_detail(complaint_id):
        complaint = db.get_or_404(Complaint, complaint_id)
        return render_template("admin_complaint_detail.html", complaint=complaint, analysis=complaint.analysis)

    @app.post("/admin/complaints/<int:complaint_id>/status")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_update_complaint(complaint_id):
        complaint = db.get_or_404(Complaint, complaint_id)
        status = request.form.get("status")
        department = request.form.get("department", "").strip()
        notes = request.form.get("resolutionNotes", "").strip()
        if status not in {"SUBMITTED", "UNDER_REVIEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"}:
            abort(400)
        complaint.status = status
        if department:
            complaint.assigned_department = department[:80]
        if notes:
            complaint.resolution_notes = notes[:1500]
        complaint.updated_at = datetime.now()
        notify(complaint.user, f"Complaint Status Updated: {complaint.reference_no}", f"Your complaint '{complaint.title}' is now marked as {status}" + (f". Notes: {notes}" if notes else ""), "COMPLAINT", url_for("complaint_detail", reference=complaint.reference_no))
        db.session.commit()
        flash(f"Complaint {complaint.reference_no} status updated to {status}.", "success")
        return redirect(url_for("admin_complaint_detail", complaint_id=complaint.id))

    @app.get("/admin/analytics")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_analytics():
        category_stats = db.session.query(Complaint.category, func.count(Complaint.id)).group_by(Complaint.category).all()
        status_stats = db.session.query(Complaint.status, func.count(Complaint.id)).group_by(Complaint.status).all()
        department_stats = db.session.query(Complaint.assigned_department, func.count(Complaint.id)).group_by(Complaint.assigned_department).all()
        sentiment_stats = db.session.query(ComplaintAnalysis.sentiment, func.count(ComplaintAnalysis.id)).group_by(ComplaintAnalysis.sentiment).all()
        total_flights = Flight.query.count()
        stats = {
            "total_flights": total_flights,
            "delayed_flights": Flight.query.filter_by(status="DELAYED").count(),
            "on_time_flights": Flight.query.filter_by(status="ON_TIME").count(),
            "cancelled_flights": Flight.query.filter_by(status="CANCELLED").count(),
            "total_bookings": TaxiBooking.query.count(),
            "active_bookings": TaxiBooking.query.filter(TaxiBooking.booking_status.in_(["CONFIRMED", "DRIVER_ASSIGNED"])).count(),
        }
        return render_template("admin_analytics.html", stats=stats, category_stats=category_stats, status_stats=status_stats, department_stats=department_stats, sentiment_stats=sentiment_stats)

    @app.post("/admin/alerts/<int:alert_id>/dismiss")
    @role_required("ROLE_AIRPORT_ADMIN")
    def admin_dismiss_alert(alert_id):
        alert = db.get_or_404(Alert, alert_id)
        alert.active = False
        db.session.commit()
        flash("Alert dismissed.", "success")
        return redirect(url_for("admin_dashboard"))

    @app.route("/console", defaults={"page": "dashboard"}, methods=["GET", "POST"])
    @app.route("/console/<page>", methods=["GET", "POST"])
    def legacy_console(page):
        titles = {
            "dashboard": "Operations overview",
            "flight-delay": "Flight Delay Prediction & Analysis",
            "weather-disruption": "Weather-Based Airport Disruption Prediction",
            "passenger-crowd": "Airport Passenger Crowd & Queue Prediction",
            "taxi-demand": "Airport Taxi Demand Prediction",
            "medical-emergency": "Airport Medical Emergency Response Analytics",
            "complaint-analyzer": "AI Airport Customer Complaint Analyzer",
            "analytics": "Airport Analytics",
            "alerts": "Centralized Alerts",
            "recommendations": "Operational Recommendations",
            "settings": "Settings & Data Notes",
        }
        if page not in titles:
            abort(404)
        data = app.extensions["legacy_data"]
        values = request.form.to_dict() if request.method == "POST" else {}
        result = None
        error = None

        def number(name, default, integer=True):
            parsed = int(values.get(name, str(default)).strip()) if integer else float(values.get(name, str(default)).strip())
            if parsed < 0:
                raise ValueError("Values cannot be negative.")
            return parsed

        if request.method == "POST":
            try:
                if page == "flight-delay":
                    delay = number("previousDelay", 18)
                    turnaround = number("turnaround", 72)
                    passengers = number("passengers", 268)
                    weather = number("weather", 0.68, False)
                    if weather > 1:
                        raise ValueError("Weather risk must be between 0 and 1.")
                    prediction = legacy_predict_flight(delay, turnaround, passengers, weather, True)
                    result = {"headline": f"Flight {values.get('flight', 'AI245')} · {round(prediction.probability * 100)}% delay probability · {prediction.expected_minutes} min expected · {prediction.risk} risk", "details": prediction.factors, "context": f"{values.get('airline', 'Air India')} · {values.get('departure', 'DEL')} → {values.get('arrival', 'BOM')} · {values.get('scheduledDeparture', '18:30')}–{values.get('scheduledArrival', '20:35')}"}
                elif page == "weather-disruption":
                    rain = number("rain", 18, False)
                    wind = number("wind", 54, False)
                    visibility = number("visibility", 3.2, False)
                    temperature = number("temperature", 31, False)
                    if temperature < -80 or temperature > 70:
                        raise ValueError("Temperature must be between -80 and 70 °C.")
                    prediction = legacy_predict_weather(rain, wind, visibility, temperature, "thunder" in values)
                    result = {"headline": f"Weather risk: {prediction.risk} · {round(prediction.probability * 100)}% disruption probability", "details": [prediction.impact]}
                elif page == "passenger-crowd":
                    passengers = number("passengerCount", 1850)
                    arrivals = number("arrivals", 14)
                    departures = number("departures", 18)
                    counters = number("counters", 14)
                    lanes = number("lanes", 7)
                    if counters < 1 or lanes < 1:
                        raise ValueError("Counters and security lanes must be at least one.")
                    prediction = legacy_predict_crowd(passengers, arrivals, departures, "peak" in values, "holiday" in values, counters, lanes)
                    recommendation = f"Open {prediction.extra_lanes} additional security lane(s)." if prediction.extra_lanes else "Maintain current lane staffing."
                    result = {"headline": f"{prediction.expected_passengers:,} expected passengers · {prediction.level} crowd · {prediction.security_queue_minutes} min security · {prediction.checkin_queue_minutes} min check-in", "details": [recommendation]}
                elif page == "taxi-demand":
                    arrivals = number("arrivals", 16)
                    passengers = number("passengers", 1450)
                    available = number("availableTaxis", 780)
                    prediction = legacy_predict_taxi(arrivals, passengers, "peak" in values, "holiday" in values, values.get("weather", "Clear"), available)
                    recommendation = "Arrange additional taxi supply." if prediction.shortage else "Current supply covers demand."
                    result = {"headline": f"{prediction.demand:,} expected demand · {available:,} available · {prediction.shortage:,} shortage · {prediction.risk} risk · ~{prediction.wait_minutes} min wait", "details": [recommendation]}
                elif page == "medical-emergency":
                    response = number("response", 7)
                    team = values.get("team", "Available") == "Available"
                    ambulance = values.get("ambulance", "Available") == "Available"
                    prediction = legacy_predict_medical(len(data.emergencies), True, team, ambulance)
                    recommendation = "Deploy additional medical resources." if not team or not ambulance else "Maintain on-call coverage."
                    result = {"headline": f"{prediction.risk} emergency risk · {prediction.required_staff} medical staff · {prediction.required_ambulances} ambulances", "details": [f"Current team: {values.get('team', 'Available')} · Ambulance: {values.get('ambulance', 'Available')} · {values.get('location', 'Terminal 1')} · {values.get('emergencyType', 'Fainting')} · response input {response} min", recommendation]}
                elif page == "complaint-analyzer":
                    text = values.get("complaint", "").strip()
                    if not text:
                        raise ValueError("Enter complaint text before analyzing.")
                    analysis = analyze_legacy_complaint(text)
                    result = {"headline": f"{analysis['category']} · {analysis['sentiment']} sentiment · {analysis['priority']} priority", "details": [analysis["explanation"]]}
            except (ValueError, TypeError) as validation_error:
                error = str(validation_error) or "Enter valid values for all fields."

        return render_template(
            "console.html", page=page, title=titles[page], values=values,
            result=result, error=error, data=data,
            analytics=legacy_analytics(data),
            metrics=legacy_dashboard_metrics(data),
            alerts=generate_legacy_alerts(data),
            recommendations=generate_legacy_recommendations(data),
            analyze_legacy_complaint=analyze_legacy_complaint,
        )

    @app.cli.command("create-admin")
    @click.option("--username", prompt=True)
    @click.option("--full-name", prompt=True)
    def create_admin(username, full_name):
        """Create an administrator account without storing its password in source."""
        username = username.strip()
        if not username or User.query.filter(func.lower(User.username) == username.lower()).first():
            raise click.ClickException("Username is blank or already registered.")
        password = click.prompt("Administrator password", hide_input=True, confirmation_prompt=True)
        if not 12 <= len(password) <= 72:
            raise click.ClickException("Administrator passwords must contain 12 to 72 characters.")
        user = User(
            username=username,
            password=bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8"),
            full_name=full_name.strip() or username,
            role="ROLE_AIRPORT_ADMIN",
        )
        db.session.add(user)
        db.session.commit()
        click.echo(f"Administrator account created for {username}.")

    @app.errorhandler(403)
    def forbidden(_error):
        return render_template("access_denied.html"), 403

    @app.errorhandler(404)
    def not_found(_error):
        return render_template("error.html"), 404

    @app.errorhandler(413)
    def too_large(_error):
        flash("Uploaded request is larger than the 10 MB limit.", "error")
        return redirect(request.referrer or url_for("index"))

    @app.get("/healthz")
    def healthz():
        return {"status": "ok"}

    with app.app_context():
        app.extensions["legacy_data"] = LegacyDataManager()
        db.create_all()
        if app.config["AIRWISE_DEMO_MODE"]:
            from seed_data import seed_demo_data
            seed_demo_data()

    from urllib.parse import parse_qs

    class VercelPathFixMiddleware:
        def __init__(self, wsgi_app):
            self.wsgi_app = wsgi_app

        def __call__(self, environ, start_response):
            qs = parse_qs(environ.get("QUERY_STRING", ""), keep_blank_values=True)
            if "__path__" in qs:
                target = qs["__path__"][0]
                if not target.startswith("/"):
                    target = "/" + target
                environ["PATH_INFO"] = target
            elif environ.get("HTTP_X_FORWARDED_URI"):
                environ["PATH_INFO"] = urlsplit(environ["HTTP_X_FORWARDED_URI"]).path
            else:
                path_info = environ.get("PATH_INFO", "")
                for prefix in ("/api/index.py", "/api/index", "/api"):
                    if path_info.startswith(prefix):
                        remainder = path_info[len(prefix):]
                        environ["PATH_INFO"] = remainder if remainder.startswith("/") else "/" + remainder.lstrip("/")
                        break
            return self.wsgi_app(environ, start_response)

    from werkzeug.middleware.proxy_fix import ProxyFix
    app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_host=1, x_port=1, x_prefix=1)
    app.wsgi_app = VercelPathFixMiddleware(app.wsgi_app)

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")))