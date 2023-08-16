package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.states.Asset;
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

public class GetAssetFlow implements ClientStartableFlow {
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        List<AssetDetail> assetList =
                ledgerService.findUnconsumedStatesByType(Asset.class).stream().map(
                        it -> new AssetDetail(
                                it.getState().getContractState().getOwner().getName(),
                                it.getState().getContractState().getAssetId(),
                                it.getState().getContractState().getAssetName(),
                                it.getState().getEncumbranceGroup() != null ?
                                        it.getState().getEncumbranceGroup().getTag() : null,
                                it.getState().getEncumbranceGroup() != null ?
                                        it.getState().getEncumbranceGroup().getSize() : 0
                        )
                ).collect(Collectors.toList());
        return jsonMarshallingService.format(assetList);
    }
}

class AssetDetail{
    private MemberX500Name owner;
    private String assetId;
    private String assetName;
    private String encumbranceGroupTag;
    private int encumbranceGroupSize;

    public AssetDetail(MemberX500Name owner, String assetId, String assetName, String encumbranceGroupTag, int encumbranceGroupSize) {
        this.owner = owner;
        this.assetId = assetId;
        this.assetName = assetName;
        this.encumbranceGroupTag = encumbranceGroupTag;
        this.encumbranceGroupSize = encumbranceGroupSize;
    }

    public MemberX500Name getOwner() {
        return owner;
    }

    public void setOwner(MemberX500Name owner) {
        this.owner = owner;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
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
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "get-asset",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.GetAssetFlow",
  "requestBody": {
  }
}
*/