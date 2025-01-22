package clientFunctions.Requests;

// richiesta specifica per l'inserimento di uno stop order
public class insertStopOrderRequest extends Request{
	public String type;
	public int size;
	public int price;
	
	public insertStopOrderRequest( String type, int size, int price) {
		this.requestType = RequestType.insertStopOrder;
		this.type = type;
		this.size = size;
		this.price = price;
	}
}