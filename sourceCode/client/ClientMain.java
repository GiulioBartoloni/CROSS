package client;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import com.google.gson.Gson;
import clientFunctions.Requests.*;
import serverDataStructures.*;
import serverFunctions.Responses.*;
import java.time.LocalDate;

public class ClientMain {
	public static void main(String args[]) {
		synchronized(System.out) {
			System.out.println("Benvenuto in CROSS!");
			System.out.println("Crea un nuovo account oppure effettua il login per iniziare.\n");
		}
		
		//Leggo le proprietà del client dal file di configurazione
		File configFile = new File("clientConfig.properties");
		String serverHost = null;
		int serverPort = 0;
		int notificationPort = 0;
		
		try (FileReader reader = new FileReader(configFile)){
			Properties props = new Properties();
			props.load(reader);
	        serverHost = props.getProperty("server.host");
	        serverPort = Integer.parseInt(props.getProperty("server.port"));
	        notificationPort = Integer.parseInt(props.getProperty("notification.port"));
		} catch (FileNotFoundException ex) {
			synchronized(System.err) {
				System.err.println("ERRORE! Il file di configurazione non esiste.");
			}
			System.exit(1);
		} catch (IOException e) {
			synchronized(System.err) {
				System.err.println("ERRORE! Impossibile leggere il file di configurazione: " + e.getMessage());
			}
			System.exit(1);
        } catch (Exception e){
        	synchronized(System.err) {
        		System.err.println("ERRORE! "+e.getMessage());
        	}
        	System.exit(1);
        }
		
		// avvio il thread delle notifiche
		Thread listenerThread = new Thread(new notificationListener(notificationPort));
        listenerThread.start();
        
		// rovo a connettermi al server, aprire lo scanner, reader e writer e se tutto ha successo posso iniziare a comunicare
        try (
        		Socket socket = new Socket(InetAddress.getByName(serverHost), serverPort);
        		Scanner scanner = new Scanner(System.in);
        		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")),true)
        ){
            Boolean amILoggedIn = false;
            
            // invio al server la porta preferita per le comunicazioni udp
            Gson gson = new Gson();
            out.println(gson.toJson(notificationPort));
            
            while(true) {
            	// leggo il comando successivo dell'utente
            	String input = null;
            	
            	//do {
        		synchronized(System.out) {
        			System.out.print("[CROSS]--> ");
        		}
        		synchronized(System.in) {
        			input = scanner.nextLine().trim();
        		}
            	String[] command = input.split(" ");
            	
            	// in base al comando richiesto, mi comporto di conseguenza ed inoltro richieste al server se necessario
            	switch(command[0]) {
            	
            		case "register": 
            			// se il formato della richiesta è corretta la eseguo, altrimenti indico l'utilizzo corretto
            			if (command.length == 3) {
            				// invio la richiesta
                            registerRequest req = new registerRequest(command[1],command[2]);
                            synchronized(out) {
                            	send(req, out);
                            }
                            
                            synchronized(in) {
	                            // attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                            String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            // leggo la risposta e mostro il messaggio fornito dal server
	                            AccountResponse response = (AccountResponse)recv(res,ResponseType.accountManagement);
	                            synchronized(System.out) {
	                            	System.out.println(response.getErrorMessage());
	                            }
                            }
                		} else {
                			synchronized(System.out) {
                				System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: register <username> <password>");
                			}
                		}
            			break;
            			
            		case "login":
            			// se il formato della richiesta è corretta la eseguo, altrimenti indico l'utilizzo corretto
            			if(command.length == 3) {
            				// invio la richiesta
                            loginRequest req = new loginRequest(command[1],command[2]);
                            synchronized(out) {
                            	send(req,out);
                            }
                            
                            synchronized(in) {
	                            // attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                            String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	
	                            // leggo la risposta e mostro il messaggio fornito dal server
	                            
	                            AccountResponse response = (AccountResponse)recv(res,ResponseType.accountManagement);
	                            synchronized(System.out) {
	                            	System.out.println(response.getErrorMessage());
	                            }
	                        	
	                        	// se il login è stato effettuato con successo, setto lo status locale di login dell'utente a vero  
	                            if(response.getResponse() == 100)
	                            	amILoggedIn=true;
                            }
            			}else {
            				synchronized(System.out) {
            					System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: login <username> <password>");
            				}
            			}
            			break;
            			
            		case "updateCredentials":
            			// se il formato della richiesta è corretta la eseguo, altrimenti indico l'utilizzo corretto
            			if (command.length == 4) {
            				// invio la richiesta
                            updateCredentialsRequest req = new updateCredentialsRequest(command[1],command[2],command[3]);
                            synchronized(out) {
                            	send(req, out);
                            }
                            
                            synchronized(in) {
	                            // attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                            String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	
	                            // leggo la risposta e mostro il messaggio fornito dal server
	                            AccountResponse response = (AccountResponse)recv(res,ResponseType.accountManagement);
	                            synchronized(System.out) {
	                            	System.out.println(response.getErrorMessage());
	                            }
                            }
                		}else {
                			synchronized(System.out) {
                				System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: updateCredentials <username> <oldPassword> <newPassword>");
                			}
                			
                		}
            			break;
            			
            		case "logout":
            			// se il formato della richiesta è corretta la eseguo, altrimenti indico l'utilizzo corretto
            			if (command.length == 1) {
                            logoutRequest req = new logoutRequest();
                            synchronized(out) {
                            	send(req, out);
                            }
                            
                            synchronized(in) {
	                            // attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                            String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            // leggo la risposta e mostro il messaggio fornito dal server
	                            AccountResponse response = (AccountResponse)recv(res,ResponseType.accountManagement);
	                            synchronized(System.out) {
	                            	System.out.println(response.getErrorMessage());
	                            }
	                        	
	                        	// se il logout ha avuto successo, setto lo status locale di login dell'utente a falso  
	                        	if(response.getResponse() == 100)
	                        		amILoggedIn=false;
                            }
                		} else {
                			synchronized(System.out) {
                				System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: logout");
                			}
                		}
            			break;
            			
            		case "exit":
            			// se l'utente è loggato, invia rapidamente una richiesta di logout
            			if(amILoggedIn) {
            				logoutRequest req = new logoutRequest();
            				synchronized(out) {
            					send(req, out);
            				}
                            
            				synchronized(in) {
	                            String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	
	                            // leggo la risposta e mostro il messaggio fornito dal server
	                            AccountResponse response = (AccountResponse)recv(res,ResponseType.accountManagement);
	                            synchronized(System.out) {
	                            	System.out.println(response.getErrorMessage());
	                            }
            				}
            			}
            				synchronized(System.out) {
            					System.out.println("Arrivederci!");
            				}
	            			System.exit(0);
	            		break;
            			
            		case "help":
            			// scrivo tutti i comandi disponibili all'utente
            			synchronized(System.out) {
	            			System.out.println("[CROSS]--> Comandi disponibili:");
	            	        System.out.println("	register <username> <password>");
	            	        System.out.println("	Registra un nuovo utente con il nome utente e la password.\n");
	            	        
	            	        System.out.println("	login <username> <password>");
	            	        System.out.println("	Esegui il login con il nome utente e la password.\n");
	            	        
	            	        System.out.println("	updateCredentials <username> <oldPassword> <newPassword>");
	            	        System.out.println("	Aggiorna le credenziali dell'utente.\n");
	            	        
	            	        System.out.println("	logout");
	            	        System.out.println("	segui il logout.\n");
	            	        
	            	        System.out.println("	help");
	            	        System.out.println("	Mostra questa lista di comandi disponibili.\n");
	            	        
	            	        System.out.println("	getPriceHistory <MMYYYY>");
	            	        System.out.println("	Mostra informazioni sugli ordini nel mese specificato.\n");
	            	        
	            	        System.out.println("	insertLimitOrder <type> <size> <price>");
	            	        System.out.println("	Inserisce un limit order.\n");
	            	        
	            	        System.out.println("	insertMarketOrder <type> <size>");
	            	        System.out.println("	Inserisce un market order.\n");
	            	        
	            	        System.out.println("	insertStopOrder <type> <size> <price>");
	            	        System.out.println("	Inserisce uno stop order.\n");
	            	        
	            	        System.out.println("	getOrders");
	            	        System.out.println("	Mostra tutti gli ordini non ancora evasi a proprio nome.\n");
	            	        
	            	        System.out.println("	getOrderBook");
	            	        System.out.println("	Visualizza l'orderBook, compreso di spread e latestPrice.\n");
	            	        
	            	        System.out.println("	exit");
	            	        System.out.println("	Termina l'esecuzione del client.");
            			}
            	        break;
            	    
            		case "getPriceHistory":
            			// richiedo la price history per un mese specificato
            			if (command.length == 2) {
            				//verifico che la data sia valida
            				if(isDateValid(command[1])) {
            					// creo ed invio la richiesta
            					getPriceHistoryRequest req = new getPriceHistoryRequest(command[1]);        					
            					synchronized(out) {
            						send(req,out);
            					}
            					
            					synchronized(in) {
	            					// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                                // altrimenti leggo la risposta e la scrivo in out
	            					String res = in.readLine();
	                                if(res == null) {
	                                	synchronized(System.out) {
	                                		System.out.println("La connessione è stata chiusa dal lato del server.");
	                                	}
	                                	System.exit(1);
	                                }
	                                // leggo la risposta e scrivo tutto
	                                priceHistoryResponse response = (priceHistoryResponse)recv(res,ResponseType.priceHistory);
	                                Map<Integer,OHLC> monthlyOHLC = response.getResults(); 
	                                if(monthlyOHLC != null)
	                                	synchronized(System.out) {
	                                		monthlyOHLC.forEach((giorno, ohlc) -> System.out.println(ohlc));
	                                	}
	                                else
	                                	synchronized(System.out) {
	                                		System.out.println("Non è disponibile alcuno storico per il mese selezionato.");
	                                	}
            					}
                                }else{
                                	synchronized(System.out) {
                                		System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: getPriceHistory <MMYYYY>");
                                	}	
                                }
            			} else {
            				synchronized(System.out) {
                			System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: getPriceHistory <MMYYYY>");
            				}
            			}
            			break;
            		case "insertLimitOrder":
            			// inserisco un nuovo limitorder
            			if (command.length == 4 && command[2].matches("-?\\d+") && command[3].matches("-?\\d+") && (command[1].equals("ask")||command[1].equals("bid"))) {
            				if(!amILoggedIn) {
            					synchronized(System.out) {
            						System.out.println("[CROSS]--> ERRORE! Devi essere loggato per utilizzare questo comando!");
            					}
            					break;
            				}
            				// creo la richiesta
            				insertLimitOrderRequest req = new insertLimitOrderRequest(command[1],Integer.parseInt(command[2]),Integer.parseInt(command[3]));
            				synchronized(out) {
            					send(req, out);
            				}
                			
            				synchronized(in) {
	                			// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                			String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            // creo la risposta e mostro l'id all'utente
	                			OrderResponse response = (OrderResponse)recv(res, ResponseType.orderResponse);
	                			synchronized(System.out) {
	                				System.out.println("Ordine creato con successo con orderId: "+response.orderId);
	                			}
            				}
                		} else {
                			synchronized(System.out) {
                				System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: insertLimitOrder <type> <size> <price>");
                			}
                		}
                			
            			break;
            			
            		case "insertMarketOrder":
            			// inserisco un nuovo market order
            			if (command.length == 3 && command[2].matches("-?\\d+") && (command[1].equals("ask")||command[1].equals("bid"))) {
            				if(!amILoggedIn) {
            					synchronized(System.out) {
            						System.out.println("[CROSS]--> ERRORE! Devi essere loggato per utilizzare questo comando!");
            					}
            					break;
            				}
            				
            				//creo la richiesta e la invio
            				insertMarketOrderRequest req = new insertMarketOrderRequest(command[1],Integer.parseInt(command[2]));
            				synchronized(out) {
            					send(req, out);
            				}
                			
            				synchronized(in) {
	                			// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                			String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            //leggo la risposta ed in base al codice avverto l'utente
	                			OrderResponse response = (OrderResponse)recv(res, ResponseType.orderResponse);
	                			if(response.orderId == -1)
	                				synchronized(System.out) {
	                					System.out.println("Non e' stato possibile eseguire l'ordine a causa di una mancanza di disponibilita'");
	                				}
	                    		else
	                    			synchronized(System.out) {
	                    				System.out.println("Ordine creato con successo con orderId: "+response.orderId);
	                    			}
            				}
            			} else {
            				synchronized(System.out) {
            					System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: insertMarketOrder <type> <size>");
            				}
            			}
            			break;
            			
            		case "insertStopOrder":
            			// inserisco un nuovo stop order
            			if (command.length == 4 && command[2].matches("-?\\d+") && command[3].matches("-?\\d+") && (command[1].equals("ask")||command[1].equals("bid"))) {
            				if(!amILoggedIn) {
            					synchronized(System.out) {
            						System.out.println("[CROSS]--> ERRORE! Devi essere loggato per utilizzare questo comando!");
            					}
            					break;
            				}
            					
            				// credo la richiesta e la invio
            				insertStopOrderRequest req = new insertStopOrderRequest(command[1],Integer.parseInt(command[2]),Integer.parseInt(command[3]));
            				synchronized(out) {
            					send(req, out);
            				}
                			
            				synchronized(in) {
	                			// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                			String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            // leggo la risposta e mostro l'id
	                			OrderResponse response = (OrderResponse)recv(res, ResponseType.orderResponse);
	                			synchronized(System.out) {
	                				System.out.println("Ordine creato con successo con orderId: "+response.orderId);
	                			}
            				}
            			} else {
            				synchronized(System.out) {
            					System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: insertStopOrder <type> <size> <price>");
            				}
            			}
            			break;
            			
            		case "cancelOrder":
            			// per cancellare un'ordine con id specifico
            			if (command.length == 2 && command[1].matches("-?\\d+")) {
            				if(!amILoggedIn) {
            					synchronized(System.out) {
            						System.out.println("[CROSS]--> ERRORE! Devi essere loggato per utilizzare questo comando!");
            					}
            					break;
            				}
            				
            				// creo la richiesta e la invio
            				cancelOrderRequest req = new cancelOrderRequest(Long.parseLong(command[1]));
            				synchronized(out) {
            					send(req, out);
            				}
                			
            				synchronized(in) {
	                			// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                			String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            // leggo la risposta e avviso l'utente
	                			AccountResponse response = (AccountResponse)recv(res, ResponseType.accountManagement);
	                			synchronized(System.out) {
	                				System.out.println(response.getErrorMessage());
	                			}
            				}
            			} else {
            				synchronized(System.out) {
            					System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: insertStopOrder <type> <size> <price>");
            				}
            			}
            			break;
            		
            		case "getOrders":
            			// per ottenere tutti gli ordini (parziali e non) e mostrarli all'utente
            			if (command.length == 1) {
            				if(!amILoggedIn) {
            					synchronized(System.out) {
            						System.out.println("[CROSS]--> ERRORE! Devi essere loggato per utilizzare questo comando!");
            					}
            					break;
            				}
            				
            				// creo la richiesta 
            				getOrdersRequest req = new getOrdersRequest();
            				synchronized(out) {
            					send(req, out);
            				}
                			
            				synchronized(in) {
	                			// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                			String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            // leggo la risposta e la mostro all'utente
	                			getOrdersResponse response = (getOrdersResponse)recv(res, ResponseType.getOrders);
	                			if(response.list.length() == 0)
	                				synchronized(System.out) {
	                					System.out.println("Non sono disponibili ordini a tuo nome.");
	                				}
	                			else
	                				synchronized(System.out) {
	                					System.out.println(response.list);
	                				}
            				}
            			} else {
            				synchronized(System.out) {
            					System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: getOrders");
            				}
            			}
            			break;
            			
            		case "getOrderBook":
            			// visualizza l'orderbook con informazioni utili
            			if (command.length == 1) {
            				// creo la richiesta
            				getOrderBookRequest req = new getOrderBookRequest();
            				synchronized(out) {
            					send(req, out);
            				}
                			
            				synchronized(in) {
	                			// attendo la richiesta, se a null la lettura è fallita causa chiusura della socket dal lato server
	                            // altrimenti leggo la risposta e la scrivo in out
	                			String res = in.readLine();
	                            if(res == null) {
	                            	synchronized(System.out) {
	                            		System.out.println("La connessione è stata chiusa dal lato del server.");
	                            	}
	                            	System.exit(1);
	                            }
	                            
	                            // leggo la risposta e mostro i risultati all'utente
	                			getOrderBookResponse response = (getOrderBookResponse)recv(res, ResponseType.getOrderBook);
	                			synchronized(System.out) {
	                				System.out.println(response.orderBook);
	                			}
            				}
            			} else {
            				synchronized(System.out) {
            					System.out.println("[CROSS]--> ERRORE! Utilizzo del comando: getOrderBook");
            				}
            			}
            			break;
            			
            		case "":
            			break;
            			
            		default:
            			// se il comando non è stato riconosciuto, invito l'utente a utilizzare help per visualizzare tutti i comandi disponibili
            			synchronized(System.out) {
            				System.out.println("[CROSS]--> Comando non riconosciuto, Utilizza `help` per visualizzare tutti i comandi disponibili.");
            			}
            			break;
            	}
            }
        } catch (IOException e) {
        	synchronized(System.err) {
        		System.err.println("Impossibile connettersi al server: " + e.getMessage());
        	}
        } 
		
    
	}
	
