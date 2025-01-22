package server;

import java.time.*;
import java.util.concurrent.*;
import java.net.*;
import java.util.*;
import java.io.*;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import serverDataStructures.*;
import clientFunctions.Requests.*;
import serverFunctions.Responses.*;

public class ServerMain {
	// inizializzo alcune variabili che utilizzerò in tutto il codice
	volatile static int lastId = 1;
    static CROSSorderBook crossOrderBook = CROSSorderBook.getInstance();
    static ConcurrentHashMap<String, List<HistoryOrder>> orderHistory = null;
	
	// definisco il Singleton (eager) del CROSSorderBook
    // in quanto tale, ha il costruttore privato e i vari metodi getter per gli orderBook e per l'istanza
    static class CROSSorderBook {
    	static LimitOrderBook limitOrderBook;
    	static StopOrderBook stopOrderBook;
        private static final CROSSorderBook INSTANCE = new CROSSorderBook();
        
        private CROSSorderBook() {
        	stopOrderBook = new StopOrderBook();
        	limitOrderBook = new LimitOrderBook();
        	stopOrderBook.setLimitOrderBook(limitOrderBook);
        }

        public static CROSSorderBook getInstance() {
            return INSTANCE;
        }
        
        public static LimitOrderBook getLimitOrderBook() {
        	return limitOrderBook;
        }
        
        public static StopOrderBook getStopOrderBook() {
        	return stopOrderBook;
        }
    }
	
    // definisco il limitOrderBook, con tutti i metodi per aggiunta dei limit e market order
    static class LimitOrderBook extends OrderBook {
    	Integer currentMarketPrice;
    	StopOrderBook stopOrderBook;
        public LimitOrderBook() {
            super();
            stopOrderBook=CROSSorderBook.getStopOrderBook();
            this.currentMarketPrice=null;
        }
        
        // per piazzare un bidLimitOrder, inizio scalando eventuali ordini che possono gia' soddisfarlo
        // dopo questo, se la size e' ancora maggiore di 0, lo aggiungo al bidOrderBook
        public long placeBidLimitOrder(int price, User user, int size) {
        	LimitOrder newOrder = null;
        	synchronized(crossOrderBook) {
	        	newOrder = new LimitOrder(user, price, size, SaleType.bid,lastId++);
	            for (OrderBookElement orderPriceRange : this.askOrderBook.orderBook) {
	                if (newOrder.size>0&&price >= orderPriceRange.getPrice()) 
	                	matchOrders(orderPriceRange, newOrder,false);
	                else
	                    break;
	            }
	
	            if (size > 0) {
	                this.bidOrderBook.addOrder(newOrder);
	            }

	            this.stopOrderBook.checkForTriggers(currentMarketPrice);
        	}
            return newOrder.orderId;
        }
        
        // per piazzare un askLimitOrder, inizio scalando eventuali ordini che possono gia' soddisfarlo
        // dopo questo, se la size e' ancora maggiore di 0, lo aggiungo all'askOrderBook
        long placeAskLimitOrder(int price, User user, int size) {
        	LimitOrder newOrder = null;
        	synchronized(crossOrderBook) {
	        	newOrder = new LimitOrder(user, price, size,SaleType.ask,lastId++);
	            for (OrderBookElement orderPriceRange : this.bidOrderBook.orderBook) {
	                if (newOrder.size>0&&price <= orderPriceRange.getPrice()) 
	                	matchOrders(orderPriceRange,newOrder,false);
	                else
	                    break;
	            }
	            if (size > 0)
	                this.askOrderBook.addOrder(newOrder);

	            this.stopOrderBook.checkForTriggers(currentMarketPrice);
	        }
        	return newOrder.orderId;
        }
        // per piazzare un bidMarketOrder, controllo la disponibilita' per ritornare un eventuale fallimento di esecuzione
        // se puo' essere soddisfatto, faccio il match
        long placeBidMarketOrder(User user, int size, StopOrder triggeredStopOrder) {
        	synchronized(crossOrderBook) {
	        	MarketOrder newOrder = new MarketOrder(user, size,SaleType.bid,lastId++);
	            if (checkAvailability(this.askOrderBook.orderBook, user.getUsername(), size)) {
	                for (OrderBookElement orderPriceRange : this.askOrderBook.orderBook) {
	                	if(newOrder.size<=0)
	                		break;
	                	matchOrders(orderPriceRange, newOrder,triggeredStopOrder!=null);
	                }
	                this.stopOrderBook.checkForTriggers(currentMarketPrice);

	                if(triggeredStopOrder!=null) {
	                	triggeredStopOrder.price=currentMarketPrice;
	                	notifyUser(triggeredStopOrder);
	                	return 0;
	                }
	                return newOrder.orderId;
	            }else {
	            	return -1;
	            }
        	}
        }
        // per piazzare un askMarketOrder, controllo la disponibilita' per ritornare un eventuale fallimento di esecuzione
        // se puo' essere soddisfatto, faccio il match
        long placeAskMarketOrder(User user, int size,StopOrder triggeredStopOrder) {
        	synchronized(crossOrderBook) {
	        	MarketOrder newOrder = new MarketOrder(user, size,SaleType.ask,lastId++);
	        	
	            if (checkAvailability(this.bidOrderBook.orderBook, user.getUsername(), size)) {
	            	for (OrderBookElement orderPriceRange : this.bidOrderBook.orderBook) {
	            		if(newOrder.size<=0)
	                		break;
	                	matchOrders(orderPriceRange, newOrder,triggeredStopOrder!=null);
	                	}
	                this.stopOrderBook.checkForTriggers(currentMarketPrice);
	                if(triggeredStopOrder!=null) {
	                	triggeredStopOrder.price=currentMarketPrice;
	                	notifyUser(triggeredStopOrder);
	                	return 0;
	                }
	                return newOrder.orderId;
	            } else {
	            	return -1;
	            }
	        }
        }
    }
    
  //definisco lo stopOorderBook, con tutti i metodi per aggiunta degli stopOrder e verifica di eventuali trigger
    static class StopOrderBook extends OrderBook{
    	LimitOrderBook limitOrderBook;
    	public StopOrderBook() {
    		super();
    		limitOrderBook = CROSSorderBook.getLimitOrderBook();
    	}
    	
    	public void setLimitOrderBook(LimitOrderBook limitOrderBook) {
    		this.limitOrderBook=limitOrderBook;
    	}
    	
    	// per piazzare un askStopOrder verifico se la condizione e' gia' soddisfatta, altrimenti aggiungo nello stopOrderBook
    	public long placeAskStopOrder(int price, User user, int size) {
    		synchronized(crossOrderBook) {
	    		StopOrder newOrder = new StopOrder(user, price,size,SaleType.ask, lastId++);
	    		if(limitOrderBook.currentMarketPrice!=null && price>=limitOrderBook.currentMarketPrice) {
	    			limitOrderBook.placeAskMarketOrder(user, size, newOrder);
	    			return newOrder.orderId;
	    		}
	    		this.askOrderBook.addOrder(newOrder);
	    		
				return newOrder.orderId;
    		}
    	}

