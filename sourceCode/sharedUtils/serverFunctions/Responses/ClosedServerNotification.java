package serverFunctions.Responses;

// notifica specifica per la chiusura del server
public class ClosedServerNotification extends Notification{
	public ClosedServerNotification() {
		super(NotificationType.closedServer);
	}
}
