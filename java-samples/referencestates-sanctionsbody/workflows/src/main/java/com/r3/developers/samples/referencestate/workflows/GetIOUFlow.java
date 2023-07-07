package com.r3.developers.samples.referencestate.workflows;

import com.r3.developers.samples.referencestate.states.SanctionableIOUState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetIOUFlow implements ClientStartableFlow {
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {

        List<IOU> iouList =
                ledgerService.findUnconsumedStatesByType(SanctionableIOUState.class).stream().map(
                        it -> new IOU(
                                it.getState().getContractState().getValue(),
                                it.getState().getContractState().getLender().getName(),
                                it.getState().getContractState().getBorrower().getName(),
                                it.getState().getContractState().getUniqueIdentifier()
                        )
                ).collect(Collectors.toList());
        return jsonMarshallingService.format(iouList);
    }

    class IOU{
        private int value;
        private MemberX500Name lender;
        private MemberX500Name borrower;
        private UUID uniqueIdentifier;

        public IOU(int value, MemberX500Name lender, MemberX500Name borrower, UUID uniqueIdentifier) {
            this.value = value;
            this.lender = lender;
            this.borrower = borrower;
            this.uniqueIdentifier = uniqueIdentifier;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public MemberX500Name getLender() {
            return lender;
        }

        public void setLender(MemberX500Name lender) {
            this.lender = lender;
        }

        public MemberX500Name getBorrower() {
            return borrower;
        }

        public void setBorrower(MemberX500Name borrower) {
            this.borrower = borrower;
        }

        public UUID getUniqueIdentifier() {
            return uniqueIdentifier;
        }

        public void setUniqueIdentifier(UUID uniqueIdentifier) {
            this.uniqueIdentifier = uniqueIdentifier;
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
 {
    "clientRequestId": "get-iou",
    "flowClassName": "com.r3.developers.samples.referencestate.workflows.GetIOUFlow",
    "requestBody": {
    }
 }
 */