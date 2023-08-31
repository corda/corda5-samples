package com.r3.developers.samples.negotiation.workflows.modify;

import com.r3.developers.samples.negotiation.Proposal;
import com.r3.developers.samples.negotiation.ProposalAndTradeContract;
import com.r3.developers.samples.negotiation.util.Member;
import com.r3.developers.samples.negotiation.workflows.util.FinalizeFlow;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.common.NotaryLookup;
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

@InitiatingFlow(protocol = "modify")
public class ModifyFlowRequest implements ClientStartableFlow {
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
        String statesNotFound = "Multiple or zero Proposal states not found wth id: ";

        // Obtain the deserialized input arguments to the flow from the requestBody.
        ModifyFlowArgs request = requestBody.getRequestBodyAs(jsonMarshallingService, ModifyFlowArgs.class);

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

        //creating a new Proposal as an output state
        Member counterParty = (memberLookup.myInfo().getName().equals(proposalInput.getProposer().getName())) ? proposalInput.getProposee() : proposalInput.getProposer();

        Proposal output = new Proposal(request.getNewAmount(),
                new Member(proposalInput.getBuyer().getName(), proposalInput.getBuyer().getLedgerKey()),
                new Member(proposalInput.getSeller().getName(), proposalInput.getSeller().getLedgerKey()),
                new Member(memberLookup.myInfo().getName(), memberLookup.myInfo().getLedgerKeys().get(0)),
                new Member(counterParty.getName(), counterParty.getLedgerKey()),
                proposalID, new Member(memberLookup.myInfo().getName(), memberLookup.myInfo().getLedgerKeys().get(0)));

        // Initiating the transactionBuilder with command to "modify"
        UtxoTransactionBuilder transactionBuilder = utxoLedgerService.createTransactionBuilder()
                .setNotary(proposalStateAndRef.getState().getNotaryName())
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                .addInputState(proposalStateAndRef.getRef())
                .addOutputState(output)
                .addCommand(new ProposalAndTradeContract.Modify())
                .addSignatories(output.getParticipants());


        // Call FinalizeIOUSubFlow which will finalise the transaction.
        // If successful the flow will return a String of the created transaction id,
        // if not successful it will return an error message.
        try {
            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();
            FlowSession counterPartySession = flowMessaging.initiateFlow(counterParty.getName());
            flowEngine.subFlow(new FinalizeFlow.FinalizeRequest(signedTransaction, List.of(counterPartySession)));
            return output.getProposalID().toString();
        } catch (Exception e) {
            throw new CordaRuntimeException(e.getMessage());
        }

    }
}