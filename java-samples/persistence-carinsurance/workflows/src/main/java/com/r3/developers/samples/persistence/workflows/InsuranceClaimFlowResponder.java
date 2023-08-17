package com.r3.developers.samples.persistence.workflows;

import com.r3.developers.samples.persistence.schema.PersistentInsurance;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.application.persistence.PersistenceService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

@InitiatedBy(protocol = "add-claim")
public class InsuranceClaimFlowResponder implements ResponderFlow {
    @CordaInject
    private UtxoLedgerService utxoLedgerService;

    @CordaInject
    private PersistenceService persistenceService;
    @Override
    @Suspendable
    public void call(@NotNull FlowSession session) {
        PersistentInsurance persistentInsurance = session.receive(PersistentInsurance.class);
        persistenceService.persist(persistentInsurance);
        utxoLedgerService.receiveFinality(session, transaction -> {});
    }
}
