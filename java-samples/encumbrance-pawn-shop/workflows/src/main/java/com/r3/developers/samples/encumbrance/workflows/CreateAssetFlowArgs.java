package com.r3.developers.samples.encumbrance.workflows;

public class CreateAssetFlowArgs {
    private String assetName;

    public CreateAssetFlowArgs() {
    }

    public CreateAssetFlowArgs(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}
