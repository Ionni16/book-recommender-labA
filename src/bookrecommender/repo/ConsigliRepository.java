package bookrecommender.repo;

import bookrecommender.model.Suggestion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * La classe {@code ConsigliRepository} gestisce la persistenza degli oggetti {@link Suggestion}
 * su file testuale.
 *
 * <p>Formato del file (separatore: ';'):</p>
 * <pre>
 * userId; idLibro; idSuggerito1; idSuggerito2; idSuggerito3
 * </pre>
 *
 * <p>Ogni riga rappresenta i consigli di un utente per un libro base.
 * I campi dei suggeriti possono essere vuoti e, in alcuni file "vecchi", la riga può contenere
 * meno colonne (es. {@code user;48575;18210}). Questo repository gestisce sia il formato completo
 * (5 colonne) sia quello abbreviato (>= 3 colonne).</p>
 */
public class ConsigliRepository {

    // --------------- ATTRIBUTI ---------------
    /** Percorso del file su cui vengono scritti/lettI i suggerimenti. */
    private final Path file;


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo repository che utilizza il file specificato per memorizzare i suggerimenti.
     *
     * @param file percorso del file dei suggerimenti
     */
    public ConsigliRepository(Path file) {
        this.file = file;
    }


    // --------------- METODI DI LETTURA ---------------
    /**
     * Restituisce tutte le {@link Suggestion} presenti nel file che si riferiscono al libro con
     * l'identificatore specificato.
     *
     * <p>La lettura è robusta:
     * <ul>
     *   <li>ignora righe vuote o malformate;</li>
     *   <li>accetta righe con numero variabile di colonne (>= 3);</li>
     *   <li>legge fino a un massimo di 3 suggeriti (colonne 3..5 del formato completo);</li>
     *   <li>ignora ID non numerici nei suggeriti.</li>
     * </ul>
     * </p>
     *
     * @param bookId identificatore del libro base
     * @return lista di suggerimenti per il libro indicato; lista vuota se il file non esiste
     *         o non contiene suggerimenti per quel libro
     * @throws IOException in caso di errore di I/O durante la lettura del file
     */
    public List<Suggestion> findByBookId(int bookId) throws IOException {
        List<Suggestion> out = new ArrayList<>();
        if (!Files.exists(file)) return out;

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

            // Salta header se presente (prima riga che inizia con "userid;")
            br.mark(4096);
            String first = br.readLine();
            if (first != null) {
                String f = first.trim().toLowerCase();
                if (!(f.startsWith("userid;") || f.startsWith("userId;".toLowerCase()))) {
                    // Non è header -> torno indietro e tratto la riga come dato
                    br.reset();
                }
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] c = line.split(";", -1);

                // minimo: user;base;almeno1Suggerito
                if (c.length < 3) continue;

                int bId;
                try {
                    bId = Integer.parseInt(c[1].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                if (bId != bookId) continue;

                List<Integer> sug = new ArrayList<>();

                // legge da colonna 2 fino a max colonna 4 (max 3 suggeriti)
                for (int i = 2; i < c.length && i <= 4; i++) {
                    String cell = c[i].trim();
                    if (cell.isEmpty()) continue;

                    try {
                        sug.add(Integer.parseInt(cell));
                    } catch (NumberFormatException ignored) {
                        // ignora ID sporchi
                    }
                }

                if (!sug.isEmpty()) {
                    out.add(new Suggestion(c[0].trim(), bId, sug));
                }
            }
        }

        return out;
    }


    // --------------- METODO DI SCRITTURA ---------------
    /**
     * Aggiunge in coda al file una nuova riga corrispondente alla {@link Suggestion} specificata.
     *
     * <p>Se il file non esiste ancora viene creato e viene scritta l'intestazione di colonna.
     * Se la lista dei libri suggeriti contiene meno di tre elementi, i campi mancanti vengono
     * lasciati vuoti nella riga scritta.</p>
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

        try (BufferedWriter bw = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            if (!exists) {
                bw.write("userid;idLibro;idSuggerito1;idSuggerito2;idSuggerito3\n");
            }

            bw.write(s.getUserid() + ";" + s.getBookId() + ";"
                    + (id1 == 0 ? "" : id1) + ";"
                    + (id2 == 0 ? "" : id2) + ";"
                    + (id3 == 0 ? "" : id3) + "\n");
        }
    }
}
