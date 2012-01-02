/*
 * Java build tools related to the Android operating system.
 * Copyright (C) 2011 DivDE <divde@free.fr>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.free.divde.android.tools;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;

public class BinaryUtils {

    public static int COPY_CHUNK_SIZE = 10000;

    public static int readIntLittleEndian(RandomAccessFile file) throws IOException {
        int a = file.readByte() & 0xFF;
        int b = file.readByte() & 0xFF;
        int c = file.readByte() & 0xFF;
        int d = file.readByte() & 0xFF;
        int res = (d << 24) | (c << 16) | (b << 8) | a;
        return res;
    }

    public static void writeIntLittleEndian(RandomAccessFile file, int value) throws IOException {
        int a = value & 0xFF;
        int b = (value >> 8) & 0xFF;
        int c = (value >> 16) & 0xFF;
        int d = (value >> 24) & 0xFF;
        file.writeByte(a);
        file.writeByte(b);
        file.writeByte(c);
        file.writeByte(d);
    }

    public static void writeIntLittleEndian(MessageDigest md, int value) throws IOException {
        int a = value & 0xFF;
        int b = (value >> 8) & 0xFF;
        int c = (value >> 16) & 0xFF;
        int d = (value >> 24) & 0xFF;
        md.update((byte) a);
        md.update((byte) b);
        md.update((byte) c);
        md.update((byte) d);
    }

    public static String readString(RandomAccessFile file, int size) throws IOException {
        int lastNonNullChar = 0;
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int c = file.read();
            if (c != 0) {
                lastNonNullChar = i;
            }
            builder.append((char) c);
        }
        return builder.toString().substring(0, lastNonNullChar + 1);
    }

    public static void writeString(RandomAccessFile file, int size, String text) throws IOException {
        int textLength = text.length();
        int remainingSize = size - textLength;
        if (remainingSize < 0) {
            throw new IllegalArgumentException(String.format("String is too long: '%s' is longer than %d", text, size));
        }
        file.writeBytes(text);
        fillPadding(file, remainingSize);
    }

    public static void copyBytes(RandomAccessFile srcFile, RandomAccessFile dstFile, int length, MessageDigest messageDigest) throws IOException {
        byte[] chunk = new byte[Math.min(length, COPY_CHUNK_SIZE)];
        while (length > 0) {
            int sizeToRead = Math.min(length, COPY_CHUNK_SIZE);
            int sizeRead = srcFile.read(chunk, 0, sizeToRead);
            if (sizeRead == -1) {
                throw new IOException(String.format("End of source file reached too early. Still %d bytes to read.", length));
            }
            if (messageDigest != null) {
                messageDigest.update(chunk, 0, sizeRead);
            }
            dstFile.write(chunk, 0, sizeRead);
            length -= sizeRead;
        }
    }

    public static void fillPadding(RandomAccessFile file, int size) throws IOException {
        if (size < 0) {
            throw new IOException(String.format("Negative padding: %d", size));
        }
        for (int i = 0; i < size; i++) {
            file.writeByte(0);
        }
    }
}
