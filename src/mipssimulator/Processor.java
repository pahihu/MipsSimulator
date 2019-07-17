/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.

	The opcode switch was adapted from the Nachos machine/instruction code
	and extended with the coproc0 instructions.

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Processor.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.3 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/06/16 12:16:39 $      by: $Author: gfa $
	
	
	$Log: Processor.java,v $
	Revision 1.3  1997/06/16 12:16:39  gfa
	checks interrupts only every 16th cycle (great speedup)

# Revision 1.2  97/05/28  12:28:06  gfa
# added fpga dummy device
# 
# Revision 1.1  1997/05/09  14:33:46  gfa
# Initial revision
#
# Revision 1.16  1997/04/23  16:35:52  conrad
# correction of lwl, lwr, swl, swr
#
# Revision 1.15  1997/04/21  16:08:05  gfa
# switched input device from ttya to ttyb
#
# Revision 1.14  1997/04/14  21:04:42  conrad
# fixed UART ...
#
# Revision 1.13  1997/04/12  10:47:44  gfa
# adjusted trace/verbose options
#
# Revision 1.12  1997/04/06  12:59:41  gfa
# adapted for the applet interface, processor runs also as a thread...
#
# Revision 1.11  1997/03/27  11:17:49  conrad
# fixed bug for OR command ...
#
# Revision 1.10  1997/03/27  08:21:42  gfa
# ip mask enabled
#
# Revision 1.9  1997/03/26  10:55:49  gfa
# added big/little endian switch for byte addressed devices
#
# Revision 1.8  97/03/18  21:19:09  gfa
# fixed SLTU and SLTIU signed java arithmetic (grrr!)
# 
# Revision 1.7  97/03/18  12:46:05  conrad
# restore from 1.5.1.2 (rcs problem ...)
# 
# Revision 1.5.1.2  1997/03/18  12:45:09  conrad
# rcs problem ...
#
# Revision 1.5.1.1  1997/03/18  11:08:24  conrad
# debugging of clock
#
# Revision 1.5  1997/03/12  17:48:13  gfa
# uniform exception handling
#
# Revision 1.3  1997/03/12  11:21:16  conrad
# *** empty log message ***
#
# Revision 1.2  1997/03/11  11:32:28  gfa
# fixed multu
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

package mipssimulator;

import java.util.*;
import java.io.IOException;
import java.lang.Thread;
import mipssimulator.Memory;
import mipssimulator.Debugger;


public class Processor extends Object implements Runnable {
	
	public final int BOOTSTACK = 0x800ffffc; // top of phys
	public final int INTERRUPTVECTOR = 0x80000080;
	public final int UTLBMISSVECTOR = 0x80000000;
	public final int RESETVECTOR = 0xbfc00000;	// virt(0x1fc00000)
	public final int RAMSIZE = 1024*1024;
	public final int ROMSIZE = 128*1024;
	public final int RAMBASE = 0x00000000;
	public final int ROMBASE = 0x1fc00000; // phys(RESETVECTOR)
	public final int UART0BASE = 0x1fe00000;
	public final int UART1BASE = UART0BASE+0x20;
	public final int CLOCKBASE = 0x1f800000;
	public final int FPGABASE = 0x1fa00000;
	public final String ROMNAME = "boot.eprom";
	public final int REGCOUNT = 32;

	public final int SIGN_BIT = 0x80000000;
	public final int R31	=	31;
	public final int STACKREG = 29;	// User's stack pointer
	public final int FRAMEREG = 30; // Framepointer
	public final int RetAddrReg	= 31;	// Holds return address for 
										// procedure calls
	// exceptions
	public final int UTLBMISSEXCEPTION = -1; // not a true exception,
											 // has its own handler
	public final int EXTINTERRUPT = 0;
	public final int TLBMODIFIED = 1;
	public final int TLBLOADMISS = 2;
	public final int TLBSTOREMISS = 3;
	public final int ADDRERRORLOAD = 4;
	public final int ADDRERRORSTORE = 5;
	public final int BUSERRORFETCH = 6;
	public final int BUSERRORDATA = 7;
	public final int SYSCALL = 8;
	public final int BREAKPOINT = 9;
	public final int RESINSTRUCTION = 10;
	public final int CPUNUSABLE = 11;
	public final int OVERFLOW = 12;
	
