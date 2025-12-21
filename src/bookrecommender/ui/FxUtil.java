package bookrecommender.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Optional;

public final class FxUtil {

    private FxUtil() {}

    public static void info(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void error(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static boolean confirm(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    /** Toast semplice in basso al centro (funziona se root Scene è StackPane). */
    public static void toast(Scene scene, String message) {
        if (scene == null) return;
        if (!(scene.getRoot() instanceof StackPane stack)) return;

        Label chip = new Label(message);
        chip.getStyleClass().add("snackbar");

        StackPane.setAlignment(chip, Pos.BOTTOM_CENTER);
        stack.getChildren().add(chip);

        PauseTransition pt = new PauseTransition(Duration.seconds(2.2));
        pt.setOnFinished(e -> stack.getChildren().remove(chip));
        pt.playFromStart();
    }
}
