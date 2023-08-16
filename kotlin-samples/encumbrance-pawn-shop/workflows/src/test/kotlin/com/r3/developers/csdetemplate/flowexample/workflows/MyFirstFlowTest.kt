package com.r3.developers.csdetemplate.flowexample.workflows

//import com.r3.developers.csdetemplate.flowexample.workflows.MyFirstFlow
//import com.r3.developers.csdetemplate.flowexample.workflows.MyFirstFlowResponder
//import com.r3.developers.csdetemplate.flowexample.workflows.MyFirstFlowStartArgs
//import net.corda.simulator.RequestData
//import net.corda.simulator.Simulator
//import net.corda.v5.base.types.MemberX500Name
//import org.junit.jupiter.api.Test

// Note: this simulator test has been commented out pending the merging of the UTXO code into the Gecko Branch.



//class MyFirstFlowTest {
//
//    // Names picked to match the corda network in config/static-network-config.json
//    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
//    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")
//
//    @Test
//    fun `test that MyFirstFLow returns correct message`() {
//
//        // Instantiate an instance of the Simulator
//        val simulator = Simulator()
//
//        // Create Alice and Bob's virtual nodes, including the Class's of the flows which will be registered on each node.
//        // We don't assign Bob's virtual node to a val because we don't need it for this particular test.
//        val aliceVN = simulator.createVirtualNode(aliceX500, MyFirstFlow::class.java)
//        simulator.createVirtualNode(bobX500, MyFirstFlowResponder::class.java)
//
//        // Create an instance of the MyFirstFlowStartArgs which contains the request arguments for starting the flow
//        val myFirstFlowStartArgs = MyFirstFlowStartArgs(bobX500)
//
//        // Create a requestData object
//        val requestData = RequestData.create(
//            "request no 1", // A unique reference for the instance of the flow request
//            MyFirstFlow::class.java, // The name of the flow class which is to be started
//            myFirstFlowStartArgs // The object which contains the start arguments of the flow
//        )
//
//        // Call the Flow on Alice's virtual node and capture the response from the flow
//        val flowResponse = aliceVN.callFlow(requestData)
//
//        // Check that the flow has returned the expected string
//        assert(flowResponse == "Hello Alice, best wishes from Bob")
//    }
//}
