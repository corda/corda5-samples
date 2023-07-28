package com.r3.developers.samples.primenumber.flows

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

        const val CALLED = "oracle-primenumber: CreatePrimeFlow.call() called"
        const val SET_UP = "Initialising flows."
        const val QUERYING_THE_ORACLE = "Querying oracle for the Nth prime."
        const val BUILDING_THE_TX = "Building transaction."
        const val VERIFYING_THE_TX = "Verifying transaction."
        const val WE_SIGN = "signing transaction."
        const val FINALISING = "Finalising transaction."


        //HARDCODED Notary and Oracle MemberX500Name - can be configured
        val notaryName: MemberX500Name = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")
        val oracleName: MemberX500Name = MemberX500Name.parse("CN=Oracle, OU=Test Dept, O=R3, L=London, C=GB")
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
        val notary = notaryLookup.lookup(notaryName) ?: throw IllegalArgumentException("Requested oracle '$notaryName' not found on network.") //Alternative: notaryLookup.notaryServices.first()
        val oracle = memberLookup.lookup(oracleName) ?: throw IllegalArgumentException("Requested oracle '$oracleName' not found on network.")

        log.info(QUERYING_THE_ORACLE)
        val nthPrimeRequestedFromOracle: Int = flowEngine.subFlow(QueryPrimeSubFlow(oracleName,index))

        log.info(BUILDING_THE_TX)
        val ourIdentity = memberLookup.myInfo().ledgerKeys.first()
        val oracleIdentity = oracle.ledgerKeys.first()
        val primeState = Prime(index, nthPrimeRequestedFromOracle,listOf(ourIdentity))
        val primeCommandData = PrimeCommands.Create(index,nthPrimeRequestedFromOracle)
        val primeCommandRequiredSigners = listOf(oracleIdentity, ourIdentity)

        log.info(VERIFYING_THE_TX) //this happens when the contractState is fed to the .addOutputState() method
        val transaction = ledgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(primeState)
            .addCommand(primeCommandData)
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(primeCommandRequiredSigners)

        log.info(WE_SIGN)
        val signedTransaction = transaction.toSignedTransaction()

        log.info(FINALISING) //log.info(ORACLE_SIGN inside the subflow)
        val finalizationResultId: SecureHash = flowEngine.subFlow(FinalizePrimeSubFlow(signedTransaction,oracleName))
        val finalTransaction = ledgerService.findSignedTransaction(finalizationResultId)

        log.info("finalized transaction: $finalTransaction")

        return primeState.toString()
    }
}