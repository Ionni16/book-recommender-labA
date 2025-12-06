package bookrecommender.service;

import bookrecommender.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/**
 * Gestisce registrazione, login e modifica/eliminazione utenti.
 * File: UtentiRegistrati.dati
 * Formato: userid;passwordHash;nome;cognome;codFiscale;email
 */
public class AuthService {

    private final Path fileUtenti;
    private String currentUserid;

    public AuthService(Path fileUtenti) {
        this.fileUtenti = fileUtenti;
    }

    // ================== API PRINCIPALE ==================

    /** Registra un nuovo utente. Ritorna false se userid già esistente. */
    public boolean registrazione(User u) throws Exception {
        Map<String, User> m = loadAll();
        if (m.containsKey(u.getUserid())) return false;
        m.put(u.getUserid(), u);
        saveAll(m.values());
        return true;
    }

    /** Login con password in chiaro. Ritorna true se ok. */
    public boolean login(String userid, String passwordPlain) throws Exception {
        Map<String, User> m = loadAll();
        User u = m.get(userid);
        if (u == null) return false;
        String hash = sha256(passwordPlain);
        if (!hash.equals(u.getPasswordHash())) return false;
        currentUserid = userid;
        return true;
    }

    public void logout() {
        currentUserid = null;
    }

    public String getCurrentUserid() {
        return currentUserid;
    }

    // --------- METODI AGGIUNTI PER LA GUI ---------

    /** Restituisce l'oggetto User completo per uno userid. */
    public User getUser(String userid) throws Exception {
        Map<String, User> m = loadAll();
        return m.get(userid);
    }

    /** Aggiorna i dati (tranne la password, che si gestisce a parte). */
    public boolean updateUser(User updated) throws Exception {
        Map<String, User> m = loadAll();
        if (!m.containsKey(updated.getUserid())) return false;
        // Mantengo l'hash già presente nell'oggetto updated
        m.put(updated.getUserid(), updated);
        saveAll(m.values());
        return true;
    }

    /**
     * Aggiorna la password (in chiaro -> hash).
     * Ritorna false se utente non esiste.
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

    /** Elimina completamente un utente dal file. */
    public boolean deleteUser(String userid) throws Exception {
        Map<String, User> m = loadAll();
        boolean removed = (m.remove(userid) != null);
        if (removed) saveAll(m.values());
        if (userid.equals(currentUserid)) currentUserid = null;
        return removed;
    }

    // ================== IO SU FILE ==================

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

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // ================== HASHING ==================

    public static String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(s.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
