package serverDataStructures;

import java.util.Comparator;
import java.util.TreeSet;

public class AskOrderBook {
    // TreeSet che mantiene gli ordini in ordine crescente per price
    public TreeSet<OrderBookElement> orderBook;

    // costruttore
    public AskOrderBook() {
        this.orderBook = new TreeSet<>(new Comparator<OrderBookElement>() {
            @Override
            public int compare(OrderBookElement o1, OrderBookElement o2) {
                return Long.compare(o1.getPrice(), o2.getPrice());
            }
        });
    }
    // la funzione aggiunge un ordine, aggiungendolo al pricerange corretto, aggiornandone la size
    public void addOrder(Order order) {
        OrderBookElement orderBookElement = getOrderBookElement(order.price);
        orderBookElement.orderList.add(order);
        orderBookElement.setSize(orderBookElement.getSize()+order.size);
    }
    
    //la funzione restituisce l'orderbook element con il prezzo specificato se esiste, altrimenti lo crea
    private OrderBookElement getOrderBookElement(int price) {
        for (OrderBookElement order : orderBook)
            if (order.getPrice() == price)
                return order;
        OrderBookElement newOrderBookElement = new OrderBookElement(price, 0);
        this.orderBook.add(newOrderBookElement);
        return newOrderBookElement;
    }

    public void printOrders() {
        for (OrderBookElement order : orderBook) {
        	synchronized(System.out) {
        		System.out.println(order);
        	}
        }
    }
}
