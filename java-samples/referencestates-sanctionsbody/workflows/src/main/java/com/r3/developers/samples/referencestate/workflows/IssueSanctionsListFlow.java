package com.r3.developers.samples.referencestate.workflows;

import com.r3.developers.samples.referencestate.contracts.SanctionListContract;
import com.r3.developers.samples.referencestate.states.Member;
import com.r3.developers.samples.referencestate.states.SanctionList;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.common.NotaryLookup;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This flows allows a party to issue a sanctions list.
 * This sanctions list will be used by other parties when they are making their
 * IOU agreements to determine whether the counter-party is trustworthy.
 *
 */
@InitiatingFlow(protocol = "issue-sanction-list")
public class IssueSanctionsListFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(IssueSanctionsListFlow.class);
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
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();
            List<MemberInfo> allParties = memberLookup.lookup();
            allParties = allParties.stream().filter(it ->
                    !it.getName().getCommonName().contains("Notary")).collect(Collectors.toList());

            SanctionList state = new SanctionList(
                    Collections.emptyList(),
                    new Member(myInfo.getName(), myInfo.getLedgerKeys().get(0)),
                    allParties.stream().map(it->it.getLedgerKeys().get(0)).collect(Collectors.toList())
            );
            UtxoTransactionBuilder transactionBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(state)
                    .addCommand(new SanctionListContract.Create())
                    .addSignatories(myInfo.getLedgerKeys().get(0));

            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();

            List<MemberInfo> otherParties = new ArrayList<>(allParties);
            otherParties.remove(myInfo);

            UtxoSignedTransaction finalizedSignedTransaction =
                    ledgerService.finalize(signedTransaction,
                            otherParties.stream().map(
                                    it->flowMessaging.initiateFlow(it.getName())).collect(Collectors.toList())
                            ).getTransaction();

            return finalizedSignedTransaction.getId().toString();

        }catch (Exception e){
            log.warn("Failed to process request because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
 {
    "clientRequestId": "issue-sanction",
    "flowClassName": "com.r3.developers.samples.referencestate.workflows.IssueSanctionsListFlow",
    "requestBody": {
    }
 }
 */