	// la funzione prende un oggetto di tipo request, lo trasforma in formato JSON e lo invia al server
	public static void send(Request req, PrintWriter out) throws IOException {
	        Gson gson = new Gson();
	        String request = gson.toJson(req);
	        out.println(request);
	}
	
	// la funzione prende una stringa e restituisce un oggetto del tipo risposta generica
	public static Response recv(String res, ResponseType responseType){
		Gson gson = new Gson();
    	Response response = null;
    	switch(responseType) {
    		case accountManagement:
        		response = gson.fromJson(res, AccountResponse.class);
        		break;
    		case priceHistory:
    			response = gson.fromJson(res, priceHistoryResponse.class);
    			break;
    		case orderResponse:
    			response = gson.fromJson(res, OrderResponse.class);
    			break;
    		case getOrders:
    			response = gson.fromJson(res,getOrdersResponse.class);
    			break;
    		case getOrderBook:
    			response = gson.fromJson(res,getOrderBookResponse.class);
    		default:
    			break;
    	}
    	if(responseType == ResponseType.accountManagement)
    		response = gson.fromJson(res, AccountResponse.class);
    	
    	return response;
	}
	
	// la funzione verifica se la data inserita è nel formato valido
	public static boolean isDateValid(String date) {
		if (date.length() != 6) 
            return false;
        
		String monthStr = date.substring(0, 2);
        String yearStr = date.substring(2);
        
        if (!monthStr.matches("\\d\\d") || !yearStr.matches("\\d\\d\\d\\d")) 
            return false;
        
        int month = Integer.parseInt(monthStr);
        int year = Integer.parseInt(yearStr);
        
        if (month < 1 || month > 12) 
            return false;
        
        LocalDate currentDate = LocalDate.now();
        int currentMonth = currentDate.getMonthValue(); 
        int currentYear = currentDate.getYear(); 

        if (year > currentYear || (year == currentYear && month > currentMonth)) 
            return false; 
        
        return true;
	}
	
