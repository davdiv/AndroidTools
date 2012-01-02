Android Tools
=============

This small project aims at providing Java build tools related to the [Android](http://www.android.com/)
mobile operating system. Currently it contains two [Ant](http://ant.apache.org) tasks
to build or extract boot images.

I hope this project can be useful for you.

Usage
=====

* First, you have to declare the library in your ant file:

&lt;taskdef
    resource="fr/free/divde/android/tools/antlib.xml"
    classpath="AndroidTools-1.0.jar"
/&gt;

* Then you can extract a boot image, to get the kernel, ramdisk and additional
information:

&lt;extractBootImage
    bootImage="input/boot.img"
    kernel="output/kernel.img"
    ramdisk="output/ramdisk.img"
    storeBaseAddr="bootBaseAddress"
    storeKernelAddr="bootKernelAddr"
    storeKernelSize="bootKernelSize"
    storeRamdiskAddr="bootRamdiskAddr"
    storeRamdiskSize="bootRamdiskSize"
    storeSecondAddr="bootSecondAddr"
    storeSecondSize="bootSecondSize"
    storeTagsAddr="bootTagsAddr"
    storePageSize="bootPageSize"
    storeName="bootName"
    storeArgs="bootArgs"
/&gt;

All the parameters prefixed with "store" expect a property name which will be
filled with the corresponding piece of information. Only the bootImage property
is mandatory. Output files or properties are not created if corresponding
parameters are not specified.

* You can also build a boot image from a kernel and ramdisk:

&lt;makeBootImage
    kernel="input/kernel.img"
    ramdisk="input/ramdisk.img"
    bootImage="output/boot.img"
    pageSize="2048"
    baseAddr="0x02600000"
    name="myImageName"
    args="init=/sbin/init root=/dev/mtdblock5"
/&gt;

Only kernel, ramdisk and bootImage are mandatory. Other values are filled with
the following defaults:
pageSize: 2048, baseAddr: 0x10000000, empty name, empty args

Useful references
=================

* An improved firmware for Android devices: [cyanogenmod](http://www.cyanogenmod.com)
* An equivalent program written in C is available [here](https://github.com/CyanogenMod/android_system_core/tree/ics/mkbootimg).
