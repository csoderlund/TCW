/*
 * THe sequence can be:
 * 	DNA (1) ESTs: name, quality, start/end coordinates 
 * 		(2) Sequence: has name, quality and translated sequence
 * 		(3) NT DB hit: name, description, either sequence or start/end coordinates
 *  AA 
 *      (1) translated UniTrans with link to DNA sequence
 *      (2) AA DB hit: name, description, either sequence or start/end coordinates
 */
package sng.dataholders;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Vector;


import sng.database.Globals;
import util.align.AAStatistics;
import util.methods.Out;

public class SequenceData implements Serializable {
	private boolean debug = false;
	public static final int smallestCodingCutoff = 100;
	public static final int bestCodingCutoff = 500;

	static final private int INVALID_SEQUENCE = 1;
	static final private int DNA_SEQUENCE = 2;
	static final private int PROTEIN_SEQUENCE = 3;
	
	public SequenceData(String type) {subType = type;}
	
	/* For debugging */
	public String getDescription() {
		String s = getName();
		if (isDNA()) s += " NT ";
		else if (nSequenceType == INVALID_SEQUENCE) s += " Invalid ";
		else {
			s += " AA ";
			if (hitData == null) s += " no hit data ";
			else s += "cstart " + hitData.getCtgStart() + " ";
		}
		s += " subtype " + subType;
		s += " NumBases " + getNumBases();
		s += " Frame " + getFrame() + " LowIndex " + getLowIndex() + " HighIndex " + getHighIndex();
		if (DBfiltered > -1) s += " Filtered " + DBfiltered;
		s += "\n " + strSequence.substring(0, 30);
		return s;
	}
	
	public boolean isDNA() { return nSequenceType == DNA_SEQUENCE; }
	public void setIsDNA() { nSequenceType = DNA_SEQUENCE;}
	
	public String getName() { return strName;}
	public void setName(String str) {strName = str;}
	
	public int getID() {return nID;}		
	public void setID(int n) {nID=n;}
	
	// for DB hit data
	public String getDBdesc() { return strDBdesc;} 
	public void setDBdesc(String str) {strDBdesc = str;}
	public String getDBspecies() { return strDBspecies;} 
	public void setDBspecies(String str) {strDBspecies = str;}
	public String getDBtype() { return strDBtype;} 
	public void setDBtype(String str) {strDBtype = str;}
	public int getDBfiltered() { return DBfiltered;} 
	public void setDBfiltered(int i) {DBfiltered = i;}
	public BlastHitData getBlastHitData() { return hitData;}
	public void setBlastHitData(BlastHitData b) { hitData = b;}
	public void setGOstuff(String g, String k, String p, String e, String i) {
		go=g; kegg=k; pfam=p; enzyme=e; interpro=i;
	}
	public String getGO() {return go;}
	public String getKEGG() {return kegg;}
	public String getPfam() {return pfam;}
	public String getEnzymeC() {return enzyme;}
	public String getInterpro() {return interpro;}
	
	public SequenceData getNucleotideSequence ( ) { return ntSeqData; }
	
	public String getSequence() {return strSequence;}
	public String getSeqRevComp() { return getSequenceReverseComplement(strSequence); }
	
	/*****************************************************
	 * Translated ORF proteins
	 **/
	public String getORFtrans(CodingRegion coding) {
		if (coding == null) return "No ORF to translate";
		int start = coding.getBegin();
		int end = coding.getEnd();
		int frame = coding.getFrame();
		
		String strSeq = strSequence.replace(".", "");
		return getTranslatedORF("", strSeq, frame, start, end);
	}
	
	public static String getORFtrans(String name, String seq, int frame, int start, int end) {
		String strSeq =  getTranslatedORF(name, seq, frame, start, end);
		if (strSeq!=null) return strSeq.replace("*", ""); // calling routine does not allow '*'
		return strSeq; 
	}
	public static String getTranslatedORF(String name, String strSeq, int frame, int start, int end) {
		if (start<=0) { 
			Out.PrtError("Incorrect start: " + name + " Frame: " + frame + " Start: " + start + " End: " + end);
			return null;
		}
		String aa="";
		String seq = strSeq;
		if (frame < 0) seq = getSequenceReverseComplement(seq);
		int cnt=0;
		char c=' ';
		for (int i = start-1; i <end && (i+2)<seq.length(); i+=3) {
			c = AAStatistics.getAminoAcidFor(
					seq.charAt(i), seq.charAt(i+1),seq.charAt(i+2));
			aa += c;
			if (c=='*') cnt++;
		}
		if (c=='*') cnt--; // stop codon
		if (cnt>0) Out.PrtErr(name + " has stop " + cnt + " codons in translation. CDS " 
								+ strSeq.length() + ";  AA " + aa.length());
		return aa;
	}

