from dataclasses import dataclass
from datetime import date, datetime, timedelta
import math

from calculations import (
    analyze_legacy_complaint,
    java_round,
    legacy_predict_crowd,
    legacy_predict_flight,
    legacy_predict_taxi,
    legacy_predict_weather,
)


class JavaRandom:
    multiplier = 0x5DEECE66D
    addend = 0xB
    mask = (1 << 48) - 1

    def __init__(self, seed):
        self.seed = (seed ^ self.multiplier) & self.mask

    def next_bits(self, bit_count):
        self.seed = (self.seed * self.multiplier + self.addend) & self.mask
        return self.seed >> (48 - bit_count)

    def next_int(self, bound):
        if bound <= 0:
            raise ValueError("bound must be positive")
        if bound & (bound - 1) == 0:
            return (bound * self.next_bits(31)) >> 31
        while True:
            bits = self.next_bits(31)
            value = bits % bound
            if bits - value + bound - 1 < 1 << 31:
                return value

    def next_double(self):
        return ((self.next_bits(26) << 27) + self.next_bits(27)) / float(1 << 53)


@dataclass
class FlightData:
    flight_number: str
    airline: str
    departure_airport: str
    arrival_airport: str
    scheduled_departure: datetime
    previous_delay_minutes: int
    turnaround_minutes: int
    passenger_count: int
    weather_risk: float
    status: str


@dataclass
class WeatherData:
    observation_date: date
    rainfall_mm: float
    wind_speed_kph: float
    visibility_km: float
    temperature_c: float
    thunderstorm: bool


@dataclass
class PassengerData:
    timestamp: datetime
    arrivals: int
    departures: int
    passenger_count: int
    active_counters: int
    security_lanes: int
    holiday: bool


@dataclass
class TaxiData:
    timestamp: datetime
    arrivals: int
    passenger_count: int
    available_taxis: int
    holiday: bool
    weather_condition: str


@dataclass
class MedicalEmergencyData:
    emergency_id: str
    location: str
    timestamp: datetime
    emergency_type: str
    response_minutes: int
    medical_team_available: bool
    ambulance_available: bool
    outcome: str


@dataclass
class ComplaintData:
    complaint_id: str
    text: str
    timestamp: datetime


class LegacyDataManager:
    def __init__(self, current_time=None):
        self.current_time = current_time or datetime.now()
        self.flights = []
        self.weather = []
        self.passengers = []
        self.taxis = []
        self.emergencies = []
        self.complaints = []
        self._generate_sample_data()

    def _generate_sample_data(self):
        random = JavaRandom(245)
        airlines = ["Air India", "IndiGo", "Vistara", "Akasa Air", "SpiceJet"]
        locations = ["Terminal 1", "Terminal 2", "Security", "Gate Area", "Baggage Hall"]
        emergency_types = ["Fainting", "Injury", "Cardiac Emergency", "Breathing Problem", "Accident", "Other"]
        complaint_texts = [
            "My baggage has not arrived and I have waited two hours.",
            "The flight was delayed and I missed my connection.",
            "The security queue was too long and slow.",
            "The restroom was dirty and needs cleaning.",
            "A staff member was rude when I asked for help.",
            "The food at the terminal was cold and poor.",
            "Parking was expensive and hard to find.",
            "The taxi shuttle was late. I waited a long time.",
            "Thank you, the staff were helpful and friendly.",
            "The check-in service was quick and excellent.",
        ]
        weather_conditions = ["Clear", "Rain", "Cloudy", "Storm"]
        outcomes = ["Treated", "Stabilized", "Transferred"]
        now = self.current_time

        for index in range(50):
            delay = random.next_int(75)
            scheduled_departure = now + timedelta(hours=index - 18)
            self.flights.append(FlightData(
                "AI" + str(210 + index),
                airlines[index % len(airlines)],
                "DEL",
                "BOM",
                scheduled_departure,
                delay,
                30 + random.next_int(75),
                120 + random.next_int(200),
                random.next_double() * 0.9,
                "Delayed" if delay > 45 else "On time",
            ))
            self.weather.append(WeatherData(
                (now.date() - timedelta(days=index)),
                random.next_double() * 35,
                5 + random.next_double() * 80,
                0.5 + random.next_double() * 8,
                18 + random.next_double() * 20,
                random.next_int(10) == 0,
            ))
            self.passengers.append(PassengerData(
                now - timedelta(hours=index * 2),
                3 + random.next_int(18),
                4 + random.next_int(20),
                400 + random.next_int(1800),
                8 + random.next_int(13),
                4 + random.next_int(9),
                random.next_int(7) == 0,
            ))
            self.taxis.append(TaxiData(
                now - timedelta(hours=index * 2),
                2 + random.next_int(20),
                300 + random.next_int(1500),
                250 + random.next_int(900),
                random.next_int(7) == 0,
                weather_conditions[random.next_int(len(weather_conditions))],
            ))
            self.complaints.append(ComplaintData(
                "C-" + str(1001 + index),
                complaint_texts[index % len(complaint_texts)],
                now - timedelta(days=random.next_int(30), hours=random.next_int(24)),
            ))

        for index in range(30):
            self.emergencies.append(MedicalEmergencyData(
                "M-" + str(501 + index),
                locations[index % len(locations)],
                now - timedelta(days=random.next_int(20), hours=random.next_int(24)),
                emergency_types[index % len(emergency_types)],
                3 + random.next_int(15),
                random.next_int(5) != 0,
                random.next_int(4) != 0,
                outcomes[random.next_int(len(outcomes))],
            ))


