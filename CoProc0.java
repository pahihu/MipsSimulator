/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/CoProc0.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.3 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/06/13 19:32:39 $      by: $Author: gfa $
	
	
	$Log: CoProc0.java,v $
	Revision 1.3  1997/06/13 19:32:39  gfa
	optimized interruptEnabled(i)

# Revision 1.2  1997/05/11  17:09:23  gfa
# removed CPRegUnavailableException class for java 1.1
#
# Revision 1.1  1997/05/09  14:33:46  gfa
# Initial revision
#
# Revision 1.9  1997/04/12  11:46:50  gfa
# fixed BD bit reset
#
# Revision 1.8  1997/03/27  08:21:09  gfa
# fixed interruptEnabled(i) to eval the IP mask of status
#
# Revision 1.7  1997/03/18  12:51:29  conrad
# rcs problem ... (restore)
#
# Revision 1.6  1997/03/18  11:09:35  conrad
# clock debugging
#
# Revision 1.5  1997/03/12  17:48:28  gfa
# utlb via exception hamdled
#
# Revision 1.3  1997/03/12  11:49:14  gfa
# fixed exception mask or-ing
#
# Revision 1.2  1997/03/11  14:45:52  gfa
# *** empty log message ***
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

import java.lang.*;
import java.util.*;
import TLBEntry;
import Instruction;
import CPRegUnavailableException;


public class CoProc0 extends Object {
	
	// register numbers
	public final int STATUS = 12;	
	public final int CAUSE = 13;
	public final int EPC = 14;
	public final int TLBHI = 10;
	public final int TLBLO = 2;
	public final int INDEX = 0;
	public final int RANDOM = 1;
	public final int CONTEXT = 4;
	public final int BADVA = 8;
	public final int PRID = 15;
	
	public final static int PAGESIZE = 4096;
	final int NOFTLBENTRIES = 64;
	final int RESERVEDENTRIES = 8;
	final int PAGEFRAMEMASK = 0xfffff000;
	final int PAGEOFFSETMASK = 0x00000fff;
	final int CONTEXT_VPN_MASK = 0x001ffffc;
	final int VALIDBIT = 0x00000200;
	final int GLOBALBIT = 0x00000100;
	final int NONCACHEBIT = 0x00000800;
	final int DIRTYBIT = 0x00000400;
	
	final int EXCMASK = 0xffffff00;
	final int CEMASK = 0xcfffffff;
	final int KERNELMODE = 0x00000002; // KUcurrent bit
	final int INTENABLED = 0x00000001; // IEcurrent bit

	final int CP0 = 0x10000000;
	final int CP1 = 0x20000000;
	final int CP2 = 0x40000000;
	final int CP3 = 0x80000000;
	final int BOOTMODE = 0x00400000;
	final int TLBSHUTDOWN = 0x00200000;
	final int INTMASK = 0x0000ff00;
	
	// system registers
	public int[] register = new int[32];	
	TLBEntry[] tlb = new TLBEntry[NOFTLBENTRIES];
	
	// pointer back to master
	Processor proc;
	
	Random random = new Random(29);
	int tlbHint = 0;
	
	CoProc0(Processor proc) {
		this.proc = proc;
		// init PRID, init in kernel mode, interrupts disabled
		register[PRID] = 0x00000230; // proc id, MIPS R3000A compatible
		register[STATUS] = CP0 & BOOTMODE & TLBSHUTDOWN;	
		for (int i = 0; i < NOFTLBENTRIES; i++) tlb[i] = new TLBEntry();
	}
	
	public void interrupt(int exception, int no, int pc, boolean braDelay) {
		// badVMaddr alread set when exception was detected, this
		// happens while translating an address in translate(), physical()
		
		// set ip bits in status register, set kernel mode
		int tmp = register[STATUS] & 0x0000000f; // mask prev and current state
		register[STATUS] &= 0xffffffc0;
		register[STATUS] |= (tmp << 2); 
									// KUcurr=0 (kmode), IEcurr=0 (int disabl.)
		// set epc
		register[EPC] = pc;
		if (braDelay) register[EPC] = pc - 4; // point to the previous instr

		
		// setup exception cause and/or hardware interrupt number
		if (exception >= 0) { // filter out utlb misses (exc code = -1)
		    register[CAUSE] = register[CAUSE] & EXCMASK;
		    register[CAUSE] = register[CAUSE] | ((exception*4) & ~EXCMASK);
		    if (exception == 0) {
			register[CAUSE] = register[CAUSE] | ((1<<no)<<10); 
		    }
		}
		// old ip's remain in cause
		if (braDelay) register[CAUSE] = register[CAUSE] | 0x80000000;
		else register[CAUSE] = register[CAUSE] & 0x7fffffff;
		
		// if it was an UTLBmiss we must provide tlbhi and part of context
		// this is to minimize OS handling effort (OS needs only to set TLBLO)
		if (exception == proc.UTLBMISSEXCEPTION) {
			register[TLBHI] &= PAGEOFFSETMASK;
			register[TLBHI] |= (register[BADVA] & PAGEFRAMEMASK);
			register[CONTEXT] &= ~CONTEXT_VPN_MASK;
			register[CONTEXT] |= (CONTEXT_VPN_MASK & (register[TLBHI] >> 10));
		}		
	}

	public boolean interruptEnabled(int i) {	
	    int mask;
	    
	    if ((register[STATUS] & INTENABLED) == 0) return false;
	    
	    if ((i >= 0) && (i <= 5)) mask = 0x00000400 << i;
	    else if ((i >= 6) && (i <= 7)) mask = 0x00000100 << (i-6);
	    else return false;
	    return ((register[STATUS] & mask) != 0);
	}

