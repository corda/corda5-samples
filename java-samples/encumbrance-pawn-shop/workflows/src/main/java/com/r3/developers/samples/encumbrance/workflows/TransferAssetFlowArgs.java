package com.r3.developers.samples.encumbrance.workflows;

public class TransferAssetFlowArgs {
    private String assetId;
    private String buyer;

    public TransferAssetFlowArgs() {
    }

    public TransferAssetFlowArgs(String assetId, String buyer) {
        this.assetId = assetId;
        this.buyer = buyer;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }
}
