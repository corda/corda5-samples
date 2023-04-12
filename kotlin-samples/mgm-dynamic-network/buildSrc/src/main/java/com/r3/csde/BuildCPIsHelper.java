package com.r3.csde;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

// todo: This class needs refactoring, see https://r3-cev.atlassian.net/browse/CORE-11624
public class BuildCPIsHelper {

    public ProjectContext pc;
    public ProjectUtils utils;

    public NetworkConfig config;
    public BuildCPIsHelper(ProjectContext _pc, NetworkConfig _config) {
        pc = _pc;
        utils = new ProjectUtils(pc);
        config = _config;
    }

    public void createGroupPolicy() throws IOException {

        File groupPolicyFile = new File(String.format("%s/GroupPolicy.json", pc.devEnvWorkspace));
        File devnetFile = new File(pc.project.getRootDir() + "/" + config.getConfigFilePath());


        if (!groupPolicyFile.exists() || groupPolicyFile.lastModified() < devnetFile.lastModified()) {

            pc.out.println("createGroupPolicy: Creating a GroupPolicy");

            List<String> configX500Ids = config.getX500Names();
            LinkedList<String> commandList = new LinkedList<>();

            commandList.add(String.format("%s/java", pc.javaBinDir));
            commandList.add(String.format("-Dpf4j.pluginsDir=%s/plugins/", pc.cordaCliBinDir));
            commandList.add("-jar");
            commandList.add(String.format("%s/corda-cli.jar", pc.cordaCliBinDir));
            commandList.add("mgm");
            commandList.add("groupPolicy");
            for (String id : configX500Ids) {
                commandList.add("--name");
                commandList.add(id);
            }
            commandList.add("--endpoint-protocol=1");
            commandList.add("--endpoint=http://localhost:1080");

            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            // todo add exception catching
            FileWriter fileWriter = new FileWriter(groupPolicyFile);
            String line;
            while (( line = reader.readLine()) != null){
                fileWriter.write(line + "\n");
            }
            fileWriter.close();

        } else {
            pc.out.println("createPolicyTask: everything up to date; nothing to do.");
        }

    }

    public void createKeyStore() throws IOException, InterruptedException {

        File keystoreFile = new File(pc.keystoreFName);
        if(!keystoreFile.exists()) {
            pc.out.println("createKeystore: Create a keystore");

            generateKeyPair();
            addDefaultSigningKey();
            exportCert();

        } else {
            pc.out.println("createKeystore:  keystore already created; nothing to do.");
        }

    }

    private void generateKeyPair() throws IOException, InterruptedException {

        LinkedList<String> cmdArray = new LinkedList<>();

        cmdArray.add(pc.javaBinDir + "/keytool");
        cmdArray.add("-genkeypair");
        cmdArray.add("-alias");
        cmdArray.add(pc.keystoreAlias);
        cmdArray.add("-keystore");
        cmdArray.add(pc.keystoreFName);
        cmdArray.add("-storepass");
        cmdArray.add("keystore password");
        cmdArray.add("-dname");
        cmdArray.add("CN=CPI Example - My Signing Key, O=CorpOrgCorp, L=London, C=GB");
        cmdArray.add("-keyalg");
        cmdArray.add("RSA");
        cmdArray.add("-storetype");
        cmdArray.add("pkcs12");
        cmdArray.add("-validity");
        cmdArray.add("4000");

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

    }

    private void addDefaultSigningKey() throws IOException, InterruptedException {

        LinkedList<String> cmdArray = new LinkedList<>();

        cmdArray.add(pc.javaBinDir + "/keytool");
        cmdArray.add("-importcert");
        cmdArray.add("-keystore");
        cmdArray.add(pc.keystoreFName);
        cmdArray.add("-storepass");
        cmdArray.add("keystore password");
        cmdArray.add("-noprompt");
        cmdArray.add("-alias");
        cmdArray.add(pc.signingCertAlias);
        cmdArray.add("-file");
        cmdArray.add(pc.signingCertFName);

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();
    }

    private void exportCert() throws IOException, InterruptedException {

        LinkedList<String> cmdArray = new LinkedList<>();

        cmdArray.add(pc.javaBinDir + "/keytool");
        cmdArray.add("-exportcert");
        cmdArray.add("-rfc");
        cmdArray.add("-alias");
        cmdArray.add(pc.keystoreAlias);
        cmdArray.add("-keystore");
        cmdArray.add(pc.keystoreFName);
        cmdArray.add("-storepass");
        cmdArray.add("keystore password");
        cmdArray.add("-file");
        cmdArray.add(pc.keystoreCertFName);

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

    }

    public void buildCPIs() throws IOException, InterruptedException, CsdeException {
        createCorDappCPI();
        createNotaryCPI();
    }

