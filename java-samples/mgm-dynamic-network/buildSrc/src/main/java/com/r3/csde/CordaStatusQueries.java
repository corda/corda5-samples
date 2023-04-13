package com.r3.csde;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import kong.unirest.HttpResponse;

// todo: This class needs refactoring, see https://r3-cev.atlassian.net/browse/CORE-11624
public class CordaStatusQueries {

    ProjectContext pc;
    public CordaStatusQueries(ProjectContext _pc){ pc = _pc; }


    public HttpResponse<JsonNode> getVNodeInfo() {
        Unirest.config().verifySsl(false);
        return Unirest.get(pc.baseURL + "/api/v1/virtualnode/")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();
    }
    public void listVNodesVerbose() {
        HttpResponse<JsonNode> vnodeResponse = getVNodeInfo();
        pc.out.println("VNodes:\n" + vnodeResponse.getBody().toPrettyString());
    }

    // X500Name, shorthash, cpiname
    public void listVNodes() {
        HttpResponse<JsonNode> vnodeResponse = getVNodeInfo();

        JSONArray virtualNodesJson = (JSONArray) vnodeResponse.getBody().getObject().get("virtualNodes");
        pc.out.println("X500 Name\tHolding identity short hash\tCPI Name");
        for(Object o: virtualNodesJson){
            if(o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("holdingIdentity");
                JSONObject cpiObj = ((JSONObject) o).getJSONObject("cpiIdentifier");
                pc.out.print("\"" + idObj.get("x500Name") + "\"");
                pc.out.print("\t\"" + idObj.get("shortHash") + "\"");
                pc.out.println("\t\"" + cpiObj.get("cpiName") + "\"");
            }
        }
    }

    public HttpResponse<JsonNode> getCpiInfo() {
        Unirest.config().verifySsl(false);
        return Unirest.get(pc.baseURL + "/api/v1/cpi/")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();
    }

    public void listCPIs() {
        HttpResponse<JsonNode> cpiResponse  = getCpiInfo();
        JSONArray jArray = (JSONArray) cpiResponse.getBody().getObject().get("cpis");

        for(Object o: jArray){
            if(o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("id");
                pc.out.print("cpiName=" + idObj.get("cpiName"));
                pc.out.println(", cpiVersion=" + idObj.get("cpiVersion"));
            }
        }
    }

}
