package com.r3.csde.dtos;

public class HoldingIdentityDTO {
    // Note, these DTOs don't cover all returned values, just the ones required for CSDE.
    private String fullHash;
    private String groupId;
    private String shortHash;
    private String x500Name;

    public HoldingIdentityDTO() {}

    public String getFullHash() { return fullHash; }

    public void setFullHash(String fullHash) { this.fullHash = fullHash; }

    public String getGroupId() { return groupId; }

    public void setGroupID(String groupID) { this.groupId = groupId; }

    public String getShortHash() { return shortHash; }

    public void setShortHash(String shortHash) { this.shortHash = shortHash; }

    public String getX500Name() { return x500Name; }

    public void setX500Name(String x500Name) { this.x500Name = x500Name; }
}
