package bookrecommender;

import bookrecommender.ui.BookRecommenderFX;
import javafx.application.Application;

import java.util.Arrays;

public final class Main {

    public static void main(String[] args) {
        boolean cli = Arrays.asList(args).contains("--cli");
        if (cli) {
            BookRecommender.runCli();
            return;
        }

        Application.launch(BookRecommenderFX.class, args);
    }
}

