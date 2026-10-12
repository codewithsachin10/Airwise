from datetime import datetime

from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin


db = SQLAlchemy()


class User(UserMixin, db.Model):
    __tablename__ = "users"
    __table_args__ = (db.Index("idx_user_username", "username", unique=True),)

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(100), nullable=False, unique=True)
    password = db.Column(db.String(255), nullable=False)
    full_name = db.Column("full_name", db.String(120), nullable=False)
    phone = db.Column(db.String(30))
    role = db.Column(db.String(30), nullable=False)
    enabled = db.Column(db.Boolean, nullable=False, default=True)
    created_at = db.Column("created_at", db.DateTime, nullable=False, default=datetime.now)

    notifications = db.relationship("Notification", back_populates="user")
    bookings = db.relationship("TaxiBooking", back_populates="user")
    complaints = db.relationship("Complaint", back_populates="user")
    taxi_operator = db.relationship("TaxiOperator", back_populates="user", uselist=False)

    @property
    def is_active(self):
        return self.enabled


class Airport(db.Model):
    __tablename__ = "airports"
    __table_args__ = (db.Index("idx_airport_code", "code", unique=True),)

    id = db.Column(db.Integer, primary_key=True)
    code = db.Column(db.String(10), nullable=False, unique=True)
    name = db.Column(db.String(150), nullable=False)
    city = db.Column(db.String(100), nullable=False)
    country = db.Column(db.String(100), nullable=False)
    latitude = db.Column(db.Float)
    longitude = db.Column(db.Float)
    terminals = db.Column(db.String(100))

    departure_flights = db.relationship(
        "Flight", foreign_keys="Flight.departure_airport_id", back_populates="departure_airport"
    )
    arrival_flights = db.relationship(
        "Flight", foreign_keys="Flight.arrival_airport_id", back_populates="arrival_airport"
    )
    weather_records = db.relationship("WeatherRecord", back_populates="airport")
    passenger_flow_records = db.relationship("PassengerFlowRecord", back_populates="airport")


class Flight(db.Model):
    __tablename__ = "flights"
    __table_args__ = (
        db.Index("idx_flight_number", "flight_number"),
        db.Index("idx_flight_dep_date", "scheduled_departure"),
    )

    id = db.Column(db.Integer, primary_key=True)
    flight_number = db.Column("flight_number", db.String(20), nullable=False)
    airline = db.Column(db.String(80), nullable=False)
    departure_airport_id = db.Column("departure_airport_id", db.Integer, db.ForeignKey("airports.id"), nullable=False)
    arrival_airport_id = db.Column("arrival_airport_id", db.Integer, db.ForeignKey("airports.id"), nullable=False)
    scheduled_departure = db.Column("scheduled_departure", db.DateTime, nullable=False)
    scheduled_arrival = db.Column("scheduled_arrival", db.DateTime, nullable=False)
    estimated_departure = db.Column("estimated_departure", db.DateTime)
    estimated_arrival = db.Column("estimated_arrival", db.DateTime)
    status = db.Column(db.String(20), nullable=False, default="ON_TIME")
    delay_minutes = db.Column("delay_minutes", db.Integer, nullable=False, default=0)
    aircraft_type = db.Column("aircraft_type", db.String(40), default="Airbus A320neo")
    passenger_count = db.Column("passenger_count", db.Integer, nullable=False, default=180)
    terminal = db.Column(db.String(20), default="Terminal 2")
    gate = db.Column(db.String(20), default="Gate 14")
    previous_delay_minutes = db.Column("previous_delay_minutes", db.Integer, default=0)
    turnaround_minutes = db.Column("turnaround_minutes", db.Integer, default=45)
    confirmed_delay_reason = db.Column("confirmed_delay_reason", db.String(255))

    departure_airport = db.relationship(
        "Airport", foreign_keys=[departure_airport_id], back_populates="departure_flights"
    )
    arrival_airport = db.relationship(
        "Airport", foreign_keys=[arrival_airport_id], back_populates="arrival_flights"
    )
    predictions = db.relationship("FlightPredictionRecord", back_populates="flight")
    bookings = db.relationship("TaxiBooking", back_populates="flight")

    @property
    def effective_departure(self):
        return self.estimated_departure or self.scheduled_departure

    @property
    def effective_arrival(self):
        return self.estimated_arrival or self.scheduled_arrival


