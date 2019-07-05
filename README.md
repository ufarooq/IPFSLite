
# IPFS Lite
IPFS Lite node with modern UI to support standard use cases of IPFS


## IPFS
The InterPlanetary File System "is a peer-to-peer (p2p) filesharing system that aims to fundamentally change the way information is distributed across & beyond the globe. IPFS consists of several innovations in communication protocols and distributed systems that have been combined to produce a file system like no other."
<br>
"It attempts to address the deficiencies of the client-server model and HTTP web through a novel p2p file sharing system."
<br>
Official website of IPFS: https://ipfs.io/


## General 
**IPFS Lite** supports the standard uses cases of IPFS (like add, cat, get, publish, etc).
It rovides a modern UI to realize this common use cases and has
the possibility to customize the configuration of the IPFS node.
The basic characteristics of the app are decentralized, respect of personal data,
open source, free of charge, transparent, free of advertising and legally impeccable.



## Documentation

**IPFS Lite** is a decentralized file-sharing and communication application which based on
the following core technologies.
- IPFS (https://ipfs.io/) 
<br>The main component is here the IPFS technology and will be described in detail
in the later sections. 
- IOTA (https://www.iota.org/)
<br>The IOTA technology is used to support two use-cases of this application.
    - Offline Mode 
    <br>TODO
    - Faster Node Access
    <br>TODO
- WebRTC (https://webrtc.org/) 
<br>The WebRTC component is required to establish a telephony functionality within
this application. Consider this feature as a proof of concept (PoC). Communicating via WebRTC 
requires, that communicating nodes must use all the **IPFS Lite** application,  
because the integration is **not** IPFS "standard" (see Limitation)


### **IPFS Lite** versus **IPFS**
This section describes the differences between an **IPFS Lite** node and an regular **IPFS** node.
<br>
Despite the enhancements of the **IPFS Lite** application (like IOTA and WebRTC)
there are some small differences between such node kinds.
<br>
In general an **IPFS Lite** has the same functionality like an regular node.
There are some small differences which are described here. The reasons are outlined in brackets.
- **No** Gateway Support
<br>An IPFS gateway is not supported [Performance,Security,AndroidQ]
- **No** CLI and HTTP API Support
<br>No public API is supported, the application itself based on the internal IPFS Core API [AndroidQ]
- **No** WebUI Support
<br>The WebUI feature is not supported [Performance,Security,AndroidQ]
- Limited Node Configuration
<br>In the current **IPFS Lite** version under "Settings" the node itself can be configured.
<br>Only a subset of possibilities of a node configuration (**config** file) can be done now. 
<br>The reason is that some of the options might be not valid for an **IPFS Lite** node, because
in the future such feature will not be supported anymore. 
<br>Another reason is that a feature is "enabled" by default.
For example the IPFS pubsub feature is right now, enable by default, there is no way to deactivate
it. (see Limitation) 
<br>But the main reason for the lack of configuration is, that such options are not yet implemented. 


### Enhancements
- Precondition is that communicating nodes using this IPFS Lite application
- Enhance the pubsub feature of IPFS to share files between two nodes (Send Option)
- Integration of WebRTC via the pubsub feature of IPFS to support telephony between two nodes
- Integration of IOTA to support faster node detection and to support a kind of offline mode
between nodes
- Inbox contains all notifications from other nodes which were sent while your node
was offline (Stored on the Tangle, Data is encrypted)



### Features
This section contains the description of the main features of **IPFS Lite**

### Settings
As mentioned before the application offers under "Naviagation/Settings" the configuration 
of the running IPFS node. The IPFS settings will not be desribed here.
<br>See https://github.com/ipfs/go-ipfs/blob/master/docs/config.md for further information.
<br>This section describes the settings **Application Settings** which are located under
"Naviagation/Settings".
The following settings are supported:
- Connection Timeout
<br>The connection timeout defines the timeout of the following operations
    - Peer Connect
    <br>When the timeout is reached and not connection is made to a peer, the process will be aborted
    - Find Content
    <br>When the timeout is reached and the content is not found, the process will be aborted
- Support Offline Notification
<br>When "offline notification" is turned off, no notification will be stored or read from the 
IOTA Tangle database. The **Inbox** might be still filled by other peers, but **IPFS Lite**
does not read out such information. But more important is that **IPFS Lite** does not write
any notification on the IOTA tangle anymore.
- Support Peer Discovery
<br>When "peer discovery" is turned off, no peer information will be stored or read out from
IOTA Tangle database.  The **Outbox** will be not filled anymore and other peers can not read
out the information for faster peer access.


**Important:**
When both options **Support Offline Notification** and **Support Peer Discovery**
are turned off, the **IPFS Lite** application behaves more like a regular IPFS node.
Switching off might have a positive effect on the overall energy consumption. 
Downside might be that the "Send Data" feature does not work offline and the peer discovery might be not working for peers behind NATs.
For sure is, when you just connecting to nodes which have static public IP addresses
and your **IPFS Lite** node also have this properties and 
additionally you and your connecting peers are all the time online, it definitely makes
sense to deactivate the two options.

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
This section contains a set of general issues handling with IPFS.
- Can not connect to "my" node (Assume that "my" node is a regular IPFS node)
<br> This kind of issue can have lots of reasons (here only some ideas)
    - Validate that your node has a static public IP address
    <br>Check that you can connect with another regular IPFS node to "your" node
    <br>**Important:** "your" node should not be behind a router (NAT)
    - IPFS Lite has a Timeout Setting under "Navigation/Settings", increase the timeout

### Todo's
This section contains a set of current and future todo's.
* Official WebRTC Integration of IPFS, when it is ready
<br>More information on https://github.com/libp2p/specs/pull/159

### Dependencies 
- threads-iota (Wrapper implementation around a IOTA light node)
<br>Source : https://gitlab.com/remmer.wilts/threads-iri
- threads-ipfs (Wrapper implementation around a IPFS node)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)

