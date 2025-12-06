package bookrecommender.repo;

import bookrecommender.model.Review;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * La classe <code>ValutazioniRepository</code> gestisce la persistenza
 * delle valutazioni ({@link Review}) su file di testo.
 * <p>
 * Il file è memorizzato in formato testuale con campi separati
 * da punto e virgola nel seguente ordine:
 * <pre>
 * userId; idLibro; stile; contenuto; gradevolezza; originalita; edizione; votoFinale; commento
 * </pre>
 *
 * @author Tuo Nome Cognome
 * @version 1.0
 * @see bookrecommender.model.Review
 */
public class ValutazioniRepository {

    // --------------- ATTRIBUTI ---------------
    private final Path file;


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo repository per le valutazioni che utilizza
     * il file specificato per la memorizzazione.
     *
     * @param file percorso del file delle valutazioni
     */
    public ValutazioniRepository(Path file) { this.file = file; }


    // --------------- GETTER ---------------
    /**
     * Restituisce tutte le valutazioni relative al libro con
     * l'identificatore specificato.
     * <p>
     * Se il file non esiste o non contiene valutazioni per quel libro,
     * viene restituita una lista vuota. Le righe con formato non valido
     * o con voti non numerici vengono ignorate.
     *
     * @param bookId identificatore del libro per cui cercare le valutazioni
     * @return lista delle valutazioni per il libro indicato; lista vuota
     *         se non sono presenti valutazioni
     * @throws IOException in caso di errore di I/O durante la lettura del file
     */
    public List<Review> findByBookId(int bookId) throws IOException {
        List<Review> out = new ArrayList<>(); // Lista che conterrà le reviews.
        if (!Files.exists(file)) return out; // Se file inesistente, ritorna lista vuota.
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                String[] c = line.split(";", -1);

                // Almeno 9 campi, altrimenti salta.
                if (c.length < 9) continue;
                int bId;
                try { bId = Integer.parseInt(c[1]); } catch (NumberFormatException e) { continue; }

                // Se la riga si riferisce a un ID diverso, quindi incongruente a quello passato come parametro, salto.
                if (bId != bookId) continue;

                try {
                    int stile = Integer.parseInt(c[2]);
                    int contenuto = Integer.parseInt(c[3]);
                    int gradevolezza = Integer.parseInt(c[4]);
                    int originalita = Integer.parseInt(c[5]);
                    int edizione = Integer.parseInt(c[6]);
                    int votoFinale = Integer.parseInt(c[7]);
                    String commento = c[8];

                    // Creo la lista output con i dati formattati correttamente.
                    out.add(new Review(c[0], bId, stile, contenuto, gradevolezza, originalita, edizione, votoFinale, commento));
                } catch (NumberFormatException ignored) {} // Se uno dei voti non è numerico la riga viene scartata.
            }
        }
        return out;
    }


    // --------------- METODO PUBBLICO ---------------
    /**
     * Aggiunge in coda al file una riga corrispondente alla
     * valutazione specificata.
     * <p>
     * Se il file non esiste ancora, viene creato e viene scritta
     * anche l'intestazione di colonna. Il commento viene normalizzato
     * rimuovendo eventuali ritorni a capo e spazi superflui.
     *
     * @param r valutazione da memorizzare
     * @throws IOException in caso di errore di I/O durante la scrittura del file
     */
    public void append(Review r) throws IOException {
        boolean exists = Files.exists(file);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (!exists) {
                bw.write("userid;idLibro;stile;contenuto;gradevolezza;originalita;edizione;votoFinale;commento\n");
            }
            bw.write(String.join(";", Arrays.asList(
                r.getUserid(),
                Integer.toString(r.getBookId()),
                Integer.toString(r.getStile()),
                Integer.toString(r.getContenuto()),
                Integer.toString(r.getGradevolezza()),
                Integer.toString(r.getOriginalita()),
                Integer.toString(r.getEdizione()),
                Integer.toString(r.getVotoFinale()),
                r.getCommento() == null ? "" : r.getCommento().replace("\n"," ").trim()
            )));
            bw.write("\n");
        }
    }
}
