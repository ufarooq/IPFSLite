
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
<br>The IOTA technology is used to support two main use-cases of this application.
    - Offline Mode 
    <br>When an **IPFS Lite** node is not online or reachable by another **IPFS Lite** node, then such
    a node can send a "offline" notification to the "offline" node. The "offline" node can later
    retrieve the information and tries to react on it.
    - Peer Discovery
    <br>When an **IPFS Lite** node is behind a NAT, the official IPFS peer discovery mechanism fails very often.
    <br>Therefore this temporary mechanism was develop to establish a connection between nodes,
    even when both nodes are behind NAT's.
    <br>Still even with this solution, the connection is not always succeeded. Just to keep that in mind.
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
This section describes the enhancements of an **IPFS Lite** node.
Precondition for using this enhancements is, that all participants using this application
The enhancements are:
- Using the Pubsub feature of IPFS to share files between two nodes (**Send Data** Option)
- Integration of WebRTC via the Pubsub feature of IPFS to support telephony between two nodes
- Integration of IOTA to support faster node detection (**Support Peer Discovery**) and to support 
a kind of offline mode between nodes (**Support Offline Notification**)
- The **Inbox** contains all notifications from other nodes which were sent while your node
was offline (Stored on the Tangle, Data is encrypted)

Example:
```
"pid": "WlI6VDdzIARqhk4kDYXMjHAFOGnLEcloFNjvh224L/Qdm3tZ7yPjJlzFRmJMl0rBtjn8p629+rV5 sw7pLU5UEJBLJcOpLDSOw9Yn3wuyJw/bkkRHUQGeksWVXxLkU2zD9W7r7rdEng0mjGvJZ44K5Ufj NHEXaVJKD8VU9h4+blEWy2SQlOVw/WSL3u+wqupruLZ26uGUIrstEij7VxAhlyq8BfM9GwwdsbmW DOCLK1g8Ew+r/KI+vFCgo5KI4bCEc9mnv7UqOdEyPWaiF6c1E4hN7hsQxIThwH6rt6zfIYEW/ubT wApwcBN2djiUx+2lUTUZY0NvHpbzgOOrkcyxQg== "
"est": "1"
"cid": "pUxf4KXU9Bbt/PGK/GqIylrehiyfpUNIMKB1vN0QMvw708jMoPvGhaEbylONZ/hxqNjtrpVm8Sok FeFGKl5S0EeexmxQ3j/r3dkQDxbRakfm6waRXiC1QM+HcHIUsOKbqEXCYQHMb5gowSPQjVFcBFZA pLsjGZ/0RXwZ5xd8Zf87hqtsj7gsTlsRmujZAmDobtTmgI8b2P+7K597oP37v2VbY1EaXp3QMzmO TvYzdaI48/hRBGEZrBKuBbFPTYdJFgHCJix1MBo8xe9qYaW+sRKERCb2xvA4ynhJGPH+OKMM95LK cCpdr57yBHs6fIS51RERqTN2M49IV3JDVPekWA== "
```
The "pid" is the Peer ID (PID) encrypted.
The "cid" is the Content ID (CID) encrypted.
The "est" is the command to execute.  When "est" is "1", then the **IPFS Lite** node can download
the CID from the given PID.
**Note:** The data is encrypted with the public key of the **IPFS Lite** node. Only the owner
of the **IPFS Lite** node can with its private key decrypt the real CID and PID value.
**Important:** When a peer sends e.g. multiple files to another peer, then the data will be stored
into a single CID object and be stored on the tangle.

- The **Outbox** contains all peer discovery information of an **IPFS Lite** node. The data
is not yet encrypted on the Tangle. There is no need so far, because no "important" data
is stored in such a transaction. The data contains the name of the  **IPFS Lite** node
(the brand name of the phone) and network access data. 


Example:
```
"peers": 
    "{
        "QmdGQoGuK3pao6bRDqGSDvux5SFHa4kC2XNFfHFcvcbydY":"/ip4/139.178.69.3/udp/4001/quic",
        "QmRdjvsyoNjA2ZfAQBtQ7A2m5NmtSXLgxE55Brn1AUjZ1v":"/ip4/147.75.106.163/udp/4001/quic
    "}"
"adds": 
    "{
        "peer":"/ip4/104.248.37.1/tcp/4001",
        "alias":"Xiaomi Mi A2",
        "pid":"Qmf6KmrY8WRishWYvAsyJJ2NcfDCqGGtAE47srYR7fqKFj
    "}"
```

The "peers" section are relays peers which were automatically detected.
Each of the peers have an address and a Peer ID (PID).
The "adds" section contains a fast "peer" address with the given PID ("pid").
The "alias" is the name of the **IPFS Lite** node.
**Note:**
The "alias" has nothing to do with the "peer" and "pid". ("peer" and "pid" belong the
the fast gateway peer)

