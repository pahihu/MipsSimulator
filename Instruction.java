/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.

	The decoder was adapted from the Nachos machine/instruction code.

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Instruction.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
	$Log: Instruction.java,v $
	Revision 1.1  1997/05/09 14:33:46  gfa
	Initial revision

# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

import Processor;

public class Instruction {
	
	final static int CPNOMASK = 0x18000000;
	
	public final int OP_ADD = 1;
	public final int OP_ADDI = 2;
	public final int OP_ADDIU = 3;
	public final int OP_ADDU = 4;
	public final int OP_AND	= 5;
	public final int OP_ANDI = 6;
	public final int OP_BEQ = 7;
	public final int OP_BGEZ = 8;
	public final int OP_BGEZAL = 9;
	public final int OP_BGTZ = 10;
	public final int OP_BLEZ = 11;
	public final int OP_BLTZ = 12;
	public final int OP_BLTZAL = 13;
	public final int OP_BNE = 14;

	public final int OP_DIV = 16;
	public final int OP_DIVU = 17;
	public final int OP_J = 18;
	public final int OP_JAL = 19;
	public final int OP_JALR = 20;
	public final int OP_JR = 21;
	public final int OP_LB = 22;
	public final int OP_LBU = 23;
	public final int OP_LH = 24;
	public final int OP_LHU = 25;
	public final int OP_LUI = 26;
	public final int OP_LW = 27;
	public final int OP_LWL = 28;
	public final int OP_LWR = 29;

	public final int OP_MFHI = 31;
	public final int OP_MFLO = 32;

	public final int OP_MTHI = 34;
	public final int OP_MTLO = 35;
	public final int OP_MULT = 36;
	public final int OP_MULTU = 37;
	public final int OP_NOR = 38;
	public final int OP_OR = 39;
	public final int OP_ORI = 40;
	public final int OP_RFE = 41;
	public final int OP_SB = 42;
	public final int OP_SH = 43;
	public final int OP_SLL = 44;
	public final int OP_SLLV = 45;
	public final int OP_SLT = 46;
	public final int OP_SLTI = 47;
	public final int OP_SLTIU = 48;
	public final int OP_SLTU = 49;
	public final int OP_SRA = 50;
	public final int OP_SRAV = 51;
	public final int OP_SRL = 52;
	public final int OP_SRLV = 53;
	public final int OP_SUB = 54;
	public final int OP_SUBU = 55;
	public final int OP_SW = 56;
	public final int OP_SWL = 57;
	public final int OP_SWR = 58;
	public final int OP_XOR = 59;
	public final int OP_XORI = 60;
	public final int OP_SYSCALL = 61;
	public final int OP_UNIMP = 62;
	public final int OP_RES = 63;

	public final int OP_MFCP0 = 65;
	public final int OP_MTCP0 = 66;
	public final int OP_CFCP0 = 67;
	public final int OP_CTCP0 = 68;
	public final int OP_BCCP0 = 69;
	public final int OP_TLBR = 70;
	public final int OP_TLBWI = 71;
	public final int OP_TLBWR = 72;
	public final int OP_TLBP = 73;
	
	public final int OP_COP1 = 75;
	public final int OP_COP2 = 76;
	public final int OP_COP3 = 77;
	
	public final int OP_BREAK = 78;
	
	/*
	 * The table below is used to translate bits 31:26 of the instruction
	 * into a value suitable for the "opCode" field of a MemWord structure,
	 * or into a special value for further decoding.
	 */
	public final int SPECIAL  = 100;
	public final int BCOND = 101;
	public final int COP0 = 102; // system, mmu
	public final int COP1 = 103; // fpu
	public final int COP2 = 104;
	public final int COP3 = 105;

	public final int IFMT = 1;
	public final int JFMT = 2;
	public final int RFMT = 3;

	final int OPCODE = 0;
	final int FORMAT = 1;
	
