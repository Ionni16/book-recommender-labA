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

package bookrecommender.repo;

import bookrecommender.model.Book;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * La classe <code>LibriRepository</code> gestisce il caricamento e
 * la persistenza dei libri dell'applicazione.
 * <p>
 * I dati vengono memorizzati principalmente nel file di testo
 * <code>Libri.dati</code>, in un formato semplificato con campi separati
 * da punto e virgola. Se il file non è presente, il repository è in grado
 * di costruire l'elenco dei libri a partire dal dataset originale
 * <code>BooksDatasetClean.csv</code>.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.model.Book
 */
public class LibriRepository {

    // --------------- ATTRIBUTI ---------------
    private final Path file;      // Libri.dati
    private final Path csvFile;   // BooksDatasetClean.csv (dataset originale)
    private final List<Book> books = new ArrayList<>();
    private int nextId = 1;


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo repository per i libri utilizzando il percorso
     * specificato per il file di persistenza principale.
     * <p>
     * Il file CSV del dataset originale viene cercato nella stessa
     * directory del file di persistenza, con il nome
     * <code>BooksDatasetClean.csv</code>.
     *
     * @param file percorso del file dei libri (es. <code>Libri.dati</code>)
     */
    public LibriRepository(Path file) {
        this.file = file;
        Path dir = file.getParent();
        if (dir == null) dir = Paths.get("."); // Se non c'è parent (path relativo), uso la dir corrente.
        // il CSV sta nella stessa cartella di Libri.dati
        this.csvFile = dir.resolve("BooksDatasetClean.csv");
    }


    // --------------- GETTERS ---------------
    /**
     * Restituisce la lista di tutti i libri attualmente caricati.
     * La lista restituita è non modificabile.
     *
     * @return lista immodificabile di tutti i libri
     */
    public List<Book> all() {
        return Collections.unmodifiableList(books);
    }


    /**
     * Restituisce il libro con l'identificatore specificato, se presente.
     *
     * @param id identificatore del libro cercato
     * @return il libro con l'id indicato, oppure <code>null</code>
     *         se nessun libro ha quell'identificatore
     */
    public Book findById(int id) {

        // Filtro la lista cercando il primo libro con uguale ID.
        return books.stream().filter(b -> b.getId() == id).findFirst().orElse(null);
    }


    /**
     * Restituisce il numero di libri attualmente caricati in memoria.
     *
     * @return numero di libri presenti
     */
    public int size() {
        return books.size();
    }

