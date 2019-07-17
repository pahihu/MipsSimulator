/*
    Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
    Computer Engineering and Networks Laboratory. All rights reserved.

    Written by George Fankhauser <gfa@acm.org>. For more documentation
    please visit http://www.tik.ee.ethz.ch/~gfa.
    
    Simulated UART is a Signetics SCN2681 (Philips) or 68HC2681 (Motorola).

    
    File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/UART.java,v $
    Author(s):             G. Fankhauser
    Affiliation:           ETH Zuerich, TIK
    Version:               $Revision: 1.1 $
    Creation Date:         December 1996
    Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
    
    
    $Log: UART.java,v $
    Revision 1.1  1997/05/09 14:33:46  gfa
    Initial revision

# Revision 1.5  1997/04/12  16:02:22  gfa
# interrupt capable version
#
# Revision 1.4  1997/03/23  12:42:38  gfa
# UART read hack (not really async because this machine is too slow :-)
#
# Revision 1.3  1997/03/18  19:01:32  gfa
# changed to real SCN2681 behaviour which requires status polling
#
# Revision 1.2  97/03/11  14:45:52  gfa
# *** empty log message ***
# 
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

import java.io.*;
import java.util.*;
import MemoryRegion;


public class UART extends MemoryRegion {
	
	final int UARTADDRSIZE = 0x1F;
	final int STATUS_REGISTER = 0x04;
	final int COMMAND_REGISTER = 0x08;
	final int TX_REGISTER = 0x0c;
	final int RX_REGISTER = TX_REGISTER;
	final int MODE_REGISTER = 0x0;
	
	final byte RECV_MASK = 0x01;
	final byte READY_TO_SEND = 0x04;
	 
//	DataInputStream in = new DataInputStream(System.in);
	InputStream in = System.in;
	int subcycles = 0;
	
	public UART(int baseAddress) {
		from = baseAddress; 
		to = baseAddress+UARTADDRSIZE;
		memory = new byte[to-from+1];
	}
	
	// read
	byte readByte(int address) throws BusErrorException {
		int offset = address-from;
		
		if (offset == STATUS_REGISTER) {
		    // simulator is always ready to send...
		    return (byte)(memory[STATUS_REGISTER] | READY_TO_SEND); 
		}
		else if (offset == RX_REGISTER) {
		    memory[STATUS_REGISTER] &= ~RECV_MASK; // clear status
//System.out.println("rx read out: '0x" + Integer.toHexString(memory[RX_REGISTER]) + "");
		    return memory[RX_REGISTER];
		}
		else if (offset == MODE_REGISTER) {
		    return memory[MODE_REGISTER];
		}
		return 0;
	}
	
	void writeByte(int address, byte data) throws BusErrorException {	
	    int offset = address-from;
	    
	    super.writeByte(address, data);
	    if (offset == TX_REGISTER) {
		System.out.write(data);
		System.out.flush();
	    }
	    else if (offset == MODE_REGISTER) {
	    }
	}
	
	void checkInterrupt(Processor p) throws IOException {

		try {
			if (in.available() > 0) {
			    memory[RX_REGISTER] = (byte)(in.read());
			    memory[STATUS_REGISTER] |= RECV_MASK; // set
//System.out.println("sim read: '0x" + Integer.toHexString(memory[RX_REGISTER]) + "");
			    p.interrupt(5);
			}
			
			//if ((in.available() > 0) && 
			  //((memory[STATUS_REGISTER] & RECV_MASK) == 0))	{
				// read char into RX when the old one was 
				// fetched, set status reg, post interrupt #5
			//	memory[RX_REGISTER] = (byte)(in.read());
//System.out.println("sim read: '0x" + Integer.toHexString(memory[RX_REGISTER]) + "");

				//memory[STATUS_REGISTER] |= RECV_MASK; // set
			//}
			else {
			}
		}
		catch (IOException e) {
			System.out.print(e.getClass().getName()+
						" ("+e.getMessage()+")\n");
			System.out.println("UART: couldn't read stdin");
		}
	}
}	
	
