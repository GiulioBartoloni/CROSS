package serverDataStructures;

// orderbook generico, con un lato ask e uno bid
public class OrderBook {
    public AskOrderBook askOrderBook;
    public BidOrderBook bidOrderBook;

    public OrderBook() {
        this.askOrderBook = new AskOrderBook();
        this.bidOrderBook = new BidOrderBook();
    }
}
