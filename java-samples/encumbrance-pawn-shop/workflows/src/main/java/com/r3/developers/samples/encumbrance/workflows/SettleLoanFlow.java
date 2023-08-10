package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.contracts.AssetContract;
import com.r3.developers.samples.encumbrance.contracts.LoanContract;
import com.r3.developers.samples.encumbrance.states.Asset;
import com.r3.developers.samples.encumbrance.states.Loan;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "settle-loan")
public class SettleLoanFlow implements ClientStartableFlow {
    private final static Logger log =  LoggerFactory.getLogger(SettleLoanFlow.class);
    @CordaInject
    private JsonMarshallingService jsonMarshallingService;
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
            SettleLoanFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, SettleLoanFlowArgs.class);

            List<StateAndRef<Loan>> filteredLoanStateAndRefs =
                ledgerService.findUnconsumedStatesByType(Loan.class).stream().filter(
                        it -> it.getState().getContractState().getLoanId().equals(flowArgs.getLoanId())
                ).collect(Collectors.toList());

            if (filteredLoanStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Loan states with id " + flowArgs.getLoanId() + " found");
            }

            StateAndRef<Loan> loanStateAndRef = filteredLoanStateAndRefs.get(0);

            List<StateAndRef<Asset>> filteredAssetStateAndRefs =
                    ledgerService.findUnconsumedStatesByType(Asset.class).stream().filter(
                            it -> it.getState().getContractState().getAssetId().equals(
                                    loanStateAndRef.getState().getContractState().getCollateral()
                            )
                    ).collect(Collectors.toList());

            if (filteredAssetStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Asset states with id " + flowArgs.getLoanId() + " found");
            }

            StateAndRef<Asset> assetStateAndRef = filteredAssetStateAndRefs.get(0);

            if (filteredLoanStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Loan states with id " + flowArgs.getLoanId() + " found");
            }

            Asset asset = new Asset(
                    assetStateAndRef.getState().getContractState().getOwner(),
                    assetStateAndRef.getState().getContractState().getAssetName(),
                    assetStateAndRef.getState().getContractState().getAssetId(),
                    Collections.singletonList(assetStateAndRef.getState().getContractState().getOwner().getLedgerKey())
            );

            UtxoTransactionBuilder transactionBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(loanStateAndRef.getState().getNotaryName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                    .addInputStates(loanStateAndRef.getRef(), assetStateAndRef.getRef())
                    .addOutputState(asset)
                    .addCommand(new LoanContract.Settle())
                    .addCommand(new AssetContract.Unlock())
                    .addSignatories(
                            loanStateAndRef.getState().getContractState().getBorrower().getLedgerKey(),
                            loanStateAndRef.getState().getContractState().getLender().getLedgerKey()
                            );

            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();
            UtxoSignedTransaction finalizedTransaction =
                    ledgerService.finalize(signedTransaction, Collections.singletonList(
                            flowMessaging.initiateFlow(
                                    loanStateAndRef.getState().getContractState().getLender().getName())
                    )).getTransaction();

            return finalizedTransaction.getId().toString();


        }catch (Exception e){
            log.warn("Failed to process flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "settle-loan",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.SettleLoanFlow",
  "requestBody": {
    "loanId": "5c9f446a-ac5a-4763-91dd-770547bc715d"
  }
}
*/