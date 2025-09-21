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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reader for binary format simulation log files (Gatling 3.14.3+)
 */
public class BinarySimulationReader implements Closeable {

    private final DataInputStream dis;
    private final byte[] skipBuffer = new byte[1024];
    private final Map<Integer, String> stringCache = new HashMap<>();

    public BinarySimulationReader(File file) throws IOException {
        this.dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    }

    public byte readByte() throws IOException {
        return dis.readByte();
    }

    public boolean readBoolean() throws IOException {
        return dis.readBoolean();
    }

    public int readInt() throws IOException {
        return dis.readInt();
    }

    public long readLong() throws IOException {
        return dis.readLong();
    }

    public String readString() throws IOException {
        int length = readInt();
        if (length == 0) {
            return "";
        }

        byte[] value = new byte[length];
        dis.readFully(value);
        byte coder = readByte();

        return StringInternals.newString(value, coder);
    }

    public String readCachedString() throws IOException {
        int cachedIndex = readInt();
        if (cachedIndex >= 0) {
            // New string - cache it
            String string = readString();
            stringCache.put(cachedIndex, string);
            return string;
        } else {
            // Reference to cached string
            String cachedString = stringCache.get(-cachedIndex);
            if (cachedString == null) {
                throw new IOException("Cached string missing for index: " + (-cachedIndex));
            }
            return cachedString;
        }
    }

    public void skip(int len) throws IOException {
        int n = 0;
        while (n < len) {
            int count = dis.read(skipBuffer, 0, Math.min(len - n, skipBuffer.length));
            if (count < 0) {
                throw new EOFException("Failed to skip " + len + " bytes");
            }
            n += count;
        }
    }

    public void skipByte() throws IOException {
        skip(Byte.BYTES);
    }

    public void skipInt() throws IOException {
        skip(Integer.BYTES);
    }

    public void skipLong() throws IOException {
        skip(Long.BYTES);
    }

    public void skipString() throws IOException {
        int length = readInt();
        if (length > 0) {
            // Skip value (byte[]) + coder (byte)
            skip(length + 1);
        }
    }

    public void skipCachedString() throws IOException {
        int cachedIndex = readInt();
        if (cachedIndex >= 0) {
            skipString();
        }
    }

    public boolean hasMore() throws IOException {
        // Mark current position
        dis.mark(1);
        int nextByte = dis.read();
        if (nextByte == -1) {
            return false;
        }
        // Reset to marked position
        dis.reset();
        return true;
    }

    @Override
    public void close() throws IOException {
        dis.close();
    }
}