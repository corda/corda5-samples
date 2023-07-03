package com.r3.csde;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;

import static java.lang.Thread.sleep;

// todo: This class needs refactoring, see https://r3-cev.atlassian.net/browse/CORE-11624
public class ProjectUtils {

    ProjectContext pc;

    ProjectUtils(ProjectContext _pc) {
        pc = _pc;
    }

    void rpcWait(int millis) {
        try {
            sleep(millis);
        }
        catch(InterruptedException e) {
            throw new UnsupportedOperationException("Interrupts not supported.", e);
        }
    }

    public void reportError(HttpResponse<JsonNode> response) throws CsdeException {

        pc.out.println("*** *** ***");
        pc.out.println("Unexpected response from Corda");
        pc.out.println("Status="+ response.getStatus());
        pc.out.println("*** Headers ***\n"+ response.getHeaders());
        pc.out.println("*** Body ***\n"+ response.getBody());
        pc.out.println("*** *** ***");
        throw new CsdeException("Error: unexpected response from Corda.");
    }
}
