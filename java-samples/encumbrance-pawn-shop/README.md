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

This is a basic sample which shows how you can use encumbrance in Corda. For this sample, we will have an Asset
created on Corda. We will request for a loan using the asset as a collateral. The Asset will would thus be locked
and cannot be transferred to another party till the loan has been repaid.

<p align="center">
  <img width="1000" alt="Encumbrance Flow" src="https://github.com/corda/corda5-samples/assets/52695915/0445431d-9aa8-4862-ba22-fc3b1e1c4cb8">
</p>

## Usage


### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload

## Running the app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Alice, Bob and Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.


### Create The Asset

Pick the Alice VNode's identity to create the Asset. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

    {
       "clientRequestId": "create-asset",
       "flowClassName": "com.r3.developers.samples.encumbrance.workflows.CreateAssetFlow",
       "requestBody": {
         "assetName": "My Asset"
       }
    }

### Request Loan

Now request for a Loan from Bob, pick Alice VNode's identity again. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

    {
      "clientRequestId": "request-loan",
      "flowClassName": "com.r3.developers.samples.encumbrance.workflows.RequestLoanFlow",
      "requestBody": {
         "lender": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
         "loanAmount": 1000,
         "collateral": "<asset-id>" // Check Viewing Data in the Vault Section on get this
      }
    }

### Viewing Data in the Vault

You could view the Loan and the Asset using the GetLoanFlow and GetAssetFlow respectively. The request body are as below:

      {
         "clientRequestId": "get-asset",
         "flowClassName": "com.r3.developers.samples.encumbrance.workflows.GetAssetFlow",
         "requestBody": {
         }
      }
Replace the ```flowClassName``` with ```com.r3.developers.samples.encumbrance.workflows.GetLoanFlow``` to view the available Loans in the vault. Don't forget to change the client-id as well.

Note that the Asset is encumbered. But Encumbrances should form a complete directed cycle,
otherwise one can spend the "encumbrance" (Loan) state, which would freeze the "encumbered" (Asset) state forever.
That's why Loan should also be dependent on Asset.

### Transfer Encumbered Asset (Should Fail)

Now try to transfer the Asset to Charlie, pick Alice VNode's identity. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

      {
         "clientRequestId": "transfer-asset",
         "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAssetFlow",
         "requestBody": {
            "assetId": "<asset-id>", // Check Viewing Data in the Vault Section on get this
            "buyer": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
         }
      }
This would result in an error, since the Asset is encumbered with the Loan and hence locked until the loan is repaid.


### Settle The Loan

Now to settle the Loan, pick Alice's VNode's identity. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

      {
         "clientRequestId": "settle-loan",
         "flowClassName": "com.r3.developers.samples.encumbrance.workflows.SettleLoanFlow",
         "requestBody": {
            "loanId": "<loan-id>"
         }
      }

### Transfer Asset
Once the Loan is settled, the asset is unlocked and can be transferred. To transfer the asset to Charlie, pick Alice's VNode's identity. Go to POST /flow/{holdingidentityshorthash}, enter the identity short hash(Alice's hash) and request body:

      {
      "clientRequestId": "transfer-asset-second-try",
      "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAssetFlow",
         "requestBody": {
            "assetId": "<asset-id>",
            "buyer": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
         }
      }
The transfer should not complete successfully.
