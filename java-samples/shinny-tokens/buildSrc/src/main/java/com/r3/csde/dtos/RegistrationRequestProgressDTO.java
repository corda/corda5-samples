package com.r3.csde.dtos;

public class RegistrationRequestProgressDTO {
    // Note, these DTOs don't cover all returned values, just the ones required for CSDE.
    private String registrationStatus;
    private String reason;

    public RegistrationRequestProgressDTO() {}

    public String getRegistrationStatus() { return registrationStatus; }

    public void setRegistrationStatus(String registrationStatus) { this.registrationStatus = registrationStatus; }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }
}
