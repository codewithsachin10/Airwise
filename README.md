# AIRWISE – AI-Powered Smart Airport Operations & Passenger Intelligence System

A complete **Java-only desktop application** built with **JavaFX** for centralized airport operational monitoring, baseline risk prediction, emergency response analytics, and passenger complaint analysis.

> **Important Honesty Notice & Transparency Disclaimer**  
> All data shown and generated in this project is **demonstration / simulated data**. The system uses deterministic rule-based algorithms, weighted factor scoring, and keyword tokenization for baseline analytics and forecasts. It is **not** connected to a live airport system or trained machine-learning model.

---

## 1. Problem Statement & Objectives

Modern airport operations managers must synthesize metrics across multiple disparate domains: flight scheduling, weather disruptions, terminal congestion, ground transportation, medical readiness, and passenger sentiment. 

**AIRWISE** addresses this challenge by providing:
* A single JavaFX desktop dashboard consolidating 6 primary airport operation modules.
* Instant, understandable prediction models for delay probabilities, queue times, taxi shortages, and weather disruption risks.
* Transparent NLP complaint scoring and classification using native Java text processing.
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
| **🤖 AI Customer Complaint Analyzer** | Classifies passenger feedback into 8 airport operational categories (Baggage, Flight Delay, Security, Cleanliness, Staff Behavior, Food, Parking, Transportation, Other), calculates sentiment (Positive, Neutral, Negative), and computes priority (Low, Medium, High, Critical). |
| **▥ Airport Analytics** | Cross-domain visualization suite with bar charts, line graphs, and pie charts summarizing airport performance. |
| **! Centralized Alerts** | Dynamic alerts raised automatically when operational metrics breach key risk thresholds. |
| **✓ Recommendation Engine** | Actionable operational suggestions derived directly from calculated crowd, transport, medical, weather, and flight risks. |

---

## 3. System Architecture & Project Structure

The project strictly follows a **100% Java-only** architecture. No HTML, CSS, JavaScript, Python, or external database engines are used.

```text
AIRWISE/
├── pom.xml
├── README.md
├── src/
│   ├── Main.java                        # JavaFX Application Entry Point
│   ├── Dashboard.java                   # Main UI Layout, Navigation & Navigation Bar
│   ├── Ui.java                          # UI Components, Form Grids, & JavaFX Chart Builders
│   ├── FlightDelayModule.java           # Flight Delay UI & Visualizations
│   ├── WeatherDisruptionModule.java     # Weather Disruption UI & Visualizations
│   ├── PassengerCrowdModule.java        # Passenger Crowd & Queue UI
│   ├── TaxiDemandModule.java            # Taxi Demand UI & Visualizations
│   ├── MedicalEmergencyModule.java      # Medical Analytics & Emergency Records Table
│   ├── ComplaintAnalyzerModule.java     # Complaint NLP Analyzer UI & Statistics
│   ├── FlightData.java                  # Flight Record Data Structure
│   ├── WeatherData.java                 # Weather Observation Data Structure
│   ├── PassengerData.java               # Passenger Volume & Capacity Data Structure
│   ├── TaxiData.java                    # Ground Transport Data Structure
│   ├── MedicalEmergencyData.java        # Emergency Incident Data Structure
│   ├── ComplaintData.java               # Customer Complaint Data Structure
│   ├── PredictionEngine.java            # Rule-Based Baseline Prediction Algorithms
│   ├── AnalyticsEngine.java             # Data Aggregation & Analytical Calculations
│   ├── ComplaintNLPAnalyzer.java        # Native Java Keyword NLP & Sentiment Engine
│   ├── AlertManager.java                # Dynamic Operational Alert Generator
│   ├── RecommendationEngine.java        # Operational Action Recommendation Generator
│   └── DataManager.java                 # Data Repository & Realistic Sample Data Generator
└── data/
    ├── flights.csv
    ├── weather.csv
    ├── passengers.csv
    ├── taxis.csv
    ├── emergencies.csv
    └── complaints.csv
```

---

## 4. How Predictions and NLP Work