	public void setCPError(int cpNo) {
		register[CAUSE] = register[CAUSE] & CEMASK;
		register[CAUSE] = register[CAUSE] & (cpNo<<28);
	}	
	
	public int getRegister(int no) throws CPRegUnavailableException {
		switch (no) {
			case STATUS: case EPC: case CAUSE: case TLBHI: case TLBLO:
			case INDEX: case CONTEXT: case BADVA: case PRID:
				return register[no];
			case RANDOM:
				return (randomIndex() << 8);
			default: throw new CPRegUnavailableException("r" + no);
		}
	}
	
	public void putRegister(int no, int data) throws CPRegUnavailableException 
	{
		switch (no) {
			case TLBLO: case TLBHI: case INDEX: case STATUS: case CAUSE: 
			case CONTEXT: 
				register[no] = data;
				break;
			default: throw new CPRegUnavailableException("r" + no);
		}
	}
	
	public void rfeInstruction() {
	  //System.out.print("sr before:" + Integer.toHexString(register[STATUS]));	  
		// restore status register, enter user mode
		int tmp = (register[STATUS] & 0x0000003f) >>> 2; // move prev to current
		register[STATUS] &= 0xffffffc0;
		register[STATUS] |= tmp;
		//System.out.print("  sr after:" + Integer.toHexString(register[STATUS]));	  
	}
	
	public void handleTLBInstruction(Instruction i) {
		int j;
		switch (i.opCode) {
			case i.OP_TLBR:
				j = (register[INDEX] & 0x00003f00) >> 8;
				register[TLBLO] = tlb[j].lo;
				register[TLBHI] = tlb[j].hi;
				break;
			case i.OP_TLBWI:
				j = (register[INDEX] & 0x00003f00) >> 8;
				tlb[j].lo = register[TLBLO];
				tlb[j].hi = register[TLBHI];
				break;
			case i.OP_TLBWR:
				j = randomIndex();
				tlb[j].lo = register[TLBLO];
				tlb[j].hi = register[TLBHI];
				break;
			case i.OP_TLBP:
				for (j = 0; j < NOFTLBENTRIES; j++) {
					if ((tlb[j].lo == register[TLBLO]) && 
											(tlb[j].hi == register[TLBHI])){
						register[INDEX] = j << 8;
						return;
					}
				}
				register[INDEX] = register[INDEX] | 0x80000000; 
														// set probe bit 31
				break;
			default:
		}
	}
	
	// reflects static part of address map of IDT R3052 extended architecture
	public int translate(int virt, boolean write) throws MemoryException {
		if (virt >= 0 && virt <= 0x7fffffff) { // kernel/user mapped
			return tlbLookup(virt, false, write);
		}
		// check for kernel mode first
		if ((register[STATUS] & KERNELMODE) != 0) {
			if (write) throw new AddressErrorStoreException("access violation at "
										+ Integer.toHexString(virt));
			else throw new AddressErrorLoadException("access violation at "
										+ Integer.toHexString(virt));
		}
		else if (virt > 0xc0000000 && virt <= 0xffffffff) { // kernel mapped
			return tlbLookup(virt, true, write);
		}
		else if (virt >= 0x80000000 && virt < 0xa0000000) { // kernel cached
			return virt-0x80000000;
		}
		else if (virt >= 0xa0000000 && virt < 0xc0000000) { // kernel uncached
			return virt-0xa0000000;
		}
		return 0;
	}
	
	final int tlbLookup(int virt, boolean kSeg2, boolean writeAccess) 
													throws MemoryException {
		// use last hit from tlbHint
		if (isTLBHit(virt, tlb[tlbHint])) {
			return physical(virt, tlb[tlbHint], writeAccess);
		}
		for (int i = 0; i < NOFTLBENTRIES; i++) {
			if (isTLBHit(virt, tlb[i])) {
				tlbHint = i;
				return (physical(virt, tlb[i], writeAccess));
			}
		}
		// generate tlb miss exception
		if (!kSeg2) {
			register[BADVA] = virt;
			throw new UTLBMissException("VM:" + Integer.toHexString(virt));
		}
		// kernel segment
		else {
			register[BADVA] = virt;
			throw new TLBLoadMissException("VM:" + Integer.toHexString(virt));
		}
	}
	
	final boolean isTLBHit(int virt, TLBEntry e) {
		return (e.hi & PAGEFRAMEMASK) == (virt & PAGEFRAMEMASK);
	}
	
	final int physical(int virt, TLBEntry e, boolean writeAccess) 
													throws MemoryException {
		// check valid bit, then dirty (read-only bit)
		// may generate TLB L/S miss
		if ((e.lo & VALIDBIT) == 0) {
			register[BADVA] = virt;
			if (writeAccess) {
				throw new TLBStoreMissException("VM:" + Integer.toHexString(virt));
			}
			else {
				throw new TLBLoadMissException("VM:" + Integer.toHexString(virt));
			}				
		}
		if (((e.lo & DIRTYBIT) == 0) && writeAccess) {
			register[BADVA] = virt;
			throw new TLBModifiedException("VM:" + Integer.toHexString(virt));
		}
		return ((tlb[tlbHint].lo & PAGEFRAMEMASK) | 
						(virt & PAGEOFFSETMASK));
	}
	
	final int randomIndex() {
		return ((random.nextInt() % (NOFTLBENTRIES - RESERVEDENTRIES)) + 
															RESERVEDENTRIES);
	}
}

