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

    data class Response(val transactionId: String)

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
        val transactionId = StateRef.parse(request.stateRef, digestService).transactionId
        val transaction = requireNotNull(utxoLedgerService.findSignedTransaction(transactionId)) {
            "Transaction is not found or verified."
        }

        val members = request.members.map { x500 ->
            requireNotNull(memberLookup.lookup(MemberX500Name.parse(x500))) {
                "Member $x500 does not exist in the membership group"
            }
        }
        val sessions = members.map { flowMessaging.initiateFlow(it.name) }

        try {
            if (request.forceBackchain) {
                utxoLedgerService.sendTransactionWithBackchain(transaction, sessions)
            } else {
                utxoLedgerService.sendTransaction(transaction, sessions)
            }
        } catch (e: Exception) {
            log.warn("Sending transaction for $transactionId failed.", e)
            throw e
        }

        return jsonMarshallingService.format(Response(transactionId.toString())).also {
            log.info("SendTransaction is successful. Response: $it")
        }
    }
}

@InitiatedBy(protocol = "utxo-transaction-transmission-protocol")
class ReceiveTransactionFlow: ResponderFlow {
    private val log = LoggerFactory.getLogger(ReceiveTransactionFlow::class.java)

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService
    @Suspendable
    override fun call(session: FlowSession) {
        val transaction = utxoLedgerService.receiveTransaction(session)
        log.info("Received transaction - ${transaction.id}")
    }
}


/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "sendAndRecieve-2",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.sendAndRecieveTransactionFlow",
    "requestBody": {
    "stateRef": "SHA-256D:01EE53398B06F1E59C8064564E49EA31C1E396ECF130D6158E71FE60A26148D2:0",
    "members": ["CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"],
    "forceBackchain": "false"

    }
}
*/