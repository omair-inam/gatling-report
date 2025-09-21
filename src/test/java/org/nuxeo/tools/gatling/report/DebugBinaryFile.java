package org.nuxeo.tools.gatling.report;

import java.io.*;

public class DebugBinaryFile {
    public static void main(String[] args) throws Exception {
        File file = new File("simulation.log");
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            // Skip marker
            int marker = dis.readInt();
            System.out.println("Marker: " + marker);

            // Version length
            byte versionLen = dis.readByte();
            System.out.println("Version length: " + versionLen);

            // Version
            byte[] version = new byte[versionLen];
            dis.readFully(version);
            System.out.println("Version: " + new String(version));

            // Sim class length
            int simClassLen = dis.readInt();
            System.out.println("Sim class length: " + simClassLen);

            // Sim class
            byte[] simClass = new byte[simClassLen];
            dis.readFully(simClass);
            System.out.println("Sim class: " + new String(simClass));

            // Timestamp
            long timestamp = dis.readLong();
            System.out.println("Timestamp: " + timestamp);

            // Run desc length
            int descLen = dis.readInt();
            System.out.println("Run desc length: " + descLen);

            if (descLen > 0) {
                byte[] desc = new byte[descLen];
                dis.readFully(desc);
                System.out.println("Run desc: " + new String(desc));
            }

            // Scenario count
            int scenarioCount = dis.readInt();
            System.out.println("Scenario count: " + scenarioCount);

            for (int i = 0; i < scenarioCount; i++) {
                int scenarioLen = dis.readInt();
                System.out.println("Scenario " + i + " length: " + scenarioLen);
                if (scenarioLen > 0) {
                    byte[] scenario = new byte[scenarioLen];
                    dis.readFully(scenario);
                    System.out.println("Scenario " + i + ": " + new String(scenario));
                }
            }

            // Assertion count
            int assertionCount = dis.readInt();
            System.out.println("Assertion count: " + assertionCount);
        }
    }
}