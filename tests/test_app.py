from datetime import datetime, timedelta
import html
import json
from io import BytesIO
import re
import os
import unittest

from app import create_app
from models import Alert, Airport, Complaint, Notification, Taxi, TaxiBooking, User, db


class AirwiseWorkflowTests(unittest.TestCase):
    def setUp(self):
        self.demo_passwords = {
            "AIRWISE_DEMO_ADMIN_PASSWORD": "admin123",
            "AIRWISE_DEMO_PASSENGER_PASSWORD": "pass123",
            "AIRWISE_DEMO_OPERATOR_PASSWORD": "operator123",
            "AIRWISE_DEMO_PRIYA_PASSWORD": "pass123",
        }
        for key, password in self.demo_passwords.items():
            os.environ[key] = password
        self.app = create_app({
            "TESTING": True,
            "WTF_CSRF_ENABLED": False,
            "SQLALCHEMY_DATABASE_URI": "sqlite://",
            "AIRWISE_DEMO_MODE": True,
        })
        self.context = self.app.app_context()
        self.context.push()
        self.client = self.app.test_client()

    def tearDown(self):
        db.session.remove()
        db.drop_all()
        db.engine.dispose()
        self.context.pop()
        for key in self.demo_passwords:
            os.environ.pop(key, None)

    def sign_in(self, username, password):
        return self.client.post(
            "/login",
            data={"username": username, "password": password},
            follow_redirects=True,
        )

    def test_public_and_legacy_console_pages_render(self):
        for path in ("/", "/flights/search", "/flights/1", "/weather", "/taxis/demand", "/console"):
            with self.subTest(path=path):
                response = self.client.get(path, follow_redirects=True)
                self.assertEqual(response.status_code, 200)

    def test_chart_json_attributes_survive_html_parsing(self):
        console = self.client.get("/console").get_data(as_text=True)
        labels_attribute = re.search(r'data-labels="([^"]+)"', console).group(1)
        labels = json.loads(html.unescape(labels_attribute))
        self.assertIn("Air India", labels)

        taxi_page = self.client.get("/taxis/demand").get_data(as_text=True)
        series_attribute = re.search(r'data-series="([^"]+)"', taxi_page).group(1)
        series = json.loads(html.unescape(series_attribute))
        self.assertEqual([item["label"] for item in series], [
            "Predicted demand", "Available taxi fleet", "Reserved bookings",
        ])

    def test_passenger_booking_assigns_vehicle_and_notifies(self):
        self.sign_in("passenger", "pass123")
        pickup = (datetime.now() + timedelta(hours=2)).strftime("%Y-%m-%dT%H:%M")
        response = self.client.post("/taxis/book", data={
            "taxiType": "SEDAN",
            "terminal": "Terminal 2",
            "destination": "Bandra Kurla Complex, Mumbai",
            "passengers": "2",
            "luggage": "2",
            "pickupTime": pickup,
            "notes": "Meet at arrivals",
        }, follow_redirects=True)
        self.assertEqual(response.status_code, 200)
        booking = TaxiBooking.query.filter_by(destination="Bandra Kurla Complex, Mumbai").one()
        self.assertEqual(booking.booking_status, "DRIVER_ASSIGNED")
        self.assertIsNotNone(booking.taxi_id)
        self.assertGreater(booking.estimated_fare, 0)
        self.assertTrue(Notification.query.filter_by(user_id=booking.user_id, type="TAXI").count() >= 2)

    def test_capacity_limit_rejects_an_overbooked_window(self):
        self.sign_in("passenger", "pass123")
        passenger = User.query.filter_by(username="passenger").one()
        pickup_time = datetime.now() + timedelta(days=2)
        for index in range(5):
            db.session.add(TaxiBooking(
                booking_reference=f"AW-TX-TEST{index:04d}",
                user=passenger,
                requested_taxi_type="SEDAN",
                pickup_terminal="Terminal 2",
                destination=f"Capacity test destination {index}",
                passenger_count=1,
                luggage_count=1,
                pickup_time=pickup_time,
                estimated_fare=500,
                booking_status="CONFIRMED",
            ))
        db.session.commit()
        response = self.client.post("/taxis/book", data={
            "taxiType": "SEDAN",
            "terminal": "Terminal 2",
            "destination": "Overflow test",
            "passengers": "1",
            "luggage": "1",
            "pickupTime": pickup_time.strftime("%Y-%m-%dT%H:%M"),
        }, follow_redirects=True)
        self.assertEqual(response.status_code, 200)
        self.assertIn(b"Capacity limit reached", response.data)
        self.assertIsNone(TaxiBooking.query.filter_by(destination="Overflow test").first())

    def test_flight_delay_flags_linked_taxi_booking_and_notifies(self):
        self.sign_in("admin", "admin123")
        response = self.client.post("/admin/flights/1/status", data={
            "status": "DELAYED",
            "delayMinutes": "60",
            "confirmedReason": "Weather hold",
        }, follow_redirects=True)
        self.assertEqual(response.status_code, 200)
        booking = TaxiBooking.query.filter_by(booking_reference="AW-TX-84912").one()
        self.assertEqual(booking.booking_status, "RESCHEDULE_REQUIRED")
        self.assertTrue(booking.flight_delayed_flag)
        self.assertTrue(Notification.query.filter_by(user_id=booking.user_id, type="FLIGHT").count() >= 1)

    def test_passenger_can_reschedule_then_cancel_owned_booking(self):
        self.sign_in("passenger", "pass123")
        booking = TaxiBooking.query.filter_by(booking_reference="AW-TX-84912").one()
        pickup = (datetime.now() + timedelta(hours=5)).strftime("%Y-%m-%dT%H:%M")
        response = self.client.post(
            f"/taxis/bookings/{booking.id}/reschedule",
            data={"newPickupTime": pickup},
            follow_redirects=True,
        )
        self.assertEqual(response.status_code, 200)
        db.session.refresh(booking)
        self.assertEqual(booking.booking_status, "CONFIRMED")
        self.assertFalse(booking.flight_delayed_flag)
        response = self.client.post(
            f"/taxis/bookings/{booking.id}/cancel", follow_redirects=True
        )
        self.assertEqual(response.status_code, 200)
        db.session.refresh(booking)
        self.assertEqual(booking.booking_status, "CANCELLED")

    def test_urgent_complaint_creates_analysis_alert_and_notification(self):
        self.sign_in("passenger", "pass123")
        airport = Airport.query.filter_by(code="BOM").one()
        response = self.client.post("/complaints/submit", data={
            "title": "Emergency: broken glass hazard",
            "description": "There is dangerous broken glass and an injury risk near the gate.",
            "category": "Other",
            "airportId": str(airport.id),
            "terminal": "Terminal 2",
            "flightNumber": "AI-245",
            "bookingReference": "",
            "preferredContact": "Email",
            "attachment": (BytesIO(b"sample evidence"), "hazard photo.png"),
        }, content_type="multipart/form-data", follow_redirects=True)
        self.assertEqual(response.status_code, 200)
        complaint = Complaint.query.filter_by(title="Emergency: broken glass hazard").one()
        self.assertEqual(complaint.priority, "URGENT")
        self.assertEqual(complaint.attachment_name, "hazard_photo.png")
        self.assertTrue(complaint.analysis.urgent_safety)
        self.assertTrue(Alert.query.filter(Alert.title.contains(complaint.reference_no)).count() >= 1)
        self.assertTrue(Notification.query.filter_by(user_id=complaint.user_id, type="COMPLAINT").count() >= 1)

    def test_admin_dispatch_updates_complaint_and_notifies_passenger(self):
        self.sign_in("admin", "admin123")
        complaint = Complaint.query.filter_by(reference_no="AW-CP-39184").one()
        response = self.client.post(
            f"/admin/complaints/{complaint.id}/status",
            data={
                "status": "IN_PROGRESS",
                "department": "Baggage Handling",
                "resolutionNotes": "Ground team is checking carousel 4.",
            },
            follow_redirects=True,
        )
        self.assertEqual(response.status_code, 200)
        db.session.refresh(complaint)
        self.assertEqual(complaint.status, "IN_PROGRESS")
        self.assertEqual(complaint.assigned_department, "Baggage Handling")
        self.assertTrue(Notification.query.filter_by(user_id=complaint.user_id, type="COMPLAINT").count() >= 1)

    def test_operator_updates_vehicle_and_dispatch_status(self):
        self.sign_in("operator", "operator123")
        taxi = Taxi.query.first()
        response = self.client.post(
            f"/operator/taxis/{taxi.id}/status",
            data={"status": "MAINTENANCE", "terminal": "Terminal 1"},
            follow_redirects=True,
        )
        self.assertEqual(response.status_code, 200)
        db.session.refresh(taxi)
        self.assertEqual((taxi.status, taxi.current_terminal), ("MAINTENANCE", "Terminal 1"))
        booking = TaxiBooking.query.filter_by(booking_reference="AW-TX-52019").one()
        response = self.client.post(
            f"/operator/bookings/{booking.id}/status",
            data={"status": "DRIVER_ASSIGNED"},
            follow_redirects=True,
        )
        self.assertEqual(response.status_code, 200)
        db.session.refresh(booking)
        self.assertEqual(booking.booking_status, "DRIVER_ASSIGNED")

    def test_passenger_cannot_view_another_users_complaint(self):
        self.sign_in("passenger", "pass123")
        response = self.client.get("/complaints/view/AW-CP-48210")
        self.assertEqual(response.status_code, 403)
        self.assertEqual(self.client.get("/admin/dashboard").status_code, 403)

    def test_registration_persists_hashed_password(self):
        response = self.client.post("/register", data={
            "username": "new.passenger@example.test",
            "password": "securepass123",
            "confirmPassword": "securepass123",
            "fullName": "New Passenger",
            "phone": "+1 555 0100",
            "role": "ROLE_PASSENGER",
        }, follow_redirects=True)
        self.assertEqual(response.status_code, 200)
        user = User.query.filter_by(username="new.passenger@example.test").one()
        self.assertNotEqual(user.password, "securepass123")
        self.assertEqual(user.role, "ROLE_PASSENGER")


if __name__ == "__main__":
    unittest.main()