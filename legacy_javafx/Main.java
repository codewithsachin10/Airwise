import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        Dashboard dashboard = new Dashboard();
        Scene scene = new Scene(dashboard.createView(), 1440, 920);
        stage.setTitle("AIRWISE — Smart Airport Intelligence Dashboard");
        stage.setMinWidth(1050);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
