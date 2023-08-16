package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.states.Loan;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class GetLoanFlow implements ClientStartableFlow {

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        List<LoanDetail> loanList =
                ledgerService.findUnconsumedStatesByType(Loan.class).stream().map(
                        it -> new LoanDetail(
                                it.getState().getContractState().getLoanId(),
                                it.getState().getContractState().getLender().getName(),
                                it.getState().getContractState().getBorrower().getName(),
                                it.getState().getContractState().getLoanAmount(),
                                it.getState().getContractState().getCollateral(),
                                it.getState().getEncumbranceGroup() != null ?
                                        it.getState().getEncumbranceGroup().getTag() : null,
                                it.getState().getEncumbranceGroup() != null ?
                                        it.getState().getEncumbranceGroup().getSize() : 0
                        )
                ).collect(Collectors.toList());
        return jsonMarshallingService.format(loanList);
    }
}

class LoanDetail {
    private String loanId;
    private MemberX500Name lender;
    private MemberX500Name borrower;
    private int loanAmount;
    private  String collateral;
    private String encumbranceGroupTag;
    private int encumbranceGroupSize;

    public LoanDetail(String loanId, MemberX500Name lender, MemberX500Name borrower, int loanAmount, String collateral,
                      String encumbranceGroupTag, int encumbranceGroupSize) {
        this.loanId = loanId;
        this.lender = lender;
        this.borrower = borrower;
        this.loanAmount = loanAmount;
        this.collateral = collateral;
        this.encumbranceGroupTag = encumbranceGroupTag;
        this.encumbranceGroupSize = encumbranceGroupSize;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
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

    public int getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(int loanAmount) {
        this.loanAmount = loanAmount;
    }

    public String getEncumbranceGroupTag() {
        return encumbranceGroupTag;
    }

    public void setEncumbranceGroupTag(String encumbranceGroupTag) {
        this.encumbranceGroupTag = encumbranceGroupTag;
    }

    public int getEncumbranceGroupSize() {
        return encumbranceGroupSize;
    }

    public void setEncumbranceGroupSize(int encumbranceGroupSize) {
        this.encumbranceGroupSize = encumbranceGroupSize;
    }

    public String getCollateral() {
        return collateral;
    }

    public void setCollateral(String collateral) {
        this.collateral = collateral;
    }
}
