# CSDE-cordapp-template-kotlin


To help make the process of prototyping CorDapps on Corda 5 beta 1.1 release more straight forward we have developed the Cordapp Standard Development Environment (CSDE).

The CSDE is obtained by cloning this CSDE-Cordapp-Template-Kotlin to your local machine. The CSDE provides:

- A pre-setup Cordapp Project which you can use as a starting point to develop your own prototypes.

- A base Gradle configuration which brings in the dependencies you need to write and test a Corda 5 Cordapp.

- A set of Gradle helper tasks which speed up and simplify the development and deployment process.

- Debug configuration for debugging a local Corda cluster.

- The MyFirstFlow code which forms the basis of this getting started documentation, this is located in package com.r3.developers.csdetemplate.flowexample

- A UTXO example in package com.r3.developers.csdetemplate.utxoexample packages

- Ability to configure the Members of the Local Corda Network.

Note, the CSDE is experimental, we may or may not release it as part of Corda 5.0, in part based on developer feedback using it.

To find out how to use the CSDE please refer to the getting started section in the Corda 5 Beta 2 documentation at https://docs.r3.com/



## Chat app
We have built a simple one to one chat app to demo some functionalities of the next gen Corda platform.

In this app you can:
1. Create a new chat with a counterparty. `CreateNewChatFlow`
2. List out the chat entries you had. `ListChatsFlow`
3. Individually query out the history of one chat entry. `GetChatFlowArgs`
4. Continue chatting within the chat entry with the counterparty. `UpdateChatFlow`

### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some of the
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload



### Running the chat app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create Chat Entry
Pick a VNode identity to initiate the chat, and get its short hash. (Let's pick Alice. Dont pick Bob because Bob is the person who we will have the chat with).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.CreateNewChatFlow",
    "requestBody": {
        "chatName":"Chat with Bob",
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "message": "Hello Bob"
        }
}
```

After trigger the create-chat flow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and clientrequestid to view the flow result

#### Step 2: List the chat
In order to continue the chat, we would need the chat ID. This step will bring out all the chat entries this entity (Alice) has.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.ListChatsFlow",
    "requestBody": {}
}
```
After trigger the list-chats flow, again, we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and check the result. As the screenshot shows, in the response body,
we will see a list of chat entries, but it currently only has one entry. And we can see the id of the chat entry. Let's record that id.


#### Step 3: Continue the chat with `UpdateChatFlow`
In this step, we will continue the chat between Alice and Bob.
Goto `POST /flow/{holdingidentityshorthash}`, enter the identity short hash and request body. Note that here we can have either Alice or Bob's short hash. If you enter Alice's hash,
this message will be recorded as a message from Alice, vice versa. And the id field is the chat entry id we got from the previous step.
```
{
    "clientRequestId": "update-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.UpdateChatFlow",
    "requestBody": {
        "id":" ** fill in id **",
        "message": "How are you today?"
        }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

#### Step 4: See the whole chat history of one chat entry
After a few back and forth of the messaging, you can view entire chat history by calling GetChatFlow.

```
{
    "clientRequestId": "get-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.GetChatFlow",
    "requestBody": {
        "id":" ** fill in id **",
        "numberOfRecords":"4"
    }
}
```
And as for the result, you need to go to the Get API again and enter the short hash and client request ID.

Thus, we have concluded a full run through of the chat app. 
