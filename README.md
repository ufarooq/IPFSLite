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
It provides a modern UI to realize such use cases and has in addition
the possibility to customize the configuration of the node.
The basic characteristics of the app are decentralized, respect of personal data,
open source, free of charge, transparent, free of advertising and legally impeccable.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/threads.server/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=threads.server)

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


### **IPFS Lite** versus **IPFS**
This section describes the differences between an **IPFS Lite** node and an regular **IPFS** node.
<br>
Despite the enhancements of the **IPFS Lite** application (like IOTA)
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
<br>But the main reason for the lack of configuration is, that such options are not yet implemented. 


### Enhancements
This section describes the enhancements of an **IPFS Lite** node.
Precondition for using this enhancements is, that all participants using this application
The enhancements are:

- Integration of IOTA to support faster node detection (**Support Peer Discovery**) and to support 
a kind of file sharing between nodes (**Send Data** Feature). The file sharing itself is realized 
through a notification, where a node A sends a notification to node B.
- The **Inbox** contains all notifications from other nodes which were sent to your peer
 (Data is encrypted on IOTA Tangle)
  A notification contains the information what content **CID** should be downloaded from whom
  **PID**
- The **Outbox** contains information of the **Peer Discovery** feature. This information can be
  evaluated by other nodes for faster node access.

 
Example Notification Content:
```
"pid": "WlI6VDdzIARqhk4kDYXMjHAFOGnLEcloFNjvh224L/Qdm3tZ7yPjJlzFRmJMl0rBtjn8p629+rV5 sw7pLU5UEJBLJcOpLDSOw9Yn3wuyJw/bkkRHUQGeksWVXxLkU2zD9W7r7rdEng0mjGvJZ44K5Ufj NHEXaVJKD8VU9h4+blEWy2SQlOVw/WSL3u+wqupruLZ26uGUIrstEij7VxAhlyq8BfM9GwwdsbmW DOCLK1g8Ew+r/KI+vFCgo5KI4bCEc9mnv7UqOdEyPWaiF6c1E4hN7hsQxIThwH6rt6zfIYEW/ubT wApwcBN2djiUx+2lUTUZY0NvHpbzgOOrkcyxQg== "
"cid": "pUxf4KXU9Bbt/PGK/GqIylrehiyfpUNIMKB1vN0QMvw708jMoPvGhaEbylONZ/hxqNjtrpVm8Sok FeFGKl5S0EeexmxQ3j/r3dkQDxbRakfm6waRXiC1QM+HcHIUsOKbqEXCYQHMb5gowSPQjVFcBFZA pLsjGZ/0RXwZ5xd8Zf87hqtsj7gsTlsRmujZAmDobtTmgI8b2P+7K597oP37v2VbY1EaXp3QMzmO TvYzdaI48/hRBGEZrBKuBbFPTYdJFgHCJix1MBo8xe9qYaW+sRKERCb2xvA4ynhJGPH+OKMM95LK cCpdr57yBHs6fIS51RERqTN2M49IV3JDVPekWA== "
"hash": "N9FTXHNQNMWGVWUMLAXODO9CMHVBLMKHQGHT9NHENULDPF9HMTQUNQUJMCJHMFMXF9BTAJ9LFVNZ99999"
```
<br>The "pid" is the Peer ID (PID) encrypted.
<br>The "cid" is the Content ID (CID) encrypted.
<br>The "hash" is the hash ID of the latest peer discovery transaction (not encrypted).
<br>The **IPFS Lite** node can now download the CID from the given PID.
<br>**Note:** The data is encrypted with the public key of the **IPFS Lite** node. Only the owner
of the **IPFS Lite** node can with its private key decrypt the real CID and PID value.
<br>**Important:** The content of the CID in the notification is **not** the real CID object, it
is more a json object which contains the information the the real CIDs (in case of multiple files)
<br>Example peer A sends two files "content.txt" and "cat.jpg" to peer B, the content of the
CID object in the notification looks like :
```
[{"cid":"QmZ8211AToxbmoy8SoYtfNu8dUMUPQQdMgY2WFpLdfv5hy","filename":"cat.jpg","image":"QmRMGkpvXqvjinVBbJiLsDHb6uq68Kyvq4WMAraaFE4CcW","mimeType":"image/jpeg","size":"140409"},
{"cid":"QmZ7k9Ue4PdpS2CLrpbeDswBeRBBST8r542DCeUDLHoGPp","filename":"content.txt","mimeType":"text/plain","size":"607"}]


Note: the "image" attribute is just a CID object of the preview image of the "cat.jpg"
```



