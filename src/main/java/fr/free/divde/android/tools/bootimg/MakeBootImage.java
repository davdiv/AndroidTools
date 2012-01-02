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
import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

@Getter
@Setter
public class MakeBootImage extends Task {

    /** input file: kernel */
    private File kernel;
    /** input file: ramdisk */
    private File ramdisk;
    /** input file: second */
    private File second;
    /** output file */
    private File bootImage;
    private String baseAddr;
    private String pageSize;
    private String name;
    private String args;

    @Override
    public void execute() throws BuildException {
        try {

            BootImageFile bootImageHeader = new BootImageFile();

            if (pageSize != null) {
                bootImageHeader.setPageSize(Integer.decode(pageSize));
            }
            
            if (baseAddr != null) {
                bootImageHeader.setBaseAddr(Integer.decode(baseAddr));
            }

            if (name != null) {
                bootImageHeader.setName(name);
            }

            if (args != null) {
                bootImageHeader.setArgs(args);
            }

            @Cleanup
            RandomAccessFile kernelAccess = new RandomAccessFile(kernel, "r");
            long kernelLength = kernelAccess.length();
            if (kernelLength > Integer.MAX_VALUE) {
                throw new RuntimeException(String.format("File is too large: %s is %d bytes", kernel, kernelLength));
            }
            bootImageHeader.setKernelSize((int) kernelLength);

            @Cleanup
            RandomAccessFile ramdiskAccess = new RandomAccessFile(ramdisk, "r");
            long ramdiskLength = ramdiskAccess.length();
            if (ramdiskLength > Integer.MAX_VALUE) {
                throw new RuntimeException(String.format("File is too large: %s is %d bytes", ramdisk, ramdiskLength));
            }
            bootImageHeader.setRamdiskSize((int) ramdiskLength);

            @Cleanup
            RandomAccessFile secondAccess = null;
            long secondLength = 0;
            if (second != null) {
                secondAccess = new RandomAccessFile(second, "r");
                secondLength = secondAccess.length();
                if (secondLength > Integer.MIN_VALUE) {
                    throw new RuntimeException(String.format("File is too large: %s is %d bytes", second, secondLength));
                }
            }
            bootImageHeader.setSecondSize((int) secondLength);

            MessageDigest sha = MessageDigest.getInstance("SHA-1");

            log(String.format("Writing boot image: %s", bootImage));
            @Cleanup
            RandomAccessFile bootImageAccess = new RandomAccessFile(bootImage, "rw");
            bootImageAccess.setLength(bootImageHeader.getFileSize());
            bootImageAccess.seek(bootImageHeader.getKernelPosition());
            BinaryUtils.copyBytes(kernelAccess, bootImageAccess, bootImageHeader.getKernelSize(), sha);
            BinaryUtils.fillPadding(bootImageAccess, bootImageHeader.getKernelPadding());
            BinaryUtils.writeIntLittleEndian(sha, bootImageHeader.getKernelSize());
            bootImageAccess.seek(bootImageHeader.getRamdiskPosition());
            BinaryUtils.copyBytes(ramdiskAccess, bootImageAccess, bootImageHeader.getRamdiskSize(), sha);
            BinaryUtils.fillPadding(bootImageAccess, bootImageHeader.getRamdiskPadding());
            BinaryUtils.writeIntLittleEndian(sha, bootImageHeader.getRamdiskSize());
            if (secondLength > 0) {
                bootImageAccess.seek(bootImageHeader.getSecondPosition());
                BinaryUtils.copyBytes(secondAccess, bootImageAccess, bootImageHeader.getSecondSize(), sha);
                BinaryUtils.fillPadding(bootImageAccess, bootImageHeader.getSecondPadding());
            }
            BinaryUtils.writeIntLittleEndian(sha, bootImageHeader.getSecondSize());
            byte[] out = sha.digest();
            byte id[] = bootImageHeader.getId();
            for (int i = out.length - 1; i >= 0; i--) {
                id[i] = out[i];
            }
            bootImageHeader.write(bootImageAccess);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
