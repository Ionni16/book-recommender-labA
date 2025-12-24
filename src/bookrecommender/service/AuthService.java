package bookrecommender.service;

import bookrecommender.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/**
 * La classe <code>AuthService</code> gestisce le operazioni di autenticazione
 * e gestione degli utenti registrati.
 * <p>
 * I dati vengono memorizzati nel file <code>UtentiRegistrati.dati</code>,
 * con righe nel formato:
 * <pre>
 * userid; passwordHash; nome; cognome; codFiscale; email
 * </pre>
 * La password non viene salvata in chiaro ma come hash SHA-256.
 *
 * @author Hamdi Kebeli
 * @version 1.0
 * @see bookrecommender.model.User
 */
public class AuthService {

    private final Path fileUtenti;
    private String currentUserid;


    /**
     * Costruisce un nuovo servizio di autenticazione a partire dal percorso
     * del file degli utenti.
     *
     * @param fileUtenti percorso del file <code>UtentiRegistrati.dati</code>
     */
    public AuthService(Path fileUtenti) {
        this.fileUtenti = fileUtenti;
    }

    // ================== API PRINCIPALE ==================

    /**
     * Registra un nuovo utente nel sistema.
     * <p>
     * L'utente viene aggiunto solo se lo <code>userid</code> non è già presente
     * nel file degli utenti.
     *
     * @param u oggetto {@link User} da registrare; si assume che contenga già
     *          l'hash della password nel campo <code>passwordHash</code>
     * @return {@code true} se la registrazione è andata a buon fine,
     *         {@code false} se esiste già un utente con lo stesso
     *         <code>userid</code>
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean registrazione(User u) throws Exception {
        Map<String, User> m = loadAll();
        if (m.containsKey(u.getUserid())) return false;
        m.put(u.getUserid(), u);
        saveAll(m.values());
        return true;
    }


    /**
     * Esegue il login di un utente a partire da userid e password in chiaro.
     * <p>
     * La password fornita viene convertita in hash SHA-256 e confrontata con
     * l'hash memorizzato per l'utente corrispondente.
     *
     * @param userid        identificatore dell'utente
     * @param passwordPlain password in chiaro inserita dall'utente
     * @return {@code true} se le credenziali sono corrette e il login va a buon fine,
     *         {@code false} se l'utente non esiste o la password è errata
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    public boolean login(String userid, String passwordPlain) throws Exception {
        Map<String, User> m = loadAll();
        User u = m.get(userid);
        if (u == null) return false;
        String hash = sha256(passwordPlain);
        if (!hash.equals(u.getPasswordHash())) return false;
        currentUserid = userid;
        return true;
    }


    /**
     * Esegue il logout dell'utente corrente, azzerando lo stato di sessione.
     */
    public void logout() {
        currentUserid = null;
    }


    /**
     * Restituisce l'identificatore dell'utente attualmente autenticato.
     *
     * @return userid dell'utente loggato oppure <code>null</code> se non
     *         c'è nessun utente autenticato
     */
    public String getCurrentUserid() {
        return currentUserid;
    }

    // --------- METODI AGGIUNTI PER LA GUI ---------

