/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.
	
	Basic concept taken from the SimOS debugger interface. Debugger must run
	as a thread since there is only a blocking accept (i.e. no select)

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Debugger.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.2 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/11 18:33:59 $      by: $Author: gfa $
	
	
	$Log: Debugger.java,v $
	Revision 1.2  1997/05/11 18:33:59  gfa
	fixed parseInt from int to long for Java 1.1

# Revision 1.1  1997/05/09  14:33:46  gfa
# Initial revision
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

import Memory;
import Processor;
import CoProc0;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

public class Debugger extends MemoryRegion implements Runnable {

	final int  GDB_NUM_REGS = 71;	
	final int  ZERO_REGNUM  =  0;         /* first integer register */
	final int  RA_REGNUM    = 31;         /* last integer register */
	final int  FP0_REGNUM   = 32;         /* first floating point register */
	final int  FP31_REGNUM  = 63;         /* last floating point register */
	final int  PC_REGNUM    = 64;         /* Contains program counter */
	final int  CAUSE_REGNUM = 65;         /* CAUSE register */
	final int  BAD_REGNUM   = 66;         /* BADVADDR register */
	final int  HI_REGNUM    = 67;         /* Multiple/divide temp */
	final int  LO_REGNUM    = 68;         /* ... */
	final int  FCRCS_REGNUM = 69;         /* FP control/status */
	final int  FCRIR_REGNUM = 70;         /* FP implementation/revision */

	final int DEBUG_PORT = 2345;
	final String SIGINT = "02";				/* sig interrupt */
	
	final boolean protocolDebug = false;

	ServerSocket listener;
	Socket gdb;
	boolean connected = false;
	public boolean stopped = false;
		

	Debugger() throws IOException {
		listener = new ServerSocket(DEBUG_PORT);
	}
	
	public void run() {
		while (true) {
			try {
				gdb = listener.accept();
				connected = true;
				System.out.println("\nGDB connection from " + 
										gdb.getInetAddress().toString());
			}
			catch (IOException e) {
				System.out.println(
					e.getClass().getName()+" (" + e.getMessage() + ")\n");
			}
		}
	}	
	
	void checkInterrupt(Processor p) throws IOException {
		int i = 0;
		char ch;
		String cmdBuffer = null;
		// like a device checking interrupts, we have a look if 
		// someone down the line pressed ^c
		if (connected) {
			// if already stopped, breakpoint or ^c pressed
			if (!stopped) {
				if (gdb.getInputStream().available() == 0) {
					return;
				}
				// commands from gdb pending
				else {
					cmdBuffer = getPacket (gdb);
					// the only thing we expect to see when polling is the
					// break instruction
					if (cmdBuffer.charAt(0) == 'p') {
						putPacket (gdb, "00000000" + "," + "01");
						stopped = true;
					}
					else if (cmdBuffer.charAt(0) == 'b') {
						putPacket (gdb, "S" + SIGINT + "00");
						stopped = true;
					}
					else {
						System.out.print("Unexpected command '" + cmdBuffer + 
												"', expected ^c or getpid\n");
						return;
					}
				}
			}
			/////////////////////////////////////////////////////////////////
			// ready to process commands
			
			while (stopped || (gdb.getInputStream().available() > 0)) {
				i = 0;
				cmdBuffer = getPacket(gdb);	
				ch = cmdBuffer.charAt(i++);
		
				switch (ch) {
				case '?':
					cmdBuffer = "S0000";
					break;
				case 'b':
					// nop: this is the packet sent when the user types ^C.		
					stopped = true;
					cmdBuffer = "S" + SIGINT + "00";
					stopped = true;
					break;
				case 'g': // read registers
					cmdBuffer = "";
					for (i = ZERO_REGNUM; i <= RA_REGNUM; i++) {
						cmdBuffer += toHex(p.register[i]);
					}
					for (i = FP0_REGNUM; i <= FP31_REGNUM; i++) {
						cmdBuffer += "00000000";
					}
					cmdBuffer += toHex(p.pc); // pc
					cmdBuffer += toHex(p.cp0.register[p.cp0.CAUSE]); // cause
					cmdBuffer += toHex(p.cp0.register[p.cp0.BADVA]); // bad
					cmdBuffer += toHex(p.hi); // hi
					cmdBuffer += toHex(p.lo); // lo
					cmdBuffer += "00000000"; // resvd
					cmdBuffer += "00000000"; // resvd
					break;
				case 'G': // write registers
					int j = 3;
					for (i = ZERO_REGNUM; i <= RA_REGNUM; i++) {
						p.register[i] = toInt(cmdBuffer.substring(j, j+8));
						j += 8;
					}
					j = 3 + PC_REGNUM*8;
					p.setPC(toInt(cmdBuffer.substring(j, j+8))); j += 8; // pc
					p.cp0.register[p.cp0.CAUSE] = 
						toInt(cmdBuffer.substring(j, j+8)); j += 8;    // cause
					p.cp0.register[p.cp0.BADVA] = 
						toInt(cmdBuffer.substring(j, j+8)); j += 8;    // bad
					p.hi = toInt(cmdBuffer.substring(j, j+8)); j += 8; // hi
					p.lo = toInt(cmdBuffer.substring(j, j+8)); j += 8; // lo
					cmdBuffer = "Ok";
					break;
				case 'c': // continue
					stopped = false;
					cmdBuffer = ""; //"S0000";
					break;
				case 'C':
				case 'k': // continue and drop connection
					gdb.close();
					stopped = false;
					connected = false;
					return;
				case 's':
				case 'S': // single step
					cmdBuffer = "S0000";
					putPacket(gdb, cmdBuffer);
					return;	
				case 'p':
					cmdBuffer = "00000000,01"; // format PID,NUM_CPUS
					break;
				case 'm': // read from memory
					cmdBuffer = readMemory(cmdBuffer, p);
					break;
				case 'M': // write to memory
					cmdBuffer = writeMemory(cmdBuffer, p);
					break;
				default:
					System.out.println("GDB: strange cmd '" + cmdBuffer + "'");
					cmdBuffer = "ENN";
					break;
				}
				putPacket(gdb, cmdBuffer);
			}
		}
	}
	
