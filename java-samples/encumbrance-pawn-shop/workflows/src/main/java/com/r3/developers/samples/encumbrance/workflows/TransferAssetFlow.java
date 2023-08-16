package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.contracts.AssetContract;
import com.r3.developers.samples.encumbrance.states.Asset;
import com.r3.developers.samples.encumbrance.states.Member;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "transfer-asset")
public class TransferAssetFlow implements ClientStartableFlow {
    private final static Logger log =  LoggerFactory.getLogger(TransferAssetFlow.class);
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
            TransferAssetFlowArgs flowArgs = requestBody
                    .getRequestBodyAs(jsonMarshallingService, TransferAssetFlowArgs.class);

            MemberInfo memberInfo = memberLookup.lookup(MemberX500Name.parse(flowArgs.getBuyer()));
            Member buyer = new Member(memberInfo.getName(), memberInfo.getLedgerKeys().get(0));

            List<StateAndRef<Asset>> filteredAssetStateAndRefs =
                    ledgerService.findUnconsumedStatesByType(Asset.class).stream().filter(
                    it -> it.getState().getContractState().getAssetId().equals(flowArgs.getAssetId())
            ).collect(Collectors.toList());

            if (filteredAssetStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Asset states with id " + flowArgs.getAssetId() + " found");
            }
            StateAndRef<Asset> assetStateAndRef = filteredAssetStateAndRefs.get(0);

            Asset output = new Asset(
                    buyer,
                    assetStateAndRef.getState().getContractState().getAssetName(),
                    assetStateAndRef.getState().getContractState().getAssetId(),
                    Collections.singletonList(buyer.getLedgerKey())
            );


            UtxoTransactionBuilder transactionBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(assetStateAndRef.getState().getNotaryName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                    .addInputStates(assetStateAndRef.getRef())
                    .addOutputState(output)
                    .addCommand(new AssetContract.Transfer())
                    .addSignatories(buyer.getLedgerKey(),
                            assetStateAndRef.getState().getContractState().getOwner().getLedgerKey());

            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();

            UtxoSignedTransaction finalizedSignedTransaction =
                    ledgerService.finalize(signedTransaction,
                            Collections.singletonList(flowMessaging.initiateFlow(buyer.getName()))).getTransaction();
            return finalizedSignedTransaction.getId().toString();

        }catch (Exception e){
            log.warn("Failed to process flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "transfer-asset",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAssetFlow",
  "requestBody": {
    "assetId": "b4beb3d5-faae-4146-ab77-34e1c59f3ee9",
    "buyer": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
*/