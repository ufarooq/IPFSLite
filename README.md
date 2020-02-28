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
    - Sending Files 
    <br>An **IPFS Lite** node can send notifications to another **IPFS Lite** which contains
    basically the identifier of the files (CIDs). The receiving **IPFS Lite** node checks
    from time to time, whether is has received such notifications.
    (Note: In the "Files" view of the application you can drag-down the view, and the 
    program will check whether new notifications have arrived)
    
    


### **IPFS Lite** versus **IPFS**
This section describes the differences between an **IPFS Lite** node and an regular **IPFS** node.
<br>
Despite the enhancements of the **IPFS Lite** application (like IOTA)
there are some small differences between such node kinds.
<br>
In general an **IPFS Lite** has the same functionality like an regular node.
There are some small differences which are described here. The reasons are outlined in brackets.
- **No** Gateway Support
<br>An IPFS gateway is not supported [Performance,Security,Android 10]
- **No** CLI and HTTP API Support
<br>No public API is supported, the application itself based on the internal IPFS Core API [Android 10]
- **No** WebUI Support
<br>The WebUI feature is not supported [Performance,Security,Android 10]
- Limited Node Configuration
<br>In the current **IPFS Lite** version under "Settings" the node itself can be configured.
<br>Only a subset of possibilities of a node configuration (**config** file) can be done now. 
<br>The reason is that some of the options might be not valid for an **IPFS Lite** node, because
in the future such feature will not be supported anymore. 


### Enhancements
This section describes the enhancements of an **IPFS Lite** node.
Precondition for using this enhancements is, that all participants using this application
The enhancements are:

- Integration of IOTA to support
a kind of file sharing between nodes (**Send Data** Feature). The file sharing itself is realized 
through a notification, where a node A sends a notification to node B.
- The **Inbox** contains all notifications from other nodes which were sent to your peer
 (Data is encrypted on IOTA Tangle)
  A notification contains the information what content **CID** should be downloaded from whom
  **PID**

 
Example Notification Content:
```
"pid": "WlI6VDdzIARqhk4kDYXMjHAFOGnLEcloFNjvh224L/Qdm3tZ7yPjJlzFRmJMl0rBtjn8p629+rV5 sw7pLU5UEJBLJcOpLDSOw9Yn3wuyJw/bkkRHUQGeksWVXxLkU2zD9W7r7rdEng0mjGvJZ44K5Ufj NHEXaVJKD8VU9h4+blEWy2SQlOVw/WSL3u+wqupruLZ26uGUIrstEij7VxAhlyq8BfM9GwwdsbmW DOCLK1g8Ew+r/KI+vFCgo5KI4bCEc9mnv7UqOdEyPWaiF6c1E4hN7hsQxIThwH6rt6zfIYEW/ubT wApwcBN2djiUx+2lUTUZY0NvHpbzgOOrkcyxQg== "
"cid": "pUxf4KXU9Bbt/PGK/GqIylrehiyfpUNIMKB1vN0QMvw708jMoPvGhaEbylONZ/hxqNjtrpVm8Sok FeFGKl5S0EeexmxQ3j/r3dkQDxbRakfm6waRXiC1QM+HcHIUsOKbqEXCYQHMb5gowSPQjVFcBFZA pLsjGZ/0RXwZ5xd8Zf87hqtsj7gsTlsRmujZAmDobtTmgI8b2P+7K597oP37v2VbY1EaXp3QMzmO TvYzdaI48/hRBGEZrBKuBbFPTYdJFgHCJix1MBo8xe9qYaW+sRKERCb2xvA4ynhJGPH+OKMM95LK cCpdr57yBHs6fIS51RERqTN2M49IV3JDVPekWA== "
```
<br>The "pid" is the Peer ID (PID) encrypted.
<br>The "cid" is the Content ID (CID) encrypted.
<br>
<br>The **IPFS Lite** node can now download the CID from the given PID, when the node is reachable
<br>**Note:** The data is encrypted with the public key of the **IPFS Lite** node. Only the owner
of the **IPFS Lite** node can with its private key decrypt the real CID and PID value.
<br>**Important:** The content of the CID in the notification is **not** the real CID object, it
is more a json object which contains the information the the real CIDs (in case of multiple files)
<br>Example peer A sends two files "content.txt" and "cat.jpg" to peer B, the content of the
CID object in the notification looks like :
```
[{"cid":"QmZ8211AToxbmoy8SoYtfNu8dUMUPQQdMgY2WFpLdfv5hy","filename":"cat.jpg","mimeType":"image/jpeg","size":"140409"},
{"cid":"QmZ7k9Ue4PdpS2CLrpbeDswBeRBBST8r542DCeUDLHoGPp","filename":"content.txt","mimeType":"text/plain","size":"607"}]


```



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
    - Personal Data
    <br>No personal data are required to run the application.

    
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
- Download Timeout
<br>The download timeout defines the timeout of the following operations
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
or by manually drag-down the view in the "Files" view of the application.
- Random Swarm Port
<br>When "random swarm port" is deactivated, the swarm port port will be default port 4001.
- Automatic Download
<br>When "automatic download" is deactivated, the user has to manually trigger the download of
notifications. This option makes sense to deactivate when user has a mobile connection which
might trigger extra costs.

