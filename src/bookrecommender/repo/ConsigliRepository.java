package bookrecommender.repo;

import bookrecommender.model.Suggestion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


/**
 * La classe <code>ConsigliRepository</code> gestisce la persistenza
 * degli oggetti {@link Suggestion} su file testuale.
 * <p>
 * Il file è in formato testo con campi separati da punto e virgola:
 * <pre>
 * userId; idLibro; idSuggerito1; idSuggerito2; idSuggerito3
 * </pre>
 * Ogni riga successiva all'intestazione rappresenta un insieme di suggerimenti
 * di un utente per un libro di riferimento, con fino a tre identificatori
 * di libri suggeriti.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.model.Suggestion
 */
public class ConsigliRepository {

    // --------------- ATTRIBUTI ---------------
    private final Path file; // Percorso file su cui vengono scritti i suggerimenti


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo repository che utilizza il file specificato
     * per memorizzare i suggerimenti.
     *
     * @param file percorso del file dei suggerimenti
     */
    public ConsigliRepository(Path file) { this.file = file; }


    // --------------- HELPERS ---------------
    /**
     * Restituisce tutte le {@link Suggestion} presenti nel file che
     * si riferiscono al libro con l'identificatore specificato.
     * <p>
     * Le righe con formato non valido o con valori non numerici negli
     * identificatori vengono ignorate.
     *
     * @param bookId identificatore del libro di riferimento
     * @return lista delle suggerimenti per il libro indicato;
     *         la lista è vuota se il file non esiste o non contiene
     *         suggerimenti per quel libro
     * @throws IOException in caso di errore di I/O durante la lettura del file
     */
    public List<Suggestion> findByBookId(int bookId) throws IOException {
        List<Suggestion> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            br.readLine(); // Salta l'header.
            String line;
            while ((line = br.readLine()) != null) {
                String[] c = line.split(";", -1); // Divisione in colonne dei dati, il -1 fa si che le
                                                             // colonne vuote non vengano troncate.
                if (c.length < 5) continue; // Se la riga è malformata viene saltata.
                int bId;

                try { bId = Integer.parseInt(c[1]); } catch (NumberFormatException e) { continue;} // c[1] equivale alla
                // colonna ID del libro, se durante il parse in int il numero non è valido viene sollevata l'eccezione
                // saltando poi la riga.

                if (bId != bookId) continue; // Se non è il libro che mi interessa lo salto.

                List<Integer> sug = new ArrayList<>(); // Inizializzazione della lista su cui porre i suggerimenti.
                for (int i = 2; i <= 4; i++) {
                    if (!c[i].isEmpty()) {
                        try { sug.add(Integer.parseInt(c[i])); } catch (NumberFormatException ignored) {} // In caso di
                        // ID sporchi, vengono ignorati tramite il catch.
                    }
                }
                out.add(new Suggestion(c[0], bId, sug)); // creazione della nuova suggestion dove c[0] è l'ID dell'utente
                                                         // bId è il riferimento del libro e sug la lista dei suggeriti.
            }
        }
        return out;
    }


    /**
     * Aggiunge in coda al file una nuova riga corrispondente alla
     * {@link Suggestion} specificata.
     * <p>
     * Se il file non esiste ancora viene creato e viene scritta
     * l'intestazione di colonna. Se la lista dei libri suggeriti
     * contiene meno di tre elementi, i campi mancanti vengono lasciati
     * vuoti nella riga scritta.
     *
     * @param s suggerimento da memorizzare
     * @throws IOException in caso di errore di I/O durante la scrittura del file
     */
    public void append(Suggestion s) throws IOException {
        boolean exists = Files.exists(file);
        List<Integer> list = s.getSuggeriti();
        int id1 = !list.isEmpty() ? list.get(0) : 0;
        int id2 = list.size() > 1 ? list.get(1) : 0;
        int id3 = list.size() > 2 ? list.get(2) : 0;

        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (!exists) {
                bw.write("userid;idLibro;idSuggerito1;idSuggerito2;idSuggerito3\n");
            }
            bw.write(s.getUserid() + ";" + s.getBookId() + ";" + (id1==0?"":id1) + ";" + (id2==0?"":id2) + ";" + (id3==0?"":id3) + "\n");
        }
    }
}