	static class notificationListener implements Runnable {

	    private int port;

	    public notificationListener(int port) {
	        this.port = port;
	    }

	    public void run() {
	        try (DatagramSocket socket = new DatagramSocket(port)) {

	            while (true) {
	                byte[] buffer = new byte[1024];
	                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	                socket.receive(packet);
	                
	                String message = new String(packet.getData(), 0, packet.getLength());
	                
	                Gson gson = new Gson();
	                Notification notif = gson.fromJson(message, Notification.class);
	                if(notif.notificationType == NotificationType.completedTrades) {
	                	CompletedTradesNotification completedTradesNotif = gson.fromJson(message, CompletedTradesNotification.class);
		                
		                try{
		                	Thread.sleep(500);
		                } catch(InterruptedException e) {}
			            	for(OrderNotification order:completedTradesNotif.trades)
			            		synchronized(System.out) {
			            			System.out.println("Un tuo ordine e' stato appena completato: "+order);
			            			System.out.print("[CROSS]--> ");
			            		}
	                }else {
	                	synchronized(System.out) {
	                		System.out.println("\nLa connessione è stata chiusa dal lato del server.");
	                	}
                    	System.exit(1);
	                }
	                	
	                
	            }
	        } catch (Exception e) {
	        	 System.err.println("Si è verificato un errore: " + e.getMessage());
	        }
	    }
	}
}
