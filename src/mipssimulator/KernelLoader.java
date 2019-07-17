/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/KernelLoader.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
	$Log: KernelLoader.java,v $
	Revision 1.1  1997/05/09 14:33:46  gfa
	Initial revision

# Revision 1.2  1997/04/06  12:59:41  gfa
# adapted for the applet interface, processor runs also as a thread...
#
# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

package mipssimulator;

import java.lang.*;
import java.io.*;
import mipssimulator.Memory;
import mipssimulator.Processor;

public class KernelLoader extends Object {
	
    static final byte nullChar = (byte)'0';
    static final byte capAChar = (byte)'A';

    KernelLoader(RandomAccessFile f, Memory m, Processor p) throws Exception
    {		
        /*  s-record loader, supports only 32bit addresses and transfer:
                S3   Data record with 32 bit load address                            
                S7   Termination record with 32 bit transfer address

                Stnnaaaaaaaa[dddd...dddd]cc

                t record type field (0,1,2,3,6,7,8,9). 		
                nn record length field, number of bytes in record excluding record type 
                and record length. 
                a...a load address field, can be 16, 24 or 32 bit address for data to 
                be loaded. 
                d...d data field, actual data to load, each byte is encoded in 2 
                characters. 
                cc checksum field, 1's complement of the sum of all bytes in the record
                length, load address and data fields 
        */
        try {
            int i;
            while (true) {
                String s = f.readLine(); 
                if (s.charAt(0) != 'S') throw new KernelLoaderException();
                if (s.charAt(1) == '3') {
                    int count = (int)readHexByte(s, 2);
                    int start = readHexInt(s, 4);
                    if (Simulator.debugMode)
                        System.out.print("S3: 0x" + Integer.toHexString(start) + "  ");
                    int pc = start-0x80000000; // translate by hand
                    int oldpc = pc;
                    for (i = 12; i < ((count-5)*2 + 12); i+=2) {
                        byte b = readHexByte(s, i);
                        m.writeByte(pc++, b);
                        if (Simulator.debugMode) {
                            String hh = Integer.toHexString(0xff & b);
                            if (hh.length() < 2) hh = "0" + hh;
                            System.out.print(" " + hh);                        
                        }
                        else {
                            animateLoading(pc);
                        }
                    }
                    if (Simulator.debugMode)
                        System.out.println();
                    // disassemble program
                    if (Simulator.debugMode) {
                        pc = oldpc;
                        for (i = 12; i < ((count-5)*2 + 12); i+=2) {
                            pc++;
                            if (pc % 4 == 0) {	
                                Instruction n = new Instruction(m.readInt(pc-4));	
                                n.decode();
                                System.out.print(Integer.toHexString(pc-4) + "  \t");
                                System.out.println(n.disassemble(pc -4));
                            }
                        }
                    }
                }
                else if (s.charAt(1) == '7') {
                    int count = (int)(readHexByte(s, 2));
                    int start = readHexInt(s, 4);
                    if (Simulator.debugMode)
                        System.out.println("S7: 0x" + Integer.toHexString(start));
                    p.rom.insertDefaultCode(start);
                }
                // ignore other Sx's
            }
        }
        catch (NullPointerException exception) { 
        }
        catch (EOFException exception) {
            System.out.write('\b');
        }
    }

    byte hexDigit(byte b) {
        if ((b >= nullChar) && (b < (nullChar+10))) {
                return (byte)(b - nullChar);
        }
        else if ((b >= capAChar) && (b < (capAChar+6))) {
                return (byte)(b - capAChar + 10);
        }
        else {
                return (byte)0;
        }	
    }

    byte readHexByte(String s, int i) {
        byte b0 = (byte)s.charAt(i);
        byte b1 = (byte)s.charAt(i+1);	
        return (byte)(16*hexDigit(b0) + hexDigit(b1));
    }

    int readHexInt(String s, int i) {	
        int a = (int)readHexByte(s, i);
        int b = (int)readHexByte(s, i+2);
        int c = (int)readHexByte(s, i+4);
        int d = (int)readHexByte(s, i+6);

        a = a << 24;
        b = (b << 16) & 0x00ff0000;
        c = (c << 8) & 0x0000ff00;
        d = d & 0x000000ff;
        return (a|b|c|d);
    }

    void animateLoading(int i) {
        //int N = 4*128;
        int N = 4*1024;

        /* old version animation in sun eprom style
        System.out.print("\b");
        if (i%N == 0) System.out.write('-');
        if (i%N == N/4) System.out.write('\\');
        if (i%N == N/2) System.out.write('|');
        if (i%N == (N/4)*3) System.out.write('/');
        */
        /* new version is applet compatible */
        if (i%N == 0) {
            System.out.print(".");
            System.out.flush();
        }
    }
}


class KernelLoaderException extends Exception {	
    KernelLoaderException() {		
    }
}
