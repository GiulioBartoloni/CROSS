package serverDataStructures;

//ordine specifico dei limit
public class LimitOrder extends Order{
	public LimitOrder( User user, int price, int size, SaleType type, int orderId) {
		super( user, size, type,orderId);
		this.price=price;
		this.orderType=OrderType.limit;
	}
}
