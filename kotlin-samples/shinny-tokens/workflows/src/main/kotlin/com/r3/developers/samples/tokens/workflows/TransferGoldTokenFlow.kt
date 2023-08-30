package com.r3.developers.samples.tokens.workflows

import com.r3.developers.samples.tokens.contracts.GoldContract.Transfer
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
import net.corda.v5.ledger.utxo.StateRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.token.selection.ClaimedToken
import net.corda.v5.ledger.utxo.token.selection.TokenClaim
import net.corda.v5.ledger.utxo.token.selection.TokenClaimCriteria
import net.corda.v5.ledger.utxo.token.selection.TokenSelection
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder
import net.corda.v5.membership.MemberInfo
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

// This flow will be triggered by Bob to transfer some of his tokens to Charlie. The remaining
// amount of tokens will be given back as change to Bob.
class TransferGoldTokenFlow : ClientStartableFlow{

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var tokenSelection: TokenSelection

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var digestService: DigestService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        var tokenClaim: TokenClaim? = null
        var totalAmount: BigDecimal? = null
        var change: BigDecimal? = null
        try{
            val (symbol, issuer, amount, receiver) = requestBody.getRequestBodyAs(
                jsonMarshallingService, TransferGoldFlowInputArgs::class.java
            )
            val myInfo = memberLookup.myInfo()

            // Take the receiver of the token whom Bob will transfer the token
            val receiverMember = Objects.requireNonNull<MemberInfo?>(
                memberLookup.lookup(receiver),
                "MemberLookup can't find otherMember specified in flow arguments."
            )

            // Get the issuer of the token
            val issuerMember = Objects.requireNonNull<MemberInfo?>(
                memberLookup.lookup(issuer),
                "MemberLookup can't find otherMember specified in flow arguments."
            )

            val notary = notaryLookup.notaryServices.iterator().next()

            // Create the token claim criteria by specifying the issuer and amount
            val tokenClaimCriteria = TokenClaimCriteria(
                GoldState::class.java.getName(),
                getSecureHash(issuerMember.name.commonName!!),
                notary.name,
                symbol,
                BigDecimal(amount)
            )

            // tryClaim will check in the vault if there are tokens which can satisfy the expected amount.
            // If yes all the fungible tokens are returned back.
            // Remaining change will be returned back to the sender.
            tokenClaim = tokenSelection.tryClaim(tokenClaimCriteria)

            if (tokenClaim == null) {
                log.info("No tokens found for" + jsonMarshallingService.format(tokenClaimCriteria))
                return "No Tokens Found"
            }

            val claimedTokenList = tokenClaim.claimedTokens

            // calculate the change to be given back to the sender
            totalAmount = claimedTokenList.stream().map<BigDecimal>({ obj: ClaimedToken -> obj.getAmount() })
                .reduce(BigDecimal.ZERO, { obj: BigDecimal, augend: BigDecimal? -> obj.add(augend) })
            change = totalAmount.subtract(BigDecimal(amount))

            log.info(
                "Found total " + totalAmount + " amount of tokens for " +
                        jsonMarshallingService.format(tokenClaimCriteria)
            )

            // create a new state representing the new owner and the expected amount.
            val goldStateNew = GoldState(
                getSecureHash(issuerMember.name.commonName!!),
                getSecureHash(receiverMember.name.commonName!!),
                symbol,
                BigDecimal(amount),
                listOf(receiverMember.ledgerKeys[0])
            )

            val txBuilder : UtxoTransactionBuilder =
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    // if there is change to be returned back to the sender, create a new gold state
                    // representing the original sender and the change.
                    val goldStateChange = GoldState(
                        getSecureHash(issuerMember.name.commonName!!),
                        getSecureHash(myInfo.name.commonName!!),
                        symbol, change,
                        listOf(myInfo.ledgerKeys[0])
                    )
                    ledgerService.createTransactionBuilder()
                        .setNotary(notary.name)
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addInputStates(
                            tokenClaim.claimedTokens.stream().map{ it.stateRef }.collect(Collectors.toList()))
                        .addOutputStates(goldStateChange, goldStateNew)
                        .addCommand(Transfer())
                        .addSignatories(listOf(myInfo.ledgerKeys[0], receiverMember.ledgerKeys[0]))
                } else {
                    // if there is no change, no need to create state representing the change to be given back to the sender.
                    ledgerService.createTransactionBuilder()
                        .setNotary(notary.name)
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addInputStates(
                            tokenClaim.claimedTokens.stream().map{ it.stateRef }.collect(Collectors.toList()))
                        .addOutputStates(goldStateNew)
                        .addCommand(Transfer())
                        .addSignatories(listOf(myInfo.ledgerKeys[0], receiverMember.ledgerKeys[0]))
                }

            val signedTransaction = txBuilder.toSignedTransaction()

            flowEngine.subFlow(FinalizeGoldTokenSubFlow(signedTransaction, receiverMember.name))

        }catch (e: Exception){
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.message)

            log.info("Released the claim on the token states, indicating we spent none of them")
            // None of the tokens were used, so release all the claimed tokens
            tokenClaim!!.useAndRelease(mutableListOf<StateRef>())

            throw CordaRuntimeException(e.message)
        }finally {
            // Remove any used tokens from the cache and unlocks any remaining tokens for other flows to claim.
            if (tokenClaim != null) {
                log.info("Release the claim on the token states, indicating we spent them all")
                tokenClaim.useAndRelease(tokenClaim.getClaimedTokens().stream().map{ obj: ClaimedToken -> obj.stateRef }
                        .collect(Collectors.toList()))
                return "Total Available amount of Tokens before transfer : " + totalAmount +
                        " change to be given back to the owner : " + change + " Total amount transferred " +
                        totalAmount?.subtract(change)
            }
            return "No Tokens Found"
        }
    }

    @Suspendable
    private fun getSecureHash(commonName: String): SecureHash {
        return digestService.hash(commonName.toByteArray(), DigestAlgorithmName.SHA2_256)
    }

}

data class TransferGoldFlowInputArgs(
    val symbol: String,
    val issuer: MemberX500Name,
    val amount: String,
    val receiver: MemberX500Name
)