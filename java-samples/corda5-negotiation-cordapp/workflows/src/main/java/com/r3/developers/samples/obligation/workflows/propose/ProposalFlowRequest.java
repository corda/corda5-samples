package com.r3.developers.samples.obligation.workflows.propose;

import com.r3.developers.samples.obligation.contracts.ProposalAndTradeContract;
import com.r3.developers.samples.obligation.states.Member;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import java.time.Duration;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import com.r3.developers.samples.obligation.states.Proposal;
import com.r3.developers.samples.obligation.workflows.propose.ProposalFlowArgs;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

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
        Member counterParty = new Member(memberInfo.getName(),memberInfo.getLedgerKeys().get(0));

        if(request.isBuyer()){
            buyer= memberLookup.myInfo().getName();
            seller = memberInfo.getName();
        } else {
            buyer= memberInfo.getName();
            seller= memberLookup.myInfo().getName();
        }

        Proposal output = new Proposal(request.getAmount(),
                new Member(seller,memberLookup.lookup(seller).getLedgerKeys().get(0)),
                new Member(buyer,memberLookup.lookup(seller).getLedgerKeys().get(0)),
                new Member(memberLookup.myInfo().getName(),memberLookup.myInfo().getLedgerKeys().get(0)),
                counterParty);



//        NotaryInfo notary = requireNonNull(
//                notaryLookup.lookup(MemberX500Name.parse("O=Notary,L=London,C=GB")),
//                "NotaryLookup can't find notary specified in flow arguments."
//        );

        NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

        //doubt about this add Signatories
        UtxoTransactionBuilder transactionBuilder = utxoLedgerService.createTransactionBuilder()
                .setNotary(notary.getName())
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                .addOutputState(output)
                .addCommand(new ProposalAndTradeContract.Propose())
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

ALice hash: 5B9459F205F0
{
  "clientRequestId": "name",
  "flowClassName": "com.r3.developers.samples.obligation.workflows.propose.ProposalFlowRequest",
  "requestBody": {
      "amount": 20,
      "counterParty":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
 */