	public void setSequence(String strInSeq) {
		strSequence = strInSeq;
		nNonGapBases = 0;
		for (int i = 0; i < strSequence.length(); ++i)
			if (strSequence.charAt(i) != Globals.gapCh)
				++nNonGapBases;
	}
	public void setSequenceToLower() {
		strSequence = strSequence.toLowerCase();
	}
	
	// DNA EST or Sequence
	
	public void setQualities(Vector<Integer> inQualities, boolean bSanityCheck) {
		qualities = inQualities;
		if (bSanityCheck) sanityCheck();
	}
	public void releaseQualities() {qualities = null;}
	
	// DNA Sequence
	public String getBestMatch() {return strBestMatch;}
	public void setBestMatch(String str) {strBestMatch = str;}
	
	public String getOldContig() {return strOldContig;}
	public void setOldContig(String str) {strOldContig = str;}
	
	// ESTs (ContigData holds number buried for contig)
	public boolean isBuried() {return bBuried;} 
	public void setBuried(boolean b) {bBuried = b;} 
	public int getBuriedChildCount() {return nBuriedChildCount;}	
	public void setBuriedChildCount(int n) {nBuriedChildCount = n;}
	public String getParentName() {return strParentName;}
	public void setParentName(String str) {strParentName = str;}
	
	public void setTGaps(int[] tGaps) {this.tGaps = tGaps;}
	public int[] getTGaps() {return tGaps;}
	public int getNumTGaps() {
		if(tGaps == null) return 0; 
		return tGaps.length;
	}	
	// obsolete ?
	public void setGroup(int n) {nGroup = n;};
	public int getGroup() {return nGroup;};
	
	/**
	 * XXX AlignCompute
	 *  The results of the alignment will have leading gapCh on one sequence
	 *  to indicate its offset for drawing. This changed the objects strSequence
	 *  through insertGapAt and changes the left and right position (it ends up
	 *  the same as strDPseq without the leading/trailing gapCh
	 */
	public void buildDPalignedSeq(String strDPseq) {
		int j = 0;

		// Count the leading gaps to determine the left position
		while (j <= (strDPseq.length() - 1) && strDPseq.charAt(j) == Globals.gapCh)
			++j;
		setLeftPos(j + 1);

		int i = getLowIndex();
		while (i <= getHighIndex() && j < strDPseq.length()) {
			if (strDPseq.charAt(j) == Globals.gapCh && getBaseAt(i) != Globals.gapCh)
				insertGapAt(i);
			else if (strDPseq.charAt(j) != getBaseAt(i))
				throw new RuntimeException("Failed to match gap strings.\n" + strDPseq);

			++i;
			++j;
		}

		// Ignore the remaining gaps
		while (j <= (strDPseq.length() - 1) && strDPseq.charAt(j) == Globals.gapCh)
			++j;

		if (i <= getHighIndex())
			throw new RuntimeException(
					"Failed to consume whole non-gap string.");
	}

	/**************** Create new sequenceData *************************************/

	public SequenceData newSeqDataNoQual() {
		SequenceData seq = new SequenceData("copy NoQual");
		seq.copyNonSequenceData(this);
		seq.strSequence = strSequence;
		
		// For annoDB hits, can be nucleotide. The info 
		// is in the database, but can't get it set right in this mess
		int cnt=0, len = strSequence.length(); 
		for (int i=0; i< len && i<30; i++) {
			if (strSequence.charAt(i) == 'a') cnt++;
			else if (strSequence.charAt(i) == 'A') cnt++;
			else if (strSequence.charAt(i) == 'c')cnt++;
			else if (strSequence.charAt(i) == 'C')cnt++;
			else if (strSequence.charAt(i) == 't')cnt++;
			else if (strSequence.charAt(i) == 'T')cnt++;
			else if (strSequence.charAt(i) == 'g')cnt++;
			else if (strSequence.charAt(i) == 'G')cnt++;
		}
		if (cnt > 20) seq.setIsDNA();

		if (ntSeqData != null)
			seq.ntSeqData = ntSeqData.newSeqDataNoQual();
		
		return seq;
	}
	
