package com.r3.csde;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

// todo: This class needs refactoring, see https://r3-cev.atlassian.net/browse/CORE-11624
public class CordaStatusQueries {

    ProjectContext pc;

    public CordaStatusQueries(ProjectContext _pc) {
        pc = _pc;
    }

    public HttpResponse<JsonNode> getCpiInfo() {
        Unirest.config().verifySsl(false);
        return Unirest.get(pc.baseURL + "/api/v1/cpi/")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();
    }

    public HttpResponse<JsonNode> getVNodeInfo() {
        Unirest.config().verifySsl(false);
        return Unirest.get(pc.baseURL + "/api/v1/virtualnode/")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();
    }

    // cpiName, cpiVersion
    public void listCPIs() {
        HttpResponse<JsonNode> cpiResponse = getCpiInfo();

        JSONArray cpisJson = (JSONArray) cpiResponse.getBody().getObject().get("cpis");

        List<List<String>> lines = new LinkedList<>();
        for (Object o : cpisJson) {
            if (o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("id");
                String cpiName = idObj.get("cpiName").toString();
                String cpiVersion = idObj.get("cpiVersion").toString();

                lines.add(Arrays.asList(cpiName, cpiVersion));
            }
        }
        List<String> title = Arrays.asList("CPI Name", "CPI Version");
        List<Integer> titleSizes = Arrays.asList(40, 20);
        printTable(titleSizes, title, lines);
    }

    public void listVNodesVerbose() {
        HttpResponse<JsonNode> vnodeResponse = getVNodeInfo();
        pc.out.println("VNodes:\n" + vnodeResponse.getBody().toPrettyString());
    }

    // x500Name, shortHash, cpiName
    public void listVNodes() {
        HttpResponse<JsonNode> vnodeResponse = getVNodeInfo();

        JSONArray virtualNodesJson = (JSONArray) vnodeResponse.getBody().getObject().get("virtualNodes");

        List<List<String>> lines = new LinkedList<>();
        for (Object o : virtualNodesJson) {
            if (o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("holdingIdentity");
                String x500Name = idObj.get("x500Name").toString();
                String shortHash = idObj.get("shortHash").toString();

                JSONObject cpiObj = ((JSONObject) o).getJSONObject("cpiIdentifier");
                String cpiName = cpiObj.get("cpiName").toString();

                lines.add(Arrays.asList(x500Name, shortHash, cpiName));
            }
        }
        List<String> title = Arrays.asList("X500 Name", "Holding identity short hash", "CPI Name");
        List<Integer> titleSizes = Arrays.asList(60, 30, 40);
        printTable(titleSizes, title, lines);
    }

    public void printTable(List<Integer> titleSizes, List<String> title, List<List<String>> lines) {
        int width = titleSizes.stream().reduce(0, Integer::sum);
        String separator = "-".repeat(width + 1);
        pc.out.println(separator);
        pc.out.println(formatLine(titleSizes, title));
        pc.out.println(separator);
        for (List<String> line : lines) {
            pc.out.println(formatLine(titleSizes, line));
        }
        pc.out.println(separator);
    }

    public String formatLine(List<Integer> titleSizes, List<String> line) {
        StringBuilder sb = new StringBuilder();
        int delta = 0;
        for (int i = 0; i < titleSizes.size(); i++) {
            String s = line.get(i);
            sb.append("| ").append(s);
            delta += titleSizes.get(i) - (2 + s.length());

            if (delta > 0) {
                sb.append(" ".repeat(delta));
                delta = 0;
            } else {
                sb.append(" ");
                delta -= 1;
            }
        }
        sb.append("|");
        return sb.toString();
    }
}
