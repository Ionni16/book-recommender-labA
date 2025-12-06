package bookrecommender.repo;

import bookrecommender.model.Library;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * La classe <code>LibrerieRepository</code> gestisce la persistenza
 * delle librerie personali degli utenti su file di testo.
 * <p>
 * Ogni riga del file rappresenta una libreria ed è memorizzata nel formato:
 * <pre>
 * userid;nomeLibreria;id1|id2|id3|...
 * </pre>
 * dove <code>userid</code> identifica l'utente proprietario,
 * <code>nomeLibreria</code> è il nome scelto dall'utente e la terza colonna
 * contiene gli identificatori dei libri presenti nella libreria,
 * separati dal carattere <code>|</code>.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.model.Library
 */
public class LibrerieRepository {

    // --------------- ATTRIBUTI ---------------
    private final Path file; // Percorso su cui vengono salvate lee librerie.


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo repository che utilizza il file specificato
     * per la memorizzazione delle librerie.
     *
     * @param file percorso del file delle librerie
     */
    public LibrerieRepository(Path file) {
        this.file = file;
    }


    // --------------- GETTER ---------------
    /**
     * Restituisce solo le librerie appartenenti all'utente specificato.
     *
     * @param userid identificatore dell'utente proprietario
     * @return lista delle librerie dell'utente indicato; lista vuota se
     *         non esistono librerie per quell'utente o se il file non esiste
     * @throws IOException in caso di errore di I/O durante la lettura
     */
    public List<Library> findByUserid(String userid) throws IOException {
        List<Library> all = loadAll();
        List<Library> res = new ArrayList<>();
        for (Library l : all) {
            if (l.getUserid().equals(userid)) {
                res.add(l);
            }
        }
        return res;
    }


    // --------------- SETTER ---------------
    /**
     * Inserisce una nuova libreria oppure aggiorna una libreria esistente.
     * <p>
     * La libreria è identificata in modo univoco dalla coppia
     * (<code>userid</code>, <code>nome</code>). Se nel file è già presente
     * una libreria con gli stessi valori, questa viene sostituita con
     * quella passata come parametro; altrimenti la libreria viene aggiunta
     * in coda.
     *
     * @param lib libreria da inserire o aggiornare
     * @throws IOException in caso di errore di I/O durante la lettura
     *                     o la scrittura del file
     */
    public void upsert(Library lib) throws IOException {
        List<Library> all = loadAll();
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            Library current = all.get(i);
            if (current.getUserid().equals(lib.getUserid()) && current.getNome().equals(lib.getNome())) {
                all.set(i, lib);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(lib);
        }
        saveAll(all);
    }


    /**
     * Sovrascrive completamente il file delle librerie con il contenuto
     * della lista specificata.
     * <p>
     * Se la directory che contiene il file non esiste, viene creata.
     * Ogni libreria viene scritta su una riga nel formato:
     * <code>userid;nomeLibreria;id1|id2|...</code>.
     *
     * @param libs lista di librerie da salvare
     * @throws IOException in caso di errore di I/O durante la scrittura
     */
    private void saveAll(List<Library> libs) throws IOException {

        // Se la cartella dei file non esiste la creo.
        if (!Files.exists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }
        try (BufferedWriter bw = Files.newBufferedWriter(file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, // Crea il file se non esiste.
                StandardOpenOption.TRUNCATE_EXISTING, // Svuota il file se esiste già.
                StandardOpenOption.WRITE)) // Apertura in scrittura.
        {

            for (Library l : libs) {
                StringBuilder sb = new StringBuilder();
                sb.append(l.getUserid()).append(";");
                sb.append(l.getNome() == null ? "" : l.getNome()).append(";"); // Se il nome è null, per evitare di
                                                                               // scrivere null nel file, salvo la
                                                                               // stringa vuota.
                sb.append(joinIds(l.getBookIds())); // Converto gli ID della libreria nel formato 'id1 | id2 | id3'.
                bw.write(sb.toString()); // Scrivo la riga completa.
                bw.newLine(); // A capo per la prossima libreria.
            }
        }
    }


    // --------------- HELPERS ---------------
    /**
     * Carica dal file tutte le librerie disponibili.
     * <p>
     * In ogni riga vine fatto il parse nel formato
     * <code>userid;nomeLibreria;id1|id2|...</code>. Le righe vuote,
     * incomplete o con identificatori non numerici vengono ignorate.
     *
     * @return lista delle librerie lette dal file; lista vuota se il file
     *         non esiste o non contiene righe valide
     * @throws IOException in caso di errore di I/O durante la lettura
     */
    private List<Library> loadAll() throws IOException {
        List<Library> res = new ArrayList<>();
        if (!Files.exists(file)) {
            return res; // Se file inesistente, restituisce lista vuota.
        }

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim(); // Rimozione spazi nella riga.
                if (line.isEmpty()) continue; // Salto righe vuote.

                // Formato: userid; nomeLibreria; id1 | id2 | id3. --> split in 3 pezzi dove il terzo contiene la lista
                // di ID.
                String[] parts = line.split(";", 3);
                if (parts.length < 2) continue; // se non ho almeno nome e userId scarto la riga.

                String userid = parts[0].trim();
                String nome   = parts[1].trim();

                // Per mantenere un ordine di inserimento ed evitare duplicati uso LinkedHashSet.
                Set<Integer> ids = new LinkedHashSet<>();

                // Se esiste la terza colonna e non è vuota, viene fatto il parse degli ID.
                if (parts.length == 3 && !parts[2].trim().isEmpty()) {

                    // Lo split viene eseguito con '|', che necessita di 2 '\\' in quanto carattere speciale nel regex.
                    String[] rawIds = parts[2].split("\\|");
                    for (String s : rawIds) {
                        s = s.trim();
                        if (s.isEmpty()) continue; // ID vuoti tipo '||' vengono saltati.
                        try {
                            ids.add(Integer.parseInt(s)); // Converto in int e aggiungo al set.
                        } catch (NumberFormatException ignored) { // Se id non è un numero viene ignorato.
                        }
                    }
                }

                // Creo library con i parametri formali indicati sotto.
                res.add(new Library(userid, nome, ids));
            }
        }

        return res;
    }


    /**
     * Converte l'insieme degli identificatori di libri in una stringa
     * nel formato <code>id1|id2|id3|...</code>.
     *
     * @param ids insieme degli identificatori dei libri
     * @return stringa contenente gli id separati dal carattere
     *         <code>|</code>, oppure stringa vuota se l'insieme è vuoto
     */
    private static String joinIds(Set<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer id : ids) {
            if (!first) sb.append("|"); // Metto il separatore '|' solo dopo il primo elemento.
            sb.append(id);
            first = false;
        }

        // Se l'insieme è vuoto allora ritorno la stringa vuota.
        return sb.toString();
    }
}
