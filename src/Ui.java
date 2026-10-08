import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public final class Ui {
    public static final String NAVY = "#10243A";
    public static final String BLUE = "#246BCE";
    public static final String MUTED = "#748399";
    public static final String BG = "#F3F6FA";
    public static final String BORDER = "#E5EAF1";

    private Ui() {
    }

    public static VBox page(String title, String subtitle) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: " + NAVY + ";");
        Label sub = new Label(subtitle);
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: " + MUTED + ";");
        VBox box = new VBox(5, heading, sub);
        box.setPadding(new Insets(4, 0, 10, 0));
        return box;
    }

    public static VBox card(String title, Node... content) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + NAVY + ";");
        VBox box = new VBox(12);
        box.getChildren().add(header);
        box.getChildren().addAll(content);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: " + BORDER
                + "; -fx-border-radius: 12;");
        return box;
    }

    public static Label value(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: " + NAVY + ";");
        return label;
    }

    public static Label small(String value) {
        Label label = new Label(value);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + ";");
        return label;
    }

    public static TextField field(String initial) {
        TextField field = new TextField(initial);
        field.setPrefWidth(190);
        field.setStyle("-fx-background-color: #F8FAFD; -fx-border-color: " + BORDER
                + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 9;");
        return field;
    }

    public static ComboBox<String> choices(String... values) {
        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(values));
        box.getSelectionModel().selectFirst();
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static Button button(String label) {
        Button button = new Button(label);
        button.setStyle("-fx-background-color: " + BLUE
                + "; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 7; -fx-padding: 10 16;");
        return button;
    }

    public static Node labeled(String label, Node control) {
        Label caption = new Label(label);
        caption.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + MUTED + ";");
        VBox group = new VBox(5, caption, control);
        VBox.setVgrow(control, Priority.NEVER);
        return group;
    }

    public static GridPane formGrid(Node... fields) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < fields.length; i++) {
            int col = i % 3;
            int row = i / 3;
            grid.add(fields[i], col, row);
        }
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(33.33);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }
        return grid;
    }

    public static BarChart<String, Number> barChart(String title, Map<String, ? extends Number> values) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        values.forEach((key, value) -> series.getData().add(new XYChart.Data<>(key, value)));
        chart.getData().add(series);
        chart.setPrefHeight(280);
        chart.setMinHeight(240);
        return chart;
    }

    public static LineChart<String, Number> lineChart(String title, List<String> labels,
                                                       List<? extends Number> values) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < Math.min(labels.size(), values.size()); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), values.get(i)));
        }
        chart.getData().add(series);
        chart.setPrefHeight(280);
        chart.setMinHeight(240);
        return chart;
    }

    public static PieChart pieChart(String title, Map<String, ? extends Number> values) {
        PieChart chart = new PieChart();
        chart.setTitle(title);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        values.forEach((key, value) -> chart.getData().add(new PieChart.Data(key, value.doubleValue())));
        chart.setPrefHeight(280);
        chart.setMinHeight(240);
        return chart;
    }

    public static HBox charts(Node... charts) {
        HBox row = new HBox(14, charts);
        for (Node chart : charts) {
            HBox.setHgrow(chart, Priority.ALWAYS);
            if (chart instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    public static VBox module(String title, String description, Node... sections) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 24, 28, 24));
        content.setStyle("-fx-background-color: " + BG + ";");
        content.getChildren().add(page(title, description));
        content.getChildren().addAll(sections);
        return content;
    }

    public static void setResult(Label target, String text) {
        target.setText(text);
        target.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + NAVY
                + "; -fx-background-color: #EFF5FF; -fx-background-radius: 8; -fx-padding: 12;");
    }

    public static HBox row(Node... nodes) {
        HBox row = new HBox(10, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
