package com.r3.developers.samples.pingpong.workflows

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// A class to hold the deserialized arguments required to start the flow.
data class PingFlowArgs( val otherMember: MemberX500Name)

@InitiatingFlow(protocol = "ping")
class Ping: ClientStartableFlow {

    private companion object {
        private val log = LoggerFactory.getLogger(Ping::class.java)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var memberLookup: MemberLookup


    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("Ping.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, PingFlowArgs::class.java)

            //Obtain the MemberX500Name of the other member
            val otherMember = flowArgs.otherMember
            val myInfo = memberLookup.myInfo().name

            val session: FlowSession = flowMessaging.initiateFlow(otherMember)
            val message: String= session.sendAndReceive(String::class.java,"ping")

           return message

        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process ping flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}

@InitiatedBy(protocol = "ping")
class Pong : ResponderFlow {
    // FlowMessaging provides a service that establishes flow sessions between virtual nodes
    // that send and receive payloads between them.
    @CordaInject
    var flowMessaging: FlowMessaging? = null

    // MemberLookup provides a service for looking up information about members of the virtual network which
    // this CorDapp operates in.
    @CordaInject
    var memberLookup: MemberLookup? = null

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("Pong.call() called")
        val message = session.receive<String>(String::class.java)
        if (message == "ping") {
            log.info("Received Ping")
            session.send("pong")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Ping::class.java)
    }
}