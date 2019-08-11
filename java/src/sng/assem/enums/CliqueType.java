package sng.assem.enums;

// single = unpaired EST
// paired = mated-paired EST
// self =  self overlap of paired EST (clique has exactly 2 EST in it, mates of each other)
// Don't change these symbols as they are used in the DB. 
public enum CliqueType {
	Single, Paired, Self
}