def _counts(items, key_function):
    counts = {}
    for item in items:
        key = key_function(item)
        counts[key] = counts.get(key, 0) + 1
    return counts


def _averages(items, key_function, value_function):
    totals = {}
    counts = {}
    for item in items:
        key = key_function(item)
        totals[key] = totals.get(key, 0) + value_function(item)
        counts[key] = counts.get(key, 0) + 1
    return {key: totals[key] / counts[key] for key in totals}


def legacy_analytics(data):
    complaint_categories = _counts(
        data.complaints,
        lambda complaint: analyze_legacy_complaint(complaint.text)["category"],
    )
    sentiment_counts = _counts(
        data.complaints,
        lambda complaint: analyze_legacy_complaint(complaint.text)["sentiment"],
    )
    priority_counts = _counts(
        data.complaints,
        lambda complaint: analyze_legacy_complaint(complaint.text)["priority"],
    )
    crowd_level_counts = _counts(
        data.passengers,
        lambda passenger: legacy_predict_crowd(
            passenger.passenger_count, passenger.arrivals, passenger.departures,
            False, passenger.holiday, passenger.active_counters, passenger.security_lanes,
        ).level,
    )
    response_time_counts = {
        "0–5 min": sum(emergency.response_minutes <= 5 for emergency in data.emergencies),
        "6–10 min": sum(5 < emergency.response_minutes <= 10 for emergency in data.emergencies),
        ">10 min": sum(emergency.response_minutes > 10 for emergency in data.emergencies),
    }

    def estimated_weather_delay(weather):
        return 5 + (40 - weather.visibility_km) * 1.4 + weather.wind_speed_kph * 0.18 + weather.rainfall_mm * 0.35 + (12 if weather.thunderstorm else 0)

    def average_or_zero(records, value_function):
        return sum(value_function(record) for record in records) / len(records) if records else 0

    rain_groups = {
        "Dry <5mm": [weather for weather in data.weather if weather.rainfall_mm < 5],
        "Rain 5–20mm": [weather for weather in data.weather if 5 <= weather.rainfall_mm < 20],
        "Heavy >20mm": [weather for weather in data.weather if weather.rainfall_mm >= 20],
    }
    visibility_groups = {
        "Low <2km": [weather for weather in data.weather if weather.visibility_km < 2],
        "Moderate 2–5km": [weather for weather in data.weather if 2 <= weather.visibility_km < 5],
        "Good ≥5km": [weather for weather in data.weather if weather.visibility_km >= 5],
    }
    wind_groups = {"<30 km/h": (0, 30), "30–60 km/h": (30, 60), ">60 km/h": (60, 200)}
    wind_disruption_rates = {}
    for label, (minimum, maximum) in wind_groups.items():
        observations = [weather for weather in data.weather if minimum <= weather.wind_speed_kph < maximum]
        wind_disruption_rates[label] = average_or_zero(
            observations,
            lambda weather: legacy_predict_weather(
                weather.rainfall_mm, weather.wind_speed_kph,
                weather.visibility_km, weather.temperature_c, weather.thunderstorm,
            ).probability * 100,
        )

    weather_risk_counts = _counts(
        data.weather,
        lambda weather: legacy_predict_weather(
            weather.rainfall_mm, weather.wind_speed_kph,
            weather.visibility_km, weather.temperature_c, weather.thunderstorm,
        ).risk,
    )
    passenger_samples = data.passengers[:12]
    taxi_samples = data.taxis[:12]
    return {
        "flights_by_airline": _counts(data.flights, lambda flight: flight.airline),
        "emergencies_by_location": _counts(data.emergencies, lambda emergency: emergency.location),
        "complaints_by_category": complaint_categories,
        "average_flight_delay": sum(f.previous_delay_minutes for f in data.flights) / max(1, len(data.flights)),
        "average_response_time": sum(e.response_minutes for e in data.emergencies) / max(1, len(data.emergencies)),
        "sentiment_counts": sentiment_counts,
        "priority_counts": priority_counts,
        "crowd_level_counts": crowd_level_counts,
        "emergency_type_counts": _counts(data.emergencies, lambda emergency: emergency.emergency_type),
        "response_time_counts": response_time_counts,
        "weather_risk_counts": weather_risk_counts,
        "weather_indicator_averages": {
            "Rainfall": average_or_zero(data.weather, lambda weather: weather.rainfall_mm),
            "Wind": average_or_zero(data.weather, lambda weather: weather.wind_speed_kph),
            "Visibility impact": average_or_zero(data.weather, lambda weather: 8 - weather.visibility_km),
        },
        "weather_rain_delays": {
            label: average_or_zero(records, estimated_weather_delay)
            for label, records in rain_groups.items()
        },
        "weather_visibility_delays": {
            label: average_or_zero(records, estimated_weather_delay)
            for label, records in visibility_groups.items()
        },
        "wind_disruption_rates": wind_disruption_rates,
        "passenger_trend_labels": [sample.timestamp.strftime("%H:%M") for sample in passenger_samples],
        "passenger_trend_values": [sample.passenger_count for sample in passenger_samples],
        "passenger_queue_values": [
            legacy_predict_crowd(
                sample.passenger_count, sample.arrivals, sample.departures,
                16 <= sample.timestamp.hour <= 20, sample.holiday,
                sample.active_counters, sample.security_lanes,
            ).security_queue_minutes
            for sample in passenger_samples
        ],
        "taxi_trend_labels": [sample.timestamp.strftime("%H:%M") for sample in taxi_samples],
        "taxi_supply_values": [sample.available_taxis for sample in taxi_samples],
        "taxi_demand_values": [
            legacy_predict_taxi(
                sample.arrivals, sample.passenger_count,
                16 <= sample.timestamp.hour <= 20, sample.holiday,
                sample.weather_condition, sample.available_taxis,
            ).demand
            for sample in taxi_samples
        ],
        "negative_complaints": sentiment_counts.get("Negative", 0),
        "high_priority_complaints": sum(
            analyze_legacy_complaint(c.text)["priority"] in ("High", "Critical")
            for c in data.complaints
        ),
        "negative_complaints_percent": java_round(
            sentiment_counts.get("Negative", 0) * 100.0 / max(1, len(data.complaints))
        ),
    }


