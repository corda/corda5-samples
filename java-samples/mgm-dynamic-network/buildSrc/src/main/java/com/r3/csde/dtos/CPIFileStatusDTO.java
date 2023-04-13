package com.r3.csde.dtos;

public class CPIFileStatusDTO {
    private String status;
    private String cpiFileChecksum;

    public CPIFileStatusDTO() {}

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getCpiFileChecksum() { return cpiFileChecksum; }

    public void setCpiFileChecksum(String cpiFileChecksum) { this.cpiFileChecksum = cpiFileChecksum; }
}
