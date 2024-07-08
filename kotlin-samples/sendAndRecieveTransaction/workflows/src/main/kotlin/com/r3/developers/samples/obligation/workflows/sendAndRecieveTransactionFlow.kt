package com.r3.developers.samples.obligation.workflows

import net.corda.v5.application.crypto.DigestService
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.StateRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "utxo-transaction-transmission-protocol")
class sendAndRecieveTransactionFlow : ClientStartableFlow {
    data class Request(
        val stateRef: String,
        val members: List<String>,
        val forceBackchain: Boolean = false
    )

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var digestService: DigestService

    private val log = LoggerFactory.getLogger(sendAndRecieveTransactionFlow::class.java)

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, Request::class.java)

        // Parse the state reference to obtain the transaction ID.
        val transactionId = StateRef.parse(request.stateRef + ":0", digestService).transactionId

        // Retrieve the signed transaction from the ledger.
        val transaction = requireNotNull(utxoLedgerService.findSignedTransaction(transactionId)) {
            "Transaction is not found or verified."
        }

        // Map the X500 names in the request to Member objects, ensuring each member exists.
        val members = request.members.map { x500 ->
            requireNotNull(memberLookup.lookup(MemberX500Name.parse(x500))) {
                "Member $x500 does not exist in the membership group"
            }
        }

        // Initialize the sessions with the memebers that will be used to send the transaction.
        val sessions = members.map { flowMessaging.initiateFlow(it.name) }

        // Send the transaction with or without backchain depending on the request.
        try {
            if (request.forceBackchain) {
                utxoLedgerService.sendTransactionWithBackchain(transaction, sessions)
            } else {
                utxoLedgerService.sendTransaction(transaction, sessions)
            }
        } catch (e: Exception) {
            // Log and rethrow any exceptions encountered during transaction sending.
            log.warn("Sending transaction for $transactionId failed.", e)
            throw e
        }

        // Format and log the successful transaction response.
        return jsonMarshallingService.format(transactionId.toString()).also {
            log.info("SendTransaction is successful. Response: $it")
        }
    }


    @InitiatedBy(protocol = "utxo-transaction-transmission-protocol")
    class ReceiveTransactionFlow : ResponderFlow {
        private val log = LoggerFactory.getLogger(ReceiveTransactionFlow::class.java)

        @CordaInject
        lateinit var utxoLedgerService: UtxoLedgerService

        @Suspendable
        override fun call(session: FlowSession) {
            // Receive the transaction and log its details.
            val transaction = utxoLedgerService.receiveTransaction(session)
            log.info("Received transaction - ${transaction.id}")
        }
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "sendAndRecieve-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.sendAndRecieveTransactionFlow",
    "requestBody": {
    "stateRef": "STATE REF ID HERE",
    "members": ["CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"],
    "forceBackchain": "false"

    }
}
*/