import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Dashboard {
    private final DataManager data = new DataManager();
    private final PredictionEngine predictor = new PredictionEngine();
    private final ComplaintNLPAnalyzer complaintAnalyzer = new ComplaintNLPAnalyzer();
    private final AnalyticsEngine analytics = new AnalyticsEngine();
    private final AlertManager alertManager = new AlertManager();
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();
    private final VBox navigation = new VBox(5);
    private final VBox content = new VBox();
    private final Label dateTime = new Label();
    private String activePage = "Dashboard";

    public Parent createView() {
        BorderPane frame = new BorderPane();
        frame.setStyle("-fx-background-color: " + Ui.BG + "; -fx-font-family: 'Segoe UI';");
        frame.setLeft(createSidebar());
        frame.setTop(createHeader());
        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setStyle("-fx-background: " + Ui.BG + "; -fx-background-color: " + Ui.BG + ";");
        frame.setCenter(scroller);
        show("Dashboard");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> dateTime.setText(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  ·  HH:mm:ss")))),
                new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        return frame;
    }

    private Node createSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPadding(new Insets(24, 14, 18, 14));
        sidebar.setPrefWidth(238);
        sidebar.setStyle("-fx-background-color: " + Ui.NAVY + ";");
        Label logo = new Label("AIRWISE");
        logo.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: white; -fx-letter-spacing: 2px;");
        Label tagline = new Label("AIRPORT INTELLIGENCE");
        tagline.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #9AB0C8; -fx-letter-spacing: 1px;");
        sidebar.getChildren().addAll(logo, tagline, new Separator());
        TextField search = Ui.field("Find a module");
        search.textProperty().addListener((obs, oldValue, newValue) -> updateNavigation(newValue));
        sidebar.getChildren().add(search);
        navigation.setSpacing(5);
        navigation.getChildren().addAll(
                navButton("Dashboard", "◈"),
                navButton("Flight Delay", "✈"),
                navButton("Weather Disruption", "☁"),
                navButton("Passenger Crowd", "◎"),
                navButton("Taxi Demand", "↗"),
                navButton("Medical Emergency", "+"),
                navButton("Complaint Analyzer", "▤"),
                new Separator(),
                navButton("Analytics", "▥"),
                navButton("Alerts", "!"),
                navButton("Recommendations", "✓"),
                navButton("Settings", "⚙"));
        sidebar.getChildren().add(navigation);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label demo = new Label("DEMONSTRATION\nSIMULATED DATA");
        demo.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #FFD37A;");
        sidebar.getChildren().addAll(spacer, demo, Ui.small("AIRWISE operations console  •  v1.0"));
        return sidebar;
    }

    private Button navButton(String label, String icon) {
        Button button = new Button(icon + "    " + label);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(38);
        button.setUserData(label);
        button.setOnAction(e -> show(label));
        return button;
    }

    private void updateNavigation(String filter) {
        String query = filter == null ? "" : filter.trim().toLowerCase();
        for (Node node : navigation.getChildren()) {
            if (node instanceof Button button) {
                String label = String.valueOf(button.getUserData());
                button.setVisible(label.toLowerCase().contains(query));
                button.setManaged(button.isVisible());
            }
        }
    }

    private Node createHeader() {
        Label brand = new Label("AIRWISE");
        brand.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + Ui.NAVY + ";");
        Label subtitle = new Label("AI-Powered Smart Airport Operations & Passenger Intelligence System");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Ui.MUTED + ";");
        VBox title = new VBox(3, brand, subtitle);
        Label airport = new Label("◉  Indira Gandhi International Airport  (DEL)");
        airport.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + Ui.NAVY + ";");
        Label status = new Label("● SYSTEM ONLINE");
        status.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #16865B;"
                + " -fx-background-color: #E8F6EF; -fx-background-radius: 20; -fx-padding: 7 10;");
        dateTime.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Ui.MUTED + ";");
        VBox right = new VBox(7, Ui.row(airport, status), dateTime);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(title, new Region(), right);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent " + Ui.BORDER + " transparent;");
        return header;
    }

    private void show(String page) {
        activePage = page;
        for (Node node : navigation.getChildren()) {
            if (node instanceof Button button) {
                boolean selected = page.equals(button.getUserData());
                button.setStyle(selected
                        ? "-fx-background-color: #254365; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 7; -fx-padding: 9 10;"
                        : "-fx-background-color: transparent; -fx-text-fill: #D0DAE6; -fx-font-weight: 600; -fx-background-radius: 7; -fx-padding: 9 10;");
            }
        }
        Node view = switch (page) {
            case "Dashboard" -> dashboardHome();
            case "Flight Delay" -> FlightDelayModule.create(data, predictor);
            case "Weather Disruption" -> WeatherDisruptionModule.create(data, predictor);
            case "Passenger Crowd" -> PassengerCrowdModule.create(data, predictor);
            case "Taxi Demand" -> TaxiDemandModule.create(data, predictor);
            case "Medical Emergency" -> MedicalEmergencyModule.create(data, predictor);
            case "Complaint Analyzer" -> ComplaintAnalyzerModule.create(data, complaintAnalyzer);
            case "Analytics" -> analyticsPage();
            case "Alerts" -> alertsPage();
            case "Recommendations" -> recommendationsPage();
            default -> settingsPage();
        };
        content.getChildren().setAll(view);
    }

    private Node dashboardHome() {
        long delayed = data.getFlights().stream().filter(f -> f.previousDelayMinutes() >= 30).count();
        double averageDelay = analytics.averageFlightDelay(data);
        PassengerData passenger = data.getPassengers().get(0);
        PredictionEngine.CrowdPrediction crowd = predictor.predictCrowd(passenger.passengerCount(),
                passenger.arrivals(), passenger.departures(), true, passenger.holiday(),
                passenger.activeCounters(), passenger.securityLanes());
        TaxiData taxi = data.getTaxis().get(0);
        PredictionEngine.TaxiPrediction taxiForecast = predictor.predictTaxi(taxi.arrivals(), taxi.passengerCount(),
                true, taxi.holiday(), taxi.weatherCondition(), taxi.availableTaxis());
        long todayEmergencies = data.getEmergencies().stream()
                .filter(e -> e.timestamp().toLocalDate().equals(java.time.LocalDate.now())).count();
        long negative = data.getComplaints().stream()
                .filter(c -> complaintAnalyzer.analyze(c.text()).sentiment().equals("Negative")).count();
        long highPriority = data.getComplaints().stream()
                .filter(c -> List.of("High", "Critical").contains(complaintAnalyzer.analyze(c.text()).priority())).count();
        double responseTime = analytics.averageResponseTime(data);
        VBox metrics = new VBox(12,
                Ui.charts(metric("FLIGHTS", data.getFlights().size(), delayed + " delayed · "
                                + data.getFlights().stream().filter(f -> f.status().equals("Cancelled")).count() + " cancelled",
                                String.format("%.1f min avg delay", averageDelay)),
                        metric("PASSENGERS", String.format("%,d", crowd.expectedPassengers()),
                                "Crowd " + crowd.level(), "Queue risk · " + crowd.securityQueueMinutes() + " min security"),
                        metric("TAXI DEMAND", String.format("%,d", taxiForecast.demand()),
                                String.format("%,d taxis available", taxi.availableTaxis()),
                                taxiForecast.shortage() + " shortage · " + taxiForecast.risk() + " risk")),
                Ui.charts(metric("MEDICAL", todayEmergencies + " today",
                                String.format("%.1f min average response", responseTime),
                                data.getEmergencies().stream().filter(MedicalEmergencyData::medicalTeamAvailable).count()
                                        + " sampled cases with team available"),
                        metric("CUSTOMER SERVICE", data.getComplaints().size() + " complaints",
                                highPriority + " high priority",
                                Math.round(negative * 100.0 / Math.max(1, data.getComplaints().size())) + "% negative sentiment")));
        Map<String, Double> airlineDelays = data.getFlights().stream().collect(Collectors.groupingBy(
                FlightData::airline, LinkedHashMap::new, Collectors.averagingInt(FlightData::previousDelayMinutes)));
        Map<String, Long> crowdLevels = data.getPassengers().stream().collect(Collectors.groupingBy(
                p -> predictor.predictCrowd(p.passengerCount(), p.arrivals(), p.departures(), false,
                        p.holiday(), p.activeCounters(), p.securityLanes()).level(),
                LinkedHashMap::new, Collectors.counting()));
        List<AlertManager.Alert> alerts = alertManager.generate(data, predictor, complaintAnalyzer);
        List<String> recommendations = recommendationEngine.generate(data, predictor, complaintAnalyzer);
        VBox alertItems = new VBox(9);
        alerts.stream().limit(4).forEach(a -> alertItems.getChildren().add(alertNode(a)));
        VBox recommendationItems = new VBox(8);
        recommendations.stream().limit(4).forEach(r -> recommendationItems.getChildren().add(Ui.value("•  " + r)));
        return Ui.module("Operations overview",
                "A single operational snapshot for airport management. All displayed records are simulated demonstration data.",
                metrics,
                Ui.charts(Ui.card("Flight performance", Ui.barChart("Average delay by airline (minutes)", airlineDelays)),
                        Ui.card("Passenger pressure", Ui.pieChart("Sample crowd levels", crowdLevels))),
                Ui.charts(Ui.card("Priority alerts", alertItems),
                        Ui.card("Action recommendations", recommendationItems)));
    }

    private VBox metric(String title, Object mainValue, String detail, String footnote) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + Ui.MUTED + ";");
        Label value = new Label(String.valueOf(mainValue));
        value.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + Ui.NAVY + ";");
        Label note = Ui.value(detail);
        Label foot = Ui.small(footnote);
        VBox card = new VBox(8, heading, value, note, foot);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: "
                + Ui.BORDER + "; -fx-border-radius: 12;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Node alertNode(AlertManager.Alert alert) {
        Label title = new Label(alert.title() + "  ·  " + alert.severity());
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #A8561A;");
        Label message = Ui.value(alert.message());
        VBox box = new VBox(4, title, message);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #FFF8EF; -fx-background-radius: 8;");
        return box;
    }

    private Node analyticsPage() {
        Map<String, Double> airlineDelay = data.getFlights().stream().collect(Collectors.groupingBy(
                FlightData::airline, LinkedHashMap::new, Collectors.averagingInt(FlightData::previousDelayMinutes)));
        Map<String, Double> weatherDelay = new LinkedHashMap<>();
        weatherDelay.put("Rainfall", data.getWeather().stream().mapToDouble(WeatherData::rainfallMm).average().orElse(0));
        weatherDelay.put("Wind", data.getWeather().stream().mapToDouble(WeatherData::windSpeedKph).average().orElse(0));
        weatherDelay.put("Visibility", data.getWeather().stream().mapToDouble(w -> 8 - w.visibilityKm()).average().orElse(0));
        List<String> labels = data.getPassengers().stream().limit(10).map(p -> p.timestamp().toLocalTime().toString().substring(0, 5)).toList();
        List<Integer> passengerTrend = data.getPassengers().stream().limit(10).map(PassengerData::passengerCount).toList();
        List<Integer> taxiSupply = data.getTaxis().stream().limit(10).map(TaxiData::availableTaxis).toList();
        Map<String, Long> emergencies = analytics.emergenciesByLocation(data);
        Map<String, Long> complaints = analytics.complaintsByCategory(data, complaintAnalyzer);
        Map<String, Long> sentiments = data.getComplaints().stream().collect(Collectors.groupingBy(
                c -> complaintAnalyzer.analyze(c.text()).sentiment(), LinkedHashMap::new, Collectors.counting()));
        return Ui.module("Airport Analytics",
                "Cross-domain charts and summaries based on reproducible simulated sample records.",
                Ui.charts(Ui.card("Flight delays by airline", Ui.barChart("Average delay (minutes)", airlineDelay)),
                        Ui.card("Weather observations", Ui.barChart("Weather indicator averages", weatherDelay))),
                Ui.charts(Ui.card("Passenger volume / peak hours", Ui.lineChart("Passengers by sample interval", labels, passengerTrend)),
                        Ui.card("Taxi availability", Ui.lineChart("Available taxis by sample interval", labels, taxiSupply))),
                Ui.charts(Ui.card("Emergency hotspots", Ui.barChart("Events by location", emergencies)),
                        Ui.card("Complaint categories", Ui.barChart("Complaint volume", complaints)),
                        Ui.card("Customer sentiment", Ui.pieChart("Sentiment distribution", sentiments))));
    }

    private Node alertsPage() {
        VBox list = new VBox(11);
        alertManager.generate(data, predictor, complaintAnalyzer).forEach(a -> list.getChildren().add(alertNode(a)));
        if (list.getChildren().isEmpty()) list.getChildren().add(Ui.value("No alert thresholds are currently exceeded."));
        return Ui.module("Centralized Alerts",
                "Threshold-based operational alerts derived from current sample conditions and baseline estimates.",
                Ui.card("Active alerts (" + alertManager.generate(data, predictor, complaintAnalyzer).size() + ")", list));
    }

    private Node recommendationsPage() {
        VBox list = new VBox(10);
        recommendationEngine.generate(data, predictor, complaintAnalyzer)
                .forEach(item -> list.getChildren().add(Ui.value("✓  " + item)));
        return Ui.module("Operational Recommendations",
                "Suggested actions are generated from calculated crowd, transport, medical, weather, complaint and flight risk values.",
                Ui.card("Recommended actions", list));
    }

    private Node settingsPage() {
        return Ui.module("Settings & Data Notes",
                "Configuration and transparency information for the demonstration project.",
                Ui.card("Demonstration data",
                        Ui.value("This application uses deterministic, simulated in-memory sample records. It does not connect to a live airport, airline, weather service or passenger system."),
                        Ui.value("The predictions are understandable rule-based baselines for demonstration and education. They are not scientifically validated or trained machine-learning models."),
                        Ui.value("Sample sizes: " + data.getFlights().size() + " flights · " + data.getWeather().size()
                                + " weather records · " + data.getPassengers().size() + " passenger observations · "
                                + data.getTaxis().size() + " taxi observations · " + data.getEmergencies().size()
                                + " medical incidents · " + data.getComplaints().size() + " complaints.")),
                Ui.card("Airport profile", Ui.value("Indira Gandhi International Airport (DEL) — demonstration profile. The airport name is illustrative for this sample dashboard.")));
    }
}
