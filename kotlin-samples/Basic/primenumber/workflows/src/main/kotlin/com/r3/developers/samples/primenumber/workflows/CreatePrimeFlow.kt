package com.r3.developers.samples.primenumber.workflows

import com.r3.developers.samples.primenumber.Prime
import com.r3.developers.samples.primenumber.PrimeCommands
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

@CordaSerializable
data class QueryPrimeRequest(val n: Int)

@CordaSerializable
data class QueryPrimeResponse(val n: Int)

@InitiatingFlow(protocol = "create-prime")
class CreatePrime(): ClientStartableFlow {

    private data class CreatePrimeRequest(
        val index: Int
    )

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val CALLED = "CreatePrimeFlow.call() called"
        const val SET_UP = "Initialising flows."
        const val QUERYING_THE_PRIME_SERVICE = "Querying prime service for the Nth prime."
        const val BUILDING_THE_TX = "Building transaction."
        const val VERIFYING_THE_TX = "Verifying transaction."
        const val WE_SIGN = "signing transaction."
        const val FINALISING = "Finalising transaction."

        val notaryName: MemberX500Name = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")
        val primeServiceName: MemberX500Name = MemberX500Name.parse("CN=PrimeService, OU=Test Dept, O=R3, L=London, C=GB")
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info(CALLED)

        log.info(SET_UP)
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,CreatePrimeRequest::class.java)
        val index = request.index
        val notary = notaryLookup.lookup(notaryName) ?: throw IllegalArgumentException("Requested notary '$notaryName' not found on network.") //Alternative: notaryLookup.notaryServices.first()
        val primeService = memberLookup.lookup(primeServiceName) ?: throw IllegalArgumentException("Requested service '$primeServiceName' not found on network.")

        log.info(QUERYING_THE_PRIME_SERVICE)
        val nthPrimeResult: Int = flowEngine.subFlow(QueryPrimeSubFlow(primeServiceName,index))

        log.info(BUILDING_THE_TX)
        val ourIdentity = memberLookup.myInfo().ledgerKeys.first()
        val primeServiceIdentity = primeService.ledgerKeys.first()
        val primeState = Prime(index, nthPrimeResult,listOf(ourIdentity))
        val primeCommandData = PrimeCommands.Create(index,nthPrimeResult)
        val primeCommandRequiredSigners = listOf(primeServiceIdentity, ourIdentity)

        log.info(VERIFYING_THE_TX) //this happens when the contractState is fed to the .addOutputState() method
        val transaction = ledgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(primeState)
            .addCommand(primeCommandData)
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(primeCommandRequiredSigners)

        log.info(WE_SIGN)
        val signedTransaction = transaction.toSignedTransaction()

        log.info(FINALISING)
        val finalizationResultId: SecureHash = flowEngine.subFlow(FinalizePrimeSubFlow(signedTransaction, primeServiceName))
        val finalTransaction = ledgerService.findSignedTransaction(finalizationResultId)

        log.info("finalized transaction: $finalTransaction")
        return primeState.toString()
    }
}