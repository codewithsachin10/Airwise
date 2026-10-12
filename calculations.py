from dataclasses import dataclass
from datetime import datetime
import math
import re


def clamp(value, minimum, maximum):
    return max(minimum, min(value, maximum))


def java_round(value):
    return math.floor(value + 0.5)


@dataclass
class FlightPrediction:
    probability: float
    expected_delay_minutes: int
    risk_level: str
    contributing_factors: list
    weather_risk_score: float
    congestion_risk_score: float


@dataclass
class PassengerFlowPrediction:
    expected_passengers: int
    security_queue_minutes: int
    checkin_queue_minutes: int
    recommended_lanes: int
    active_lanes: int
    congestion_level: str


@dataclass
class TaxiDemandPrediction:
    predicted_demand: int
    available_taxis: int
    reserved_taxis: int
    unmet_demand: int
    demand_level: str
    recommended_vehicles: int
    estimated_wait_minutes: int


@dataclass
class WeatherDisruptionPrediction:
    disruption_score: float
    disruption_level: str
    operational_impact: str
    ground_stop_risk: bool


def predict_flight_delay(previous_delay_minutes, turnaround_minutes, passenger_count,
                         weather_risk_score, departure_time=None, congestion_score=0.0):
    factors = []
    score = 0.08

    if previous_delay_minutes > 0:
        score += min(previous_delay_minutes / 180.0, 0.28)
        factors.append(f"Previous leg inbound delay (+{previous_delay_minutes} min)")

    if turnaround_minutes > 50:
        score += min((turnaround_minutes - 50) / 180.0, 0.20)
        factors.append(f"Extended aircraft turnaround ({turnaround_minutes} min vs standard 45 min)")

    if passenger_count > 200:
        score += min((passenger_count - 200) / 1600.0, 0.12)
        factors.append(f"High passenger load ({passenger_count} passengers)")

    bounded_weather = clamp(weather_risk_score, 0.0, 1.0)
    score += bounded_weather * 0.30
    if bounded_weather >= 0.35:
        factors.append(f"Elevated weather risk index ({java_round(bounded_weather * 100)}%)")

    hour = departure_time.hour if departure_time else 12
    if 7 <= hour <= 9 or 17 <= hour <= 20:
        score += 0.12
        factors.append("Peak-hour airport slot departure congestion")

    if congestion_score > 0.4:
        score += min(congestion_score * 0.15, 0.15)
        factors.append("Terminal passenger flow above nominal threshold")

    probability = clamp(score, 0.03, 0.97)
    risk_level = "HIGH" if probability >= 0.65 else "MEDIUM" if probability >= 0.35 else "LOW"
    if not factors:
        factors.append("Nominal operating parameters with low scheduled disruption probability")

    return FlightPrediction(
        probability,
        java_round(8 + probability * 52),
        risk_level,
        factors,
        bounded_weather,
        congestion_score,
    )


def predict_passenger_flow(flight_movements, active_security_lanes, current_time=None):
    hour = current_time.hour if current_time else 12
    peak = 6 <= hour <= 9 or 17 <= hour <= 21
    expected_passengers = max(300, flight_movements * 165 + (550 if peak else 80))
    active_lanes = max(1, active_security_lanes)
    security_queue = max(3, java_round(expected_passengers / (active_lanes * 130) * 25))
    checkin_queue = max(2, java_round(security_queue * 0.65))
    recommended_lanes = active_lanes
    if security_queue > 15:
        recommended_lanes += math.ceil((security_queue - 15) / 5.0)

    level = "CRITICAL" if security_queue >= 25 else "HIGH" if security_queue >= 18 else "MODERATE" if security_queue >= 10 else "LOW"
    return PassengerFlowPrediction(
        expected_passengers,
        security_queue,
        checkin_queue,
        recommended_lanes,
        active_lanes,
        level,
    )


