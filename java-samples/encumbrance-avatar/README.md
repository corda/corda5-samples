# Corda encumbrance sample

Corda supports the idea of "Linked States", using the TransactionState.encumbrance property. When building a transaction, a state x can
point to other state y by specifying the index of y's state in the transaction output index.
In this situation x is linked to y, i.e. x is dependent on y. x cannot be consumed unless you consume y.
Hence if you want to consume x, y should also be present in the input of this transaction.
Hence y's contract is also always run, when x is about to be consumed.
In this situation, x is the encumbered state, and y is the encumbrance.
At present, if you do not specify any encumbrance, it defaults to NULL.

There are many use cases which can use encumbrance like -
1. Cross chain Atomic Swaps
2. Layer 2 games like https://github.com/akichidis/lightning-chess etc.

## About this sample

This is a basic sample which shows how you can use encumbrance in Corda. For this sample, we will have an Avatar
created on Corda. We will transfer this Avatar from one party to the other within a specified time limit.
After this time window, the Avatar will be expired, and you cannot transfer it to anyone.

Avatar state is locked up by the Expiry state which suggests that the Avatar will expire after a certain time,
and cannot be transferred to anyone after that.

This sample can be extended further, where the Avatar can be represented as a NFT using Corda's Token SDK, and
can be traded and purchased by a buyer on the exchange. The tokens can be locked up using an encumbrance before
performing the DVP for the NFT against the tokens.

## Usage


### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload

### Running the app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Alice and Bob. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.


Pick the Alice VNode's identity to create the Avatar. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

    {
      "clientRequestId": "create-avatar",
      "flowClassName": "com.r3.developers.samples.encumbrance.workflows.CreateAvatarFlow",
      "requestBody": {
         "avatarId": "AVATAR-1321",
         "expiryAfterMinutes": 10
      }
    }

Now sell the Avatar to Bob, pick Alice VNode's identity again to sell the Avatar. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

    {
      "clientRequestId": "transfer-avatar",
      "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAvatarFlow",
      "requestBody": {
         "avatarId": "AVATAR-1321",
         "buyer": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
      }
    }

To confirm Bob now owns the Avatar. Pick Bob's VNode's identity. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Bob's hash) and request body:

      {
         "clientRequestId": "get-avatar",
         "flowClassName": "com.r3.developers.samples.encumbrance.workflows.GetAvatarFlow",
         "requestBody": {
         }
      }

Note that the Avatar is encumbered. But Encumbrances should form a complete directed cycle, 
otherwise one can spend the "encumbrance" (Expiry) state, which would freeze the "encumbered" (Avatar) state forever. 
That's why Expiry should also be dependent on Avatar.