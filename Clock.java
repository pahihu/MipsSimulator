/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.
	
	This device is a simple version of an intel 8254 timer. Counter 2 
	divides the input clock (which is in our case ticking at every 
	instruction) and feeds the divided clock into both counter 0 and 1. 
	That's the way IDT wired their board...

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Clock.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
	$Log: Clock.java,v $
	Revision 1.1  1997/05/09 14:33:46  gfa
	Initial revision

# Revision 1.6  1997/04/14  21:04:59  conrad
# fixed UART ...
#
# Revision 1.5  1997/03/18  21:22:56  gfa
# *** empty log message ***
#
# Revision 1.4  97/03/18  11:08:24  conrad
# debugging of clock
# 
# Revision 1.3  1997/03/12  11:21:16  conrad
# *** empty log message ***
#
# Revision 1.2  1997/03/11  14:45:39  gfa
# fixed control mode bug
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

import MemoryRegion;
import ClockErrorException;

public class Clock extends MemoryRegion {
	
	final int CLOCKADDRSIZE = 0x14;
	final int COUNTER0 = 0x00;
	final int COUNTER1 = 0x04;
	final int COUNTER2 = 0x08;
	final int CONTROL = 0x0c;
	final byte LSBMSBRWMODE = 0x30;
	final byte CLOCKMODE2 = 0x04;

	// ctrl register: sc1 sc0 rw1 rw0 m2 m1 m0 bcd
	
	byte mode[] = {2,2,2};
	int counter[] = {0,0,0};
	int counterInitValue[] = {0,0,0};
	byte control;
	boolean lsb = true;
	
	public Clock(int baseAddress) {
		from = baseAddress; 
		to = baseAddress+CLOCKADDRSIZE;
	}
	
	// read/write depends on hi/low byte settings of the control register
	byte readByte(int address) throws ClockErrorException {
	  // must be allowed for use with IDT board :-(
	  // throw new ClockErrorException();
	  return 0;
	}
	
	void writeByte(int address, byte data) throws ClockErrorException {	
		int addr = address - from;
		byte mode = (byte)(LSBMSBRWMODE | CLOCKMODE2);

		if ((addr == COUNTER0) || (addr == COUNTER1) || (addr == COUNTER2)) {
			writeCountRegister(data, addr);
		}
		else if (addr == CONTROL) {
			if ((data & mode) == mode) {
				control = data;
				lsb = true;
			}
			else {
			    throw new ClockErrorException("i8254 mode not supported");
			}
		}
		else {
		  throw new ClockErrorException("i8254 invalid address");
		}
	}

	void writeCountRegister(byte data, int addr) {
		int regNo = addr >> 2;
		if (lsb) {
			counterInitValue[regNo] &= 0x0000ff00;
			counterInitValue[regNo] |= data;
			lsb = false;
		}
		else {
			int d = data << 8;
			d &= 0x0000ff00;
			counterInitValue[regNo] &= 0x000000ff;
			counterInitValue[regNo] |= d;
			counter[regNo] = counterInitValue[regNo];
		}
	}
	
	// check for clock interrupts, clock mode 2
	void checkInterrupt(Processor p) {	  
		counter[2]--;
		if (counter[2] == 1) {
			counter[2] = counterInitValue[2];
			counter[1]--;
			counter[0]--;
			if (counter[0] == 1) {
		                //System.out.println("clock 0 hit ");
				counter[0] = counterInitValue[0];
				p.interrupt(0);			// post interrupt #0
			}
			if (counter[1] == 1) {
				counter[1] = counterInitValue[1];
				//p.interrupt(1);			// post interrupt #1
			}
		}
	}
}	
