package bookrecommender.service;

import bookrecommender.model.Suggestion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestione dei suggerimenti di libri correlati.
 * File: ConsigliLibri.dati
 * Formato: userid;bookId;idSug1,idSug2,...
 */
public class SuggestionService {

    private final Path fileConsigli;
    private final Path fileLibrerie;

    public SuggestionService(Path fileConsigli, Path fileLibrerie) {
        this.fileConsigli = fileConsigli;
        this.fileLibrerie = fileLibrerie;
    }

    // ============ API PRINCIPALE ============

    /**
     * Inserisce un nuovo suggerimento.
     * Requisiti (come da specifiche progetto):
     *  - max 3 libri suggeriti
     *  - tutti diversi tra loro e diversi dal libro base
     *  - tutti presenti nelle librerie dell'utente
     */
    public boolean inserisciSuggerimento(Suggestion s) throws Exception {
        List<Integer> ids = new ArrayList<>(s.getSuggeriti());

        // filtra duplicati e self
        ids = ids.stream().distinct()
                .filter(id -> id != s.getBookId())
                .collect(Collectors.toList());
        if (ids.isEmpty() || ids.size() > 3) return false;

        // controlla che ogni libro suggerito sia in almeno una libreria dell'utente
        for (int id : ids) {
            if (!utenteHaLibroInLibreria(s.getUserid(), id)) {
                return false;
            }
        }

        // ⚠️ NUOVO: l'utente può suggerire per quel libro UNA SOLA VOLTA
        List<Suggestion> all = loadAll();
        boolean esisteGia = all.stream()
                .anyMatch(old -> old.getUserid().equals(s.getUserid())
                        && old.getBookId() == s.getBookId());
        if (esisteGia) {
            return false;
        }

        all.add(new Suggestion(s.getUserid(), s.getBookId(), ids));
        saveAll(all);
        return true;
    }


    // ============ METODI PER LA GUI ============

    /** Tutti i suggerimenti fatti da un utente. */
    public List<Suggestion> listByUser(String userid) throws Exception {
        return loadAll().stream()
                .filter(s -> s.getUserid().equals(userid))
                .collect(Collectors.toList());
    }

    /** Elimina un suggerimento dell'utente per un certo libro base. */
    public boolean deleteSuggestion(String userid, int bookId) throws Exception {
        List<Suggestion> all = loadAll();
        boolean removed = all.removeIf(s ->
                s.getUserid().equals(userid) && s.getBookId() == bookId);
        if (removed) saveAll(all);
        return removed;
    }

    // ============ IO SU FILE ============

    private List<Suggestion> loadAll() throws Exception {
        List<Suggestion> list = new ArrayList<>();
        if (!Files.exists(fileConsigli)) return list;

        try (BufferedReader br = Files.newBufferedReader(fileConsigli)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 3) continue;
                String userid = c[0];
                int bookId = parseIntSafe(c[1]);
                List<Integer> ids = new ArrayList<>();
                if (!c[2].isBlank()) {
                    for (String p : c[2].split(",")) {
                        try { ids.add(Integer.parseInt(p.trim())); }
                        catch (NumberFormatException ignored) {}
                    }
                }
                list.add(new Suggestion(userid, bookId, ids));
            }
        }
        return list;
    }

    private void saveAll(List<Suggestion> list) throws Exception {
        Files.createDirectories(fileConsigli.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(fileConsigli)) {
            for (Suggestion s : list) {
                String ids = s.getSuggeriti().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                bw.write(s.getUserid() + ";" + s.getBookId() + ";" + ids);
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
