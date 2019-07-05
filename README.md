
# IPFS Lite
IPFS Lite node with modern UI to support standard use cases of IPFS


## IPFS
The InterPlanetary File System "is a peer-to-peer (p2p) filesharing system that aims to fundamentally change the way information is distributed across & beyond the globe. IPFS consists of several innovations in communication protocols and distributed systems that have been combined to produce a file system like no other."
<br>
"It attempts to address the deficiencies of the client-server model and HTTP web through a novel p2p file sharing system."
<br>
Official website of IPFS: https://ipfs.io/


## Features 
Support of the standard uses cases of IPFS (like add, cat, get, publish, etc).
Provide a modern UI to realize this common use cases.
Possibility to customize the configuration of the IPFS node.

## Enhancements:
- Precondition is that communicating nodes using this IPFS Lite application
- Enhance the pubsub feature of IPFS to share files between two nodes (Send Option)
- Integration of WebRTC via the pubsub feature of IPFS to support telephony between two nodes
- Integration of IOTA to support faster node detection and to support a kind of offline mode
between nodes
- Inbox contains all notifications from other nodes which were sent while your node
was offline (Stored on the Tangle, Data is encrypted)


## Dependencies 
- threads-iota (Wrapper implementation around a IOTA light node)
<br>Source : https://gitlab.com/remmer.wilts/threads-iri
- threads-ipfs (Wrapper implementation around a IPFS node)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)


## Documentation

**IPFS Lite** is a decentralized file-sharing and communication application which based on the
three core technologies.
* IPFS (https://ipfs.io/) 
* IOTA (https://www.iota.org/)
* WebRTC (https://webrtc.org/) 
The main component is here the IPFS technology and will be described a little bit more in detail
in the later sections. 
</br>
The WebRTC component is just required to establish a telephony functionality within
this application. Consider this feature as a proof of concept (PoC). Communicating via WebRTC 
requires that such nodes must use the **IPFS Lite** application, because the integration is not 
IPFS "standard" (see Limitation)
</br>
The IOTA technology is used to support two use-cases of this application.
* Offline Mode 
* Faster Node Access



### Limitation
This section contains a set of current limitations.
* WebRTC Integration
In the current version of this application it is required that both communicating nodes using 
this app to communicate successfully over WebRTC. In a later version of this tool an official 
enhancement of the IPFS technology should be used.
More information on https://github.com/libp2p/specs/pull/159


### ISSUES
This section contains a set of current issues.
* Official WebRTC Integration of IPFS, when it is ready