	public int opTable[][] = {
		{SPECIAL, RFMT}, {BCOND, IFMT}, {OP_J, JFMT}, {OP_JAL, JFMT},
		{OP_BEQ, IFMT}, {OP_BNE, IFMT}, {OP_BLEZ, IFMT}, {OP_BGTZ, IFMT},
		{OP_ADDI, IFMT}, {OP_ADDIU, IFMT}, {OP_SLTI, IFMT}, {OP_SLTIU, IFMT},
		{OP_ANDI, IFMT}, {OP_ORI, IFMT}, {OP_XORI, IFMT}, {OP_LUI, IFMT},
		{COP0, RFMT}, {COP1, IFMT}, {COP2, IFMT}, {COP3,IFMT},
		{OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT},
		{OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT},
		{OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT},
		{OP_LB, IFMT}, {OP_LH, IFMT}, {OP_LWL, IFMT}, {OP_LW, IFMT},
		{OP_LBU, IFMT}, {OP_LHU, IFMT}, {OP_LWR, IFMT}, {OP_RES, IFMT},
		{OP_SB, IFMT}, {OP_SH, IFMT}, {OP_SWL, IFMT}, {OP_SW, IFMT},
		{OP_RES, IFMT}, {OP_RES, IFMT}, {OP_SWR, IFMT}, {OP_RES, IFMT},
		{OP_UNIMP, IFMT}, {OP_UNIMP, IFMT}, {OP_UNIMP, IFMT}, {OP_UNIMP,IFMT},
		{OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT},
		{OP_UNIMP, IFMT}, {OP_UNIMP, IFMT}, {OP_UNIMP, IFMT}, {OP_UNIMP,IFMT},
		{OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT}, {OP_RES, IFMT}
	};

	public int specialTable[] = {
		OP_SLL, OP_RES, OP_SRL, OP_SRA, OP_SLLV, OP_RES, OP_SRLV, OP_SRAV,
		OP_JR, OP_JALR, OP_RES, OP_RES, OP_SYSCALL, OP_BREAK, OP_RES, OP_RES,
		OP_MFHI, OP_MTHI, OP_MFLO, OP_MTLO, OP_RES, OP_RES, OP_RES, OP_RES,
		OP_MULT, OP_MULTU, OP_DIV, OP_DIVU, OP_RES, OP_RES, OP_RES, OP_RES,
		OP_ADD, OP_ADDU, OP_SUB, OP_SUBU, OP_AND, OP_OR, OP_XOR, OP_NOR,
		OP_RES, OP_RES, OP_SLT, OP_SLTU, OP_RES, OP_RES, OP_RES, OP_RES,
		OP_RES, OP_RES, OP_RES, OP_RES, OP_RES, OP_RES, OP_RES, OP_RES,
		OP_RES, OP_RES, OP_RES, OP_RES, OP_RES, OP_RES, OP_RES, OP_RES
	};	
	
	// the following is for disassembly only
	
	static public final String opStrings[] = {
		"Shouldn't happen", "ADD  ", "ADDI ", "ADDIU", "ADDU ", "AND  ", 
		"ANDI ", "BEQ  ", "BGEZ ", "BGEZAL", "BGTZ ", "BLEZ ", "BLTZ ", 
		"BLTZAL", "BNE  ", "Shouldn't happen", "DIV  ", "DIVU ", "J    ", 
		"JAL  ", "JALR ", "JR   ", "LB   ", "LBU  ", "LH   ", "LHU  ", 
		"LUI  ", "LW   ", "LWL  ", "LWR  ", "Shouldn't happen", "MFHI ", 
		"MFLO ", "Shouldn't happen", "MTHI ", "MTLO ", "MULT ","MULTU", 
		"NOR  ", "OR   ", "ORI  ", "RFE  ", "SB   ", "SH   ", "SLL  ", 
		"SLLV ","SLT  ", "SLTI ", "SLTIU", "SLTU ", "SRA  ", "SRAV ", 
		"SRL  ", "SRLV ", "SUB  ", "SUBU ", "SW   ", "SWL  ", "SWR  ", 
		"XOR  ", "XORI ", "SYSCALL", "Unimplemented", "Reserved", 
		"Shouldn't happen", "MFCP0", "MTCP0", "CFCP0", "CTCP0", "BCCP0",
		"TLBR ", "TLBWI", "TLBWR", "TLBP ", "Shouldn't happen", "??CP1",
		"??CP2", "??CP3", "BREAK"
    };
	
	public int value;
	public int rs, rt, rd; // source, target, destination register
	public int opCode;
	public int extra; // immediate or displacement
	
	Instruction(int instr) {
		value = instr;
	}
	
