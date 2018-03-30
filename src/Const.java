// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-23

/*
 * A class of constants
 */
public class Const 
{
	final static int TFTP_PORT = 69;
	final static int MAX_PORT = 65535;
	final static int RESERVED_PORTS = 1024;
	final static int PACKET_SIZE = 516;
	final static int HEADER_SIZE = 4;
	final static int DATA_SIZE = PACKET_SIZE - HEADER_SIZE;
	final static byte TERM = 0;
	
	final static int FIRST_BYTE_MASK = 0x000000FF;
	final static int SECOND_BYTE_MASK = 0x0000FF00;
	
	// opcodes
	final static byte RRQ = 1;
	final static byte WRQ = 2;
	final static byte DATA = 3;
	final static byte ACK = 4;
	final static byte ERROR = 5;
	
	// packet data locations
	final static int OPCODE_MSB_OFFSET = 0;
	final static int OPCODE_LSB_OFFSET = 1;
	final static int BLCK_NUM_MSB_OFFSET = 2;
	final static int BLCK_NUM_LSB_OFFSET = 3;

	// Modes
	final static String NETASCII = "netascii";
	final static String OCTET = "octet";
}
