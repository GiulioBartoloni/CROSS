package serverFunctions.Responses;

import serverDataStructures.OrderNotification;

// notifica specifica per l'ordine completato
public class CompletedTradesNotification extends Notification{
	public OrderNotification[] trades;
	
	public CompletedTradesNotification(OrderNotification[] trades) {
		super(NotificationType.completedTrades);
		this.trades=trades;
	}
}