	public final boolean WRITE = true;
	public final boolean READ = !WRITE;
	
	public Instruction currentInstr = new Instruction(0);
	public Memory memory;
	public ROM rom;
	Debugger gdb;
	public CoProc0 cp0;
	Vector interruptList;
	public int pc;
	public int hi, lo;
	public int register[] = new int[REGCOUNT];
	int loadReg, loadValueReg, nextPCReg;
	
	Processor() throws BusErrorException, IOException {
		int i;
		UART uart0, uart1;
		Clock clock;
		FPGA_NetworkDevice fpga;
		
		// init processor state
		pc = RESETVECTOR;
		nextPCReg = pc+4;
		hi = 0; lo = 0;
		for (i = 0; i < REGCOUNT; i++) { register[i] = 0; }
		register[STACKREG] = BOOTSTACK;
		
		// create system coproc
		cp0 = new CoProc0(this);
		
		// optional: FPU
		// ...
		
		// handle memory
		memory = new Memory();
		memory.addRegion(new RAM(RAMBASE, RAMSIZE));
		
		// these devices are byte-addressed and use offsets of
		// 0, 4, 8, etc
		if (Simulator.bigEndian) {
			memory.addRegion(uart0 = new UART(UART0BASE+3));
			memory.addRegion(uart1 = new UART(UART1BASE+3));
			memory.addRegion(clock = new Clock(CLOCKBASE+3));
		} else {
			memory.addRegion(uart0 = new UART(UART0BASE));
			memory.addRegion(uart1 = new UART(UART1BASE));
			memory.addRegion(clock = new Clock(CLOCKBASE));
		}
		memory.addRegion(fpga = new FPGA_NetworkDevice(FPGABASE));
		memory.addRegion(rom = new ROM(ROMBASE, ROMSIZE, ROMNAME));
		memory.addRegion(gdb = new Debugger()); 
		new Thread(gdb).start(); 
		
		// init interrupts
		interruptList = new Vector();
      		interruptList.addElement(clock);
		// whoever checks interrupts first gets the input first
		// here, uart1 (ttyb) is the input device
		interruptList.addElement(uart1);
		interruptList.addElement(uart0);

		// whoever checks interrupts first gets the input first
		// here, uart0 (ttya) is the input device
		//interruptList.addElement(uart0);
		//interruptList.addElement(uart1);
	}
	
	public void run() {
	
                int i = 0;
		while (true) {
			try {
				currentInstr.value = memory.readInt(cp0.translate(pc, READ));
				currentInstr.decode();
				if (Simulator.traceMode) {
					where();
				}
				Thread.yield(); 	  // give debugger a chance to accept
				gdb.checkInterrupt(this); // gdb is independent of CP0
				exec(currentInstr);
                                if (i == 16) { 
                                    checkInterrupts(); 
				    i = 0;
                                }
				i++;
			}
			catch (BusErrorException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(BUSERRORDATA);
			}
			catch (TLBLoadMissException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(TLBLOADMISS);
			}
			catch (TLBStoreMissException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(TLBSTOREMISS);
			}
			catch (TLBModifiedException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(TLBMODIFIED);
			}
			catch (UTLBMissException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(UTLBMISSEXCEPTION);
			}
			catch (AddressErrorLoadException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(ADDRERRORLOAD);
			}
			catch (AddressErrorStoreException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(ADDRERRORSTORE);
			}
			catch (ReservedInstructionException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				exception(RESINSTRUCTION);
			}
			catch (CPRegUnavailableException e) {
				verbose(e.getClass().getName()+" (" + e.getMessage() + ")\n");
				verbose("CP register unavailable\n");
			}
			catch (BreakpointException e) {
				// debugger support, do not jump to exception code
				// this is not the machines natural behaviour, but it
				// allows us to use a simple debugger interface
				// When we continue PC points till to the same location
				// and we hope that gdb replaces the BREAK instruktion by
				// the old one he has stolen...	
				gdb.stopped = true;
				try {
				    gdb.checkInterrupt(this); 
				    		// go directly to gdb handler
				}
				catch (IOException exc) {
				    verbose("IOException in gdb\n");
				}
			}
			catch (MemoryException e) {
				// this is a shortcut out of the interpreter 
				// switch statement. 
				verbose("unknown exception: " + 
					e.getClass().getName()+" (" + e.getMessage() + ")\n");
			}
			catch (Exception e) {
				verbose("unknown exception: " + 
				e.getClass().getName()+" (" + 
				e.getMessage() + ")\n");
			}
		}
	}
	
