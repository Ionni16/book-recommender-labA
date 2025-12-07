package bookrecommender.ui;

import bookrecommender.model.User;
import bookrecommender.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.regex.Pattern;

public class UserProfileWindow extends Stage {

    private final AuthService authService;
    private User user;

    private TextField tfUser;
    private TextField tfNome;
    private TextField tfCognome;
    private TextField tfCF;
    private TextField tfMail;
    private PasswordField pfNewPw;
    private PasswordField pfNewPw2;

    private static final Pattern EMAIL_RX =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CF_RX =
            Pattern.compile("^[A-Za-z0-9]{16}$");

    public UserProfileWindow(AuthService authService) {
        this.authService = authService;

        String userid = authService.getCurrentUserid();
        if (userid == null) {
            new Alert(Alert.AlertType.ERROR,
                    "Nessun utente loggato.", ButtonType.OK).showAndWait();
            close();
            return;
        }

        try {
            this.user = authService.getUser(userid);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossibile caricare i dati dell'utente:\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
            close();
            return;
        }

        setTitle("Profilo utente");
        initModality(Modality.APPLICATION_MODAL);

        // ==== CENTRO: form dati ====
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        tfUser = new TextField(nullSafe(user.getUserid()));
        tfUser.setEditable(false);

        tfNome = new TextField(nullSafe(user.getNome()));
        tfCognome = new TextField(nullSafe(user.getCognome()));
        tfCF = new TextField(nullSafe(user.getCodiceFiscale()));
        tfMail = new TextField(nullSafe(user.getEmail()));

        pfNewPw = new PasswordField();
        pfNewPw2 = new PasswordField();
        pfNewPw.setPromptText("Nuova password (min 8 caratteri, lettere e numeri)");
        pfNewPw2.setPromptText("Ripeti password");

        grid.add(new Label("Userid"), 0, 0); grid.add(tfUser, 1, 0);
        grid.add(new Label("Nome"), 0, 1); grid.add(tfNome, 1, 1);
        grid.add(new Label("Cognome"), 0, 2); grid.add(tfCognome, 1, 2);
        grid.add(new Label("Codice fiscale"), 0, 3); grid.add(tfCF, 1, 3);
        grid.add(new Label("Email"), 0, 4); grid.add(tfMail, 1, 4);
        grid.add(new Label("Nuova password"), 0, 5); grid.add(pfNewPw, 1, 5);
        grid.add(new Label("Ripeti password"), 0, 6); grid.add(pfNewPw2, 1, 6);

        // ==== TOP: titolo ====
        Label title = new Label("Profilo di " + user.getUserid());
        title.getStyleClass().add("title2");
        BorderPane.setMargin(title, new Insets(10, 10, 0, 10));

        // ==== BOTTOM: bottoni ====
        Button btnSave = new Button("Salva modifiche");
        btnSave.getStyleClass().add("button-black");


        Button btnDelete = new Button("Elimina account");
        btnDelete.getStyleClass().add("danger");
        btnDelete.setOnAction(e -> deleteAccount());

        Button btnClose = new Button("Chiudi");
        btnClose.setOnAction(e -> close());

        HBox buttons = new HBox(10, btnSave, btnDelete, btnClose);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10));

        // ==== ROOT: BorderPane ====
        BorderPane root = new BorderPane();
        root.setTop(title);
        root.setCenter(grid);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 520, 360);
        URL css = getClass().getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        else scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");

        setScene(scene);
    }

    private void saveChanges() {
        String email = tfMail.getText().trim();
        String cf = tfCF.getText().trim();

        if (!cf.isEmpty() && !CF_RX.matcher(cf).matches()) {
            alert(Alert.AlertType.WARNING,
                    "Codice fiscale non valido.\nDeve contenere 16 caratteri alfanumerici.");
            return;
        }

        if (!EMAIL_RX.matcher(email).matches()) {
            alert(Alert.AlertType.WARNING, "Email non valida.");
            return;
        }

        String pw1 = pfNewPw.getText();
        String pw2 = pfNewPw2.getText();
        if (!pw1.isEmpty() || !pw2.isEmpty()) {
            if (!pw1.equals(pw2)) {
                alert(Alert.AlertType.WARNING, "Le password non coincidono.");
                return;
            }
            if (!isStrongPassword(pw1)) {
                alert(Alert.AlertType.WARNING,
                        "La password deve contenere almeno 8 caratteri, con lettere e numeri.");
                return;
            }
        }

        user = new User(
                user.getUserid(),
                user.getPasswordHash(),
                tfNome.getText().trim(),
                tfCognome.getText().trim(),
                cf,
                email
        );

        try {
            boolean ok = authService.updateUser(user);
            if (!ok) {
                alert(Alert.AlertType.ERROR, "Impossibile salvare i dati utente.");
                return;
            }
            if (!pw1.isEmpty()) {
                boolean pwOk = authService.updatePassword(user.getUserid(), pw1);
                if (!pwOk) {
                    alert(Alert.AlertType.ERROR,
                            "Dati salvati, ma aggiornamento password fallito.");
                    return;
                }
            }
            alert(Alert.AlertType.INFORMATION, "Profilo aggiornato correttamente.");
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Errore nel salvataggio: " + e.getMessage());
        }
    }

    private void deleteAccount() {
        Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi davvero eliminare il tuo account?\n" +
                        "Le tue librerie, valutazioni e suggerimenti potrebbero essere non più utilizzabili.",
                ButtonType.NO, ButtonType.YES);
        ask.setHeaderText("Conferma eliminazione account");
        ask.showAndWait();
        if (ask.getResult() != ButtonType.YES) return;

        try {
            boolean ok = authService.deleteUser(user.getUserid());
            if (!ok) {
                alert(Alert.AlertType.WARNING, "Impossibile eliminare l'account (non trovato).");
                return;
            }
            authService.logout();
            alert(Alert.AlertType.INFORMATION, "Account eliminato. Verrai disconnesso.");
            close();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Errore nell'eliminazione account: " + e.getMessage());
        }
    }

    private static boolean isStrongPassword(String pw) {
        if (pw == null || pw.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : pw.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public static void open(AuthService authService) {
        UserProfileWindow w = new UserProfileWindow(authService);
        w.show();
    }
}
