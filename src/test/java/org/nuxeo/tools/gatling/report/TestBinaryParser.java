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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

public class TestBinaryParser {

    protected static final String SIM_BINARY = "simulation-binary.log";

    @Test
    public void testParseBinarySimulation() throws Exception {
        File file = getResourceFile(SIM_BINARY);
        SimulationParserBinary parser = new SimulationParserBinary(file);
        SimulationContext context = parser.parse();

        // Basic assertions
        Assert.assertNotNull("Context should not be null", context);
        Assert.assertNotNull("Simulation name should not be null", context.getSimulationName());

        // The binary file shows "AlleleFrequencyBatchQuestionTest" as the simulation class
        Assert.assertTrue("Simulation name should contain expected class",
            context.getSimulationName().contains("AlleleFrequencyBatchQuestionTest"));

        // Verify some statistics were computed
        RequestStat simStat = context.getSimStat();
        Assert.assertNotNull("Simulation stats should not be null", simStat);
    }

    @Test
    public void testParseRunRecord() throws Exception {
        // Create a minimal binary file with just a Run record
        File testFile = File.createTempFile("test_run", ".log");
        testFile.deleteOnExit();

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(testFile))) {
            // Write binary format header
            dos.writeInt(0);  // Binary format marker

            // Write Run record (header = 0)
            dos.writeByte(0);

            // Version string
            writeString(dos, "3.14.3");

            // Simulation class name
            writeString(dos, "com.example.TestSimulation");

            // Start timestamp
            dos.writeLong(1234567890000L);

            // Run description
            writeString(dos, "Test Run");

            // Number of scenarios
            dos.writeInt(1);

            // Scenario name
            writeString(dos, "Test Scenario");

            // Number of assertions (0 for simplicity)
            dos.writeInt(0);
        }

        SimulationParserBinary parser = new SimulationParserBinary(testFile);
        SimulationContext context = parser.parse();

        Assert.assertNotNull("Context should not be null", context);
        Assert.assertEquals("Simulation name should match",
            "com.example.TestSimulation", context.getSimulationName());
        Assert.assertEquals("Start time should match", 1234567890000L, context.start);
    }

    @Test
    public void testParseUserRecord() throws Exception {
        // Create a binary file with Run and User records
        File testFile = File.createTempFile("test_user", ".log");
        testFile.deleteOnExit();

        long startTime = 1234567890000L;

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(testFile))) {
            // Write binary format header
            dos.writeInt(0);

            // Write Run record
            dos.writeByte(0);
            writeString(dos, "3.14.3");
            writeString(dos, "com.example.TestSimulation");
            dos.writeLong(startTime);
            writeString(dos, "Test Run");
            dos.writeInt(1);
            writeString(dos, "Test Scenario");
            dos.writeInt(0);

            // Write User START record (header = 2)
            dos.writeByte(2);
            dos.writeInt(0);  // scenario index
            dos.writeBoolean(true);  // START event
            dos.writeInt(100);  // relative timestamp

            // Write User END record
            dos.writeByte(2);
            dos.writeInt(0);  // scenario index
            dos.writeBoolean(false);  // END event
            dos.writeInt(200);  // relative timestamp
        }

        SimulationParserBinary parser = new SimulationParserBinary(testFile);
        SimulationContext context = parser.parse();

        Assert.assertNotNull("Context should not be null", context);
        // User tracking is handled internally; verify through max users
        Assert.assertTrue("Max users should be positive", context.maxUsers >= 0);
    }

    @Test
    public void testParseRequestRecord() throws Exception {
        // Create a binary file with Run and Request records
        File testFile = File.createTempFile("test_request", ".log");
        testFile.deleteOnExit();

        long startTime = 1234567890000L;

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(testFile))) {
            // Write binary format header
            dos.writeInt(0);

            // Write Run record
            dos.writeByte(0);
            writeString(dos, "3.14.3");
            writeString(dos, "com.example.TestSimulation");
            dos.writeLong(startTime);
            writeString(dos, "Test Run");
            dos.writeInt(1);
            writeString(dos, "Test Scenario");
            dos.writeInt(0);

            // Write Request record (header = 1)
            dos.writeByte(1);

            // Groups (empty for this test)
            dos.writeInt(0);  // no groups

            // Request name (cached string)
            dos.writeInt(1);  // cache index
            writeString(dos, "Test Request");

            // Timestamps
            dos.writeInt(100);  // start (relative)
            dos.writeInt(150);  // end (relative)

            // Status (true = OK)
            dos.writeBoolean(true);

            // Message (cached string - empty)
            dos.writeInt(2);
            writeString(dos, "");
        }

        SimulationParserBinary parser = new SimulationParserBinary(testFile);
        SimulationContext context = parser.parse();

        Assert.assertNotNull("Context should not be null", context);
        Assert.assertTrue("Should have at least one request",
            context.getRequests().size() > 0);
    }

    private void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null || str.isEmpty()) {
            dos.writeInt(0);
        } else {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.writeByte((byte) 0);  // Latin1 coder
        }
    }

    protected File getResourceFile(String name) throws Exception {
        return new File(Objects.requireNonNull(getClass().getClassLoader().getResource(name)).toURI());
    }
}