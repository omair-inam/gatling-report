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

import java.nio.charset.StandardCharsets;

/**
 * Helper class for string encoding/decoding used in binary format.
 * Simplified version without reflection for compatibility.
 */
public class StringInternals {

    // String coder values
    public static final byte LATIN1 = 0;
    public static final byte UTF16 = 1;

    /**
     * Create a new String from bytes and coder.
     *
     * @param bytes the byte array
     * @param coder the coder (0 for Latin1, 1 for UTF16)
     * @return the decoded string
     */
    public static String newString(byte[] bytes, byte coder) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        if (coder == LATIN1) {
            // Latin1 encoding - direct byte to char conversion
            return new String(bytes, StandardCharsets.ISO_8859_1);
        } else {
            // UTF16 encoding
            return new String(bytes, StandardCharsets.UTF_16LE);
        }
    }

    /**
     * Get the byte array value of a string.
     *
     * @param str the string
     * @return the byte array
     */
    public static byte[] value(String str) {
        if (str == null || str.isEmpty()) {
            return new byte[0];
        }
        // Use UTF-8 as default encoding
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get the coder for a string (simplified - always returns LATIN1 for ASCII-compatible).
     *
     * @param str the string
     * @return the coder byte
     */
    public static byte coder(String str) {
        // Simplified: always use LATIN1 for ASCII-compatible strings
        return LATIN1;
    }
}