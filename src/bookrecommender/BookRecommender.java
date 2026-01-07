/*
 * Nome: Ionut
 * Cognome: Puiu
 * Matricola: 758296
 * Sede: VA
 *
 * Nome: Matteo
 * Cognome: Ferrario
 * Matricola: 756147
 * Sede: VA
 */

package bookrecommender;

import bookrecommender.model.*;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.*;
import bookrecommender.ui.BookRecommenderFX;

import javafx.application.Application;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Classe principale dell'applicazione <b>Book Recommender</b> (Lab A).
 * <p>
 * La classe funge da <i>entry point</i> del progetto (come richiesto dalle specifiche)
 * e consente di avviare l'applicazione in due modalità:
 * </p>
 * <ul>
 *   <li><b>GUI (JavaFX)</b>: avvio di default senza argomenti.</li>
 *   <li><b>CLI (console)</b>: avvio passando l'argomento {@code --cli}.</li>
 * </ul>
 *
 * <h2>Persistenza dati</h2>
 * <p>
 * I dati sono letti/scritti nella cartella {@code data} tramite file {@code .dati}:
 * {@code Libri.dati}, {@code UtentiRegistrati.dati}, {@code Librerie.dati},
 * {@code ValutazioniLibri.dati}, {@code ConsigliLibri.dati}.
 * </p>
 *
 * @author Ionut Puiu, Matteo Ferrario
 * @version 1.0
 */
public class BookRecommender {

    /**
     * Metodo principale dell'applicazione.
     * <p>
     * Se tra gli argomenti è presente {@code --cli}, avvia la modalità console.
     * In caso contrario avvia l'interfaccia grafica JavaFX.
     * </p>
     *
     * @param args argomenti da riga di comando; usare {@code --cli} per forzare la modalità testuale
     */
    public static void main(String[] args) {
        if (hasArg(args, "--cli")) {
            runCli();
            return;
        }
        Application.launch(BookRecommenderFX.class, args);
    }

