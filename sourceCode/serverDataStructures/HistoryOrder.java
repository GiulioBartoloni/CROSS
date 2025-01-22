package serverDataStructures;

import java.time.*;

// classe che definisce gli ordini facenti parte dello storico
// seguono un formato diverso per rappresentarne solo i dati necessari
public class HistoryOrder {
	private int orderId;
	private SaleType type;
	private OrderType orderType;
	private int size;
	private int price;
	private long timestamp;
	
	public HistoryOrder(int orderId, String type, String orderType, int size, int price, long timestamp){
		this.orderId=orderId;
		this.type= SaleType.valueOf(type);
		this.orderType= OrderType.valueOf(orderType);
		this.size=size;
		this.price=price;
		this.timestamp=timestamp;
	}
	
	public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public SaleType getType() {
        return type;
    }

    public void setType(SaleType type) {
        this.type = type;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Integer getPrice() {
        return price;
    }
    
    public long getUSDPrice() {
    	return price/1000;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public LocalDate getDate() {
    	Instant instant = Instant.ofEpochSecond(this.timestamp);
    	return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
