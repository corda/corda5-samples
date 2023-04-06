package com.r3.csde.dtos;

public class CpiMetadataDTO {
    // Note, these DTOs don't cover all returned values, just the ones required for CSDE.
    private String cpiFileChecksum;
    private CpiIdentifierDTO id;

    public CpiMetadataDTO() {}

    public String getCpiFileChecksum() { return cpiFileChecksum; }

    public void setCpiFileChecksum(String cpiFileChecksum) { this.cpiFileChecksum = cpiFileChecksum; }

    public CpiIdentifierDTO getId() { return id; }

    public void setId(CpiIdentifierDTO id) { this.id = id; }
}
