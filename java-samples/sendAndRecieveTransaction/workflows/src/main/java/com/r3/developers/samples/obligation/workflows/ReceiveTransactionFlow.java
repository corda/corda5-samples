package com.r3.developers.samples.obligation.workflows;

import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InitiatedBy(protocol = "utxo-transaction-transmission-protocol")
public class ReceiveTransactionFlow implements ResponderFlow {
    private static final Logger log = LoggerFactory.getLogger(ReceiveTransactionFlow.class);

    @CordaInject
    private UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public void call(FlowSession session) {
        // Receive the transaction and log its details.
        var transaction = utxoLedgerService.receiveTransaction(session);
        log.info("Received transaction - " + transaction.getId());
    }
}