def generate_legacy_alerts(data):
    alerts = []
    flight = data.flights[0]
    flight_risk = legacy_predict_flight(
        flight.previous_delay_minutes, flight.turnaround_minutes,
        flight.passenger_count, flight.weather_risk, True,
    )
    if flight_risk.probability >= 0.6:
        alerts.append({
            "title": "HIGH DELAY RISK",
            "message": f"Flight {flight.flight_number} has {round(flight_risk.probability * 100)}% delay probability.",
            "severity": "HIGH",
        })

    current_weather = data.weather[0]
    weather_risk = legacy_predict_weather(
        current_weather.rainfall_mm, current_weather.wind_speed_kph,
        current_weather.visibility_km, current_weather.temperature_c,
        current_weather.thunderstorm,
    )
    if weather_risk.probability >= 0.45:
        alerts.append({
            "title": "WEATHER ALERT",
            "message": "Low visibility or severe weather may disrupt operations.",
            "severity": weather_risk.risk,
        })

    passenger = data.passengers[0]
    crowd = legacy_predict_crowd(
        passenger.passenger_count, passenger.arrivals, passenger.departures,
        True, passenger.holiday, passenger.active_counters, passenger.security_lanes,
    )
    if crowd.security_queue_minutes > 20:
        alerts.append({
            "title": "CROWD ALERT",
            "message": f"Security queue expected to reach {crowd.security_queue_minutes} minutes.",
            "severity": crowd.level,
        })

    taxi = data.taxis[0]
    taxi_risk = legacy_predict_taxi(
        taxi.arrivals, taxi.passenger_count, True, taxi.holiday,
        taxi.weather_condition, taxi.available_taxis,
    )
    if taxi_risk.shortage > 0:
        alerts.append({
            "title": "TAXI SHORTAGE",
            "message": f"Expected demand exceeds supply by {taxi_risk.shortage} taxis.",
            "severity": taxi_risk.risk,
        })

    unavailable = sum(not emergency.ambulance_available for emergency in data.emergencies)
    if unavailable > 8:
        alerts.append({
            "title": "MEDICAL RESOURCE ALERT",
            "message": "Historical records indicate frequent ambulance unavailability.",
            "severity": "HIGH",
        })

    baggage_count = sum(
        analyze_legacy_complaint(complaint.text)["category"] == "Baggage"
        for complaint in data.complaints
    )
    if baggage_count >= 5:
        alerts.append({
            "title": "CUSTOMER SERVICE ALERT",
            "message": f"{baggage_count} baggage complaints detected in the demonstration dataset.",
            "severity": "MODERATE",
        })
    return alerts


