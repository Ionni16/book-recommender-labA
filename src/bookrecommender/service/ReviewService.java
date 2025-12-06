package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.model.Review;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestione delle valutazioni degli utenti.
 * File: ValutazioniLibri.dati
 * Formato: userid;bookId;stile;contenuto;gradev;originalita;edizione;votoFinale;commento
 *
 * Il file Librerie.dati viene usato solo per controllare che
 * l'utente abbia il libro in almeno una libreria.
 */
public class ReviewService {

    private final Path fileValutazioni;
    private final Path fileLibrerie;

    public ReviewService(Path fileValutazioni, Path fileLibrerie) {
        this.fileValutazioni = fileValutazioni;
        this.fileLibrerie = fileLibrerie;
    }

    // ============ API PRINCIPALE (già esistente) ============

    /**
     * Inserisce una nuova valutazione.
     * Requisiti:
     *  - il libro deve essere presente in almeno una libreria dell'utente
     *  - non deve esistere già una valutazione per (userid, bookId)
     */
    public boolean inserisciValutazione(Review r) throws Exception {
        if (!utenteHaLibroInLibreria(r.getUserid(), r.getBookId())) {
            return false;
        }

        List<Review> all = loadAll();
        boolean esisteGia = all.stream()
                .anyMatch(x -> x.getUserid().equals(r.getUserid())
                        && x.getBookId() == r.getBookId());
        if (esisteGia) return false;

        all.add(r);
        saveAll(all);
        return true;
    }

    // ============ METODI AGGIUNTI PER LA GUI ============

    /** Tutte le valutazioni fatte da un certo utente. */
    public List<Review> listByUser(String userid) throws Exception {
        return loadAll().stream()
                .filter(r -> r.getUserid().equals(userid))
                .collect(Collectors.toList());
    }

    /** Aggiorna una valutazione (userid + bookId fissi). */
    public boolean updateReview(Review updated) throws Exception {
        List<Review> all = loadAll();
        boolean found = false;
        List<Review> nuovo = new ArrayList<>();
        for (Review r : all) {
            if (r.getUserid().equals(updated.getUserid())
                    && r.getBookId() == updated.getBookId()) {
                nuovo.add(updated);
                found = true;
            } else {
                nuovo.add(r);
            }
        }
        if (!found) return false;
        saveAll(nuovo);
        return true;
    }

    /** Elimina la valutazione di (userid, bookId). */
    public boolean deleteReview(String userid, int bookId) throws Exception {
        List<Review> all = loadAll();
        boolean removed = all.removeIf(r ->
                r.getUserid().equals(userid) && r.getBookId() == bookId);
        if (removed) saveAll(all);
        return removed;
    }

    // ============ IO SU FILE ============

    private List<Review> loadAll() throws Exception {
        List<Review> list = new ArrayList<>();
        if (!Files.exists(fileValutazioni)) return list;

        try (BufferedReader br = Files.newBufferedReader(fileValutazioni)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 9) continue;
                String userid = c[0];
                int bookId = parseIntSafe(c[1]);
                int stile = parseIntSafe(c[2]);
                int contenuto = parseIntSafe(c[3]);
                int gradev = parseIntSafe(c[4]);
                int orig = parseIntSafe(c[5]);
                int ediz = parseIntSafe(c[6]);
                int votoFinale = parseIntSafe(c[7]);
                String commento = c[8];
                Review r = new Review(userid, bookId, stile, contenuto,
                        gradev, orig, ediz, votoFinale, commento);
                list.add(r);
            }
        }
        return list;
    }

    private void saveAll(List<Review> list) throws Exception {
        Files.createDirectories(fileValutazioni.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(fileValutazioni)) {
            for (Review r : list) {
                bw.write(r.getUserid() + ";" +
                        r.getBookId() + ";" +
                        r.getStile() + ";" +
                        r.getContenuto() + ";" +
                        r.getGradevolezza() + ";" +
                        r.getOriginalita() + ";" +
                        r.getEdizione() + ";" +
                        r.getVotoFinale() + ";" +
                        (r.getCommento() == null ? "" : r.getCommento()));
                bw.newLine();
            }
        }
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }

    // ============ controllo libreria ============

    private boolean utenteHaLibroInLibreria(String userid, int bookId) throws Exception {
        if (!Files.exists(fileLibrerie)) return false;
        try (BufferedReader br = Files.newBufferedReader(fileLibrerie)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 3) continue;
                if (!c[0].equals(userid)) continue;
                if (Arrays.stream(c[2].split(","))
                        .anyMatch(p -> p.trim().equals(String.valueOf(bookId)))) {
                    return true;
                }
            }
        }
        return false;
    }
}
