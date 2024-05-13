# Custom StateJSON and Query Cordapp -kotlin (Corda v5.2)

## This sample repository provides:

A sample CorDapp that demonstrate the functionalities of storing custom stateJSON into the vNode's database and the functionalities of querying 
specific states via the custom stateJSON. The functions are demo on the Chat app that are in our template app. 
We will be seeing both Alice and Dave sending a chat entry to Bob, and we will query only Alice's chat entry via the custom
query. 

## Prerequisite
1. Java 17
2. Corda-cli (v5.2), Download [here](https://github.com/corda/corda-runtime-os/releases/tag/release-5.2.0.0). You need to install Java 17 first.
3. Docker Desktop

## Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v5_2/swagger#. You can test out some of the
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload

## Flow Management Tool[Optional]
We had developed a simple GUI for you to interact with the cordapp. You can access the website by using https://localhost:5000 or https://127.0.0.1:5000. The Flow Management Tool will automatically connect with the CorDapp running locally from your Corda cluster. You can test the connection by click on the dropdown list at the Flow Initiator section. You should be able to see the vNodes of your started CorDapp. You can easily trigger and query a Corda flow. 



![image](https://github.com/corda/cordapp-template-kotlin/assets/51169685/88e6568e-49b4-46a8-b1e1-34140bcf03a9)


## Running the sample app
We will use the Chat app to demonstrate the functionalities of custom StateJSON and custom queries.

### Running the flows

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `listVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create Chat Entry from Alice
Pick a VNode identity to initiate the chat, and get its short hash. (Let's pick Alice. Dont pick Bob because Bob is the person who we will have the chat with).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.customStateJSONandQuery.workflows.CreateNewChatFlow",
    "requestBody": {
        "chatName":"Chat with Bob",
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "message": "Hello Bob"
        }
}
```

After trigger the create-chat flow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and clientrequestid to view the flow result

#### Step 2: View the Custom StateJson in Database
By using database tool such DBeaver or similar app, we connect to the postgre Database. Then, we locate to the Alice's vNode. We select 
`utxo_visible_transaction_output` table, and we should be able to see the transaction record of Alice. We can see one column is called `custom_representation`, and 
it will hold the custom StateJSON of the chat state that Alice created for Bob. 
![image](https://github.com/corda/corda5-samples/assets/51169685/f9b11845-013b-4a34-99f3-307a49c074cb)

For example,
```
{
   "net.corda.v5.ledger.utxo.ContractState":{
      "stateRef":"SHA-256D:3817854BD2DA35817360DFF00076DB9105DFF0F7B0824F4DF91F1BBA0E1039C4:0"
   },
   "com.r3.developers.customStateJSONandQuery.states.ChatState":{
      "Id":"d47b10d2-f3bd-4571-8ed2-3ac651492874",
      "chatName":"Chat with Bob",
      "messageContent":"Hello Bob",
      "messageContentFrom":"CN=Alice, OU=Test Dept, O=R3, L=London, C=GB"
   }
}
```

#### Step 3: Create Chat Entry from Dave
In order to demonstrate the custom query functionality, we will need to create another chat entry to Bob from a different person. We will choose Dave,
```
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.customStateJSONandQuery.workflows.CreateNewChatFlow",
    "requestBody": {
        "chatName":"Chat with Bob from Dave",
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "message": "Hello Bob from Dave"
        }
}
```
Notice, the `chatName` and `message` are annotated with "from Dave".

#### Step 4: Perform Custom Query at Bob
Now, we will perform the custom query, we will select Bob to trigger the flow with the following requestBody. Note that the `ListChatsByCustomQueryFlow` is hard coded
to query only Alice's chat entries.
```
{
    "clientRequestId": "customlist-1",
    "flowClassName": "com.r3.developers.customStateJSONandQuery.workflows.ListChatsByCustomQueryFlow",
    "requestBody": {}
}
```


#### Step 5: Perform Generic Query at Bob
As a contrast, we will perform a generic query at Bob to query all the chat entries that Bob receives. This will print out all the chats that Bob has, including the message from Dave.
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.customStateJSONandQuery.workflows.ListChatsFlow",
    "requestBody": {}
}
```

Thus, we have concluded a full run through of the Custom StateJson and Query sample app. 
