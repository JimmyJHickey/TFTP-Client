import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// CS 413 TFTP Client
// Ben Andrews, Will Diedrick, Jimmy Hickey
// 2018-3-24

public class Driver 
{
	
	
	public static void main(String[] args) 
	{
		Client client = null;
		
		String serverIP = null;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		String lineArgs[];
		
		String mode = Const.OCTET;
		
		if(args.length > 0)
		{
			serverIP = args[0];
		}
		
		
		
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
			
			//System.out.printf("%s\n", line);
			lineArgs = line.split(" ");
			
			switch(lineArgs[0])
			{
				case "connect":
					if(lineArgs.length == 2)
						serverIP = lineArgs[1];
					else
						System.out.printf("address not provided\n");
					
					break;
				case "mode":
					if(lineArgs.length == 2 && (lineArgs[1].equals(Const.OCTET) || lineArgs[1].equals(Const.NETASCII)))
						mode = lineArgs[1];
					else
						System.out.printf("invalid mode\nchoose from: \"%s\" and \"%s\"\n", Const.OCTET, Const.NETASCII);
					
					break;
				case "get":
					if(lineArgs.length == 2)
					{
						client = new Client(serverIP);
						
						client.getFile(lineArgs[1], mode);
					}
					else
						System.out.printf("file not provided\n");
					
					break;
				case "put":
					if(lineArgs.length == 2)
					{
						client = new Client(serverIP);
						
						client.sendFile(lineArgs[1], mode);
					}
					else
						System.out.printf("file not provided\n");
					
					break;
				case "exit":
				case "quit":
					System.out.printf("Terminating\n");
					
					break forever;
				default:
					System.out.printf("invalid command, type \"help\" for help\n");
			}
		}
		
	}

}
