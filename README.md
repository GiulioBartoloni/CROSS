# CROSS
CROSS: an exChange oRder bOokS Service

# Descrizione
Questo progetto implementa un distema di Order Book utilizzando il paradigma client-server in Java 8.

# Architettura
Il sistema è composto da:
- **Server**:
  - Fornisce un sistema di registrazione per il piazzamento degli ordini.
  - Gestisce gli ordini di ask e bid di tipo Stop, Limit e Market.
  - Esegue il matching tra ordini di acquisto e vendita secondo un algoritmo time/price priority.
  - Esegue imediatamente i MarketOrder se possono essere soddisfatti.
  - Esegue gli StopOrder quando il prezzo specificato viene raggiunto.
  - Mantiene permanentemente informazioni sullo storico e sugli utenti registrati.
  - Fornisce dati aggregati sugli ordini nello storico.

- **Client**:
  - Permette agli utenti di registrarsi, effettuare il login e modificare le proprie credenziali.
  - Permette agli utenti di inviare ordini di acquisto o vendita.
  - Consente di visualizzare l'order book corrente.
  - Interagisce con il server per inviare richieste di operazioni.
  - Gestisce la ricezione delle notifiche per il completamento degli ordini.
 
  # Comandi per il client
- `register <username> <password>`: Registra un nuovo utente con il nome utente e la password.
- `login <username> <password>`: Esegui il login con il nome utente e la password.
- `updateCredentials <username> <oldPassword> <newPassword>`: Aggiorna le credenziali dell'utente.
- `logout`: Esegui il logout.
- `help`: Mostra la lista di comandi disponibili.
- `getPriceHistory <MMYYYY>`: Mostra informazioni sugli ordini nel mese specificato.
- `insertLimitOrder <type> <size> <price>`: Inserisce un limit order.
- `insertMarketOrder <type> <size>`: Inserisce un market order.
- `insertStopOrder <type> <size> <price>`: Inserisce uno stop order.
- `getOrders`: Mostra tutti gli ordini non ancora evasi a proprio nome.
- `getOrderBook`: Visualizza l'orderBook, compreso di spread e latestPrice.
- `cancelOrder <orderId>`: Cancella l'ordine con l'ID specificato.
- `exit`: Termina l'esecuzione del client.");
-  # Comandi per il server
-  `shutdown`: effettua il salvataggio di tuti i dati permanenti e chiude il server.
-  `showOrders`: mostra gli ordini presenti nell'OrderBook.

  
  # Istruzioni per l'esecuzione
Il folder *sourceCode* contiene tutto il codice sorgente del progetto ed è quindi possibile compilarlo direttamente mediante `javac`.
Alternativamente, il folder JARS contiene un `.jar` per il client e per il server già pronti all'utilizzo.
Per l'esecuzione, il client e il server hanno ciascuno un file `.properties` con i seguenti parametri:
- **serverConfig.properties**:
  - server.port: permette di indicare la porta che il server utilizzerà per accet-
tare le connessioni.
  - session.timeout: specifica il timeout in ms, causerà le disconnessioni per
inattività.
  - server.corepoolsize: indica la dimensione del core della pool per i thread
che gestiscono i client.
  - server.maximumpoolsize: indica la dimensione massima della pool per i
thread che gestiscono i client.
  - cleanup.timer: specifica ogni quanti secondi sarà eseguita la routine di
cleanup.
  - usermap.filename: indica il nome del file per la userMap.
  - storicoordini.filename: indica il nome del file per lo storico degli ordini.
  - pendingorders.filename: indica il nome del file per gli ordini dell’orderBook
da caricare.

- **clientConfig.properties**:
  - server.host: contiene l’hostname del server con cui tenterà di stabilire una
connessione.
  - server.port: contiene la porta del server con cui tenterà di stabilire una
connessione.
  - notification.port: contiene la porta del client che verrà utilizzata per ricevere
le notifiche dal server.
