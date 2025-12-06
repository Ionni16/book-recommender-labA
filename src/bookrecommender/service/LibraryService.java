package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.repo.LibriRepository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestisce le librerie personali dell'utente.
 * File: Librerie.dati
 * Formato riga: userid;nomeLibreria;id1,id2,id3,...
 */
public class LibraryService {

    private final Path file;
    private final LibriRepository libriRepo; // non usato per IO ma comodo

    public LibraryService(Path file, LibriRepository libriRepo) {
        this.file = file;
        this.libriRepo = libriRepo;
    }

    // ============ API USATA DALLA GUI ============

    /** Restituisce tutte le librerie di un utente. */
    public List<Library> listUserLibraries(String userid) throws Exception {
        return loadAll().stream()
                .filter(l -> l.getUserid().equals(userid))
                .collect(Collectors.toList());
    }

    /**
     * Salva/aggiorna una libreria (upsert).
     * Se esiste già una libreria dello stesso utente con lo stesso nome, viene sovrascritta.
     */
    public boolean saveLibrary(Library lib) throws Exception {
        List<Library> all = loadAll();
        boolean updated = false;
        List<Library> newAll = new ArrayList<>();
        for (Library l : all) {
            if (l.getUserid().equals(lib.getUserid()) &&
                l.getNome().equals(lib.getNome())) {
                newAll.add(lib);
                updated = true;
            } else {
                newAll.add(l);
            }
        }
        if (!updated) newAll.add(lib);
        saveAll(newAll);
        return true;
    }

    /** Elimina completamente una libreria di un utente, per nome. */
    public boolean deleteLibrary(String userid, String nome) throws Exception {
        List<Library> all = loadAll();
        boolean removed = all.removeIf(l ->
                l.getUserid().equals(userid) && l.getNome().equals(nome));
        if (removed) saveAll(all);
        return removed;
    }

    // ============ IO SU FILE ============

    private List<Library> loadAll() throws Exception {
        List<Library> list = new ArrayList<>();
        if (!Files.exists(file)) return list;

        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 3) continue;
                String userid = c[0];
                String nome = c[1];
                Set<Integer> ids = new LinkedHashSet<>();
                if (!c[2].isBlank()) {
                    for (String p : c[2].split(",")) {
                        try {
                            ids.add(Integer.parseInt(p.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                list.add(new Library(userid, nome, ids));
            }
        }
        return list;
    }

    private void saveAll(List<Library> libs) throws Exception {
        Files.createDirectories(file.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            for (Library l : libs) {
                String ids = l.getBookIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                bw.write(l.getUserid() + ";" + l.getNome() + ";" + ids);
                bw.newLine();
            }
        }
    }
}
