package clientFunctions.Requests;

//richiesta specifica per la pricehistory
public class getPriceHistoryRequest extends Request{
	private String month;
	
	public getPriceHistoryRequest(String month) {
		this.requestType = RequestType.getPriceHistory;
		this.month = month;
	}
	
	public String getMonth() {
		return this.month;
	}
}