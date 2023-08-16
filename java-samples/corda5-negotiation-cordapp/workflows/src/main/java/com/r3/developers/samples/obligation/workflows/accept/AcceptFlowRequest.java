package com.r3.developers.samples.obligation.workflows.accept;

import com.r3.developers.samples.obligation.contracts.ProposalAndTradeContract;
import com.r3.developers.samples.obligation.states.Member;
import com.r3.developers.samples.obligation.states.Proposal;
import com.r3.developers.samples.obligation.states.Trade;
import com.r3.developers.samples.obligation.workflows.modify.ModifyFlowArgs;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "accept")
public class AcceptFlowRequest implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(AcceptFlowRequest.class);

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

    @CordaInject
    public FlowEngine flowEngine;

    @Suspendable
    @Override
    public String call(@NotNull ClientRequestBody requestBody) {
        AcceptFlowArgs request = requestBody.getRequestBodyAs(jsonMarshallingService,AcceptFlowArgs.class);
        // Get UUID from input JSON
        UUID proposalID = request.getProposalID();

        UUID stateID = utxoLedgerService.findUnconsumedStatesByType(Proposal.class).get(0).getState().getContractState().getLinearId();


        List<StateAndRef<Proposal>> proposalStatAndRef = utxoLedgerService.findUnconsumedStatesByType(Proposal.class);
        List< StateAndRef<Proposal>> proposalStatAndRefWithId = proposalStatAndRef.stream().
                filter(it->it.getState().getContractState().getLinearId().equals(proposalID)).collect(Collectors.toList());

        if(proposalStatAndRefWithId.size()!=1) throw  new CordaRuntimeException("Multiple or zero Proposal states with id " + proposalID + "found");
        StateAndRef<Proposal> proposalStateAndRef = proposalStatAndRefWithId.get(0);
        Proposal proposalInput = proposalStateAndRef.getState().getContractState();


        Trade output = new Trade(proposalInput.getAmount(),
                new Member(proposalInput.getBuyer().getName(),proposalInput.getBuyer().getLedgerKey()),
                new Member(proposalInput.getSeller().getName(), proposalInput.getSeller().getLedgerKey()),
                proposalInput.getParticipants()
                );

        Member counterParty= (memberLookup.myInfo().getName().equals(proposalInput.getProposer().getName()))?proposalInput.getProposee(): proposalInput.getProposer();

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
        try{
            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();
            FlowSession counterPartySession = flowMessaging.initiateFlow(counterParty.getName());
            UtxoSignedTransaction finalizedTransaction= utxoLedgerService.finalize(signedTransaction,List.of(counterPartySession)).getTransaction();

            return  finalizedTransaction.getId().toString();
        }
        catch (Exception e){
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
/*

Alice hash: 5B9459F205F0

{
  "clientRequestId": "flow name",
  "flowClassName": "com.r3.developers.samples.obligation.workflows.accept.AcceptFlowRequest",
  "requestBody": {
      "proposalID": "9d9d36df-6091-4bcf-8334-5926a6ba422b"
  }
}
 */