	public SequenceData newSeqDataRevComp() {
		if (!isDNA()) {
			System.out.println("Sequence type is " + nSequenceType);
			throw new RuntimeException(
					"Cannot complement a sequence that is not DNA.");
		}
		SequenceData rcSeq = new SequenceData("copy RevComp");
		rcSeq.copyNonSequenceData(this);
		
		rcSeq.compAndSet(strSequence, qualities, true);
		rcSeq.setComplement(!isReverseComplement());
		return rcSeq;
	}
	
	public void copyNonSequenceData(SequenceData copyFrom) {
		strName = copyFrom.strName;
		strDBdesc = copyFrom.strDBdesc;
		strDBspecies = copyFrom.strDBspecies;
		strDBtype = copyFrom.strDBtype;
		strBestMatch = copyFrom.strBestMatch;
		strOldContig = copyFrom.strOldContig;
		bComplement = copyFrom.bComplement;
		nFrame = copyFrom.nFrame;
		nNonGapBases = copyFrom.nNonGapBases;
		nSequenceType = copyFrom.nSequenceType;
		hitData = copyFrom.hitData;
		DBfiltered = copyFrom.DBfiltered;
	}
	/**
	 * PairswiseAlignmentData: to align gapless
	 * ContigPanel: create EST objects, followed by appendToFASTAFiles to enter seq and qual
	 * Called from other places to returns sequence
	 */
	public SequenceData newSeqDataNoGap() {

		SequenceData returnSequence = new SequenceData("copy NoGap");
		returnSequence.copyNonSequenceData(this);

		// Remove all of the gaps
		Vector<Integer> newQualities = null;
		if (qualities != null) {
			newQualities = new Vector<Integer>();
			newQualities.setSize(qualities.size());
			Collections.copy(newQualities, qualities);
		}
		String newSequence = strSequence;
		String strBefore, strAfter;

		for (int i = newSequence.length() - 1; i >= 0; --i) {
			if (newSequence.charAt(i) == Globals.gapCh) {
				if (newQualities != null
						&& newQualities.get(i).intValue() != 0)
					throw new RuntimeException("Bad quality value");

				// Remove gap from sequence
				strBefore = newSequence.substring(0, i);
				if (i + 1 < newSequence.length())
					strAfter = newSequence.substring(i + 1);
				else
					strAfter = "";
				newSequence = strBefore + strAfter;

				// Remove gap from qualities
				if (newQualities != null)
					newQualities.remove(i);
			}
		}

		if (bComplement) {
			newSequence = getSequenceReverseComplement(newSequence);
			if (debug) System.out.println("newSeqDataNoGap");
			if (newQualities != null)
				Collections.reverse(newQualities);
		}

		returnSequence.setComplement(false);
		returnSequence.setSequence(newSequence);
		returnSequence.setQualities(newQualities, false);
		returnSequence.setIsDNA();
		return returnSequence;
	}
	
	/* ************** Translated Amino Acid Sequence *********************************/
	public int getFrame() {return nFrame;}
	public int getStops() { return nStops; }
	
