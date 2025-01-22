package clientFunctions.Requests;

// richiesta specifica per l'update delle credenziali
public class updateCredentialsRequest extends Request{
	private String username;
    private String oldPassword; 
    private String newPassword;
    
    public updateCredentialsRequest(String username, String oldPassword, String newPassword) {
    	this.requestType = RequestType.updateCredentials;
    	this.username = username;
    	this.oldPassword = oldPassword;
    	this.newPassword = newPassword;
    }
    
    public String getUsername(){
    	return this.username;
    }
    
    public String getOldPassword(){
    	return this.oldPassword;
    }
    
    public String getNewPassword(){
    	return this.newPassword;
    }
}