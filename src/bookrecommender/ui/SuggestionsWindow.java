package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Suggestion;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.SuggestionService;
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
 * Gestione dei suggerimenti inseriti dall'utente:
 * - visualizza tutti i suggerimenti
 * - permette di eliminarli
 *
 * Richiede che SuggestionService esponga:
 *  - List<Suggestion> listByUser(String userid)
 *  - boolean deleteSuggestion(String userid, int bookId) // libro "base"
 */
public class SuggestionsWindow extends Stage {

    private final AuthService authService;
    private final SuggestionService suggestionService;
    private final LibriRepository libriRepo;

    private TableView<SuggestionRow> table;

    public SuggestionsWindow(AuthService authService,
                             SuggestionService suggestionService,
                             LibriRepository libriRepo) {
        this.authService = authService;
        this.suggestionService = suggestionService;
        this.libriRepo = libriRepo;

        String userid = authService.getCurrentUserid();
        if (userid == null) {
            new Alert(Alert.AlertType.ERROR,
                    "Nessun utente loggato.", ButtonType.OK).showAndWait();
            close();
            return;
        }

        setTitle("I miei suggerimenti");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, 900, 420);
        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        setScene(scene);
        loadData();
    }

    private VBox buildCenter() {
        table = new TableView<>();
        table.setPlaceholder(new Label("Nessun suggerimento inserito."));

        TableColumn<SuggestionRow, Number> colBaseId = new TableColumn<>("Libro base (ID)");
        colBaseId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().baseId));
        colBaseId.setPrefWidth(110);

        TableColumn<SuggestionRow, String> colBaseTitle = new TableColumn<>("Libro base");
        colBaseTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().baseTitle));
        colBaseTitle.setPrefWidth(260);

        TableColumn<SuggestionRow, String> colSug = new TableColumn<>("Suggeriti");
        colSug.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().suggeritiDescr));
        colSug.setPrefWidth(430);

        table.getColumns().addAll(colBaseId, colBaseTitle, colSug);

        Button btnDelete = new Button("Elimina suggerimento");
        btnDelete.getStyleClass().add("danger");
        btnDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.setOnAction(e -> deleteSelected());

        VBox box = new VBox(8, new Label("Suggerimenti inseriti"), table, btnDelete);
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
            List<Suggestion> list = suggestionService.listByUser(userid);
            List<SuggestionRow> rows = list.stream()
                    .map(this::toRow)
                    .collect(Collectors.toList());
            table.getItems().setAll(rows);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nel caricamento dei suggerimenti:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private SuggestionRow toRow(Suggestion s) {
        Book base = libriRepo.findById(s.getBookId());
        String baseTitle = base != null ? base.getTitolo() : ("[ID " + s.getBookId() + "]");
        String descr = s.getSuggeriti().stream()
                .map(id -> {
                    Book b = libriRepo.findById(id);
                    return (b != null ? b.getTitolo() : ("ID " + id));
                })
                .collect(Collectors.joining(", "));
        return new SuggestionRow(s, baseTitle, descr);
    }

    private void deleteSelected() {
        SuggestionRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;

        Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminare il suggerimento relativo al libro base ID " + row.baseId + "?",
                ButtonType.NO, ButtonType.YES);
        ask.setHeaderText("Conferma eliminazione suggerimento");
        ask.showAndWait();
        if (ask.getResult() != ButtonType.YES) return;

        try {
            boolean ok = suggestionService.deleteSuggestion(row.userid, row.baseId);
            if (!ok) {
                new Alert(Alert.AlertType.WARNING,
                        "Suggerimento non trovato.",
                        ButtonType.OK).showAndWait();
            }
            loadData();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nell'eliminazione del suggerimento:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private static class SuggestionRow {
        final Suggestion suggestion;
        final String userid;
        final int baseId;
        final String baseTitle;
        final String suggeritiDescr;

        SuggestionRow(Suggestion s, String baseTitle, String descr) {
            this.suggestion = s;
            this.userid = s.getUserid();
            this.baseId = s.getBookId();
            this.baseTitle = baseTitle;
            this.suggeritiDescr = descr;
        }
    }

    public static void open(AuthService authService,
                            SuggestionService suggestionService,
                            LibriRepository libriRepo) {
        SuggestionsWindow w = new SuggestionsWindow(authService, suggestionService, libriRepo);
        w.show();
    }
}