	// PairwiseAlignment
	public SequenceData newSeqDataNTtoAA(int nFrame) {
		
		if (nFrame < 0) {
			SequenceData revSeq = newSeqDataRevComp();
			return revSeq.newSeqDataNTtoAA(-nFrame);
		}

		SequenceData aaSeqData = new SequenceData("AA");
		aaSeqData.copyNonSequenceData(this);

		// Attach the nucleotide sequence for codon comparisons and coordinate conversion
		if (isReverseComplement()) {
			aaSeqData.ntSeqData = this;
			aaSeqData.nFrame = -nFrame;
		} else {
			aaSeqData.ntSeqData = this.newSeqDataNoQual();
			aaSeqData.nFrame = nFrame;
		}

		// Convert each codon to it's amino acid
		String newSequence = "";
		String transSeq = aaSeqData.ntSeqData.strSequence;

		// Translation
		for (int i = nFrame-1; i < transSeq.length() - 3; i+=3) {
			char c = AAStatistics.getAminoAcidFor(
					transSeq.charAt(i), transSeq.charAt(i+1),transSeq.charAt(i+2));
			newSequence += c;
			if (c == '*') aaSeqData.nStops++;
		}

		// Save the AA sequence data and Sync up left position of the nucleotide sequence
		aaSeqData.ntSeqData.setLeftPos(aaSeqData.ntSeqData.nLeftPos); 
		aaSeqData.setSequence(newSequence);
		aaSeqData.nSequenceType = SequenceData.PROTEIN_SEQUENCE; 
		return aaSeqData;
	}
	
	/**
	 * Adjust the sequence based on the tGaps stored in the database
	 */
	public void setSequenceFromTGaps() {
		if (strSequence == null || tGaps == null || tGaps.length <= 0)
			return;

		int position = 0;
		int offset = 0;
		int tGapIndex = 0;

		while (tGapIndex < tGaps.length) {
			if (position >= 0 && position < tGaps[tGapIndex] + offset) {
				position = strSequence.indexOf(Globals.gapCh, position);
				if (position >= 0) position++;
				offset++;
			} else {
				strSequence = strSequence.substring(0, tGaps[tGapIndex]
						+ offset - (tGapIndex + 1))
						+ strSequence.substring(tGaps[tGapIndex] + offset
								- (tGapIndex + 1) + 1);
				this.qualities.remove(tGaps[tGapIndex] + offset
						- (tGapIndex + 1));
				tGapIndex++;
			}
		}
	}
	private void sanityCheck() {
		if (strSequence != null && qualities != null
				&& strSequence.length() != qualities.size())
			throw new RuntimeException(
					"The number of bases in the sequence and number of quality values "
							+ "for " + strName + " don't match up.\n" +
									" " + strSequence.length() + " " + qualities.size() + "\n");

		// Determine the sequence type
		nSequenceType = INVALID_SEQUENCE;
		if (!(strSequence==null || strSequence.length()==0)) {
			nSequenceType = DNA_SEQUENCE;
			for (int i = 0; i < strSequence.length(); ++i) {
				if (nSequenceType == DNA_SEQUENCE
						&& !SequenceData.isDNALetter(strSequence.charAt(i)))
					nSequenceType = PROTEIN_SEQUENCE;
				if (nSequenceType == PROTEIN_SEQUENCE
						&& !AAStatistics.isAcidLetter(strSequence.charAt(i))) {
					nSequenceType = INVALID_SEQUENCE;
					throw new RuntimeException("Invalid sequence letter of "
							+ strSequence.charAt(i));
				}
			}
		}
	}


	// EST
	public boolean hasStrandLabel() {
		return strName != null
				&& strName.length() > 0
				&& (strName.charAt(strName.length() - 1) == 'f' || strName
						.charAt(strName.length() - 1) == 'r');
	}

	// CCS
	public boolean hasNs() {
		return getNsStart() > 0;
	}

	public int getNsStart() {
		if (strSequence == null)
			return -1;
		int nEnd = strSequence.indexOf("NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN");
		if (nEnd >= 0)
			nEnd += 1;
		return nEnd;
	}

	public int getNsEnd() {
		if (strSequence == null)
			return -1;
		String strNs = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN";
		int nEnd = strSequence.lastIndexOf(strNs);
		if (nEnd > 0)
			nEnd += strNs.length();
		return nEnd;
	}

	public boolean isForwardStrand() {
		return strName != null && strName.length() > 0
				&& strName.charAt(strName.length() - 1) == 'f';
	}

	public boolean isReverseComplement() {
		return bComplement || nFrame < 0;
	}

	public void setComplement(boolean b) {
		bComplement = b;
		if (bComplement)
			nFrame = -Math.abs(nFrame);
		else
			nFrame = Math.abs(nFrame);
	}

	public void padAndSetQualities(Vector<Integer> inQualities) {
		// Insert a gap in the quality list everywhere there is one in the sequence
		qualities = padQualityList(strSequence, inQualities);
		sanityCheck();
	}
	
