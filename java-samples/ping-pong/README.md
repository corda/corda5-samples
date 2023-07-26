# Ping-Pong CorDapp
This CorDapp allows a node to ping any other node on the network that also has this CorDapp installed.
It demonstrates how to use Corda for messaging and passing data using a [flow](https://docs.r3.com/en/platform/corda/5.0-beta/developing/ledger/flows.html) without saving any states or using any contracts.


### Concepts
The `ping` utility is normally used to send a Send ICMP ECHO_REQUEST packet to network hosts. The idea being that the receiving host will echo the message back.
In this example the Ping flow will send the String "Ping" to a other member in the network.
The otherMember will correspondingly reply with "Pong".

## Flows
You'll notice in our code we call these two classes ping and pong, the flow that sends the `"ping"`, and the flow that returns with a `"pong"`.

Take a look at [Ping.java](./workflows/src/main/java/com/r3/developers/pingpong/workflows/Ping.java).
You'll notice that this flow does what we expect, which is to send an outbound ping, and expect to receive a pong. 
If we receive a pong, then our flow is successful.
And of course we see a similar behavior in [Pong.java](./workflows/src/main/java/com/r3/developers/pingpong/workflows/Pong.java).
We expect to receive data from a counterparty that contains a ping, when we receive it, we respond with a pong.

## Pre-Requisites
For development environment setup, please refer to: [Setup Guide](https://docs.r3.com/).


## Running the nodes
1. We will begin our test deployment with clicking the `startCorda`. 
   `./gradlew startCorda` run this from the Intellij terminal
   This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. 
   You can test out some functions to check connectivity.(GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, 
   the GET /cpi function call should now return the meta data of the cpi you just upload


### Running the app
In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at 
`GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short 
  hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

####  Pinging a node:
Pick a VNode identity to initiate the ping, and get its short hash. Let's pick Alice.

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
  "clientRequestId": "ping-1",
  "flowClassName": "com.r3.developers.pingpong.workflows.Ping",
  "requestBody": {
    "otherMember": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
```

Now hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short 
hash(Alice's hash) and clientrequestid to view the flow result

##Stop corda 
To stop the combined worker - run the task `stopCorda` from the terminal.
```
./gradlew stopCorda
```
