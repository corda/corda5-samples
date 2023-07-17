package com.r3.developers.samples.encumbrance.workflows;

public class TransferAvatarFlowArgs {
    private String avatarId;
    private String buyer;

    public TransferAvatarFlowArgs() {
    }

    public TransferAvatarFlowArgs(String avatarId, String buyer) {
        this.avatarId = avatarId;
        this.buyer = buyer;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }
}
