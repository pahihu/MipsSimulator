/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	Written by George Fankhauser <gfa@acm.org>. For more documentation
	please visit http://www.tik.ee.ethz.ch/~gfa.

	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/RAM.java,v $
 	Author(s):             G. Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         December 1996
 	Last Date of Change:   $Date: 1997/05/09 14:33:46 $      by: $Author: gfa $
	
	
	$Log: RAM.java,v $
	Revision 1.1  1997/05/09 14:33:46  gfa
	Initial revision

# Revision 1.1  1997/02/04  10:42:01  topsy
# Initial revision
#
*/

package mipssimulator;

import mipssimulator.MemoryRegion;


public class RAM extends MemoryRegion {
	
	
	RAM(int base, int size) {
		from = base;
		to = base+size-1;
		memory = new byte[to-from+1];
	}
	
	// read/write is default behaviour
}
