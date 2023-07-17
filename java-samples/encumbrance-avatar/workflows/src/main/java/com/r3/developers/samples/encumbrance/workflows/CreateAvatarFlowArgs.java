package com.r3.developers.samples.encumbrance.workflows;

public class CreateAvatarFlowArgs {
    private String avatarId;
    private long expiryAfterMinutes;

    public CreateAvatarFlowArgs() {
    }

    public CreateAvatarFlowArgs(String avatarId, long expiryAfterMinutes) {
        this.avatarId = avatarId;
        this.expiryAfterMinutes = expiryAfterMinutes;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public long getExpiryAfterMinutes() {
        return expiryAfterMinutes;
    }

    public void setExpiryAfterMinutes(long expiryAfterMinutes) {
        this.expiryAfterMinutes = expiryAfterMinutes;
    }
}
