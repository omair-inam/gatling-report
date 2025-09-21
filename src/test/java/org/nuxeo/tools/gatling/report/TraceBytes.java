package org.nuxeo.tools.gatling.report;

import java.io.*;

public class TraceBytes {
    public static void main(String[] args) throws Exception {
        File file = new File("simulation.log");
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            // Skip to position 0x0B (after marker, version length, and version string)
            dis.skipNBytes(0x0B);

            System.out.println("Starting from position 0x0B:");
            for (int pos = 0x0B; pos < 0x70; pos++) {
                byte b = dis.readByte();
                if (b >= 32 && b <= 126) {
                    System.out.printf("Position 0x%02X: 0x%02X (%3d) '%c'\n", pos, b, b, (char)b);
                } else {
                    System.out.printf("Position 0x%02X: 0x%02X (%3d)\n", pos, b, b);
                }
            }
        }
    }
}