class WeatherRecord(db.Model):
    __tablename__ = "weather_records"
    __table_args__ = (db.Index("idx_weather_airport", "airport_id"),)

    id = db.Column(db.Integer, primary_key=True)
    airport_id = db.Column("airport_id", db.Integer, db.ForeignKey("airports.id"), nullable=False)
    recorded_at = db.Column("recorded_at", db.DateTime, nullable=False, default=datetime.now)
    condition = db.Column(db.String(60), nullable=False, default="Clear")
    temperature_c = db.Column("temperature_c", db.Float, default=28.0)
    rainfall_mm = db.Column("rainfall_mm", db.Float, default=0.0)
    wind_speed_kmh = db.Column("wind_speed_kmh", db.Float, default=14.0)
    visibility_km = db.Column("visibility_km", db.Float, default=10.0)
    thunderstorm = db.Column(db.Boolean, default=False)
    disruption_level = db.Column("disruption_level", db.String(20), nullable=False, default="LOW")

    airport = db.relationship("Airport", back_populates="weather_records")


class PassengerFlowRecord(db.Model):
    __tablename__ = "passenger_flow_records"
    __table_args__ = (db.Index("idx_flow_airport", "airport_id"),)

    id = db.Column(db.Integer, primary_key=True)
    airport_id = db.Column("airport_id", db.Integer, db.ForeignKey("airports.id"), nullable=False)
    terminal = db.Column(db.String(30), nullable=False, default="Terminal 2")
    recorded_at = db.Column("recorded_at", db.DateTime, nullable=False, default=datetime.now)
    passenger_volume = db.Column("passenger_volume", db.Integer, default=2400)
    security_queue_minutes = db.Column("security_queue_minutes", db.Integer, default=14)
    checkin_queue_minutes = db.Column("checkin_queue_minutes", db.Integer, default=8)
    active_lanes = db.Column("active_lanes", db.Integer, default=6)
    recommended_lanes = db.Column("recommended_lanes", db.Integer, default=6)
    congestion_level = db.Column("congestion_level", db.String(20), nullable=False, default="MODERATE")

    airport = db.relationship("Airport", back_populates="passenger_flow_records")


class FlightPredictionRecord(db.Model):
    __tablename__ = "flight_predictions"
    __table_args__ = (db.Index("idx_flight_pred_flight", "flight_id"),)

    id = db.Column(db.Integer, primary_key=True)
    flight_id = db.Column("flight_id", db.Integer, db.ForeignKey("flights.id"), nullable=False)
    predicted_at = db.Column("predicted_at", db.DateTime, nullable=False, default=datetime.now)
    delay_probability = db.Column("delay_probability", db.Float, nullable=False)
    expected_delay_minutes = db.Column("expected_delay_minutes", db.Integer, nullable=False)
    risk_level = db.Column("risk_level", db.String(20), nullable=False, default="LOW")
    contributing_factors = db.Column("contributing_factors", db.String(500))
    weather_risk_score = db.Column("weather_risk_score", db.Float, default=0.0)
    congestion_risk_score = db.Column("congestion_risk_score", db.Float, default=0.0)
    demo_data = db.Column("demo_data", db.Boolean, nullable=False, default=True)

    flight = db.relationship("Flight", back_populates="predictions")


class TaxiOperator(db.Model):
    __tablename__ = "taxi_operators"

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column("user_id", db.Integer, db.ForeignKey("users.id"), nullable=False, unique=True)
    company_name = db.Column("company_name", db.String(120), nullable=False)
    license_number = db.Column("license_number", db.String(60), nullable=False)
    contact_phone = db.Column("contact_phone", db.String(30))
    fleet_size = db.Column("fleet_size", db.Integer, default=15)

    user = db.relationship("User", back_populates="taxi_operator")
    taxis = db.relationship("Taxi", back_populates="operator")