    private void createCorDappCPI() throws IOException, InterruptedException, CsdeException {

        String appCPIFilePath = pc.workflowBuildDir + "/" +
                pc.project.getRootProject().getName() + "-" +
                pc.project.getVersion() + ".cpi";

        File appCPIFile = new File(appCPIFilePath);
        appCPIFile.delete();

        File srcDir = new File(pc.workflowBuildDir + "/libs");
        File[] appCPBs = srcDir.listFiles(( x , name ) -> name.endsWith(".cpb"));
        if (appCPBs == null) throw new CsdeException("Expecting exactly one CPB but no CPB found.");
        if (appCPBs.length != 1) throw new CsdeException("Expecting exactly one CPB but more than one found.");

        pc.out.println("appCpbs:");
        pc.out.println(appCPBs[0].getAbsolutePath());

        LinkedList<String> commandList = new LinkedList<>();

        commandList.add(String.format("%s/java", pc.javaBinDir));
        commandList.add(String.format("-Dpf4j.pluginsDir=%s/plugins/", pc.cordaCliBinDir));
        commandList.add("-jar");
        commandList.add(String.format("%s/corda-cli.jar", pc.cordaCliBinDir));
        commandList.add("package");
        commandList.add("create-cpi");
        commandList.add("--cpb");
        commandList.add(appCPBs[0].getAbsolutePath());
        commandList.add("--group-policy");
        commandList.add(pc.devEnvWorkspace + "/GroupPolicy.json");
        commandList.add("--cpi-name");
        commandList.add(pc.appCPIName);
        commandList.add("--cpi-version");
        commandList.add(pc.project.getVersion().toString());
        commandList.add("--file");
        commandList.add(appCPIFilePath);
        commandList.add("--keystore");
        commandList.add(pc.devEnvWorkspace + "/signingkeys.pfx");
        commandList.add("--storepass");
        commandList.add("keystore password");
        commandList.add("--key");
        commandList.add("my-signing-key"); // todo: should be passed as context property

        ProcessBuilder pb = new ProcessBuilder(commandList);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

// todo: work out how to capture error code better than the following code

//        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//        File tempOutputFile = new File(String.format("%s/tempOutput.txt", pc.devEnvWorkspace));
//        tempOutputFile.delete();
//        FileWriter fileWriter = new FileWriter(tempOutputFile);
//        String line;
//        while (( line = reader.readLine()) != null){
//            fileWriter.write(line + "\n");
//        }
//        fileWriter.close();

    }

    private void createNotaryCPI() throws CsdeException, IOException, InterruptedException {

        String notaryCPIFilePath = pc.workflowBuildDir + "/" +
                pc.notaryCPIName.replace(' ', '-').toLowerCase() + "-" +
                pc.project.getVersion() + ".cpi";

        File notaryCPIFile = new File(notaryCPIFilePath);
        notaryCPIFile.delete();

        File srcDir = new File(pc.cordaNotaryServiceDir);
        File[] notaryCPBs = srcDir.listFiles(( x , name ) -> name.endsWith(".cpb") && name.contains(pc.cordaNotaryPluginsVersion));
        if (notaryCPBs == null) throw new CsdeException("Expecting exactly one notary CPB but no CPB found.");
        if (notaryCPBs.length != 1) throw new CsdeException("Expecting exactly one notary CPB but more than one found.");

        pc.out.println("notaryCpbs:");
        pc.out.println(notaryCPBs[0]);

        LinkedList<String> commandList = new LinkedList<>();

        commandList.add(String.format("%s/java", pc.javaBinDir));
        commandList.add(String.format("-Dpf4j.pluginsDir=%s/plugins/", pc.cordaCliBinDir));
        commandList.add("-jar");
        commandList.add(String.format("%s/corda-cli.jar", pc.cordaCliBinDir));
        commandList.add("package");
        commandList.add("create-cpi");
        commandList.add("--cpb");
        commandList.add(notaryCPBs[0].getAbsolutePath());
        commandList.add("--group-policy");
        commandList.add(pc.devEnvWorkspace + "/GroupPolicy.json");
        commandList.add("--cpi-name");
        commandList.add(pc.notaryCPIName);
        commandList.add("--cpi-version");
        commandList.add(pc.project.getVersion().toString());
        commandList.add("--file");
        commandList.add(notaryCPIFilePath);
        commandList.add("--keystore");
        commandList.add(pc.devEnvWorkspace + "/signingkeys.pfx");
        commandList.add("--storepass");
        commandList.add("keystore password");
        commandList.add("--key");
        commandList.add("my-signing-key");

        ProcessBuilder pb = new ProcessBuilder(commandList);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

    }

    // todo: this might be needed for improved logging
    private void printCmdArray(LinkedList<String> cmdArray) {
        for (int i = 0; i < cmdArray.size(); i++) {
            pc.out.print(cmdArray.get(i) + " ");
        }
    }

}