	public void compAndSet(String strInSeq, Vector<Integer> inQualities,
			boolean bInComplement) {
		// Save the sequence and qualities
		setSequence(strInSeq);
		qualities = inQualities;

		// If the sequence is a complement reverse it and its quality array
		if (bInComplement) {
			strSequence = getSequenceReverseComplement(strSequence);
			if (debug) System.out.println("compAndSet");

			if (qualities != null) {
				Vector<Integer> newQualities = new Vector<Integer>(qualities);
				qualities = newQualities;
				Collections.reverse(qualities);
			}
			nFrame = -1;
		} else
			nFrame = 1;

		bComplement = bInComplement;
		sanityCheck();
	}

	/**
	 * Converts the input ungapped, unaligned, nucleotide index, to be relative
	 * to the called object's sequence. Assumes that the reading frame is set
	 * correctly for amino acid sequences.
	 */
	public int convertDNAToLocalIndex(int orfCoord) {
		if (!isDNA()) {
			// Use the nucleotide sequence since it may have frame-shift corrections
			if (ntSeqData == null) return 0; 
				
			int nGapIndex = ntSeqData.convertDNAToLocalIndex(orfCoord);

			int nLostNucleotides = 0;
			if (Math.abs(nFrame) == 2) nLostNucleotides = 1;
			else if (Math.abs(nFrame) == 3) nLostNucleotides = 2;

			// Covert to the coordinates of the amino acid sequence
			return getLowIndex()
					+ (nGapIndex - ntSeqData.getLowIndex() - nLostNucleotides) / 3;
		} else {
			int seqLen = getNumNonGapBases(); // pads removed as the ORF coordinates are based on this
			int nEnd = getHighIndex(); 		
			int nStart = getLowIndex(); 

			// Sanity
			if (orfCoord < 0) {
				System.err.println("Internal Error: Invalid index of " + orfCoord
						+ ".  Valid range is [" + 1 + "," + seqLen + "] " + strName);
				return 1;
			}
			
			if (orfCoord>seqLen) orfCoord=seqLen;
			
			if (orfCoord == 0) return 1;
			// Find the ungapped index, aligned index
			int nNonGapIndex = 0;
			int nGapIndex;
			if (isReverseComplement()) {
				for (nGapIndex = nEnd; nGapIndex >= nStart; --nGapIndex) {
					if (!isGapAt(nGapIndex))
						++nNonGapIndex;
					if (nNonGapIndex >= orfCoord)
						return nGapIndex;
				}
			} else {
				for (nGapIndex = nStart; nGapIndex <= nEnd; ++nGapIndex) {
					if (!isGapAt(nGapIndex)) 
						++nNonGapIndex;
					if (nNonGapIndex >= orfCoord)
						return nGapIndex;
				}
			}
		}
		throw new RuntimeException("Error in convertDNAToLocalIndex!");
	}

	// Aligning a sequence on the graphical display
	public int getLowIndex() {
		return nLeftPos;
	}
	
	public int getHighIndex() {
		return nLeftPos + getLength() - 1;
	}
	public boolean isValidIndex(int i) {
		return getLowIndex() <= i && i <= getHighIndex();
	}

	//Now returns stored value if no sequence set 
	public int getLength() {
		if(strSequence==null || strSequence.length() == 0)
			return nSequenceLength;
		return strSequence.length();
	}	
	// Only called when EST loaded without sequence
	public void setLength( int length ) {
		nSequenceLength = length;
	}
	// ContigData and SequenceData
	public int getNumBases() {
		if (strSequence != null) return strSequence.length();
		else return 0;
	}
	public int getNumNonGapBases() {return nNonGapBases;}

	private char getBaseAt(int n) {
		if(strSequence == null || strSequence.length() <= (n + nOffsetVal))
			return '-';
		return strSequence.charAt(n + nOffsetVal);
	}
	// MainAlignPanel
	public char getOutBaseAt(int n) {
		if(strSequence == null) return '*';
		int nAdjIdx = n + nOffsetVal;
		
		if (nAdjIdx < 0 || nAdjIdx >= strSequence.length()) return ' '; // shorter seq
		return baseToOutputChar(strSequence.charAt(nAdjIdx), getQualityAt(n));
	}
	// ContigData.calcSNPState
	// ContigAlignPanel and MainPairAlignmentPanel
	public char safeGetBaseAt(int nIdx) {
		if (getLowIndex() <= nIdx && nIdx <= getHighIndex())
			return Character.toUpperCase(getBaseAt(nIdx));
		else
			return ' '; // No read at the position
	}

