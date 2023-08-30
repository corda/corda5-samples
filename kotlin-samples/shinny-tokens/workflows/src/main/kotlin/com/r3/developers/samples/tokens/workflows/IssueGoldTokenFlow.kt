package com.r3.developers.samples.tokens.workflows

import com.r3.developers.samples.tokens.contracts.GoldContract.Issue
import com.r3.developers.samples.tokens.states.GoldState
import net.corda.v5.application.crypto.DigestService
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.membership.MemberInfo
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.*

class IssueGoldTokenFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
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

    @CordaInject
    lateinit var digestService: DigestService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        try{

            // Obtain the deserialized input arguments to the flow from the requestBody.
            val (symbol, amount, owner) = requestBody.getRequestBodyAs(
                jsonMarshallingService,
                IssueGoldTokenFlowArgs::class.java
            )

            // Get MemberInfos for the Vnode running the flow and the issuerMember.
            val myInfo = memberLookup.myInfo()
            val ownerMember = Objects.requireNonNull<MemberInfo?>(
                memberLookup.lookup(owner),
                "MemberLookup can't find owner specified in flow arguments."
            )

            // Obtain the Notary
            val notary = notaryLookup.notaryServices.iterator().next()

            val goldState = GoldState(
                getSecureHash(myInfo.name.commonName!!),
                getSecureHash(ownerMember.name.commonName!!),
                symbol,
                BigDecimal(amount), listOf<PublicKey>(ownerMember.ledgerKeys[0])
            )


            // Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(notary.name)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(goldState)
                .addCommand(Issue())
                .addSignatories(myInfo.ledgerKeys[0])

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            return flowEngine.subFlow<String>(FinalizeGoldTokenSubFlow(signedTransaction, ownerMember.name))

        }catch (e: Exception){
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.message)
            throw CordaRuntimeException(e.message)
        }
    }

    @Suspendable
    private fun getSecureHash(commonName: String): SecureHash {
        return digestService.hash(commonName.toByteArray(), DigestAlgorithmName.SHA2_256)
    }

}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "mint-1",
    "flowClassName": "com.r3.developers.samples.tokens.workflows.IssueGoldTokensFlow",
    "requestBody": {
        "symbol": "GOLD",
        "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "amount": "20"
    }
}
 */

data class IssueGoldTokenFlowArgs(
    val symbol: String,
    val amount: String,
    val owner: MemberX500Name
)