- The **Outbox** contains all peer discovery information of an **IPFS Lite** node. The data
is not yet encrypted on the Tangle. There is no need so far, because no "important" data
is stored in such a transaction. The data contains the alias name of the **IPFS Lite** node
(the brand name of the phone) and network access data (usually connected relay nodes) . 


<br>Example :
```
"peers": 
    "{
        "QmdGQoGuK3pao6bRDqGSDvux5SFHa4kC2XNFfHFcvcbydY":"/ip4/139.178.69.3/udp/4001/quic",
        "QmRdjvsyoNjA2ZfAQBtQ7A2m5NmtSXLgxE55Brn1AUjZ1v":"/ip4/147.75.106.163/udp/4001/quic"
    }"
"adds": 
    "{
        "alias":"Xiaomi Mi A2"  
     }"
```

The "peers" section are relays peers which were automatically detected.
Each of the peers have an address and a Peer ID (PID).
The "adds" section contains the alias of your **IPFS Lite** node.


### Features
This section contains the description of the main features of the **IPFS Lite** application.
- Share Content
<br>The **Send Data** feature is the main feature of this application.
<br>A regular IPFS node support does **not** support a share content feature in a way that
a node "A" tells another node "B", that it can download data from node "A".
To support this feature in an **IPFS Lite** node, some enhancements has to be made.
    - IOTA Integration
    <br>To support the sharing feature, notifications are introduced. This notifications are stored
    on the IOTA Tangle. A notification contains the data, what to download **CID** and from whom **PID**.
    <br>It might be useful to consider a IOTA Tangle as a public database.
    <br>Each **IPFS Lite** node has its own **Inbox** where the notifications are stored.
    <br>To have faster access to the **PID** node, which has the content data, a peer discovery feature
    has be introduced.
    <br>The peer discovery feature stores information about an IPFS node on the IOTA Tangle. This
    information are basically gateway and relay nodes to have a faster access to the peer (also
    an alias name of the peer is stored).
    <br>The information are stored in an **Outbox** which can be read by any other peer.
    - Peers Whitelist 
    <br>Each **IPFS Lite** node has a list of peers which are allowed to send data.
    <br>Such whitelisted peers are in the application section "Peers" and which are not blocked
    by the user manually.
- Transparency
<br>The **IPFS Lite** application itself should be transparent as possible for the user.
<br>To reach this goal several aspects are handled
    -  Open Source and Free
    <br>The **IPFS Lite** application is a open source project and also all its dependencies are open source.
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
    - Personal Data
    <br>No personal data are required to run the application. 
    <br>The name of the peer is simply the brand name of the phone.

    
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
    <br>When the timeout is reached and no connection is made to the peer, the process will be aborted
    - Find Content
    <br>When the timeout is reached and the content is not yet found, the process will be aborted
- Support Sending Notifications
<br>When "sending notifications" is turned off, no notifications will be send to the other peer.
The feature **Send Data** is turned off in this case.
- Support Receiving Notifications
<br>When "receiving notifications" is turned off, no notifications will be read from the
IOTA Tangle database. The **Inbox** might be still filled by other peers, but **IPFS Lite**
does not evaluate such information.
<br>**Note:** The notifications are readout by the application at the **start** of the application
and also every **30 sec** while the IPFS Lite daemon service is running ("IPFS is running forever")
- Support Peer Discovery
<br>When "peer discovery" is turned off, no peer information will be stored or read out from
IOTA Tangle database.  The **Outbox** will be not filled anymore and other peers can not read
out the information for faster peer access.
- Random Swarm Port
<br>When "random swarm port" is deactivated, the swarm port port will be default port 4001.
- Automatic Download
<br>When "automatic download" is deactivated, the user has to manually trigger the download of
notifications. This option makes sense to deactivate when user has a mobile connection which
might trigger extra costs.

**Important:**
When the three options **Support Sending Notifications**,
 **Support Receiving Notifications** and **Support Peer Discovery**
are turned off, the **IPFS Lite** application behaves more like a regular IPFS node.
Switching off might have a positive effect on the overall energy consumption. 
Downside is that the **Send Data** feature is dprobalisticeactivated and the peer discovery 
might not be working for peers behind NATs.
For sure is, when you just connecting to nodes which have static public IP addresses
and your **IPFS Lite** node also have this properties and 
additionally you and your connecting peers are all the time online, it definitely makes
sense to deactivate the options.




