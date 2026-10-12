# AIRWISE – AI-Powered Smart Airport Operations & Passenger Intelligence System

A **Python Flask web application** for airport operations, passenger services, taxi booking, complaint tracking, and operational analytics. The original Spring Boot and JavaFX code remains in `src/main/java` and `legacy_javafx` as a migration reference; AIRWISE now runs from `app.py`.

> **Important Honesty Notice & Transparency Disclaimer**  
> All data shown and generated in this project is **demonstration / simulated data**. The system uses deterministic rule-based algorithms, weighted factor scoring, and keyword tokenization for baseline analytics and forecasts. It is **not** connected to a live airport system or trained machine-learning model.

---

## 1. Problem Statement & Objectives

Modern airport operations managers must synthesize metrics across multiple disparate domains: flight scheduling, weather disruptions, terminal congestion, ground transportation, medical readiness, and passenger sentiment. 

**AIRWISE** addresses this challenge by providing:
* A browser-based Flask application with role-based passenger, airport-admin, and taxi-operator workflows.
* Instant, understandable prediction models for delay probabilities, queue times, taxi shortages, and weather disruption risks.
* Transparent NLP complaint scoring and classification using Python keyword and token processing.
* Dynamic alert generation and action recommendations based on operational threshold evaluations.

---

## 2. System Features & Modules

| Module Name | Key Functions & Calculations |
| :--- | :--- |
| **Dashboard Overview** | KPI metric cards (flights, passengers, taxi demand, medical response, customer sentiment), system status header with real-time clock, quick alerts and recommendation summaries. |
| **✈ Flight Delay Prediction & Analysis** | Evaluates previous delay duration, aircraft turnaround time, passenger volume, weather risk, and peak-hour factors to compute delay probability, expected delay duration, and risk level. Displays delay distributions by airline, severity, and hour. |
| **🌦 Weather-Based Disruption Prediction** | Assesses rainfall, wind speed, visibility, temperature, and thunderstorm indicators to compute disruption probability and operational impacts. Displays rainfall/visibility impact charts and wind risk distributions. |
| **👥 Passenger Crowd & Queue Prediction** | Forecasts total terminal passengers, security queue minutes, check-in queue minutes, and recommended additional security lanes based on flight movements, operating hours, and active service counters. |
| **🚕 Taxi Demand Prediction** | Predicts ground transport demand, available taxi shortages, demand risk level, and estimated passenger wait times. Includes landside demand vs. supply trend charts. |
| **🏥 Medical Emergency Response Analytics** | Records and analyzes incident locations, emergency types, response times, medical team availability, and ambulance readiness. Computes required medical staff and ambulances. |
| **🤖 AI Customer Complaint Analyzer** | Classifies passenger reports into 10 operational categories (Safety Concerns, Accessibility, Flight Delay, Baggage Problems, Taxi or Transportation, Security, Cleanliness, Staff Behavior, Food Services, Other), calculates sentiment, and assigns LOW/MEDIUM/HIGH/URGENT priority. The legacy console retains its original smaller classifier and CRITICAL label. |
| **▥ Airport Analytics** | Cross-domain visualization suite with bar charts, line graphs, and pie charts summarizing airport performance. |
| **! Centralized Alerts** | Dynamic alerts raised automatically when operational metrics breach key risk thresholds. |
| **✓ Recommendation Engine** | Actionable operational suggestions derived directly from calculated crowd, transport, medical, weather, and flight risks. |

---

## 3. System Architecture & Project Structure

The active application uses Flask, Jinja templates, SQLAlchemy, Flask-Login, and BCrypt-compatible password hashing. Existing MySQL tables and names are retained for persistent migration; a durable SQLite file under `instance/` is used when no database URL is configured. Demo seed records are opt-in by deployment configuration. The JavaFX dashboard is a browser-based operations console in Flask, and both Java prediction formula families are retained separately.

```text
AIRWISE/
├── app.py                 # Flask routes, role checks, sessions, local startup
├── models.py              # SQLAlchemy mappings for the original relational tables
├── calculations.py        # Both Java prediction engines and complaint analyzers
├── legacy_data.py         # Java-compatible seeded sample data and console analytics
├── seed_data.py           # Optional web demo accounts and operational records
├── requirements.txt
├── render.yaml
├── templates/             # Responsive Jinja views for passenger, admin, operator, console
├── static/css/             # AIRWISE responsive design system
├── static/js/              # Navigation and charts
├── data/                   # Original CSV sample files, retained unchanged
├── src/main/java/          # Java source kept as conversion comparison reference
├── legacy_javafx/          # JavaFX source kept as conversion comparison reference
├── README.md
└── DEPLOYMENT.md           # Local, MySQL, and Render instructions
```

---

## 4. How Predictions and NLP Work

### Prediction Algorithms
The Python implementation in [`calculations.py`](calculations.py) preserves the active web calculations and the separate legacy JavaFX calculations. These formula sets intentionally remain distinct because their thresholds differ.