	String toHex(int x) {
		String tmp = Integer.toHexString(x);
		String ret = "";
		for (int i = 0; i < 8-tmp.length(); i++) ret += "0";
		return ret + tmp;
	}
	
	String toHexByte(byte x) {
		String tmp = Integer.toHexString((int)x & 0x000000ff);
		String ret = "";
		if (tmp.length() == 1) ret = "0";
		return ret + tmp;
	}
	
	int toInt(String s) {
		return (int)Long.parseLong(s, 16);
	}
	
	String checkSum(String s) {
		String tmp;
		int sum = 0;
		for (int i = 0; i < s.length(); i++) {
			sum += (int)(s.charAt(i));
		}
		sum = sum % 0x100;
		tmp = Integer.toHexString(sum);
		if (tmp.length() == 1) tmp = "0" + tmp;
		return tmp;
	}
	
	String getPacket(Socket gdb) throws IOException {
		char c1, c2;
		byte b[] = new byte[1];
		String s = new String();
		int c;
		
		try {
			while (true) {
				gdb.getInputStream().read(b);
				if (b[0] == (byte)'$') break;
			}
			while (true) {
				gdb.getInputStream().read(b);
				if (b[0] == (byte)'#') break;
				s += (new String(b, 0, b.length));
			}			
			if (protocolDebug) System.out.println("recv:"+ s);

			gdb.getInputStream().read(b); 
			gdb.getInputStream().read(b);
			
			gdb.getOutputStream().write((byte)('+')); // send an ACK
		}
		catch (IOException e) {
			connected = false;
			System.out.println(
			"GDB: " + e.getClass().getName()+" (" + e.getMessage() + ")\n");
		}
		return s;	
	}
	
	void putPacket(Socket gdb, String s) throws IOException {
		int len = s.length() + 4;
		byte byteBuffer[];
		byte answer[] = new byte[1];

		String sendBuffer = '$' + s + '#' + checkSum(s);

		if (protocolDebug) System.out.println("send:"+ sendBuffer);

		byteBuffer = sendBuffer.getBytes();

		try {		
			/* Send it over and over until we get a positive ack.  */
			do {
				gdb.getOutputStream().write(byteBuffer);
				gdb.getInputStream().read(answer);
			} while (answer[0] != '+');
		}
		catch (IOException e) {
			connected = false;
			System.out.println(
			"GDB: " + e.getClass().getName()+" (" + e.getMessage() + ")\n");
		}
	}
	
	String readMemory(String command, Processor p) {
		String reply = "";
		byte mem;
		int i;
		
		for (i = 4; i < command.length(); i++) {
			if (command.charAt(i) == ',') break;
		}
		int at = toInt(command.substring(4, i));
		int count = toInt(command.substring(i+1));
	
		try {
			for (i = 0; i < count; i++) { 
				mem = p.memory.readByte(p.cp0.translate(at, p.READ));
				reply += toHexByte(mem);
				at++;
			}
		}
		catch (Exception e) {
			return "ENN";
		}
		return reply;
	}	
	
	String writeMemory(String command, Processor p) {
		
		int value, i, j;
		
		for (i = 4; i < command.length(); i++) {
			if (command.charAt(i) == ',') break;
		}
		for (j = 4; j < command.length(); j++) {
			if (command.charAt(j) == ':') break;
		}
		int at = toInt(command.substring(4, i));
		int count = toInt(command.substring(i+1, j));
		String data = command.substring(j+1);
		try {
			for (i = 0; i < count; i++) { 
				value = toInt(data.substring(i*2, i*2 + 2));
				p.memory.writeByte(p.cp0.translate(at, p.WRITE), (byte)value);
				at++;
			}
		}
		catch (Exception e) {
			return "ENN";
		}
		return "Ok";
	}
}
