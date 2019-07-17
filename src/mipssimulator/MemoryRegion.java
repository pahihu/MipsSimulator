/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.
	
	Implements bi-endian addressing. Please check subclasses too...

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/MemoryRegion.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.2 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/06/01 17:38:15 $      by: $Author: gfa $
	
	
	$Log: MemoryRegion.java,v $
	Revision 1.2  1997/06/01 17:38:15  gfa
	*** empty log message ***

# Revision 1.1  1997/05/09  14:33:46  gfa
# Initial revision
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

package mipssimulator;

import java.io.*;
import mipssimulator.Memory;


public abstract class MemoryRegion extends Object {
	
	public int from, to;
	byte memory[];
	
	byte readByte(int address) throws BusErrorException {
		return memory[address-from];
	}
	void writeByte(int address, byte data) throws BusErrorException {	
		memory[address-from] = data;
	}
	short readShort(int address) throws BusErrorException {
		if (Simulator.bigEndian) {
			return (short)(((short)memory[address-from] << 8) |
					(((short)memory[address-from+1])&0x00ff));
		}
		else {
			return (short)(((short)memory[address-from+1] << 8) |
					(((short)memory[address-from])&0x00ff));
		}
	}
	void writeShort(int address, short data) throws BusErrorException {	
		if (Simulator.bigEndian) {
			memory[address-from] = (byte)(data >> 8);
			memory[address-from+1] = (byte)(data & 0x00ff);			
		}
		else {
			memory[address-from+1] = (byte)(data >> 8);
			memory[address-from] = (byte)(data & 0x00ff);
		}
	}
	int readInt(int address) throws BusErrorException {
		int i = address-from;

		if (Simulator.bigEndian) {			
			return (((int)memory[i] << 24) |
					(((int)memory[i+1] << 16)&0x00ff0000) |
					(((int)memory[i+2] << 8)&0x0000ff00) |
					(((int)memory[i+3])&0x000000ff));
		}
		else {
			return (((int)memory[i+3] << 24) |
					(((int)memory[i+2] << 16)&0x00ff0000) |
					(((int)memory[i+1] << 8)&0x0000ff00) |
					(((int)memory[i])&0x000000ff));
		}
	}
	void writeInt(int address, int data) throws BusErrorException {	
		int i = address-from;
			
		if (Simulator.bigEndian) {
			memory[i] = (byte)(data >> 24);
			memory[i+1] = (byte)((data & 0x00ff0000) >> 16);
			memory[i+2] = (byte)((data & 0x0000ff00) >> 8);
			memory[i+3] = (byte)(data & 0x000000ff);
		}
		else {
			memory[i+3] = (byte)(data >> 24);
			memory[i+2] = (byte)((data & 0x00ff0000) >> 16);
			memory[i+1] = (byte)((data & 0x0000ff00) >> 8);
			memory[i] = (byte)(data & 0x000000ff);
		}
	}
	
	void checkInterrupt(Processor p) throws IOException {
	}
}

