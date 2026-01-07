/*
 * Nome: Ionut
 * Cognome: Puiu
 * Matricola: 758296
 * Sede: VA
 *
 * Nome: Matteo
 * Cognome: Ferrario
 * Matricola: 756147
 * Sede: VA
 */

package bookrecommender.service;

import bookrecommender.model.Book;
import bookrecommender.repo.LibriRepository;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * La classe <code>SearchService</code> fornisce funzionalità di ricerca
 * sui libri memorizzati nel {@link LibriRepository}.
 * <p>
 * Le ricerche disponibili sono:
 * <ul>
 *     <li>Per titolo;</li>
 *     <li>Per autore;</li>
 *     <li>Per autore e anno di pubblicazione.</li>
 * </ul>
 * Tutte le ricerche sono case-insensitive e ignorano gli accenti grazie
 * a una normalizzazione preventiva delle stringhe.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.model.Book
 * @see bookrecommender.repo.LibriRepository
 */
public class SearchService {

    /** Repository dei libri su cui effettuare le ricerche. */
    private final LibriRepository libriRepo;

    /**
     * Costruisce un nuovo servizio di ricerca libri.
     *
     * @param libriRepo repository dei libri da utilizzare come sorgente dati
     */
    public SearchService(LibriRepository libriRepo) {
        this.libriRepo = libriRepo;
    }

    /**
     * Cerca i libri il cui titolo contiene la stringa indicata.
     * <p>
     * La ricerca non distingue tra maiuscole e minuscole e ignora gli accenti.
     *
     * @param q parte del titolo da ricercare; se <code>null</code> o vuota
     *          viene restituita una lista vuota
     * @return lista dei {@link Book} il cui titolo, una volta normalizzato,
     *         contiene la stringa di ricerca normalizzata; lista vuota se
     *         il parametro è assente o non produce corrispondenze
     */
    public List<Book> cercaLibroPerTitolo(String q) {
        String needle = norm(q);
        if (needle.isEmpty()) return List.of();
        return libriRepo.all().stream()
                .filter(b -> norm(b.getTitolo()).contains(needle))
                .collect(Collectors.toList());
    }

    /**
     * Cerca i libri in base al nome di un autore.
     * <p>
     * La ricerca viene effettuata su tutti gli autori associati a ciascun
     * libro, in modo case-insensitive e senza considerare gli accenti.
     *
     * @param a nome (o parte del nome) dell'autore da ricercare;
     *          se <code>null</code> o vuoto viene restituita una lista vuota
     * @return lista dei {@link Book} che hanno almeno un autore il cui nome,
     *         una volta normalizzato, contiene la stringa di ricerca
     *         normalizzata
     */
    public List<Book> cercaLibroPerAutore(String a) {
        String needle = norm(a);
        if (needle.isEmpty()) return List.of();
        return libriRepo.all().stream()
                .filter(b -> b.getAutori().stream().anyMatch(x -> norm(x).contains(needle)))
                .collect(Collectors.toList());
    }

    /**
     * Cerca i libri in base all'autore e all'anno di pubblicazione.
     * <p>
     * Vengono selezionati solo i libri il cui anno di pubblicazione coincide
     * con il valore passato in ingresso e che hanno almeno un autore il cui
     * nome contiene la stringa indicata (dopo normalizzazione).
     *
     * @param a    nome (o parte del nome) dell'autore da ricercare
     * @param anno anno di pubblicazione richiesto; viene confrontato
     *             con il valore restituito da {@link Book#getAnno()}
     * @return lista dei {@link Book} che soddisfano entrambi i criteri
     *         (autore e anno); lista vuota se non ci sono corrispondenze
     */
    public List<Book> cercaLibroPerAutoreEAnno(String a, int anno) {
        String needle = norm(a);
        return libriRepo.all().stream()
                .filter(b -> (b.getAnno() != null && b.getAnno() == anno))
                .filter(b -> b.getAutori().stream().anyMatch(x -> norm(x).contains(needle)))
                .collect(Collectors.toList());
    }

    /**
     * Normalizza una stringa per l'utilizzo nelle ricerche testuali.
     * <p>
     * La normalizzazione prevede:
     * <ul>
     *     <li>Conversione in minuscolo utilizzando il <code>Locale.ITALIAN</code>;</li>
     *     <li>Rimozione degli accenti tramite decomposizione Unicode ed
     *         eliminazione dei segni diacritici.</li>
     * </ul>
     * Se la stringa di ingresso è <code>null</code>, viene restituita la
     * stringa vuota.
     *
     * @param s stringa da normalizzare
     * @return stringa normalizzata pronta per il confronto; stringa vuota
     *         se il parametro è <code>null</code>
     */
    private static String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ITALIAN);
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // toglie accenti
        return t;
    }
}
