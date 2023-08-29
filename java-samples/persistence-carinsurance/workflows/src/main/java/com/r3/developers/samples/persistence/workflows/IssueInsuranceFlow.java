package com.r3.developers.samples.persistence.workflows;

import com.r3.developers.samples.persistence.contracts.InsuranceContract;
import com.r3.developers.samples.persistence.schema.PersistentInsurance;
import com.r3.developers.samples.persistence.schema.PersistentVehicle;
import com.r3.developers.samples.persistence.states.InsuranceState;
import com.r3.developers.samples.persistence.states.VehicleDetail;
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
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

@InitiatingFlow(protocol = "issue-insurance")
public class IssueInsuranceFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(IssueInsuranceFlow.class);

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
    @CordaInject
    private PersistenceService persistenceService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        try{
            IssueInsuranceFlowArgs flowArgs =
                    requestBody.getRequestBodyAs(jsonMarshallingService, IssueInsuranceFlowArgs.class);

            MemberInfo myInfo = memberLookup.myInfo();
            MemberInfo insuree = memberLookup.lookup(flowArgs.getInsuree());

            InsuranceState insurance = getInsuranceState(flowArgs, myInfo, insuree.getLedgerKeys().get(0));

            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(insurance)
                    .addCommand(new InsuranceContract.IssueInsurance())
                    .addSignatories(myInfo.getLedgerKeys().get(0));

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            // Persist the state information in custom database tables.
            PersistentInsurance persistentInsurance = persistInsurance(insurance);

            // Send the entity to counterparty. They use it to persist the information at their end.
            FlowSession session = flowMessaging.initiateFlow(flowArgs.getInsuree());
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
    private PersistentInsurance persistInsurance(InsuranceState insuranceState){
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
                Collections.emptyList()
        );
            persistenceService.persist(persistentInsurance);
        return persistentInsurance;
    }

    @Suspendable
    private InsuranceState getInsuranceState(IssueInsuranceFlowArgs flowArgs, MemberInfo myInfo, PublicKey insureeKey) {
        VehicleDetail vehicleDetail = new VehicleDetail(
                flowArgs.getVehicleInfo().getRegistrationNumber(),
                flowArgs.getVehicleInfo().getChasisNumber(),
                flowArgs.getVehicleInfo().getMake(),
                flowArgs.getVehicleInfo().getModel(),
                flowArgs.getVehicleInfo().getVariant(),
                flowArgs.getVehicleInfo().getColor(),
                flowArgs.getVehicleInfo().getFuelType()
        );

        return new InsuranceState(
                flowArgs.getPolicyNumber(),
                flowArgs.getInsuredValue(),
                flowArgs.getDuration(),
                flowArgs.getPremium(),
                myInfo.getName(),
                flowArgs.getInsuree(),
                vehicleDetail,
                Collections.emptyList(),
                Arrays.asList(myInfo.getLedgerKeys().get(0), insureeKey));
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "issue-1",
  "flowClassName": "com.r3.developers.samples.persistence.workflows.IssueInsuranceFlow",
  "requestBody": {
    "vehicleInfo": {
        "registrationNumber": "MH7777",
        "chasisNumber": "CH8771",
        "make": "Hyundai",
        "model": "i20",
        "variant": "Asta",
        "color": "grey",
        "fuelType": "Petrol"
    },
    "policyNumber" : "P001",
    "insuredValue": 500000,
    "duration": 2,
    "premium": 20000,
    "insuree": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
*/