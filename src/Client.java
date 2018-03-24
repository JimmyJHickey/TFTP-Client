// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-23

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;



public class Client 
{
	private InetAddress myAddress;
	private InetAddress serverAddress;
	private int myPort;
	private int serverPort;
	
	private DatagramSocket socket;
	
	
	public Client(String myAddress, String serverAddress)
	{
		Random ran = new Random();
		
		try 
		{
			// get a port number between 1024 and 65535
			myPort = ran.nextInt(Const.MAX_PORT - Const.RESERVED_PORTS) + Const.RESERVED_PORTS;
			this.myAddress = InetAddress.getLocalHost();
			
			
			serverPort = 69;
			this.serverAddress = InetAddress.getByName(serverAddress);
			
			socket = new DatagramSocket(this.myPort, this.myAddress);
		} 
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
	}
	
	public byte[] getMail()
	{
		byte[] data = new byte[512];
		
		DatagramPacket packet = new DatagramPacket(data, data.length);
		
		try 
		{
			socket.receive(packet);
		} 
		catch (IOException e) 
		{
			
			e.printStackTrace();
		}
		
		return packet.getData();
	}
	
	
	
	public void sendPacket(byte[] data)
	{
		DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
		
		try 
		{
			socket.send(packet);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args) 
	{
		

	}

}