	private int getQualityAt(int n) {
		if (qualities == null) return 40;

		int nAdjIdx = n + nOffsetVal;
		return qualities.get(nAdjIdx).intValue();
	};
	// ContigData and ContigPanel
	// called by drawing routine (see AlignmentPanelBase)  and generateSNPData
	public boolean isLowQualityAt(int n) {
		int nQual = getQualityAt(n);
		return nQual < LOW_QUALITY_THRESHOLD && nQual != 0;
	}
	// SequenceData and ContigPanel
	public boolean isGapAt(int n) {
		return getBaseAt(n) == Globals.gapCh;
	}
	// ContigPanel
	public boolean isNAt(int n) {
		char c = getBaseAt(n);
		return c == 'N' || c == 'n';
	}
	// SequenceData, seqDetail.LoadFromDB, CAP3AceFileReader
	public void setLeftPos(int n) {

		nLeftPos = n; // this is used for aligning the output in the graphical display
		// Set the offset value used to convert the input index to 0-based
		nOffsetVal = -nLeftPos;

		// Keep the nucleotide sequence in sync
		if (ntSeqData != null)
			ntSeqData.setLeftPos(getNucleotideIndex(nLeftPos) - (Math.abs(nFrame) - 1));
	}
	public int getOffset() { return nOffsetVal;};
	
	// ContigData, SequenceData
	public int getLeftPos() {return nLeftPos;}
	// ContigData
	public void setGroupedLeftPos(int n) {nGroupedLeftPos = n;}
	public int getGroupedLeftPos() { return nGroupedLeftPos; }

	// LoadFromDB, SequenceData
	public void insertGapAt(int nPos) {
		char chBase = Globals.gapCh;

		if (isDNA() && !isDNALetter(chBase))
			throw new RuntimeException("Invalid base of " + chBase);
		else if (!isDNA() && !AAStatistics.isAcidLetter(chBase))
			throw new RuntimeException("Invalid base of " + chBase);
		boolean isGap = (chBase == Globals.gapCh);

		// Ignore insertion after the end of the sequence for gaps; for all others always append
		// the new character at the end if the index is beyond the sequence.
		if (nPos <= getHighIndex() || !isGap) {
			if (nPos < nLeftPos) {
				if (isGap)
					// Gap is before the aligned sequence, just adjust the start position
					setLeftPos(nLeftPos + 1);
				else
					throw new RuntimeException("Invalid index of " + nPos);
			} else {
				int nAdjIdx = nPos + nOffsetVal;

				// Insert the letter into the sequence where gaps are indicated:
				String strNewSeq = "";
				if (nAdjIdx > 0)
					strNewSeq += strSequence.substring(0, nAdjIdx);
				strNewSeq += chBase;
				if (nAdjIdx < strSequence.length())
					strNewSeq += strSequence.substring(nAdjIdx);
				strSequence = strNewSeq;

				// Insert a zero into the quality list for the sequence
				if (qualities != null)
					qualities.add(nAdjIdx, 0);

				if (!isGap)
					++nNonGapBases;

				// Insert gaps into the nucleotide sequence
				if (ntSeqData != null) {
					int nNukeIndex = getNucleotideIndex(nPos);
					ntSeqData.insertGapAt(nNukeIndex);
					ntSeqData.insertGapAt(nNukeIndex + 1);
					ntSeqData.insertGapAt(nNukeIndex + 2);
				}
			}
		}
	}

	private int getNucleotideIndex(int nPos) {
		if (isDNA())
			throw new RuntimeException("This method is only for amino acid sequences.");
		return (nPos - 1) * 3 + 1;
	}
	