	void decode() {  
		int[] opEntry;
		
		rs = (value >> 21) & 0x1f;
		rt = (value >> 16) & 0x1f;
		rd = (value >> 11) & 0x1f;
		opEntry = opTable[(value >> 26) & 0x3f];
		opCode = opEntry[OPCODE];
		if (opEntry[FORMAT] == IFMT) { // Immediate Type
			extra = value & 0xffff;
			if ((extra & 0x8000) == 0x8000) {
			   extra |= 0xffff0000;
			}
		} 
		else if (opEntry[FORMAT] == RFMT) { // Register Type
			extra = (value >> 6) & 0x1f;
		} 
		else { // Jump Type
			extra = value & 0x03ffffff;
		}
		if (opCode == SPECIAL) {
			opCode = specialTable[value & 0x3f];
		} 
		else if (opCode == BCOND) {
			int i = value & 0x1f0000;
			if (i == 0) {	opCode = OP_BLTZ; } 
			else if (i == 0x10000) {	opCode = OP_BGEZ; } 
			else if (i == 0x100000) {	opCode = OP_BLTZAL; } 
			else if (i == 0x110000) {	opCode = OP_BGEZAL; } 
			else {	opCode = OP_UNIMP; }
		}
		else if (opCode == COP0) {
			int i = value & 0x0000001f; // function field
			if (i == 0) {
				if (rs == 0) {	opCode = OP_MFCP0; }
				else if (rs == 0x4) {	opCode = OP_MTCP0; }
			}
			else if (i == 0x1) {	opCode = OP_TLBR; }
			else if (i == 0x2) {	opCode = OP_TLBWI; }
			else if (i == 0x6) {	opCode = OP_TLBWR; }
			else if (i == 0x8) {	opCode = OP_TLBP; }
			else if (i == 0x10) {	opCode = OP_RFE; }
			else { opCode = OP_UNIMP; }
		}
		else if (opCode == COP1) {
			opCode = OP_COP1;
		}
		else if (opCode == COP2) {
			opCode = OP_COP2;
		}
		else if (opCode == COP3) {
			opCode = OP_COP3;
		}
	}
	
	int cpNo() {
		return value & CPNOMASK;
	}
	
	String disassemble() {
		String s = new String();
		
		try { s = opStrings[opCode] + "  "; }
		catch (ArrayIndexOutOfBoundsException e) { s = Integer.toString(opCode); }
		switch (opCode) {
			case OP_ADD: case OP_ADDU: case OP_AND: case OP_NOR: case OP_OR: 
			case OP_SLT: case OP_SLTU: case OP_SUB: case OP_SUBU: case OP_XOR:
			case OP_SRAV: case OP_SLLV: case OP_SRLV: 	
				if (rd == 0) 
					s = "NOP"; // writing to r0 is a NOP
				else
					s = s + "rd"+ rd + ", rs" + rs + ", rt" + rt;
				break;				
			case OP_ADDI: case OP_ADDIU: case OP_ANDI: case OP_ORI: 
			case OP_SLTI: case OP_SLTIU:case OP_XORI:
				if (rt == 0) 
					s = "NOP"; // writing to r0 is a NOP
				else
					s = s + "rt" + rt + ", rs" + rs + ", " + extra;
				break;
			case OP_SRA: case OP_SLL: case OP_SRL: 
				if (rd == 0) 
					s = "NOP"; // writing to r0 is a NOP
				else
					s = s + "rd" + rd + ", rt" + rt + ", " + extra;
				break;
			case OP_LB: case OP_LBU: case OP_LH: case OP_LHU: case OP_LW: 
			case OP_LWL: case OP_LWR: case OP_SB: case OP_SH: case OP_SW: 
			case OP_SWL: case OP_SWR:
				s = s + "rt" + rt + ", " + extra + "(r" + rs + ")";
				break;
			case OP_BGEZ: case OP_BGEZAL: case OP_BGTZ: case OP_BLEZ: 
			case OP_BLTZ: case OP_BLTZAL: 
				s = s + "rs" + rs + ", " + extra*4;
				break;
			case OP_BEQ: case OP_BNE:
				s = s + "rs" + rs + ", rt" + rt + ", " + extra*4;
				break;			
			case OP_LUI:
				s = s + "rt" + rt + ", " + extra;
				break;
			case OP_J: case OP_JAL:
				s = s + extra*4;
				break;
			case OP_JALR:
				s = s + "rd"+ rd + ", rs" + rs;
				break;
			case OP_MFHI: case OP_MFLO:
				s = s + "rd" + rd;
				break;
			case OP_JR: case OP_MTHI: case OP_MTLO:
				s = s + "rs" + rs;
				break;
			case OP_MULT: case OP_MULTU: case OP_DIV: case OP_DIVU:
				s = s + "rs"+ rs + ", rt" + rt;
				break;
			case OP_MFCP0: case OP_MTCP0:
				s = s + "rt" + rt + ", rd" + rd;
				break;
			case OP_UNIMP: case OP_RES:
				s = s + Integer.toHexString(value);
				break;
			default:
				break;
		}
		return s;
	}
}
