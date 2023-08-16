package com.r3.developers.samples.negotiation.workflows.accept;

import com.r3.developers.samples.negotiation.contracts.Proposal;
import com.r3.developers.samples.negotiation.contracts.ProposalAndTradeContract;
import com.r3.developers.samples.negotiation.contracts.Trade;
import com.r3.developers.samples.negotiation.util.Member;
import com.r3.developers.samples.negotiation.workflows.util.FInalizeFlow;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "accept")
public class AcceptFlowRequest implements ClientStartableFlow {
    @CordaInject
    public FlowMessaging flowMessaging;

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    UtxoLedgerService utxoLedgerService;

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    public FlowEngine flowEngine;

    @Suspendable
    @Override
    public String call(@NotNull ClientRequestBody requestBody) {
        String statesNotFound = "Multiple or zero Proposal states not found wth id: ";

        AcceptFlowArgs request = requestBody.getRequestBodyAs(jsonMarshallingService, AcceptFlowArgs.class);
        // Get UUID from input JSON
        UUID proposalID = request.getProposalID();

        // Getting the old Proposal State as an input state
        List<StateAndRef<Proposal>> proposalStatAndRef = utxoLedgerService.findUnconsumedStatesByType(Proposal.class);
        List<StateAndRef<Proposal>> proposalStatAndRefWithId = proposalStatAndRef.stream().
                filter(it -> it.getState().getContractState().getProposalID().equals(proposalID)).collect(Collectors.toList());


        if (proposalStatAndRefWithId.size() != 1)
            throw new CordaRuntimeException(statesNotFound + proposalID);
        StateAndRef<Proposal> proposalStateAndRef = proposalStatAndRefWithId.get(0);
        Proposal proposalInput = proposalStateAndRef.getState().getContractState();

        // Creating a Trade as an output state
        Trade output = new Trade(proposalInput.getAmount(),
                new Member(proposalInput.getBuyer().getName(), proposalInput.getBuyer().getLedgerKey()),
                new Member(proposalInput.getSeller().getName(), proposalInput.getSeller().getLedgerKey()),
                proposalInput.getParticipants()
        );

        Member counterParty = (memberLookup.myInfo().getName().equals(proposalInput.getProposer().getName())) ? proposalInput.getProposee() : proposalInput.getProposer();

        //Initiate the transactionBuilder with command to "Accept"
        UtxoTransactionBuilder transactionBuilder = utxoLedgerService.createTransactionBuilder()
                .setNotary(proposalStateAndRef.getState().getNotaryName())
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                .addInputState(proposalStateAndRef.getRef())
                .addOutputState(output)
                .addCommand(new ProposalAndTradeContract.Accept())
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