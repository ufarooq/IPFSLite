
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




## Documentation

**IPFS Lite** is a decentralized file-sharing and communication application which based on
the following core technologies.
- IPFS (https://ipfs.io/) 
<br>The main component is here the IPFS technology and will be described in detail
in the later sections. 
- IOTA (https://www.iota.org/)
<br>The IOTA technology is used to support two use-cases of this application.
    - Offline Mode 
    - Faster Node Access
- WebRTC (https://webrtc.org/) 
<br>The WebRTC component is required to establish a telephony functionality within
this application. Consider this feature as a proof of concept (PoC). Communicating via WebRTC 
requires, that communicating nodes must use all the **IPFS Lite** application,  
because the integration is **not** IPFS "standard" (see Limitation)


### **IPFS Lite** versus **IPFS**
This section describes the differences between an **IPFS Lite** node and an regular **IPFS** node.
<br>
Despite the not official enhancements of the **IPFS Lite** application (like IOTA and WebRTC)
there are some general differences between such node kinds.
<br>
In general an **IPFS Lite** has the same functionality like an regular node.
There are some differences which are described here. The reasons are outlined in brackets.
- **No** Gateway Support
<br>An IPFS gateway is not supported [Performance,Security,AndroidQ-Support]
- **No** CLI and HTTP API Support
<br>No public API is supported, the API based on the internal IPFS Core API [AndroidQ-Support]
- **No** WebUI Support
<br>An IPFS gateway is not supported [Performance,Security,AndroidQ-Support]
- Limited Node Configuration
<br>In the current **IPFS Lite** version under "Settings" the node itself can be configured.
<br>Only a subset of possibilities of a node configuration (**config** file) can be done now. 
<br>The reason is that some of the options might be not valid for an **IPFS Lite** node, because
in the future such feature will not be supported anymore. 
<br>Another reason is that a feature is "enabled" by default.
For example the IPFS pubsub feature is right now, enable by default, there is no way to deactivate
it. (see Limitation) 
<br>But the main reason for a lack of configuration is, that such options are not yet implemented. 


### Enhancements
- Precondition is that communicating nodes using this IPFS Lite application
- Enhance the pubsub feature of IPFS to share files between two nodes (Send Option)
- Integration of WebRTC via the pubsub feature of IPFS to support telephony between two nodes
- Integration of IOTA to support faster node detection and to support a kind of offline mode
between nodes
- Inbox contains all notifications from other nodes which were sent while your node
was offline (Stored on the Tangle, Data is encrypted)


### Limitation
This section contains a set of current limitations.
* WebRTC Integration
In the current version of this application it is required that both communicating nodes using 
this app to communicate successfully over WebRTC. In a later version of this tool an official 
enhancement of the IPFS technology should be used.
More information on https://github.com/libp2p/specs/pull/159
* Disable Pubsub Feature (Note Configuration)
In the current version of the application the pubsub feature of IPFS is enabled by default,
even when the user does not require it. Reason is, that the pubsub feature is required by
the application features "Send Data" and "WebRTC Telephony". Nevertheless the user should
have the possibility to switch off "pubsub" with the consequence that unofficial IPFS features
do not work anymore.

### Issues
This section contains a set of current issues.
* Official WebRTC Integration of IPFS, when it is ready
<br>More information on https://github.com/libp2p/specs/pull/159


### Dependencies 
- threads-iota (Wrapper implementation around a IOTA light node)
<br>Source : https://gitlab.com/remmer.wilts/threads-iri
- threads-ipfs (Wrapper implementation around a IPFS node)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)