	static private Vector<Integer> padQualityList(String strInSeq,
			Vector<Integer> inQualities) {
		
		if (inQualities.size()==1) {
			int q = inQualities.get(0);
			for (int i=1; i<strInSeq.length(); i++) {
				if (strInSeq.charAt(i) == Globals.gapCh)
					inQualities.add(i, 0);
				else
					inQualities.add(i, q);
			}
		}
		if (strInSeq.length() != inQualities.size()) {
			Vector<Integer> newQualities = new Vector<Integer>(inQualities);
			inQualities = newQualities;

			// Insert a gap in the quality list everywhere there is one in the sequence
			for (int i = 0; i < strInSeq.length(); ++i)
				if (strInSeq.charAt(i) == Globals.gapCh)
					inQualities.add(i, 0);
		}
		return inQualities;
	}

	static public String getSequenceReverseComplement(String seqIn) {
		String compSeq = "";
		for (int i = seqIn.length() - 1; i >= 0; --i) {
			compSeq += getBaseComplement(seqIn.charAt(i));
		}
		return compSeq;
	}

	static public String normalizeBases(String strInBases, char chOldGap,
			char chNewGap) {
		String strOutBases = strInBases.toUpperCase();

		if (chOldGap != chNewGap)
			return strOutBases.replace(chOldGap, chNewGap);
		else
			return strOutBases;
	}

	/**
	 * ContigData.getDescription: to pass to UniProt from graphical interface
	 * removes gaps from a sequence in order to be placed back in the DB
	 */
	public String sequenceSansGaps() {
		String str = "";
		int x;

		for (x = 0; x < strSequence.length(); x++) {
			if (strSequence.charAt(x) != Globals.gapCh)
				str += strSequence.charAt(x);
		}

		return str.trim();
	}
	
	public int getNumGapsInSequence() {
		int x, retVal = 0;

		for (x = 0; x < strSequence.length(); x++)
			if (strSequence.charAt(x) == Globals.gapCh)
				retVal++;

		return retVal;
	}
	
	/****************** FASTA files ************************************/
	// Create an FASTA file using the sequence name in the > line.
	public void appendToFASTAFiles(PrintStream seqFile, PrintStream qualFile)
			throws Exception {
		appendToFASTAFiles(strName, seqFile, qualFile);
	}
	private void appendToFASTAFiles(String strName, PrintStream seqFile,
			PrintStream qualFile) throws Exception {
		// Create base sequence object and append to FASTA
		seqFile.println(">" + strName);
		appendSequenceToFASTA(seqFile, strSequence);

		if (qualFile != null && qualities != null) {
			qualFile.println(">" + strName);
			appendQualitiesToFASTA(qualFile, qualities);
		}
	}

	static final private int FASTA_COL_WIDTH = 60;

	static private void appendSequenceToFASTA(PrintStream seqFile,
			String strSequence) {
		for (int i = 0; i < strSequence.length();) {
			// Determine where to end this line within the sequence
			int nextI = i + FASTA_COL_WIDTH;
			nextI = Math.min(nextI, strSequence.length());
			seqFile.println(strSequence.substring(i, nextI));
			i = nextI;
		}
	}

	static private void appendQualitiesToFASTA(PrintStream qualsFile,
			Vector<Integer> quals) {
		int NUMS_PER_ROW = FASTA_COL_WIDTH / 3;

		for (int i = 0; i < quals.size();) {
			for (int j = 0; j < NUMS_PER_ROW && i < quals.size(); ++j, ++i) {
				qualsFile.print(quals.get(i).intValue());
				qualsFile.print(' ');
			}
			qualsFile.println();
		}
	}
	
	/************* DNA and Amino Acid character conversion and tests *************/

	static private char getBaseComplement(char chBase) {
		switch (chBase) {
		case 'a': return 't'; case 'A': return 'T';
		case 'c': return 'g'; case 'C': return 'G';
		case 'g': return 'c'; case 'G': return 'C';
		case 't': return 'a'; case 'T': return 'A';
		case 'R': return 'Y'; case 'Y': return 'R';
		case 'M': return 'K'; case 'K': return 'M';
		case 'H': return 'D'; case 'B': return 'V';
		case 'V': return 'B'; case 'D': return 'H';
		case 'W': case 'S': case 'N': case 'n': 
		case Globals.gapCh: return chBase;
		default: 		
			return chBase;
		}
	}

