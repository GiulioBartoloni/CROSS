package serverDataStructures;

// classe che definisce un ordine
public abstract class Order {
    public int orderId;
    public SaleType type;
    public OrderType orderType;
    public User user;
    public int price;
    public long timestamp;
    public int size;
    public int originalSize;

    public Order(User user, int size, SaleType type, int orderId) {
        this.orderId = orderId;
        this.user = user;
        this.size = size;
        this.originalSize=size;
        this.timestamp = System.currentTimeMillis() / 1000;
        this.type=type;
    }

    public String toString() {
        return "Order{orderID:" + orderId + ", type:"+type+ ", orderType:"+orderType+ ", user=" + user.getUsername() + ", timestamp=" + timestamp + ", originalSize=" + originalSize + ", currentSize="+size+ ", price="+price+"}";
    }
}
