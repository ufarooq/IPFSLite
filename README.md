
## IPFS Lite
- IPFS Lite node with modern UI to support standard use cases of IPFS


### IPFS
The InterPlanetary File System "is a peer-to-peer (p2p) filesharing system that aims to fundamentally change the way information is distributed across & beyond the globe. IPFS consists of several innovations in communication protocols and distributed systems that have been combined to produce a file system like no other."
<br>
"It attempts to address the deficiencies of the client-server model and HTTP web through a novel p2p file sharing system."
<br>
Source: https://hackernoon.com/a-beginners-guide-to-ipfs-20673fedd3f
<br>
Official website of IPFS: https://ipfs.io/


### Features 
Support of the standart uses cases of IPFS (like cat, get, publish, etc).
Provide a modern UI to realize the use cases.
Possibility to customize the configuraton of the IPFS node.

#### Enhancements:
- Enhance the pubsub feature of IPFS to automatically share files between two nodes.
(Precondition is that both nodes using this IPFS Lite application)
- Integration of WebRTC via the pubsub feature of IPFS to support telephony between two nodes.
(Precondition is that both nodes using this IPFS Lite application)
- Integration of IOTA to support faster node detection and to support a kind of offline mode
between nodes. (Precondition is that both nodes using this IPFS Lite application)


### Dependencies 
- go-ipfs (IPFS node implementation)
<br>Source : https://github.com/ipfs/go-ipfs
- threads-ipfs (CLI interface)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)




