#
#	Simulator Makefile
#
#	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
#	Computer Engineering and Networks Laboratory. All rights reserved.
#
#	8.1.97	gfa		created
#
#	
#	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/Makefile,v $
# 	Author(s):             G. Fankhauser
# 	Affiliation:           ETH Zuerich, TIK
# 	Version:               $Revision: 1.3 $
# 	Creation Date:         December 1996
# 	Last Date of Change:   $Date: 1997/05/28 12:28:21 $      by: $Author: gfa $
#	
#	
#	$Log: Makefile,v $
#	Revision 1.3  1997/05/28 12:28:21  gfa
#	added FPGA dummy device
#
# Revision 1.2  1997/05/11  17:46:28  gfa
# added CPRegUnavailableException source file
#
# Revision 1.1  1997/05/09  14:36:45  gfa
# Initial revision
#
# Revision 1.3  1997/03/18  21:23:53  gfa
# turned on java optimizer
#
# Revision 1.2  97/02/04  10:58:43  conrad
# Adding of a SOURCE variable to make Makefile compatible with RCS
# 
# Revision 1.1  1997/02/04  10:42:24  topsy
# Initial revision
#

SOURCES = AddressErrorLoadException.java AddressErrorStoreException.java \
	BreakpointException.java BusErrorException.java Clock.java \
	ClockErrorException.java CoProc0.java CPRegUnavailableException.java \
	Debugger.java Instruction.java FPGA_NetworkDevice.java \
	KernelLoader.java Memory.java MemoryException.java MemoryRegion.java \
	Processor.java RAM.java ROM.java ReservedInstructionException.java \
	Simulator.java TLBEntry.java TLBLoadMissException.java \
	TLBModifiedException.java TLBStoreMissException.java UART.java \
	UTLBMissException.java

all: ${SOURCES} 
	javac -O ${SOURCES}

clean:
	$(RM) -f *~ *.bak *.class