	void exec(Instruction i) throws Exception {
  
		int nextLoadReg = 0; 	
		int nextLoadValue = 0; 	// record delayed load operation, to apply
					// in the future
		// Compute next pc, but don't install in case there's an error or branch.
		int pcAfter = nextPCReg + 4;
		int sum, diff, tmp, value;
		short shortValue;
		int rs, rt, imm;
		
		switch (i.opCode) {
			case Instruction.OP_ADD:
				sum = register[i.rs] + register[i.rt];
				if (!(((register[i.rs] ^ register[i.rt]) & SIGN_BIT) == 
					SIGN_BIT) &&
					(((register[i.rs] ^ sum) & SIGN_BIT) == SIGN_BIT)) {
					exception(OVERFLOW);
					return;
				}
				register[i.rd] = sum;
				break;
			case Instruction.OP_ADDI:
				sum = register[i.rs] + i.extra;
				if (!(((register[i.rs] ^ i.extra) & SIGN_BIT) == SIGN_BIT) &&
					(((i.extra ^ sum) & SIGN_BIT) == SIGN_BIT)) {
					exception(OVERFLOW);
					return;
				}
				register[i.rt] = sum;
				break;
			case Instruction.OP_ADDIU:
				register[i.rt] = register[i.rs] + i.extra;
				break;
			case Instruction.OP_ADDU:
				register[i.rd] = register[i.rs] + register[i.rt];
				break;
			case Instruction.OP_AND:
				register[i.rd] = register[i.rs] & register[i.rt];
				break;
			case Instruction.OP_ANDI:
				register[i.rt] = register[i.rs] & (i.extra & 0xffff);
				break;
			case Instruction.OP_BEQ:
				if (register[i.rs] == register[i.rt])
					pcAfter = nextPCReg + indexToAddr(i.extra);
				break;
			case Instruction.OP_BGEZAL:
				register[R31] = nextPCReg + 4;
			case Instruction.OP_BGEZ:
				if (!((register[i.rs] & SIGN_BIT) == SIGN_BIT))
					pcAfter = nextPCReg + indexToAddr(i.extra);
				break;
			case Instruction.OP_BGTZ:
				if (register[i.rs] > 0)
					pcAfter = nextPCReg + indexToAddr(i.extra);
				break;
			case Instruction.OP_BLEZ:
				if (register[i.rs] <= 0)
					pcAfter = nextPCReg + indexToAddr(i.extra);
				break;
			case Instruction.OP_BLTZAL:
				register[R31] = nextPCReg + 4;
			case Instruction.OP_BLTZ:
				if ((register[i.rs] & SIGN_BIT) == SIGN_BIT)
					pcAfter = nextPCReg + indexToAddr(i.extra);
				break;
			case Instruction.OP_BNE:
				if (register[i.rs] != register[i.rt])
					pcAfter = nextPCReg + indexToAddr(i.extra);
				break;
			case Instruction.OP_BREAK:
				throw new BreakpointException();
			case Instruction.OP_DIV:
				if (register[i.rt] == 0) {
					lo = 0;
					hi = 0;
				} else {
					lo =  register[i.rs] / register[i.rt];
					hi = register[i.rs] % register[i.rt];
				}
				break;
			case Instruction.OP_DIVU:	  
				rs = register[i.rs]; // was cast to unsigned int
				rt = register[i.rt];
				if (rt == 0) {
				  lo = 0;
				  hi = 0;
				} else {
				  tmp = rs / rt;
				  lo = (int) tmp;
				  tmp = rs % rt;
				  hi = (int) tmp;
				}
				break;
			case Instruction.OP_JAL:
				register[R31] = nextPCReg + 4;
			case Instruction.OP_J:
				pcAfter = (pcAfter & 0xf0000000) | indexToAddr(i.extra);
				break;
			case Instruction.OP_JALR:
				register[i.rd] = nextPCReg + 4;
			case Instruction.OP_JR:
				pcAfter = register[i.rs];
				break;
			case Instruction.OP_LB:
			case Instruction.OP_LBU:
				tmp = register[i.rs] + i.extra;
				value = (int)(memory.readByte(cp0.translate(tmp, READ)));
				if (((value & 0x80) > 0) && (i.opCode == Instruction.OP_LB))
					value |= 0xffffff00;
				else
					value &= 0xff;
				nextLoadReg = i.rt;
				nextLoadValue = value;
				break;
			case Instruction.OP_LH:
			case Instruction.OP_LHU:	  
				tmp = register[i.rs] + i.extra;
				if ((tmp & 0x1) > 0) {
					exception(ADDRERRORLOAD);
					return;
				}
				value = (short)(memory.readShort(cp0.translate(tmp, READ)));
				if (((value & 0x8000) > 0) && (i.opCode == Instruction.OP_LH))
					value |= 0xffff0000;
				else
					value &= 0xffff;
				nextLoadReg = i.rt;
				nextLoadValue = value;
				break;
			case Instruction.OP_LUI:
				register[i.rt] = i.extra << 16;
				break;
			case Instruction.OP_LW:
				tmp = register[i.rs] + i.extra;
				if ((tmp & 0x3) > 0) {
					exception(ADDRERRORLOAD);
					return;
				}
				value = memory.readInt(cp0.translate(tmp, READ));
				nextLoadReg = i.rt;
				nextLoadValue = value;
				break;
			case Instruction.OP_LWL:	  
				tmp = register[i.rs] + i.extra;

				if (loadReg == i.rt)
					nextLoadValue = loadValueReg;
				else
					nextLoadValue = register[i.rt];
				shortValue = memory.readShort(cp0.translate(tmp, READ));
				nextLoadValue = (shortValue << 16) | (nextLoadValue & 0x0000ffff);
				nextLoadReg = i.rt;
				
				break;
			case Instruction.OP_LWR:
				tmp = register[i.rs] + i.extra;

				if (loadReg == i.rt)
					nextLoadValue = loadValueReg;
				else
					nextLoadValue = register[i.rt];
				shortValue = memory.readShort(cp0.translate(tmp-1, READ));
				nextLoadValue = (shortValue) | (nextLoadValue & 0xffff0000);
				nextLoadReg = i.rt;
				
				break;
			case Instruction.OP_MFCP0:
				register[i.rt] = cp0.getRegister(i.rd);
				break;
			case Instruction.OP_MTCP0:
				cp0.putRegister(i.rd, register[i.rt]);
				break;
			case Instruction.OP_MFHI:
				register[i.rd] = hi;
				break;
			case Instruction.OP_MFLO:
				register[i.rd] = lo;
				break;
			case Instruction.OP_MTHI:
				hi = register[i.rs];
				break;
			case Instruction.OP_MTLO:
				lo = register[i.rs];
				break;
			case Instruction.OP_MULT:
				mult(register[i.rs], register[i.rt], false);
				break;
			case Instruction.OP_MULTU:
				mult(register[i.rs], register[i.rt], true);
				break;
			case Instruction.OP_NOR:
				register[i.rd] = ~(register[i.rs] | register[i.rt]);
				break;
			case Instruction.OP_OR:
				register[i.rd] = register[i.rs] | register[i.rt];
				break;
			case Instruction.OP_ORI:
				register[i.rt] = register[i.rs] | (i.extra & 0xffff);
				break;
			case Instruction.OP_RFE:
				cp0.rfeInstruction(); // restore status register
				break;
			case Instruction.OP_SB:
				memory.writeByte(
					cp0.translate(register[i.rs]+i.extra, WRITE), 
					(byte)(register[i.rt]));
				break;
			case Instruction.OP_SH:
				memory.writeShort(
					cp0.translate(register[i.rs]+i.extra, WRITE), 
					(short)(register[i.rt]));
				break;
			case Instruction.OP_SLL:
				register[i.rd] = register[i.rt] << i.extra;
				break;
			case Instruction.OP_SLLV:
				register[i.rd] = register[i.rt] <<
					(register[i.rs] & 0x1f);
				break;
			case Instruction.OP_SLT:
				if (register[i.rs] < register[i.rt])
					register[i.rd] = 1;
				else
					register[i.rd] = 0;
				break;
			case Instruction.OP_SLTI:
				if (register[i.rs] < i.extra)
					register[i.rt] = 1;
				else
					register[i.rt] = 0;
				break;
			case Instruction.OP_SLTIU:	  
				rs = register[i.rs];
				imm = i.extra;
				if (rs < 0)
					register[i.rt] = 0;
				else if (rs < imm)
					register[i.rt] = 1;
				else
				    	register[i.rt] = 0;
				break;
			case Instruction.OP_SLTU:	  
				rs = register[i.rs];
				rt = register[i.rt];
				if ((rs < 0 && rt >= 0) || (rs >= 0 && rt < 0)) 
				{				    
				    if (rs >= rt)
					    register[i.rd] = 1;
				    else
					    register[i.rd] = 0;
				} else {
				    if (rs < rt)
					    register[i.rd] = 1;
				    else
					    register[i.rd] = 0;
				}				
				break;
			case Instruction.OP_SRA:
				register[i.rd] = register[i.rt] >> i.extra;
				break;
			case Instruction.OP_SRAV:
				register[i.rd] = register[i.rt] >>
					(register[i.rs] & 0x1f);
				break;
			case Instruction.OP_SRL:
				tmp = register[i.rt];
				tmp >>>= i.extra;
				register[i.rd] = tmp;
				break;
			case Instruction.OP_SRLV:
				tmp = register[i.rt];
				tmp >>>= (register[i.rs] & 0x1f);
				register[i.rd] = tmp;
				break;
			case Instruction.OP_SUB:	  
				diff = register[i.rs] - register[i.rt];
				tmp = register[i.rs] ^ register[i.rt];
				if (((tmp & SIGN_BIT) == SIGN_BIT) && 
					(((register[i.rs] ^ diff) & SIGN_BIT) == SIGN_BIT)) {
					exception(OVERFLOW);
					return;
				}
				register[i.rd] = diff;
				break;
			case Instruction.OP_SUBU:
				register[i.rd] = register[i.rs] - register[i.rt];
				break;
			case Instruction.OP_SW:
				memory.writeInt(
					cp0.translate(register[i.rs]+i.extra, WRITE), 
					register[i.rt]);
				break;
			case Instruction.OP_SWL:	  
				tmp = register[i.rs] + i.extra;

				shortValue = (short)(register[i.rt] >>> 16);
				memory.writeShort(cp0.translate(tmp, WRITE), shortValue);
				
				break;
			case Instruction.OP_SWR:	  
				tmp = register[i.rs] + i.extra;

				shortValue = (short)(register[i.rt] & 0x0000ffff);
				memory.writeShort(cp0.translate(tmp-1, WRITE), shortValue);
				
				break;
			case Instruction.OP_SYSCALL:
				exception(SYSCALL);
				return; 
			case Instruction.OP_TLBR:
			case Instruction.OP_TLBWI:
			case Instruction.OP_TLBWR:
			case Instruction.OP_TLBP:
				cp0.handleTLBInstruction(i);
				break;
			case Instruction.OP_XOR:
				register[i.rd] = register[i.rs] ^ register[i.rt];
				break;
			case Instruction.OP_XORI:
				register[i.rt] = register[i.rs] ^ (i.extra & 0xffff);
				break;
			case Instruction.OP_RES:
			case Instruction.OP_UNIMP:
				throw new ReservedInstructionException();
			default: 
				throw new ReservedInstructionException();

		}
		// Do any delayed load operation
		register[loadReg] = loadValueReg;
		loadReg = nextLoadReg;
		loadValueReg = nextLoadValue;
		register[0] = 0; 	// and always make sure R0 stays zero.

		// Advance program counters.
		pc = nextPCReg;
		nextPCReg = pcAfter;			
	}
	