    	// per piazzare un bidStopOrder verifico se la condizione e' gia' soddisfatta, altrimenti aggiungo nello stopOrderBook
    	public long placeBidStopOrder(int price, User user, int size) {
    		synchronized(crossOrderBook) {
	    		StopOrder newOrder = new StopOrder(user, price,size,SaleType.bid, lastId++);
	    		if(limitOrderBook.currentMarketPrice!=null &&price<=limitOrderBook.currentMarketPrice) {
	    			limitOrderBook.placeBidMarketOrder(user, size, newOrder);
	    			return newOrder.orderId;
	    		}
	    		this.bidOrderBook.addOrder(newOrder);
				return newOrder.orderId;
    		}
    	}
    	// la funzione controlla eventuali triggers, verifica tutti gli elementi di ask e bid orderBook, trasformandolo in marketOrders
    	public void checkForTriggers(Integer newMarketPrice) {
    		if(newMarketPrice!=null) {
	    		synchronized(crossOrderBook) {
	    			List<Order> completedOrders = new ArrayList<>();
		    		for (OrderBookElement orderPriceRange : this.askOrderBook.orderBook) {
		                for (Order order : orderPriceRange.orderList) {
		                	if(order.size>0 && newMarketPrice<=order.price) {
		                		completedOrders.add(order);
			            		orderPriceRange.size-=order.size;
			            		order.size=0;
		                	}
		                }
		    		}
		    		for(Order toRemove: completedOrders) 
		            	limitOrderBook.placeAskMarketOrder(toRemove.user, toRemove.originalSize, (StopOrder)toRemove);
		    	
		    		completedOrders = new ArrayList<>();
					for (OrderBookElement orderPriceRange : this.bidOrderBook.orderBook) {
			            for (Order order : orderPriceRange.orderList) {
			            	if(order.size>0 && newMarketPrice>=order.price) {
			            		completedOrders.add(order);
			            		orderPriceRange.size-=order.size;
			            		order.size=0;
			            	}
			            }
					}
					for(Order toRemove: completedOrders)
		            	limitOrderBook.placeBidMarketOrder(toRemove.user, toRemove.originalSize, (StopOrder)toRemove);
				}
	    	}
    	}
    }
    
    // la funzione esegue l'algoritmo di matching confrontando un ordine con un certo priceRange
    // la parte della priorita' sui prezzi dell'algoritmo e' implementata prima di questa chiamata nel codice
    static public void matchOrders(OrderBookElement orderPriceRange, Order newOrder, boolean wasStopOrder) {
    	LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
    	// sincronizzo sull'orderBook
    	synchronized(crossOrderBook) {
    		// itero su tutti gli ordini nel pricerange
	        for (Order order : orderPriceRange.orderList) {
	        	// se il mio ordine si e' svuotato, posso uscire
	        	if(newOrder.size<=0)
	        		break;
        		// se lo username e' valido, posso passare ai compare
        		if(!newOrder.user.getUsername().equals(order.user.getUsername())) {
	            	// se arrivo a terminare il mio ordine, aggiorno le varie size e il marketPrice
	                if (newOrder.size <= order.size) {
	                    limitOrderBook.currentMarketPrice=order.price;
	                	if(!wasStopOrder) 
	                		addToHistory(newOrder.orderId,newOrder.type,newOrder.orderType,newOrder.size,order.price);
	                	else
	                		addToHistory(newOrder.orderId,newOrder.type,OrderType.stop,newOrder.size,order.price);
	                	addToHistory(order.orderId,order.type,order.orderType,newOrder.size,order.price);
	                    order.size -= newOrder.size;
	                    orderPriceRange.setSize(orderPriceRange.getSize() - newOrder.size);
	                    newOrder.size = 0;
	                    if(newOrder.size==order.size)
	                    	notifyUser(order);
	                    if(!wasStopOrder) {
	                    	if(newOrder instanceof MarketOrder)
	                    		newOrder.price=order.price;
	                    	notifyUser(newOrder);
	                    }
	                    break;
	                }
                    limitOrderBook.currentMarketPrice=order.price;
	                if(!wasStopOrder) 
	                	addToHistory(newOrder.orderId,newOrder.type,OrderType.stop,order.size,order.price);
	                else
	                	addToHistory(newOrder.orderId,newOrder.type,newOrder.orderType,order.size,order.price);
                	addToHistory(order.orderId,order.type,order.orderType,order.size,order.price);
	                // altrimenti, completo l'ordine nel priceRange e aggiorno il mio, procedero' nella lista
	                orderPriceRange.setSize(orderPriceRange.getSize() - order.size);
	                newOrder.size -= order.size;
	                order.size = 0;
	                notifyUser(order);
	            }
	        }
    	}
    }
    
    // la funzione controlla se c'è disponibilità per soddisfare la size specificata
    static boolean checkAvailability(TreeSet<OrderBookElement> list, String username, int size) {
        int tot = 0;
        synchronized(crossOrderBook) {
	        for (OrderBookElement orderPriceRange : list) {
	            for (Order order : orderPriceRange.orderList) {
	                if (!order.user.getUsername().equals(username))
	                    tot += order.size;
	            }
	        }
        }
        return tot >= size;
	}
    
    // questo thread gestisce la pulizia degli orderBook
    // con la sincronizzazione sono sicuro di non incorrere in concurrentModificationException
    // inoltre questo thread salva anche periodicamente i file di userMap e storicoOrdini
    static class OrderBookCleanup implements Runnable {

    	LimitOrderBook limitOrderBook;
    	StopOrderBook stopOrderBook;
    	ConcurrentHashMap<String, User> userMap;
    	ConcurrentHashMap<String, List<HistoryOrder>> orderHistory;
    	String storicoOrdiniFilename;
    	String userMapFilename;
    	public OrderBookCleanup(LimitOrderBook limitOrderBook, StopOrderBook stopOrderBook, ConcurrentHashMap<String, User> userMap, ConcurrentHashMap<String, List<HistoryOrder>> orderHistory,String storicoOrdiniFilename,String userMapFilename) {
    		this.limitOrderBook=limitOrderBook;
    		this.stopOrderBook=stopOrderBook;
    		this.userMap=userMap;
    		this.orderHistory=orderHistory;
    		this.storicoOrdiniFilename=storicoOrdiniFilename;
    		this.userMapFilename=userMapFilename;
    	}
    	
