package clientFunctions.Requests;

//richiesta specifica per il login di un utente
public class loginRequest extends Request{
	private String username;
    private String password;
    
    public loginRequest(String username, String password) {
    	this.requestType = RequestType.login;
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