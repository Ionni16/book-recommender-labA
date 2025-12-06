package bookrecommender.repo;

import bookrecommender.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * La classe <code>UtentiRepository</code> gestisce gli utenti
 * registrati su file di testo.
 * <p>
 * Il file è memorizzato in formato testuale con campi separati
 * da punto e virgola nel seguente ordine:
 * <pre>
 * userid; passwordHash; nome; cognome; codiceFiscale; email
 * </pre>
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.model.User
 */
public class UtentiRepository {

    // --------------- ATTRIBUTI ---------------
    private final Path file;


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo repository per gli utenti che utilizza
     * il file specificato per la memorizzazione.
     *
     * @param file percorso del file degli utenti
     */
    public UtentiRepository(Path file) {
        this.file = file;
    }


    // --------------- GETTER ---------------
    /**
     * Cerca un utente a partire dal suo identificatore.
     * <p>
     * Se il file non esiste oppure non è presente alcuna riga
     * con l'identificatore indicato, viene restituito un
     * {@link Optional} vuoto.
     *
     * @param userid identificatore dell'utente da cercare
     * @return un {@code Optional} contenente l'utente trovato,
     *         oppure {@code Optional.empty()} se l'utente non esiste
     * @throws IOException in caso di errore di I/O durante la lettura del file
     */
    public Optional<User> findByUserid(String userid) throws IOException {
        // Se il file non esiste ancora, viene restituito un Optional vuoto.
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                // Split su ';' e mantengo anche campi vuoti.
                String[] c = line.split(";", -1);
                if (c.length < 6) continue; // Se non abbastanza lunga ignoro.
                if (userid.equals(c[0])) { // Contiene l'userId.
                    return Optional.of(new User(c[0], c[1], c[2], c[3], c[4], c[5])); // Costruisco l'user con i 6 campi.
                }
            }
        }

        // Se non trovo nulla ritorno un Optional vuoto
        return Optional.empty();
    }


    // --------------- METODI PUBBLICI ---------------
    /**
     * Verifica se esiste già un utente con l'identificatore specificato.
     *
     * @param userid identificatore da verificare
     * @return {@code true} se esiste un utente con quell'identificatore,
     *         {@code false} altrimenti
     * @throws IOException in caso di errore di I/O durante la lettura del file
     */
    public boolean existsUserid(String userid) throws IOException {
        // Per vedere se esiste controllo che l'optional in uscita non sia vuoto.
        return findByUserid(userid).isPresent();
    }


    /**
     * Aggiunge in coda al file una riga corrispondente all'utente
     * specificato.
     * <p>
     * Se il file non esiste ancora, viene creato e viene scritta
     * anche l'intestazione di colonna. I campi sono salvati nel
     * seguente ordine:
     * <pre>
     * userid; passwordHash; nome; cognome; codiceFiscale; email
     * </pre>
     *
     * @param u utente da aggiungere al file
     * @throws IOException in caso di errore di I/O durante la scrittura del file
     */
    public void append(User u) throws IOException {
        // Verifico che esista il relativo file.
        boolean exists = Files.exists(file);
        try (BufferedWriter bw = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, // Crea file se inesistente.
                StandardOpenOption.APPEND // Scrive.
        )) {
            if (!exists) {
                // Se non esisteva prima, devo aggiungere l'header.
                bw.write("userid;passwordHash;nome;cognome;codiceFiscale;email\n");
            }

            // Campi utente in una lista scritti in linea dal buffer.
            bw.write(String.join(";", Arrays.asList(
                    u.getUserid(),
                    u.getPasswordHash(),
                    u.getNome(),
                    u.getCognome(),
                    u.getCodiceFiscale(),
                    u.getEmail()
            )));
            bw.write("\n");
        }
    }
}