def predict_taxi_demand(arrivals_count, arriving_passengers, slot_time=None,
                        weather_condition="Clear", available_taxis=0, existing_reservations=0):
    hour = slot_time.hour if slot_time else 14
    peak = 8 <= hour <= 10 or 18 <= hour <= 22
    condition = (weather_condition or "").lower()
    weather_factor = 1.25 if "storm" in condition or "thunder" in condition else 1.15 if "rain" in condition or "fog" in condition else 1.0
    raw_demand = (arrivals_count * 14.0 + arriving_passengers * 0.32 + (35.0 if peak else 5.0)) * weather_factor
    demand = java_round(max(10, raw_demand))
    effective_available = max(0, available_taxis)
    remaining_capacity = max(0, effective_available - existing_reservations)
    unmet = max(0, demand - remaining_capacity)
    level = "HIGH" if unmet > 40 or demand > 120 else "MEDIUM" if unmet > 10 or demand > 60 else "LOW"
    recommended = math.ceil(unmet * 1.1) if unmet > 0 else 0
    wait = 15 + min(unmet // 3, 30) if remaining_capacity <= 5 else 5
    return TaxiDemandPrediction(demand, available_taxis, existing_reservations, unmet, level, recommended, wait)


def evaluate_weather_disruption(rainfall_mm, wind_speed_kmh, visibility_km,
                                temperature_c, thunderstorm):
    rain_impact = min(max(rainfall_mm, 0.0) / 40.0, 0.30)
    wind_impact = min(max(wind_speed_kmh, 0.0) / 100.0, 0.28)
    visibility_impact = max(0.0, (10.0 - visibility_km) / 10.0) * 0.30
    temperature_impact = 0.08 if temperature_c < 2.0 or temperature_c > 42.0 else 0.0
    storm_impact = 0.35 if thunderstorm else 0.0
    score = clamp(rain_impact + wind_impact + visibility_impact + temperature_impact + storm_impact, 0.02, 0.99)

    if score >= 0.70:
        level = "HIGH"
        impact = "Severe disruption: high probability of holding patterns, runway reassignments, and flight cancellations"
        ground_stop = thunderstorm or visibility_km < 1.0 or wind_speed_kmh > 75.0
    elif score >= 0.40:
        level = "MEDIUM"
        impact = "Moderate disruption: extended taxi times, schedule padding, and minor arrival/departure delays"
        ground_stop = False
    else:
        level = "LOW"
        impact = "Optimal conditions: standard airport throughput and normal visual approach flight rules"
        ground_stop = False

    return WeatherDisruptionPrediction(score, level, impact, ground_stop)


@dataclass
class LegacyFlightPrediction:
    probability: float
    expected_minutes: int
    risk: str
    factors: list


@dataclass
class LegacyWeatherPrediction:
    probability: float
    risk: str
    impact: str


@dataclass
class LegacyCrowdPrediction:
    expected_passengers: int
    level: str
    security_queue_minutes: int
    checkin_queue_minutes: int
    extra_lanes: int


@dataclass
class LegacyTaxiPrediction:
    demand: int
    shortage: int
    wait_minutes: int
    risk: str


@dataclass
class MedicalPrediction:
    risk: str
    required_staff: int
    required_ambulances: int


def legacy_risk_for(probability):
    return "HIGH" if probability >= 0.75 else "MODERATE" if probability >= 0.45 else "LOW"


def legacy_predict_flight(previous_delay_minutes, turnaround_minutes, passenger_count,
                         weather_risk, peak_hour):
    factors = []
    score = 0.08
    if previous_delay_minutes > 0:
        score += min(previous_delay_minutes / 180.0, 0.28)
        factors.append(f"Previous delay: +{previous_delay_minutes} min")
    if turnaround_minutes > 50:
        score += min((turnaround_minutes - 50) / 180.0, 0.20)
        factors.append("Long aircraft turnaround")
    if passenger_count > 220:
        score += min((passenger_count - 220) / 1800.0, 0.12)
        factors.append("High passenger load")
    bounded_weather = clamp(weather_risk, 0, 1)
    score += bounded_weather * 0.30
    if bounded_weather >= 0.35:
        factors.append("Weather risk elevated")
    if peak_hour:
        score += 0.12
        factors.append("Peak-hour departure")
    probability = clamp(score, 0.03, 0.97)
    return LegacyFlightPrediction(
        probability,
        java_round(8 + probability * 52),
        legacy_risk_for(probability),
        factors or ["No major risk factors identified"],
    )


def legacy_predict_weather(rainfall_mm, wind_speed_kph, visibility_km,
                           temperature_c=22, thunderstorm=False):
    score = (
        min(max(rainfall_mm, 0) / 40.0, 0.28)
        + min(max(wind_speed_kph, 0) / 150.0, 0.25)
        + max(0, (8 - visibility_km) / 8.0) * 0.32
        + (0.06 if temperature_c < 0 or temperature_c > 40 else 0)
        + (0.30 if thunderstorm else 0)
    )
    probability = clamp(score, 0.02, 0.98)
    impact = "Flight delays and possible cancellations" if probability >= 0.7 else "Schedule padding and ground-operation monitoring" if probability >= 0.4 else "Limited operational impact expected"
    return LegacyWeatherPrediction(probability, legacy_risk_for(probability), impact)


def legacy_predict_crowd(passenger_count, arrivals, departures, peak_hour, holiday,
                         active_counters, security_lanes):
    expected = max(0, passenger_count + max(0, arrivals + departures) * 18
                   + (420 if peak_hour else 0) + (300 if holiday else 0))
    load = expected / max(1, active_counters * 75 + security_lanes * 120)
    level = "CRITICAL" if load >= 4.5 else "HIGH" if load >= 3 else "MODERATE" if load >= 1.8 else "LOW"
    security_queue = max(3, java_round(expected / max(1, security_lanes * 150)))
    checkin_queue = max(2, java_round(expected / max(1, active_counters * 115)))
    extra_lanes = max(0, (security_queue - 20 + 9) // 10)
    return LegacyCrowdPrediction(expected, level, security_queue, checkin_queue, extra_lanes)


def legacy_predict_taxi(arrivals, passengers, peak_hour, holiday, weather_condition, available_taxis):
    condition = (weather_condition or "").lower()
    weather_factor = 1.15 if condition == "rain" else 1.25 if condition == "storm" else 1.0
    demand = math.ceil(max(0, (arrivals * 12 + passengers * 0.42
                               + (120 if peak_hour else 0) + (90 if holiday else 0)) * weather_factor))
    shortage = max(0, demand - max(0, available_taxis))
    wait = 5 if shortage == 0 else 5 + math.ceil(shortage / 30.0)
    risk = "HIGH" if shortage > 250 else "MODERATE" if shortage > 0 else "LOW"
    return LegacyTaxiPrediction(demand, shortage, wait, risk)


def legacy_predict_medical(recent_incidents, peak_hour, team_available, ambulance_available):
    staff = max(2, math.ceil(recent_incidents / 8.0) + (1 if peak_hour else 0))
    ambulances = max(1, math.ceil(recent_incidents / 15.0))
    risk = "HIGH" if not team_available or not ambulance_available or recent_incidents >= 12 else "MODERATE" if recent_incidents >= 6 else "LOW"
    return MedicalPrediction(risk, staff, ambulances)


@dataclass
class ComplaintAnalysis:
    category: str
    sentiment: str
    priority: str
    extracted_keywords: str
    summary_reason: str
    recommended_department: str
    urgent_safety: bool


ACTIVE_CATEGORY_KEYWORDS = {
    "Safety Concerns": ("safety", "hazard", "danger", "dangerous", "fire", "smoke", "slip", "fall", "injury", "medical", "first aid", "security risk", "threat", "emergency", "spill", "wiring", "broken glass"),
    "Accessibility": ("wheelchair", "disabled", "disability", "accessibility", "elderly", "special assistance", "ramp", "elevator", "lift", "blind", "deaf", "hearing", "mobility", "escort"),
    "Flight Delay": ("flight", "delay", "delayed", "reschedule", "gate change", "boarding", "takeoff", "airline", "pilot", "missed flight", "connection", "cancelled", "overbooked"),
    "Baggage Problems": ("bag", "bags", "baggage", "luggage", "suitcase", "carousel", "lost bag", "damaged bag", "missing luggage", "tag", "belt", "lost property", "trolley"),
    "Taxi or Transportation": ("taxi", "cab", "uber", "driver", "transport", "shuttle", "bus", "fare", "overcharging", "pickup", "ride", "dropoff", "meter", "parking", "car park"),
    "Security": ("security", "screening", "tsa", "checkpoint", "frisking", "metal detector", "scanner", "pat down", "immigration", "passport control", "customs", "queue line"),
    "Cleanliness": ("clean", "dirty", "restroom", "washroom", "toilet", "hygiene", "trash", "garbage", "smell", "dust", "sanitizer", "foul", "filthy", "water on floor"),
    "Staff Behavior": ("staff", "agent", "employee", "officer", "rude", "attitude", "shouting", "unprofessional", "behavior", "ignored", "refused", "counter staff", "ground staff"),
    "Food Services": ("food", "restaurant", "cafe", "coffee", "meal", "water", "price", "expensive", "expired", "stale", "dining", "snack", "hygiene food", "lounge food"),
}

ACTIVE_DEPARTMENTS = {
    "Safety Concerns": "Airport Safety & Emergency Operations",
    "Accessibility": "Passenger Special Assistance Services",
    "Flight Delay": "Flight Operations & Terminal Dispatch",
    "Baggage Problems": "Baggage Handling & Lost Property",
    "Taxi or Transportation": "Landside Ground Transportation",
    "Security": "Aviation Security & Screening Division",
    "Cleanliness": "Terminal Facilities & Janitorial Services",
    "Staff Behavior": "Customer Experience & Staffing Relations",
    "Food Services": "Terminal Concessions & Food Services",
    "Other": "General Customer Relations",
}

SAFETY_KEYWORDS = ("fire", "smoke", "injury", "injured", "medical", "blood", "assault", "hazard", "threat", "emergency", "danger", "dangerous", "unconscious", "collapse", "stolen", "weapon")
URGENCY_KEYWORDS = ("urgent", "immediately", "emergency", "asap", "critical", "hours waiting", "stranded", "missed connection", "stuck", "two hours", "three hours")
NEGATIVE_WORDS = set("delay delayed late cancelled cancel lost broken damaged dirty disgusting rude horrible terrible worst unacceptable angry frustrated poor bad slow incompetent negligent smelly stolen missing unhelpful chaos crowded waste failed unhappy pain shouting complaint".split())
POSITIVE_WORDS = set("thank thanks helpful good great excellent kind prompt appreciate resolved polite quick clean wonderful satisfied courteous smooth".split())
TOKEN_SPLIT = re.compile(r"[^a-zA-Z0-9']+")


def analyze_complaint(title, description, user_category=None):
    full_text = f"{title or ''} {description or ''}".lower()
    tokens = TOKEN_SPLIT.split(full_text)
    matched = []
    urgent_safety = False
    has_urgency = False

    for keyword in SAFETY_KEYWORDS:
        if keyword in full_text:
            urgent_safety = True
            matched.append(keyword)
    for keyword in URGENCY_KEYWORDS:
        if keyword in full_text:
            has_urgency = True
            if keyword not in matched:
                matched.append(keyword)

    category = "Other"
    best_score = 0
    for name, keywords in ACTIVE_CATEGORY_KEYWORDS.items():
        score = sum(keyword in full_text for keyword in keywords)
        if score > best_score:
            best_score = score
            category = name
        for keyword in keywords:
            if keyword in full_text and keyword not in matched and len(matched) < 10:
                matched.append(keyword)

    if user_category and user_category.lower() != "other" and best_score < 2:
        category = user_category

    sentiment_score = sum(-1 if token in NEGATIVE_WORDS else 1 if token in POSITIVE_WORDS else 0 for token in tokens)
    sentiment = "NEGATIVE" if sentiment_score < 0 else "POSITIVE" if sentiment_score > 0 else "NEUTRAL"
    if urgent_safety:
        priority = "URGENT"
    elif has_urgency or sentiment == "NEGATIVE" and sentiment_score <= -3:
        priority = "HIGH"
    elif sentiment == "NEGATIVE" or has_urgency:
        priority = "MEDIUM"
    else:
        priority = "LOW"

    department = ACTIVE_DEPARTMENTS.get(category, "General Customer Relations")
    summary = (
        f"Classified under '{category}' based on {len(matched)} matched lexicon terms "
        f"(keywords: {', '.join(matched)}). Sentiment evaluated as {sentiment} "
        f"(score: {sentiment_score}). Priority set to {priority}."
    )
    extracted = ", ".join(matched) or "general, feedback"
    return ComplaintAnalysis(category, sentiment, priority, extracted, summary, department, urgent_safety)


LEGACY_CATEGORY_TERMS = {
    "Flight Delay": ("delay", "late", "cancel", "reschedule", "missed flight"),
    "Baggage": ("baggage", "bag", "luggage", "suitcase", "lost"),
    "Security": ("security", "screening", "checkpoint", "queue", "line"),
    "Cleanliness": ("dirty", "clean", "restroom", "toilet", "hygiene"),
    "Staff Behavior": ("staff", "rude", "employee", "service agent", "behavior"),
    "Food": ("food", "meal", "restaurant", "drink", "coffee"),
    "Parking": ("parking", "parked", "car park", "garage"),
    "Transportation": ("taxi", "transport", "shuttle", "bus", "ride"),
}
LEGACY_NEGATIVE = set("not never hasn't isn't didn't lost late rude bad poor angry unhappy missing dirty wait delay terrible wrong".split())
LEGACY_POSITIVE = set("thank thanks good great helpful excellent clean friendly quick appreciate resolved".split())


def analyze_legacy_complaint(text):
    normalized = (text or "").lower()
    category = "Other"
    best_score = 0
    for name, terms in LEGACY_CATEGORY_TERMS.items():
        score = sum(term in normalized for term in terms)
        if score > best_score:
            category = name
            best_score = score
    tokens = re.split(r"[^a-z0-9']+", normalized)
    sentiment_score = sum(-1 if token in LEGACY_NEGATIVE else 1 if token in LEGACY_POSITIVE else 0 for token in tokens)
    sentiment = "Negative" if sentiment_score < 0 else "Positive" if sentiment_score > 0 else "Neutral"
    urgency = (2 if "urgent" in normalized or "emergency" in normalized else 0)
    urgency += 1 if "two hours" in normalized or "hours" in normalized else 0
    urgency += 1 if sentiment == "Negative" else 0
    priority = "Critical" if urgency >= 3 else "High" if urgency >= 2 else "Medium" if urgency == 1 else "Low"
    return {
        "category": category,
        "sentiment": sentiment,
        "priority": priority,
        "explanation": "Keyword matching and token scoring (demonstration rules; not a trained AI model).",
    }