	static private boolean isDNAAmbiguity(char ch) {
		switch (Character.toUpperCase(ch)) {
		// Ambiguity symbols
		case 'R': // # 3.2. Purine (adenine or guanine): R
		case 'Y': // # 3.3. Pyrimidine (thymine or cytosine): Y
		case 'W': // # 3.4. Adenine or thymine: W
		case 'S': // # 3.5. Guanine or cytosine: S
		case 'M': // # 3.6. Adenine or cytosine: M
		case 'K': // # 3.7. Guanine or thymine: K
		case 'H': // # 3.8. Adenine or thymine or cytosine: H
		case 'B': // # 3.9. Guanine or cytosine or thymine: B
		case 'V': // # 3.10. Guanine or adenine or cytosine: V
		case 'D': // # 3.11. Guanine or adenine or thymine: D
		case 'N': // # 3.12. Guanine or adenine or thymine or cytosine: N
			return true;
		default:
			return false;
		}
	}

	static private boolean isDNALetter(char ch) {
		if (isDNAAmbiguity(ch))
			return true;

		switch (Character.toUpperCase(ch)) {
		case 'C': case 'G': case 'A': case 'T': case Globals.gapCh: return true;
		default: return false;
		}
	}
	static private char baseToOutputChar(char chBase, int quality) {
		char chOut = chBase;

		if (quality < LOW_QUALITY_THRESHOLD) {
			if (Character.isLetter(chBase))
				chOut = Character.toLowerCase(chOut);
		}
		return chOut;
	}
	/*********************** Comparison Methods for Sorting ***************************/
	// ContigData
	static public int comp_Sequences_LeftPos(SequenceData a, SequenceData b) {
		if (a.getLeftPos() == b.getLeftPos())
			return a.getName().compareTo(b.getName());
		else
			return a.getLeftPos() - b.getLeftPos();
	}

	// ContigData.setSortOrder
	static public int comp_Sequences_GroupedLeftPos(SequenceData a,
			SequenceData b) {
		if (a.getGroupedLeftPos() == b.getGroupedLeftPos())
			return a.getName().compareTo(b.getName());
		else
			return a.getGroupedLeftPos() - b.getGroupedLeftPos();
	}

	static public int comp_Sequences_Name(SequenceData a, SequenceData b) {
		return a.getName().compareTo(b.getName());
	}

	public void clear() {
		if (qualities != null)
			qualities.clear();
		if (ntSeqData != null)
			ntSeqData.clear();
	}

	private static final long serialVersionUID = 1;
	
	/************** Instance variables ******************/
	// Object of type PROTEIN are the amino acid translations of a unitrans
	// they have this variable defined to point to the original nt sequence
	private SequenceData ntSeqData = null;
	
	private String strSequence = null;
	private int nSequenceType = INVALID_SEQUENCE;
	private String strName = "";
	private int nID = 0; 	
	
	// only for DB hit data
	private String strDBdesc = ""; 
	private String strDBspecies = "";
	private String strDBtype = "";
	private int DBfiltered = -1;
	private String kegg="", enzyme="", interpro="", go="", pfam="";
	private BlastHitData hitData = null;
	
	private int nNonGapBases = 0;

	private Vector<Integer> qualities = null;
	private int[] tGaps = null;			

	private String subType = "unknown";

	private int nLeftPos = 1; // The 1-based index of where this sequence aligns
								// to the reference sequence
	private int nOffsetVal = -1; // The offset added to input indexes
	private int nGroupedLeftPos = 1; // The minimum of the sequence and its
										// "mates" left position. Used for sorting
	// a list of sequences by left position, but forward/reverse
	// strands together.

	private boolean bComplement = false;
	private int nFrame = 1;					// unitrans
	private int nStops = 0;					// AA
	
	private String strParentName = "";		// EST
	private String strBestMatch = "";		// unitrans protein 

	private int nSequenceLength = 0; //Used in case sequence was not loaded
									 //(Show Buried Location) 

	private boolean bBuried = false;			// EST
	private int nBuriedChildCount = 0;		// EST

	private static final int LOW_QUALITY_THRESHOLD = 20;
	
	// obsolete i think
	private int nGroup = -1;
	private String strOldContig = "";
}
