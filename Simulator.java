/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	MIPS Simulator for a R3000 based machine. For details, have a look at the
	hardware documentation of the Integrated Device Technology ID79R3052E 
	processor and the evaluation board 7RS385.
	
	Permission to use, copy, modify, and distribute this software and its
	documentation for any purpose, without fee, and without written 
	agreement is hereby granted, provided that the above copyright notice 
	and the following two paragraphs appear in all copies of this software.


	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Simulator.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
*/

import java.lang.*;
import java.io.*;
import Processor;
import KernelLoader;
import KernelLoaderException;

public final class Simulator extends Object
{	
	static Processor proc;
	public static boolean traceMode = false;
	public static boolean verboseMode = false;
	public static boolean bigEndian = false;
	
	public static void main(String argv[])
	{
		String bootFilename = "topsy";	// default kernel name
		RandomAccessFile kernelFile;

		// print some nice message and open kernel file
		System.out.print("\nMIPS/IDT R3052E Simulator - (c) 1996-1997 gfa\n");
		for (int i = 0; i < argv.length; i++) {
			if (argv[i].charAt(0) == '-') {
				if (argv[i].charAt(1) == 't') traceMode = true;
				else if (argv[i].charAt(1) == 'v') verboseMode = true;
				else if (argv[i].charAt(1) == 'b') bigEndian = true;
			}
			else {
				bootFilename = argv[i];
			}
		}
		System.out.print("Configured as ");
		System.out.print(bigEndian ? "big endian" : "little endian");
		System.out.println(", trace="+traceMode+", verbose="+verboseMode);
		try {
			kernelFile = new RandomAccessFile(bootFilename, "r");
		}
		catch (Exception exception) {
			System.out.print("Couldn't find kernel '"+bootFilename+"' (");
			System.out.print(exception.getClass().getName()+")\n\n");
			return;
		}		
		// now we have a stream with the kernel file, 
		// let's init cpu/memory/devices and load it
		try { 
			proc = new Processor();
		}
		catch (Exception exception) {
			System.out.print("Couldn't create processor (");
			System.out.print(exception.getClass().getName() + ")\n\n");
			return;
		}
		System.out.print("Loading from '" + bootFilename + "'\n");
		try {
			new KernelLoader(kernelFile, proc.memory, proc);
		}
		catch (Exception exception) {
			System.out.print("Couldn't load kernel (");
			System.out.print(exception.getClass().getName() + ")\n\n");
			return;
		}
		// machine initialized, kernel loaded, let's go!		
		System.out.print("ok. Booting from '"+ bootFilename +"'\n\n");
		try {
			new Thread(proc).start();
			//proc.run();
		}
		catch (Exception exception) {
			System.out.print("Exception while running (");
			System.out.print(exception.getClass().getName() + ")\n");
			proc.where();
			return;
		}
	}	
}
