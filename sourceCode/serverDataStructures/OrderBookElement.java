package serverDataStructures;

import java.util.Comparator;
import java.util.TreeSet;

// elemento dell'orderbook, contiene una lista dei vari ordini e il prezzo e dimensione aggregato 
public class OrderBookElement {
    public int price;
    public int size;
    public TreeSet<Order> orderList;
    
    // nella definizione, modifico il compare per avere l'ordine corretto dei dati
    public OrderBookElement(int price, int size) {
        this.price = price;
        this.size = size;
        this.orderList = new TreeSet<>(new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                int timestampComparison = Long.compare(o1.timestamp, o2.timestamp);
                if (timestampComparison != 0) {
                    return timestampComparison;
                }
                return Long.compare(o1.orderId, o2.orderId);
            }
        });
    }

    public long getPrice() {
        return this.price;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int newSize) {
        this.size = newSize;
    }

    @Override
    public String toString() {
        return "OrderBookElement{price=" + price + ", size= " + size + ", lista ordini=" + orderList + "}";
    }
}
