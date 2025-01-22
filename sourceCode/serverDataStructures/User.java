package serverDataStructures;

// classe per rappresentare gli utenti della piattaforma, contiene anche semplici setter, getter e toString per rappresentazioni
public class User {

    private String username;
    private String password;
    private boolean isLoggedOn;
    private int preferredPort;
    private String latestAddress;

   public User(String username, String password, Integer port) {
        this.username = username;
        this.password = password;
        this.isLoggedOn = false;
        this.preferredPort=port;
        this.latestAddress=null;
    }

    public String getAddress() {
    	return this.latestAddress;
    }
    
    public void setAddress(String address) {
    	this.latestAddress=address;
    }
   
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoggedOn() {
        return isLoggedOn;
    }

    public void setLoggedOn(boolean loggedOn) {
        isLoggedOn = loggedOn;
    }

    public void setPort(int port) {
    	this.preferredPort=port;
    }
    
    public int getPort() {
    	return this.preferredPort;
    }
    
    public String toString() {
        return "Utente{username='" + username + "', isLoggedOn=" + isLoggedOn + "}";
    }
}
