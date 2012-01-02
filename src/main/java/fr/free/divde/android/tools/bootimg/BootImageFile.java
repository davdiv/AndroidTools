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
package fr.free.divde.android.tools.bootimg;

import fr.free.divde.android.tools.BinaryUtils;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.RandomAccessFile;
import lombok.Data;
import lombok.NonNull;

@Data
public class BootImageFile {

    public static final String BOOT_MAGIC = "ANDROID!";
    public static final int BOOT_MAGIC_SIZE = BOOT_MAGIC.length();
    public static final int BOOT_NAME_SIZE = 16;
    public static final int BOOT_ARGS_SIZE = 512;
    public static final int HEADER_SIZE = BOOT_MAGIC_SIZE + BOOT_NAME_SIZE + BOOT_ARGS_SIZE + 10 * 4 + 32;
    public static final int DEFAULT_PAGE_SIZE = 2048;
    public static final int DEFAULT_BASE_ADDR = 0x10000000;
    public static final int OFFSET_KERNEL_ADDR = 0x00008000;
    public static final int OFFSET_RAMDISK_ADDR = 0x01000000;
    public static final int OFFSET_SECOND_ADDR = 0x00F00000;
    public static final int OFFSET_TAGS_ADDR = 0x00000100;
    private String magic = BOOT_MAGIC;
    /** size in bytes */
    private int kernelSize;
    /** physical load addr */
    private int kernelAddr = DEFAULT_BASE_ADDR + OFFSET_KERNEL_ADDR;
    /** size in bytes */
    private int ramdiskSize;
    /** physical load addr */
    private int ramdiskAddr = DEFAULT_BASE_ADDR + OFFSET_RAMDISK_ADDR;
    /** size in bytes */
    private int secondSize;
    /** physical load addr */
    private int secondAddr = DEFAULT_BASE_ADDR + OFFSET_SECOND_ADDR;
    /** physical addr for kernel tags */
    private int tagsAddr = DEFAULT_BASE_ADDR + OFFSET_TAGS_ADDR;
    /** flash page size we assume */
    private int pageSize = DEFAULT_PAGE_SIZE;
    /** future expansion: should be 0 */
    private int[] unused = new int[2];
    /** asciiz product name */
    @NonNull
    private String name = "";
    /** command line */
    @NonNull
    private String args = "";
    /** timestamp / checksum / sha1 / etc */
    private byte[] id = new byte[32];

    public void read(RandomAccessFile file) throws IOException {
        file.seek(0);
        magic = BinaryUtils.readString(file, BOOT_MAGIC_SIZE);
        kernelSize = BinaryUtils.readIntLittleEndian(file);
        kernelAddr = BinaryUtils.readIntLittleEndian(file);
        ramdiskSize = BinaryUtils.readIntLittleEndian(file);
        ramdiskAddr = BinaryUtils.readIntLittleEndian(file);
        secondSize = BinaryUtils.readIntLittleEndian(file);
        secondAddr = BinaryUtils.readIntLittleEndian(file);
        tagsAddr = BinaryUtils.readIntLittleEndian(file);
        pageSize = BinaryUtils.readIntLittleEndian(file);
        unused[0] = BinaryUtils.readIntLittleEndian(file);
        unused[1] = BinaryUtils.readIntLittleEndian(file);
        name = BinaryUtils.readString(file, BOOT_NAME_SIZE);
        args = BinaryUtils.readString(file, BOOT_ARGS_SIZE);
        for (int i = 0; i < 32; i++) {
            id[i] = file.readByte();
        }
        check();
        assert (file.getFilePointer() == HEADER_SIZE);
        file.seek(getKernelPosition());
        long longFileSize = file.length();
        if (longFileSize > Integer.MAX_VALUE) {
            throw new InvalidObjectException(String.format("File is too large: %d bytes.", longFileSize));
        }
        int fileSize = (int) longFileSize;
        int expectedFileSize = getFileSize();
        if (fileSize < getFileSize()) {
            throw new InvalidObjectException(String.format("File size is smaller than expected. Expected %d bytes, found %d bytes.", expectedFileSize, fileSize));
        }
    }

    private void check() throws IOException {
        if (!BOOT_MAGIC.equals(magic)) {
            throw new InvalidObjectException(String.format("Invalid boot magic. Expected '%s', found '%s'.", BOOT_MAGIC, magic));
        }
        if (kernelSize <= 0 || ramdiskSize <= 0 || secondSize < 0 || pageSize < HEADER_SIZE) {
            throw new InvalidObjectException(String.format("Unsupported value for kernelSize (%d<=0), ramdiskSize (%d<=0), secondSize (%d<0) or pageSize (%d<%d).", kernelSize, ramdiskSize, secondSize, pageSize, HEADER_SIZE));
        }
    }

    public void write(RandomAccessFile file) throws IOException {
        check();
        file.seek(0);
        BinaryUtils.writeString(file, BOOT_MAGIC_SIZE, magic);
        BinaryUtils.writeIntLittleEndian(file, kernelSize);
        BinaryUtils.writeIntLittleEndian(file, kernelAddr);
        BinaryUtils.writeIntLittleEndian(file, ramdiskSize);
        BinaryUtils.writeIntLittleEndian(file, ramdiskAddr);
        BinaryUtils.writeIntLittleEndian(file, secondSize);
        BinaryUtils.writeIntLittleEndian(file, secondAddr);
        BinaryUtils.writeIntLittleEndian(file, tagsAddr);
        BinaryUtils.writeIntLittleEndian(file, pageSize);
        BinaryUtils.writeIntLittleEndian(file, unused[0]);
        BinaryUtils.writeIntLittleEndian(file, unused[1]);
        BinaryUtils.writeString(file, BOOT_NAME_SIZE, name);
        BinaryUtils.writeString(file, BOOT_ARGS_SIZE, args);
        for (int i = 0; i < 32; i++) {
            file.write(id[i]);
        }
        int curPos = (int) file.getFilePointer();
        assert (curPos == HEADER_SIZE);
        BinaryUtils.fillPadding(file, getKernelPosition() - curPos);
    }

    public int getKernelPosition() {
        return pageSize;
    }

    public int computePadding(int size) {
        int lastPageBytes = (size % pageSize);
        if (lastPageBytes == 0) {
            return 0;
        } else {
            return pageSize - lastPageBytes;
        }
    }

    public int getKernelPadding() {
        return computePadding(getKernelSize());
    }

    public int getRamdiskPosition() {
        return getKernelPosition() + getKernelSize() + getKernelPadding();
    }

    public int getRamdiskPadding() {
        return computePadding(getRamdiskSize());
    }

    public int getSecondPosition() {
        return getRamdiskPosition() + getRamdiskSize() + getRamdiskPadding();
    }

    public int getSecondPadding() {
        return computePadding(getSecondSize());
    }

    public int getFileSize() {
        return getSecondPosition() + getSecondSize() + getSecondPadding();
    }

    public void setBaseAddr(int base) {
        setKernelAddr(base + OFFSET_KERNEL_ADDR);
        setRamdiskAddr(base + OFFSET_RAMDISK_ADDR);
        setSecondAddr(base + OFFSET_SECOND_ADDR);
        setTagsAddr(base + OFFSET_TAGS_ADDR);
    }

    public int getBaseAddr() {
        return getKernelAddr() - OFFSET_KERNEL_ADDR;
    }
}