        @Override
        public void run() {
            synchronized(crossOrderBook) {
            	
            	for (OrderBookElement orderPriceRange : limitOrderBook.bidOrderBook.orderBook) 
            		orderPriceRange.orderList.removeIf(item->item.size==0);
            	
            	for (OrderBookElement orderPriceRange : limitOrderBook.askOrderBook.orderBook) 
            		orderPriceRange.orderList.removeIf(item->item.size==0);
            	
            	limitOrderBook.askOrderBook.orderBook.removeIf(item->item.size==0);
            	limitOrderBook.bidOrderBook.orderBook.removeIf(item->item.size==0);
            	
            	for (OrderBookElement orderPriceRange : stopOrderBook.bidOrderBook.orderBook) 
            		orderPriceRange.orderList.removeIf(item->item.size==0);
            	
            	for (OrderBookElement orderPriceRange : stopOrderBook.askOrderBook.orderBook) 
            		orderPriceRange.orderList.removeIf(item->item.size==0);
            	
            	stopOrderBook.askOrderBook.orderBook.removeIf(item->item.size==0);
            	stopOrderBook.bidOrderBook.orderBook.removeIf(item->item.size==0);
            	
            	saveUserMapToFile(userMap,userMapFilename);
            	saveOrderHistoryToFile(orderHistory,storicoOrdiniFilename);
            	
            }
        }
    }
    
    // questo thread ascolta gli input e accetta i comandi di shutdown e di showOrders
    public static class InputListener implements Runnable {
    	ConcurrentHashMap<String, User> userMap;
    	ConcurrentHashMap<String, List<HistoryOrder>> orderHistory;
    	ExecutorService clientHandlerPool;
    	
    	LimitOrderBook limitOrderBook;
    	StopOrderBook stopOrderBook;
    	
    	File configFile;
    	
    	String storicoOrdiniFilename;
    	String userMapFilename;
    	String pendingOrdersFilename;
    	public InputListener(ConcurrentHashMap<String, User> userMap, ConcurrentHashMap<String, List<HistoryOrder>> orderHistory, ExecutorService clientHandlerPool,LimitOrderBook limitOrderBook, StopOrderBook stopOrderBook,File configFile, String storicoOrdiniFilename, String userMapFilename, String pendingOrdersFilename) {
    		this.userMap=userMap;
    		this.orderHistory=orderHistory;
    		this.clientHandlerPool=clientHandlerPool;
    		this.limitOrderBook=limitOrderBook;
    		this.stopOrderBook=stopOrderBook;
    		this.configFile=configFile;
    		this.storicoOrdiniFilename=storicoOrdiniFilename;
    		this.userMapFilename=userMapFilename;
    		this.pendingOrdersFilename=pendingOrdersFilename;
    	}
    	
