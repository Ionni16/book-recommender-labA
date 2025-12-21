package bookrecommender.ui;

import bookrecommender.model.User;
import bookrecommender.service.AuthService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

public class UserProfileWindow extends Stage {

    private final AuthService authService;

    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public UserProfileWindow(AuthService authService) {
        this.authService = authService;

        setTitle("Account");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        Scene scene = new Scene(new StackPane(root), 820, 560);

        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        setScene(scene);
    }

    private Node buildHeader() {
        Label h = new Label("Account");
        h.getStyleClass().add("title");

        Label sub = new Label("Gestisci email, password e sicurezza dell’account");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, h, sub);
        box.getStyleClass().add("appbar");
        return box;
    }

    private Node buildCenter() {
        String userId = authService.getCurrentUserid();
        if (userId == null) {
            VBox v = new VBox(10, new Label("Login richiesto."));
            v.getStyleClass().add("card");
            v.setPadding(new Insets(14));
            BorderPane wrap = new BorderPane(v);
            wrap.setPadding(new Insets(14));
            return wrap;
        }

        User u;
        try {
            u = authService.getUser(userId);
        } catch (Exception e) {
            FxUtil.error(this, "Errore", "Impossibile leggere i dati utente: " + e.getMessage());
            u = null;
        }

        VBox page = new VBox(14);
        page.setPadding(new Insets(14));
        page.setFillWidth(true);

        // ---- Card: dati personali ----
        VBox info = new VBox(8);
        info.getStyleClass().add("card2");
        info.setPadding(new Insets(14));

        Label t1 = new Label("Dati personali");
        t1.getStyleClass().add("card-title");

        Label nome = muted("Nome: " + (u == null ? "-" : safe(u.getNome())));
        Label cognome = muted("Cognome: " + (u == null ? "-" : safe(u.getCognome())));
        Label cf = muted("Codice Fiscale: " + (u == null ? "-" : safe(u.getCodiceFiscale())));
        Label user = muted("Username: " + (u == null ? "-" : safe(u.getUserid())));

        info.getChildren().addAll(t1, nome, cognome, cf, user);

        // ---- Card: email ----
        VBox emailCard = new VBox(10);
        emailCard.getStyleClass().add("card");
        emailCard.setPadding(new Insets(14));

        Label t2 = new Label("Email");
        t2.getStyleClass().add("card-title");
        Label t2sub = muted("Aggiorna la tua email di contatto. Verrà salvata subito.");

        TextField email = new TextField(u == null ? "" : safe(u.getEmail()));
        email.setPromptText("email@dominio.it");

        Button saveEmail = new Button("Aggiorna Email");
        saveEmail.getStyleClass().add("primary");
        saveEmail.setOnAction(e -> {
            try {
                String em = safe(email.getText());
                if (!EMAIL_RX.matcher(em).matches())
                    throw new IllegalArgumentException("Email non valida.");

                User current = authService.getUser(userId);
                User updated = new User(
                        current.getUserid(),
                        current.getPasswordHash(),
                        current.getNome(),
                        current.getCognome(),
                        current.getCodiceFiscale(),
                        em
                );

                boolean ok = authService.updateUser(updated);
                if (!ok) throw new IllegalStateException("Aggiornamento fallito.");
                FxUtil.toast(getScene(), "Email aggiornata");
            } catch (Exception ex) {
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });

        HBox emailActions = new HBox(saveEmail);
        emailActions.setAlignment(Pos.CENTER_RIGHT);

        emailCard.getChildren().addAll(t2, t2sub, email, emailActions);

        // ---- Card: password ----
        VBox pwCard = new VBox(10);
        pwCard.getStyleClass().add("card");
        pwCard.setPadding(new Insets(14));

        Label t3 = new Label("Password");
        t3.getStyleClass().add("card-title");
        Label t3sub = muted("Scegli una password forte (min 8 caratteri).");

        PasswordReveal pw1 = PasswordReveal.create("Nuova password");
        PasswordReveal pw2 = PasswordReveal.create("Ripeti nuova password");

        Button savePw = new Button("Cambia Password");
        savePw.getStyleClass().add("primary");
        savePw.setOnAction(e -> {
            try {
                String a = pw1.getText();
                String b = pw2.getText();
                if (a == null || a.isBlank() || b == null || b.isBlank())
                    throw new IllegalArgumentException("Inserisci entrambe le password.");
                if (!Objects.equals(a, b))
                    throw new IllegalArgumentException("Le password non corrispondono.");
                if (a.length() < 8)
                    throw new IllegalArgumentException("Password troppo corta (min 8).");

                boolean ok = authService.updatePassword(userId, a);
                if (!ok) throw new IllegalStateException("Password non aggiornata.");

                pw1.clear();
                pw2.clear();
                FxUtil.toast(getScene(), "Password aggiornata");
            } catch (Exception ex) {
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });

        HBox pwActions = new HBox(savePw);
        pwActions.setAlignment(Pos.CENTER_RIGHT);

        pwCard.getChildren().addAll(t3, t3sub, pw1.getNode(), pw2.getNode(), pwActions);

        // ---- Card: danger zone ----
        VBox danger = new VBox(10);
        danger.getStyleClass().add("card2");
        danger.setPadding(new Insets(14));

        Label t4 = new Label("Zona pericolosa");
        t4.getStyleClass().add("card-title");
        Label t4sub = muted("Elimina definitivamente l’account. Operazione irreversibile.");

        Button deleteAcc = new Button("Elimina account");
        deleteAcc.getStyleClass().add("danger");
        deleteAcc.setOnAction(e -> {
            if (!FxUtil.confirm(this, "Conferma", "Eliminare definitivamente l'account? Operazione irreversibile."))
                return;
            try {
                boolean ok = authService.deleteUser(userId);
                if (!ok) throw new IllegalStateException("Eliminazione fallita.");
                authService.logout();
                FxUtil.info(this, "Account eliminato", "L’account è stato eliminato e sei stato disconnesso.");
                close();
            } catch (Exception ex) {
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });

        HBox dangerActions = new HBox(deleteAcc);
        dangerActions.setAlignment(Pos.CENTER_RIGHT);

        danger.getChildren().addAll(t4, t4sub, dangerActions);

        page.getChildren().addAll(info, emailCard, pwCard, danger);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // ✅ classe CSS che rende trasparente anche la viewport
        sp.getStyleClass().add("scroll-clean");

        // ✅ fallback: forza viewport trasparente via codice (alcune versioni JavaFX ignorano il CSS)
        Platform.runLater(() -> {
            Node viewport = sp.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: transparent;");
            }
        });

        return sp;
    }

    private Node buildFooter() {
        Label hint = new Label("Suggerimento: usa l’icona 👁 per vedere la password mentre scrivi.");
        hint.getStyleClass().add("muted");

        Button close = new Button("Chiudi");
        close.getStyleClass().add("ghost");
        close.setOnAction(e -> close());

        HBox bar = new HBox(10, hint, new Pane(), close);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // Password reveal component
    private static final class PasswordReveal {
        private final PasswordField pf = new PasswordField();
        private final TextField tf = new TextField();
        private final Button eye = new Button("👁");
        private final HBox root;

        private PasswordReveal(String prompt) {
            pf.setPromptText(prompt);
            tf.setPromptText(prompt);

            tf.textProperty().bindBidirectional(pf.textProperty());

            tf.setVisible(false);
            tf.setManaged(false);

            eye.getStyleClass().addAll("ghost", "icon");
            eye.setFocusTraversable(false);

            eye.setOnAction(e -> {
                boolean showing = tf.isVisible();
                if (showing) {
                    tf.setVisible(false); tf.setManaged(false);
                    pf.setVisible(true);  pf.setManaged(true);
                } else {
                    pf.setVisible(false); pf.setManaged(false);
                    tf.setVisible(true);  tf.setManaged(true);
                }
            });

            StackPane stack = new StackPane(pf, tf);
            HBox.setHgrow(stack, Priority.ALWAYS);

            root = new HBox(10, stack, eye);
            root.setAlignment(Pos.CENTER_LEFT);
        }

        static PasswordReveal create(String prompt) { return new PasswordReveal(prompt); }
        Node getNode() { return root; }
        String getText() { return pf.getText(); }
        void clear() { pf.clear(); }
    }

    public static void open(AuthService authService) {
        UserProfileWindow w = new UserProfileWindow(authService);
        w.showAndWait();
    }
}
