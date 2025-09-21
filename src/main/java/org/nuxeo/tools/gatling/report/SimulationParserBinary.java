/*
 * (C) Copyright 2024 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.nuxeo.tools.gatling.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for binary format simulation log files (Gatling 3.14.3+)
 */
public class SimulationParserBinary extends SimulationParser {

    // Record headers from Gatling's RecordHeader
    private static final byte RUN_HEADER = 0;
    private static final byte REQUEST_HEADER = 1;
    private static final byte USER_HEADER = 2;
    private static final byte GROUP_HEADER = 3;
    private static final byte ERROR_HEADER = 4;

    private String[] scenarios;
    private long runStart;

    public SimulationParserBinary(File file, Float apdexT) {
        super(file, apdexT);
    }

    public SimulationParserBinary(File file) {
        super(file);
    }

    @Override
    public SimulationContext parse() throws IOException {
        SimulationContext context = new SimulationContext(file.getAbsolutePath(), apdexT);

        try (BinarySimulationReader reader = new BinarySimulationReader(file)) {
            // Skip binary format marker (4 zero bytes)
            reader.skipInt();

            // The next byte after the marker could be:
            // - The version string length (if no explicit Run header)
            // - The Run record header (0)
            // In the actual binary file, the version comes immediately after the marker

            // Read version length byte
            byte versionLengthByte = reader.readByte();
            int versionLength = versionLengthByte & 0xFF;

            // Read version string (no coder byte)
            byte[] versionBytes = new byte[versionLength];
            for (int i = 0; i < versionLength; i++) {
                versionBytes[i] = reader.readByte();
            }
            String version = new String(versionBytes);

            // Read the RUN record header byte (should be 0)
            byte runHeader = reader.readByte();
            if (runHeader != RUN_HEADER) {
                throw new IOException("Expected RUN record header but got: " + runHeader);
            }

            // Now parse the Run record using standard format (with coder bytes)
            // The next bytes are the string length as a 4-byte int (00 00 00 49)
            parseRunRecordAfterVersion(reader, context, version);

            // Parse remaining records
            while (reader.hasMore()) {
                byte header = reader.readByte();
                switch (header) {
                    case REQUEST_HEADER:
                        parseRequestRecord(reader, context);
                        break;
                    case USER_HEADER:
                        parseUserRecord(reader, context);
                        break;
                    case GROUP_HEADER:
                        parseGroupRecord(reader, context);
                        break;
                    case ERROR_HEADER:
                        parseErrorRecord(reader, context);
                        break;
                    default:
                        throw new IOException("Unknown record header: " + header);
                }
            }
        }

        context.computeStat();
        return context;
    }

    private void parseRunRecordWithoutCoder(BinarySimulationReader reader, SimulationContext context, String gatlingVersion) throws IOException {
        // Read strings as raw bytes without coder
        // Simulation class name - skip the padding zeros first
        reader.skipInt(); // Skip 00 00 00 00

        // Now read the actual string length (1 byte in this format)
        int simClassLength = reader.readByte() & 0xFF;
        byte[] simClassBytes = new byte[simClassLength];
        for (int i = 0; i < simClassLength; i++) {
            simClassBytes[i] = reader.readByte();
        }
        String simulationClassName = new String(simClassBytes);
        context.setSimulationName(simulationClassName);

        // Read padding byte after string
        reader.readByte(); // Skip the 00 byte after the string

        // Start timestamp (8 bytes)
        runStart = reader.readLong();
        context.setStart(runStart);

        // Skip padding
        reader.skipInt(); // Skip 00 00 00 01

        // Run description length (1 byte)
        int descLength = reader.readByte() & 0xFF;
        String runDescription = "";
        if (descLength > 0) {
            byte[] descBytes = new byte[descLength];
            for (int i = 0; i < descLength; i++) {
                descBytes[i] = reader.readByte();
            }
            runDescription = new String(descBytes);
        }
        context.setScenarioName(runDescription.isEmpty() ? simulationClassName : runDescription);

        // Skip padding
        reader.skipInt(); // Skip 00 00 00 00

        // Number of scenarios (no scenarios in this file)
        // Will continue parsing as needed
    }

    private void parseRunRecord(BinarySimulationReader reader, SimulationContext context) throws IOException {
        // This method is only used for test files with explicit Run header
        // Gatling version
        String gatlingVersion = reader.readString();

        parseRunRecordAfterVersion(reader, context, gatlingVersion);
    }

