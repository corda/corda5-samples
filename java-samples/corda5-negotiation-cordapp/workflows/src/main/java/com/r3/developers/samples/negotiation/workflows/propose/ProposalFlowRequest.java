package com.r3.developers.samples.negotiation.workflows.propose;

import com.r3.developers.samples.negotiation.contracts.Proposal;
import com.r3.developers.samples.negotiation.contracts.ProposalAndTradeContract;
import com.r3.developers.samples.negotiation.util.Member;
import com.r3.developers.samples.negotiation.workflows.util.FInalizeFlow;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@InitiatingFlow(protocol = "proposal")
public class ProposalFlowRequest implements ClientStartableFlow {

    @CordaInject
    public FlowMessaging flowMessaging;

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    NotaryLookup notaryLookup;

    @CordaInject
    UtxoLedgerService utxoLedgerService;

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    public FlowEngine flowEngine;

    @Suspendable
    @Override
    public String call(@NotNull ClientRequestBody requestBody) {

        // Obtain the deserialized input arguments to the flow from the requestBody.
        ProposalFlowArgs request = requestBody.getRequestBodyAs(jsonMarshallingService, ProposalFlowArgs.class);
        MemberX500Name buyer;
        MemberX500Name seller;

        MemberInfo memberInfo = memberLookup.lookup(MemberX500Name.parse(request.getCounterParty()));
        Member counterParty = new Member(Objects.requireNonNull(memberInfo).getName(), memberInfo.getLedgerKeys().get(0));

        if (request.isBuyer()) {
            buyer = memberLookup.myInfo().getName();
            seller = memberInfo.getName();
        } else {
            buyer = memberInfo.getName();
            seller = memberLookup.myInfo().getName();
        }

        //Create a new Proposal state as an output state
        Proposal output = new Proposal(request.getAmount(),
                new Member(seller, Objects.requireNonNull(memberLookup.lookup(seller)).getLedgerKeys().get(0)),
                new Member(buyer, Objects.requireNonNull(memberLookup.lookup(seller)).getLedgerKeys().get(0)),
                new Member(memberLookup.myInfo().getName(), memberLookup.myInfo().getLedgerKeys().get(0)),
                counterParty);


        NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

        //Initiate the transactionBuilder with command to "propose"
        UtxoTransactionBuilder transactionBuilder = utxoLedgerService.createTransactionBuilder()
                .setNotary(notary.getName())
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                .addOutputState(output)
                .addCommand(new ProposalAndTradeContract.Propose())
                .addSignatories(output.getParticipants());


        // Call FinalizeIOUSubFlow which will finalise the transaction.
        // If successful the flow will return a String of the created transaction id,
        // if not successful it will return an error message.
        try {
            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();
            FlowSession counterPartySession = flowMessaging.initiateFlow(counterParty.getName());
            return flowEngine.subFlow(new FInalizeFlow.FinalizeRequest(signedTransaction, List.of(counterPartySession)));

        } catch (Exception e) {
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