    /**
     * Verifica se tra gli argomenti è presente un flag.
     *
     * @param args array di argomenti; può essere {@code null}
     * @param flag flag da cercare (es. {@code "--cli"})
     * @return {@code true} se il flag è presente (case-insensitive), {@code false} altrimenti
     */
    private static boolean hasArg(String[] args, String flag) {
        if (args == null) return false;
        for (String a : args) {
            if (flag.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    /**
     * Avvia la modalità testuale (CLI) dell'applicazione.
     * <p>
     * Esegue il caricamento del repository dei libri da {@code data/Libri.dati},
     * inizializza i servizi applicativi e mostra un menu ciclico finché l'utente
     * non seleziona l'uscita.
     * </p>
     * <p>
     * In caso di errore nel caricamento del repository libri, stampa un messaggio su
     * {@code stderr} e termina la CLI.
     * </p>
     */
    public static void runCli() {
        Scanner in = new Scanner(System.in);
        Path dataDir = Path.of("data");

        // Repo libri (read-only)
        LibriRepository libriRepo = new LibriRepository(dataDir.resolve("Libri.dati"));
        try {
            libriRepo.load();
        } catch (Exception e) {
            System.err.println("Errore nel caricamento di Libri.dati: " + e.getMessage());
            return;
        }

        System.out.println("=== Book Recommender (Lab A) ===");
        System.out.println("Libri caricati: " + libriRepo.size());

        // Servizi
        SearchService search = new SearchService(libriRepo);
        AuthService auth = new AuthService(dataDir.resolve("UtentiRegistrati.dati"));
        LibraryService libraryService = new LibraryService(dataDir.resolve("Librerie.dati"));
        ReviewService reviewService = new ReviewService(
                dataDir.resolve("ValutazioniLibri.dati"),
                dataDir.resolve("Librerie.dati")
        );
        SuggestionService suggestionService = new SuggestionService(
                dataDir.resolve("ConsigliLibri.dati"),
                dataDir.resolve("Librerie.dati")
        );
        AggregationService agg = new AggregationService(
                dataDir.resolve("ValutazioniLibri.dati"),
                dataDir.resolve("ConsigliLibri.dati")
        );

        while (true) {
            System.out.println();
            System.out.println("Menu:");
            System.out.println("1) Cerca per titolo");
            System.out.println("2) Cerca per autore");
            System.out.println("3) Cerca per autore e anno");
            System.out.println("4) Registrazione");
            System.out.println("5) Login");
            System.out.println("6) Crea/Aggiorna Libreria (richiede login)");
            System.out.println("7) Inserisci Valutazione (richiede login)");
            System.out.println("8) Inserisci Suggerimenti (richiede login)");
            System.out.println("9) Visualizza Dettagli Libro (aggregati)");
            System.out.println("L) Logout");
            System.out.println("0) Esci");
            System.out.print("Scelta: ");
            String choice = in.nextLine().trim();

            switch (choice) {
                case "1": doSearchByTitle(in, search); break;
                case "2": doSearchByAuthor(in, search); break;
                case "3": doSearchByAuthorYear(in, search); break;
                case "4": doRegister(in, auth); break;
                case "5": doLogin(in, auth); break;
                case "6": doLibrary(in, auth, libraryService); break;
                case "7": doReview(in, auth, reviewService); break;
                case "8": doSuggest(in, auth, suggestionService); break;
                case "9": doVisualizza(in, search, agg, libriRepo); break;
                case "L":
                case "l":
                    auth.logout();
                    System.out.println("Logout eseguito.");
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Scelta non valida.");
            }
        }
    }

    // === Cerca ===

    /**
     * Gestisce la ricerca dei libri per titolo (sottostringa, case-insensitive).
     *
     * @param in scanner per leggere l'input da console
     * @param search servizio di ricerca sui libri
     */
    private static void doSearchByTitle(Scanner in, SearchService search) {
        System.out.print("Titolo (sottostringa): ");
        printBooks(search.cercaLibroPerTitolo(in.nextLine()));
    }

    /**
     * Gestisce la ricerca dei libri per autore (sottostringa, case-insensitive).
     *
     * @param in scanner per leggere l'input da console
     * @param search servizio di ricerca sui libri
     */
    private static void doSearchByAuthor(Scanner in, SearchService search) {
        System.out.print("Autore (sottostringa): ");
        printBooks(search.cercaLibroPerAutore(in.nextLine()));
    }

    /**
     * Gestisce la ricerca dei libri per autore e anno.
     * <p>
     * Se l'anno inserito non è un intero valido, stampa un messaggio e annulla l'operazione.
     * </p>
     *
     * @param in scanner per leggere l'input da console
     * @param search servizio di ricerca sui libri
     */
    private static void doSearchByAuthorYear(Scanner in, SearchService search) {
        System.out.print("Autore: ");
        String a = in.nextLine();
        System.out.print("Anno (es. 1999): ");
        try {
            int anno = Integer.parseInt(in.nextLine().trim());
            printBooks(search.cercaLibroPerAutoreEAnno(a, anno));
        } catch (NumberFormatException e) {
            System.out.println("Anno non valido.");
        }
    }

    // === Registrazione/Login ===

    /**
     * Gestisce la procedura di registrazione di un nuovo utente.
     * <p>
     * Esegue controlli di validità su:
     * </p>
     * <ul>
     *   <li>userid e password non vuoti</li>
     *   <li>formato email tramite regex</li>
     *   <li>robustezza password (min 8 caratteri, almeno una lettera e un numero)</li>
     * </ul>
     * <p>
     * La password viene salvata come hash (SHA-256) tramite {@link AuthService#sha256(String)}.
     * </p>
     *
     * @param in scanner per leggere l'input da console
     * @param auth servizio di autenticazione/registrazione
     */
    private static void doRegister(Scanner in, AuthService auth) {
        System.out.println("=== Registrazione ===");
        System.out.print("Userid: "); String userid = in.nextLine().trim();
        System.out.print("Password: "); String pass = in.nextLine();
        System.out.print("Nome: "); String nome = in.nextLine().trim();
        System.out.print("Cognome: "); String cognome = in.nextLine().trim();
        System.out.print("Codice Fiscale: "); String cf = in.nextLine().trim();
        System.out.print("Email: "); String email = in.nextLine().trim();

        if (userid.isEmpty() || pass.isEmpty()) {
            System.out.println("Userid e password sono obbligatori.");
            return;
        }
        if (!EMAIL_RX.matcher(email).matches()) {
            System.out.println("Email non valida.");
            return;
        }
        if (!isStrongPassword(pass)) {
            System.out.println("Password troppo debole. Minimo 8 caratteri, con lettere e numeri.");
            return;
        }

        try {
            String hash = AuthService.sha256(pass);
            User u = new User(userid, hash, nome, cognome, cf, email);
            boolean ok = auth.registrazione(u);
            System.out.println(ok
                    ? "Registrazione completata."
                    : "Registrazione fallita (userid già esistente?).");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    /**
     * Gestisce la procedura di login.
     *
     * @param in scanner per leggere l'input da console
     * @param auth servizio di autenticazione
     */
    private static void doLogin(Scanner in, AuthService auth) {
        System.out.println("=== Login ===");
        System.out.print("Userid: "); String userid = in.nextLine().trim();
        System.out.print("Password: "); String pass = in.nextLine();
        try {
            boolean ok = auth.login(userid, pass);
            System.out.println(ok ? "Login OK." : "Credenziali errate.");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Librerie ===

    /**
     * Gestisce creazione/aggiornamento delle librerie personali dell'utente loggato.
     * <p>
     * Se l'utente non è autenticato, l'operazione viene bloccata.
     * </p>
     *
     * @param in scanner per leggere l'input da console
     * @param auth servizio di autenticazione (per verificare l'utente corrente)
     * @param libraryService servizio di gestione librerie
     */
    private static void doLibrary(Scanner in, AuthService auth, LibraryService libraryService) {
        String me = auth.getCurrentUserid();
        if (me == null) { System.out.println("Devi fare login."); return; }

        try {
            System.out.println("=== Librerie di " + me + " ===");
            var libs = libraryService.listUserLibraries(me);
            if (libs.isEmpty()) System.out.println("(nessuna libreria)");
            else libs.forEach(L -> System.out.println("- " + L.getNome() + " -> " + L.getBookIds().size() + " libri"));

            System.out.print("Nome libreria da creare/aggiornare: ");
            String nome = in.nextLine().trim();

            System.out.print("Inserisci idLibro separati da virgola (es: 10,25,31): ");
            String line = in.nextLine().trim();
            Set<Integer> ids = parseIdSet(line);

            Library lib = new Library(me, nome, ids);
            boolean ok = libraryService.saveLibrary(lib);
            System.out.println(ok ? "Libreria salvata." : "Errore nel salvataggio.");

        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Valutazioni ===

    /**
     * Gestisce l'inserimento di una valutazione per un libro da parte dell'utente loggato.
     * <p>
     * Richiede l'autenticazione. I punteggi devono essere compresi tra 1 e 5.
     * Il voto finale viene calcolato tramite {@link Review#calcolaVotoFinale(int, int, int, int, int)}.
     * </p>
     *
     * @param in scanner per leggere l'input da console
     * @param auth servizio di autenticazione
     * @param reviewService servizio di gestione valutazioni
     */
    private static void doReview(Scanner in, AuthService auth, ReviewService reviewService) {
        String me = auth.getCurrentUserid();
        if (me == null) { System.out.println("Devi fare login."); return; }

        try {
            System.out.print("idLibro da valutare: ");
            int id = Integer.parseInt(in.nextLine().trim());
            int stile = askInt(in, "Stile (1..5): ");
            int contenuto = askInt(in, "Contenuto (1..5): ");
            int gradev = askInt(in, "Gradevolezza (1..5): ");
            int orig = askInt(in, "Originalità (1..5): ");
            int ed = askInt(in, "Edizione (1..5): ");
            System.out.print("Commento (max 256, opzionale): ");
            String comm = in.nextLine();
            if (comm != null && comm.length() > 256) { System.out.println("Commento troppo lungo."); return; }

            int votoFinale = Review.calcolaVotoFinale(stile, contenuto, gradev, orig, ed);
            Review r = new Review(me, id, stile, contenuto, gradev, orig, ed, votoFinale, comm);
            boolean ok = reviewService.inserisciValutazione(r);
            System.out.println(ok ? "Valutazione registrata." : "Impossibile registrare (controlla che il libro sia nella tua libreria).");

        } catch (NumberFormatException nfe) {
            System.out.println("idLibro non valido.");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Suggerimenti ===

    /**
     * Gestisce l'inserimento di suggerimenti (massimo 3) per un libro,
     * da parte dell'utente loggato.
     *
     * @param in scanner per leggere l'input da console
     * @param auth servizio di autenticazione
     * @param suggestionService servizio di gestione suggerimenti
     */
    private static void doSuggest(Scanner in, AuthService auth, SuggestionService suggestionService) {
        String me = auth.getCurrentUserid();
        if (me == null) { System.out.println("Devi fare login."); return; }

        try {
            System.out.print("idLibro di riferimento: ");
            int id = Integer.parseInt(in.nextLine().trim());
            System.out.print("Suggerisci fino a 3 idLibro (es: 101,202,303): ");
            List<Integer> list = parseIdList(in.nextLine().trim());
            Suggestion s = new Suggestion(me, id, list);
            boolean ok = suggestionService.inserisciSuggerimento(s);
            System.out.println(ok ? "Suggerimento registrato." : "Impossibile registrare (max 3, no duplicati/self, e libri nella tua libreria).");

        } catch (NumberFormatException nfe) {
            System.out.println("id non valido.");
        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Visualizza Dettagli ===

    /**
     * Gestisce la visualizzazione dei dettagli di un libro:
     * <ul>
     *   <li>dati bibliografici dal repository libri</li>
     *   <li>statistiche aggregate delle valutazioni</li>
     *   <li>statistiche aggregate dei suggerimenti</li>
     * </ul>
     *
     * @param in scanner per leggere l'input da console
     * @param search servizio di ricerca
     * @param agg servizio di aggregazione valutazioni/suggerimenti
     * @param libriRepo repository libri
     */
    private static void doVisualizza(Scanner in, SearchService search, AggregationService agg, LibriRepository libriRepo) {
        System.out.print("Cerca titolo per scegliere il libro: ");
        var results = search.cercaLibroPerTitolo(in.nextLine());
        if (results.isEmpty()) { System.out.println("Nessun risultato."); return; }
        printBooks(results);
        System.out.print("Inserisci idLibro da visualizzare: ");
        try {
            int id = Integer.parseInt(in.nextLine().trim());
            var opt = libriRepo.all().stream().filter(b -> b.getId() == id).findFirst();
            if (opt.isEmpty()) { System.out.println("idLibro non trovato."); return; }
            Book b = opt.get();

            System.out.println("\n=== Dettagli Libro ===");
            System.out.println("[" + b.getId() + "] " + b.getTitolo());
            System.out.println("Autori: " + String.join(", ", b.getAutori()));
            System.out.println("Anno: " + (b.getAnno() == null ? "" : b.getAnno()));
            System.out.println("Editore: " + (b.getEditore() == null ? "" : b.getEditore()));
            System.out.println("Categoria: " + (b.getCategoria() == null ? "" : b.getCategoria()));

            var rs = agg.getReviewStats(b.getId());
            System.out.println("\n-- Valutazioni --");
            System.out.println("Numero valutazioni: " + rs.count);
            if (rs.count > 0) {
                System.out.printf(Locale.ROOT,
                        "Medie -> Stile: %.2f, Contenuto: %.2f, Gradevolezza: %.2f, Originalità: %.2f, Edizione: %.2f%n",
                        rs.mediaStile, rs.mediaContenuto, rs.mediaGradevolezza, rs.mediaOriginalita, rs.mediaEdizione);
                System.out.println("Distribuzione voto finale: " + rs.distribuzioneVoti);
            }

            var ss = agg.getSuggestionsStats(b.getId());
            System.out.println("\n-- Suggerimenti (idLibro -> conteggio utenti) --");
            if (ss.suggeritiCount.isEmpty()) System.out.println("(nessuno)");
            else {
                String line = ss.suggeritiCount.entrySet().stream()
                        .map(e -> e.getKey() + "->" + e.getValue())
                        .collect(Collectors.joining(", "));
                System.out.println(line);
            }

        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

    // === Utils stampa e parsing ===

    /**
     * Stampa a console una lista di libri (massimo 20) nel formato:
     * {@code [id] titolo (anno) | Autori: ...}
     *
     * @param results lista di risultati da stampare
     */
    private static void printBooks(List<Book> results) {
        if (results.isEmpty()) { System.out.println("Nessun risultato."); return; }
        System.out.println("Trovati " + results.size() + " risultati:");
        for (int i = 0; i < Math.min(results.size(), 20); i++) {
            Book b = results.get(i);
            System.out.printf("- [%d] %s (%s) | Autori: %s%n",
                    b.getId(),
                    b.getTitolo(),
                    b.getAnno() == null ? "" : b.getAnno().toString(),
                    String.join(", ", b.getAutori()));
        }
        if (results.size() > 20) System.out.println("... (mostrati i primi 20)");
    }

    /**
     * Converte una stringa CSV di interi (separati da virgola) in un {@link Set}.
     * Gli elementi non numerici vengono ignorati.
     *
     * @param csv stringa in input (es. {@code "10,25,31"})
     * @return insieme (ordinato per inserimento) degli id presenti
     */
    private static Set<Integer> parseIdSet(String csv) {
        Set<Integer> out = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) return out;
        for (String p : csv.split(",")) {
            try { out.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    /**
     * Converte una stringa CSV di interi (separati da virgola) in una {@link List}.
     * Gli elementi non numerici vengono ignorati.
     *
     * @param csv stringa in input (es. {@code "101,202,303"})
     * @return lista degli id presenti (anche vuota)
     */
    private static List<Integer> parseIdList(String csv) {
        List<Integer> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String p : csv.split(",")) {
            try { out.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    /**
     * Legge un intero da console e verifica che sia compreso tra 1 e 5.
     *
     * @param in scanner per leggere l'input
     * @param prompt testo mostrato all'utente
     * @return valore inserito (1..5)
     * @throws NumberFormatException se il valore non è valido o fuori range
     */
    private static int askInt(Scanner in, String prompt) {
        System.out.print(prompt);
        int x = Integer.parseInt(in.nextLine().trim());
        if (x < 1 || x > 5) throw new NumberFormatException();
        return x;
    }

    /**
     * Verifica robustezza minima della password:
     * lunghezza >= 8, almeno una lettera e almeno una cifra.
     *
     * @param pass password in chiaro
     * @return {@code true} se la password soddisfa i requisiti, {@code false} altrimenti
     */
    private static boolean isStrongPassword(String pass) {
        if (pass == null) return false;
        if (pass.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : pass.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    /**
     * Pattern regex minimale per validazione email (non esaustiva).
     */
    private static final Pattern EMAIL_RX =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
}