### Features
This section contains the description of the main features of the **IPFS Lite** application.
- Share Content
<br>The **Send Data** (Share Content) feature is the main feature of this application.
<br>A regular IPFS node support does **not** support a share content feature in a way that
a node "A" tells another node "B", that it can download data from node "A".
To support this feature in an **IPFS Lite** node some adoptions has to be made.
    - Enable Pubsub
    <br>Each **IPFS Lite** node enables the IPFS Pubsub feature by default.
    - Communication via Pubsub
    <br>So when a node "A" tells another node "B" that it can download content data, then
    it is basically a pubsub message from "A" to "B" with the given information.
    In this case it is just a CID object which should be downloaded.
    **ToDo:** 
    In the current implementation the CID object is not yet encrypted (but can easily be done).
    But the topic which is used for the 
    - Whitelist of Peers
    <br>Each **IPFS Lite** node has a list of peers which are allowed to send data.
    <br>Basically such peers which are listed in the section "Peers" and which are not blocked
    by the user manually.
- Transparency
<br>The **IPFS Lite** application itself should be transparent as possible for the user.
<br>To reach this goal several aspects are handled
    -  Open Source and Free
    <br>All the source code of the **IPFS Lite** application is open source.
    <br>**IPFS Lite** application is available on FDroid, which is an indicator for good free software. 
    (No malware has yet be found on FDroid)
    <br>The app is free and therefore decentralized, that ensures that no specific server are running which costs money for the provider.
    - Transparency Information
    <br>**IPFS Lite** application contains functionality which are only there for transparency.
    <br>The **Console** prints out messages from the underlying IPFS node. Besides for debug reason, 
    the user should also get trust in the application. The **Settings** of the application offers 
    a debug flag, where even more output can be retrieved from the underlying IPFS node.
    <br>The **Inbox** and **Outbox** have no real value for the user of the application, it should
    only show how, where and what kind of information are stored.

    
### Settings
As mentioned before the application offers under "Navigation/Settings" the configuration 
of the running IPFS node. The IPFS settings will not be described here.
<br>See https://github.com/ipfs/go-ipfs/blob/master/docs/config.md for further information.
<br>This section describes the settings **Application Settings** which are located under
"Navigation/Settings".
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
Downside might be that the **Send Data** feature does not work offline and the peer discovery 
might be not working for peers behind NATs.
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
the application features **Send Data** and "WebRTC Telephony". Nevertheless the user should
have the possibility to switch off "pubsub" with the consequence that unofficial IPFS features
do not work anymore.


### Issues
This section contains a set of general issues handling with IPFS.
- Can not connect to "my" node (Assume that "my" node is a **regular** IPFS node)
<br> This kind of issue can have lots of reasons (here only some ideas)
    - Validate that your node has a static public IP address
    <br>Check that you can connect with another regular IPFS node to "your" node
    <br>**Important:** "your" node should not be behind a router (NAT). But when "your" node
    is behind a router, your need a "third" node which behaves like a "relay" node. Both
    "your" node and also your **IPFS Lite** node should be connected before to the "relay" node.
    Then a successful communication can take place.
    - IPFS Lite has a Timeout Setting under "Navigation/Settings", increase the timeout
- Can not connect to "my" node (Assume that "my" node is also a **IPFS Lite** node)
<br> This kind of issue should in theory not happen, when both  **IPFS Lite** nodes are online and
the options **Support Offline Notification** and **Support Peer Discovery** in the settings are 
activated on both machines.
    - Verify that on the "Console" no errors are printed
    - Verify that the "Navigation/Outbox" has peer access information 
    <br>Date of the transaction might be a useful information.
    <br>Thought in the current implementation the data is not decrypted, see json output
    of the transaction. The data itself might be difficult to understand at first glance.
    


### Todo's
This section contains a set of current and future todo's.
* Official WebRTC Integration of IPFS, when it is ready
<br>More information on https://github.com/libp2p/specs/pull/159
* Deactivate the temporary feature **Support Peer Discovery**, when IPFS finally can better handle
nodes behind NAT's (IPFS feature Relay, RelayHop, AutoNat etc.)
* Pubsub Messages sometimes get lost (or at least do not arrive to the final peer destination).
This makes the feature **Send Data** sometimes not very reliable
* Storing data on IOTA Tangle is sometimes very time expensive and in general IOTA is not yet
fully decentralized (Hope for a better version and better access to IOTA gateways)
* Sometimes connection are not valid, thought they appear to be online, they are actually offline
(Hope for a better IPFS core API, maybe IPFS eventbus seems to be a solution)
* In the current implementation when an **IPFS Lite** node sends data to another **IPFS Lite** node,
the download is triggered automatically. Even when the user is connected over a mobile connection.
That might not be wished, can be somehow expensive for the receiving node.
Better solution would be, that the user trigger the download manually when the connection is a 
mobile connection. A notification would also be nice in such circumstances.
* Encrypt the Pubsub messages between **IPFS Lite** nodes [low hanging fruit]
* General issues are: https://gitlab.com/remmer.wilts/threads-server/issues

### Dependencies 
- threads-iota (Wrapper implementation around a IOTA light node)
<br>Source : https://gitlab.com/remmer.wilts/threads-iri
- threads-ipfs (Wrapper implementation around a IPFS node)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)

