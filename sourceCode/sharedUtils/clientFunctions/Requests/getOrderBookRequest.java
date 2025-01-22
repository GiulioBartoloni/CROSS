package clientFunctions.Requests;

//richiesta specifica per l'orderbook
public class getOrderBookRequest extends Request{
	public getOrderBookRequest() {
		this.requestType = RequestType.getOrderBook;
	}
}