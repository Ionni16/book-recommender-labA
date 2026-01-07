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

import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.repo.ConsigliRepository;
import bookrecommender.repo.ValutazioniRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


/**
 * La classe <code>AggregationService</code> fornisce metodi per calcolare
 * statistiche aggregate sulle valutazioni e sui suggerimenti dei libri.
 * <p>
 * Utilizza i repository {@link ValutazioniRepository} e
 * {@link ConsigliRepository} per leggere i dati grezzi dai file
 * e li trasforma in strutture riassuntive pronte per la visualizzazione
 * nelle finestre di dettaglio dei libri.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.repo.ValutazioniRepository
 * @see bookrecommender.repo.ConsigliRepository
 * @see bookrecommender.model.Review
 * @see bookrecommender.model.Suggestion
 */
public class AggregationService {
    private final ValutazioniRepository valRepo;
    private final ConsigliRepository consRepo;


    /**
     * Costruisce un nuovo servizio di aggregazione a partire dai percorsi
     * dei file delle valutazioni e dei suggerimenti.
     *
     * @param valutazioniFile percorso del file contenente le valutazioni
     * @param consigliFile    percorso del file contenente i suggerimenti
     */
    public AggregationService(Path valutazioniFile, Path consigliFile) {
        this.valRepo = new ValutazioniRepository(valutazioniFile);
        this.consRepo = new ConsigliRepository(consigliFile);
    }


    /**
     * Struttura dati che rappresenta le statistiche aggregate sulle
     * valutazioni di un singolo libro.
     * <p>
     * Contiene:
     * <ul>
     *     <li>Il numero totale di valutazioni disponibili;</li>
     *     <li>La distribuzione dei voti finali;</li>
     *     <li>Le medie per ciascun criterio di valutazione
     *         (stile, contenuto, gradevolezza, originalità, edizione);</li>
     *     <li>La media complessiva del voto finale.</li>
     * </ul>
     */
    public static class ReviewStats {
        public int count;

        /**
         * Mappa che associa a ciascun ID di libro suggerito il numero di
         * utenti che lo hanno indicato come correlato.
         * <p>
         * La mappa è mantenuta in ordine di inserimento, che in questo
         * caso corrisponde all'ordinamento per frequenza (dal più suggerito
         * al meno suggerito).
         */
        public final Map<Integer,Integer> distribuzioneVoti = new TreeMap<>(); // votoFinale  count
        public double mediaStile, mediaContenuto, mediaGradevolezza, mediaOriginalita, mediaEdizione;
        public double mediaVotoFinale; //  NUOVO: media totale (voto finale)
    }


    /**
     * Struttura dati che rappresenta le statistiche aggregate sui
     * suggerimenti di libri correlati a un libro base.
     * <p>
     * Per ogni ID di libro suggerito viene memorizzato il numero di utenti
     * che lo hanno indicato come correlato al libro considerato.
     */
    public static class SuggestionsStats {
        // idLibro suggerito - numero di utenti che l'hanno suggerito
        public Map<Integer,Integer> suggeritiCount = new LinkedHashMap<>();
    }


    /**
     * Calcola le statistiche aggregate sulle valutazioni per un determinato libro.
     * <p>
     * Le statistiche includono:
     * <ul>
     *     <li>Numero totale di valutazioni;</li>
     *     <li>Distribuzione dei voti finali;</li>
     *     <li>Medie per ciascun criterio (stile, contenuto, gradevolezza,
     *         originalità, edizione);</li>
     *     <li>Media complessiva del voto finale.</li>
     * </ul>
     * Se non esistono valutazioni per il libro indicato, viene restituita
     * una struttura con conteggio pari a zero e campi numerici a valore
     * predefinito.
     *
     * @param bookId identificatore del libro di cui calcolare le statistiche
     * @return oggetto {@link ReviewStats} popolato con i dati aggregati
     * @throws IOException in caso di errore di I/O durante la lettura delle valutazioni
     */
    public ReviewStats getReviewStats(int bookId) throws IOException {
        List<Review> reviews = valRepo.findByBookId(bookId);
        ReviewStats s = new ReviewStats();
        s.count = reviews.size();
        if (s.count == 0) return s;

        int sumS=0, sumC=0, sumG=0, sumO=0, sumE=0, sumVF=0;
        for (Review r : reviews) {
            s.distribuzioneVoti.merge(r.getVotoFinale(), 1, Integer::sum);
            sumS += r.getStile();
            sumC += r.getContenuto();
            sumG += r.getGradevolezza();
            sumO += r.getOriginalita();
            sumE += r.getEdizione();
            sumVF += r.getVotoFinale();     //  sommo il voto finale
        }
        s.mediaStile        = sumS / (double)s.count;
        s.mediaContenuto    = sumC / (double)s.count;
        s.mediaGradevolezza = sumG / (double)s.count;
        s.mediaOriginalita  = sumO / (double)s.count;
        s.mediaEdizione     = sumE / (double)s.count;
        s.mediaVotoFinale   = sumVF / (double)s.count; //  media totale
        return s;
    }


    /**
     * Calcola le statistiche aggregate sui suggerimenti di libri correlati
     * per un determinato libro base.
     * <p>
     * Per ogni suggerimento presente a file viene incrementato il conteggio
     * del relativo ID di libro. Il risultato finale è ordinato in modo
     * decrescente rispetto al numero di suggerimenti ricevuti, così da
     * poter visualizzare facilmente i libri più frequentemente associati.
     *
     * @param bookId identificatore del libro base di cui analizzare i suggerimenti
     * @return oggetto {@link SuggestionsStats} contenente, per ciascun ID di libro
     *         suggerito, il numero di utenti che lo hanno indicato
     * @throws IOException in caso di errore di I/O durante la lettura dei suggerimenti
     */
    public SuggestionsStats getSuggestionsStats(int bookId) throws IOException {
        List<Suggestion> sugs = consRepo.findByBookId(bookId);
        SuggestionsStats s = new SuggestionsStats();
        for (Suggestion sg : sugs) {
            for (Integer sug : sg.getSuggeriti()) {
                s.suggeritiCount.merge(sug, 1, Integer::sum);
            }
        }
        // Ordina per conteggio desc
        s.suggeritiCount = s.suggeritiCount.entrySet().stream()
            .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(LinkedHashMap::new,
                     (m,e) -> m.put(e.getKey(), e.getValue()),
                     LinkedHashMap::putAll);
        return s;
    }
}
