[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](/LICENSE)


<p align="center">
  <img src="docs/logoInsubria.svg" width="200" alt="Logo Università">
</p>


# Book Recommender

Applicazione desktop sviluppata in JavaFX per la gestione di librerie, libri,
recensioni e suggerimenti tra libri.  
Il progetto è stato realizzato come attività di laboratorio universitario.

---

## Funzionalità principali
- Gestione di librerie e collezioni di libri
- Inserimento, modifica e visualizzazione di recensioni
- Sistema di suggerimenti tra libri correlati
- Autenticazione utente
- Interfaccia grafica basata su JavaFX

---

## Tecnologie utilizzate
- Java 17
- JavaFX 17
- IntelliJ IDEA
- Persistenza tramite file di testo (`Libri.dati`)

---

## Avvio del progetto
1. Importare il progetto in **IntelliJ IDEA**
2. Assicurarsi di avere **Java 17** installato
3. Configurare il **JavaFX SDK** nel progetto
4. Avviare la classe `Main` oppure `BookRecommenderFX`

---

## Struttura del progetto
- `model` → classi del dominio (Book, User, Library, Review, Suggestion, ecc.)
- `repo` → accesso e gestione dei file dati
- `service` → logica applicativa
- `ui` → finestre e componenti JavaFX
- `util` → classi di utilità e supporto
- `data` → file `.dati` usati per la persistenza
- `docs` → documentazione e risorse.

---

## Autori
- Ionut Puiu
- Matteo Ferrario

## License
This project is licensed under the MIT License.

