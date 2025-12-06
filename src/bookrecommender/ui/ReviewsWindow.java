package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Review;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.ReviewService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Finestra per la gestione delle valutazioni:
 * - mostra tutte le valutazioni fatte dall'utente loggato
 * - permette di modificarle o cancellarle
 *
 * Richiede che ReviewService esponga:
 *  - List<Review> listByUser(String userid)
 *  - boolean deleteReview(String userid, int bookId)
 *  - boolean updateReview(Review r)
 */
public class ReviewsWindow extends Stage {

    private final AuthService authService;
    private final ReviewService reviewService;
    private final LibriRepository libriRepo;

    private TableView<ReviewRow> table;

    public ReviewsWindow(AuthService authService,
                         ReviewService reviewService,
                         LibriRepository libriRepo) {
        this.authService = authService;
        this.reviewService = reviewService;
        this.libriRepo = libriRepo;

        String userid = authService.getCurrentUserid();
        if (userid == null) {
            new Alert(Alert.AlertType.ERROR,
                    "Nessun utente loggato.", ButtonType.OK).showAndWait();
            close();
            return;
        }

        setTitle("Le mie valutazioni");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, 900, 480);
        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        setScene(scene);
        loadData();
    }

    private VBox buildCenter() {
        table = new TableView<>();
        table.setPlaceholder(new Label("Nessuna valutazione trovata."));

        TableColumn<ReviewRow, Number> colId = new TableColumn<>("ID libro");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().bookId));
        colId.setPrefWidth(90);

        TableColumn<ReviewRow, String> colTitolo = new TableColumn<>("Titolo");
        colTitolo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().title));
        colTitolo.setPrefWidth(350);

        TableColumn<ReviewRow, Number> colVoto = new TableColumn<>("Voto finale");
        colVoto.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().votoFinale));
        colVoto.setPrefWidth(100);

        TableColumn<ReviewRow, String> colComm = new TableColumn<>("Commento");
        colComm.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().commento));
        colComm.setPrefWidth(320);

        table.getColumns().addAll(colId, colTitolo, colVoto, colComm);

        Button btnEdit = new Button("Modifica");
        btnEdit.setOnAction(e -> editSelected());
        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button btnDelete = new Button("Elimina");
        btnDelete.getStyleClass().add("danger");
        btnDelete.setOnAction(e -> deleteSelected());
        btnDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(8, btnEdit, btnDelete);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        VBox box = new VBox(8, new Label("Valutazioni effettuate"), table, actions);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private HBox buildFooter() {
        Button btnClose = new Button("Chiudi");
        btnClose.setOnAction(e -> close());
        HBox box = new HBox(btnClose);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(8, 12, 8, 12));
        return box;
    }

    private void loadData() {
        try {
            String userid = authService.getCurrentUserid();
            List<Review> rs = reviewService.listByUser(userid);
            List<ReviewRow> rows = rs.stream().map(r -> {
                Book b = libriRepo.findById(r.getBookId());
                String titolo = b != null ? b.getTitolo() : ("[ID " + r.getBookId() + "]");
                return new ReviewRow(r, titolo);
            }).collect(Collectors.toList());
            table.getItems().setAll(rows);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nel caricamento delle valutazioni:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private void editSelected() {
        ReviewRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        Review r = row.review;

        Dialog<Review> dlg = new Dialog<>();
        dlg.setTitle("Modifica valutazione");
        dlg.setHeaderText("Libro ID " + r.getBookId());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        Spinner<Integer> spS = spinner15(r.getStile());
        Spinner<Integer> spC = spinner15(r.getContenuto());
        Spinner<Integer> spG = spinner15(r.getGradevolezza());
        Spinner<Integer> spO = spinner15(r.getOriginalita());
        Spinner<Integer> spE = spinner15(r.getEdizione());
        TextField tfComm = new TextField(r.getCommento());
        tfComm.setPromptText("Commento (max 256 caratteri)");

        grid.add(new Label("Stile"), 0, 0); grid.add(spS, 1, 0);
        grid.add(new Label("Contenuto"), 0, 1); grid.add(spC, 1, 1);
        grid.add(new Label("Gradevolezza"), 0, 2); grid.add(spG, 1, 2);
        grid.add(new Label("Originalità"), 0, 3); grid.add(spO, 1, 3);
        grid.add(new Label("Edizione"), 0, 4); grid.add(spE, 1, 4);
        grid.add(new Label("Commento"), 0, 5); grid.add(tfComm, 1, 5);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String comm = tfComm.getText();
            if (comm != null && comm.length() > 256) {
                new Alert(Alert.AlertType.WARNING,
                        "Il commento supera i 256 caratteri.",
                        ButtonType.OK).showAndWait();
                return null;
            }
            int s = spS.getValue();
            int c = spC.getValue();
            int g = spG.getValue();
            int o = spO.getValue();
            int e = spE.getValue();
            int voto = Review.calcolaVotoFinale(s, c, g, o, e);
            return new Review(r.getUserid(), r.getBookId(), s, c, g, o, e, voto, comm);
        });

        Review updated = dlg.showAndWait().orElse(null);
        if (updated == null) return;

        try {
            boolean ok = reviewService.updateReview(updated);
            if (!ok) {
                new Alert(Alert.AlertType.WARNING,
                        "Impossibile aggiornare la valutazione.",
                        ButtonType.OK).showAndWait();
            }
            loadData();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nell'aggiornamento della valutazione:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private void deleteSelected() {
        ReviewRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        Review r = row.review;

        Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminare la valutazione del libro ID " + r.getBookId() + "?",
                ButtonType.NO, ButtonType.YES);
        ask.setHeaderText("Conferma eliminazione valutazione");
        ask.showAndWait();
        if (ask.getResult() != ButtonType.YES) return;

        try {
            boolean ok = reviewService.deleteReview(r.getUserid(), r.getBookId());
            if (!ok) {
                new Alert(Alert.AlertType.WARNING,
                        "Valutazione non trovata.",
                        ButtonType.OK).showAndWait();
            }
            loadData();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nell'eliminazione della valutazione:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private Spinner<Integer> spinner15(int value) {
        Spinner<Integer> sp = new Spinner<>(1, 5, value);
        sp.setEditable(false);
        sp.setMaxWidth(80);
        return sp;
    }

    private static class ReviewRow {
        final Review review;
        final int bookId;
        final int votoFinale;
        final String title;
        final String commento;

        ReviewRow(Review r, String title) {
            this.review = r;
            this.bookId = r.getBookId();
            this.votoFinale = r.getVotoFinale();
            this.title = title;
            this.commento = r.getCommento() == null ? "" : r.getCommento();
        }
    }

    public static void open(AuthService authService,
                            ReviewService reviewService,
                            LibriRepository libriRepo) {
        ReviewsWindow w = new ReviewsWindow(authService, reviewService, libriRepo);
        w.show();
    }
}
