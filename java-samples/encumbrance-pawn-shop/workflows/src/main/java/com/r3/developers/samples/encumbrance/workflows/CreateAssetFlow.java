package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.contracts.AssetContract;
import com.r3.developers.samples.encumbrance.states.Asset;
import com.r3.developers.samples.encumbrance.states.Member;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

public class CreateAssetFlow implements ClientStartableFlow {
    private final static Logger log =  LoggerFactory.getLogger(CreateAssetFlow.class);
    @CordaInject
    private JsonMarshallingService jsonMarshallingService;
    @CordaInject
    private MemberLookup memberLookup;
    @CordaInject
    private NotaryLookup notaryLookup;
    @CordaInject
    private UtxoLedgerService ledgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        try{
            CreateAssetFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateAssetFlowArgs.class);
            MemberInfo myInfo = memberLookup.myInfo();
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

            Asset asset = new Asset(
                    new Member(myInfo.getName(), myInfo.getLedgerKeys().get(0)),
                    flowArgs.getAssetName(),
                    UUID.randomUUID().toString(),
                    CollecCtions.singletonList(myInfo.getLedgerKeys().get(0))
            );
            UtxoTransactionBuilder transactionBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                    .addOutputState(asset)
                    .addCommand(new AssetContract.Create())
                    .addSignatories(asset.getOwner().getLedgerKey());

            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();
            UtxoSignedTransaction finalizedTransaction =
                    ledgerService.finalize(signedTransaction, Collections.emptyList()).getTransaction();

            return finalizedTransaction.getId().toString();
        }catch (Exception e){
            log.warn("Failed to process flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
    "clientRequestId": "create-asset",
    "flowClassName": "com.r3.developers.samples.encumbrance.workflows.CreateAssetFlow",
    "requestBody": {
    "assetName": "My Asset"
    }
}
*/