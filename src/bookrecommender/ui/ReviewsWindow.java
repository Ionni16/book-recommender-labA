package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Review;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.ReviewService;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;

public class ReviewsWindow extends Stage {

    private final AuthService authService;
    private final ReviewService reviewService;
    private final LibriRepository libriRepo;

    private final ObservableList<Review> reviews = FXCollections.observableArrayList();

    private TableView<Review> tbl;
    private Label lblHeader;

    public ReviewsWindow(AuthService authService, ReviewService reviewService, LibriRepository libriRepo) {
        this.authService = authService;
        this.reviewService = reviewService;
        this.libriRepo = libriRepo;

        setTitle("Le mie valutazioni");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        Scene scene = new Scene(new StackPane(root), 980, 560);

        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        setScene(scene);
        load();
    }

    private Node buildHeader() {
        lblHeader = new Label("Valutazioni");
        lblHeader.getStyleClass().add("title");

        Label sub = new Label("Visualizza ed elimina le tue valutazioni.");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, lblHeader, sub);
        box.getStyleClass().add("appbar");
        return box;
    }

    private Node buildCenter() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card2");
        card.setPadding(new Insets(14));

        tbl = new TableView<>(reviews);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("Nessuna valutazione disponibile."));

        TableColumn<Review, Integer> cBookId = new TableColumn<>("ID Libro");
        cBookId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getBookId()));
        cBookId.setMaxWidth(110);

        TableColumn<Review, String> cTitle = new TableColumn<>("Titolo");
        cTitle.setCellValueFactory(v -> {
            Book b = libriRepo.findById(v.getValue().getBookId());
            return new ReadOnlyObjectWrapper<>(b == null ? "(n/d)" : b.getTitolo());
        });

        TableColumn<Review, Integer> cFinal = new TableColumn<>("Voto");
        cFinal.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getVotoFinale()));
        cFinal.setMaxWidth(110);

        TableColumn<Review, String> cComment = new TableColumn<>("Commento");
        cComment.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(
                v.getValue().getCommento() == null ? "" : v.getValue().getCommento()
        ));

        tbl.getColumns().addAll(cBookId, cTitle, cFinal, cComment);

        Button btnDelete = new Button("Elimina valutazione");
        btnDelete.getStyleClass().add("danger");
        btnDelete.disableProperty().bind(tbl.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.setOnAction(e -> deleteSelected());

        Button btnReload = new Button("Ricarica");
        btnReload.getStyleClass().add("ghost");
        btnReload.setOnAction(e -> load());

        HBox actions = new HBox(10, btnReload, btnDelete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(new Label("Elenco"), tbl, actions);
        VBox.setVgrow(tbl, Priority.ALWAYS);

        BorderPane wrap = new BorderPane(card);
        wrap.setPadding(new Insets(14));
        return wrap;
    }

    private Node buildFooter() {
        Label hint = new Label("Nota: per creare/modificare valutazioni usa i comandi previsti nella tua UI principale.");
        hint.getStyleClass().add("muted");

        Button close = new Button("Chiudi");
        close.getStyleClass().add("ghost");
        close.setOnAction(e -> close());

        HBox bar = new HBox(10, hint, new Pane(), close);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

    private void load() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            lblHeader.setText("Valutazioni (login richiesto)");
            reviews.clear();
            return;
        }

        lblHeader.setText("Valutazioni di: " + user);

        try {
            reviews.setAll(reviewService.listByUser(user));
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }

    private void deleteSelected() {
        Review r = tbl.getSelectionModel().getSelectedItem();
        if (r == null) return;

        Book b = libriRepo.findById(r.getBookId());
        String title = b == null ? String.valueOf(r.getBookId()) : b.getTitolo();

        if (!FxUtil.confirm(this, "Conferma", "Eliminare la valutazione per: " + title + "?"))
            return;

        try {
            boolean ok = reviewService.deleteReview(authService.getCurrentUserid(), r.getBookId());
            if (!ok) throw new IllegalStateException("Eliminazione fallita.");
            load();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }

    public static void open(AuthService authService, ReviewService reviewService, LibriRepository repo) {
        ReviewsWindow w = new ReviewsWindow(authService, reviewService, repo);
        w.showAndWait();
    }
}
