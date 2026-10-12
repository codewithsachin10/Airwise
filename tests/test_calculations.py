import unittest
from datetime import datetime

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
)
from legacy_data import JavaRandom


class CalculationParityTests(unittest.TestCase):
    def test_web_flight_delay_formula(self):
        result = predict_flight_delay(40, 68, 248, 0.72, datetime(2026, 10, 12, 13), 0.4)
        self.assertAlmostEqual(result.probability, 0.6482222222222223)
        self.assertEqual(result.expected_delay_minutes, 42)
        self.assertEqual(result.risk_level, "MEDIUM")

    def test_web_weather_caps_and_ground_stop_rule(self):
        result = evaluate_weather_disruption(38.2, 54, 3.2, 27.5, True)
        self.assertEqual(result.disruption_score, 0.99)
        self.assertEqual(result.disruption_level, "HIGH")
        self.assertTrue(result.ground_stop_risk)

    def test_legacy_flight_weather_crowd_taxi_and_medical_formulas(self):
        flight = legacy_predict_flight(42, 72, 268, 0.68, True)
        self.assertAlmostEqual(flight.probability, 0.7862222222222223)
        self.assertEqual((flight.expected_minutes, flight.risk), (49, "HIGH"))
        weather = legacy_predict_weather(18.5, 54, 3.2, 31, True)
        self.assertEqual((weather.probability, weather.risk), (0.98, "HIGH"))
        crowd = legacy_predict_crowd(1850, 16, 18, True, True, 14, 7)
        self.assertEqual(crowd.expected_passengers, 3182)
        taxi = legacy_predict_taxi(16, 1450, True, True, "Rain", 780)
        self.assertEqual((taxi.demand, taxi.shortage, taxi.wait_minutes), (1163, 383, 18))
        medical = legacy_predict_medical(30, True, True, True)
        self.assertEqual((medical.risk, medical.required_staff, medical.required_ambulances), ("HIGH", 5, 2))

    def test_active_and_legacy_complaint_classifiers_remain_distinct(self):
        active = analyze_complaint("Emergency: water spill", "Broken glass is a hazard")
        legacy = analyze_legacy_complaint("My baggage is lost")
        self.assertEqual((active.category, active.priority), ("Safety Concerns", "URGENT"))
        self.assertEqual((legacy["category"], legacy["sentiment"], legacy["priority"]), ("Baggage", "Negative", "Medium"))

    def test_legacy_random_generator_matches_java_seed(self):
        self.assertEqual(JavaRandom(245).next_int(75), 36)


if __name__ == "__main__":
    unittest.main()