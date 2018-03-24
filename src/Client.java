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
	
	
	public Client(String serverAddress)
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
		byte[] data = new byte[Const.PACKET_SIZE];
		
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
	
	public void readFile(String filepath, String mode)
	{
		byte request[] = createReadWriteRequest(Const.RRQ, filepath, mode);
		
		
		for(int i = 0; i < request.length; ++i ) 
		{
			System.out.printf("# %d : %c\n", request[i], (char)request[i]);
		}
		System.out.printf("\n");

		sendPacket(request);
		
		byte reply[] = getMail();
		
		for(int i = 0; i < reply.length; ++i)
		{
			System.out.printf("# %d : %c\n", reply[i], (char)reply[i]);
		}
			
		
		//byte [] readRQ = {Const.TERM, Const.RRQ, Const.TERM, mode.getBytes(), Const.TERM}; 
		//sendPacket();
	}
	
	private byte[] createReadWriteRequest(byte opcode, String filepath, String mode)
	{
		mode = mode.toLowerCase();
		
		if(mode != Const.NETASCII && mode != Const.OCTET)
		{
			System.err.printf("Bad mode: %s does not exist. Try \"netascii\" or \"octet\".\n", mode);
			return null;
		}
		
		if(opcode != Const.RRQ && opcode != Const.WRQ)
		{
			System.err.printf("Bad opcode: %d invalid. Try \"RRQ\" or \"WRQ\".\n", opcode);
			return null;
		}
		

		//        2 bytes     string    1 byte     string   1 byte
		//        ------------------------------------------------
		//       | Opcode |  Filename  |   0  |    Mode    |   0  |
		//        ------------------------------------------------
		//
		//                   	RRQ/WRQ packet
		
		byte[] request = new byte[4 + filepath.length() + mode.length()];
		System.out.printf("Length of array %d\n", request.length);
		
		for(int i = 0; i < request.length; ++i)
		{
			if(i == 0)
				request[i] = Const.TERM;
			else if(i == 1)
				request[i] = opcode;
			else if(i < filepath.length() +1)
			{
				for(int j = 0; j < filepath.length(); ++j)
					if( (int)filepath.charAt(j) < 256 )
						request[i++] = (byte) filepath.charAt(j);
					else
					{
						System.err.printf("Character '%c' is not ascii\n", filepath.charAt(j));
						return null;
					}
				request[i] = Const.TERM;
			}
			else 
			{
				for(int j = 0; j < mode.length(); ++j)
					request[i++] = (byte) mode.charAt(j);
				
				System.out.printf("i = %d\n", i);
				request[i] = Const.TERM;
			}
			
		}
		
		
		
		return request;
	}
	

	public static void main(String[] args) 
	{
		

	}

}