class Taxi(db.Model):
    __tablename__ = "taxis"
    __table_args__ = (db.Index("idx_taxi_veh_num", "vehicle_number", unique=True),)

    id = db.Column(db.Integer, primary_key=True)
    operator_id = db.Column("operator_id", db.Integer, db.ForeignKey("taxi_operators.id"), nullable=False)
    vehicle_number = db.Column("vehicle_number", db.String(30), nullable=False, unique=True)
    model = db.Column(db.String(80), nullable=False)
    taxi_type = db.Column("taxi_type", db.String(20), nullable=False, default="SEDAN")
    passenger_capacity = db.Column("passenger_capacity", db.Integer, nullable=False, default=4)
    luggage_capacity = db.Column("luggage_capacity", db.Integer, nullable=False, default=3)
    base_fare_per_km = db.Column("base_fare_per_km", db.Float, nullable=False, default=22.0)
    status = db.Column(db.String(20), nullable=False, default="AVAILABLE")
    current_terminal = db.Column("current_terminal", db.String(30), default="Terminal 2")

    operator = db.relationship("TaxiOperator", back_populates="taxis")
    bookings = db.relationship("TaxiBooking", back_populates="taxi")
    availability = db.relationship("TaxiAvailability", back_populates="taxi")


class TaxiAvailability(db.Model):
    __tablename__ = "taxi_availability"
    __table_args__ = (db.Index("idx_avail_slot", "time_slot_start", "time_slot_end"),)

    id = db.Column(db.Integer, primary_key=True)
    taxi_id = db.Column("taxi_id", db.Integer, db.ForeignKey("taxis.id"), nullable=False)
    time_slot_start = db.Column("time_slot_start", db.DateTime, nullable=False)
    time_slot_end = db.Column("time_slot_end", db.DateTime, nullable=False)
    terminal = db.Column(db.String(30), nullable=False, default="Terminal 2")
    available = db.Column(db.Boolean, nullable=False, default=True)

    taxi = db.relationship("Taxi", back_populates="availability")


class TaxiDemandForecast(db.Model):
    __tablename__ = "taxi_demand_predictions"

    id = db.Column(db.Integer, primary_key=True)
    airport_id = db.Column("airport_id", db.Integer, db.ForeignKey("airports.id"), nullable=False)
    terminal = db.Column(db.String(30), nullable=False, default="Terminal 2")
    time_slot_start = db.Column("time_slot_start", db.DateTime, nullable=False)
    time_slot_end = db.Column("time_slot_end", db.DateTime, nullable=False)
    predicted_demand = db.Column("predicted_demand", db.Integer, default=120)
    available_taxis = db.Column("available_taxis", db.Integer, default=85)
    reserved_taxis = db.Column("reserved_taxis", db.Integer, default=30)
    unmet_demand = db.Column("unmet_demand", db.Integer, default=5)
    demand_level = db.Column("demand_level", db.String(20), nullable=False, default="MEDIUM")
    recommended_vehicles = db.Column("recommended_vehicles", db.Integer, default=15)
    demo_data = db.Column("demo_data", db.Boolean, nullable=False, default=True)

    airport = db.relationship("Airport")


class TaxiBooking(db.Model):
    __tablename__ = "taxi_bookings"
    __table_args__ = (
        db.Index("idx_booking_ref", "booking_reference", unique=True),
        db.Index("idx_booking_user", "user_id"),
        db.Index("idx_booking_slot", "pickup_time"),
    )

    id = db.Column(db.Integer, primary_key=True)
    booking_reference = db.Column("booking_reference", db.String(30), nullable=False, unique=True)
    user_id = db.Column("user_id", db.Integer, db.ForeignKey("users.id"), nullable=False)
    flight_id = db.Column("flight_id", db.Integer, db.ForeignKey("flights.id"))
    taxi_id = db.Column("taxi_id", db.Integer, db.ForeignKey("taxis.id"))
    requested_taxi_type = db.Column("requested_taxi_type", db.String(20), nullable=False, default="SEDAN")
    pickup_terminal = db.Column("pickup_terminal", db.String(50), nullable=False, default="Terminal 2")
    destination = db.Column(db.String(200), nullable=False)
    passenger_count = db.Column("passenger_count", db.Integer, nullable=False, default=1)
    luggage_count = db.Column("luggage_count", db.Integer, nullable=False, default=1)
    pickup_time = db.Column("pickup_time", db.DateTime, nullable=False)
    estimated_fare = db.Column("estimated_fare", db.Float, nullable=False)
    booking_status = db.Column("booking_status", db.String(30), nullable=False, default="CONFIRMED")
    flight_delayed_flag = db.Column("flight_delayed_flag", db.Boolean, default=False)
    flight_delay_notice = db.Column("flight_delay_notice", db.String(255))
    notes = db.Column(db.String(500))
    created_at = db.Column("created_at", db.DateTime, nullable=False, default=datetime.now)

    user = db.relationship("User", back_populates="bookings")
    flight = db.relationship("Flight", back_populates="bookings")
    taxi = db.relationship("Taxi", back_populates="bookings")


