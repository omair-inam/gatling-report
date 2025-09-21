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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class TestBinaryDetection {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDetectBinaryFormat() throws IOException {
        // Create a binary format simulation log file (starts with 00 00 00 00)
        File binaryFile = tempFolder.newFile("binary_simulation.log");
        byte[] binaryHeader = new byte[] {
            0x00, 0x00, 0x00, 0x00,  // Binary format marker
            0x06,                     // Version string length
            0x33, 0x2E, 0x31, 0x34, 0x2E, 0x33  // "3.14.3"
        };
        Files.write(binaryFile.toPath(), binaryHeader);

        // Test detection
        Assert.assertTrue("Should detect binary format", Utils.isBinaryFormat(binaryFile));
    }

    @Test
    public void testDetectTextFormat() throws IOException {
        // Create a text format simulation log file
        File textFile = tempFolder.newFile("text_simulation.log");
        String textContent = "RUN\tSimulationName\tscenario\t1234567890\tdescription\t3.0\n";
        Files.write(textFile.toPath(), textContent.getBytes());

        // Test detection
        Assert.assertFalse("Should detect text format", Utils.isBinaryFormat(textFile));
    }

    @Test
    public void testDetectEmptyFile() throws IOException {
        // Create an empty file
        File emptyFile = tempFolder.newFile("empty_simulation.log");

        // Test detection - empty files should be considered text
        Assert.assertFalse("Empty file should be considered text", Utils.isBinaryFormat(emptyFile));
    }

    @Test
    public void testDetectShortFile() throws IOException {
        // Create a file shorter than 4 bytes
        File shortFile = tempFolder.newFile("short_simulation.log");
        byte[] shortContent = new byte[] { 0x00, 0x00 };
        Files.write(shortFile.toPath(), shortContent);

        // Test detection - short files should be considered text
        Assert.assertFalse("Short file should be considered text", Utils.isBinaryFormat(shortFile));
    }
}