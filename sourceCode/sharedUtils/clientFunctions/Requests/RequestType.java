package clientFunctions.Requests;

// enum con i vari tipi di richiesta
public enum RequestType{
	register,
	login,
	updateCredentials,
	logout,
	getPriceHistory,
	insertMarketOrder,
	insertLimitOrder,
	insertStopOrder,
	cancelOrder,
	getOrders,
	getOrderBook
}