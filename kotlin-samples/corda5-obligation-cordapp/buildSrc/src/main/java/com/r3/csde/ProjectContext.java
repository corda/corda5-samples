package com.r3.csde;

import org.gradle.api.Project;
import java.io.PrintStream;
import java.util.Map;

public class ProjectContext {
    Project project;
    String baseURL = "https://localhost:8888";
    String rpcUser = "admin";
    String rpcPasswd = "admin";
     String workspaceDir = "workspace";
    int retryWaitMs = 1000;
    PrintStream out = System.out;
    String CPIUploadStatusBaseName = "CPIFileStatus.json";
    String NotaryCPIUploadBaseName = "CPIFileStatus-NotaryServer.json";
    String CPIUploadStatusFName;
    String NotaryCPIUploadStatusFName;
    String javaBinDir;
    String cordaPidCache = "CordaPIDCache.dat";
    String dbContainerName;
    String JDBCDir;
    String combinedWorkerBinRe;
    Map<String, String> notaryRepresentatives = null;
    String signingCertAlias;
    String signingCertFName;
    String keystoreAlias;
    String keystoreFName;
    String keystoreCertFName;
    String appCPIName;
    String notaryCPIName;
    String devEnvWorkspace;
    String cordaCliBinDir;
    String cordaNotaryServiceDir;
    String workflowBuildDir;
    String cordaNotaryPluginsVersion;

    public ProjectContext (Project inProject,
                           String inBaseUrl,
                           String inRpcUser,
                           String inRpcPasswd,
                           String inWorkspaceDir,
                           String inJavaBinDir,
                           String inDbContainerName,
                           String inJDBCDir,
                           String inCordaPidCache,
                           String inSigningCertAlias,
                           String inSigningCertFName,
                           String inKeystoreAlias,
                           String inKeystoreFName,
                           String inKeystoreCertFName,
                           String inAppCPIName,
                           String inNotaryCPIName,
                           String inDevEnvWorkspace,
                           String inCordaCLiBinDir,
                           String inCordaNotaryServiceDir,
                           String inWorkflowBuildDir,
                           String inCordaNotaryPluginsVersion
    ) {
        project = inProject;
        baseURL = inBaseUrl;
        rpcUser = inRpcUser;
        rpcPasswd = inRpcPasswd;
        workspaceDir = inWorkspaceDir;
        javaBinDir = inJavaBinDir;
        cordaPidCache = inCordaPidCache;
        dbContainerName = inDbContainerName;
        JDBCDir = inJDBCDir;
        CPIUploadStatusFName = workspaceDir + "/" + CPIUploadStatusBaseName;
        NotaryCPIUploadStatusFName = workspaceDir + "/" + NotaryCPIUploadBaseName;
        signingCertAlias = inSigningCertAlias;
        signingCertFName = inSigningCertFName;
        keystoreAlias = inKeystoreAlias;
        keystoreFName = inKeystoreFName;
        keystoreCertFName = inKeystoreCertFName;
        appCPIName = inAppCPIName;
        notaryCPIName = inNotaryCPIName;
        devEnvWorkspace = inDevEnvWorkspace;
        cordaCliBinDir = inCordaCLiBinDir;
        cordaNotaryServiceDir = inCordaNotaryServiceDir;
        workflowBuildDir = inWorkflowBuildDir;
        cordaNotaryPluginsVersion = inCordaNotaryPluginsVersion;
     }
}
