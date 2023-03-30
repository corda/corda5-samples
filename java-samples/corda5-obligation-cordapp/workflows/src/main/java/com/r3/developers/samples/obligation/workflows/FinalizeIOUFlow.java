package com.r3.developers.samples.obligation.workflows;

import com.r3.developers.samples.obligation.states.IOUState;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FinalizeIOUFlow {
    private final static Logger log = LoggerFactory.getLogger(FinalizeIOUFlow.class);

    // @InitiatingFlow declares the protocol which will be used to link the initiator to the responder.
    @InitiatingFlow(protocol = "finalize-iou-protocol")
    public static class FinalizeIOU implements SubFlow<String> {

        private final UtxoSignedTransaction signedTransaction;
        private final List<MemberX500Name> otherMembers;

        public FinalizeIOU(UtxoSignedTransaction signedTransaction, List<MemberX500Name> otherMembers) {
            this.signedTransaction = signedTransaction;
            this.otherMembers = otherMembers;
        }

        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        public UtxoLedgerService ledgerService;

        @CordaInject
        public FlowMessaging flowMessaging;

        @Override
        @Suspendable
        public String call() {

            log.info("FinalizeIOU.call() called");

            // Initiates a session with the other Member.
            //FlowSession session = flowMessaging.initiateFlow(otherMember);

            List<FlowSession> sessionsList = new ArrayList<>();

            for (MemberX500Name member: otherMembers) {
                sessionsList.add(flowMessaging.initiateFlow(member));
            }

            // Calls the Corda provided finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.
            // On success returns the id of the transaction created.
            String result;
            try {

                UtxoSignedTransaction finalizedSignedTransaction = ledgerService.finalize(
                        signedTransaction,
                        sessionsList
                );

                result = finalizedSignedTransaction.getId().toString();
                log.info("Success! Response: " + result);

            }
            // Soft fails the flow and returns the error message without throwing a flow exception.
            catch (Exception e) {
                log.warn("Finality failed", e);
                result = "Finality failed, " + e.getMessage();
            }
            // Returns the transaction id converted as a string
            return result;
        }
    }

    @InitiatedBy(protocol = "finalize-iou-protocol")
    public static class FinalizeIOUResponderFlow implements ResponderFlow {

        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        public UtxoLedgerService utxoLedgerService;

        @Suspendable
        @Override
        public void call(FlowSession session) {

            log.info("FinalizeIOUResponderFlow.call() called");

            try {
                // Defines the lambda validator used in receiveFinality below.
                UtxoTransactionValidator txValidator = ledgerTransaction -> {

                    // Note, this exception will only be shown in the logs if Corda Logging is set to debug.
                    if(!(ledgerTransaction.getOutputContractStates().get(0).getClass().equals(IOUState.class)))
                        throw new CordaRuntimeException("Failed verification - transaction did not have exactly one output IOUState.");

                    log.info("Verified the transaction - " + ledgerTransaction.getId());
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
}
