package com.r3.developers.csdetemplate.tokens.workflows;

import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// See Chat CorDapp Design section of the getting started docs for a description of this flow.

//@InitiatingBy declares the protocol which will be used to link the initiator to the responder.
@InitiatedBy(protocol = "finalize-gold-protocol")
public class FinalizeMintResponderFlow implements ResponderFlow {
    private final static Logger log = LoggerFactory.getLogger(FinalizeMintResponderFlow.class);

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public void call(FlowSession session) {

        log.info("FinalizeChatResponderFlow1.call() called");

        try {
            // Defines the lambda validator used in receiveFinality below.
            UtxoTransactionValidator txValidator = ledgerTransaction -> {
                //GoldState state = (GoldState) ledgerTransaction.getOutputContractStates().get(0);
               // log.info("Verified the transaction - " + ledgerTransaction.getId());
            };

            // Calls receiveFinality() function which provides the responder to the finalise() function
            // in the Initiating Flow. Accepts a lambda validator containing the business logic to decide whether
            // responder should sign the Transaction.
            UtxoSignedTransaction finalizedSignedTransaction = utxoLedgerService.receiveFinality(session, txValidator);
            log.info("Finished responder flow - " + finalizedSignedTransaction.getId());
        }
        // Soft fails the flow and log the exception.
        catch(Exception e)
        {
            log.warn("Exceptionally finished responder flow", e);
        }
    }
}
