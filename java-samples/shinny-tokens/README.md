# Tokens in Next-Gen Corda

Unlike Corda 4, we don’t have an SDK for tokens in Next-Gen Corda; 
the token’s functionality is brought into the core C5 platform. 
We have also introduced a new Token Selection API, which enables a flow to claim 
tokens exclusively and provides a way to merge and return fungible tokens satisfying a given amount.

In this sample, I will show you how you can create a gold stablecoin, 
a commodity backed enterprise-grade and regulatory-friendly stablecoin 
using Next-Gen Corda.

## Tokens app
In this application, we will mint gold tokens and then transfer these tokens.

In this app you can:
1. Write a flow to Create a Gold Asset/State on Ledger. `MintGoldTokensFlow`
2. List out the gold entries you had. `ListGoldTokens`
4. Claim and transfer the tokens to a new member. `TransferGoldTokenFlow`

### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some of the
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload

### Running the tokens app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create Gold State
Pick a VNode identity, and get its short hash. (Let's pick Alice.).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "mint-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.MintGoldTokensFlow",
    "requestBody": {
        "symbol":"GOLD",
        "issuer":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "value":"20"
        }
}
```

After trigger the MintGoldTokensFlow flow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and clientrequestid to view the flow result

#### Step 2: List the gold state
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.ListGoldTokens",
    "requestBody": {}
}
```
After trigger the ListGoldTokens flow, again, we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` 
and check the result. 
As the screenshot shows, in the response body, we will see a list of the gold state we created.

#### Step 3: Transfer the gold token with `TransferGoldTokenFlow`
In this step, we will Alice will transfer some tokens from his vault to Charlie.
Goto `POST /flow/{holdingidentityshorthash}`, enter the identity short hash and request body. 
Use Alice's holdingidentityshorthash to fire this psot API.
```
{
    "clientRequestId": "transfer-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.TransferGoldTokenFlow",
    "requestBody": {
        "symbol":"GOLD",
        "issuer":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "newOwner":"CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
        "value": "5"
        }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

#### Step 4: Confirm the token balances of Alice and Charlie
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.ListGoldTokens",
    "requestBody": {}
}
```
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Charlie's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.ListGoldTokens",
    "requestBody": {}
}
```

And as for the result, you need to go to the Get API again and enter the short hash and client request ID.
Thus, we have concluded a full run through of the token app. 


# Additional Information

To read more about Token Selection API, you can visit the [docs](https://docs.r3.com/en/platform/corda/5.0-beta/developing/api/api-ledger-token-selection.html#tokens) and 
read [this](https://r3-cev.atlassian.net/wiki/spaces/DR/pages/4435017960/Shiny+tokens+in+Next-Gen+Corda) blog.