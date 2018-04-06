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
	
	// error codes
	public final static int EC_NOTDEF = 0;
	public final static int EC_NOTFOUND = 1;
	public final static int EC_ACCVIO = 2;
	public final static int EC_DISKFULL = 3;
	public final static int EC_ILLEGAL = 4;
	public final static int EC_UNKNOWN = 5;
	public final static int EC_FILEEXIST = 6;
	public final static int EC_NOUSER = 7;
	
	// error messages
	public final static String EM_NOTDEF = "Unknown error. ¯\\_(^.^)_/¯";
	public final static String EM_NOTFOUND = "File not found.";
	public final static String EM_ACCVIO = "Access violation.";
	public final static String EM_DISKFULL = "Disk full or allocation exceeded.";
	public final static String EM_ILLEGAL = "Illegal TFTP operation.";
	public final static String EM_UNKNOWN = "Unknown transfer ID.";
	public final static String EM_FILEEXIST = "File already exists.";
	public final static String EM_NOUSER = "No such user.";
	
	
	// packet data locations
	public final static int OPCODE_MSB_OFFSET = 0;
	public final static int OPCODE_LSB_OFFSET = 1;
	public final static int BLCK_NUM_MSB_OFFSET = 2;
	public final static int BLCK_NUM_LSB_OFFSET = 3;

	// Modes
	public final static String NETASCII = "netascii";
	public final static String OCTET = "octet";
	
	
	// driver help strings
	public final static String HELP_CONNECT = "\tconnect <IPv4 Address>\tSpecify the address of the TFTP server to interact with.\n";
	public final static String HELP_MODE = "\tmode [octet, netascii]\tThe operating mode of the file transfers.\n";
	public final static String HELP_GET = "\tget <filepath>\tDownload the given file from the selected server.\n";
	public final static String HELP_PUT = "\tput <filepath>\tUpload the given file to the selected server.\n";
	public final static String HELP_STATUS = "\tstatus\tShow the current status\n";
	public final static String HELP_EXIT = "\texit\tExit JavaTFTP\n";
	public final static String HELP_QUIT = "\tquit\talias for exit\n";
	public final static String HELP_HELP = "\thelp\tShow this help\n";
	public final static String MALFORMED = "Malformed command\n";





}
