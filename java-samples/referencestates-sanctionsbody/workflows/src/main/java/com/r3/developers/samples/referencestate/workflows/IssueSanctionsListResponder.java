package com.r3.developers.samples.referencestate.workflows;

import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

@InitiatedBy(protocol = "issue-sanction-list")
public class IssueSanctionsListResponder implements ResponderFlow {
    @CordaInject
    private UtxoLedgerService utxoLedgerService;


    @Override
    @Suspendable
    public void call(@NotNull FlowSession session) {
        utxoLedgerService.receiveFinality(session, transaction -> {});
    }
}
