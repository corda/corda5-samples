# Negotiation Cordapp

This CorDapp shows how multi-party negotiation is handled on the Corda ledger, in the absence of an API for user
interaction.

## Concepts

A flow is provided that allows a node to propose a trade to a counterparty. The counterparty has two options:

* Accepting the proposal, converting the `ProposalState` into a `TradeState` with identical attributes
* Modifying the proposal, consuming the existing `ProposalState` and replacing it with a new `ProposalState` for a new
  amount

Only the recipient of the proposal has the ability to accept it or modify it. If the sender of the proposal tries to
accept or modify the proposal, this attempt will be rejected automatically at the flow level.

### Flows

We start with the proposal flow implemented in `ProposalFlow.java`.


The modification of the proposal is implemented in `ModificationFlow.java`.


In the `AcceptanceFlow.java`, we receive the modified ProposalState and it's converted into a TradeState.



### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload



### Running the app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create ProposalState between two parties
Pick a VNode identity to initiate the Proposal creation, and get its short hash. (Let's pick Alice.).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
  "clientRequestId": "createProposal1",
  "flowClassName": "com.r3.developers.samples.obligation.workflows.propose.ProposalFlowRequest",
  "requestBody": {
      "amount": 20,
      "counterParty":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
```
After trigger the create-ProposalFlow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and client request id to view the flow result


#### Step 2: List created Proposal state
In order to continue the app logics, we would need the Proposal ID. This step will bring out all the Proposal this entity (Alice) has.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.util.ListProposal",
    "requestBody": {}
}
```
After trigger the List Proposal, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and client request id to view the flow result 


#### Step 3: Modify the proposal 
In order to continue the app logics, we would need the Proposal ID. This step will bring out the Proposal entries this entity (Alice) has. Bob can edit the proposal if required by entering the new amount. 
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Bob hash) and request body:
```
{
  "clientRequestId": "ModifyFlow",
  "flowClassName": "com.r3.developers.samples.obligation.workflows.modify.CreateModifyRequestFlow",
  "requestBody": {
      "newAmount": 22,
      "proposalID": "<use the proposal id here>"
  }
}
```
After triggering the modify flow we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and check the result. Enter bob's hash id and the modify flow id which is "ModifyFlow" in the case above.


#### Step 4: Accept the new proposal from bob  `AcceptFlow`
In this step, alice will accept the new proposal of Bob.
Goto `POST /flow/{holdingidentityshorthash}`, enter the identity short hash (of Alice) and request body, we also need to provide the proposalId, which is same as the proposal ID used in the modifyFlow body. 
```
{
  "clientRequestId": "AcceptFlow",
  "flowClassName": "com.r3.developers.samples.obligation.workflows.modify.CreateModifyRequestFlow",
  "requestBody": {
      "proposalID": "<use the proposal id here>"
  }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

Thus, we have concluded a full run through of the Negotiation app.




