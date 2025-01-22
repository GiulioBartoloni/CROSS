package serverFunctions.Responses;

// risposta generica per operazioni legate agli account
public class AccountResponse extends Response{
	private Integer response;
    private String errorMessage;
    
    public AccountResponse(Integer response, String errorMessage) {
    	super(ResponseType.accountManagement);
    	this.response=response;
    	this.errorMessage=errorMessage;
    }
    
    public void setResponse(Integer response) {
    	this.response=response;
    }
    
    public Integer getResponse() {
    	return this.response;
    }
    
    public String getErrorMessage() {
    	return this.errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
    	this.errorMessage=errorMessage;
    }
}
