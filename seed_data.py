from datetime import datetime, timedelta
import math
import os

import bcrypt

from calculations import analyze_complaint, predict_flight_delay
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
    TaxiBooking,
    TaxiDemandForecast,
    TaxiOperator,
    User,
    WeatherRecord,
    db,
)


def _password_hash(password):
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def _add_complaint(user, reference, title, description, category, airport,
                   terminal, flight_number=None, booking_reference=None):
    analysis_result = analyze_complaint(title, description, category)
    created = datetime.now() - timedelta(hours=3)
    complaint = Complaint(
        reference_no=reference,
        user=user,
        title=title,
        description=description,
        category=analysis_result.category,
        priority=analysis_result.priority,
        status="UNDER_REVIEW" if analysis_result.priority == "URGENT" else "SUBMITTED",
        assigned_department=analysis_result.recommended_department,
        airport=airport,
        terminal=terminal,
        flight_number=flight_number,
        booking_reference=booking_reference,
        preferred_contact="Email",
        created_at=created,
        updated_at=datetime.now() - timedelta(hours=1),
    )
    db.session.add(complaint)
    db.session.flush()
    db.session.add(ComplaintAnalysis(
        complaint=complaint,
        detected_category=analysis_result.category,
        sentiment=analysis_result.sentiment,
        priority=analysis_result.priority,
        extracted_keywords=analysis_result.extracted_keywords,
        summary_reason=analysis_result.summary_reason,
        recommended_department=analysis_result.recommended_department,
        urgent_safety=analysis_result.urgent_safety,
    ))
    return complaint


