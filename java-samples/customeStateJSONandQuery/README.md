# cordapp-template-java (Corda v5.2)


## This template repository provides:

- A pre-setup Cordapp Project which you can use as a starting point to develop your own prototypes.

- A base Gradle configuration which brings in the dependencies you need to write and test a Corda 5 Cordapp.

- A set of Gradle helper tasks, provided by the [Corda runtime gradle plugin](https://github.com/corda/corda-runtime-os/tree/release/os/5.2/tools/corda-runtime-gradle-plugin#readme), which speed up and simplify the development and deployment process.

- Debug configuration for debugging a local Corda cluster.

- The MyFirstFlow code which forms the basis of this getting started documentation, this is located in package com.r3.developers.cordapptemplate.flowexample

- A UTXO example in package com.r3.developers.cordapptemplate.customeStateJSONandQuery packages

- Ability to configure the Members of the Local Corda Network.

To find out how to use the template, please refer to the *CorDapp Template* subsection within the *Developing Applications* section in the latest Corda 5 documentation at https://docs.r3.com/

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


## Running the Chat app
We have built a simple one to one chat app to demo some functionalities of the next gen Corda platform.

In this app you can:
1. Create a new chat with a counterparty. `CreateNewChatFlow`
2. List out the chat entries you had. `ListChatsFlow`
3. Individually query out the history of one chat entry. `GetChatFlowArgs`
4. Continue chatting within the chat entry with the counterparty. `UpdateChatFlow`




### Running the chat app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `listVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create Chat Entry
Pick a VNode identity to initiate the chat, and get its short hash. (Let's pick Alice. Dont pick Bob because Bob is the person who we will have the chat with).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.cordapptemplate.customeStateJSONandQuery.workflows.CreateNewChatFlow",
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
    "flowClassName": "com.r3.developers.cordapptemplate.customeStateJSONandQuery.workflows.ListChatsFlow",
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
    "flowClassName": "com.r3.developers.cordapptemplate.customeStateJSONandQuery.workflows.UpdateChatFlow",
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
    "flowClassName": "com.r3.developers.cordapptemplate.customeStateJSONandQuery.workflows.GetChatFlow",
    "requestBody": {
        "id":" ** fill in id **",
        "numberOfRecords":"4"
    }
}
```
And as for the result, you need to go to the Get API again and enter the short hash and client request ID.

Thus, we have concluded a full run through of the chat app. 

## Saving a run as JSON

#### Description
The ChatJsonFactory is a custom JSON factory designed to facilitate the serialization and deserialization of ChatState objects to and from JSON format within a Corda application.

#### Usage
This factory is utilized within Corda applications to convert ChatState objects to JSON format after each use. 
It is integrated into the Corda framework, ensuring seamless interoperability with other components of the application.

#### Functionality
getStateType(): Specifies the type of state the factory is responsible for, in this case, ChatState.
create(ChatState state, JsonMarshallingService jsonMarshallingService): Generates a JSON representation of a ChatState object by constructing a map and serializing it to JSON format using the provided JsonMarshallingService constants

ID: Key name for the identifier of the chat.

CHATNAME: Key name for the name of the chat.

MESSAGE: Key name for the content of the message.

MESSAGEFROM: Key name for the sender of the message.

#### Integration
The app is integrated automatically by corda when implementing ContractStateVaultJsonFactory for the class. 
It will run whenever a flow runs for the chatState

## Running the query 

#### Description
The CustomChatQuery class provides custom named queries for querying Vault states related to a chat application within a Corda application. 
These queries are designed to facilitate specific data retrieval operations from the Corda vault which were stored 
using the JSON function previously mentioned.

#### Usage
This component is integrated into Corda applications to execute custom named queries for retrieving chat-related states from the Corda vault.
It enables developers to perform targeted data retrieval operations efficiently.

#### Functionality
create(VaultNamedQueryBuilderFactory vaultNamedQueryBuilderFactory): Defines custom named queries for querying the Corda vault.
GET_ALL_MSG: Retrieves all messages from the vault.
GET_MSG_FROM: Retrieves messages from a specific sender.

### Integration
#### Process
The integration process began with the instantiation of the ListChatByCustomQueryFlow class within the application codebase. 
This class provides the necessary functionality to execute custom named queries against the Corda vault from the saved JSONs.

Subsequently, the custom query GET_MSG_FROM was executed using the query method provided by VaultService. 
This involved setting parameters for the query, such as specifying the name of the sender whose messages were to be retrieved in this 
application it is hard coded to alice. 
Additionally, optional parameters like timestamp limits and result limits were configured as per the application's requirements.

Once the query execution was completed, the results were processed to extract the relevant ChatState objects. 
This step involved mapping the query results to extract the desired states from the Corda vault.

Following the retrieval of ChatState objects, they were converted into a human-readable format or DTOs suitable for presentation purposes. 
This ensured that the retrieved chat messages were formatted in a manner understandable by users or other components of the application.

Finally, the results were serialized to JSON format using a JsonMarshallingService.
This step prepared the results for transmission as a response,
enabling other components or external systems to consume the data in a standardized format.

#### Calling a query

After creating the query it can be run using the following REST call using Bob's id:

```
{
    "clientRequestId": "customlist-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.ListChatsByCustomQueryFlow",
    "requestBody": {}
}
```

Which will display all messages sent by just Alice.
So even if Bob received messages from someone else it will only display the messages sent by Alice
