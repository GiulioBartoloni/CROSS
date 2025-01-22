package clientFunctions.Requests;

//richiesta specifica per il logout di un utente
public class logoutRequest extends Request {
	public logoutRequest() {
		this.requestType = RequestType.logout;
	}
}