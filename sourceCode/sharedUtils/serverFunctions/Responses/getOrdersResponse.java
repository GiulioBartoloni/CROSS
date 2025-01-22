package serverFunctions.Responses;

//risposta specifica per gli ordini
public class getOrdersResponse extends Response{
	public String list;
	
	public getOrdersResponse(String list) {
		super(ResponseType.getOrders);
		this.list=list;
	}
}
