package com.r3.csde;


/**
 * This class is a representation of a Vnode used to express the vNodes required on the network.
 */

public class VNode {
    private String x500Name;
    private String cpi;

    private String serviceX500Name;

    public VNode() { }

    public String getX500Name(){ return x500Name; }
    public void setX500Name(String _x500Name) { x500Name = _x500Name; }

    public String getCpi() { return cpi; }
    public void setCpi(String _cpi) { cpi = _cpi; }

    public String getServiceX500Name() { return serviceX500Name; }
    public void setServiceX500Name(String _name) { serviceX500Name = _name; }

}
