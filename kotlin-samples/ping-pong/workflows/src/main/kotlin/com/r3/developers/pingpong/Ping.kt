package com.r3.developers.pingpong.workflows

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.membership.MemberInfo
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

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