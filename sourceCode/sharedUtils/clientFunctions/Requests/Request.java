package clientFunctions.Requests;

// request generica, contiene solo il tipo, sarà estesa dalle altre richieste specifiche
public class Request {
	RequestType requestType;
	
	public RequestType getRequestType(){
		return this.requestType;
	}
}