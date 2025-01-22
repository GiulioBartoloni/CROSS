package clientFunctions.Requests;

//richiesta specifica per l'inserimento di un limit order
public class insertLimitOrderRequest extends Request{
	public String type;
	public int size;
	public int price;
	
	public insertLimitOrderRequest(String type, int size, int price) {
		this.requestType = RequestType.insertLimitOrder;
		this.type = type;
		this.size = size;
		this.price = price;
	}
}