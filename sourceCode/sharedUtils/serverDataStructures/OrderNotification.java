package serverDataStructures;

// formato dell'ordine che sarà contenuto nella notifica
public class OrderNotification {
	Integer orderId;
	SaleType type;
	OrderType orderType;
	Integer size;
	Integer price;
	Long timestamp;
	
	public OrderNotification(Integer orderId,SaleType type,OrderType orderType,Integer size,Integer price,Long timestamp){
		this.orderId=orderId;
		this.type=type;
		this.orderType=orderType;
		this.size=size;
		this.price=price;
		this.timestamp=timestamp;
	}
	public String toString(){
        return "Order{orderID:" + orderId + ", type:"+type+ ", orderType:"+orderType+ ", size=" + size + ", price="+price+ ", timestamp=" + timestamp+"}";
	}
}
