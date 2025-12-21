package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Suggestion;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.SuggestionService;

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
import java.util.stream.Collectors;

public class SuggestionsWindow extends Stage {

    private final AuthService authService;
    private final SuggestionService suggestionService;
    private final LibriRepository libriRepo;

    private final ObservableList<Suggestion> suggestions = FXCollections.observableArrayList();

    private TableView<Suggestion> tbl;
    private Label lblHeader;

    public SuggestionsWindow(AuthService authService, SuggestionService suggestionService, LibriRepository libriRepo) {
        this.authService = authService;
        this.suggestionService = suggestionService;
        this.libriRepo = libriRepo;

        setTitle("I miei consigli");
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
        lblHeader = new Label("Consigli");
        lblHeader.getStyleClass().add("title");

        Label sub = new Label("Visualizza ed elimina i consigli associati ai tuoi libri.");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, lblHeader, sub);
        box.getStyleClass().add("appbar");
        return box;
    }

    private Node buildCenter() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card2");
        card.setPadding(new Insets(14));

        tbl = new TableView<>(suggestions);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("Nessun consiglio disponibile."));

        TableColumn<Suggestion, Integer> cBook = new TableColumn<>("ID Libro");
        cBook.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getBookId()));
        cBook.setMaxWidth(120);

        TableColumn<Suggestion, String> cBookTitle = new TableColumn<>("Titolo libro");
        cBookTitle.setCellValueFactory(v -> {
            Book b = libriRepo.findById(v.getValue().getBookId());
            return new ReadOnlyObjectWrapper<>(b == null ? "(n/d)" : b.getTitolo());
        });

        TableColumn<Suggestion, String> cSug = new TableColumn<>("Suggeriti (max 3)");
        cSug.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(
                v.getValue().getSuggeriti() == null ? "" :
                        v.getValue().getSuggeriti().stream()
                                .map(id -> {
                                    Book sb = libriRepo.findById(id);
                                    return sb == null ? String.valueOf(id) : sb.getTitolo();
                                })
                                .collect(Collectors.joining(" • "))
        ));

        tbl.getColumns().addAll(cBook, cBookTitle, cSug);

        Button btnDelete = new Button("Elimina consiglio");
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
        Label hint = new Label("Nota: la creazione/modifica consigli dipende dai comandi previsti nella tua UI principale.");
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
            lblHeader.setText("Consigli (login richiesto)");
            suggestions.clear();
            return;
        }

        lblHeader.setText("Consigli di: " + user);

        try {
            suggestions.setAll(suggestionService.listByUser(user));
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }

    private void deleteSelected() {
        Suggestion s = tbl.getSelectionModel().getSelectedItem();
        if (s == null) return;

        Book b = libriRepo.findById(s.getBookId());
        String title = b == null ? String.valueOf(s.getBookId()) : b.getTitolo();

        if (!FxUtil.confirm(this, "Conferma", "Eliminare il consiglio associato a: " + title + "?"))
            return;

        try {
            boolean ok = suggestionService.deleteSuggestion(authService.getCurrentUserid(), s.getBookId());
            if (!ok) throw new IllegalStateException("Eliminazione fallita.");
            load();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }

    public static void open(AuthService authService, SuggestionService suggestionService, LibriRepository repo) {
        SuggestionsWindow w = new SuggestionsWindow(authService, suggestionService, repo);
        w.showAndWait();
    }
}
