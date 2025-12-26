package bookrecommender.ui;

import bookrecommender.model.*;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.*;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



/**
 * Classe principale dell'interfaccia grafica JavaFX dell'applicazione
 * <code>Book Recommender</code>.
 * <p>
 * Si occupa di:
 * <ul>
 *     <li>Inizializzare i servizi di accesso ai dati
 *         ({@link bookrecommender.repo.LibriRepository} e i vari
 *         servizi nel package <code>bookrecommender.service</code>);</li>
 *     <li>Configurare lo stile JavaFX e caricare il foglio di stile CSS;</li>
 *     <li>Costruire la finestra principale con barra superiore, area di ricerca
 *         e tabella dei risultati;</li>
 *     <li>Aprire le finestre secondarie (login, registrazione, profilo utente,
 *         gestione librerie, valutazioni e suggerimenti);</li>
 *     <li>Gestire lo stato dell'utente corrente (login/logout) aggiornando
 *         i controlli visibili nella UI.</li>
 * </ul>
 *
 * @author Ionut Puiu
 * @version 1.0
 */
public class BookRecommenderFX extends Application {

    // ---- Services ----
    private LibriRepository libriRepo;
    private SearchService searchService;
    private AuthService authService;
    private LibraryService libraryService;
    private ReviewService reviewService;
    private SuggestionService suggestionService;
    private AggregationService aggregationService;
    private Path dataDir;


    // ---- AppBar buttons ----
    private Button btnLogin;
    private Button btnRegister;
    private Button btnLogout;

    // ---- UI ----
    private Label lblUserBadge;
    private Label lblStatus;

    private ComboBox<SearchMode> cbSearchMode;

    private TextField tfTitle;
    private TextField tfAuthor;
    private Spinner<Integer> spYear;
    private Spinner<Integer> spLimit;
    private CheckBox ckOnlyMyLibraries;

    private TableView<Book> tbl;
    private final ObservableList<Book> data = FXCollections.observableArrayList();

    // detail panel
    private Label dTitle, dAuthors, dMeta, dCategory, dPublisher;
    private Label dAvg;
    private VBox dStarsBox;
    private VBox dSuggestions;

    private Button btnRateThis;
    private Button btnSuggestThis;

    private Book selectedBook; // libro selezionato per azioni rapide

    private enum SearchMode {
        TITLE("Solo Titolo"),
        AUTHOR("Solo Autore"),
        TITLE_AUTHOR("Titolo + Autore"),
        TITLE_YEAR("Titolo + Anno");

        private final String label;
        SearchMode(String l) { this.label = l; }
        @Override public String toString() { return label; }
    }

    private static final DecimalFormat DF1 = new DecimalFormat("0.0");

    // validazioni registrazione
    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CF_RX = Pattern.compile("^[A-Za-z0-9]{16}$");




    /**
     * Punto di ingresso dell'applicazione JavaFX.
     * <p>
     * In questo metodo vengono:
     * <ul>
     *     <li>Creata (se necessario) la cartella locale dei dati;</li>
     *     <li>Inizializzati i repository e i servizi di dominio
     *         (ricerca, autenticazione, librerie, valutazioni, suggerimenti,
     *         aggregazione statistiche);</li>
     *     <li>Costruiti i nodi principali dell'interfaccia grafica
     *         (barra superiore, area ricerca, tabella risultati);</li>
     *     <li>Impostata la scena principale e mostrato lo <code>Stage</code> passato
     *         dal runtime JavaFX.</li>
     * </ul>
     *
     * @param stage finestra principale fornita dal runtime JavaFX
     * @throws Exception in caso di errori di I/O o in fase di inizializzazione
     *                   dei servizi e delle risorse grafiche
     */
    @Override
    public void start(Stage stage) throws Exception {
        dataDir = Paths.get("data");
        Files.createDirectories(dataDir);

        libriRepo = new LibriRepository(dataDir.resolve("Libri.dati"));
        libriRepo.load();

        searchService      = new SearchService(libriRepo);
        authService        = new AuthService(dataDir.resolve("UtentiRegistrati.dati"));
        libraryService     = new LibraryService(dataDir.resolve("Librerie.dati"));
        reviewService      = new ReviewService(dataDir.resolve("ValutazioniLibri.dati"), dataDir.resolve("Librerie.dati"));
        suggestionService  = new SuggestionService(dataDir.resolve("ConsigliLibri.dati"), dataDir.resolve("Librerie.dati"));
        aggregationService = new AggregationService(dataDir.resolve("ValutazioniLibri.dati"), dataDir.resolve("ConsigliLibri.dati"));

        StackPane stack = new StackPane();
        BorderPane app = new BorderPane();
        app.getStyleClass().add("app-bg");
        stack.getChildren().add(app);

        StackPane.setAlignment(app, Pos.TOP_LEFT);

        // e il BorderPane deve sempre riempire la scena
        app.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);


        app.setTop(buildAppBar(stage));
        app.setCenter(buildMain(stage));
        app.setBottom(buildStatusBar());

