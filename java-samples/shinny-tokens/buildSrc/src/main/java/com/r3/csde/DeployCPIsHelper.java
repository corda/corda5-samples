package com.r3.csde;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import kong.unirest.HttpResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;

// todo: This class needs refactoring, see https://r3-cev.atlassian.net/browse/CORE-11624
public class DeployCPIsHelper {

    public DeployCPIsHelper() {
    }
    ProjectContext pc;
    CordaStatusQueries queries;
    ProjectUtils utils;

    public DeployCPIsHelper(ProjectContext _pc) {
        pc = _pc;
        queries = new CordaStatusQueries(pc);
        utils = new ProjectUtils(pc);
    }

    public void deployCPIs() throws FileNotFoundException, CsdeException{

        uploadCertificate(pc.signingCertAlias, pc.signingCertFName);
        uploadCertificate(pc.keystoreAlias, pc.keystoreCertFName);

        // todo: make consistent with other string building code - remove String.format
        String appCPILocation = String.format("%s/%s-%s.cpi",
                pc.workflowBuildDir,
                pc.project.getName(),
                pc.project.getVersion());
        deployCPI(appCPILocation, pc.appCPIName,pc.project.getVersion().toString());

        String notaryCPILocation = String.format("%s/%s-%s.cpi",
                pc.workflowBuildDir,
                pc.notaryCPIName.replace(' ','-').toLowerCase(),
                pc.project.getVersion());
        deployCPI(notaryCPILocation,
                pc.notaryCPIName,
                pc.project.getVersion().toString(),
                "-NotaryServer" );

    }

    public void uploadCertificate(String certAlias, String certFName) {
        Unirest.config().verifySsl(false);
        HttpResponse<JsonNode> uploadResponse = Unirest.put(pc.baseURL + "/api/v1/certificates/cluster/code-signer")
                .field("alias", certAlias)
                .field("certificate", new File(certFName))
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();
        pc.out.println("Certificate/key upload, alias "+certAlias+" certificate/key file "+certFName);
        pc.out.println(uploadResponse.getBody().toPrettyString());
    }

    public void forceuploadCPI(String cpiFName) throws FileNotFoundException, CsdeException {
        forceuploadCPI(cpiFName, "");
    }

    public void forceuploadCPI(String cpiFName, String uploadStatusQualifier) throws FileNotFoundException, CsdeException {
        Unirest.config().verifySsl(false);
        HttpResponse<JsonNode> jsonResponse = Unirest.post(pc.baseURL + "/api/v1/maintenance/virtualnode/forcecpiupload/")
                .field("upload", new File(cpiFName))
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(jsonResponse.getStatus() == HTTP_OK) {
            String id = (String) jsonResponse.getBody().getObject().get("id");
            pc.out.println("get id:\n" +id);
            HttpResponse<JsonNode> statusResponse = uploadStatus(id);

            if (statusResponse.getStatus() == HTTP_OK) {
                PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(
                        pc.CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json" )));
                cpiUploadStatus.print(statusResponse.getBody());
                pc.out.println("Caching CPI file upload status:\n" + statusResponse.getBody());
            } else {
                utils.reportError(statusResponse);
            }
        }
        else {
            utils.reportError(jsonResponse);
        }
    }

    private boolean uploadStatusRetry(HttpResponse<JsonNode> response) {
        int status = response.getStatus();
        JsonNode body = response.getBody();
        // Do not retry on success // todo: need to think through the possible outcomes here - what if the bodyTitle is null, it won't retry
        if(status == HTTP_OK) {
            // Keep retrying until we get "OK" may move through "Validating upload", "Persisting CPI"
            return !(body.getObject().get("status").equals("OK"));
        }
        else if (status == HTTP_BAD_REQUEST){
            String bodyTitle = response.getBody().getObject().getString("title");
            return bodyTitle != null && bodyTitle.matches("No such requestId=[-0-9a-f]+");
        }
        return false;
    }

    public HttpResponse<JsonNode> uploadStatus(String requestId) {
        HttpResponse<JsonNode> statusResponse = null;
        do {
            utils.rpcWait(1000);
            statusResponse = Unirest
                    .get(pc.baseURL + "/api/v1/cpi/status/" + requestId + "/")
                    .basicAuth(pc.rpcUser, pc.rpcPasswd)
                    .asJson();
            pc.out.println("Upload status="+statusResponse.getStatus()+", status query response:\n"+statusResponse.getBody().toPrettyString());
        }
        while(uploadStatusRetry(statusResponse));

        return statusResponse;
    }

    public void deployCPI(String cpiFName, String cpiName, String cpiVersion) throws FileNotFoundException, CsdeException {
        deployCPI(cpiFName, cpiName, cpiVersion, "");
    }

    public void deployCPI(String cpiFName,
                          String cpiName,
                          String cpiVersion,
                          String uploadStatusQualifier) throws FileNotFoundException, CsdeException {
        // todo: where is the primary instance declared?
        Unirest.config().verifySsl(false);

        HttpResponse<JsonNode> cpiResponse  = queries.getCpiInfo();
        JSONArray jArray = (JSONArray) cpiResponse.getBody().getObject().get("cpis");

        int matches = 0;
        for(Object o: jArray.toList() ) {
            if(o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("id");
                if((idObj.get("cpiName").toString().equals(cpiName)
                        && idObj.get("cpiVersion").toString().equals(cpiVersion))) {
                    matches++;
                }
            }
        }
        pc.out.println("Matching CPIS="+matches);

        if(matches == 0) {
            HttpResponse<JsonNode> uploadResponse = Unirest.post(pc.baseURL + "/api/v1/cpi/")
                    .field("upload", new File(cpiFName))
                    .basicAuth(pc.rpcUser, pc.rpcPasswd)
                    .asJson();

            JsonNode body = uploadResponse.getBody();

            int status = uploadResponse.getStatus();

            pc.out.println("Upload Status:" + status);
            pc.out.println("Pretty print the body\n" + body.toPrettyString());

            // We expect the id field to be a string.
            if (status == HTTP_OK) {
                String id = (String) body.getObject().get("id");
                pc.out.println("get id:\n" + id);

                HttpResponse<JsonNode> statusResponse = uploadStatus(id);
                if (statusResponse.getStatus() == HTTP_OK) {
                    PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(
                            pc.CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json" )));
                    cpiUploadStatus.print(statusResponse.getBody());
                    pc.out.println("Caching CPI file upload status:\n" + statusResponse.getBody());
                } else {
                    utils.reportError(statusResponse);
                }
            } else {
                utils.reportError(uploadResponse);
            }
        }
        else {
            pc.out.println("CPI already uploaded doing a 'force' upload.");
            forceuploadCPI(cpiFName, uploadStatusQualifier);
        }
    }

}
