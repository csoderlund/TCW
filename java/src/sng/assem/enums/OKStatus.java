package sng.assem.enums;


// result of OkCtg test on cap3 assembly
public enum OKStatus {
	OK, TooManyCtg, TooManySing, UnMatchedRF,PairSameOrient,TooFewBridge,Redundant,
	MixedSingleBridge,TooManyNonBridgeMates,MixedUCRatio,RFImbalance,TooManyUncompRF,
	MixedStrictMerge,CapFailure,Chimeric,UnpairedMultCtg,Hang
}