        Scene scene = new Scene(stack, 1280, 740);

        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                Object target = e.getTarget();
                if (target instanceof TextInputControl) return; // non cercare mentre scrivi
                doSearch(stage);
            }
            if (e.getCode() == KeyCode.F5) { e.consume(); refresh(stage); }
        });


        stage.setTitle("Book Recommender");
        stage.setScene(scene);
        stage.show();

        refreshUserUi();
        lblStatus.setText("Dataset caricato: " + libriRepo.size() + " libri");
    }

    // ---------------- AppBar ----------------

    private Node buildAppBar(Stage owner) {
        Label title = new Label("Book Recommender");
        title.getStyleClass().add("title");
        Label sub = new Label("Cerca libri, gestisci librerie, valutazioni e consigli");
        sub.getStyleClass().add("subtitle");

        VBox left = new VBox(2, title, sub);

        lblUserBadge = new Label("Ospite");
        lblUserBadge.getStyleClass().add("badge");

        btnLogin = new Button("Accedi");
        btnLogin.getStyleClass().add("primary");
        btnLogin.setOnAction(_ -> openLogin(owner));

        btnRegister = new Button("Registrati");
        btnRegister.setOnAction(_ -> openRegister(owner));

        btnLogout = new Button("Logout");
        btnLogout.getStyleClass().add("ghost");
        btnLogout.setOnAction(_ -> {
            authService.logout();
            refreshUserUi();
            FxUtil.toast(owner.getScene(), "Logout effettuato");
        });

        Button btnArea = new Button("Area riservata");
        btnArea.setOnAction(_ -> openReservedHome(owner));

        HBox right = new HBox(10, lblUserBadge, btnArea, btnLogin, btnRegister, btnLogout);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(20, left, new Pane(), right);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("appbar");
        bar.setPadding(new Insets(18, 18, 14, 18)); // più aria sopra e sotto
        bar.setMinHeight(88);
        bar.setPrefHeight(88);
        bar.setMaxHeight(88);
        return bar;

    }

    private void refreshUserUi() {
        String u = authService.getCurrentUserid();
        boolean logged = (u != null);

        lblUserBadge.setText(logged ? ("Loggato: " + u) : "Ospite");

        btnLogin.setVisible(!logged);    btnLogin.setManaged(!logged);
        btnRegister.setVisible(!logged); btnRegister.setManaged(!logged);
        btnLogout.setVisible(logged);    btnLogout.setManaged(logged);

        ckOnlyMyLibraries.setDisable(!logged);
        if (!logged) ckOnlyMyLibraries.setSelected(false);

        // abilita/disabilita azioni libro nel dettaglio
        if (btnRateThis != null) {
            btnRateThis.setDisable(!logged || selectedBook == null);
            btnSuggestThis.setDisable(!logged || selectedBook == null);
            btnRateThis.setText(logged ? "Valuta questo libro" : "Valuta (login richiesto)");
            btnSuggestThis.setText(logged ? "Consiglia libri" : "Consiglia (login richiesto)");
        }
    }

    // ---------------- Main layout ----------------

    private Node buildMain(Stage owner) {
        VBox searchCard = buildSearchCard(owner);
        VBox tableCard = buildTableCard(owner);
        VBox detailCard = buildDetailCard(owner);

        HBox main = new HBox(14, searchCard, tableCard, detailCard);
        main.setPadding(new Insets(14));

        // 🔹 SINISTRA: più compatta
        searchCard.setMinWidth(320);
        searchCard.setPrefWidth(340);
        searchCard.setMaxWidth(360);

        // 🔹 CENTRO: prende più spazio
        tableCard.setMinWidth(520);
        tableCard.setPrefWidth(640);

        // 🔹 DESTRA: stabile e leggibile
        detailCard.setMinWidth(400);
        detailCard.setPrefWidth(420);
        detailCard.setMaxWidth(460);

        // 🔹 Grow policy: SOLO il centro cresce
        HBox.setHgrow(searchCard, Priority.NEVER);
        HBox.setHgrow(detailCard, Priority.NEVER);
        HBox.setHgrow(tableCard, Priority.ALWAYS);

        return main;
    }



    private VBox buildSearchCard(Stage owner) {
        Label t = new Label("Cerca un libro");
        t.getStyleClass().add("card-title");

        cbSearchMode = new ComboBox<>();
        cbSearchMode.getItems().setAll(SearchMode.values());
        cbSearchMode.getSelectionModel().select(SearchMode.TITLE);

        tfTitle = new TextField();
        tfTitle.setPromptText("Titolo… (min 2 caratteri)");

        tfAuthor = new TextField();
        tfAuthor.setPromptText("Autore… (min 2 caratteri)");

        spYear = new Spinner<>();
        spYear.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1400, 2100, 2000));
        spYear.setEditable(true);

        spLimit = new Spinner<>();
        spLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 500, 50, 10));
        spLimit.setEditable(true);

        ckOnlyMyLibraries = new CheckBox("Cerca solo nelle mie librerie (login)");
        ckOnlyMyLibraries.setSelected(false);

        cbSearchMode.valueProperty().addListener((ignoreObs, ignoreOld, n) -> applySearchMode(n));
        applySearchMode(cbSearchMode.getValue());

        Button btnSearch = new Button("Cerca");
        btnSearch.getStyleClass().add("primary");
        btnSearch.setMaxWidth(Double.MAX_VALUE);
        btnSearch.setOnAction(_ -> doSearch(owner));

        Button btnClear = new Button("Reset");
        btnClear.getStyleClass().add("ghost");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setOnAction(_ -> {
            tfTitle.clear();
            tfAuthor.clear();
            ckOnlyMyLibraries.setSelected(false);
            data.clear();
            clearDetail();
            FxUtil.toast(owner.getScene(), "Campi resettati");
        });

        VBox box = new VBox(
                10,
                t,
                new Separator(),
                label("Modalità ricerca"), cbSearchMode,
                label("Titolo"), tfTitle,
                label("Autore"), tfAuthor,
                label("Anno"), spYear,
                label("Limite risultati"), spLimit,
                ckOnlyMyLibraries,
                new Separator(),
                btnSearch,
                btnClear
        );
        box.getStyleClass().add("card");

        return box;
    }



    private void applySearchMode(SearchMode mode) {
        setVisibleManaged(tfTitle, false);
        setVisibleManaged(tfAuthor, false);
        setVisibleManaged(spYear, false);

        if (mode == SearchMode.TITLE) {
            setVisibleManaged(tfTitle, true);
        } else if (mode == SearchMode.AUTHOR) {
            setVisibleManaged(tfAuthor, true);
        } else if (mode == SearchMode.TITLE_AUTHOR) {
            setVisibleManaged(tfTitle, true);
            setVisibleManaged(tfAuthor, true);
        } else { // TITLE_YEAR
            setVisibleManaged(tfTitle, true);
            setVisibleManaged(spYear, true);
        }
    }

    private static void setVisibleManaged(Control c, boolean on) {
        c.setVisible(on);
        c.setManaged(on);
    }

    // ---------------- TABLE (fix) ----------------

    private VBox buildTableCard(Stage owner) {
        Label t = new Label("Risultati");
        t.getStyleClass().add("card-title");

        tbl = new TableView<>(data);

        // più “web”: niente tagli strani, scroll se serve
        tbl.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("Esegui una ricerca per visualizzare risultati."));

        TableColumn<Book, Integer> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getId()));
        cId.setPrefWidth(70);

        TableColumn<Book, String> cTitle = new TableColumn<>("Titolo");
        cTitle.setCellValueFactory(new PropertyValueFactory<>("titolo"));
        cTitle.setPrefWidth(340);

        TableColumn<Book, String> cAuthor = new TableColumn<>("Autore/i");
        cAuthor.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(
                v.getValue().getAutori() == null ? "" : String.join(", ", v.getValue().getAutori())
        ));
        cAuthor.setPrefWidth(320);

        TableColumn<Book, Integer> cYear = new TableColumn<>("Anno");
        cYear.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getAnno()));
        cYear.setPrefWidth(90);

        cId.setMinWidth(70);      cId.setMaxWidth(90);
        cTitle.setMinWidth(320);  cTitle.setMaxWidth(900);
        cAuthor.setMinWidth(260); cAuthor.setMaxWidth(800);
        cYear.setMinWidth(80);    cYear.setMaxWidth(120);

        cYear.setStyle("-fx-alignment: CENTER;");
        cYear.setCellFactory(ignoredEvent -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "" : String.valueOf(item)));
                setAlignment(Pos.CENTER);
            }
        });

        FxUtil.addColumns(tbl, List.of(cId, cTitle, cAuthor, cYear));
        


        tbl.getSelectionModel().selectedItemProperty().addListener((ignoreignoreObs, ignoreOld, n) -> {
            if (n != null) showDetail(owner, n);
        });

        // doppio click resta utile
        tbl.setRowFactory(ignoredTv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showDetail(owner, row.getItem());
            });
            return row;
        });

        Button btnRefresh = new Button("Ricarica (F5)");
        btnRefresh.getStyleClass().add("ghost");
        btnRefresh.setOnAction(_ -> refresh(owner));

        HBox actions = new HBox(10, btnRefresh);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(10, headerRow(t, actions), tbl);
        box.getStyleClass().add("card2");
        VBox.setVgrow(tbl, Priority.ALWAYS);
        return box;
    }


    private void openAddToLibraryDialog(Stage owner, Book book) {
        String user = ensureLoggedIn(owner);
        if (user == null || book == null) return;

        List<Library> libs;
        try {
            libs = libraryService.listUserLibraries(user);
        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Impossibile leggere le librerie: " + e.getMessage());
            return;
        }

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Aggiungi a libreria");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label("Aggiungi libro alla tua libreria");
        h.getStyleClass().add("title");
        Label sub = new Label(book.getTitolo());
        sub.getStyleClass().add("subtitle");

        ComboBox<Library> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);

        cb.setCellFactory(ignoredEvent -> new ListCell<>() {
            @Override
            protected void updateItem(Library item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getNome() + "  (" + item.getBookIds().size() + " libri)");
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Library item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("Seleziona libreria…");
                else setText(item.getNome());
            }
        });

        cb.getItems().setAll(libs);
        if (!libs.isEmpty()) cb.getSelectionModel().select(0);

        TextField tfNew = new TextField();
        tfNew.setPromptText("Oppure crea nuova libreria (min 5 caratteri)");

        Button btnCreate = new Button("Crea");
        btnCreate.getStyleClass().add("ghost");

        Button btnAdd = new Button("Aggiungi");
        btnAdd.getStyleClass().add("primary");
        btnAdd.setMaxWidth(Double.MAX_VALUE);

        btnCreate.setOnAction(_ -> {
            String name = tfNew.getText() == null ? "" : tfNew.getText().trim();
            if (name.length() < 5) {
                FxUtil.error(owner, "Nome non valido", "Il nome deve avere almeno 5 caratteri.");
                return;
            }
            try {
                Library lib = new Library(user, name, new HashSet<>());
                boolean ok = libraryService.saveLibrary(lib);
                if (!ok) throw new IllegalStateException("Creazione libreria fallita.");
                cb.getItems().setAll(libraryService.listUserLibraries(user));
                cb.getSelectionModel().select(cb.getItems().stream()
                        .filter(x -> x.getNome().equals(name))
                        .findFirst().orElse(null));
                tfNew.clear();
                FxUtil.toast(owner.getScene(), "Libreria creata");
            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        btnAdd.setOnAction(_ -> {
            Library sel = cb.getValue();
            if (sel == null) {
                FxUtil.error(owner, "Selezione mancante", "Seleziona una libreria.");
                return;
            }
            try {
                Set<Integer> newSet = new HashSet<>(sel.getBookIds());
                if (newSet.contains(book.getId())) {
                    FxUtil.toast(owner.getScene(), "Libro già presente in \"" + sel.getNome() + "\"");
                    return;
                }
                newSet.add(book.getId());
                Library updated = new Library(sel.getUserid(), sel.getNome(), newSet);

                boolean ok = libraryService.saveLibrary(updated);
                if (!ok) {
                    FxUtil.error(owner, "Operazione non riuscita",
                            "Non posso aggiungere il libro. Controlla i dati.");
                    return;
                }

                FxUtil.toast(owner.getScene(), "Aggiunto a \"" + sel.getNome() + "\"");
                d.close();

            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(label("Libreria"), 0, 0);
        grid.add(cb, 1, 0);

        grid.add(label("Nuova libreria"), 0, 1);
        HBox newRow = new HBox(10, tfNew, btnCreate);
        HBox.setHgrow(tfNew, Priority.ALWAYS);
        grid.add(newRow, 1, 1);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox box = new VBox(12, h, sub, new Separator(), grid, new Separator(), btnAdd);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }



    // ---------------- DETAIL (aggiungo bottoni rapidi) ----------------

    private VBox buildDetailCard(Stage owner) {
        Label t = new Label("Dettaglio libro");
        t.getStyleClass().add("card-title");

        dTitle = new Label("-");
        dTitle.getStyleClass().add("title");
        dTitle.setWrapText(true);

        dAuthors = new Label("-");
        dAuthors.getStyleClass().add("muted");
        dAuthors.setWrapText(true);

        dMeta = new Label("-");
        dMeta.getStyleClass().add("chip");

        dCategory = new Label("-");
        dCategory.getStyleClass().add("chip");

        dPublisher = new Label("-");
        dPublisher.getStyleClass().add("chip");

        FlowPane chips = new FlowPane(10, 10, dMeta, dCategory, dPublisher);
        chips.setPrefWrapLength(360); // adatta alla larghezza della colonna destra
        chips.setAlignment(Pos.CENTER_LEFT);

        Label rateT = new Label("Recensioni");
        rateT.getStyleClass().add("card-title");

        dAvg = new Label("-");
        dAvg.getStyleClass().add("muted");
        dAvg.setWrapText(true);

        dStarsBox = new VBox(6);

        Label sugT = new Label("Consigliati");
        sugT.getStyleClass().add("card-title");
        dSuggestions = new VBox(8);

        Button btnAddToLibrary = new Button("Aggiungi alla mia libreria");
        btnAddToLibrary.getStyleClass().add("primary");
        btnAddToLibrary.setMaxWidth(Double.MAX_VALUE);
        btnAddToLibrary.setOnAction(_ -> {
            if (selectedBook == null) return;
            if (ensureLoggedIn(owner) == null) return;
            openAddToLibraryDialog(owner, selectedBook);
        });

        // Azioni rapide
        btnRateThis = new Button("Valuta questo libro");
        btnRateThis.getStyleClass().add("ghost");
        btnRateThis.setMaxWidth(Double.MAX_VALUE);
        btnRateThis.setOnAction(_ -> {
            if (selectedBook == null) return;
            if (ensureLoggedIn(owner) == null) return;
            openReviewEditor(owner, selectedBook);
        });

        btnSuggestThis = new Button("Consiglia libri");
        btnSuggestThis.getStyleClass().add("ghost");
        btnSuggestThis.setMaxWidth(Double.MAX_VALUE);
        btnSuggestThis.setOnAction(_ -> {
            if (selectedBook == null) return;
            if (ensureLoggedIn(owner) == null) return;
            openSuggestionEditor(owner, selectedBook);
        });

        // Liste (restano utili)
        Button btnOpenReviewList = new Button("Le mie valutazioni…");
        btnOpenReviewList.getStyleClass().add("ghost");
        btnOpenReviewList.setMaxWidth(Double.MAX_VALUE);
        btnOpenReviewList.setOnAction(_ -> {
            if (ensureLoggedIn(owner) == null) return;
            ReviewsWindow.open(authService, reviewService, libriRepo);
        });

        Button btnOpenSugList = new Button("I miei consigli…");
        btnOpenSugList.getStyleClass().add("ghost");
        btnOpenSugList.setMaxWidth(Double.MAX_VALUE);
        btnOpenSugList.setOnAction(_ -> {
            if (ensureLoggedIn(owner) == null) return;
            SuggestionsWindow.open(authService, suggestionService, libriRepo);
        });

        VBox box = new VBox(
                10,
                t,
                new Separator(),
                dTitle, dAuthors,
                chips,
                new Separator(),
                rateT, dAvg, dStarsBox,          
                new Separator(),
                sugT, dSuggestions,
                new Separator(),
                btnAddToLibrary,
                btnRateThis,
                btnSuggestThis,
                btnOpenReviewList,
                btnOpenSugList
        );
        box.getStyleClass().add("card");

        refreshUserUi();
        return box;
    }


    private static HBox starsRow(String label, double value0to5) {
        int full = (int) Math.round(value0to5); // 0..5
        full = Math.max(0, Math.min(5, full));

        Label name = new Label(label);
        name.getStyleClass().add("star-label");
        name.setMinWidth(110);

        HBox stars = new HBox(2);
        for (int i = 1; i <= 5; i++) {
            Label s = new Label("★");
            s.getStyleClass().add("star");
            if (i > full) s.getStyleClass().add("off");
            stars.getChildren().add(s);
        }

        Label num = new Label(" " + DF1.format(value0to5));
        num.getStyleClass().add("muted");

        HBox row = new HBox(10, name, stars, num);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }



    private Node buildStatusBar() {
        lblStatus = new Label("Pronto");
        lblStatus.getStyleClass().add("muted");

        Label hint = new Label("Invio = Cerca • Doppio click = Dettagli • F5 = Ricarica");
        hint.getStyleClass().add("muted");

        HBox bar = new HBox(14, lblStatus, new Pane(), hint);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

    // ---------------- Search logic ----------------

    private void doSearch(Stage owner) {
        SearchMode mode = cbSearchMode.getValue();
        int limit = spLimit.getValue();

        try {
            List<Book> res;

            if (mode == SearchMode.TITLE) {
                String title = safe(tfTitle.getText());
                if (title.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nel titolo.");
                res = searchService.cercaLibroPerTitolo(title);

            } else if (mode == SearchMode.AUTHOR) {
                String author = safe(tfAuthor.getText());
                if (author.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nell'autore.");
                res = searchService.cercaLibroPerAutore(author);

            } else if (mode == SearchMode.TITLE_AUTHOR) {
                String title = safe(tfTitle.getText());
                String author = safe(tfAuthor.getText());
                if (title.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nel titolo.");
                if (author.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nell'autore.");

                res = searchService.cercaLibroPerTitolo(title).stream()
                        .filter(b -> b.getAutori() != null && b.getAutori().stream()
                                .anyMatch(a -> a.toLowerCase().contains(author.toLowerCase())))
                        .collect(Collectors.toList());

            } else { // TITLE_YEAR
                String title = safe(tfTitle.getText());
                if (title.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nel titolo.");
                int year = spYear.getValue();

                res = searchService.cercaLibroPerTitolo(title).stream()
                        .filter(b -> b.getAnno() != null && b.getAnno() == year)
                        .collect(Collectors.toList());
            }

            if (ckOnlyMyLibraries.isSelected()) {
                String user = ensureLoggedIn(owner);
                if (user == null) return;

                Set<Integer> myBookIds = libraryService.listUserLibraries(user).stream()
                        .flatMap(l -> l.getBookIds().stream())
                        .collect(Collectors.toSet());

                res = res.stream().filter(b -> myBookIds.contains(b.getId())).collect(Collectors.toList());
            }

            res = res.stream().limit(limit).collect(Collectors.toList());

            data.setAll(res);
            lblStatus.setText("Risultati: " + res.size());
            clearDetail();
            if (!res.isEmpty()) tbl.getSelectionModel().select(0);

        } catch (Exception ex) {
            FxUtil.error(owner, "Ricerca non valida", ex.getMessage());
        }
    }

    private void refresh(Stage owner) {
        try {
            libriRepo.load();
            FxUtil.toast(owner.getScene(), "Dataset ricaricato: " + libriRepo.size() + " libri");
            lblStatus.setText("Dataset ricaricato");
        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Errore nel refresh del dataset:\n" + e.getMessage());
        }
    }

    // ---------------- Detail ----------------

    private void showDetail(Stage ignoredOwner, Book b) {
        if (b == null) return;

        selectedBook = b;
        refreshUserUi();

        dTitle.setText(nvl(b.getTitolo(), "-"));
        dAuthors.setText(b.getAutori() == null ? "-" : String.join(", ", b.getAutori()));

        String meta = "ID " + b.getId() + " • " + (b.getAnno() == null ? "Anno n/d" : b.getAnno());
        dMeta.setText(meta);

        dCategory.setText(nvl(b.getCategoria(), "Categoria n/d"));
        dPublisher.setText(nvl(b.getEditore(), "Editore n/d"));

        try {
            AggregationService.ReviewStats rs = aggregationService.getReviewStats(b.getId());
            dStarsBox.getChildren().clear();

            if (rs == null || rs.count == 0) {
                dAvg.setText("Nessuna valutazione disponibile.");
            } else {
                dAvg.setText("Media voto finale: " + DF1.format(rs.mediaVotoFinale) + "  |  N. valutazioni: " + rs.count);

                dStarsBox.getChildren().addAll(
                        starsRow("Stile", rs.mediaStile),
                        starsRow("Contenuto", rs.mediaContenuto),
                        starsRow("Gradevolezza", rs.mediaGradevolezza),
                        starsRow("Originalità", rs.mediaOriginalita),
                        starsRow("Edizione", rs.mediaEdizione)
                );
            }
        } catch (Exception e) {
            dAvg.setText("Errore aggregazione: " + e.getMessage());
            if (dStarsBox != null) dStarsBox.getChildren().clear();
        }

        try {
            AggregationService.SuggestionsStats ss = aggregationService.getSuggestionsStats(b.getId());
            dSuggestions.getChildren().clear();

            if (ss == null || ss.suggeritiCount == null || ss.suggeritiCount.isEmpty()) {
                Label none = new Label("Nessun consiglio disponibile.");
                none.getStyleClass().add("muted");
                dSuggestions.getChildren().add(none);
            } else {
                int shown = 0;
                for (Integer id : ss.suggeritiCount.keySet()) {
                    Book sb = libriRepo.findById(id);
                    if (sb == null) continue;

                    int count = ss.suggeritiCount.getOrDefault(id, 0);

                    Button link = new Button(sb.getTitolo() + "  (" + count + ")");
                    link.getStyleClass().add("ghost");
                    link.setMaxWidth(Double.MAX_VALUE);
                    link.setOnAction(_ -> {
                        Optional<Book> match = data.stream().filter(x -> x.getId() == sb.getId()).findFirst();
                        match.ifPresentOrElse(
                                m -> tbl.getSelectionModel().select(m),
                                () -> showDetail(ignoredOwner, sb)
                        );
                    });

                    dSuggestions.getChildren().add(link);
                    shown++;
                    if (shown >= 5) break;
                }
            }
        } catch (Exception e) {
            dSuggestions.getChildren().setAll(labelMuted("Errore nel caricamento consigli: " + e.getMessage()));
        }
    }

    private void clearDetail() {
        selectedBook = null;
        refreshUserUi();

        dTitle.setText("-");
        dAuthors.setText("-");
        dMeta.setText("-");
        dCategory.setText("-");
        dPublisher.setText("-");
        dAvg.setText("-");
        if (dStarsBox != null) dStarsBox.getChildren().setAll(labelMuted("Nessuna valutazione disponibile."));
        dSuggestions.getChildren().setAll(labelMuted("Seleziona un libro per vedere i dettagli."));
    }

    // ---------------- Quick actions: Review / Suggestion ----------------

    private void openReviewEditor(Stage owner, Book book) {
        String user = ensureLoggedIn(owner);
        if (user == null || book == null) return;

        Review found = null;
        try {
            for (Review r : reviewService.listByUser(user)) {
                if (r.getBookId() == book.getId()) {
                    found = r;
                    break;
                }
            }
        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Impossibile leggere le tue valutazioni: " + e.getMessage());
            return;
        }

        final Review existing = found;
        final boolean editing = (existing != null);

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle(editing ? "Modifica valutazione" : "Nuova valutazione");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label(editing ? "Aggiorna la tua valutazione" : "Valuta questo libro");
        h.getStyleClass().add("title");
        Label sub = new Label(book.getTitolo());
        sub.getStyleClass().add("subtitle");

        Spinner<Integer> sStile = spinner1to5();
        Spinner<Integer> sCont = spinner1to5();
        Spinner<Integer> sGrad = spinner1to5();
        Spinner<Integer> sOrig = spinner1to5();
        Spinner<Integer> sEdiz = spinner1to5();

        TextArea comment = new TextArea();
        comment.setPromptText("Commento (max 256 caratteri) — opzionale");
        comment.setWrapText(true);

        if (editing) {
            sStile.getValueFactory().setValue(existing.getStile());
            sCont.getValueFactory().setValue(existing.getContenuto());
            sGrad.getValueFactory().setValue(existing.getGradevolezza());
            sOrig.getValueFactory().setValue(existing.getOriginalita());
            sEdiz.getValueFactory().setValue(existing.getEdizione());
            comment.setText(existing.getCommento() == null ? "" : existing.getCommento());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int r = 0;
        grid.add(label("Stile (1-5)"), 0, r); grid.add(sStile, 1, r++);
        grid.add(label("Contenuto (1-5)"), 0, r); grid.add(sCont, 1, r++);
        grid.add(label("Gradevolezza (1-5)"), 0, r); grid.add(sGrad, 1, r++);
        grid.add(label("Originalità (1-5)"), 0, r); grid.add(sOrig, 1, r++);
        grid.add(label("Edizione (1-5)"), 0, r); grid.add(sEdiz, 1, r++);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(170);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        Label hint = labelMuted("Nota: puoi valutare un libro solo se è presente in almeno una tua libreria.");

        Button save = new Button(editing ? "Aggiorna valutazione" : "Salva valutazione");
        save.getStyleClass().add("primary");
        save.setMaxWidth(Double.MAX_VALUE);

        save.setOnAction(_ -> {
            try {
                String comm = comment.getText() == null ? "" : comment.getText().trim();
                if (comm.length() > 256) throw new IllegalArgumentException("Commento troppo lungo (max 256).");

                int stile = sStile.getValue();
                int cont = sCont.getValue();
                int grad = sGrad.getValue();
                int orig = sOrig.getValue();
                int ediz = sEdiz.getValue();

                int votoFinale = (int) Math.round((stile + cont + grad + orig + ediz) / 5.0);

                Review newR = new Review(user, book.getId(), stile, cont, grad, orig, ediz, votoFinale, comm);

                boolean ok = editing ? reviewService.updateReview(newR)
                                    : reviewService.inserisciValutazione(newR);

                if (!ok) {
                    FxUtil.error(owner, "Operazione non riuscita",
                            "Non posso salvare la valutazione.\n" +
                            "Controlla che il libro sia in una tua libreria e che i dati siano validi.");
                    return;
                }

                FxUtil.toast(owner.getScene(), editing ? "Valutazione aggiornata" : "Valutazione salvata");
                showDetail(owner, book);
                d.close();

            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        VBox box = new VBox(12, h, sub, new Separator(), grid, comment, hint, new Separator(), save);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


        private void openSuggestionEditor(Stage owner, Book book) {
        String user = ensureLoggedIn(owner);
        if (user == null || book == null) return;

        // 1) Carico SOLO libri presenti nelle librerie dell’utente (escluso il libro base)
        List<Book> myBooks;
        try {
            Set<Integer> myIds = libraryService.listUserLibraries(user).stream()
                    .map(Library::getBookIds)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            myIds.remove(book.getId());

            // LinkedHashMap per eliminare doppioni per ID
            LinkedHashMap<Integer, Book> map = new LinkedHashMap<>();
            for (Integer id : myIds) {
                Book b = libriRepo.findById(id);
                if (b != null) map.put(b.getId(), b);
            }

            myBooks = new ArrayList<>(map.values());
            myBooks.sort(Comparator.comparing(
                    b -> b.getTitolo() == null ? "" : b.getTitolo(),
                    String.CASE_INSENSITIVE_ORDER
            ));

        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Impossibile leggere le tue librerie: " + e.getMessage());
            return;
        }

        if (myBooks.isEmpty()) {
            FxUtil.error(owner, "Nessun libro selezionabile",
                    "Non hai libri nelle tue librerie da poter consigliare.\n" +
                    "Aggiungi prima qualche libro alla tua libreria e riprova.");
            return;
        }

        // 2) Dialog
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Consiglia libri");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label("Consiglia libri correlati");
        h.getStyleClass().add("title");

        Label sub = new Label("Libro base: " + (book.getTitolo() == null ? "" : book.getTitolo()));
        sub.getStyleClass().add("subtitle");
        sub.setWrapText(true);

        Label hint = labelMuted(
                """
                        Puoi consigliare fino a 3 libri.
                        Mostro solo i libri presenti nelle tue librerie.
                        Suggerimento: scrivi nel campo per filtrare (titolo / ID)."""
        );

        ComboBox<Book> c1 = suggestCombo(myBooks);
        ComboBox<Book> c2 = suggestCombo(myBooks);
        ComboBox<Book> c3 = suggestCombo(myBooks);

        Button save = new Button("Salva consiglio");
        save.getStyleClass().add("primary");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setDisable(true);

        Runnable refreshSave = () -> {
            boolean any = (c1.getValue() != null) || (c2.getValue() != null) || (c3.getValue() != null);
            save.setDisable(!any);
        };

        // anti-duplicati “vero”
        Runnable enforceNoDuplicates = () -> {
            Book b1 = c1.getValue();
            Book b2 = c2.getValue();
            Book b3 = c3.getValue();

            if (b1 != null && b2 != null && b1.getId() == b2.getId()) c2.setValue(null);
            if (b1 != null && b3 != null && b1.getId() == b3.getId()) c3.setValue(null);
            if (b2 != null && b3 != null && b2.getId() == b3.getId()) c3.setValue(null);

            refreshSave.run();
        };

        c1.valueProperty().addListener((ignoreObs, ignoreOld, ignoreN) -> enforceNoDuplicates.run());
        c2.valueProperty().addListener((ignoreObs, ignoreOld, ignoreN) -> enforceNoDuplicates.run());
        c3.valueProperty().addListener((ignoreObs, ignoreOld, ignoreN) -> enforceNoDuplicates.run());

        save.setOnAction(_ -> {
            try {
                LinkedHashSet<Integer> ids = new LinkedHashSet<>();
                if (c1.getValue() != null) ids.add(c1.getValue().getId());
                if (c2.getValue() != null) ids.add(c2.getValue().getId());
                if (c3.getValue() != null) ids.add(c3.getValue().getId());

                if (ids.isEmpty()) {
                    FxUtil.error(owner, "Selezione mancante", "Seleziona almeno 1 libro da consigliare.");
                    return;
                }
                if (ids.contains(book.getId())) {
                    FxUtil.error(owner, "Non valido", "Non puoi consigliare lo stesso libro base.");
                    return;
                }


                Suggestion s = new Suggestion(user, book.getId(), new ArrayList<>(ids));
                boolean ok = suggestionService.inserisciSuggerimento(s);

                if (!ok) {
                    FxUtil.error(owner, "Operazione non riuscita",
                            "Non posso salvare il consiglio.\n" +
                            "Controlla che i libri scelti siano nelle tue librerie e che siano massimo 3.");
                    return;
                }

                aggregationService = new AggregationService(
                        dataDir.resolve("ValutazioniLibri.dati"),
                        dataDir.resolve("ConsigliLibri.dati")
                );

                FxUtil.toast(owner.getScene(), "Consiglio salvato");
                showDetail(owner, book);
                d.close();


            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label("Libro consigliato 1"), 0, 0); grid.add(c1, 1, 0);
        grid.add(label("Libro consigliato 2"), 0, 1); grid.add(c2, 1, 1);
        grid.add(label("Libro consigliato 3"), 0, 2); grid.add(c3, 1, 2);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setMinWidth(170);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);

        VBox box = new VBox(12, h, sub, new Separator(), hint, grid, new Separator(), save);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }

    private ComboBox<Book> suggestCombo(List<Book> source) {
        ObservableList<Book> base = FXCollections.observableArrayList(source);

        FilteredList<Book> filtered = new FilteredList<>(base, ignoreB -> true);

        ComboBox<Book> cb = new ComboBox<>(filtered);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setEditable(true);
        cb.getEditor().setPromptText("Cerca per titolo o ID…");

        cb.setConverter(new StringConverter<>() {
            @Override public String toString(Book b) {
                return (b == null) ? "" : (b.getTitolo() == null ? "" : b.getTitolo());
            }
            @Override public Book fromString(String s) {
                // NON creare Book da testo: serve solo il filtro
                return cb.getValue();
            }
        });

        cb.setCellFactory(ignoreList -> new ListCell<>() {
            @Override protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String title = item.getTitolo() == null ? "" : item.getTitolo();
                setText(title + "  (ID " + item.getId() + ")");
            }
        });

        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("");
                else setText(item.getTitolo() == null ? "" : item.getTitolo());
            }
        });

        // filtro live mentre scrivi (senza mai resettare items -> selezione funziona SEMPRE)
        cb.getEditor().textProperty().addListener((ignoreObs, ignoreOld, txt) -> {
            String q = (txt == null) ? "" : txt.trim().toLowerCase();

            // se l’utente ha selezionato un valore, non ricalcolare mentre il testo è identico al selezionato
            Book sel = cb.getValue();
            if (sel != null) {
                String selTxt = (sel.getTitolo() == null ? "" : sel.getTitolo()).trim().toLowerCase();
                if (q.equals(selTxt)) return;
            }

            if (q.isEmpty()) {
                filtered.setPredicate(ignoreB -> true);
            } else {
                filtered.setPredicate(b -> {
                    String title = (b.getTitolo() == null ? "" : b.getTitolo()).toLowerCase();
                    String id = String.valueOf(b.getId());
                    return title.contains(q) || id.contains(q);
                });
            }

            if (!cb.isShowing()) cb.show();
        });

        return cb;
    }




    private static Spinner<Integer> spinner1to5() {
        Spinner<Integer> sp = new Spinner<>();
        sp.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3));
        sp.setEditable(false);
        return sp;
    }



    // ---------------- Reserved / Auth ----------------

    private void openReservedHome(Stage owner) {
        String user = ensureLoggedIn(owner);
        if (user == null) return;

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Area riservata");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label title = new Label("Area Riservata");
        title.getStyleClass().add("title");
        Label sub = new Label("Gestisci librerie, valutazioni, consigli e account");
        sub.getStyleClass().add("subtitle");

        Button btnLibs = new Button("Le mie librerie");
        btnLibs.getStyleClass().add("primary");
        btnLibs.setMaxWidth(Double.MAX_VALUE);
        btnLibs.setOnAction(_ -> LibrariesWindow.open(authService, libraryService, libriRepo));

        Button btnReviews = new Button("Le mie valutazioni");
        btnReviews.setMaxWidth(Double.MAX_VALUE);
        btnReviews.setOnAction(_ -> ReviewsWindow.open(authService, reviewService, libriRepo));

        Button btnSug = new Button("I miei consigli");
        btnSug.setMaxWidth(Double.MAX_VALUE);
        btnSug.setOnAction(_ -> SuggestionsWindow.open(authService, suggestionService, libriRepo));

        Button btnAcc = new Button("Account");
        btnAcc.setMaxWidth(Double.MAX_VALUE);
        btnAcc.setOnAction(_ -> UserProfileWindow.open(authService));

        VBox box = new VBox(10, title, sub, new Separator(), btnLibs, btnReviews, btnSug, btnAcc);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }

    private void openLogin(Stage owner) {
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Login");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TextField user = new TextField();
        user.setPromptText("Username");

        PasswordManager pr = new PasswordManager("Password");

        Button btn = new Button("Accedi");
        btn.getStyleClass().add("primary");
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(_ -> {
            try {
                boolean ok = authService.login(safe(user.getText()), pr.getText());
                if (!ok) {
                    FxUtil.error(owner, "Login fallito", "Credenziali errate.");
                    return;
                }
                refreshUserUi();
                FxUtil.toast(owner.getScene(), "Benvenuto, " + authService.getCurrentUserid());
                d.close();
            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        VBox box = new VBox(10,
                label("Username"), user,
                label("Password"), pr.getNode(),
                new Separator(),
                btn
        );
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }

    private void openRegister(Stage owner) {
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Registrazione");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TextField nome = new TextField(); nome.setPromptText("Nome");
        TextField cognome = new TextField(); cognome.setPromptText("Cognome");
        TextField cf = new TextField(); cf.setPromptText("Codice fiscale (16 caratteri)");
        TextField email = new TextField(); email.setPromptText("email@dominio.it");
        TextField username = new TextField(); username.setPromptText("Username (5-20)");

        PasswordManager pw = new PasswordManager("Password (min 8, lettere+numeri)");
        PasswordManager pw2 = new PasswordManager("Ripeti password");

        Button btn = new Button("Crea account e accedi");
        btn.getStyleClass().add("primary");
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(_ -> {
            try {
                String n = safe(nome.getText());
                String c = safe(cognome.getText());
                String codice = safe(cf.getText());
                String em = safe(email.getText());
                String u = safe(username.getText());
                String p1 = pw.getText();
                String p2 = pw2.getText();

                if (n.isEmpty() || c.isEmpty() || codice.isEmpty() || em.isEmpty() || u.isEmpty() || p1.isEmpty() || p2.isEmpty())
                    throw new IllegalArgumentException("Tutti i campi sono obbligatori.");
                if (u.length() < 5 || u.length() > 20)
                    throw new IllegalArgumentException("Username deve essere tra 5 e 20 caratteri.");
                if (!CF_RX.matcher(codice).matches())
                    throw new IllegalArgumentException("Codice fiscale non valido (attesi 16 caratteri alfanumerici).");
                if (!EMAIL_RX.matcher(em).matches())
                    throw new IllegalArgumentException("Email non valida.");
                if (!PasswordManager.isStrongPassword(p1))
                    throw new IllegalArgumentException("Password troppo debole (min 8, almeno una lettera e un numero).");
                if (!Objects.equals(p1, p2))
                    throw new IllegalArgumentException("Le password non corrispondono.");

                String hash = AuthService.sha256(p1);

                boolean okReg = authService.registrazione(new User(u, hash, n, c, codice, em));
                if (!okReg) throw new IllegalStateException("Registrazione fallita: username già esistente?");

                boolean okLogin = authService.login(u, p1);
                if (!okLogin) throw new IllegalStateException("Account creato, ma login automatico fallito.");

                refreshUserUi();
                FxUtil.toast(owner.getScene(), "Account creato. Benvenuto, " + u);
                d.close();

                openReservedHome(owner);

            } catch (Exception ex) {
                FxUtil.error(owner, "Registrazione non valida", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int r = 0;
        grid.add(label("Nome"), 0, r); grid.add(nome, 1, r++);
        grid.add(label("Cognome"), 0, r); grid.add(cognome, 1, r++);
        grid.add(label("Codice fiscale"), 0, r); grid.add(cf, 1, r++);
        grid.add(label("Email"), 0, r); grid.add(email, 1, r++);
        grid.add(label("Username"), 0, r); grid.add(username, 1, r++);
        grid.add(label("Password"), 0, r); grid.add(pw.getNode(), 1, r++);
        grid.add(label("Ripeti password"), 0, r); grid.add(pw2.getNode(), 1, r++);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(160);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox box = new VBox(12,
                labelMuted("Dopo la registrazione verrai loggato automaticamente ed entrerai nell’area riservata."),
                grid,
                new Separator(),
                btn
        );
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }

    private String ensureLoggedIn(Stage owner) {
        if (authService.getCurrentUserid() != null) return authService.getCurrentUserid();
        FxUtil.error(owner, "Accesso richiesto", "Devi effettuare il login per accedere a questa funzione.");
        return null;
    }

    // ---------------- Helpers ----------------

    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static String nvl(String s, String def) { return (s == null || s.isBlank()) ? def : s; }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }

    private static Label labelMuted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    private static HBox headerRow(Label left, Node right) {
        HBox row = new HBox(10, left, new Pane(), right);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

}
