package clientFunctions.Requests;

//richiesta specifica per la cancellazione di un ordine
public class cancelOrderRequest extends Request{
	public long orderId;
	
	public cancelOrderRequest(long orderId) {
		this.requestType = RequestType.cancelOrder;
		this.orderId = orderId;
	}
}