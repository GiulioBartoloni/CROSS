package serverFunctions.Responses;

import java.util.Map;

import serverDataStructures.OHLC;

// risposta specifica per la price history
public class priceHistoryResponse extends Response{
	private Map<Integer, OHLC> monthlyOHLC;
    
    public priceHistoryResponse(Map<Integer, OHLC> results) {
    	super(ResponseType.priceHistory);
    	this.monthlyOHLC=results;
    }
    
    public Map<Integer, OHLC> getResults() {
    	return this.monthlyOHLC;
    }
}
