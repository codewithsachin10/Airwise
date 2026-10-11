import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ComplaintAnalyzerModule {
    public static Node create(DataManager data, ComplaintNLPAnalyzer analyzer) {
        TextArea complaint = new TextArea("My baggage hasn't arrived even though the flight landed two hours ago.");
        complaint.setWrapText(true);
        complaint.setPrefRowCount(3);
        Label result = Ui.value("Submit a complaint for transparent keyword-based classification.");
        var analyze = Ui.button("Analyze complaint");
        analyze.setOnAction(event -> {
            if (complaint.getText().isBlank()) {
                Ui.setResult(result, "Enter complaint text before analyzing.");
                return;
            }
            ComplaintNLPAnalyzer.Analysis analysis = analyzer.analyze(complaint.getText());
            Ui.setResult(result, "Category: " + analysis.category() + "  |  Sentiment: " + analysis.sentiment()
                    + "  |  Priority: " + analysis.priority() + "  |  " + analysis.explanation());
        });
        VBox form = Ui.card("Complaint text analysis",
                Ui.labeled("Passenger complaint", complaint), analyze, result,
                Ui.small("Uses transparent keyword matching and token scoring. This is not an advanced or trained AI model."));
        Map<String, Long> categories = data.getComplaints().stream()
                .collect(Collectors.groupingBy(c -> analyzer.analyze(c.text()).category(),
                        LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> sentiment = data.getComplaints().stream()
                .collect(Collectors.groupingBy(c -> analyzer.analyze(c.text()).sentiment(),
                        LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> priority = data.getComplaints().stream()
                .collect(Collectors.groupingBy(c -> analyzer.analyze(c.text()).priority(),
                        LinkedHashMap::new, Collectors.counting()));
        String mostCommon = categories.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("None");
        return Ui.module("AI Airport Customer Complaint Analyzer",
                "Demonstration / simulated data. Simple Java text processing classifies categories, sentiment and priority.",
                form, Ui.charts(Ui.barChart("Complaint categories", categories),
                        Ui.card("Sentiment distribution", Ui.pieChart("Sentiment", sentiment)),
                        Ui.card("Priority distribution", Ui.pieChart("Priority", priority))),
                Ui.card("Complaint overview", Ui.value("Analyzed sample complaints: " + data.getComplaints().size()
                        + "  |  Most common category: " + mostCommon
                        + "  |  Negative sentiment: " + sentiment.getOrDefault("Negative", 0L))));
    }
}
