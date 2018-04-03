import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-24

public class Driver 
{
	private static void printHelp()
	{
		System.out.printf("A Java implementation of TFTP as described by RFC 1350.\n\n");
		
		System.out.printf("Commands: \n");
		System.out.printf("%s", Const.HELP_CONNECT);
		System.out.printf("%s", Const.HELP_MODE);
		
		System.out.printf("\n");
		
		System.out.printf("%s", Const.HELP_GET);
		System.out.printf("%s", Const.HELP_PUT);
		
		System.out.printf("\n");
		
		System.out.printf("%s", Const.HELP_STATUS);
		System.out.printf("%s",Const.HELP_EXIT);
		System.out.printf("%s", Const.HELP_QUIT);
		System.out.printf("%s", Const.HELP_HELP);

		System.out.printf("\n");
	}
	
	public static void main(String[] args) 
	{
		Client client = null;
		
		String serverIP = null;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		String lineArgs[];
		
		String mode = Const.OCTET;
		
		
		// intercept command line arguments
		if(args.length == 1)
		{
			if(args[0].equals("help"))
			{
				printHelp();
				System.exit(0);
			}
			else
			{
				// if not looking for help assume user entered a valid IP
				serverIP = args[0];
			}
		}
		else if(args.length > 1)
		{
			System.out.printf("Only one command line argument is supported.\n");
			System.out.printf("Choose from: <IPv4 Address> or \"help\"\n");
		}
			
		
		// run the prompt, take user input until exit is requested
		forever:
		for(;;)
		{
			System.out.printf("JavaTFTP>");
			
			try
			{
				line = input.readLine();
			}
			catch (IOException e)
			{
				System.err.printf("error communicating with cli, exiting\n");
				System.exit(-1);
			}
			
			lineArgs = line.split(" ");
			
			switch(lineArgs[0])
			{
				case "connect":
					if(lineArgs.length == 2)
					{
						serverIP = lineArgs[1];
					}
					else
					{
						System.out.printf("%s", Const.MALFORMED);
						System.out.printf("%s\n", Const.HELP_CONNECT);
					}
					
					break;
					
				case "mode":
					if(lineArgs.length == 2 && (lineArgs[1].equals(Const.OCTET) || lineArgs[1].equals(Const.NETASCII)))
					{
						mode = lineArgs[1];
					}
					else
					{
						System.out.printf("%s", Const.MALFORMED);
						System.out.printf("%s", Const.HELP_MODE);
					}
					
					break;
					
				case "get":
					if(lineArgs.length == 2)
					{
						client = new Client(serverIP);
						
						client.getFile(lineArgs[1], mode);
					}
					else
					{
						System.out.printf("%s", Const.MALFORMED);
						System.out.printf("%s", Const.HELP_GET);
					}
					
					break;
					
				case "put":
					if(lineArgs.length == 2)
					{
						client = new Client(serverIP);
						
						client.sendFile(lineArgs[1], mode);
					}
					else
					{
						System.out.printf("%s", Const.MALFORMED);
						System.out.printf("%s", Const.HELP_PUT);
					}
					
					break;
				case "status":
					System.out.printf("Server: %s\n", serverIP == null ? "none" : serverIP);
					System.out.printf("Mode: %s\n", mode);
					
					break;
					
				case "help":
					printHelp();
					
					break;
					
				case "exit":
				case "quit":
				case "q":
					System.out.printf("Terminating\n");
					
					break forever;
					
				default:
					System.out.printf("invalid command, type \"help\" for help\n");
			}
		}
		
	}

}
