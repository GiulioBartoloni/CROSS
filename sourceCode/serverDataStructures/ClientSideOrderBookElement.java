package serverDataStructures;

// classe per definire gli elementi per la composizione dell'orderbook
public class ClientSideOrderBookElement {
	public int price;
	public int size;
	public long total;
	
	public ClientSideOrderBookElement(int price,int size,long total) {
		this.price=price;
		this.size=size;
		this.total=total;
	}
	
	public String toString() {
		return "price:"+this.price+", size:"+this.size+", total:"+this.total+"\n";
	}
}
