package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Library;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.LibraryService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Finestra per la gestione delle librerie dell'utente:
 * - visualizza le librerie
 * - rinomina una libreria
 * - elimina una libreria
 * - rimuove libri da una libreria
 */
public class LibrariesWindow extends Stage {

    private final AuthService authService;
    private final LibraryService libraryService;
    private final LibriRepository libriRepo;

    private final ObservableList<Library> libsData = FXCollections.observableArrayList();
    private final ObservableList<Book> booksData = FXCollections.observableArrayList();

    private TableView<Library> tblLibs;
    private TableView<Book> tblBooks;
    private Label lblUser;

    public LibrariesWindow(AuthService authService,
                           LibraryService libraryService,
                           LibriRepository libriRepo) {
        this.authService = authService;
        this.libraryService = libraryService;
        this.libriRepo = libriRepo;

        setTitle("Gestione librerie personali");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, 900, 500);

        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        setScene(scene);
        loadLibraries();
    }

    private VBox buildHeader() {
        lblUser = new Label();
        lblUser.getStyleClass().add("title2");

        Label subt = new Label("Visualizza, rinomina ed elimina le tue librerie");
        subt.getStyleClass().add("subtitle");

        VBox box = new VBox(2, lblUser, subt);
        box.setPadding(new Insets(10));
        return box;
    }

    private HBox buildCenter() {
        // tabella librerie
        tblLibs = new TableView<>(libsData);
        tblLibs.setPlaceholder(new Label("Nessuna libreria trovata."));

        TableColumn<Library, String> colNome = new TableColumn<>("Nome libreria");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setPrefWidth(250);

        TableColumn<Library, Number> colSize = new TableColumn<>("Numero libri");
        colSize.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getBookIds().size()));
        colSize.setPrefWidth(120);

        tblLibs.getColumns().addAll(colNome, colSize);
        tblLibs.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> loadBooksOf(n));

        VBox left = new VBox(new Label("Le mie librerie"), tblLibs);
        left.setSpacing(4);
        left.setPadding(new Insets(10));
        VBox.setVgrow(tblLibs, Priority.ALWAYS);

        // tabella libri nella libreria selezionata
        tblBooks = new TableView<>(booksData);
        tblBooks.setPlaceholder(new Label("Seleziona una libreria per vedere i libri."));

        TableColumn<Book, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colId.setPrefWidth(70);

        TableColumn<Book, String> colTitolo = new TableColumn<>("Titolo");
        colTitolo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitolo()));
        colTitolo.setPrefWidth(320);

        tblBooks.getColumns().addAll(colId, colTitolo);

        Button btnRemoveBook = new Button("Rimuovi libro dalla libreria");
        btnRemoveBook.setOnAction(e -> removeBookFromLibrary());
        btnRemoveBook.disableProperty().bind(tblBooks.getSelectionModel().selectedItemProperty().isNull());

        VBox right = new VBox(new Label("Libri nella libreria selezionata"),
                tblBooks,
                new HBox(8, btnRemoveBook));
        right.setSpacing(4);
        right.setPadding(new Insets(10));
        VBox.setVgrow(tblBooks, Priority.ALWAYS);

        // pulsanti gestione librerie
        Button btnRename = new Button("Rinomina libreria");
        btnRename.setOnAction(e -> renameLibrary());
        btnRename.disableProperty().bind(tblLibs.getSelectionModel().selectedItemProperty().isNull());

        Button btnDelete = new Button("Elimina libreria");
        btnDelete.getStyleClass().add("danger");
        btnDelete.setOnAction(e -> deleteLibrary());
        btnDelete.disableProperty().bind(tblLibs.getSelectionModel().selectedItemProperty().isNull());

        VBox leftWithButtons = new VBox(left, new HBox(8, btnRename, btnDelete));
        leftWithButtons.setSpacing(6);

        HBox center = new HBox(10, leftWithButtons, right);
        HBox.setHgrow(leftWithButtons, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return center;
    }

    private HBox buildFooter() {
        Button btnClose = new Button("Chiudi");
        btnClose.setOnAction(e -> close());
        HBox box = new HBox(btnClose);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(8, 12, 8, 12));
        return box;
    }

    private void loadLibraries() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            lblUser.setText("Nessun utente loggato");
            libsData.clear();
            booksData.clear();
            return;
        }
        lblUser.setText("Librerie di: " + user);
        try {
            List<Library> libs = libraryService.listUserLibraries(user);
            libsData.setAll(libs);
            if (!libs.isEmpty()) {
                tblLibs.getSelectionModel().select(0);
            } else {
                booksData.clear();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nel caricamento delle librerie:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private void loadBooksOf(Library lib) {
        booksData.clear();
        if (lib == null) return;

        List<Book> bs = lib.getBookIds().stream()
                .map(libriRepo::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        booksData.setAll(bs);
    }

    private void renameLibrary() {
        Library selected = tblLibs.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dlg = new TextInputDialog(selected.getNome());
        dlg.setTitle("Rinomina libreria");
        dlg.setHeaderText("Nuovo nome per la libreria");
        dlg.setContentText("Nome:");

        String inserito = dlg.showAndWait().orElse(null);
        if (inserito == null) return;
        final String nuovoNome = inserito.trim();
        if (nuovoNome.isEmpty()) return;

        try {
            List<Library> libs = libraryService.listUserLibraries(selected.getUserid());
            boolean esisteGia = libs.stream()
                    .anyMatch(l -> l.getNome().equalsIgnoreCase(nuovoNome)
                            && l != selected);
            if (esisteGia) {
                new Alert(Alert.AlertType.WARNING,
                        "Hai già una libreria con questo nome.",
                        ButtonType.OK).showAndWait();
                return;
            }

            Library rinominata = new Library(
                    selected.getUserid(),
                    nuovoNome,
                    new LinkedHashSet<>(selected.getBookIds())
            );

            // salvo la nuova libreria e cancello la vecchia
            libraryService.saveLibrary(rinominata);
            libraryService.deleteLibrary(selected.getUserid(), selected.getNome());

            loadLibraries();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nella rinomina della libreria:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private void deleteLibrary() {
        Library selected = tblLibs.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi davvero eliminare la libreria \"" + selected.getNome() + "\"?\n" +
                        "I libri nel repository NON vengono cancellati, si elimina solo la raccolta personale.",
                ButtonType.NO, ButtonType.YES);
        ask.setHeaderText("Conferma eliminazione libreria");
        ask.showAndWait();
        if (ask.getResult() != ButtonType.YES) return;

        try {
            boolean ok = libraryService.deleteLibrary(selected.getUserid(), selected.getNome());
            if (!ok) {
                new Alert(Alert.AlertType.WARNING,
                        "Impossibile eliminare la libreria (non trovata).",
                        ButtonType.OK).showAndWait();
            }
            loadLibraries();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nell'eliminazione della libreria:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    private void removeBookFromLibrary() {
        Library lib = tblLibs.getSelectionModel().getSelectedItem();
        Book book = tblBooks.getSelectionModel().getSelectedItem();
        if (lib == null || book == null) return;

        Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Rimuovere \"" + book.getTitolo() + "\" dalla libreria \"" + lib.getNome() + "\"?",
                ButtonType.NO, ButtonType.YES);
        ask.setHeaderText("Conferma rimozione libro");
        ask.showAndWait();
        if (ask.getResult() != ButtonType.YES) return;

        try {
            Set<Integer> ids = new LinkedHashSet<>(lib.getBookIds());
            ids.remove(book.getId());
            Library aggiornata = new Library(lib.getUserid(), lib.getNome(), ids);
            libraryService.saveLibrary(aggiornata);
            loadLibraries();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore nella rimozione del libro:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    // helper statico per aprire la finestra dalla GUI principale
    public static void open(AuthService authService,
                            LibraryService libraryService,
                            LibriRepository libriRepo) {
        LibrariesWindow w = new LibrariesWindow(authService, libraryService, libriRepo);
        w.show();
    }
}
