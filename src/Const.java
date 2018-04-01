// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-23

/*
 * A class of constants
 */
public final class Const 
{
	public final static int TFTP_PORT = 69;
	public final static int PACKET_SIZE = 516;
	public final static int HEADER_SIZE = 4;
	public final static int DATA_SIZE = PACKET_SIZE - HEADER_SIZE;
	public final static byte TERM = 0;
	
	public final static int FIRST_BYTE_MASK = 0x000000FF;
	public final static int SECOND_BYTE_MASK = 0x0000FF00;
	
	public final static int SOCKET_TIMEOUT = 5000; // milliseconds
	public final static int SOCKET_TIMEOUT_LIMIT = 3;
	
	public final static int MAX_BLOCK_NUMBER = 65535;
	
	// opcodes
	public final static byte RRQ = 1;
	public final static byte WRQ = 2;
	public final static byte DATA = 3;
	public final static byte ACK = 4;
	public final static byte ERROR = 5;
	
	// packet data locations
	public final static int OPCODE_MSB_OFFSET = 0;
	public final static int OPCODE_LSB_OFFSET = 1;
	public final static int BLCK_NUM_MSB_OFFSET = 2;
	public final static int BLCK_NUM_LSB_OFFSET = 3;

	// Modes
	public final static String NETASCII = "netascii";
	public final static String OCTET = "octet";
}
