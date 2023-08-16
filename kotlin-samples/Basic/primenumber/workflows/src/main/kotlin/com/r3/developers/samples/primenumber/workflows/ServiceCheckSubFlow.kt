package com.r3.developers.samples.primenumber.workflows

import com.r3.developers.samples.primenumber.PrimeCommands
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.filtered.UtxoFilteredTransaction
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "service-check")
class ServiceCheckSubFlow(private val request: ServiceSignRequest): SubFlow<ServiceSignResponse>{

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(): ServiceSignResponse {
        log.info("Function called")

        val signedPrimeTransaction = request.primeSignedTransaction
        val primeService = request.primeService
        val primeServiceName = request.primeServiceName
        val primeServiceIdentity = request.primeServiceIdentity
        val requesterIdentity = request.requesterIdentity

        // You must include the components of the transaction that you want to pass on to the filtered transaction builder.
        // Otherwise, the unspecified components will be dropped
        // Note how you can include predicates to add granularity and filter elements of a transaction component
        val filteredTransaction: UtxoFilteredTransaction = ledgerService.filterSignedTransaction(signedPrimeTransaction)
            .withCommands{command -> command is PrimeCommands.Create}
            .withOutputStates()
            .withSignatories{ signatory -> signatory.equals(requesterIdentity) }
            // implicitly, 'notary', 'inputStates', and 'timeWindow' are not included.
            .build() // the filteredTransactionBuilder equivalent of the toSignedTransaction() method for full transactions

        val session = flowMessaging.initiateFlow(primeServiceName)

        val message = ServiceCheckRequest(filteredTransaction, primeService, requesterIdentity)

        return session.sendAndReceive(ServiceSignResponse::class.java,message)

    }

}

@InitiatedBy(protocol = "service-check")
class ServiceCheckResponderSubFlow(): ResponderFlow {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("ServiceCheckResponderSubFlow() called.")

        val receivedMessage = session.receive(ServiceCheckRequest::class.java)
        val filteredTransaction = receivedMessage.filteredTransaction
        val primeService = receivedMessage.primeService
        val requesterIdentity = receivedMessage.requesterIdentity

        val response: ServiceSignResponse = primeService.yesOrNo(filteredTransaction, primeService, requesterIdentity)

        session.send(response)
    }
}
