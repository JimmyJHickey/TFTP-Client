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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


public class Client 
{
	private InetAddress serverAddress;
	private int serverPort;
	
	FileWriter fw = null;
	BufferedWriter bw = null;
	
	File file = null;
	
	byte overflow_buf[] = null;
	
	private DatagramSocket socket;
	private int timeoutCounter = 0;
	
	
	public Client(String serverAddress)
	{		
		try 
		{
			serverPort = Const.TFTP_PORT;
			this.serverAddress = InetAddress.getByName(serverAddress);
			
			socket = new DatagramSocket();
			socket.setSoTimeout(Const.SOCKET_TIMEOUT);
		} 
		catch (UnknownHostException e) 
		{
			System.err.printf("Unkown Host.\n");
		} 
		catch (SocketException e) 
		{
			System.err.printf("Unable to create socket.\n");
		}
	}
	
	private byte[] getMail(byte[] resend_buf, boolean resend)
	{
		byte[] buff = new byte[Const.PACKET_SIZE];
		
		DatagramPacket packet = new DatagramPacket(buff, buff.length);
		
		for(timeoutCounter = 0; timeoutCounter < Const.SOCKET_TIMEOUT_LIMIT; ++timeoutCounter)
			try 
			{
				socket.receive(packet);
				break;
			} 
			catch (SocketTimeoutException e)
			{
				System.err.printf("Timeout while reciveing, trying %d more times.\n", Const.SOCKET_TIMEOUT_LIMIT - timeoutCounter -1);
				
				if(resend)
					sendPacket(resend_buf);
			}
			catch (IOException e) 
			{
				System.err.printf("Unknown error receiving file.\n");
				
				return null;
			}
		
		if(timeoutCounter >= Const.SOCKET_TIMEOUT_LIMIT)
			return null;
		
		if (serverPort == Const.TFTP_PORT) 
		{
			serverPort = packet.getPort();
			System.out.printf("New server port: %d\n", serverPort);
		}
		
		byte data[] = new byte[packet.getLength()];
		
		for(int i = 0; i < data.length; ++i)
			data[i] = buff[i];
		
		if(getOpcode(data) == Const.ERROR)
		{
			System.err.printf("Error! Error Code: %d\n", getBlockNumber(data));
			System.err.printf("%s\n", new String(data, Const.HEADER_SIZE, data.length -Const.HEADER_SIZE));
			return null;
		}
			
		
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
			System.err.printf("Unknown error sending packet.\n");
		}
	}
	
	
	/*
	 * Gets a file from TFTP server using the given filepath and mode.
	 */
	public boolean getFile(String filepath, String mode)
	{

		boolean isOctet = mode.toLowerCase().equals("octet");
		
		byte reply[] = null;
		byte request[] = createReadWriteRequest(Const.RRQ, filepath, mode);
		
		if(request == null)
			return false;
		
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
				System.err.printf("Unknown error opening file.\n");
			}
		}
		
		// send the file request packet
		sendPacket(request);
		
		reply = getMail(request, false);
		
		// receive and save the data of the requested file
		while(reply != null && reply.length == Const.PACKET_SIZE)
		{
			blockNumber_server = getBlockNumber(reply);
			
			
			if(getOpcode(reply) != Const.DATA)
			{
				// handle error
				System.err.printf("Packet does not contain data. Opcode: %d", reply[1]);
				System.err.printf("Expected data packet (opcode %d). Got opcode %d\n", Const.DATA, getOpcode(reply));
				return false;
				
			}
			
			if(blockNumber_server != blockNumber_client)
			{
				// a repeat ACK on the network will kill us, whoops
				System.err.printf("Expected block number %d. Got block number %d\n", blockNumber_client, blockNumber_server);
				sendErrorPacket(Const.EC_UNKNOWN);
				return false;
			}
			
			
			if(isOctet)
				writeOctetToFile(reply);
			else
				writeAsciiToFile(reply);
			
			// build the ack using the raw block number from the servers packet
			byte ack[] = {Const.TERM, Const.ACK, reply[2], reply[3] };
			sendPacket(ack);
			
			reply = getMail(ack, true);
			
			if(reply == null)
			{
				System.err.printf("Server is unresponsive, file download failed\n");
				return false;
			}
			
			if(++blockNumber_client > Const.MAX_BLOCK_NUMBER)
				blockNumber_client = 0;
			
		} // end while
		
		
		// send the last ack and save the last data packet only if the last reply was not an error
		if(reply != null)
		{
			if(isOctet)
				writeOctetToFile(reply);
			else
				writeAsciiToFile(reply);
			
			byte ack[] = {Const.TERM, Const.ACK, reply[2], reply[3] };
			sendPacket(ack);
		}
		
		
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
				System.err.printf("Error writing to file.\n");
				return false;
			}
		}
		
		return true;
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
			str = new String(inArray, Const.HEADER_SIZE, inArray.length -Const.HEADER_SIZE, "UTF-8");
			
			str = str.replaceAll("\r\n", System.lineSeparator());
			
			// if the "\r\n" is split into two packets their will be a trailing
			//  '\r' at the end of this string that does not get caught by the replace all
			if(System.lineSeparator().equals("\n") && str.charAt(str.length() -1) == '\r')
				str = str.substring(0, str.length() -1);
			
			bw.write(str);
		} 
		catch (UnsupportedEncodingException e) 
		{
			System.err.printf("Text Encoding error.\n");
			
			sendErrorPacket(Const.EC_NOTDEF);
			return false;
		} 
		catch (IOException e) 
		{
			System.err.printf("Unknown IO error.\n");
			
			sendErrorPacket(Const.EC_NOTDEF);
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
			System.err.printf("Error writing to file.\n");
			
			sendErrorPacket(Const.EC_DISKFULL);
			return false;
		}
		
		return true;	
	}
	
	
	/*
	 * Sends a file to a TFTP server using the given filepath and mode.
	 */
	public boolean sendFile(String filepath, String mode)
	{
		boolean isOctet = mode.toLowerCase().equals("octet");
		file = new File(filepath);
		
		long fileLength = file.length();
		long bytesRead = 0;
		long bytesToRead;
		
		if(!file.exists()) 
		{
			System.err.printf("Could not find specified file\n");
			return false;
		}
			
		
		InputStream inStream = null;
		
		try 
		{
			 inStream = new FileInputStream(file);
		}
		catch (FileNotFoundException e)
		{
			System.out.printf("Error opening file.\n");
			
			return false;
		}
		
		byte data[] = createReadWriteRequest(Const.WRQ, filepath, mode);
		
		if(data == null)
			return false;
		
		int blockNumber_client = 0;
		int blockNumber_server = 0;
		
		byte reply[];
		byte inBytes[] = null;
		
		// send data packets, receive and verify acks, and build the next packet to send
		for(;;)
		{
			sendPacket(data);

			reply = getMail(data, true);
			
			if(reply == null)
			{
				System.err.printf("Server is unresponsive, file upload failed\n");
				return false;
			}
			
			if(getOpcode(reply) != Const.ACK)
			{
				System.err.printf("Expected ACK (opcode %d). Got opcode %d%d\n", Const.ACK, reply[0], reply[1]);
				sendErrorPacket(Const.EC_UNKNOWN);
				return false;
			}
				
			blockNumber_server = getBlockNumber(reply);
			
			if(blockNumber_server != blockNumber_client)
			{
				System.err.printf("Expected block number %d. Got block number %d\n", blockNumber_client, blockNumber_server);
				sendErrorPacket(Const.EC_UNKNOWN);
				return false;
			}
			
			// !!!   if it is not the first packet   !!!
			// !!! and it is not the max packet size !!!
			// !!!       we are done receiving       !!!
			if(data.length < Const.DATA_SIZE && blockNumber_client !=0)
				break;
			
			
			// else prepare the next block to be sent
			
			if(++blockNumber_client > Const.MAX_BLOCK_NUMBER)
				blockNumber_client = 0;
			
			// if in ASCII mode and the overflow buffer has filled larger than DATA_SIZE
			// we need to empty it before sending any new data
			if(mode.equals(Const.NETASCII) && overflow_buf != null && overflow_buf.length >= Const.DATA_SIZE)
			{				
				data = new byte[Const.PACKET_SIZE];
				
				// move the data to be sent out out of the overflow buffer
				System.arraycopy(overflow_buf, 0, data, Const.HEADER_SIZE, Const.DATA_SIZE);
				
				// copy the overflow buffer
				byte overflow_cp[] = new byte[overflow_buf.length];
				System.arraycopy(overflow_buf, 0, overflow_cp, 0, overflow_buf.length);
				
				// create new overflow buffer from the overflow from sending the overflow
				overflow_buf = new byte[overflow_buf.length - Const.DATA_SIZE];
				System.arraycopy(overflow_cp, Const.DATA_SIZE, overflow_buf, 0, overflow_buf.length);
								
			}
			else
			{
				bytesToRead = Math.min(Const.DATA_SIZE, fileLength - bytesRead);
				
				try 
				{
					inBytes = IOUtils.toByteArray(inStream, bytesToRead);
				} 
				catch (IOException e) 
				{
					System.err.printf("Error reading from file.\n");
					
					return false;
				}
				
				bytesRead += bytesToRead;
				
				System.out.printf("bytes read: %d\n", bytesRead);
				
				// put the data into the packet
				if(isOctet)
					data = formatOctetWrite(inBytes);
				else
					data = formatAsciiWrite(inBytes);
			}
			
			
			// build packet header
			data[Const.OPCODE_MSB_OFFSET] = Const.TERM;
			data[Const.OPCODE_LSB_OFFSET] = Const.DATA;
			
			data[Const.BLCK_NUM_MSB_OFFSET] = (byte)((blockNumber_client & Const.SECOND_BYTE_MASK) >> 8);
			data[Const.BLCK_NUM_LSB_OFFSET] = (byte)(blockNumber_client & Const.FIRST_BYTE_MASK);			
		} // end for(;;)	
		
		return true;
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
			System.err.printf("Unknown text encoding.\n");
			
			return null;
		}
		
		// format text to fit netascii standard 
		if(System.lineSeparator().equals("\n"))
			strBytes = strBytes.replaceAll("\n", "\r\n");
		
		
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
		
		if(!mode.equals(Const.NETASCII) && !mode.equals(Const.OCTET))
		{
			System.err.printf("Bad mode: %s does not exist. Try \"%s\" or \"%s\".\n", mode, Const.OCTET, Const.NETASCII);
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
		
		// this for loop is evil
		//  i gets incremented in multiple places within it
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
					{
						request[i++] = (byte) filepath.charAt(j);
					}
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
				
				request[i] = Const.TERM;
			}
			
		}
		
		return request;
	}
	

	private void sendErrorPacket(int errorCode)
	{
		String errorMessage;
		switch (errorCode)
		{
			case Const.EC_NOTDEF:
				errorMessage = Const.EM_NOTDEF;
				break;
				
			case Const.EC_NOTFOUND:
				errorMessage = Const.EM_NOTFOUND;
				break;
				
			case Const.EC_ACCVIO:
				errorMessage = Const.EM_ACCVIO;
				break;
				
			case Const.EC_DISKFULL:
				errorMessage = Const.EM_DISKFULL;
				break;
				
			case Const.EC_ILLEGAL:
				errorMessage = Const.EM_ILLEGAL;
				break;
				
			case Const.EC_UNKNOWN:
				errorMessage = Const.EM_UNKNOWN;
				break;
				
			case Const.EC_FILEEXIST:
				errorMessage = Const.EM_FILEEXIST;
				break;
				
			case Const.EC_NOUSER:
				errorMessage = Const.EM_NOUSER;
				break;
				
			default:
				System.err.printf("WTF\n");
				return;	
		}
		byte errorPacket [] = new byte[Const.HEADER_SIZE + errorMessage.length() + 1];
		
		// Op code
		errorPacket[Const.OPCODE_MSB_OFFSET] = 0;
		errorPacket[Const.OPCODE_LSB_OFFSET]= Const.ERROR;
		
		// Error code
		errorPacket[Const.BLCK_NUM_MSB_OFFSET] = 0;
		errorPacket[Const.BLCK_NUM_LSB_OFFSET] = (byte) errorCode;
		
		// Error message
		System.arraycopy(errorMessage.getBytes(), 0, errorPacket, Const.HEADER_SIZE, errorMessage.length());
		
		// End packet
		errorPacket[errorPacket.length - 1] = Const.TERM;
		
		
		System.err.printf("%s\n", errorMessage);
		sendPacket(errorPacket);
		return;
		
	}
	
	public static void main(String[] args) 
	{
		Client myClient = new Client("127.0.0.3");
		// Ben is right this time
		//myClient.getFile("lorem_100000", "netascii");
		myClient.sendFile("lorem_lines", "netascii");

	}

}
