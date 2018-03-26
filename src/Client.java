// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-23

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.commons.io.FileUtils;


public class Client 
{
	private InetAddress myAddress;
	private InetAddress serverAddress;
	private int myPort;
	private int serverPort;
	
	FileWriter fw = null;
	BufferedWriter bw = null;
	
	File file = null;
	
	private DatagramSocket socket;
	
	
	public Client(String serverAddress)
	{
		Random ran = new Random();
		
		try 
		{
			// get a port number between 1024 and 65535
			myPort = ran.nextInt(Const.MAX_PORT - Const.RESERVED_PORTS) + Const.RESERVED_PORTS;
			this.myAddress = InetAddress.getLocalHost();
			
			
			serverPort = Const.TFTP_PORT;
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
		byte[] buff = new byte[Const.PACKET_SIZE];
		
		DatagramPacket packet = new DatagramPacket(buff, buff.length);
		
		try 
		{
			socket.receive(packet);
		} 
		catch (IOException e) 
		{
			
			e.printStackTrace();
		}
		
		if (serverPort == Const.TFTP_PORT) 
		{
			serverPort = packet.getPort();
			System.out.printf("New server port: %d\n", serverPort);
		}
		
		
		System.out.printf("data length: %d\n", packet.getLength());
		
		byte data[] = new byte[packet.getLength()];
		
		for(int i = 0; i < data.length; ++i)
			data[i] = buff[i];
		
		return data;
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

		boolean isOctet = mode.toLowerCase().equals("octet");
		byte reply[] = null;
		
		byte request[] = createReadWriteRequest(Const.RRQ, filepath, mode);
		int blockNumber_client = 1;
		int blockNumber_server = 0;
		
		if(isOctet)
		{
			file = new File(filepath);
			file.delete();
		}
		else
		{
			try 
			{
				fw = new FileWriter(filepath);
				bw = new BufferedWriter(fw);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		
		sendPacket(request);
		
		do
		{
			reply = getMail();
			
			blockNumber_server = 0;
			blockNumber_server |= unsignedByteToInt(reply[2]);
			blockNumber_server <<= 8;
			blockNumber_server |= unsignedByteToInt(reply[3]);
			
			
			if(reply[1] != Const.DATA)
				// handle error
				System.err.printf("Packet does not contain data. Opcode: %d", reply[1]);
			
			if(blockNumber_server != blockNumber_client)
				 // very bad things
				System.err.printf("Incorrect Block Number. Got: %d Want: %d\n", blockNumber_server, blockNumber_client);
			
			
			if(isOctet)
				writeOctetToFile(reply);
			else
				writeAsciiToFile(reply);
			
			byte ack[] = {Const.TERM, Const.ACK, reply[2], reply[3] };
			sendPacket(ack);
			
			if(++blockNumber_client > 65535)
				blockNumber_client = 0;
		
		
		} while(reply.length == Const.PACKET_SIZE);
		
		
		if(isOctet)
		{
			file = null;
		}
		else
		{
			try 
			{
				bw.flush();
				fw = null;
				bw = null;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		
	}
	
	
	private boolean writeAsciiToFile(byte inArray[]) 
	{
		String str = null;
		try 
		{
			str = new String(inArray, 4, inArray.length -4, "UTF-8");
			
			str = str.replaceAll("\r\n", System.lineSeparator());
			
			bw.write(str);
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		
		return true;	
	}
	
	
	private boolean writeOctetToFile(byte inArray[]) 
	{
		byte b[] = new byte[inArray.length -4];
		
		for(int i = 0; i < b.length; ++i)
			b[i] = inArray[i + 4];
		
		try 
		{
			FileUtils.writeByteArrayToFile(file, b, true);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		return true;	
	}
	
	/*
	 * Fixing the cancer that is Java
	 * Thanks to http://www.rgagnon.com/javadetails/java-0026.html
	 */
	private int unsignedByteToInt(byte b)
	{
		return (int) b & 0xFF;
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
