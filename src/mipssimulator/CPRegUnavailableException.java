/*
	Copyright (c) 1996-1997 Swiss Federal Institute of Technology, 
	Computer Engineering and Networks Laboratory. All rights reserved.

	MIPS Simulator for a R3000 based machine. For details, have a look at the
	hardware documentation of the Integrated Device Technology ID79R3052E 
	processor and the evaluation board 7RS385.
	
	Permission to use, copy, modify, and distribute this software and its
	documentation for any purpose, without fee, and without written 
	agreement is hereby granted, provided that the above copyright notice 
	and the following two paragraphs appear in all copies of this software.


	
	File:                  $Source: /proj/topsy/ss98/MipsSimulator/RCS/CPRegUnavailableException.java,v $
 	Author(s):             George Fankhauser
 	Affiliation:           ETH Zuerich, TIK
 	Version:               $Revision: 1.1 $
 	Creation Date:         
 	Last Date of Change:   $Date: 1997/05/11 17:09:18 $      by: $Author: gfa $
	
	
*/
package mipssimulator;

class CPRegUnavailableException extends MemoryException {
	CPRegUnavailableException() { }
	CPRegUnavailableException(String s) { super(s); }
}