    private void parseRunRecordAfterVersionDirect(BinarySimulationReader reader, SimulationContext context, String gatlingVersion) throws IOException {
        // In the actual binary format, strings are stored as:
        // 4-byte length + raw bytes (no coder byte)

        try {
            // Simulation class name
            int simClassLength = reader.readInt();
            byte[] simClassBytes = new byte[simClassLength];
            for (int i = 0; i < simClassLength; i++) {
                simClassBytes[i] = reader.readByte();
            }
            String simulationClassName = new String(simClassBytes);
            context.setSimulationName(simulationClassName);

            // Start timestamp
            runStart = reader.readLong();
            context.setStart(runStart);

            // Run description
            int descLength = reader.readInt();
            String runDescription = "";
            if (descLength > 0 && descLength < 10000) {  // Sanity check
                byte[] descBytes = new byte[descLength];
                for (int i = 0; i < descLength; i++) {
                    descBytes[i] = reader.readByte();
                }
                runDescription = new String(descBytes);
            }
            context.setScenarioName(runDescription.isEmpty() ? simulationClassName : runDescription);

            // Number of scenarios
            int scenarioCount = reader.readInt();
            scenarios = new String[scenarioCount];
            for (int i = 0; i < scenarioCount; i++) {
                int scenarioLength = reader.readInt();
                if (scenarioLength > 0) {
                    byte[] scenarioBytes = new byte[scenarioLength];
                    for (int j = 0; j < scenarioLength; j++) {
                        scenarioBytes[j] = reader.readByte();
                    }
                    scenarios[i] = new String(scenarioBytes);
                } else {
                    scenarios[i] = "";
                }
                // Initialize user tracking for each scenario
                context.addUser(scenarios[i]);
                context.endUser(scenarios[i]);
            }

            // Number of assertions
            int assertionCount = reader.readInt();
            for (int i = 0; i < assertionCount; i++) {
                // Skip assertion bytes
                int assertionBytesLength = reader.readInt();
                reader.skip(assertionBytesLength);
            }
        } catch (IOException e) {
            throw new IOException("Error parsing binary run record: " + e.getMessage(), e);
        }
    }

    private void parseRunRecordAfterVersion(BinarySimulationReader reader, SimulationContext context, String gatlingVersion) throws IOException {
        // Simulation class name
        String simulationClassName = reader.readString();
        context.setSimulationName(simulationClassName);

        // Start timestamp
        runStart = reader.readLong();
        context.setStart(runStart);

        // Run description
        String runDescription = reader.readString();
        // We could use this for scenario name, but it's usually empty or generic
        context.setScenarioName(runDescription.isEmpty() ? simulationClassName : runDescription);

        // Number of scenarios
        int scenarioCount = reader.readInt();
        scenarios = new String[scenarioCount];
        for (int i = 0; i < scenarioCount; i++) {
            scenarios[i] = reader.readString();
            // Initialize user tracking for each scenario
            context.addUser(scenarios[i]);
            context.endUser(scenarios[i]);
        }

        // Number of assertions
        int assertionCount = reader.readInt();
        for (int i = 0; i < assertionCount; i++) {
            // Skip assertion bytes
            int assertionBytesLength = reader.readInt();
            reader.skip(assertionBytesLength);
        }
    }

    private void parseUserRecord(BinarySimulationReader reader, SimulationContext context) throws IOException {
        // Scenario index
        int scenarioIndex = reader.readInt();
        String scenario = scenarios[scenarioIndex];

        // Start/end flag
        boolean isStart = reader.readBoolean();

        // Timestamp (relative to run start)
        int relativeTimestamp = reader.readInt();
        long absoluteTimestamp = runStart + relativeTimestamp;

        if (isStart) {
            context.addUser(scenario);
        } else {
            context.endUser(scenario);
        }
    }

    private void parseRequestRecord(BinarySimulationReader reader, SimulationContext context) throws IOException {
        // Groups
        int groupCount = reader.readInt();
        List<String> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(reader.readCachedString());
        }

        // Request name
        String requestName = reader.readCachedString();

        // Start timestamp (relative to run start)
        int relativeStart = reader.readInt();
        long startTimestamp = runStart + relativeStart;

        // End timestamp (relative to run start)
        int relativeEnd = reader.readInt();
        long endTimestamp = runStart + relativeEnd;

        // Status (true = OK, false = KO)
        boolean success = reader.readBoolean();

        // Message (usually empty for successful requests)
        String message = reader.readCachedString();

        // For now, use the first scenario name if available
        String scenario = scenarios != null && scenarios.length > 0 ? scenarios[0] : "default";

        // Add to context
        context.addRequest(scenario, requestName, startTimestamp, endTimestamp, success);
    }

    private void parseGroupRecord(BinarySimulationReader reader, SimulationContext context) throws IOException {
        // Groups
        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            reader.skipCachedString();
        }

        // Start timestamp (relative)
        reader.skipInt();

        // End timestamp (relative)
        reader.skipInt();

        // Cumulated response time
        reader.skipInt();

        // Status
        reader.skipByte();

        // Groups are not directly exposed in SimulationContext, so we skip them
    }

    private void parseErrorRecord(BinarySimulationReader reader, SimulationContext context) throws IOException {
        // Message
        reader.skipCachedString();

        // Timestamp (relative)
        reader.skipInt();

        // Errors are not directly exposed in SimulationContext, so we skip them
    }

    // These abstract methods from SimulationParser are not used in binary format
    // since we override the parse() method completely
    @Override
    protected String getSimulationName(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected String getSimulationStart(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected String getScenario(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected String getType(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected String getUserType(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected String getRequestName(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected Long getRequestStart(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected Long getRequestEnd(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }

    @Override
    protected boolean getRequestSuccess(List<String> line) {
        throw new UnsupportedOperationException("Binary format does not use line-based parsing");
    }
}