package clientFunctions.Requests;

//richiesta specifica per la registrazione di un utente
public class registerRequest extends Request{
	private String username;
    private String password;
    
    public registerRequest(String username, String password) {
    	this.requestType = RequestType.register;
    	this.password = password;
    	this.username = username;
    }
    
    public String getUsername() {
    	return this.username;
    }
    
    public String getPassword() {
    	return this.password;
    }
}