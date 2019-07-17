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


	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/FPGA_NetworkDevice.java,v $
 	Author(s):             George Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         
 	Last Date of Change:   $Date: 1997/07/14 07:40:00 $      by: $Author: gfa $
	
	
*/

package mipssimulator;

import mipssimulator.MemoryRegion;

public class FPGA_NetworkDevice extends MemoryRegion {

	final int FPGAADDRSIZE = 0xc0010;

	public FPGA_NetworkDevice(int baseAddress) {
	    from = baseAddress; 
	    to = baseAddress+FPGAADDRSIZE;
	}

	public byte readByte(int address) {
	    return (byte)0xff;
	}
	public int readInt(int address) {
	    return (int)0xffffffff;
	}
	public void writeByte(int address, byte data) {
	}
	public void writeInt(int address, int data) {
	}
}

