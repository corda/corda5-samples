# Sanctionbody -- ReferenceState

This CorDapp demonstrates the use of reference states in a transaction and in the verification method of a contract.

## Concepts
This CorDapp allows two nodes to enter into an IOU agreement, but enforces that both parties belong to a list of sanctioned entities. This list of sanctioned entities is taken from a referenced SanctionedEntities state.

## Usage


### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the REST APIs at: https://localhost:8888/api/v1/swagger#. You can test out some
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `5-vNodeSetup` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload



### Running the app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie SanctionBody, Bob, Charlie and DodgyParty. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.


### Running the flows

Pick the SanctionBody VNode identity to issue the sanctions list
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(SanctionBody's hash) and request body:

    {
    "clientRequestId": "issue-sanction",
    "flowClassName": "com.r3.developers.samples.referencestate.workflows.IssueSanctionsListFlow",
    "requestBody": {
      }
    }

Now that the sanctions list has been made, Next, we want to issue an IOU. Pick Bob VNode identity to issue the IOU.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Bob's hash) and request body:

    {
      "clientRequestId": "issue-iou",
      "flowClassName": "com.r3.developers.samples.referencestate.workflows.IOUIssueFlow",
      "requestBody": {
      "iouValue": 100,
      "lenderName": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
      "sanctionAuthority": "CN=SanctionsBody, OU=Test Dept, O=R3, L=London, C=GB"
      }
    }

We've seen how to successfully send an IOU to a non-sanctioned party, so what if we want to send one to a sanctioned party? First we need to update the sanction list.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(SanctionBody's hash) and request body:

    {
      "clientRequestId": "update-sanction",
      "flowClassName": "com.r3.developers.samples.referencestate.workflows.UpdateSanctionListFlow",
      "requestBody": {
      "partyToSanction": "CN=DodgyParty, OU=Test Dept, O=R3, L=London, C=GB"
      }
    }


Now try an issue an IOU to DodgyParty. Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Bob's hash) and request body:

    {
      "clientRequestId": "issue-iou-1",
      "flowClassName": "com.r3.developers.samples.referencestate.workflows.IOUIssueFlow",
      "requestBody": {
      "iouValue": 100,
      "lenderName": "CN=DodgyParty, OU=Test Dept, O=R3, L=London, C=GB",
      "sanctionAuthority": "CN=SanctionsBody, OU=Test Dept, O=R3, L=London, C=GB"
      }
    }

The flow will error with the message: The borrower O=DodgyParty, L=Moscow, C=RU is a sanctioned entity'!

You could use the GetIOUFlow and GetSanctionListFlow to query the vault and check the current unconsumed states
issued on the ledgers. Go to `POST /flow/{holdingidentityshorthash}`, enter the vnode's identity short hash and request body:

To Get IOU List:

    {
       "clientRequestId": "get-iou",
       "flowClassName": "com.r3.developers.samples.referencestate.workflows.GetIOUFlow",
       "requestBody": {
       }
    }

To Get Sanction List:

    {
       "clientRequestId": "get-sanction",
       "flowClassName": "com.r3.developers.samples.referencestate.workflows.GetSanctionListFlow",
       "requestBody": {
       }
    }
