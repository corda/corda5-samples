package com.r3.developers.samples.negotiation.workflows.util;

import com.r3.developers.samples.negotiation.Proposal;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FInalizeFlow {
    private final static Logger log = LoggerFactory.getLogger(FInalizeFlow.class);

    @InitiatingFlow(protocol = "finalize-protocol")
    public static class FinalizeRequest implements SubFlow<String> {

        private final UtxoSignedTransaction signedTransaction;

        private final List<FlowSession> sessions;

        public FinalizeRequest(UtxoSignedTransaction signedTransaction, List<FlowSession> sessions) {
            this.signedTransaction = signedTransaction;
            this.sessions = sessions;
        }

        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        public UtxoLedgerService ledgerService;

        @Override
        @Suspendable
        public String call() {
            // Calls the Corda provided
            // finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.
            String result;
            try {
                UtxoSignedTransaction finalizedSignedTransaction = ledgerService.finalize(
                        signedTransaction,
                        sessions
                ).getTransaction();

                result = finalizedSignedTransaction.getId().toString();
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

    @InitiatedBy(protocol = "finalize-protocol")
    public static class FinalizeResponder implements ResponderFlow {


        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        public UtxoLedgerService utxoLedgerService;

        @Override
        @Suspendable
        public void call(@NotNull FlowSession session) {
            String proposalError = "Only the proposee can modify or accept a proposal.";
            String successMessage = "Successfully finished modification responder flow - ";

            try {
                UtxoSignedTransaction finalizedSignedTransaction = utxoLedgerService.receiveFinality(session, transaction -> {
                    // goes into this if block is command is either modify or accept
                    if (!transaction.getInputStates(Proposal.class).isEmpty()) {
                        MemberX500Name proposee = transaction.getInputStates(Proposal.class).get(0).getProposee().getName();
                        if (!proposee.toString().equals(session.getCounterparty().toString())) {
                            throw new CordaRuntimeException(proposalError);
                        }
                    }
                }).getTransaction();

                log.info(successMessage + finalizedSignedTransaction.getId());


            }
            // Soft fails the flow and log the exception.
            catch (Exception e) {
                log.warn("Exceptionally finished responder flow", e);
            }
        }
    }


}
