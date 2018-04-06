# Trivial File Transfer Protocol Client
## CS 413 Spring 2018 Final Project
#### Ben Andrews, Will Diedrick, Jimmy Hickey

#### TFTP 
A Java implementation of TFTP as described by RFC 1350.

Commands:
	connect <IPv4 Address>	Specify the address of the TFTP server to interact with.
	mode [octet, netascii]	The operating mode of the file transfers.

	get <filepath>	Download the given file from the selected server.
	put <filepath>	Upload the given file to the selected server.

	status	Show the current status
	exit	Exit JavaTFTP
	quit	alias for exit
	help	Show this help
