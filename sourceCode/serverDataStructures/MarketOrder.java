package serverDataStructures;

//ordine specifico dei market
public class MarketOrder extends Order{
	public MarketOrder( User user, int size, SaleType type, int orderId) {
		super(user ,size, type,orderId);
		this.orderType=OrderType.market;
	}
}
