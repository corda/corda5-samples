package com.r3.csde.dtos;

public class VirtualNodeInfoDTO {
    // Note, these DTOs don't cover all returned values, just the ones required for CSDE.
    private HoldingIdentityDTO holdingIdentity;
    private CpiIdentifierDTO cpiIdentifier;

    public VirtualNodeInfoDTO() {}

    public HoldingIdentityDTO getHoldingIdentity() { return holdingIdentity; }

    public void setHoldingIdentity(HoldingIdentityDTO holdingIdentity) { this.holdingIdentity = holdingIdentity; }

    public CpiIdentifierDTO getCpiIdentifier() { return cpiIdentifier; }

    public void setCpiIdentifier(CpiIdentifierDTO cpiIdentifier) { this.cpiIdentifier = cpiIdentifier; }
}