	final int indexToAddr(int x) {
		return (x << 2);
	}
	
	final void mult(int a, int b, boolean unsigned) {
		long al;
		long bl;
		long result;
		long r;
		
		al = (long)a; 
		bl = (long)b;
		if (unsigned) {
			r = 1024*1024*1024; // java bug?
			r = r*4;
			if (a < 0) { al = (long)a; al += r; }
			if (b < 0) { bl = (long)b; bl += r; }
//System.out.println("multu a=" + a + "  b=" + b +  "  r=" + r + "  al="+al+"  bl="+bl+"  result="+ al*bl);			
		}
		result = al*bl;
		hi = (int)(result >> 32);
		lo = (int)(result & 0x00000000ffffffff);
	}

	void checkInterrupts() throws Exception {
	    for (int i = 0; i < interruptList.size(); i++) {
		if (cp0.interruptEnabled(i)) {
	       ((MemoryRegion)interruptList.elementAt(i)).checkInterrupt(this);
		}
	    }
	}

	// external interrupts: exception 0, ip_no [0..5]
        // save oldPc for storing in EPC
	public void interrupt(int no) {
		boolean branchDelay = isBranchDelaySlot();		
		int oldPC = pc;

		verbose("hardware interrupt " + no + "\n");
		if (! Simulator.traceMode) where();

		pc = INTERRUPTVECTOR;
		nextPCReg = pc+4;
		cp0.interrupt(0, no, oldPC, branchDelay);
	}
	
	// other exceptions
	public void exception(int exception) {
		boolean branchDelay = isBranchDelaySlot();
		int oldPC = pc;
		
		if (! Simulator.traceMode) where();
				
		if (exception == CPUNUSABLE) 
		    cp0.setCPError(currentInstr.cpNo());
		
		if (exception == UTLBMISSEXCEPTION) {
			pc = UTLBMISSVECTOR;
		}
		else {
			pc = INTERRUPTVECTOR;
		}
		nextPCReg = pc+4;

		cp0.interrupt(exception, 0, oldPC, branchDelay);
	}
		
	boolean isBranchDelaySlot() {
		return ((pc+4) != nextPCReg);
	}
	
	// for trace mode and display on catastrophic events
	public void where() {
		verbose(Integer.toHexString(pc) + "   ");
		verbose(currentInstr.disassemble(nextPCReg) + "\n");
	}
	
	public void setPC(int newPC) {
		// the -4 compensates for the next pc+=4
		pc = newPC-4;
		nextPCReg = pc+4;
	}
		
	final void verbose(String s) {
		if (Simulator.verboseMode || Simulator.traceMode) 
		    System.err.print(s);
	}
}
