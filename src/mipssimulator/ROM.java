/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.
	
	This is not really used. It just happened to be on that board... 
	and its base is mapped to the reset vector of the processor. 

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/ROM.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
	$Log: ROM.java,v $
	Revision 1.1  1997/05/09 14:33:46  gfa
	Initial revision

# Revision 1.2  1997/03/12  17:45:58  gfa
# added message
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

package mipssimulator;

import java.lang.*;
import java.io.*;
import mipssimulator.MemoryRegion;


public class ROM extends MemoryRegion {
	int base;
	
	ROM(int base, int size, String imageFilename) throws BusErrorException {
		int i;
		RandomAccessFile f;
		
		this.base = base;
		from = base;
		to = base+size-1;
		memory = new byte[to-from+1];
		try {
			f = new RandomAccessFile(imageFilename, "r");
			f.read(memory, 0, size);
		}
		catch (Exception exception) {
			System.out.print("ROM not initialized (imagefile: ");
			System.out.print(imageFilename + ", ");
			System.out.print(exception.getClass().getName() + ")\n");			
		}
	}
	
	public void insertDefaultCode(int startAddr) throws BusErrorException {
		// if no ROM file is found, insert instructions to jump
		// to loader start address in RAM (one of the seldom cases
		// we're allowed to write to a ROM, enjoy!)
		int startHi = (startAddr >> 16) & 0x0000ffff;
		int startLo = startAddr & 0x0000ffff;
		int opcode = 0x3c020000 | startHi;	// LUI r2, startHi
		super.writeInt(base, opcode);
		opcode = 0x34420000 | startLo;		// ORI r2, r2, startLo
		super.writeInt(base+4, opcode);
		opcode = 0x00400008; 				// JR  r2
		super.writeInt(base+8, opcode);			
		opcode = 0x00000000; 				// NOP
		super.writeInt(base+12, opcode);
	}
	
	// use default read method

	// writing?! hey, this a ROM...	
	void writeByte(int address, byte data) throws BusErrorException {	
		throw new BusErrorException("ROM: faulting phys addr: " + 
											Integer.toHexString(address));
	}
	void writeShort(int address, short data) throws BusErrorException {	
		throw new BusErrorException("ROM: faulting phys addr: " + 
											Integer.toHexString(address));
	}
	void writeInt(int address, int data) throws BusErrorException {	
		throw new BusErrorException("ROM: faulting phys addr: " + 
											Integer.toHexString(address));
	}
}		