        @Override
        public void run() {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                
            	
                String input;

                while (true) {
                	synchronized(System.in) {
                		input = reader.readLine();
                	}
                    
                    if (input.equals("shutdown")) 
                        break;
                    if (input.equals("showOrders")) {
                    	synchronized(System.out) {
	                    	System.out.println("\n\nSELL(ask):");
	            	        limitOrderBook.askOrderBook.printOrders();
	            	
	            	        System.out.println("\n\nBUY(bid):");
	            	        limitOrderBook.bidOrderBook.printOrders();
	            	        
	            	        System.out.println("\n\nSELL(ask):");
	            	        stopOrderBook.askOrderBook.printOrders();
	            	
	            	        System.out.println("\n\nBUY(bid):");
	            	        stopOrderBook.bidOrderBook.printOrders();
	                    }
                    }
                }
            } catch (IOException e) {
            	synchronized(System.out) {
            		System.out.println("Errore di I/O durante la lettura: " + e.getMessage());
            	}
            } finally {
            	saveUserMapToFile(userMap,userMapFilename);
            	saveOrderHistoryToFile(orderHistory,storicoOrdiniFilename);
            	saveOrderBookToFile(pendingOrdersFilename);
            	
            	// notifico tutti gli utenti di cui ho un indirizzo e port salvati per avvisarli della chiusura
            	try (DatagramSocket socket = new DatagramSocket()) {
        			ClosedServerNotification notif = new ClosedServerNotification();
        			Gson gson = new Gson();
        			String notification = gson.toJson(notif);
        			for (String key : userMap.keySet()) {
                        User user = userMap.get(key);
                        if(user.getAddress()!=null) {
                        	synchronized(socket) {
		                        InetAddress address = InetAddress.getByName(user.getAddress());
		                        DatagramPacket packet = new DatagramPacket(notification.getBytes(), notification.length(), address, user.getPort());
		                        socket.send(packet);
                        	}
                        }
                    }
                      
                } catch (Exception e) {
                    e.printStackTrace();
                }
            	
            	clientHandlerPool.shutdownNow();
            	System.exit(0);
            }
        }
    }
    
	public static void main(String[] args) {
		// leggo il file config per il server con tutti i parametri
		File configFile = new File("serverConfig.properties");
		int serverPort=0;
		int sessionTimeout=0;
		int corePoolSize=10;
		int maximumPoolSize=10;
		int cleanupTimer=10;
		String userMapFilename="userMap.json";
		String storicoOrdiniFilename="storicoOrdini.json";
		String pendingOrdersFilename="pendingOrders.json";
		try (FileReader reader = new FileReader(configFile)){
			Properties props = new Properties();
			props.load(reader);
			serverPort = Integer.parseInt(props.getProperty("server.port"));
			sessionTimeout = Integer.parseInt(props.getProperty("session.timeout"));
			corePoolSize = Integer.parseInt(props.getProperty("server.corepoolsize"));
			maximumPoolSize = Integer.parseInt(props.getProperty("server.maximumpoolsize"));
			cleanupTimer= Integer.parseInt(props.getProperty("cleanup.timer"));
			userMapFilename= props.getProperty("usermap.filename");
			storicoOrdiniFilename= props.getProperty("storicoordini.filename");
			pendingOrdersFilename= props.getProperty("pendingorders.filename");
			synchronized(System.out) {
				System.out.println("Il file di configurazione è stato letto correttamente.");
			}
		}catch (FileNotFoundException ex) {
			synchronized(System.err) {
				System.err.println("Il file di configurazione non esiste.");
			}
        } catch (IOException e) {
        	synchronized(System.err) {
        		System.err.println("Impossibile leggere il file di configurazione: " + e.getMessage());
        	}
        }
		
		// creo la userMap e chiamo la funzione per caricarla/crearla
		ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();
		loadUserMapFromFile(userMap,userMapFilename);
		
		// creo la mappa per lo storico degli ordini e la carico/creo
		 orderHistory = new ConcurrentHashMap<>();
		 loadOrderHistory(orderHistory,storicoOrdiniFilename);
		 
		 // carico l'orderbook con gli ordini da evadere
		 loadOrderBook(userMap,pendingOrdersFilename);
		
		// preparo il threadpool dei client handler
		ExecutorService clientHandlerPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		
		
		LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
    	StopOrderBook stopOrderBook = CROSSorderBook.getStopOrderBook();
		
    	// eseguo il thread di cleanup secondo i parametri specificati
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        OrderBookCleanup task = new OrderBookCleanup(limitOrderBook,stopOrderBook,userMap,orderHistory,storicoOrdiniFilename,userMapFilename);
    	scheduler.scheduleAtFixedRate(task, 0, cleanupTimer, TimeUnit.SECONDS);
    	
		// eseguo il thread che ascolta gli input
    	Thread exitListener = new Thread(new InputListener(userMap,orderHistory,clientHandlerPool,limitOrderBook,stopOrderBook,configFile,storicoOrdiniFilename,userMapFilename,pendingOrdersFilename));
    	exitListener.start();
    	
		// loop principale del welcome thread
		try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
			
			synchronized(System.out) {
				System.out.println("Server in ascolto sulla porta " + serverPort);
			}
           
            // accetto nuove connessioni, imposto il timeout di disconnessione e sottometto il thread al threadpool
            while(true) {
	            Socket clientSocket = serverSocket.accept();
	            synchronized(System.out) {
	            	System.out.println("Nuova connessione accettata da " + clientSocket.getInetAddress());
	            }
	            clientSocket.setSoTimeout(sessionTimeout);
	            clientHandlerPool.submit(new clientHandler(clientSocket, userMap, orderHistory));    

            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
        	saveUserMapToFile(userMap, userMapFilename);
        	saveOrderHistoryToFile(orderHistory,storicoOrdiniFilename);
        	clientHandlerPool.shutdownNow();
        	System.exit(0);
        }
	}
	
	
	
	// la funzione gestisce l'esecuzione del thread che gestisce il client
	static class clientHandler implements Runnable{
		public Socket clientSocket;
		private ConcurrentHashMap<String, User> userMap;
		private ConcurrentHashMap<String, List<HistoryOrder>> orderHistory;
		
		public clientHandler(Socket clientSocket, ConcurrentHashMap<String, User> userMap, ConcurrentHashMap<String, List<HistoryOrder>> orderHistory) {
			this.clientSocket=clientSocket;
			this.userMap=userMap;
			this.orderHistory=orderHistory;
		}
		
		// override della funzione run, per la sottomissione al threadpool
		public void run() {
			
			LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
	    	StopOrderBook stopOrderBook = CROSSorderBook.getStopOrderBook();
			
			// try-with-resources per la chiusura automatica degli stream di input e output
			try(
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
			){
				User thisUser = null;

				// leggo la porta ricevuta, la impostero' a tutti gli utenti che fanno login con questa
				String json = null;
				synchronized(in) {
					json = in.readLine();
				}
				
				Gson gson = new Gson();
	            int portReceived = gson.fromJson(json, int.class);
				
				// ciclo di lettura, gestisce le richieste del client
				while(true) {
					// legge la richiesta generica per determinarne il tipo
					String req=null;
					synchronized(in) {
						req = in.readLine();
					}
		            // se la richiesta è null, il socket è stato chiuso dal lato del client e si è disconnesso
		            if(req==null)
		            	return;
		            
		            // prendo la richiesta genrica per poterne estrarre il tipo
		            Request genericRequest = gson.fromJson(req,Request.class);
		            
		            // inizializzazione di variabili
		            Response res = null;
		            String response = null;
		            
		            // faccio uno switch sul tipo di richiesta
		            switch(genericRequest.getRequestType()) {
		            
		            	// gestisce il caso del register di un utente
		            	case register:
		            		// utilizzo fromJson per creare l'oggetto registerRequest
		            		registerRequest registerRequest = gson.fromJson(req,registerRequest.class);
		            		// creo un nuovo utente ed inizio le varie verifiche	            		
		                    User newUser = new User(registerRequest.getUsername(),registerRequest.getPassword(),-1);
		                    // se la password non è valida, imposto la risposta
		                    if(!isPasswordValid(registerRequest.getPassword())) {
		                    	res = new AccountResponse(101,"Password non valida. Deve contenere almeno 8 caratteri, una lettera maiuscola ed un numero.");
		                    // provo ad aggiungere l'utente, se entro in questo if è già presente e aggiungo alla risposta che lo username è già in utilizzo
		                    } else if(userMap.putIfAbsent(newUser.getUsername(), newUser)!=null) {
		            	    	res = new AccountResponse(102,"Lo username è già in utilizzo.");
		            	    // se passo tutti i controlli, ho aggiunto l'utente
		            	    } else {
		            	    	res = new AccountResponse(100,"Account registrato con successo.");
		            	    }
		                    // trasformo la risposta creata in JSON e la invio
		            	    response=gson.toJson(res);
		            	    synchronized(out) {
		            	    	out.println(response);
		            	    }
		            	    break;
		            	    
		            	    
		            	    
		            	// gestisce il caso del login di un utente    
		            	case login:
		            		// utiilzzo fromJson per creare l'oggetto loginRequest
		            		loginRequest loginRequest = gson.fromJson(req,loginRequest.class);
		            		// inizio i controlli verificando che l'utente sia nella userMap
		            		if(!userMap.containsKey(loginRequest.getUsername())) {
		            			res = new AccountResponse(101,"Non esiste nessun utente con il seguente username.");
		            		// Controllo le credenziali
		            		} else if(!areCredentialsValid(userMap,loginRequest.getUsername(),loginRequest.getPassword())) {
		            			res = new AccountResponse(101,"La password inserita è errata."); 
		            		} else{
		            			// prendo l'utente selezionato e controllo se è già loggato
		            			User user = userMap.get(loginRequest.getUsername());
		            			if(user.isLoggedOn()){
		            				res = new AccountResponse(102,"Utente già loggato."); 
		            			} else {
		            				// se il login ha successo salvo l'utente per verifiche future e imposto lo stato
		            				thisUser=user;
				                    thisUser.setLoggedOn(true);
				                    thisUser.setPort(portReceived);
				                    thisUser.setAddress(clientSocket.getInetAddress().getHostAddress());
				                    res = new AccountResponse(100,"Login effettuato con successo, benvenuto "+thisUser.getUsername()+"!"); 
		            			}
		            		}
		            		// trasformo la risposta create in JSON e la invio
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            	    break;
		            	    
		            	    
		            	    
		            	// gestisco il caso della richiesta di logout    
		            	case logout:
		            		// verifico che il client connesso sie effettivamente loggato
		            		// se così, imposto lo stato di loggedOn e lo user a null e preparo la risposta
		            		if(thisUser!=null) {
		            			thisUser.setLoggedOn(false);
		            			thisUser=null;
		            			res = new AccountResponse(100,"Logout effettuato con successo."); 
		            		} else {
		            			res = new AccountResponse(101,"Non è stato possibile effettuare il logout."); 
		            		}
		            		// trasformo la risposta create in JSON e la invio		            		
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            		break;
		          
		            		

			            // gestisco il caso della richiesta di update delle credenziali
		            	case updateCredentials:
		            		// utilizzo il fromJson per creare l'oggetto updateCredentialsRequest
		            		updateCredentialsRequest updateCredentialsRequest = gson.fromJson(req,updateCredentialsRequest.class);
		            		// verifico subito se la vecchia password proposta e la nuova combaciano
		            		if(updateCredentialsRequest.getOldPassword().equals(updateCredentialsRequest.getNewPassword())) {
		            			res = new AccountResponse(103,"Inserisci una password diversa da quella precedente."); 
		            		// controllo se l'utente esiste
		            		} else if(!userMap.containsKey(updateCredentialsRequest.getUsername())) {
		            			res = new AccountResponse(102,"Non esiste nessun utente con il seguente username."); 
		            		// controllo se le credenziali vecchie sono valide
		            		} else if(!areCredentialsValid(userMap,updateCredentialsRequest.getUsername(),updateCredentialsRequest.getOldPassword())) {
		            			res = new AccountResponse(102,"La password vecchia inserita è errata."); 
		            		// controllo se la password segue il formato richiesto
		            		} else if(!isPasswordValid(updateCredentialsRequest.getNewPassword())) {
		            			res = new AccountResponse(101,"La password nuova inserita non è valida."); 
		            		} else {
		            			// creo l'oggetto dell'utente in questione
		            			User user = userMap.get(updateCredentialsRequest.getUsername());
		            			// se loggato, impedisco la modifica
		            			if(user.isLoggedOn()){
		            				res = new AccountResponse(104,"Utente già loggato."); 
		            			} else {
		            				// altrimenti setto la password a quella nuova
		            				user.setPassword(updateCredentialsRequest.getNewPassword());
				                    res = new AccountResponse(100,"Cambiamento effettuato con successo"); 
		            			}
		            		}
		            		// trasformo la risposta create in JSON e la invio
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            		break;
		            		
		            		
		            	case getPriceHistory:
		            		// ottengo la richiesta
		            		getPriceHistoryRequest priceHistoryRequest = gson.fromJson(req,getPriceHistoryRequest.class);
		            		// inizializzo le strutture dati necessarie
		            		List<HistoryOrder> orders = orderHistory.get(priceHistoryRequest.getMonth());
		            		Map<Integer,OHLC> monthlyOHLC = new HashMap<>();
		            		
		            		// se ho degli ordini in quel mese, itero tra tutti
		            		if(orders != null) {
		                        for (HistoryOrder ordine : orders) {
		                        	// estraggo la data e il prezzo in USD
		                        	Integer day = ordine.getDate().getDayOfMonth();
		                            //double price = (double)ordine.getPrice()/(double)ordine.getSize();
		                            long price = ordine.getUSDPrice();
		                        	// se quella che ho è la prima nella data, aggiungo l'oggetto nuovo OHLC
		                            if(!monthlyOHLC.containsKey(day)) {
		                            	monthlyOHLC.put(day, new OHLC(day,price,price,price,price));
		                            } else {
		                            	// altrimenti provo ad aggiornare il Low e High e aggiorno ogni volta il CLose
		                            	OHLC ohlc = monthlyOHLC.get(day);
		                                
		                                ohlc.setHigh(Math.max(ohlc.getHigh(), price));
		                                ohlc.setLow(Math.min(ohlc.getLow(), price));
		                                ohlc.setClose(price);
		                            }
			                    }
		            		}
		            		// se la OHLC è vuota, la annullo per unificare il comportamento
	                        if(monthlyOHLC.size()==0)
	                        	monthlyOHLC = null;
		            		// creo, formatto ed invio la risposta
	                        res = new priceHistoryResponse(monthlyOHLC);
	                        response=gson.toJson(res);
	                        synchronized(out) {
	                        	out.println(response);
	                        }
		            		break;
		            		
		            	// per inserire un limit order 
		            	case insertLimitOrder:
		            		long newLimitOrderId;
		            		insertLimitOrderRequest limitOrderRequest = gson.fromJson(req, insertLimitOrderRequest.class);
		            		// faccio differenza nel caso ask e bid
		            		if(limitOrderRequest.type.equals("ask"))
		            			newLimitOrderId=limitOrderBook.placeAskLimitOrder(limitOrderRequest.price,thisUser,limitOrderRequest.size);
		            		else
		            			newLimitOrderId=limitOrderBook.placeBidLimitOrder(limitOrderRequest.price,thisUser,limitOrderRequest.size);
		            		
		            		// preparo la risposta in base al codice ottenuto e la invio
		            		res = new OrderResponse(newLimitOrderId);
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            		break;
		            		
		            	// per inserire un market order 
		            	case insertMarketOrder:
		            		long newMarketOrderId;
		            		insertMarketOrderRequest marketOrderRequest = gson.fromJson(req, insertMarketOrderRequest.class);
		            		// faccio differenza nel caso ask e bid
		            		if(marketOrderRequest.type.equals("ask"))
		            			newMarketOrderId=limitOrderBook.placeAskMarketOrder(thisUser,marketOrderRequest.size, null);
		            		else
		            			newMarketOrderId=limitOrderBook.placeBidMarketOrder(thisUser,marketOrderRequest.size, null);
		            		
		            		// preparo la risposta in base al codice ottenuto e la invio
		            		res = new OrderResponse(newMarketOrderId);
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            		break;
		            	
		            	// per inserire uno stop order 
		            	case insertStopOrder:
		            		long newStopOrderId;
		            		insertStopOrderRequest stopOrderRequest = gson.fromJson(req, insertStopOrderRequest.class);
		            		// faccio differenza nel caso ask e bid
		            		if(stopOrderRequest.type.equals("ask"))
		            			newStopOrderId=stopOrderBook.placeAskStopOrder(stopOrderRequest.price,thisUser,stopOrderRequest.size);
		            		else
		            			newStopOrderId=stopOrderBook.placeBidStopOrder(stopOrderRequest.price,thisUser,stopOrderRequest.size);
		            		
		            		// preparo la risposta in base al codice ottenuto e la invio
		            		res = new OrderResponse(newStopOrderId);
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            		break;
		            		
		            	// per la richiesta di cancellazione dell'ordine
		            	case cancelOrder:
		            		cancelOrderRequest cancelRequest = gson.fromJson(req, cancelOrderRequest.class);
		            		// verifico che l'utente sia loggato e tento la cancellazione, rispondo di conseguenza
		            		if(thisUser!=null&&removeOrder(cancelRequest.orderId,thisUser.getUsername()))
		            			res = new AccountResponse(100,"Ordine cancellato con successo");
		            		else
		            			res = new AccountResponse(101, "Si è verificato un errore nella cancellazione dell'ordine");
		            		
		            		// preparo la risposta e la invio
		            		response=gson.toJson(res);
		            		synchronized(out) {
		            			out.println(response);
		            		}
		            		break;
		            		
		            	// per la richiesta degli ordini
		            	case getOrders:
		            		String username = thisUser.getUsername();
		            		String userOrders = "";
		            		// itero su tutti gli ordrBook e li aggiungo se matchano il nome
		            		synchronized(crossOrderBook) {
		            			for (OrderBookElement orderPriceRange : limitOrderBook.bidOrderBook.orderBook) 
		            	    		for(Order order: orderPriceRange.orderList)
		            	    			if(order.size>0&&order.user.getUsername().equals(username)) 
		            	    				userOrders+=("\n"+order.toString());
		            	    	for (OrderBookElement orderPriceRange : limitOrderBook.askOrderBook.orderBook) 
		            	    		for(Order order: orderPriceRange.orderList)
		            	    			if(order.size>0&&order.user.getUsername().equals(username)) 
		            	    				userOrders+=("\n"+order.toString());
		            	    	for (OrderBookElement orderPriceRange : stopOrderBook.askOrderBook.orderBook) 
		            	    		for(Order order: orderPriceRange.orderList)
		            	    			if(order.size>0&&order.user.getUsername().equals(username)) 
		            	    				userOrders+=("\n"+order.toString());
		            	    	for (OrderBookElement orderPriceRange : stopOrderBook.bidOrderBook.orderBook) 
		            	    		for(Order order: orderPriceRange.orderList)
		            	    			if(order.size>0&&order.user.getUsername().equals(username)) 
		            	    				userOrders+=("\n"+order.toString());
		            	    	
		            	    	// preparo la risposta e la invio
		            	    	res = new getOrdersResponse(userOrders);
		            	    	response=gson.toJson(res);
		            	    	synchronized(out) {
		            	    		out.println(response);
		            	    	}
		            		}
		            		break;
		            	
		            	// per la richiesta dell'orderbook
		            	case getOrderBook:
		            		// prendo prima i 5 bidOrder migliori
		            		int count=0;
		            		ArrayList<ClientSideOrderBookElement> bidList= new ArrayList<>();
		            		Integer bidMax=null;
		            		synchronized(crossOrderBook) {
			            		Iterator<OrderBookElement> iterator = limitOrderBook.bidOrderBook.orderBook.iterator();
			            		
			            		while (iterator.hasNext() && count < 5) {
			            			OrderBookElement next= iterator.next();
			            			if(count==0)
			            				bidMax=next.price;
			            			bidList.add(new ClientSideOrderBookElement(next.price,next.size,next.price*next.size));
			                        count++;
			                    }
		            		}
		            		// prendo poi i 5 askOrder migliori
		            		ArrayList<ClientSideOrderBookElement> askList= new ArrayList<>();
		            		count=0;
		            		Integer askMin=null;
		            		synchronized(crossOrderBook) {
			            		NavigableSet<OrderBookElement> reverseiterator = limitOrderBook.askOrderBook.orderBook.descendingSet();
			            		for (OrderBookElement next : reverseiterator) {
			            			if(askMin==null||next.price<askMin)
			            				askMin=next.price;
			            			if(count==5)
			            				break;
			            			askList.add(new ClientSideOrderBookElement(next.price,next.size,next.price*next.size));
			                        count++;
			                    }
		            		}
		            		// preparo la stringa
		            		String orderBook="\n";
		            		
		            		for(ClientSideOrderBookElement elem:askList)
		            			orderBook+=elem;
		            		
		            		Integer spread;
		            		if(askMin==null||bidMax==null)
		            			spread=null;
		            		else
		            			spread=askMin-bidMax;
		            		
		            		orderBook+="ASK--------\n";
		            		orderBook+="\nSpread:"+spread+"      LatestPrice: "+limitOrderBook.currentMarketPrice+"\n\n";

		            		orderBook+="BID--------\n";
		            		
		            		for(ClientSideOrderBookElement elem:bidList)
		            			orderBook+=elem;
		            		
		            		// preparo la risposta e la invio
		            		res = new getOrderBookResponse(orderBook);
	            	    	response=gson.toJson(res);
	            	    	synchronized(out) {
	            	    		out.println(response);
	            	    	}
		            		
		            		break;
		            		
		            }
	            }
			} catch (SocketTimeoutException e) {
				// se ottengo questa eccezione, il timer di inattività e scaduto, posso quindi passare alla finally
				synchronized(System.out) {
					System.out.println("Timeout raggiunto durante la lettura dei dati dal server.");
				}
	        }catch (IOException e) {
	            e.printStackTrace();
	        }  catch(Exception e){
	        	e.printStackTrace();
	        } finally {
	        	// uscito da tutto, chiudo la clientSocket
	        	try {
	        		clientSocket.close();
	        	} catch(IOException e) {}
	        	synchronized(System.out) {
	        		System.out.println("Un client si è disconnesso");
	        	}
	        }
		}
	}
	
	// funzione per rimuovere un ordine dato un id
	static boolean removeOrder(long orderId, String username) {
    	LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
    	StopOrderBook stopOrderBook = CROSSorderBook.getStopOrderBook();
    	OrderBookElement containsToRemove = null;
    	int size=0;
    	// scorro tutti gli orderBook e se trovo un match me lo salvo
    	synchronized(crossOrderBook) {
	    	for (OrderBookElement orderPriceRange : limitOrderBook.bidOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			if(order.size>0&&order.orderId==orderId && order.user.getUsername()==username) {
	    				containsToRemove=orderPriceRange;
	    				size=order.size;
	    			}
	    	for (OrderBookElement orderPriceRange : limitOrderBook.askOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			if(order.size>0&&order.orderId==orderId && order.user.getUsername()==username) {
	    				containsToRemove=orderPriceRange;
	    				size=order.size;
	    			}
	    	for (OrderBookElement orderPriceRange : stopOrderBook.askOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
		    		if(order.size>0&&order.orderId==orderId && order.user.getUsername()==username) {
		    			containsToRemove=orderPriceRange;
		    			size=order.size;
		    		}
	    	for (OrderBookElement orderPriceRange : stopOrderBook.bidOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			if(order.size>0&&order.orderId==orderId && order.user.getUsername()==username) {
	    				containsToRemove=orderPriceRange;
						size=order.size;
					}
    	
	    	// rimuovo l'elemento dopo per la modifica concorrente all'iterator
	    	if(containsToRemove==null)
	    		return false;
	    	containsToRemove.orderList.removeIf(elem->elem.orderId==orderId);
	    	containsToRemove.size-=size;
	    	return true;
    	}
    }
	
	// la funzione salva la user map nel file JSON
	public static void saveUserMapToFile(ConcurrentHashMap<String, User> userMap, String filename) {
		try (JsonWriter writer = new JsonWriter(new FileWriter(filename))) {
			// inizio dell'oggetto JSON
			writer.beginObject();

			// itero su tutte le chiavi della userMap
            for (String key : userMap.keySet()) {
            	// scrivo la chiave
                writer.name(key);
                User user = userMap.get(key);
                // inizio l'oggetto per il singolo utente e aggiungo i vari attributi
                writer.beginObject();
                writer.name("username").value(user.getUsername());
                writer.name("password").value(user.getPassword());
                writer.name("isLoggedOn").value(false);
                writer.name("preferredPort").value(user.getPort());
                writer.name("latestAddress").value(user.getAddress());
                writer.endObject();
            }
            // finisco l'oggetto JSON principale
            writer.endObject();

            System.out.println("userMap salvata.");

        } catch (IOException e) {
        	synchronized(System.err) {
        		System.err.println("Errore durante il salvataggio della userMap: "+ e.getMessage());
        	}
        }
	}
	
	//la funzione carica la user map dal file
	public static void loadUserMapFromFile(ConcurrentHashMap<String, User> userMap, String filename){
		
		// verifico l'esistenza del file della userMap
		// se non esiste creo il file
		File file = new File(filename);

		if (!file.exists()) {
            try {
                file.createNewFile();
                synchronized(System.out) {
                	System.out.println("Il file della userMap non esisteva, ne creo uno nuovo...");
                }
                return;
            } catch (IOException e) {
            	synchronized(System.err) {
            		System.err.println("Errore nella creazione del file "+filename+": " + e.getMessage());
            	}
            }
        }
		
		// se il file è vuoto, esco senza fare niente
		if(file.length()==0) {
			synchronized(System.out) {
				System.out.println("Il file della userMap è vuoto, procedo ugualmente...");
			}
			return;
		}
		
		// passati tutti i casi precedenti, posso iniziare a leggere la userMap dal file e aggiungere gli elementi
		 try (JsonReader reader = new JsonReader(new FileReader(filename))) {
	            reader.beginObject();
	            
	            // finché ci sono nuovi elementi, li leggo
	            while (reader.hasNext()) {
	                String key = reader.nextName();
	                reader.beginObject();
	                
	                String username = null;
	                String password = null;
	                boolean isLoggedOn = false;
	                Integer preferredPort = -1;
	                InetAddress latestAddress= null;
	                
	                while (reader.hasNext()) {
	                    String name = reader.nextName();
	                    switch(name) {
	                    	case "username":
	                    		username = reader.nextString();
	                    		break;
	                    	case "password":
	                    		password = reader.nextString();
	                    		break;
	                    	case "isLoggedOn":
	                    		isLoggedOn = reader.nextBoolean();
	                    		break;
	                    	case "preferredPort":
	                    		preferredPort = reader.nextInt();
	                    		break;
	                    	case "latestAddress":
	                    		if (reader.peek() == JsonToken.NULL) {
	                                reader.nextNull(); 
	                            } else
	                            	latestAddress=InetAddress.getByName(reader.nextString());
	                    		break;
	                    	default:
	                            reader.skipValue();
	                            break;
	                    }
	                }

	                reader.endObject();
	                
	                // se ho ottenuto correttamente username e password, aggiungo alla userMap
	                if (username != null && password != null) {
	                	User user = new User(username,password,preferredPort);
	                	user.setLoggedOn(isLoggedOn);
	                	if(latestAddress==null)
	                		user.setAddress(null);
	                	else
	                		user.setAddress(latestAddress.getHostName());
	                    userMap.put(key, user);
	                }
	                
	            }
	            reader.endObject(); 
	            synchronized(System.out) {
	            	System.out.println("Il file della userMap è stato caricato correttamente...");
	            }
		 	} catch (IOException e) {
		 		synchronized(System.err) {
		 			System.err.println("Errore nella lettura della userMap: "+ e.getMessage());
		 		}
	        } catch (JsonParseException e) {
	        	synchronized(System.err) {
	        		System.err.println("Errore nel parsing del file JSON: " + e.getMessage());
	        	}
	        }
	}
	
	public static void loadOrderHistory(ConcurrentHashMap<String, List<HistoryOrder>> orderHistoryMap, String filename) {
		// verifico l'esistenza del file della userMap
		// se non esiste creo il file
		File file = new File(filename);
		
		if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println("Il file dello storicoOrdini non esisteva, ne creo uno nuovo...");
                return;
            } catch (IOException e) {
            	synchronized(System.err) {
            		System.err.println("Errore nella creazione del file '"+filename+": " + e.getMessage());
            	}
            }
        }
		
		if(file.length()==0) {
			synchronized(System.out) {
				System.out.println("Il file dello storicoOrdini è vuoto, procedo ugualmente...");
			}
			return;
		}
		
		// passo al codice che legge il file dello storico
		try (FileReader reader = new FileReader(filename)) {
			Gson gson = new Gson();
		    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

		    // estraggo l'array "trades" dal JSON
		    JsonArray tradesArray = jsonObject.getAsJsonArray("trades");

		    // itero per ogni elemento di questo array
		    for (JsonElement element : tradesArray) {
		        HistoryOrder order = gson.fromJson(element, HistoryOrder.class);
		        
		        if(order.getOrderId()>lastId)
		        	lastId=order.getOrderId();
		        
		        LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
		        limitOrderBook.currentMarketPrice=order.getPrice();
		        
		        // creo la chiave che utilizzerò per la mia struttura dati
		        LocalDate data = order.getDate();
		        String newKey = String.format("%02d%04d", data.getMonthValue(), data.getYear());
		        
		        List<HistoryOrder> orders = orderHistoryMap.get(newKey);

		        // se la lista esiste, aggiungo l'ordine, altrimenti creo una nuova lista conmtenente il nuovo ordine
		        if (orders != null) {
		        	orders.add(order);
		        } else {
		            // crea una nuova lista normale (ArrayList)
		        	orders = new ArrayList<>();
		        	orders.add(order);
		            orderHistoryMap.put(newKey, orders);
		        }
		    }

		    synchronized(System.out) {
		    	System.out.println("Il file dello storicoOrdini è stato caricato correttamente...");
		    }
		} catch (JsonSyntaxException e) {
			synchronized(System.err) {
				System.err.println("Errore nel parsing del JSON: " + e.getMessage());
			}
	    } catch (IOException e) {
	    	synchronized(System.err) {
	    		System.err.println("Errore di I/O durante la lettura del file '"+filename+"': " + e.getMessage());
	    	}
	    } 
	}
	
	public static void saveOrderHistoryToFile(ConcurrentHashMap<String, List<HistoryOrder>> orderHistoryMap, String filename) {
		
		JsonArray tradesArray = new JsonArray();
	    
	    // itero su ogni lista di ordini nella mappa e aggiungo ogni ordine al JsonArray
	    Gson gson = new Gson();
	    for (List<HistoryOrder> orders : orderHistoryMap.values()) {
	        for (HistoryOrder order : orders) {
	            tradesArray.add(gson.toJsonTree(order));
	        }
	    }

	    if (tradesArray.size() == 0) {
	    	synchronized(System.out) {
	    		System.out.println("Nessun ordine da salvare. Il file rimane storicoOrdini.json rimane invariato.");
	    	}
	        return;  
	    }

	    // creo il JsonObject e inserisco il JsonArray sotto la chiave "trades"
	    JsonObject result = new JsonObject();
	    result.add("trades", tradesArray);
		
        //Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(result, writer);
            synchronized(System.out) {
            	System.out.println("Il file dello storico degli ordini è stato salvato con successo.");
            }
        } catch (IOException e) {
        	synchronized(System.err) {
        		System.err.println("Errore durante la scrittura del file '"+filename+"': " + e.getMessage());
        	}
        }
	}
	
	public static void saveOrderBookToFile(String filename) {
		
		LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
    	StopOrderBook stopOrderBook = CROSSorderBook.getStopOrderBook();
		
		JsonArray pendingOrders = new JsonArray();
	    
	    // itero su ogni lista di ordini nella mappa e aggiungo ogni ordine al JsonArray
	    Gson gson = new Gson();
	    synchronized(crossOrderBook) {
		    for (OrderBookElement orderPriceRange : limitOrderBook.bidOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			pendingOrders.add(gson.toJsonTree(order));
		    
		    for (OrderBookElement orderPriceRange : limitOrderBook.askOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			pendingOrders.add(gson.toJsonTree(order));
		    
		    for (OrderBookElement orderPriceRange : stopOrderBook.bidOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			pendingOrders.add(gson.toJsonTree(order));
		    
		    for (OrderBookElement orderPriceRange : stopOrderBook.askOrderBook.orderBook) 
	    		for(Order order: orderPriceRange.orderList)
	    			pendingOrders.add(gson.toJsonTree(order));
		    
		    if (pendingOrders.size() == 0) {
		        return;  
		    }
	    }

	    // creo il JsonObject e inserisco il JsonArray sotto la chiave "trades"
	    JsonObject result = new JsonObject();
	    result.add("pendingOrders", pendingOrders);
		
        //Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(result, writer);
            synchronized(System.out) {
            	System.out.println("Il file dello storico degli ordini in attesa è stato salvato con successo.");
            }
        } catch (IOException e) {
        	synchronized(System.err) {
        		System.err.println("Errore durante la scrittura del file '"+filename+"': " + e.getMessage());
        	}
        }
	}
	
	// funzione per caricare l'orderbook
	public static void loadOrderBook(ConcurrentHashMap<String, User> userMap, String filename) {
		// dopo alcune prime verifiche
		LimitOrderBook limitOrderBook = CROSSorderBook.getLimitOrderBook();
    	StopOrderBook stopOrderBook = CROSSorderBook.getStopOrderBook();
    	
		File file = new File(filename);
		
		if (!file.exists()) {
            try {
                file.createNewFile();
                synchronized(System.out) {
                	System.out.println("Il file dei pendingOrders non esisteva, ne creo uno nuovo...");
                }
                return;
            } catch (IOException e) {
            	synchronized(System.err) {
            		System.err.println("Errore nella creazione del file '"+filename+"': " + e.getMessage());
            	}
            }
        }
		
		if(file.length()==0) {
			synchronized(System.out) {
				System.out.println("Il file dei pendingOrders è vuoto, procedo ugualmente...");
			}
			return;
		}
		// leggo il file, estraggo ogni oggetto e lo aggiungo al corretto orderBook
		try (FileReader reader = new FileReader(filename)) {
			Gson gson = new Gson();
		    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

		    JsonArray tradesArray = jsonObject.getAsJsonArray("pendingOrders");

		    for (JsonElement element : tradesArray) {
		    	JsonObject order = gson.fromJson(element, JsonObject.class);
		        if(order.get("orderType").getAsString().equals("limit")) {
		        	LimitOrder limitOrder = gson.fromJson(order, LimitOrder.class);
		        	
		        	limitOrder.user=userMap.get(limitOrder.user.getUsername());
		        	
		        	if(limitOrder.type==SaleType.ask)
		        		limitOrderBook.askOrderBook.addOrder(limitOrder);
		        	else
		        		limitOrderBook.bidOrderBook.addOrder(limitOrder);
		        	
		        	if(limitOrder.orderId>=lastId)
		        		lastId=limitOrder.orderId+1;
		        } else{
		        	StopOrder stopOrder = gson.fromJson(order, StopOrder.class);
		        	
		        	stopOrder.user=userMap.get(stopOrder.user.getUsername());
		        	
		        	if(stopOrder.type==SaleType.ask)
		        		stopOrderBook.askOrderBook.addOrder(stopOrder);
		        	else
		        		stopOrderBook.bidOrderBook.addOrder(stopOrder);
		        	
		        	if(stopOrder.orderId>=lastId)
		        		lastId=stopOrder.orderId+1;
		        }
		    }

            
		    synchronized(System.out) {
		    	System.out.println("Il file dei pendingOrders è stato caricato correttamente...");
		    }
		} catch (JsonSyntaxException e) {
			synchronized(System.err) {
				System.err.println("Errore nel parsing del JSON: " + e.getMessage());
			}
	    } catch (IOException e) {
	    	synchronized(System.err) {
	    		System.err.println("Errore di I/O durante la lettura del file '"+filename+"': " + e.getMessage());
	    	}
	    } 
	}
	
	// la funzione aggiunge un ordine allo storico a partire dai suoi dati
	public static void addToHistory(int orderId, SaleType type, OrderType orderType, int size, int price) {
		// salvo l'ordine completato nella orderHistoryMap
        HistoryOrder historyOrder =new HistoryOrder(orderId,String.valueOf(type),String.valueOf(orderType),size,price,System.currentTimeMillis()/1000);
        // creo la chiave che utilizzerò per la mia struttura dati
        LocalDate data = historyOrder.getDate();
        String newKey = String.format("%02d%04d", data.getMonthValue(), data.getYear());
        List<HistoryOrder> orders = orderHistory.get(newKey);
        
        // se la lista esiste, aggiungo l'ordine, altrimenti creo una nuova lista conmtenente il nuovo ordine
        if (orders != null) {
        	orders.add(historyOrder);
        } else {
            // crea una nuova lista normale (ArrayList)
        	orders = new ArrayList<>();
        	orders.add(historyOrder);
        	orderHistory.put(newKey, orders);
        }
	}
	
	// notifico l'utente per la completazione di un certo ordine
	public static void notifyUser(Order order) {
		try (DatagramSocket socket = new DatagramSocket()) {
			
			OrderNotification[] orders = {new OrderNotification(order.orderId,order.type,order.orderType,order.originalSize,order.price,order.timestamp)};
			
			CompletedTradesNotification notif = new CompletedTradesNotification(orders);
			Gson gson = new Gson();
			String notification = gson.toJson(notif);
            InetAddress address = InetAddress.getByName(order.user.getAddress());
            
            DatagramPacket packet = new DatagramPacket(notification.getBytes(), notification.length(), address, order.user.getPort());
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	//la funzione verifica la validità delle credenziali, controllando se l'utente è presente nella userMap e se la password combacia
	public static boolean areCredentialsValid(ConcurrentHashMap<String, User> userMap, String username, String password) {
		if (userMap.containsKey(username)) {
            User user = userMap.get(username);
            return user.getPassword().equals(password);
        }
        return false;
	}
	
	//la funzione verifica se la password contiene almeno un carattere maiuscolo, un numero e contiene almeno 8 caratteri.
	public static boolean isPasswordValid(String password) {
		
		if(password.length()<8)
			return false;
		
		boolean containsNumber = false;
        boolean containsUpperCase = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if (Character.isDigit(c))
                containsNumber = true;

            if (Character.isUpperCase(c)) 
                containsUpperCase = true;

            if (containsNumber && containsUpperCase) 
                return true;
        }

        return containsNumber && containsUpperCase;
	}
}
