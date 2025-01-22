package serverFunctions.Responses;

//risposta specifica per le richieste riguardanti ordini
public class OrderResponse extends Response{
	
	public long orderId;
	
	public OrderResponse(long orderId) {
		super(ResponseType.orderResponse);
		this.orderId=orderId;
	}
}
