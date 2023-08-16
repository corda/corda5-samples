package com.r3.developers.samples.negotiation.workflows.accept;

import com.r3.developers.samples.negotiation.states.Proposal;
import com.r3.developers.samples.negotiation.workflows.accept.AcceptFlowResponder;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@InitiatedBy(protocol = "accept")
public class AcceptFlowResponder implements ResponderFlow {

    private final static Logger log = LoggerFactory.getLogger(AcceptFlowResponder.class);
    @CordaInject
    private UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public void call(@NotNull FlowSession session) {
        try {
            UtxoSignedTransaction finalizedSignedTransaction= utxoLedgerService.receiveFinality(session, transaction -> {
                MemberX500Name proposee = transaction.getInputStates(Proposal.class).get(0).getProposee().getName();
                if(!proposee.toString().equals(session.getCounterparty().toString())){
                    throw new CordaRuntimeException("Only the proposee can accept a proposal.");
                }
            }).getTransaction();

            log.info("Finished responder flow - " + finalizedSignedTransaction.getId());

        } catch (Exception e){
            log.warn("Exceptionally finished responder flow", e);
        }
    }
}
