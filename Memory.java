/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.
	
	This is a model for physical RAM, ROM and mapped IO resources.

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Memory.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
	$Log: Memory.java,v $
	Revision 1.1  1997/05/09 14:33:46  gfa
	Initial revision

# Revision 1.2  1997/03/12  17:45:58  gfa
# added message
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

import java.util.*;
import MemoryRegion;

public class Memory extends Object {
	
	Vector regionList;
	
	public Memory() {
		regionList = new Vector();
	}
	
	void addRegion(MemoryRegion r) {
		regionList.addElement(r);
	}

	MemoryRegion findRegion(int address) throws BusErrorException {
		int i;
		MemoryRegion r;
		
		for (i = 0; i < regionList.size(); i++) {
			r = (MemoryRegion)(regionList.elementAt(i));
			if ((address >= r.from) && (address <= r.to)) {				
				return r;
			}
		}		
		// no matching region found	
		throw new BusErrorException("Memory: faulting phys addr: " + 
					Integer.toHexString(address));	
	}

	public byte readByte(int address) throws BusErrorException {
		return findRegion(address).readByte(address);
	}	
	public void writeByte(int address, byte data) throws BusErrorException {
		findRegion(address).writeByte(address, data);
	}	
	public short readShort(int address) throws BusErrorException { 
		return findRegion(address).readShort(address);
	}
	public void writeShort(int address, short data) throws BusErrorException {
		findRegion(address).writeShort(address, data);
	}	
	public int readInt(int address) throws BusErrorException { 
		return findRegion(address).readInt(address);
	}	
	public void writeInt(int address, int data) throws BusErrorException {
		findRegion(address).writeInt(address, data);
	}
}

