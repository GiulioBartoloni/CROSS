package serverFunctions.Responses;

// risposta generica
public class Response {
	private ResponseType responseType;
	
	public Response(ResponseType responseType) {
		this.responseType = responseType;
	}
	
	public ResponseType getResponseType(){
		return this.responseType;
	}
}
