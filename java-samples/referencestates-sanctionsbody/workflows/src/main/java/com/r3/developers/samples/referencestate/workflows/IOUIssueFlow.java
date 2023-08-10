package com.r3.developers.samples.referencestate.workflows;

import com.r3.developers.samples.referencestate.contracts.SanctionableIOUContract;
import com.r3.developers.samples.referencestate.states.Member;
import com.r3.developers.samples.referencestate.states.SanctionableIOUState;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "iou-issue")
public class IOUIssueFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(IOUIssueFlow.class);
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
            IOUIssueFlowArgs flowArgs =
                    requestBody.getRequestBodyAs(jsonMarshallingService, IOUIssueFlowArgs.class);
            MemberInfo myInfo = memberLookup.myInfo();

            StateAndRef<SanctionList> sanctionsListToUse = getSanctionsList(flowArgs.getSanctionsBody());
            SanctionableIOUState iouState = new SanctionableIOUState(
                    flowArgs.getIouValue(),
                    new Member(myInfo.getName(), myInfo.getLedgerKeys().get(0)),
                    new Member(flowArgs.getOtherParty(),
                            Objects.requireNonNull(memberLookup.lookup(flowArgs.getOtherParty())).getLedgerKeys().get(0))
            );

            MemberX500Name notaryName = sanctionsListToUse.getState().getNotaryName();
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notaryName)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addReferenceState(sanctionsListToUse.getRef())
                    .addOutputState(iouState)
                    .addCommand(new SanctionableIOUContract.Create(flowArgs.getSanctionsBody()))
                    .addSignatories(iouState.getParticipants());

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            UtxoSignedTransaction finalizedTransaction = ledgerService.finalize(signedTransaction,
                    Collections.singletonList(flowMessaging.initiateFlow(flowArgs.getOtherParty()))).getTransaction();

            return finalizedTransaction.getId().toString();

        }catch (Exception e){
            log.warn("Failed to process request because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }

    }

    @Suspendable
    public StateAndRef<SanctionList> getSanctionsList(MemberX500Name sanctionsBody) {
        List<StateAndRef<SanctionList>> sanctionLists =
                ledgerService.findUnconsumedStatesByType(SanctionList.class).stream().filter(
                        it -> it.getState().getContractState().getIssuer().getName().equals(sanctionsBody)
                        ).collect(Collectors.toList());
        if(sanctionLists.isEmpty()){
            return null;
        }else {
            return sanctionLists.get(0);
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "issue-iou",
  "flowClassName": "com.r3.developers.samples.referencestate.workflows.IOUIssueFlow",
  "requestBody": {
    "iouValue": 100,
    "otherParty": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
    "sanctionsBody": "CN=SanctionsBody, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
 */