# C5-Obligation-CorDapp

This app is our signature CorDapp that we use to show the main functionalities of Corda, which are creating a digital asset, 
updating the digital asset, and transferring the digital asset. This app depicts a simple use 
case of money borrowing between two parties. In the app, the borrowed asset is called the `IOUState` (I-owe-you) 
and it is quantifiable. 

In this app you can:
1. Create a new IOUState with a counterparty. `IOUIssueFlow`
2. List out the IOU entries you had. `ListIOUFlow`
3. Settle(pau back) the IOUState `IOUSettleFlow`
4. Lender transfer the debt to a different person `IOUTransferFlow`

### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload



### Running the app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create IOUState between two parties
Pick a VNode identity to initiate the IOU creation, and get its short hash. (Let's pick Alice. Don't pick Bob because Bob is the person who alice will borrow from).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "createiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUIssueFlow",
    "requestBody": {
        "amount":"20",
        "lender":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
```

After trigger the create-IOUflow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and client request id to view the flow result

#### Step 2: List created IOU state
In order to continue the app logics, we would need the IOU ID. This step will bring out all the IOU entries this entity (Alice) has.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.ListIOUFlow",
    "requestBody": {}
}
```
After trigger the list-IOUs flow, again, we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and check the result. Let's record that id.


#### Step 3: Partially settle the IOU with `IOUSettleFlow`
In this step, we will partially settle the IOU with some amount.
Goto `POST /flow/{holdingidentityshorthash}`, enter the identity short hash and request body. Note that the settle action can only be initiated by the borrower of the IOU
```
{
    "clientRequestId": "settleiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUSettleFlow",
    "requestBody": {
        "amountSettle":"10",
        "iouID":" ** fill in id **"
    }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

#### Step 4: Lastly, the lender of the IOU can transfer the IOU to a different owner.
Note this transfer action can only be initiated by the lender of the IOU. We will have Bob transfer his IOU to Charlie. 
We will now take Bob's shorthash and enter the following request Body. 
```
{
    "clientRequestId": "transferiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUTransferFlow",
    "requestBody": {
        "newLender":"CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
        "iouID":" ** fill in id **"
        }
}
```
And as for the result, you need to go to the Get API again and enter the short hash and client request ID.

Thus, we have concluded a full run through of the obligation app. 