**Important:**
When the options **Support Sending Notifications** and 
 **Support Receiving Notifications**
are turned off, the **IPFS Lite** application behaves more like a regular IPFS node.
Switching off might have a positive effect on the overall energy consumption. 
Downside is that the **Send Data** feature will be disabled.




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
    (**Support Sending Notifications** and **Support Receiving Notifications**) 
     and increase the connection and download timeout in the settings

* Running the app in a "server" mode   
Assumption is here, that you have an "old" Android phone, which you like to use as an IPFS node.
In this use case scenario, some optimizations can be made.
    - Plug-in the device for charging and always run the application (**Start Daemon**) 
    - When you like to communicate with other **IPFS Lite** notes, activate the options
    (**Support Sending Notifications**,**Support Receiving Notifications**) otherwise 
    deactivate them
    - When you have a public static IP address, and be nice to other peers, 
    activate **Enable Auto NAT Service** and **Enable Relay Hop**. The option **Enable Auto Relay** can
    be deactivated in this case.
    
* Running the app to communicate with other "IPFS Lite" nodes
Assumption is here, that you would like to communicate with other "IPFS Lite" nodes, to share
data for instance.
In this use case scenario, you have a "friend" peer to share data with.
    - Activate the options (**Support Sending Notifications** and **Support Receiving Notifications**)
    - Always run the application (you never know when data is send or a communication should take place)
    - When you have a mobile connection, better do deactivate the **Automatic Download** in the settings 
    and trigger it manually
    - Activate **Enable Auto Relay** in the settings, in case you behind a NAT (very probabilistic, in
    case of mobile connection and also WiFI connection)

### Todo's
This section contains a set of current and future todo's.
* Storing data on IOTA Tangle is sometimes very time expensive and in general IOTA is not yet
fully decentralized (Hope for a better version and better access to IOTA gateways)
* Sometimes connection are not valid, thought they appear to be online, they are actually offline
(Hope for a better IPFS core API, maybe IPFS an event-bus seems to be a solution)
* In the current implementation when an **IPFS Lite** node sends data to another **IPFS Lite** node,
the download is triggered automatically. Even when the user is connected over a mobile connection.
That might not be wished, can be somehow expensive for the receiving node.
Better solution would be, that the user trigger the download manually when the connection is a 
mobile connection. A notification would also be nice in such circumstances.
First step is already implemented. Under settings it is possible to deactivate the automatic
download of content.
* Energy consumption. Improve the energy consumption, so that the app can run at all time
  (Ideas connection manager introduce idle mode, deactivating IPFS feature
  like Mount, etc.)
* General issues are: https://gitlab.com/remmer.wilts/threads-server/issues

### Dependencies 
- threads-iota (Wrapper implementation around a IOTA light node)
<br>Source : https://gitlab.com/remmer.wilts/threads-iri
- threads-ipfs (Wrapper implementation around a IPFS node)
<br>Source : https://gitlab.com/remmer.wilts/threads-ipfs
- MinSdkVersion 24 (Android 7.0)

