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

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class TestBinaryReader {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testReadPrimitiveTypes() throws IOException {
        File binaryFile = tempFolder.newFile("test_primitives.bin");

        // Write test data
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(binaryFile))) {
            dos.writeByte((byte) 42);
            dos.writeBoolean(true);
            dos.writeInt(12345);
            dos.writeLong(9876543210L);
        }

        // Test reading
        try (BinarySimulationReader reader = new BinarySimulationReader(binaryFile)) {
            Assert.assertEquals("Should read byte correctly", (byte) 42, reader.readByte());
            Assert.assertTrue("Should read boolean correctly", reader.readBoolean());
            Assert.assertEquals("Should read int correctly", 12345, reader.readInt());
            Assert.assertEquals("Should read long correctly", 9876543210L, reader.readLong());
        }
    }

    @Test
    public void testReadString() throws IOException {
        File binaryFile = tempFolder.newFile("test_strings.bin");
        String testString = "Test String";
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);

        // Write test data (format: length + bytes + coder)
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(binaryFile))) {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.writeByte((byte) 0); // Latin1 coder
        }

        // Test reading
        try (BinarySimulationReader reader = new BinarySimulationReader(binaryFile)) {
            Assert.assertEquals("Should read string correctly", testString, reader.readString());
        }
    }

    @Test
    public void testReadEmptyString() throws IOException {
        File binaryFile = tempFolder.newFile("test_empty_string.bin");

        // Write empty string (length = 0)
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(binaryFile))) {
            dos.writeInt(0);
        }

        // Test reading
        try (BinarySimulationReader reader = new BinarySimulationReader(binaryFile)) {
            Assert.assertEquals("Should read empty string", "", reader.readString());
        }
    }

    @Test
    public void testReadCachedString() throws IOException {
        File binaryFile = tempFolder.newFile("test_cached_strings.bin");
        String testString = "Cached String";
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);

        // Write test data
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(binaryFile))) {
            // First occurrence: positive index + string data
            dos.writeInt(1); // cache index
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.writeByte((byte) 0); // Latin1 coder

            // Second occurrence: negative index (reference to cached)
            dos.writeInt(-1);
        }

        // Test reading
        try (BinarySimulationReader reader = new BinarySimulationReader(binaryFile)) {
            String first = reader.readCachedString();
            String second = reader.readCachedString();
            Assert.assertEquals("First read should return string", testString, first);
            Assert.assertEquals("Second read should return cached string", testString, second);
        }
    }

    @Test
    public void testSkipMethods() throws IOException {
        File binaryFile = tempFolder.newFile("test_skip.bin");

        // Write test data
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(binaryFile))) {
            dos.writeByte((byte) 1);
            dos.writeInt(100);
            dos.writeLong(200L);
            // String: length + bytes + coder
            dos.writeInt(5);
            dos.write("Hello".getBytes(StandardCharsets.UTF_8));
            dos.writeByte((byte) 0);
            dos.writeInt(999); // Value to read after skips
        }

        // Test skipping
        try (BinarySimulationReader reader = new BinarySimulationReader(binaryFile)) {
            reader.skipByte();
            reader.skipInt();
            reader.skipLong();
            reader.skipString();
            Assert.assertEquals("Should read correct value after skips", 999, reader.readInt());
        }
    }
}