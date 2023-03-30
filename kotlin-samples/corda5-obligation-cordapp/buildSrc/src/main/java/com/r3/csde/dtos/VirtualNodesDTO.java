package com.r3.csde.dtos;

import com.r3.csde.dtos.VirtualNodeInfoDTO;

import java.util.List;

public class VirtualNodesDTO {

    private List<VirtualNodeInfoDTO> virtualNodes;

    public VirtualNodesDTO() {}

    public List<VirtualNodeInfoDTO> getVirtualNodes() { return virtualNodes; }

    public void setVirtualNodes(List<VirtualNodeInfoDTO> virtualNodes) { this.virtualNodes = virtualNodes; }
}
