package serverFunctions.Responses;

//risposta specifica per l'orderbook
public class getOrderBookResponse extends Response{
	public String orderBook;
	
	public getOrderBookResponse(String orderBook) {
		super(ResponseType.getOrderBook);
		this.orderBook=orderBook;
	}
}
