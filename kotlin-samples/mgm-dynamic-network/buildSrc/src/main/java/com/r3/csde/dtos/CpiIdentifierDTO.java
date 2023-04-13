package com.r3.csde.dtos;

public class CpiIdentifierDTO {

    // Note, these DTOs don't cover all returned values, just the ones required for CSDE
    private String cpiName;
    private String cpiVersion;
    private String signerSummaryHash;

    public CpiIdentifierDTO() { }

    public String getCpiName() { return cpiName; }

    public void setCpiName(String cpiName) { this.cpiName = cpiName; }

    public String getCpiVersion() { return cpiVersion; }

    public void setCpiVersion(String cpiVersion) { this.cpiVersion = cpiVersion; }

    public String getSignerSummaryHash() { return signerSummaryHash; }

    public void setSignerSummaryHash(String signerSummaryHash) { this.signerSummaryHash = signerSummaryHash; }
}
