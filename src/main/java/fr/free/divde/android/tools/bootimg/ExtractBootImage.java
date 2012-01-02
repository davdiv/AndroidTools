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
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

@Getter
@Setter
public class ExtractBootImage extends Task {

    /** input file */
    private File bootImage;
    /** output file: kernel */
    private File kernel;
    /** output file: ramdisk */
    private File ramdisk;
    /** output file: second */
    private File second;
    private String storeBaseAddr;
    private String storeKernelAddr;
    private String storeKernelSize;
    private String storeRamdiskAddr;
    private String storeRamdiskSize;
    private String storeSecondAddr;
    private String storeSecondSize;
    private String storeTagsAddr;
    private String storePageSize;
    private String storeName;
    private String storeArgs;

    private void storeProperty(String propertyName, int propertyValue) {
        if (propertyName != null) {
            getProject().setProperty(propertyName, String.format("0x%08X", propertyValue));
        }
    }

    private void storeProperty(String propertyName, String propertyValue) {
        if (propertyName != null) {
            getProject().setProperty(propertyName, propertyValue);
        }
    }

    @Override
    public void execute() throws BuildException {
        try {
            System.out.println(String.format("Reading boot image: %s", bootImage));
            @Cleanup
            RandomAccessFile bootImageAccess = new RandomAccessFile(bootImage, "r");
            BootImageFile bootImageHeader = new BootImageFile();
            bootImageHeader.read(bootImageAccess);
            storeProperty(storeBaseAddr, bootImageHeader.getBaseAddr());
            storeProperty(storeKernelAddr, bootImageHeader.getKernelAddr());
            storeProperty(storeKernelSize, bootImageHeader.getKernelSize());
            storeProperty(storeRamdiskAddr, bootImageHeader.getRamdiskAddr());
            storeProperty(storeRamdiskSize, bootImageHeader.getRamdiskSize());
            storeProperty(storeSecondAddr, bootImageHeader.getSecondAddr());
            storeProperty(storeSecondSize, bootImageHeader.getSecondSize());
            storeProperty(storeTagsAddr, bootImageHeader.getTagsAddr());
            storeProperty(storePageSize, bootImageHeader.getPageSize());
            storeProperty(storeName, bootImageHeader.getName());
            storeProperty(storeArgs, bootImageHeader.getArgs());

            if (kernel != null) {
                System.out.println(String.format("Extracting kernel: %s", kernel));
                @Cleanup
                RandomAccessFile kernelAccess = new RandomAccessFile(kernel, "rw");
                int kernelFileSize = bootImageHeader.getKernelSize();
                kernelAccess.setLength(kernelFileSize);
                bootImageAccess.seek(bootImageHeader.getKernelPosition());
                BinaryUtils.copyBytes(bootImageAccess, kernelAccess, kernelFileSize, null);
            }
            if (ramdisk != null) {
                System.out.println(String.format("Extracting ramdisk: %s", ramdisk));
                @Cleanup
                RandomAccessFile ramdiskAccess = new RandomAccessFile(ramdisk, "rw");
                int ramdiskFileSize = bootImageHeader.getRamdiskSize();
                ramdiskAccess.setLength(ramdiskFileSize);
                bootImageAccess.seek(bootImageHeader.getRamdiskPosition());
                BinaryUtils.copyBytes(bootImageAccess, ramdiskAccess, ramdiskFileSize, null);
            }
            if (second != null) {
                System.out.println(String.format("Extracting second: %s", second));
                @Cleanup
                RandomAccessFile secondAccess = new RandomAccessFile(second, "rw");
                int secondFileSize = bootImageHeader.getSecondSize();
                secondAccess.setLength(secondFileSize);
                bootImageAccess.seek(bootImageHeader.getSecondPosition());
                BinaryUtils.copyBytes(bootImageAccess, secondAccess, secondFileSize, null);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
