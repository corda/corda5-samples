package com.r3.csde.dtos;

import java.util.List;

public class GetCPIsResponseDTO {

    private List<CpiMetadataDTO> cpis;

    public GetCPIsResponseDTO() {}

    public List<CpiMetadataDTO> getCpis() { return cpis; }

    public void setCpis(List<CpiMetadataDTO> cpis) { this.cpis = cpis; }
}
