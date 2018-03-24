
public class Driver {

	
	public static void main(String[] args) {
		Client myClient = new Client("127.0.0.1");
		
		myClient.readFile("jimmy", "netascii");

	}

}
