package com.r3.developers.samples.primenumber.workflows

import com.r3.developers.samples.primenumber.Prime
import com.r3.developers.samples.primenumber.PrimeCommands
import com.r3.developers.samples.primenumber.Query
import com.r3.developers.samples.primenumber.services.PrimeService
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
import net.corda.v5.ledger.utxo.StateRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.ledger.utxo.transaction.filtered.UtxoFilteredTransaction
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit

@CordaSerializable
data class QueryPrimeRequest(val n: Int, val primeService: PrimeService)

@CordaSerializable
data class QueryPrimeResponse(val nthPrime: Int, val primeService: PrimeService)

@CordaSerializable
data class ServiceSignRequest(val primeSignedTransaction: UtxoSignedTransaction, val primeService: PrimeService, val primeServiceName: MemberX500Name, val primeServiceIdentity: PublicKey, val requesterIdentity: PublicKey)

@CordaSerializable
data class ServiceCheckRequest(val filteredTransaction: UtxoFilteredTransaction, val primeService: PrimeService, val requesterIdentity: PublicKey)

@CordaSerializable
data class ServiceSignResponse(val verified: Boolean)

@CordaSerializable
data class PrimeFinalizationRequest(val primeSignedTransaction: UtxoSignedTransaction, val otherMemberName: MemberX500Name, val otherMemberIdentity: PublicKey)

@CordaSerializable
data class PrimeFinalizationResponse(val finalTransactionId: SecureHash)

// This is the client-side workflow that get called first, and will:
// - set up and identify the member nodes, notary, and the independently written prime service
// - call the QueryPrimeSubFlow to calculate the Nth prime
// - use the result of the QueryPrimeSubFlow to build an initial transaction where the prime service
//   signs the transaction which will be used as a reference for the second transaction
// - call the FinalizeSignedQueryTransactionSubFlow to finalize the first transaction,
// - build the second transaction which involves the requesting member and the counterparty, where
//   the
// - call the ServiceCheckSubFlow to create a filtered transaction and request the prime service
//   to verify the transaction before finalization
// - call the FinalizeSignedPrimeTransactionSubFlow to finalize the second transaction
// - return the primeTransaction's output state as a string to give the Nth prime given index n
@InitiatingFlow(protocol = "create-prime")
class CreatePrime(): ClientStartableFlow {

    private data class CreatePrimeRequest(val index: Int)

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val CALLED = "CreatePrimeFlow.call() called"
        const val SET_UP = "Initialising flows."
        const val CREATE_SERVICE = "Creating the prime service"
        const val QUERYING_THE_PRIME_SERVICE = "Querying prime service for the Nth prime."
        const val PREPARING_THE_TX = "Preparing Transaction Data."
        const val BUILD_QUERY_TX = "Building the initial Query transaction."
        const val BUILD_CREATE_TX = "Building the final Create transaction."
        const val WE_SIGN = "Signing transaction."
        const val PRIME_SERVICE_CHECK = "Requesting PrimeService approval."
        const val FAILED_VERIFICATION = "Proposed transaction is invalid"
        const val FINALISING = "Finalising transaction."

        val notaryName: MemberX500Name = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")
        val primeServiceName: MemberX500Name = MemberX500Name.parse("CN=PrimeService, OU=Test Dept, O=R3, L=London, C=GB")
        val daveMemberNodeName: MemberX500Name = MemberX500Name.parse("CN=Dave, OU=Test Dept, O=R3, L=London, C=GB")
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
        val notaryInfo = notaryLookup.lookup(notaryName) ?: throw IllegalArgumentException("Requested notary '$notaryName' not found on network.") //Alternative: notaryLookup.notaryServices.first()
        val primeServiceInfo = memberLookup.lookup(primeServiceName) ?: throw IllegalArgumentException("Requested service '$primeServiceName' not found on network.")
        val daveMemberNodeInfo = memberLookup.lookup(daveMemberNodeName) ?: throw IllegalArgumentException("Member '$daveMemberNodeName' not found on network.")
        val ourIdentity = memberLookup.myInfo().ledgerKeys.first()
        val primeServiceIdentity = primeServiceInfo.ledgerKeys.first()
        val daveIdentity = daveMemberNodeInfo.ledgerKeys.first()

        log.info(CREATE_SERVICE)
        val primeService = PrimeService()

        log.info(QUERYING_THE_PRIME_SERVICE)
        val queryPrimeResult: QueryPrimeResponse = flowEngine.subFlow(QueryPrimeSubFlow(primeServiceName,index, primeService))
        val nthPrimeResult: Int = queryPrimeResult.nthPrime

        log.info(PREPARING_THE_TX)
        val queryOutputState = Query(nthPrimeResult,listOf(ourIdentity,primeServiceIdentity))
        val primeOutputState = Prime(index, nthPrimeResult,listOf(ourIdentity,daveIdentity))
        val queryCommand = PrimeCommands.Query(index)
        val createCommand = PrimeCommands.Create(index,nthPrimeResult)

        // this creates the transaction between the prime service to serve as third-party proof
        log.info(BUILD_QUERY_TX)
        val queryTransaction = ledgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addOutputState(queryOutputState) //contract state verification happens under the hood here
            .addCommand(queryCommand)
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(ourIdentity,primeServiceIdentity))
            .toSignedTransaction()

        val finalizedQueryTransaction = flowEngine.subFlow(FinalizeSignedQueryTransactionSubFlow(queryTransaction, primeServiceName))
        log.info("finalized first transaction, transaction id: $finalizedQueryTransaction")
        val finalizedQueryTransactionStateRef: StateRef = ledgerService.findUnconsumedStatesByType(Query::class.java)
            .first{ stateAndRef -> stateAndRef.ref.transactionId == finalizedQueryTransaction }
            ?.ref!!
        log.info("finalizedQueryTransactionStateRef: $finalizedQueryTransactionStateRef")


        // this creates the next transaction which involves the other member node and not the service
        log.info(BUILD_CREATE_TX)
        val primeTransaction = ledgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputState(finalizedQueryTransactionStateRef)
            .addOutputState(primeOutputState) //contract state verification happens under the hood here
            .addCommand(createCommand)
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(ourIdentity,daveIdentity))

        log.info(WE_SIGN)
        val primeSignedTransaction = primeTransaction.toSignedTransaction()

        // a filtered transaction will be sent to the service to verify the signed prime transaction before finalization
        log.info(PRIME_SERVICE_CHECK)
        val serviceSignRequest = ServiceSignRequest(
            primeSignedTransaction,
            queryPrimeResult.primeService,
            primeServiceName,
            primeServiceIdentity,
            ourIdentity
        )
        val serviceCheckResult: ServiceSignResponse = flowEngine.subFlow(ServiceCheckSubFlow(serviceSignRequest))

        if(!serviceCheckResult.verified){
            return FAILED_VERIFICATION
        }

        log.info(FINALISING)
        val primeFinalizeRequest = PrimeFinalizationRequest(
            primeSignedTransaction,
            daveMemberNodeName,
            daveIdentity
        )

        val finalizedPrimeTransaction: PrimeFinalizationResponse = flowEngine.subFlow(FinalizeSignedPrimeTransactionSubFlow(primeFinalizeRequest))

        val finalTransaction = ledgerService.findSignedTransaction(finalizedPrimeTransaction.finalTransactionId)
        log.info("finalized transaction: $finalTransaction")

        return primeOutputState.toString()
    }
}