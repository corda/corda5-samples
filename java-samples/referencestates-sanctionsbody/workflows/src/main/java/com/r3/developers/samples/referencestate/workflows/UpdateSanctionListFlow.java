package com.r3.developers.samples.referencestate.workflows;

import com.r3.developers.samples.referencestate.contracts.SanctionListContract;
import com.r3.developers.samples.referencestate.states.Member;
import com.r3.developers.samples.referencestate.states.SanctionList;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "update-sanction-list")
public class UpdateSanctionListFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(UpdateSanctionListFlow.class);
    @CordaInject
    private JsonMarshallingService jsonMarshallingService;
    @CordaInject
    private MemberLookup memberLookup;
    @CordaInject
    private NotaryLookup notaryLookup;
    @CordaInject
    private UtxoLedgerService ledgerService;
    @CordaInject
    private FlowMessaging flowMessaging;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        try{
            MemberInfo myInfo = memberLookup.myInfo();
            List<MemberInfo> allParties = memberLookup.lookup();
            allParties = allParties.stream().filter(it ->
                    !it.getName().getCommonName().contains("Notary")).collect(Collectors.toList());

            UpdateSanctionListFlowArgs flowArgs =
                    requestBody.getRequestBodyAs(jsonMarshallingService, UpdateSanctionListFlowArgs.class);

            List<StateAndRef<SanctionList>> oldList =
                    ledgerService.findUnconsumedStatesByType(SanctionList.class);

            if(oldList.isEmpty()){
                throw new CordaRuntimeException("Sanction List not found.");
            }
            SanctionList oldStateData = oldList.get(0).getState().getContractState();

            List<Member> badPeople = new ArrayList<>(oldStateData.getBadPeople());
            Member partyToSanction = new Member(
                flowArgs.getPartyToSanction(),
                memberLookup.lookup(flowArgs.getPartyToSanction()).getLedgerKeys().get(0)
            );
            badPeople.add(partyToSanction);
            SanctionList newList = new SanctionList(
                    badPeople,
                    oldStateData.getIssuer(),
                    oldStateData.getUniqueId(),
                    oldStateData.getParticipants()
            );

            MemberX500Name notaryName = oldList.get(0).getState().getNotaryName();
            //Create an unsigned transaction
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notaryName)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(oldList.get(0).getRef())
                    .addOutputState(newList)
                    .addCommand(new SanctionListContract.Update())
                    .addSignatories(myInfo.getLedgerKeys().get(0));

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            List<MemberInfo> otherParties = new ArrayList<>(allParties);
            otherParties.remove(myInfo);

            UtxoSignedTransaction finalizedSignedTransaction =
                    ledgerService.finalize(signedTransaction,
                            otherParties.stream().map(
                                    it->flowMessaging.initiateFlow(it.getName())).collect(Collectors.toList())
                    ).getTransaction();

            return finalizedSignedTransaction.getId().toString();

        }catch(Exception e){
            log.warn("Failed to process request because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "update-sanction",
  "flowClassName": "com.r3.developers.samples.referencestate.workflows.UpdateSanctionListFlow",
  "requestBody": {
    "partyToSanction": "CN=DodgyParty, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
 */