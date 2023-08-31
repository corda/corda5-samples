package com.r3.developers.samples.negotiation.workflows.Propose

import com.r3.developers.samples.negotiation.Proposal
import com.r3.developers.samples.negotiation.ProposalAndTradeContract.Propose
import com.r3.developers.samples.negotiation.util.Member
import com.r3.developers.samples.negotiation.workflows.util.FinalizeRequest
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Duration
import java.time.Instant
import java.util.*


@InitiatingFlow(protocol = "proposal")
class ProposalFlowRequest : ClientStartableFlow {

    @CordaInject
    var jsonMarshallingService: JsonMarshallingService? = null

    @CordaInject
    var memberLookup: MemberLookup? = null

    @CordaInject
    var notaryLookup: NotaryLookup? = null

    @CordaInject
    var utxoLedgerService: UtxoLedgerService? = null

    @CordaInject
    var flowEngine: FlowEngine? = null

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        // Obtain the deserialized input arguments to the flow from the requestBody.
        val request = requestBody.getRequestBodyAs(
            jsonMarshallingService!!,
            ProposalFlowArgs::class.java
        )
        val buyer: MemberX500Name
        val seller: MemberX500Name
        val memberInfo = memberLookup!!.lookup(request.counterParty)
        val counterParty = Member(
            memberInfo!!.name, memberInfo.ledgerKeys[0]
        )
        if (request.isBuyer) {
            buyer = memberLookup!!.myInfo().name
            seller = memberInfo.name
        } else {
            buyer = memberInfo.name
            seller = memberLookup!!.myInfo().name
        }

        //Create a new Proposal state as an output state
        val output = Proposal(
            request.amount,
            Member(seller, memberLookup!!.lookup(seller)!!.ledgerKeys[0]),
            Member(buyer, memberLookup!!.lookup(seller)!!.ledgerKeys[0]),
            Member(memberLookup!!.myInfo().name, memberLookup!!.myInfo().ledgerKeys[0]),
            counterParty, null, UUID.randomUUID()
        )

        val notary = notaryLookup!!.notaryServices.iterator().next()

        //Initiate the transactionBuilder with command to "propose"
        val transactionBuilder = utxoLedgerService!!.createTransactionBuilder()
            .setNotary(notary.name)
            .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
            .addOutputState(output)
            .addCommand(Propose())
            .addSignatories(output.participants)

        val proposalStateId = output.proposalID

        // Call FinalizeIOUSubFlow which will finalise the transaction.
        // If successful the flow will return a String of the created transaction id,
        // if not successful it will return an error message.
        try {
            val signedTransaction = transactionBuilder.toSignedTransaction()
            flowEngine!!.subFlow(FinalizeRequest(signedTransaction, listOf(counterParty.name)))

            // returns the proposalId
            return proposalStateId.toString()
        } catch (e: Exception) {
            throw CordaRuntimeException(e.message)
        }
    }
}
