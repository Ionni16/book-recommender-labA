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

package bookrecommender.util;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Utilities {


    /**
     * Verifica che un utente possieda un certo libro in almeno una delle
     * proprie librerie.
     * <p>
     * Il controllo viene eseguito scorrendo il file <code>Librerie.dati</code>
     * e cercando lo <code>userid</code> indicato e la presenza del relativo
     * <code>bookId</code> tra gli ID elencati nelle librerie.
     *
     * @param userid identificatore dell'utente
     * @param bookId identificatore del libro da cercare nelle librerie
     * @return {@code true} se il libro è presente in almeno una libreria
     *         dell'utente, {@code false} altrimenti
     * @throws Exception in caso di errore di I/O durante la lettura del file
     *                   delle librerie
     */
    public static boolean utenteHaLibroInLibreria(String userid, int bookId, Path fileLibrerie) throws Exception {
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