class Complaint(db.Model):
    __tablename__ = "complaints"
    __table_args__ = (
        db.Index("idx_complaint_ref", "reference_no", unique=True),
        db.Index("idx_complaint_user", "user_id"),
        db.Index("idx_complaint_status", "status"),
        db.Index("idx_complaint_priority", "priority"),
    )

    id = db.Column(db.Integer, primary_key=True)
    reference_no = db.Column("reference_no", db.String(30), nullable=False, unique=True)
    user_id = db.Column("user_id", db.Integer, db.ForeignKey("users.id"), nullable=False)
    title = db.Column(db.String(150), nullable=False)
    description = db.Column(db.String(3000), nullable=False)
    category = db.Column(db.String(60), nullable=False, default="Other")
    priority = db.Column(db.String(20), nullable=False, default="MEDIUM")
    status = db.Column(db.String(20), nullable=False, default="SUBMITTED")
    assigned_department = db.Column("assigned_department", db.String(80), default="Customer Relations")
    airport_id = db.Column("airport_id", db.Integer, db.ForeignKey("airports.id"))
    terminal = db.Column(db.String(30), default="Terminal 2")
    flight_number = db.Column("flight_number", db.String(30))
    booking_reference = db.Column("booking_reference", db.String(30))
    preferred_contact = db.Column("preferred_contact", db.String(50), default="Email")
    attachment_name = db.Column("attachment_name", db.String(255))
    resolution_notes = db.Column("resolution_notes", db.String(1500))
    created_at = db.Column("created_at", db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column("updated_at", db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    user = db.relationship("User", back_populates="complaints")
    airport = db.relationship("Airport")
    analysis = db.relationship("ComplaintAnalysis", back_populates="complaint", uselist=False)


class ComplaintAnalysis(db.Model):
    __tablename__ = "complaint_analyses"

    id = db.Column(db.Integer, primary_key=True)
    complaint_id = db.Column("complaint_id", db.Integer, db.ForeignKey("complaints.id"), nullable=False, unique=True)
    detected_category = db.Column("detected_category", db.String(60), nullable=False)
    sentiment = db.Column(db.String(20), nullable=False, default="NEUTRAL")
    priority = db.Column(db.String(20), nullable=False, default="MEDIUM")
    extracted_keywords = db.Column("extracted_keywords", db.String(500))
    summary_reason = db.Column("summary_reason", db.String(1000))
    recommended_department = db.Column("recommended_department", db.String(100), nullable=False)
    urgent_safety = db.Column("urgent_safety", db.Boolean, nullable=False, default=False)
    analyzed_at = db.Column("analyzed_at", db.DateTime, nullable=False, default=datetime.now)

    complaint = db.relationship("Complaint", back_populates="analysis")


class Alert(db.Model):
    __tablename__ = "alerts"
    __table_args__ = (
        db.Index("idx_alert_active", "active"),
        db.Index("idx_alert_severity", "severity"),
    )

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(150), nullable=False)
    message = db.Column(db.String(1000), nullable=False)
    severity = db.Column(db.String(20), nullable=False, default="WARNING")
    category = db.Column(db.String(50), nullable=False, default="OPERATIONS")
    active = db.Column(db.Boolean, nullable=False, default=True)
    airport_id = db.Column("airport_id", db.Integer, db.ForeignKey("airports.id"))
    created_at = db.Column("created_at", db.DateTime, nullable=False, default=datetime.now)

    airport = db.relationship("Airport")


class Notification(db.Model):
    __tablename__ = "notifications"
    __table_args__ = (
        db.Index("idx_notif_user", "user_id"),
        db.Index("idx_notif_read", "is_read"),
    )

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column("user_id", db.Integer, db.ForeignKey("users.id"), nullable=False)
    title = db.Column(db.String(120), nullable=False)
    message = db.Column(db.String(1000), nullable=False)
    type = db.Column(db.String(30), nullable=False, default="SYSTEM")
    is_read = db.Column("is_read", db.Boolean, nullable=False, default=False)
    link_url = db.Column("link_url", db.String(200))
    created_at = db.Column("created_at", db.DateTime, nullable=False, default=datetime.now)

    user = db.relationship("User", back_populates="notifications")