def generate_legacy_recommendations(data):
    recommendations = []
    passenger = data.passengers[0]
    crowd = legacy_predict_crowd(
        passenger.passenger_count, passenger.arrivals, passenger.departures,
        True, passenger.holiday, passenger.active_counters, passenger.security_lanes,
    )
    if crowd.extra_lanes > 0:
        recommendations.append(
            f"Open {crowd.extra_lanes} additional security lane(s) to reduce the predicted {crowd.security_queue_minutes}-minute queue."
        )

    taxi = data.taxis[0]
    taxi_forecast = legacy_predict_taxi(
        taxi.arrivals, taxi.passenger_count, True, taxi.holiday,
        taxi.weather_condition, taxi.available_taxis,
    )
    if taxi_forecast.shortage > 0:
        recommendations.append(f"Arrange {taxi_forecast.shortage} additional taxis for the predicted arrival demand.")

    team_gaps = sum(not emergency.medical_team_available for emergency in data.emergencies)
    if team_gaps > 5:
        recommendations.append(f"Review medical shift coverage; {team_gaps} sampled incidents recorded a team availability gap.")

    weather = data.weather[0]
    weather_forecast = legacy_predict_weather(
        weather.rainfall_mm, weather.wind_speed_kph, weather.visibility_km,
        weather.temperature_c, weather.thunderstorm,
    )
    if weather_forecast.probability >= 0.4:
        recommendations.append(f"Prepare weather contingency procedures: {weather_forecast.impact.lower()}.")

    baggage_count = sum(
        analyze_legacy_complaint(complaint.text)["category"] == "Baggage"
        for complaint in data.complaints
    )
    if baggage_count > 0:
        recommendations.append(f"Prioritize baggage-service review; {baggage_count} baggage cases appear in the sample.")

    flight = data.flights[0]
    flight_forecast = legacy_predict_flight(
        flight.previous_delay_minutes, flight.turnaround_minutes,
        flight.passenger_count, flight.weather_risk, True,
    )
    if flight_forecast.probability >= 0.45:
        recommendations.append(
            f"Monitor {flight.flight_number} and similar high-risk departures; estimated risk is {round(flight_forecast.probability * 100)}%."
        )

    if not recommendations:
        recommendations.append("Continue routine monitoring; no major thresholds are currently exceeded.")
    return recommendations


def legacy_dashboard_metrics(data):
    analytics = legacy_analytics(data)
    passenger = data.passengers[0]
    crowd = legacy_predict_crowd(
        passenger.passenger_count, passenger.arrivals, passenger.departures,
        True, passenger.holiday, passenger.active_counters, passenger.security_lanes,
    )
    taxi = data.taxis[0]
    taxi_forecast = legacy_predict_taxi(
        taxi.arrivals, taxi.passenger_count, True, taxi.holiday,
        taxi.weather_condition, taxi.available_taxis,
    )
    today_emergencies = sum(e.timestamp.date() == data.current_time.date() for e in data.emergencies)
    return {
        "delayed_flights": sum(f.previous_delay_minutes >= 30 for f in data.flights),
        "cancelled_flights": sum(f.status == "Cancelled" for f in data.flights),
        "average_delay": analytics["average_flight_delay"],
        "expected_passengers": crowd.expected_passengers,
        "crowd_level": crowd.level,
        "security_queue_minutes": crowd.security_queue_minutes,
        "taxi_demand": taxi_forecast.demand,
        "available_taxis": taxi.available_taxis,
        "taxi_shortage": taxi_forecast.shortage,
        "taxi_risk": taxi_forecast.risk,
        "today_emergencies": today_emergencies,
        "average_response_time": analytics["average_response_time"],
        "medical_teams_available": sum(e.medical_team_available for e in data.emergencies),
        "complaint_count": len(data.complaints),
        "negative_complaints": analytics["negative_complaints"],
        "high_priority_complaints": analytics["high_priority_complaints"],
        "negative_complaints_percent": analytics["negative_complaints_percent"],
    }