def seed_demo_data():
    if User.query.count() > 0:
        return False

    password_keys = {
        "admin": "AIRWISE_DEMO_ADMIN_PASSWORD",
        "passenger": "AIRWISE_DEMO_PASSENGER_PASSWORD",
        "operator": "AIRWISE_DEMO_OPERATOR_PASSWORD",
        "priya": "AIRWISE_DEMO_PRIYA_PASSWORD",
    }
    demo_passwords = {username: os.getenv(key, "") for username, key in password_keys.items()}
    missing_passwords = [key for username, key in password_keys.items() if not demo_passwords[username]]
    if missing_passwords:
        raise RuntimeError(
            "Demo seeding requires local environment passwords: " + ", ".join(missing_passwords)
        )

    now = datetime.now()
    admin = User(
        username="admin", password=_password_hash(demo_passwords["admin"]),
        full_name="Airport Operations Admin", phone="+91 98200 11001",
        role="ROLE_AIRPORT_ADMIN",
    )
    passenger = User(
        username="passenger", password=_password_hash(demo_passwords["passenger"]),
        full_name="Johnathan Sterling", phone="+91 98200 22002",
        role="ROLE_PASSENGER",
    )
    operator_user = User(
        username="operator", password=_password_hash(demo_passwords["operator"]),
        full_name="Rajesh Kumar (AeroCabs)", phone="+91 98200 33003",
        role="ROLE_TAXI_OPERATOR",
    )
    second_passenger = User(
        username="priya", password=_password_hash(demo_passwords["priya"]),
        full_name="Priya Sharma", phone="+91 98200 44004",
        role="ROLE_PASSENGER",
    )
    db.session.add_all([admin, passenger, operator_user, second_passenger])
    db.session.flush()

    airports = [
        Airport(code="BOM", name="Chhatrapati Shivaji Maharaj International Airport", city="Mumbai", country="India", latitude=19.0896, longitude=72.8656, terminals="Terminal 1, Terminal 2"),
        Airport(code="DEL", name="Indira Gandhi International Airport", city="New Delhi", country="India", latitude=28.5562, longitude=77.1000, terminals="Terminal 1, Terminal 2, Terminal 3"),
        Airport(code="BLR", name="Kempegowda International Airport", city="Bengaluru", country="India", latitude=13.1986, longitude=77.7066, terminals="Terminal 1, Terminal 2"),
        Airport(code="DXB", name="Dubai International Airport", city="Dubai", country="UAE", latitude=25.2532, longitude=55.3657, terminals="Terminal 1, Terminal 2, Terminal 3"),
        Airport(code="LHR", name="Heathrow Airport", city="London", country="United Kingdom", latitude=51.4700, longitude=-0.4543, terminals="Terminal 2, Terminal 3, Terminal 4, Terminal 5"),
        Airport(code="SIN", name="Singapore Changi Airport", city="Singapore", country="Singapore", latitude=1.3644, longitude=103.9915, terminals="Terminal 1, Terminal 2, Terminal 3, Terminal 4"),
    ]
    db.session.add_all(airports)
    db.session.flush()
    bom, delhi, blr, dxb, lhr, sin = airports

    db.session.add_all([
        WeatherRecord(airport=bom, condition="Heavy Rain & Winds", temperature_c=27.5, rainfall_mm=38.2, wind_speed_kmh=54.0, visibility_km=3.2, thunderstorm=True, disruption_level="HIGH"),
        WeatherRecord(airport=delhi, condition="Foggy", temperature_c=18.0, rainfall_mm=0.0, wind_speed_kmh=12.0, visibility_km=1.8, thunderstorm=False, disruption_level="MEDIUM"),
        WeatherRecord(airport=blr, condition="Clear", temperature_c=24.5, rainfall_mm=0.0, wind_speed_kmh=14.0, visibility_km=10.0, thunderstorm=False, disruption_level="LOW"),
        WeatherRecord(airport=dxb, condition="Sunny", temperature_c=36.0, rainfall_mm=0.0, wind_speed_kmh=18.0, visibility_km=10.0, thunderstorm=False, disruption_level="LOW"),
        WeatherRecord(airport=lhr, condition="Overcast", temperature_c=14.0, rainfall_mm=4.2, wind_speed_kmh=28.0, visibility_km=8.0, thunderstorm=False, disruption_level="LOW"),
        WeatherRecord(airport=sin, condition="Scattered Showers", temperature_c=29.0, rainfall_mm=12.0, wind_speed_kmh=16.0, visibility_km=6.5, thunderstorm=False, disruption_level="LOW"),
    ])

    db.session.add_all([
        PassengerFlowRecord(airport=bom, terminal="Terminal 2", passenger_volume=3420, security_queue_minutes=22, checkin_queue_minutes=12, active_lanes=7, recommended_lanes=9, congestion_level="HIGH"),
        PassengerFlowRecord(airport=delhi, terminal="Terminal 3", passenger_volume=2850, security_queue_minutes=14, checkin_queue_minutes=8, active_lanes=8, recommended_lanes=8, congestion_level="MODERATE"),
        PassengerFlowRecord(airport=blr, terminal="Terminal 2", passenger_volume=1640, security_queue_minutes=8, checkin_queue_minutes=5, active_lanes=6, recommended_lanes=6, congestion_level="LOW"),
    ])

    flights = [
        Flight(flight_number="AI-245", airline="Air India", departure_airport=delhi, arrival_airport=bom, scheduled_departure=now + timedelta(hours=1), scheduled_arrival=now + timedelta(hours=3, minutes=15), estimated_departure=now + timedelta(hours=1, minutes=40), estimated_arrival=now + timedelta(hours=3, minutes=55), status="DELAYED", delay_minutes=40, aircraft_type="Boeing 787-8 Dreamliner", passenger_count=248, terminal="Terminal 2", gate="Gate 42B", previous_delay_minutes=35, turnaround_minutes=68, confirmed_delay_reason="Air Traffic Control hold at BOM due to heavy rainfall and low cloud ceiling"),
        Flight(flight_number="6E-512", airline="IndiGo", departure_airport=bom, arrival_airport=blr, scheduled_departure=now + timedelta(hours=2), scheduled_arrival=now + timedelta(hours=3, minutes=40), status="ON_TIME", delay_minutes=0, aircraft_type="Airbus A321neo", passenger_count=212, terminal="Terminal 1", gate="Gate 18", previous_delay_minutes=0, turnaround_minutes=40),
        Flight(flight_number="UK-981", airline="Vistara", departure_airport=blr, arrival_airport=delhi, scheduled_departure=now + timedelta(hours=4), scheduled_arrival=now + timedelta(hours=6, minutes=45), status="ON_TIME", delay_minutes=0, aircraft_type="Airbus A320neo", passenger_count=164, terminal="Terminal 2", gate="Gate 22", previous_delay_minutes=0, turnaround_minutes=45),
        Flight(flight_number="EK-501", airline="Emirates", departure_airport=bom, arrival_airport=dxb, scheduled_departure=now + timedelta(hours=5), scheduled_arrival=now + timedelta(hours=8, minutes=20), status="ON_TIME", delay_minutes=0, aircraft_type="Boeing 777-300ER", passenger_count=354, terminal="Terminal 2", gate="Gate 56", previous_delay_minutes=10, turnaround_minutes=55),
        Flight(flight_number="BA-138", airline="British Airways", departure_airport=lhr, arrival_airport=bom, scheduled_departure=now - timedelta(hours=4), scheduled_arrival=now + timedelta(hours=5, minutes=30), estimated_arrival=now + timedelta(hours=6, minutes=15), status="DELAYED", delay_minutes=45, aircraft_type="Boeing 787-9", passenger_count=216, terminal="Terminal 2", gate="Gate 44", previous_delay_minutes=40, turnaround_minutes=75, confirmed_delay_reason="Delayed departure slot ex-Heathrow due to European airspace flow restrictions"),
        Flight(flight_number="SQ-422", airline="Singapore Airlines", departure_airport=sin, arrival_airport=bom, scheduled_departure=now - timedelta(hours=2), scheduled_arrival=now + timedelta(hours=3, minutes=10), status="ON_TIME", delay_minutes=0, aircraft_type="Airbus A350-900", passenger_count=280, terminal="Terminal 2", gate="Gate 36"),
    ]
    db.session.add_all(flights)
    db.session.flush()

    for flight in flights:
        result = predict_flight_delay(
            flight.previous_delay_minutes or 0,
            flight.turnaround_minutes or 45,
            flight.passenger_count or 180,
            0.72 if flight.arrival_airport.code == "BOM" else 0.15,
            flight.scheduled_departure,
            0.4,
        )
        db.session.add(FlightPredictionRecord(
            flight=flight,
            delay_probability=result.probability,
            expected_delay_minutes=result.expected_delay_minutes,
            risk_level=result.risk_level,
            contributing_factors="; ".join(result.contributing_factors),
            weather_risk_score=result.weather_risk_score,
            congestion_risk_score=result.congestion_risk_score,
            demo_data=True,
        ))

    operator = TaxiOperator(
        user=operator_user,
        company_name="AeroCabs Airport Ground Fleet Ltd.",
        license_number="MH-TX-2024-889",
        contact_phone="+91 98200 33003",
        fleet_size=20,
    )
    taxis = [
        Taxi(operator=operator, vehicle_number="MH-02-CB-4019", model="Toyota Camry Hybrid", taxi_type="SEDAN", passenger_capacity=4, luggage_capacity=3, base_fare_per_km=24.0, status="AVAILABLE", current_terminal="Terminal 2"),
        Taxi(operator=operator, vehicle_number="MH-02-ER-9011", model="Hyundai Ioniq 5 EV", taxi_type="ELECTRIC", passenger_capacity=4, luggage_capacity=3, base_fare_per_km=28.0, status="AVAILABLE", current_terminal="Terminal 2"),
        Taxi(operator=operator, vehicle_number="MH-02-XU-7722", model="Toyota Innova Crysta", taxi_type="SUV", passenger_capacity=6, luggage_capacity=5, base_fare_per_km=34.0, status="AVAILABLE", current_terminal="Terminal 2"),
        Taxi(operator=operator, vehicle_number="MH-02-BM-1100", model="Mercedes-Benz E-Class", taxi_type="PREMIUM", passenger_capacity=4, luggage_capacity=3, base_fare_per_km=52.0, status="AVAILABLE", current_terminal="Terminal 2"),
        Taxi(operator=operator, vehicle_number="MH-02-VN-3388", model="Force Urbania Luxury Van", taxi_type="VAN", passenger_capacity=9, luggage_capacity=8, base_fare_per_km=45.0, status="AVAILABLE", current_terminal="Terminal 1"),
    ]
    db.session.add_all(taxis)
    db.session.flush()
    sedan, electric = taxis[0], taxis[1]

    for index in range(8):
        slot_start = now.replace(minute=0, second=0, microsecond=0) + timedelta(hours=index)
        demand = 95 + index * 12 + (25 if index % 2 == 0 else 5)
        available = 75
        reserved = 28 + index * 3
        unmet = max(0, demand - (available - reserved))
        db.session.add(TaxiDemandForecast(
            airport=bom,
            terminal="Terminal 2",
            time_slot_start=slot_start,
            time_slot_end=slot_start + timedelta(hours=1),
            predicted_demand=demand,
            available_taxis=available,
            reserved_taxis=reserved,
            unmet_demand=unmet,
            demand_level="HIGH" if unmet > 20 else "MEDIUM" if unmet > 5 else "LOW",
            recommended_vehicles=math.ceil(unmet * 1.15) if unmet > 0 else 0,
            demo_data=True,
        ))

    first_booking = TaxiBooking(
        booking_reference="AW-TX-84912", user=passenger, flight=flights[0], taxi=sedan,
        requested_taxi_type="SEDAN", pickup_terminal="Terminal 2",
        destination="Bandra Kurla Complex (BKC), Mumbai", passenger_count=2,
        luggage_count=2, pickup_time=flights[0].effective_arrival + timedelta(minutes=35),
        estimated_fare=780.0, booking_status="RESCHEDULE_REQUIRED",
        flight_delayed_flag=True,
        flight_delay_notice="Flight AI-245 delayed +40 min due to weather. Please confirm your updated pickup slot.",
        notes="Meet at Pillar 14 outside Terminal 2 Arrivals",
    )
    second_booking = TaxiBooking(
        booking_reference="AW-TX-52019", user=passenger, flight=flights[5], taxi=electric,
        requested_taxi_type="ELECTRIC", pickup_terminal="Terminal 2",
        destination="Powai Lake Tech Park, Mumbai", passenger_count=1,
        luggage_count=1, pickup_time=now + timedelta(hours=4), estimated_fare=620.0,
        booking_status="CONFIRMED", notes="Prefer quiet EV transfer",
    )
    db.session.add_all([first_booking, second_booking])
    db.session.flush()

    _add_complaint(passenger, "AW-CP-39184", "Severe Baggage Delay on Arrival Belt 4", "Waited over two hours for luggage at Carousel 4 after landing. No ground staff were present to provide updates and several suitcases appeared damaged.", "Baggage Problems", bom, "Terminal 2", "AI-245")
    _add_complaint(second_passenger, "AW-CP-48210", "Emergency: Water Spill & Broken Glass Near Gate 42", "Hazardous wet spill and broken glass unattended directly in front of boarding Gate 42. Risk of slip, injury, and passenger fall.", "Safety Concerns", bom, "Terminal 2", "AI-245")
    _add_complaint(passenger, "AW-CP-11928", "Rude Taxi Driver and Overcharging Attempt", "Taxi driver at Terminal 2 pre-paid stand refused to use meter and demanded cash above standard airport fare tariff.", "Taxi or Transportation", bom, "Terminal 2", booking_reference="AW-TX-84912")
    _add_complaint(second_passenger, "AW-CP-77103", "Long Security Queue and Closed Screening Lanes", "Only two security checkpoint lanes were active during evening peak hour. Huge line with over 45 minutes wait causing missed flights.", "Security", delhi, "Terminal 3", "UK-981")

    db.session.add_all([
        Alert(title="Heavy Monsoonal Rainfall & High Winds at BOM", message="Gusts up to 54 km/h and reduced visibility (3.2 km). Expect aircraft holding delays and runway pacing.", severity="WARNING", category="WEATHER", airport=bom),
        Alert(title="Taxi Peak Shortage Forecast: 19:00 - 21:00", message="High arrival concentration at Terminal 2. Estimated unmet taxi demand of 35 vehicles. Taxi operators advised to mobilize additional fleet.", severity="WARNING", category="TAXI", airport=bom),
        Notification(user=passenger, title="Flight AI-245 Delay Alert", message="Your flight AI-245 is delayed by 40 minutes due to weather congestion at BOM. Your taxi pickup time has been flagged for rescheduling.", type="FLIGHT", link_url=f"/flights/{flights[0].id}"),
        Notification(user=passenger, title="Taxi Booking Action Required", message="Booking AW-TX-84912 requires your confirmation for the updated landing time.", type="TAXI", link_url="/taxis/bookings/AW-TX-84912"),
    ])
    db.session.commit()
    return True