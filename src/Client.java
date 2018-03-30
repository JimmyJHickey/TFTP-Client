// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-23

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


public class Client 
{
	private InetAddress myAddress;
	private InetAddress serverAddress;
	private int myPort;
	private int serverPort;
	
	FileWriter fw = null;
	BufferedWriter bw = null;
	
	File file = null;
	
	byte overflow_buf[] = null;
	
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
	
	private byte[] getMail()
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
		
		byte data[] = new byte[packet.getLength()];
		
		for(int i = 0; i < data.length; ++i)
			data[i] = buff[i];
		
		return data;
	}
	
	private void sendPacket(byte[] data)
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
	
	
	/*
	 * Gets a file from TFTP server using the given filepath and mode.
	 */
	public void getFile(String filepath, String mode)
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
		
		// send the file request packet
		sendPacket(request);
		
		// receive and save the data of the requested file
		do
		{
			reply = getMail();
			
			blockNumber_server = getBlockNumber(reply);
			
			
			if(getOpcode(reply) != Const.DATA)
				// handle error
				System.err.printf("Packet does not contain data. Opcode: %d", reply[1]);
			
			if(blockNumber_server != blockNumber_client)
				 // very bad things
				System.err.printf("Incorrect Block Number. Got: %d Want: %d\n", blockNumber_server, blockNumber_client);
			
			
			if(isOctet)
				writeOctetToFile(reply);
			else
				writeAsciiToFile(reply);
			
			// build the ack using the raw block number from the servers packet
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
	
	
	/*
	 * Writes an ASCII file to the local machine.
	 * inArray is a raw packet from a TFTP server.
	 */
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
	
	
	/*
	 * Writes a binary file to the local machine.
	 * inArray is a raw packet from a TFTP server.
	 */
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
	 * Sends a file to a TFTP server using the given filepath and mode.
	 */
	public void sendFile(String filepath, String mode)
	{
		boolean isOctet = mode.toLowerCase().equals("octet");
		file = new File(filepath);
		
		long fileLength = file.length();
		long bytesRead = 0;
		
		if(!file.exists()) 
			System.out.printf("Everything sucks. There's no file\n");
		
		InputStream inStream = null;
		
		try 
		{
			 inStream = new FileInputStream(file);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		byte data[] = createReadWriteRequest(Const.WRQ, filepath, mode);
		
		int blockNumber_client = 0;
		int blockNumber_server = 0;
		
		byte reply[];
		byte inBytes[] = null;
		
		// send data packets, receive and verify acks, and build the next packet to send
		for(;;)
		{
			sendPacket(data);

			reply = getMail();
			
			if(getOpcode(reply) != Const.ACK)
				System.err.printf("Yo that ain't an ACK. It's an %d%d\n", reply[0], reply[1]);
				
			blockNumber_server = getBlockNumber(reply);
			
			if(blockNumber_server != blockNumber_client)
				System.err.printf("Yo that ain't tha right block. It's %d%d\n", reply[2], reply[3]);
			
			
			// !!!   if it is not the first packet   !!!
			// !!! and it is not the max packet size !!!
			// !!!       we are done receiving       !!!
			if(data.length < Const.DATA_SIZE && blockNumber_client !=0)
				break;
			
			blockNumber_client++;
			
			try 
			{
				inBytes = IOUtils.toByteArray(inStream, Math.min(Const.DATA_SIZE, fileLength - bytesRead));
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
			bytesRead += Const.DATA_SIZE;
			
			// put the data into the packet
			if(isOctet)
				data = formatOctetWrite(inBytes);
			else
				data = formatAsciiWrite(inBytes);
			
			
			
			// build packet header
			data[Const.OPCODE_MSB_OFFSET] = Const.TERM;
			data[Const.OPCODE_LSB_OFFSET] = Const.DATA;
			
			data[Const.BLCK_NUM_MSB_OFFSET] = (byte)((blockNumber_client & Const.SECOND_BYTE_MASK) >> 8);
			data[Const.BLCK_NUM_LSB_OFFSET] = (byte)(blockNumber_client & Const.FIRST_BYTE_MASK);			
		} // end for(;;)		
	}
	
	
	/*
	 * Formats an ASCII byte array to send to a TFTP server.
	 */
	private byte[] formatAsciiWrite(byte inArray[])
	{		
		String strBytes = null;
		
		try 
		{
			strBytes = new String(inArray, 0, inArray.length, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
		
		// format text to fit netascii standard 
		if(System.lineSeparator().equals("\n"))
			strBytes = strBytes.replaceAll("\n", "\r\n");
		
		System.out.printf("%s\n", strBytes);
		
		
		byte byteArray[] = strBytes.getBytes();
		
		// overflow from last packet
		int residualOverflow = (overflow_buf == null ? 0 : overflow_buf.length);
		
		// the overflow that will be created by this packet
		int numOverflow = Math.max(0, (byteArray.length - Const.DATA_SIZE) + residualOverflow);
		
		// that data we are sending is either the size of the old overlow + the data in,
		//  or the max data packet size, which ever is _smaller_
		byte data[] = new byte[Math.min(residualOverflow + byteArray.length, Const.DATA_SIZE) + Const.HEADER_SIZE];
		int data_p = Const.HEADER_SIZE;
		
		// fill the packet data
		for(int i = 0; i < residualOverflow; ++i)
			data[data_p++] = overflow_buf[i];
		
		for(int i = 0; i < byteArray.length - numOverflow; ++i)
			data[data_p++] = byteArray[i];		
		
		
		// if there is overflow save it into the overflow buffer
		if(numOverflow > 0)
		{
			overflow_buf = new byte[numOverflow];
			
			for(int i = 0; i < numOverflow; ++i)
				overflow_buf[i] = byteArray[byteArray.length - numOverflow +  i];
		}
		else
		{
			overflow_buf = null;
		}
				
		return data;
	}
	
	
	/*
	 * Formats a binary byte array to be sent to a TFTP server.
	 */
	private byte[] formatOctetWrite(byte inArray[])
	{
		byte data[] = new byte[inArray.length + Const.HEADER_SIZE];
		
		for(int i = 0; i < inArray.length; ++i)
			data[i + Const.HEADER_SIZE] = inArray[i];
		
		
		return data;
	}
	
	
	/*
	 * Returns the block number from the given packet data.
	 */
	private int getBlockNumber(byte[] data)
	{
		int blockNumber = 0;
		blockNumber |= unsignedByteToInt(data[Const.BLCK_NUM_MSB_OFFSET]);
		blockNumber <<= 8;
		blockNumber |= unsignedByteToInt(data[Const.BLCK_NUM_LSB_OFFSET]);
		
		return blockNumber;
	}
	
	
	/*
	 * Returns the opcode from the given packet data.
	 */
	private int getOpcode(byte[] data)
	{
		int code = 0;
		code |= unsignedByteToInt(data[Const.OPCODE_MSB_OFFSET]);
		code <<= 8;
		code |= unsignedByteToInt(data[Const.OPCODE_LSB_OFFSET]);
		
		return code;
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
