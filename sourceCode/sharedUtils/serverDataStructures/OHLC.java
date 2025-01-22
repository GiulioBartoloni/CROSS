package serverDataStructures;

// descrive l'open-high-low-close
public class OHLC {
	private int day;
	private long open;
	private long high;
	private long low;
	private long close;
	
	public OHLC(int day, long open, long high, long low, long close) {
		this.day=day;
		this.open=open;
		this.high=high;
		this.low=low;
		this.close=close;
	}
	
	public long getOpen() {
		return this.open;
	}
	
	public long getHigh() {
		return this.high;
	}
	
	public long getLow() {
		return this.low;
	}
	
	public long getClose() {
		return this.close;
	}
	
	public void setHigh(long newHigh) {
		this.high=newHigh;
	}
	
	public void setLow(long newLow) {
		this.low=newLow;
	}
		
	public void setClose(long newClose) {
		this.close=newClose;
	}
	
	public String toString() {
        return "Giorno " + day + ": \n" +
                "	open=" + open + "     high=" + high + "\n" +
                "	low=" + low + "     close=" + close;
    }

}