    // --------------- METODI PUBBLICI ---------------
    /**
     * Salva l'elenco corrente di libri nel file di persistenza
     * <code>Libri.dati</code>.
     * <p>
     * Il formato del file è:
     * <pre>
     * idLibro;Titolo;Autori;Anno;Editore;Categoria
     * </pre>
     * Con la lista degli autori memorizzata come singola stringa
     * in cui i nomi sono separati dal carattere <code>|</code>.
     *
     * @throws IOException in caso di errore di I/O durante la scrittura del file
     */
    public void save() throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {

            // Intestazione delle colonne.
            w.write("idLibro;Titolo;Autori;Anno;Editore;Categoria");
            w.newLine();
            for (Book b : books) {

                // Converto la lista degli autori in formato 'a1 | a2 | a3'.
                String autori = String.join("|", b.getAutori());

                // Se anno è null salvo stringa vuota.
                String annoStr = (b.getAnno() == null) ? "" : b.getAnno().toString();

                // Se editore o categoria nulla faccio lo stesso.
                String editore = b.getEditore() == null ? "" : b.getEditore();
                String categoria = b.getCategoria() == null ? "" : b.getCategoria();
                w.write(b.getId() + ";" + b.getTitolo() + ";" + autori + ";" + annoStr + ";" + editore + ";" + categoria);
                w.newLine();
            }
        }
    }


    /**
     * Carica l'elenco dei libri dai file disponibili.
     * <p>
     * Il comportamento è il seguente:
     * <ul>
     *     <li>Se esiste il file <code>Libri.dati</code>, viene letto
     *         tramite {@link #loadFromLibri()},</li>
     *     <li>Altrimenti, se esiste <code>BooksDatasetClean.csv</code>,
     *         viene utilizzato per costruire i libri con
     *         {@link #buildFromCsv()} e viene creato il file
     *         <code>Libri.dati</code> con {@link #save()},</li>
     *     <li>Se nessuno dei due file è presente viene sollevata
     *         una {@link FileNotFoundException}.</li>
     * </ul>
     * Prima del caricamento eventuali libri già presenti in memoria
     * vengono rimossi e il contatore {@code nextId} viene azzerato.
     *
     * @throws IOException in caso di errore di I/O durante la lettura o scrittura dei file
     */
    public void load() throws IOException {
        books.clear(); // Svuoto la lista correntemente in memoria.
        nextId = 1; // Riparto da 1.

        if (Files.exists(file)) {

            // Uso il formato pulito di libri.
            loadFromLibri();
        } else if (Files.exists(csvFile)) { // Se è il primo avvio genero il file  Libri.dati.
            System.out.println("Libri.dati non trovato, genero da BooksDatasetClean.csv...");
            buildFromCsv();
            save(); // Scrive su Libri.dati per le prossime esecuzioni.
        } else { // Nessun dato disponibile.
            throw new FileNotFoundException(
                    "Non trovo né " + file.toAbsolutePath() +
                    " né " + csvFile.toAbsolutePath());
        }
    }


    // --------------- HELPERS ---------------
    /**
     * Legge i libri dal file di persistenza <code>Libri.dati</code>.
     * <p>
     * Ogni riga del file (esclusa l'intestazione) è attesa nel formato:
     * <pre>
     * idLibro;Titolo;Autori;Anno;Editore;Categoria
     * </pre>
     * Le righe con formato non valido o con identificatori non numerici
     * vengono ignorate. Per ogni libro viene aggiornato il contatore
     * {@code nextId} in modo che sia sempre maggiore di tutti gli id letti.
     *
     * @throws IOException in caso di errore di I/O durante la lettura del file
     */
    private void loadFromLibri() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = r.readLine(); // Header
            if (line == null) return; // Se vuoto non fare niente.

            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue; // All'occorrenza salta le righe vuote.

                // Il -1 è usato anche per considerare i campi vuoti.
                String[] c = line.split(";", -1);
                if (c.length < 3) continue; // Ma devo almeno avere ID, titolo e autore

                int id;
                try {
                    id = Integer.parseInt(c[0].trim());
                } catch (NumberFormatException ex) {
                    // Se non è utilizzabile passo al prossimo.
                    id = nextId;
                }

                String titolo = c[1].trim();
                String autoriRaw = c[2].trim();

                // L'anno è opzionale.
                Integer anno = null;
                if (c.length > 3 && !c[3].trim().isEmpty()) {
                    try {
                        anno = Integer.parseInt(c[3].trim());
                    } catch (NumberFormatException ignored) {}
                }

                // Anche editore e categoria sono opzionali.
                String editore = c.length > 4 ? c[4].trim() : null;
                String categoria = c.length > 5 ? c[5].trim() : null;

                // Converto la stringa in una lista di nomi.
                List<String> autori = normalizeAutori(autoriRaw);

                books.add(new Book(id, titolo, autori, anno, editore, categoria));
                nextId = Math.max(nextId, id + 1);
            }
        }
    }


    /**
     * Costruisce la lista dei libri a partire dal file CSV originale
     * <code>BooksDatasetClean.csv</code>.
     * <p>
     * Il CSV è atteso con almeno 8 colonne, tra cui:
     * <ul>
     *     <li>0: Title</li>
     *     <li>1: Authors</li>
     *     <li>3: Category</li>
     *     <li>4: Publisher</li>
     *     <li>7: Publish Date (Year)</li>
     * </ul>
     * Ogni riga viene convertita in un oggetto {@link Book}, assegnando
     * identificatori progressivi a partire da {@code nextId}.
     *
     * @throws IOException in caso di errore di I/O durante la lettura del CSV
     */
    private void buildFromCsv() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line = br.readLine(); // header
            if (line == null) return;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;


                List<String> cols = parseCSV(line);
                if (cols.size() < 8) continue; // Evita il blocco in caso non ci siano tutti i dati.

                String title     = cols.get(0).trim();
                String authors   = cols.get(1).trim();
                String category  = cols.get(3).trim();
                String publisher = cols.get(4).trim();
                String yearStr   = cols.get(7).trim();

                // Opzionale.
                Integer year = null;
                try {
                    if (!yearStr.isEmpty()) {
                        year = Integer.parseInt(yearStr);
                    }
                } catch (NumberFormatException ignored) {}

                // Pulizia stringa autori.
                List<String> autori = normalizeAutori(authors);

                // Se publisher e category sono vuoti li salvo come null.
                Book b = new Book(nextId++, title, autori, year,
                        publisher.isEmpty() ? null : publisher,
                        category.isEmpty() ? null : category);

                books.add(b);
            }
        }
    }


    /**
     * Effettua il parsing di una riga CSV gestendo correttamente i campi
     * racchiusi tra virgolette doppie, che possono contenere virgole interne.
     * <p>
     * La riga viene suddivisa in campi utilizzando la virgola come
     * separatore solo quando non ci si trova all'interno di un campo
     * quotato. Le virgolette non vengono incluse nei valori restituiti.
     *
     * @param line riga di testo del file CSV
     * @return lista dei campi estratti dalla riga
     */
    private static List<String> parseCSV(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false; // se sono dentro "..." allora true.
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') { // Ogni " inverte lo stato entro/esco da un campo.
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) { // La virgola rappresenta la separazione tra i campi quotati.
                result.add(sb.toString());
                sb.setLength(0); // Resetto il buffer per il campo successivo
            } else {
                sb.append(c);
            }
        }

        // Aggiungo l'ultimo campo dopo l'ultima virgola.
        result.add(sb.toString());
        return result;
    }


    /**
     * Normalizza una stringa contenente uno o più autori in una lista
     * di nomi.
     * <p>
     * Vengono effettuate le seguenti operazioni:
     * <ul>
     *     <li>Rimozione di un eventuale prefisso iniziale
     *         <code>"By "</code> / <code>"by "</code>,</li>
     *     <li>Divisione su virgola, punto e virgola o carattere
     *         <code>|</code>, con eventuali spazi ignorati,</li>
     *     <li>Eliminazione di parti vuote.</li>
     * </ul>
     *
     * @param raw stringa grezza contenente gli autori
     * @return lista di nomi di autore; lista vuota se la stringa è
     *         <code>null</code> o non contiene nomi validi
     */
    private List<String> normalizeAutori(String raw) {
        if (raw == null) return Collections.emptyList();
        String s = raw.trim();
        // Rimuovo eventuale prefisso "By ".
        if (s.toLowerCase().startsWith("by ")) s = s.substring(3);
        // Split su virgola, ';' o '|'
        String[] parts = s.split("\\s*[;,|]\\s*");

        // Tolgo i pezzi vuoti nella parte .filter.
        return Arrays.stream(parts).map(String::trim).filter(p -> !p.isEmpty()).collect(Collectors.toList());
    }
}
