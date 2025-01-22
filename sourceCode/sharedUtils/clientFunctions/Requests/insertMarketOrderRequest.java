package clientFunctions.Requests;

//richiesta specifica per l'inserimento di un market order
public class insertMarketOrderRequest extends Request{
	public String type;
	public int size;
	
	public insertMarketOrderRequest(String type, int size) {
		this.requestType = RequestType.insertMarketOrder;
		this.type = type;
		this.size = size;
	}
}