* **Active web flight delay:** base $0.08$, inbound delay capped at $0.28$, turnaround impact capped at $0.20$, passenger impact above 200 capped at $0.12$, weather weight $0.30$, peak-hour increment $0.12$, and congestion weight above threshold. Probability is clamped to $[0.03,0.97]$; expected delay is $8 + 52p$ minutes.
* **Legacy console flight delay:** preserves its separate passenger threshold of 220 and denominator 1800, boolean peak-hour input, and legacy risk labels.

* **Weather disruption:** the active web formula and legacy console formula retain their separate rainfall, wind, visibility, temperature, and thunderstorm weights and probability bounds.

* **Passenger Crowd & Security Queue:** The active web engine estimates expected flow from flight movements and peak hours, then calculates security/check-in queues and additional lanes when queue time exceeds 15 minutes. The legacy console keeps its separate passenger + movement + peak + holiday equation, counter/lane load levels, and its original extra-lane threshold above 20 minutes.

### Complaint Analysis
The active-web analyzer preserves category-keyword matching, sentiment lexicons, urgency and safety terms, extracted keywords, recommended department, and the LOW/MEDIUM/HIGH/URGENT priority matrix. The legacy console analyzer retains its smaller category map and LOW/MEDIUM/HIGH/CRITICAL matrix. Both are transparent rules, not trained models.

---

## 5. How to Run the Project

### Run locally
Requirements: Python 3.13 recommended.

```powershell
py -3.13 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
Copy-Item .env.example .env
python app.py
```

Open `http://localhost:5000`. Without `DATABASE_URL` or `DB_URL`, the application persists to `instance/airwise.db`. Demo seeding is off by default. For local demo data, set `AIRWISE_DEMO_MODE=true` and configure your own values for `AIRWISE_DEMO_ADMIN_PASSWORD`, `AIRWISE_DEMO_PASSENGER_PASSWORD`, `AIRWISE_DEMO_OPERATOR_PASSWORD`, and `AIRWISE_DEMO_PRIYA_PASSWORD` in the ignored `.env` file before startup. Demo usernames are `admin`, `passenger`, `operator`, and `priya`; no password defaults are provided. Never enable demo mode on a public deployment.

To use the existing MySQL database, set `DATABASE_URL=mysql+pymysql://user:password@host:3306/airwise?charset=utf8mb4`, or set the Java-compatible `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Keep credentials in environment variables or ignored `.env`, not in source control. See [`DEPLOYMENT.md`](DEPLOYMENT.md) for Render.

## Feature preservation checklist

| Original feature | Flask conversion |
| --- | --- |
| Passenger, admin, and taxi-operator login / role access | Flask-Login sessions and role-protected routes |
| Registration, profile updates, BCrypt-compatible passwords | Flask forms and BCrypt hashes |
| Flight search filters and flight detail analysis | `/flights/search`, `/flights/<id>` |
| Delay probabilities and contributing factors | Active Java web formula retained in Python |
| Weather dashboard and disruption scoring | `/weather` and separate active/legacy formulas |
| Passenger crowd and queue calculator | Operations console calculator and charts |
| Taxi demand, shortage, wait, forecast chart | `/taxis/demand` and console calculator |
| Advance reservations, capacity checks, fare estimate, vehicle assignment | Passenger taxi booking and confirmation pass |
| Flight delay linked taxi rescheduling and notifications | Admin flight update triggers booking flags and notifications |
| Taxi operator fleet, status, and dispatch workflow | Operator dashboard, fleet inventory, booking actions |
| Complaint category, sentiment, priority, department, safety alerts | Submission, tracking, admin review, and analysis records |
| Complaint attachment filename | Filename metadata retained; durable file storage is not provided |
| Passenger notifications and read state | Notification list, mark-read, mark-all-read |
| JavaFX medical incident analytics | Web console with sample records, response metrics, and resource calculator |
| Cross-domain analytics, alerts, recommendations, settings | Web operations console with charts and summaries |
| CSV sample datasets | Original files retained; JavaFX-compatible seeded dataset remains generated in code |

Automated coverage is in `tests/`. Java remains in the repository for comparison; the Flask app is the new run/deployment target.

---

## 6. Sample Visual Screenshots Description

* **Responsive browser dashboard:** Flask/Jinja pages share AIRWISE navigation and styling.
* **Passenger and operator workflows:** Persistent SQLAlchemy records back login, bookings, complaints, and role dashboards.
* **Operations console:** The former JavaFX calculators, medical records, charts, alerts, and recommendations are available in the browser.

---

## 7. Future Enhancements

1. **Controlled schema migrations:** Replace Hibernate `ddl-auto=update` with a reviewed migration tool/workflow before production.
2. **Real-Time Data Streaming:** Integrate an authorized airport/airline feed for live flight telemetry.
3. **Machine Learning Models:** Evaluate validated models to replace rule-based baseline heuristics.
4. **Durable complaint attachments:** Store uploads in managed object storage rather than the app's local filesystem.
