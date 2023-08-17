package com.r3.developers.samples.persistence.workflows;

import com.r3.developers.samples.persistence.contracts.InsuranceContract;
import com.r3.developers.samples.persistence.schema.PersistentClaim;
import com.r3.developers.samples.persistence.schema.PersistentInsurance;
import com.r3.developers.samples.persistence.schema.PersistentVehicle;
import com.r3.developers.samples.persistence.states.Claim;
import com.r3.developers.samples.persistence.states.InsuranceState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.application.persistence.PersistenceService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "add-claim")
public class InsuranceClaimFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(InsuranceClaimFlow.class);
    @CordaInject
    private JsonMarshallingService jsonMarshallingService;
    @CordaInject
    private MemberLookup memberLookup;
    @CordaInject
    private UtxoLedgerService ledgerService;
    @CordaInject
    private FlowMessaging flowMessaging;
    @CordaInject
    private PersistenceService persistenceService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        try{
            InsuranceClaimFlowArgs flowArgs =
                    requestBody.getRequestBodyAs(jsonMarshallingService, InsuranceClaimFlowArgs.class);
            MemberInfo myInfo = memberLookup.myInfo();

            // Query the vault to fetch a list of all Insurance state, and filter the results based on the policyNumber
            // to fetch the desired Insurance state from the vault. This filtered state would be used as input to the
            // transaction.
            List<StateAndRef<InsuranceState>> filteredInsuranceStateAndRefs =
                    ledgerService.findUnconsumedStatesByType(InsuranceState.class).stream().filter(
                            it -> it.getState().getContractState().getPolicyNumber().equals(flowArgs.getPolicyNumber())
                    ).collect(Collectors.toList());
            if (filteredInsuranceStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Insurance states with id " + flowArgs.getPolicyNumber() + " found");
            }
            StateAndRef<InsuranceState> loanStateAndRef = filteredInsuranceStateAndRefs.get(0);

            // Create claims
            Claim claim = new Claim(flowArgs.getClaimNumber(), flowArgs.getClaimDescription(),
                    flowArgs.getClaimAmount());
            List<Claim> claims = new ArrayList<>();
            if(loanStateAndRef.getState().getContractState().getClaims() == null ||
                    loanStateAndRef.getState().getContractState().getClaims().isEmpty()){
                claims.add(claim);
            }else {
                claims.addAll(loanStateAndRef.getState().getContractState().getClaims());
                claims.add(claim);
            }

            //Create the output state
            InsuranceState output = new InsuranceState(
                    loanStateAndRef.getState().getContractState().getPolicyNumber(),
                    loanStateAndRef.getState().getContractState().getInsuredValue(),
                    loanStateAndRef.getState().getContractState().getDuration(),
                    loanStateAndRef.getState().getContractState().getPremium(),
                    loanStateAndRef.getState().getContractState().getInsurer(),
                    loanStateAndRef.getState().getContractState().getInsuree(),
                    loanStateAndRef.getState().getContractState().getVehicleDetail(),
                    claims,
                    loanStateAndRef.getState().getContractState().getParticipants());

            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(loanStateAndRef.getState().getNotaryName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(loanStateAndRef.getRef())
                    .addOutputState(output)
                    .addCommand(new InsuranceContract.AddClaim())
                    .addSignatories(myInfo.getLedgerKeys().get(0));

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();
            PersistentInsurance persistentInsurance = persistInsurance(output, claims);
            FlowSession session = flowMessaging.initiateFlow(loanStateAndRef.getState().getContractState().getInsurer());
            session.send(persistentInsurance);

            UtxoSignedTransaction finalizedTransaction = ledgerService.finalize(signedTransaction,
                    Collections.singletonList(session)).getTransaction();

            return finalizedTransaction.getId().toString();

        }catch (Exception e){
            log.warn("Failed to process request because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }

    @Suspendable
    private PersistentInsurance persistInsurance(InsuranceState insuranceState, List<Claim> claims){
        List<PersistentClaim> persistentClaims = new ArrayList<>();
        if(claims != null && !claims.isEmpty()) {
            for(Claim claim: claims){
                PersistentClaim persistentClaim = new PersistentClaim(
                        claim.getClaimNumber(),
                        insuranceState.getPolicyNumber(),
                        claim.getClaimDescription(),
                        claim.getClaimAmount()
                );
                persistentClaims.add(persistentClaim);
            }
        }

        PersistentInsurance persistentInsurance = new PersistentInsurance(
                insuranceState.getPolicyNumber(),
                insuranceState.getInsuredValue(),
                insuranceState.getDuration(),
                insuranceState.getPremium(),
                new PersistentVehicle(
                        insuranceState.getVehicleDetail().getRegistrationNumber(),
                        insuranceState.getVehicleDetail().getChasisNumber(),
                        insuranceState.getVehicleDetail().getMake(),
                        insuranceState.getVehicleDetail().getModel(),
                        insuranceState.getVehicleDetail().getVariant(),
                        insuranceState.getVehicleDetail().getColor(),
                        insuranceState.getVehicleDetail().getFuelType()
                ),
                persistentClaims
        );

        persistenceService.persist(persistentInsurance);
        return persistentInsurance;
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "claim-1",
  "flowClassName": "com.r3.developers.samples.persistence.workflows.InsuranceClaimFlow",
  "requestBody": {
    "policyNumber" : "P001",
    "claimNumber": "CM001",
    "claimDescription": "Simple Claim",
    "claimAmount": 50000
  }
}
*/