### Prediction Algorithms
The system implements deterministic baseline prediction algorithms in [`PredictionEngine.java`](file:///c:/Users/Vanitha/Desktop/Airwise/src/PredictionEngine.java):

* **Flight Delay Risk:**  
  $$\text{Score} = 0.08 + \min\left(\frac{\text{Previous Delay}}{180}, 0.28\right) + \min\left(\frac{\text{Turnaround} - 50}{180}, 0.20\right) + \min\left(\frac{\text{Passenger Load} - 220}{1800}, 0.12\right) + (\text{Weather Risk} \times 0.30) + (\text{Peak Hour ? } 0.12 : 0)$$
  *Probability is bounded within $[0.03, 0.97]$. Expected delay is computed as $8 + (\text{Probability} \times 52)$ minutes.*

* **Weather Disruption Risk:**  
  $$\text{Score} = \min\left(\frac{\text{Rainfall}}{40}, 0.28\right) + \min\left(\frac{\text{Wind Speed}}{150}, 0.25\right) + \frac{8 - \text{Visibility}}{8} \times 0.32 + (\text{Thunderstorm ? } 0.30 : 0)$$

* **Passenger Crowd & Security Queue:**  
  Expected passengers are calculated from flight arrivals/departures and peak factors. Security queue length is derived using lane processing capacity (150 passengers/hour per lane). Extra lane recommendations are generated if queue time exceeds 20 minutes.

### Native Java Complaint Analysis
The text processing engine in [`ComplaintNLPAnalyzer.java`](file:///c:/Users/Vanitha/Desktop/Airwise/src/ComplaintNLPAnalyzer.java) operates via string normalization, tokenization, keyword matching, and sentiment scoring:
1. **Category Mapping:** Matches text tokens against predefined keyword dictionaries for Baggage, Flight Delay, Security, Cleanliness, Staff Behavior, Food, Parking, and Transportation.
2. **Sentiment Scoring:** Tokenizes input text using regex `[^a-z0-9']+`, scoring positive vs. negative sentiment terms.
3. **Priority Matrix:** Evaluates urgency keywords ("urgent", "two hours", "emergency") combined with negative sentiment to assign Low, Medium, High, or Critical priority.

---

## 5. How to Run the Project

### Prerequisites
1. **Java Development Kit (JDK 21 or later)** installed.
2. **VS Code** with the **Extension Pack for Java** installed.
3. **Maven** (or VS Code Java Project Manager).

### Running in VS Code
1. Open the workspace folder `AIRWISE` in VS Code (`File > Open Folder...`).
2. Open terminal in VS Code (`Ctrl + ~` or `Terminal > New Terminal`).
3. Set your `JAVA_HOME` if not already configured in your shell environment:
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Java\jdk-26.0.2"
   ```
4. Compile the project using Maven:
   ```powershell
   mvn clean compile
   ```
5. Run the JavaFX Application using Maven plugin:
   ```powershell
   mvn javafx:run
   ```
   *Alternative:* Open [`src/Main.java`](file:///c:/Users/Vanitha/Desktop/Airwise/src/Main.java) in VS Code editor and click **Run** above the `main` method.

---

## 6. Sample Visual Screenshots Description

* **Dashboard Header & Navigation Sidebar:** Features a dark navy navigation sidebar (`#10243A`) with search filtering, active page highlighting, live operational date/time, and status indicators.
* **KPI Metrics Row:** Clean card widgets displaying high-level totals, risk levels, average delays, and percentage statistics across all 6 modules.
* **Prediction Forms & Charts:** Interactive forms with responsive grid layouts, custom input fields, styled action buttons, immediate result cards, and JavaFX BarCharts/LineCharts/PieCharts.

---

## 7. Future Enhancements

1. **Database Integration:** Connect to PostgreSQL/MySQL via JDBC for persistent historical record management.
2. **Real-Time Data Streaming:** Integrate Apache Kafka or WebSockets to stream live airport flight telemetry.
3. **Machine Learning Models:** Implement Weka or Deeplearning4j Java libraries to replace rule-based baseline heuristics with trained regression/classification models.