    /**
     * Restituisce l'oggetto {@link User} completo associato a uno userid.
     *
     * @param userid identificatore dell'utente da cercare
     * @return l'utente corrispondente oppure <code>null</code> se non esiste
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    public User getUser(String userid) throws Exception {
        Map<String, User> m = loadAll();
        return m.get(userid);
    }


    /**
     * Aggiorna i dati anagrafici di un utente.
     * <p>
     * La password non viene modificata da questo metodo, ma deve essere
     * già presente nell'oggetto <code>updated</code> (se necessario).
     *
     * @param updated oggetto {@link User} con gli stessi <code>userid</code>
     *                ma con i dati aggiornati
     * @return {@code true} se l'utente esisteva ed è stato aggiornato,
     *         {@code false} se non esiste nessun utente con quello userid
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean updateUser(User updated) throws Exception {
        Map<String, User> m = loadAll();
        if (!m.containsKey(updated.getUserid())) return false;
        // Mantengo l'hash già presente nell'oggetto updated
        m.put(updated.getUserid(), updated);
        saveAll(m.values());
        return true;
    }


    /**
     * Aggiorna la password di un utente a partire dalla nuova password in chiaro.
     * <p>
     * La nuova password viene convertita in hash SHA-256 e salvata nel file.
     *
     * @param userid           identificatore dell'utente di cui modificare la password
     * @param newPlainPassword nuova password in chiaro
     * @return {@code true} se l'utente esisteva e la password è stata aggiornata,
     *         {@code false} se non esiste alcun utente con quello userid
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean updatePassword(String userid, String newPlainPassword) throws Exception {
        Map<String, User> m = loadAll();
        User u = m.get(userid);
        if (u == null) return false;
        String hash = sha256(newPlainPassword);
        User nuovo = new User(u.getUserid(), hash,
                u.getNome(), u.getCognome(), u.getCodiceFiscale(), u.getEmail());
        m.put(userid, nuovo);
        saveAll(m.values());
        return true;
    }


    /**
     * Elimina definitivamente un utente dal file degli utenti registrati.
     * <p>
     * Se l'utente eliminato è quello attualmente autenticato, viene eseguito
     * anche il logout implicito.
     *
     * @param userid identificatore dell'utente da eliminare
     * @return {@code true} se l'utente è stato trovato e rimosso,
     *         {@code false} se non esisteva alcun utente con quello userid
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean deleteUser(String userid) throws Exception {
        Map<String, User> m = loadAll();
        boolean removed = (m.remove(userid) != null);
        if (removed) saveAll(m.values());
        if (userid.equals(currentUserid)) currentUserid = null;
        return removed;
    }

    // ================== IO SU FILE ==================


    /**
     * Carica tutti gli utenti dal file degli utenti registrati.
     * <p>
     * Le righe vuote o con formato non valido vengono ignorate.
     *
     * @return mappa che associa lo <code>userid</code> all'oggetto {@link User}
     *         corrispondente; mappa vuota se il file non esiste
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    private Map<String, User> loadAll() throws Exception {
        Map<String, User> m = new LinkedHashMap<>();
        if (!Files.exists(fileUtenti)) return m;

        try (BufferedReader br = Files.newBufferedReader(fileUtenti)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 6) continue;
                String userid = c[0];
                String hash = c[1];
                String nome = c[2];
                String cognome = c[3];
                String cf = c[4];
                String email = c[5];
                User u = new User(userid, hash, nome, cognome, cf, email);
                m.put(userid, u);
            }
        }
        return m;
    }


    /**
     * Sovrascrive completamente il file degli utenti con la collezione specificata.
     * <p>
     * Ogni utente viene scritto su una riga nel formato:
     * <pre>
     * userid; passwordHash; nome; cognome; codFiscale; email
     * </pre>
     *
     * @param users collezione di {@link User} da salvare
     * @throws Exception in caso di errore di I/O durante la scrittura del file
     */
    private void saveAll(Collection<User> users) throws Exception {
        Files.createDirectories(fileUtenti.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(fileUtenti)) {
            for (User u : users) {
                bw.write(u.getUserid() + ";" +
                        u.getPasswordHash() + ";" +
                        nullToEmpty(u.getNome()) + ";" +
                        nullToEmpty(u.getCognome()) + ";" +
                        nullToEmpty(u.getCodiceFiscale()) + ";" +
                        nullToEmpty(u.getEmail()));
                bw.newLine();
            }
        }
    }


    /**
     * Converte una stringa <code>null</code> in stringa vuota.
     *
     * @param s stringa da normalizzare
     * @return la stringa di ingresso se non è <code>null</code>, altrimenti
     *         stringa vuota
     */
    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // ================== HASHING ==================
    /**
     * Calcola l'hash SHA-256 di una stringa.
     * <p>
     * L'hash viene restituito come stringa esadecimale a 64 caratteri,
     * dove ogni byte è rappresentato da due cifre in base 16.
     *
     * @param s stringa di ingresso (ad esempio una password in chiaro)
     * @return stringa esadecimale che rappresenta l'hash SHA-256 della stringa
     * @throws Exception se l'algoritmo <code>SHA-256</code> non è disponibile
     */
    public static String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(s.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
