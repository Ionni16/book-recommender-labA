package bookrecommender.util;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class Utilities {

    public static Node buildFooter() {
        Label hint = new Label("Suggerimento: usa l’icona 👁 per vedere la password mentre scrivi.");
        hint.getStyleClass().add("muted");

        Button close = new Button("Chiudi");
        close.getStyleClass().add("ghost");
        close.setOnAction(_ -> close.getScene().getWindow().hide());

        HBox bar = new HBox(10, hint, new Pane(), close);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }
}
