package com.r3.csde;

import kong.unirest.Unirest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Manages Bringing corda up, testing for liveness and taking corda down
 */
// todo: This class needs refactoring, see https://r3-cev.atlassian.net/browse/CORE-11624
public class CordaLifeCycleHelper {

    ProjectContext pc;
    ProjectUtils utils;

    public CordaLifeCycleHelper(ProjectContext _pc) {
        pc = _pc;
        utils = new ProjectUtils(pc);
        Unirest.config().verifySsl(false);
    }

    public void startCorda() throws IOException, CsdeException {
        File cordaPIDFile = new File(pc.cordaPidCache);
        if (cordaPIDFile.exists()) {
            throw new CsdeException("Cannot start the Combined worker. Cached process ID file " + cordaPIDFile + " existing. Was the combined worker already started?");
        }
        PrintStream pidStore = new PrintStream(new FileOutputStream(cordaPIDFile));
        File combinedWorkerJar = pc.project.getConfigurations().getByName("combinedWorker").getSingleFile();

        // Manual version of the command to start postgres (for reference):
        // docker run -d --rm -p5432:5432 --name CSDEpostgresql -e POSTGRES_DB=cordacluster -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password postgres:latest

        new ProcessBuilder(
                "docker",
                "run", "-d", "--rm",
                "-p", "5432:5432",
                "--name", pc.dbContainerName,
                "-e", "POSTGRES_DB=cordacluster",
                "-e", "POSTGRES_USER=postgres",
                "-e", "POSTGRES_PASSWORD=password",
                "postgres:latest").start();

        // todo: we should poll for readiness not wait 10 seconds, see https://r3-cev.atlassian.net/browse/CORE-11626
        utils.rpcWait(10000);

        ProcessBuilder procBuild = new ProcessBuilder(pc.javaBinDir + "/java",
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
                "-DsecurityMangerEnabled=false",
                "-Dlog4j.configurationFile=" + pc.project.getRootDir() + "/config/log4j2.xml",
                "-Dco.paralleluniverse.fibers.verifyInstrumentation=true",
                "-jar",
                combinedWorkerJar.toString(),
                "--instance-id=0",
                "-mbus.busType=DATABASE",
                "-spassphrase=password",
                "-ssalt=salt",
                "-ddatabase.user=user",
                "-ddatabase.pass=password",
                "-ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster",
                "-ddatabase.jdbc.directory="+pc.JDBCDir);

        procBuild.redirectErrorStream(true);
        Process proc = procBuild.start();
        pidStore.print(proc.pid());
        pc.out.println("Corda Process-id="+proc.pid());
        proc.getInputStream().transferTo(pc.out);

        // todo: we should poll for readiness before completing the startCorda task, see https://r3-cev.atlassian.net/browse/CORE-11625
    }


    public void stopCorda() throws IOException, CsdeException {
        File cordaPIDFile = new File(pc.cordaPidCache);
        if(cordaPIDFile.exists()) {
            Scanner sc = new Scanner(cordaPIDFile);
            long pid = sc.nextLong();
            pc.out.println("pid to kill=" + pid);

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("Powershell", "-Command", "Stop-Process", "-Id", Long.toString(pid), "-PassThru").start();
            } else {
                new ProcessBuilder("kill", "-9", Long.toString(pid)).start();
            }

            Process proc = new ProcessBuilder("docker", "stop", pc.dbContainerName).start();

            cordaPIDFile.delete();
        }
        else {
            throw new CsdeException("Cannot stop the Combined worker. Cached process ID file " + pc.cordaPidCache + " missing. Was the combined worker not started?");
        }
    }
}
