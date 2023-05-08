# CSDE

A csde provides:
A ready set up Cordapp Project which you can use as a starting point to develop your own prototypes.
A set of Gradle helper tasks which speed up and simplify the development and deployment process.
Debug configuration for debugging a local Corda cluster.
Using CSDE we will see how to deploy a local corda cluster with combined worker, how to build a cpk, cpb and cpi file,
how to install a cpi on the corda cluster, how to create a virtual node and register it with MGM.
And then we will run a flow and print the output.

# Prerequisite
You need the corda-cli installed on your system, and docker-postgres.

# Token Issuance Cordapp with Corda 5

We had built a token app to demo some functionalities of the next gen Corda platform.

In this app you can:
1. Issue a new token between from issuer to owner. `TokenIssueFlow`
2. List out the token entries you had. `ListTokenFlow`

### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the HTTP RPC at: https://localhost:8888/api/v1/swagger#. You can test out some of the
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `quickDeployCordapp` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload

### Running the Token app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Issue a token from Alice to Bob
Pick a VNode identity to initiate the Token Issuance, and get its short hash.
Let's pick Alice.

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "issue-token",
    "flowClassName": "com.r3.developers.csdetemplate.tokenflows.TokenIssueFlow",
    "requestData": {
        "amount":"100",
        "owner":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
```

After trigger the TokenIssueFlow flow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and clientrequestid to view the flow result

#### Step 2: List the Tokens

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list",
    "flowClassName": "com.r3.developers.csdetemplate.tokenflows.ListTokenFlow",
    "requestData": {}
}
```
After trigger the ListTokenFlow, again, we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and check the result. As the screenshot shows, in the response body,
we will see a list of token entries.
