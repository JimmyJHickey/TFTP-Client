# TFTP Client
## CS 413 Spring 2018 Final Project
#### Ben Andrews, Will Diedrick, Jimmy Hickey

#### TFTP Overview:
__3 modes__
* ascii
* octet
* !mail

start with r/w request
send in 512 byte blocks
terminate with not full block

If a packet gets lost in the network, the intended recipient will timeout and may retransmit his last packet

the sender keeps a one packet buffer
both parties are senders and recievers, one sends file gets acks, other sends acks gets file

most error causes termination, error packet gets sent
errors can be caused by:
* can't satisfy request (file not found)
* bad packet
* lost access (disc full, permission denied)

bad src port does not cause termination, but an error packet is sent

read request is accepted by the first data packet being sent
write request is accepted by an ack, block number 0

ack packet contains block number of the data, are consecutive and start at 1

each party chooses a random TID, get used as src and dest ports