### Issues
This section contains a set of general issues handling with IPFS.
- Can not connect to "my" node (Assume that "my" node is a **regular** IPFS node)
<br> This kind of issue can have lots of reasons (here only some ideas)
    - Validate that your node has a static public IP address
    <br>Check that you can connect with another regular IPFS node to "your" node
    <br>**Important:** "your" node should not be behind a router (NAT). But when "your" node
    is behind a router, your need a "third" node which behaves like a "relay" node. Both
    "your" node and also your **IPFS Lite** node should be connected to the "relay" node.
    Then a successful communication can take place.
    - IPFS Lite has a Timeout Setting under "Navigation/Settings", increase the timeout
- Can not connect to "my" node (Assume that "my" node is also a **IPFS Lite** node)
<br> This kind of issue should in theory not happen, when both  **IPFS Lite** nodes are online and
the options **Support Sending Notifications**, **Support Receiving Notifications** and 
**Support Peer Discovery** in the settings are activated on both machines.
    - Verify that on the "Console" no errors are printed
    - Verify that the "Navigation/Outbox" has peer access information 
    <br>Date of the transaction might be a useful information.
    <br>Thought in the current implementation the data is not decrypted, see json output
    of the transaction. The data itself might be difficult to understand at first glance.
    

### Usage
This section describes how to use the app in what circumstances and what settings fits best to
this approach.
* Running the app in a "pure" client mode
Assumption is here, that you have a "real" IPFS node and just want to download content from your
IPFS node. This is probably the most used scenario. (You have only the option via QR code or edit
the CID object directly)
In this use case scenario, some optimizations can be made. 
    - If your "real" IPFS node is behind a NAT (e.g. router at home), make sure that MDNS setting
    is enable (should be enabled by default)
    - When you connecting with your **IPFS Lite** node to your "real" IPFS node from outside, you
    probably have to do a kind of port forwarding to your "real" IPFS node in case you have
    not a public IP address.
    - Other optimizations: Deactivate all IOTA Tangle integration (not valid with a "real" IPFS node)
    (**Support Sending Notifications**, **Support Receiving Notifications** and 
     **Support Peer Discovery**), maybe increase the connection timeout for downloading content

* Running the app in a "server" mode   
Assumption is here, that you have an "old" Android phone, which you like to use as an IPFS service.
In this use case scenario, some optimizations can be made.
    - Plug-in the device for charging and always run the application (Obvious) 
    - When you like to communicate with other **IPFS Lite** notes, activate all three options
    (**Support Sending Notifications**, **Support Receiving Notifications** and 
     **Support Peer Discovery**) otherwise deactivate the options
    - When you have a public static IP address, and be nice to other peers, 
    activate **Enable Auto NAT Service** and **Enable Relay Hop**. The option **Enable Auto Relay** can
    be deativated in this case.
    
* Running the app to communicate with other "IPFS Lite" nodes
Assumption is here, that you would like to communicate with other "IPFS Lite" nodes, to share
data for instance.
In this use case scenario, you have a "friend" peer to share data with.
    - Activate all three options
    (**Support Sending Notifications**, **Support Receiving Notifications** and **Support Peer Discovery**)
    - Always run the application (you never know when data is send or a communication should take place)
    - When you have a mobile connection, better do deactivate the **Automatic Download** in the settings 
    and trigger it manually
    - Activate **Enable Auto Relay* in the settings, in case you behind a NAT (very probabilistic, in
    case of mobile connection and also WiFI connection)

### Todo's
This section contains a set of current and future todo's.
* Deactivate the temporary feature **Support Peer Discovery**, when IPFS finally can better handle
nodes behind NAT's (IPFS feature Relay, RelayHop, AutoNat etc.)
* Storing data on IOTA Tangle is sometimes very time expensive and in general IOTA is not yet
fully decentralized (Hope for a better version and better access to IOTA gateways)
* Sometimes connection are not valid, thought they appear to be online, they are actually offline
(Hope for a better IPFS core API, maybe IPFS eventbus seems to be a solution)
* In the current implementation when an **IPFS Lite** node sends data to another **IPFS Lite** node,
the download is triggered automatically. Even when the user is connected over a mobile connection.
That might not be wished, can be somehow expensive for the receiving node.
Better solution would be, that the user trigger the download manually when the connection is a 
mobile connection. A notification would also be nice in such circumstances.
First step is already implemented. Under settings it is possible to deactivate the automatic
download of content.
* Energy consumption. Improve the energy consumption, so that the app can run at all time
  (Ideas deactivate pubsub, connection manager introduce idle mode, deactivating IPFS feature
  like Mount, etc.)
* General issues are: https://gitlab.com/remmer.wilts/threads-server/issues

### Dependencies 
- threads-iota (Wrapper implementation around a IOTA light node)
<br>Source : https://gitlab.com/remmer.wilts/threads-iri
- threads-ipfs (Wrapper implementation around a IPFS node)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)

