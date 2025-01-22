package clientFunctions.Requests;

//richiesta specifica per gli ordini dell'utente
public class getOrdersRequest extends Request{
	public getOrdersRequest(){
		this.requestType = RequestType.getOrders;
	}
}