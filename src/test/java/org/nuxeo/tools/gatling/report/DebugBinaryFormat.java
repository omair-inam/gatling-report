package org.nuxeo.tools.gatling.report;

import java.io.*;

public class DebugBinaryFormat {
    public static void main(String[] args) throws Exception {
        File file = new File("simulation.log");
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            // Skip marker
            int marker = dis.readInt();
            System.out.printf("Position 0x00-0x03: Marker = %d (0x%08X)\n", marker, marker);

            // Version length
            byte versionLen = dis.readByte();
            System.out.printf("Position 0x04: Version length = %d\n", versionLen);

            // Version string
            byte[] version = new byte[versionLen];
            dis.readFully(version);
            System.out.printf("Position 0x05-0x%02X: Version = %s\n", 4 + versionLen, new String(version));

            // Read next 4 bytes to see what they are
            System.out.printf("\nPosition 0x%02X: Next 4 bytes:\n", 5 + versionLen);
            for (int i = 0; i < 4; i++) {
                byte b = dis.readByte();
                System.out.printf("  Byte %d: 0x%02X (%d)\n", i, b, b);
            }

            // Now try to read string length
            int strLen = dis.readInt();
            System.out.printf("\nPosition 0x%02X-0x%02X: String length = %d (0x%08X)\n",
                5 + versionLen + 4, 5 + versionLen + 7, strLen, strLen);

            // Read string
            if (strLen > 0 && strLen < 1000) {
                byte[] strBytes = new byte[strLen];
                dis.readFully(strBytes);
                System.out.printf("String value = %s\n", new String(strBytes));

                // Read coder byte
                byte coder = dis.readByte();
                System.out.printf("Coder byte = 0x%02X\n", coder);

                // Read timestamp
                long timestamp = dis.readLong();
                System.out.printf("Timestamp = %d\n", timestamp);

                // Read next string length
                int nextStrLen = dis.readInt();
                System.out.printf("Next string length = %d\n", nextStrLen);

                if (nextStrLen > 0 && nextStrLen < 1000) {
                    byte[] nextStr = new byte[nextStrLen];
                    dis.readFully(nextStr);
                    System.out.printf("Next string = %s\n", new String(nextStr));

                    byte nextCoder = dis.readByte();
                    System.out.printf("Next coder = 0x%02X\n", nextCoder);
                }
            }
        }
    }
}