package serverDataStructures;

// ordine specifico degli stop
public class StopOrder extends Order{
	public StopOrder(User user, int price, int size, SaleType type, int orderId) {
		super (user, size, type, orderId);
		this.price=price;
		this.orderType=OrderType.stop;
	}
}
