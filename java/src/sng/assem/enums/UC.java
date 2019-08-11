package sng.assem.enums;

// Whether alignment in cap3 is uncomplemented or complemented.
// Don't change these symbols as they are used in the DB. 
public enum UC 
{
	U,C;
	
	public UC invert()
	{
		if (this == UC.U) return UC.C;
		else return UC.U;
	}
}
