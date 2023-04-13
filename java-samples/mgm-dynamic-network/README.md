# MGM-Dynamic-Network

This demo CorDapp is to show the use of the MGM tool on setting up a dynamic Corda5 application network. This app will use the Corda 5 combined worker as the foundation. We will then use the MGM tool to deploy the dynamic Corda5 application network on it. All the instructions are written into 4 shell scripts, which you can simply run them.

## Before running the app
* You need to prepare a self-generate CA for create certificates for keys generated within Corda. This CA will be external to Corda.
* You would need to reset the path for both `WORK_DIR` and `RUNTIME_OS` variables in the script.

[R3 INTERAL NOTES]: If you are an R3 employee, you can use the mock ca tool in Corda-runtime-os. Get the `release/os/5.0-Beta2` branch of the Corda-runtime-os.


## Running the demo
Find the gradle task `startCorda` and wait for the combined worker to be fully started. Once the swagger page is loaded, run the shell script one by one, and follow the prompts.
```
.
├── Step1-mgm-deploy.sh
├── Step2-notary-onboard.sh
├── Step3-first-member-onboard.sh
└── Step4-more-member-onboard.sh
```

## Test your deployment
#### Step 1: Create Chat Entry
Pick a VNode identity to initiate the chat, and get its short hash. (Let's pick Alice. Don't pick Bob because Bob is the person who we will have the chat with).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.CreateNewChatFlow",
    "requestBody": {
        "chatName":"Chat with Bob",
        "otherMember":"THE-NODE-YOU-CREATED",
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

