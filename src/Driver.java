// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-24

public class Driver {

	
	public static void main(String[] args) {
		Client myClient = new Client("127.0.0.3");
		// Ben is wrong
//		myClient.getFile("cool.jpeg", "octet");
		myClient.sendFile("lorem_ipsum", "netascii");

	}

}
