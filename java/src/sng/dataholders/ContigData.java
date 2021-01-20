package sng.dataholders;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Vector;

import sng.database.Globals;


/**
 * Holds all of the data for a single contig
 * USED BY BOTH ANNOTATOR AND VIEWER
 **/
public class ContigData implements Comparable<ContigData>, Serializable {
	private String HASMARK="'";
	
	public String ctgInfo() {
		return strContigID + " " + cntSwiss;
	}
	// data recorded in ContigOverviewPanel and some are used for Contig panel
	public String getContigID() {return strContigID;}
	
	public int getAllReads() { return nCount; }
	public int getNumBuried() {
		updateStats();// bad programming
		return nBuried;
	}
	public int getGCPercent() {
		if (GCratio <= 0.000001) { // set by annotator on newer databases
			if (nGCCount==0) computeGC(); 
			if (nConsensusBases==0) nConsensusBases = seqData.getLength();
			GCratio = (float) nGCCount / (float) nConsensusBases;
		}
		return (int) (GCratio * 100.0);
	}
	public double getGCRatio() {
		if (nGCCount == 0 || nConsensusBases == 0) return 0;
		return (double) nGCCount / (double) nConsensusBases;
	}
	public boolean hasSanger() {
		if (nEST5Prime > 0 || nEST3Prime > 0 || nESTMatePairs > 0) return true;
		return false;
	}
	public int getEST5Prime() {return nEST5Prime;}
	public int getEST3Prime() {return nEST3Prime;}
	public int getESTMatePairs() {return nESTMatePairs;}
	public int getESTLoners() {return nESTLoners;}
	public boolean hasNs() {return bHasNs;}
	
	public int getSNPCount() {
		if (listSNPs != null) return listSNPs.size();
		else return nSNPCount;
	}
	public String getLongestORFCoords() {
		if (longestORF==null) return "-";
		int len = seqData.getLength();
		int start = longestORF.getBegin();
		int end = longestORF.getEnd();
		int frame = longestORF.getFrame();
		boolean hasStart = longestORF.getHasBegin();
		boolean hasEnd = longestORF.getHasEnd();
		String x = "";
		if (frame < 0) {
			int xstart = len - start + 1; 
			int xend = len - end + 1;
			x = " [" + xstart + ".." + xend + "]"; 
		}
		String s="", e="";
		if (!hasStart) s=HASMARK;
		if (!hasEnd) e=HASMARK;
		
		if (start==0 && end == 0) return "---";
		return "RF" + frame + " " + start + s + ".." + end + e + x;
	}
	public String getTCWNotes() {return strTCWNotes;}
	public String getUserNotes() {return strUserNotes;}
	public int getCntPairwise() { return cntPairwise;}
	public int getCntGene() { return cntGene;}
	public int getCntSpecies() { return cntSpec;}
	public int getCntOverlap() { return cntOlap;}
	public int getCntSwiss() { return cntSwiss;}
	public int getCntTrembl() { return cntTrembl;}
	public int getCntNT() { return cntNT;}
	public int getCntGI() { return cntGI;}
	public int getCntTotal() { return cntSwiss+cntTrembl+cntNT+cntGI;}
	public int getCntDBs() { return cntDBs;}
	public int getCntAnnodb() { return cntAnnodbs;}
	public String getDBlist() { return listDBs;}
	
	public ArrayList <SequenceData> seqDataHitList() {return seqDataHitList;}
	
	public int getCntGO() { return nGO;}
	public void setCntGO(int n) {nGO=n;}; 
	
	////////////////////////////////////////////////
	public String toString() {return strContigID;}
	public void setContigID(String str) {strContigID = str;}
	public void setCTGID(int cid) {CTGID = cid;}
	
	public int getCTGID() {return CTGID;}
	
	public void setNumSequences(int n) {nCount = n;};
	public int getNumSequences() {
		// estList will not include buried if they have not been shown
	    if (estList != null) return estList.size(); 
		return nCount;
	};

	public void setSNPCount(int n) {nSNPCount = n;}	
	public int getSNPInDels() {return nSNPInDels;}
	public void setSNPInDels(int n) { nSNPInDels = n;}
	public void setEST5Prime(int n) {nEST5Prime = n;}
	public void setEST3Prime(int n) {nEST3Prime = n;}
	public void setESTMatePairs(int n) {nESTMatePairs = n;}
	public void setESTLoners(int n) {nESTLoners = n;}
	public void setGCratio(float g) {GCratio = g;}

	public void setTCWNotes(String str) {strTCWNotes = str; if (str==null) strTCWNotes="";}
	public void setUserNotes(String str) {strUserNotes = str; if (str==null) strUserNotes="";}
	// recap not used
	public boolean getRecap() {return bRecap;}
	public void setRecap(boolean b) {bRecap = b;}
	
	public void setLongest(String id) {strLongest=id;} // CAS311
	public String getLongest() {return strLongest;}
	
	public void setHasNs(boolean b) {bHasNs = b;};
	
	public BlastHitData getBestEval() { // contig.PID
		return bestEvalBlastObj;
	}
	public String getBestEvalStr() {
		if (bestEvalBlastObj==null) return "";
		int nStart = bestEvalBlastObj.getCtgStart();
		int nEnd = bestEvalBlastObj.getCtgEnd();
		int nFrame;
		if (nStart < nEnd) {
			nFrame = (nStart%3);
			if (nFrame==0) nFrame=3;
		}
		else {
			int x = (getConsensusBases()-nStart+1);
			nFrame = (x%3);
			if (nFrame==0) nFrame=3;
			nFrame = -nFrame;
		}
		return "Best Eval: " + nFrame + " " + nStart + ".." + nEnd +bestEvalBlastObj.getStrEVal();
	}
	
	public String getBestMatch() {
		if (strBestMatch == null) return "";
		else return strBestMatch;
	}
	
	public void setBestMatch(String str) { // loaded with contig
			strBestMatch = str;
	};	
	
	public BlastHitData getBestAnno() { // contig.PIDov
		return bestAnnoBlastObj;
	}
	
	// seqDetail.LoadFromDB.loadContig, pairsTable.LoadPairFromDB.loadTwoContigs
	public void setSeqDataHitList(ArrayList <SequenceData> list) {
		seqDataHitList = list;
		HashSet <String> set = new HashSet <String> (); 
		for (SequenceData seq : list) {
			BlastHitData bd = seq.getBlastHitData();
			String typetaxo = bd.getDBtypeTaxo();
			if (!set.contains(typetaxo)) set.add(typetaxo);
			
			int filter = bd.getFiltered();
			if ((filter & 16) != 0) bestEvalBlastObj = bd;
    			if ((filter & 32) != 0) bestAnnoBlastObj = bd;
		}
		cntDBs = set.size();
		listDBs = "";
		for (String db : set) listDBs += db + " "; 
	}	

	public void setGeneCntEtc(int cntG, int cntS, int cntO, int cntT) {
		cntGene = cntG; 
		cntSpec = cntS;
		cntOlap = cntO;
		cntAnnodbs = cntT;
	}
	public void setSwissTremblNTCnt(int cntS, int cntT, int cntN, int cntG) {
		cntSwiss = cntS;
		cntTrembl = cntT;
		cntNT = cntN;
		cntGI = cntG; 
	}
	public void setPairwiseCnt(int p) { cntPairwise = p;}
		
	public int getConsensusBases() {return nConsensusBases;}
	public void setConsensusBases(int n) {nConsensusBases = n;}
	
	public SNPthresholds getThresholds() {return thresholds;}

	/******** Methods for accessing the reference sequence ******************/

	public SequenceData getSeqData() {
		updateStats();
		return seqData;
	}
	
	// Contains the 'consensus' sequence from database
	public void setSeqData(SequenceData inRefSeq) {
		bStatsDirty = true;
		seqData = inRefSeq;
	}
	/**
	 * @return Total number of TGaps in a contig
	 */
	public int getNumTGaps() {
		int total = 0;
		Iterator<SequenceData> iter = estList.iterator();
		while (iter.hasNext()) {
			SequenceData temp = iter.next();
			if (!temp.isBuried())
				total += temp.getNumTGaps();
		}
		return total;
	}
	
	public int compareTo(ContigData oR) {
		return getContigID().compareTo(oR.getContigID());
	}

	static public String getCloneName(String strEST) {
		if (strEST.endsWith(".f-r"))
			return strEST.substring(0, strEST.length() - 4);
		else if (strEST.endsWith(Globals.def5p) || strEST.endsWith(Globals.def3p))
			return strEST.substring(0, strEST.length() - 2);
		else
			return strEST;
	}
	/********************************************
	 * ContigPanel for display above contig
	 */
	public Vector<String> getDescription() {
		Vector<String> lines = new Vector<String>();
		String strDesc = getContigID(); 
		if (nCount > 1) strDesc += "     " + nCount + " sequences";
		
		if (nBuried > 0) 
			strDesc += "     " + nBuried + " buried";
		if (getESTMatePairs() > 0)
			strDesc += "     " + (getESTMatePairs() ) + " mate-pairs";

		if (getNumTGaps() > 0)
			strDesc += "     (" + getNumTGaps() + " Extra bases)";
		
		strDesc += "     ";
		String notes = getTCWNotes(); 
		if (notes != null) strDesc += notes;
		lines.add(strDesc);

		return lines;
	}
	public Vector <String> getUniProtURL() {
		Vector<String> lines = new Vector<String>();
	
		lines.add("Blast sequence against UniProt.org\n"
				+ "http://www.uniprot.org/blast/?query=" + getSeqData().sequenceSansGaps());
		return lines;
	}
	/**
	 * Called by ContigPanel
	 *            (SORT_BY_GROUPED_LEFT_POS | SORT_BY_LEFT_POS | SORT_BY_NAME)
	 */
	public void setSortOrder(int n) {
		if (n == nSortOrder) return;

		// Make sure we have 1) Grouped left position 2) Min/max 3) SNP data
		updateStats();

		// Sort (this must be after setting the grouped left position)
		Comparator<SequenceData> compObj = null;
		switch (n) {
		case SORT_BY_GROUPED_LEFT_POS:
			compObj = new Comparator<SequenceData>() {
				public int compare(SequenceData o1, SequenceData o2) {
					return SequenceData.comp_Sequences_GroupedLeftPos(o1, o2);
				}
			};
			break;
		case SORT_BY_LEFT_POS:
			compObj = new Comparator<SequenceData>() {
				public int compare(SequenceData o1, SequenceData o2) {
					return SequenceData.comp_Sequences_LeftPos(o1, o2);
				}
			};
			break;
		case SORT_BY_NAME:
			compObj = new Comparator<SequenceData>() {
				public int compare(SequenceData o1, SequenceData o2) {
					return SequenceData.comp_Sequences_Name(o1, o2);
				}
			};
			break;
		default:
			throw new RuntimeException("Unrecognized sort order of " + n);
		}

		if (estList != null)
			Collections.sort(estList, compObj);
		nSortOrder = n;
	}
	
	/*********************** Methods for accessing list of EST Sequences *********/
	
	public void addSequence(SequenceData seqData) {
		bStatsDirty = true;
		groupList = null;
		nSortOrder = SORT_BY_RANDOM;

		if (estList == null)
			estList = new Vector<SequenceData>();
		if (estNameMap == null)
			estNameMap = new TreeMap<String, SequenceData>();

		estNameMap.put(seqData.getName(), seqData);
		estList.add(seqData);
	}
	
	//remove buried sequences when buried ESTs are only locations, 
	//and are about to be replaced by detailed ESTs
	public void removeBuriedSequences() {
		//if empty, do nothing
		if(estList != null && estList.size() == 0) return;
		int x=0;
		while(x<estList.size()) {
			if(estList.get(x).isBuried()) {
				estList.get(x).clear();
				estList.remove(x);
			}
			else
				x++;
		}
	}
	
	public boolean areSequencesLoaded() {
		return estList != null && estList.size() > 0;
	}
	public Vector<SequenceData> getAllSequences() {
		return estList;
	}
	public boolean haveOldContigData() {
		return estList != null && estList.size() > 0
				&& isEmpty(getSequenceAt(0).getOldContig());
	}
	private boolean isEmpty(String str) {
		 return str == null || str.length() == 0;
	}
	public int getTotalBaseWidth() {
		updateStats(); 
		return nHighIdx - nLowIdx + 1;
	};
	public int getLowIndex() {
		updateStats(); 
		return nLowIdx;
	};
	public int getHighIndex() {
		updateStats(); 
		return nHighIdx;
	};

	public void releaseSequences(boolean bReleaseConsensus) {
		updateStats(); 
		estList = null;
		estNameMap = null;
		groupList = null;
		baseInfo = null;
		if (bReleaseConsensus)
			seqData = null;
		else
			seqData.releaseQualities();
	}

	// Returns the sequence by 0-based indexing relative to the contigs sort order
	public SequenceData getSequenceAt(int i) {
		updateStats(); 
		return  estList.get(i);
	};
	public SequenceData getSequenceByName(String str) {
		return  estNameMap.get(str);
	};
	
	public SequenceData getSequenceMate(SequenceData sequence) {
		String strMateName = getMateName(sequence.getName());
		if (strMateName == null)
			return null;

		return getSequenceByName(strMateName);
	}
	private String getMateName(String strName) {
		if (strName == null || strName.length() < 3) return null;

		String strBaseName = strName.substring(0, strName.length() - 2);
		String strInEnd = strName.substring(strName.length() - 2);

		if (strInEnd.equals(Globals.def5p)) return strBaseName + Globals.def3p;
		if (strInEnd.equals(Globals.def3p)) return strBaseName + Globals.def5p;
		return null;
	}
	
	public boolean areMatesAt(int i, int j) {
		if (i < 0 || j < 0 || i >= estList.size() || j >= estList.size())
			return false;
		else
			return areMates(getSequenceAt(i), getSequenceAt(j));
	};

	public static boolean areMates(SequenceData seqA, SequenceData seqB) {
		if (seqA == null || seqB == null)
			return false;

		String nameA = seqA.getName();
		String nameB = seqB.getName();
		if (nameA.length() != nameB.length())
			return false;

		if (!(nameA.endsWith(Globals.def3p) && nameB.endsWith(Globals.def5p))
		 && !(nameA.endsWith(Globals.def5p) && nameB.endsWith(Globals.def3p)))
			return false;

		String groupA = nameA.substring(0, nameA.length() - 1);
		String groupB = nameB.substring(0, nameB.length() - 1);

		return groupA.equals(groupB);
	}

	/**
	 * Methods for accessing list of Sequences group into EST pairs
	 */
	public int getNumGroups() {
		buildGroupList();
		return groupList.size();
	}
	public char safeGetGroupBaseAt(int nCloneIdx, int nBaseIdx) {
		return safeGetGroupBaseAt(getGroupAt(nCloneIdx), nBaseIdx);
	}
	public String getGroupNameAt(int nCloneIdx) {
		return getGroupName(getGroupAt(nCloneIdx));
	}
	public int getGroupGroupAt(int nCloneIdx) {
		return getGroupGroup(getGroupAt(nCloneIdx));
	}
	private Vector<SequenceData> getGroupAt(int nCloneIdx) {
		buildGroupList();
		return  groupList.get(nCloneIdx);
	}

	public void setGroupSortOrder(int n) {
		buildGroupList();

		Comparator<Vector<SequenceData>> compObj = null;
		switch (n) {
		case SORT_BY_LEFT_POS:
			compObj = new Comparator<Vector<SequenceData>>() {
				public int compare(Vector<SequenceData> o1,
						Vector<SequenceData> o2) {
					return getGroupLeftPos(o1) - getGroupLeftPos(o2);
				}
			};
			break;
		case SORT_BY_NAME:
			compObj = new Comparator<Vector<SequenceData>>() {
				public int compare(Vector<SequenceData> o1,
						Vector<SequenceData> o2) {
					return getGroupName(o1).compareTo(getGroupName(o2));
				}
			};
			break;
		case SORT_BY_ALLELE:
			compObj = new Comparator<Vector<SequenceData>>() {
				public int compare(Vector<SequenceData> o1,
						Vector<SequenceData> o2) {
					int nRes = getGroupGroup(o1) - getGroupGroup(o2);
					if (nRes == 0)
						nRes = getGroupName(o1).compareTo(getGroupName(o2));
					return nRes;
				}
			};
			break;
		default:
			throw new RuntimeException("Unrecognized sort order of " + n);
		}

		Collections.sort(groupList, compObj);
	}

	public void sortGroupBySNPVector(Vector<SNPData> sortSNPS, int nStartSNP,
			boolean bAscending) {
		Comparator<Vector<SequenceData>> compObj = createSNPSorter(sortSNPS,
				nStartSNP, bAscending);
		Collections.sort(groupList, compObj);
	}

	private int getGroupLeftPos(Vector<SequenceData> group) {
		SequenceData seq1 = group.firstElement();
		if (group.size() == 1)
			return seq1.getLeftPos();
		SequenceData seq2 = group.lastElement();
		return Math.min(seq1.getLeftPos(), seq2.getLeftPos());
	}

	private String getGroupName(Vector<SequenceData> group) {
		SequenceData seq1 = group.firstElement();
		String str = seq1.getName();
		if (group.size() == 2) {
			str = str.substring(0, str.length() - 1);
			str += "f-r";
		}
		return str;
	}

	private int getGroupCoverage(Vector<SequenceData> group) {
		SequenceData seq1 = group.firstElement();
		int nLen1 = seq1.getNumBases();
		if (group.size() == 1)
			return nLen1;
		SequenceData seq2 = group.lastElement();
		int nTotal = seq2.getNumBases() + nLen1;

		// Remove the area of overlap if any
		int nLow = Math.max(seq1.getLowIndex(), seq2.getLowIndex());
		int nHigh = Math.min(seq1.getHighIndex(), seq2.getHighIndex());
		if (nLow <= nHigh)
			nTotal -= nHigh - nLow + 1;

		return nTotal;
	}

	private int getGroupGroup(Vector<SequenceData> group) {
		SequenceData seq1 = group.firstElement();
		return seq1.getGroup();
	}

	private void setGroupGroup(Vector<SequenceData> group, int n) {
		Iterator<SequenceData> iter = group.iterator();
		while (iter.hasNext()) {
			SequenceData seq = iter.next();
			seq.setGroup(n);
		}
	}

	private char safeGetGroupBaseAt(Vector<SequenceData> group, int n) {
		SequenceData seq1 = group.firstElement();
		char ch1 = seq1.safeGetBaseAt(n);
		if (group.size() == 1)
			return ch1;

		SequenceData seq2 = group.lastElement();
		char ch2 = seq2.safeGetBaseAt(n);

		// Use the precedence of ' ' -> gapCh -> any read letter
		// Otherwise if both ESTs aren't in agreement, return N
		if (ch1 == ' ') return ch2;
		if (ch2 == ' ') return ch1;

		if (ch1 == Globals.gapCh) return ch2;
		else if (ch2 == Globals.gapCh || ch1 == ch2) return ch1;
		else return 'N';
	}

	public Set<String> getSetOfSequenceNames() {
		if (estNameMap != null)
			return estNameMap.keySet();
		else
			return new TreeSet<String>();
	}

	public String getListOfSequenceNames(String strDelim) {
		String strOut = "";
		if (estNameMap == null)
			return strOut;

		// Iterator over the list of key values
		Iterator<String> iter = estNameMap.keySet().iterator();
		while (iter.hasNext()) {
			// Add the current name to the list
			String curName = iter.next();
			if (!(strOut == null || strOut.length() == 0))
				strOut += strDelim;
			strOut += curName;
		}

		return strOut;
	}

	private void buildGroupList() {
		if (groupList == null) {
			groupList = new Vector<Vector<SequenceData>>();

			Iterator<Map.Entry<String, SequenceData>> iter = estNameMap
					.entrySet().iterator();
			SequenceData lastSeq = null;

			while (iter.hasNext()) {
				Map.Entry<String, SequenceData> cur = iter.next();
				SequenceData curSeq = cur.getValue();

				// If the current is the mate of the last EST, add it to the
				// last group. Otherwise create a new one.
				Vector<SequenceData> groupForCur = null;
				if (areMates(lastSeq, curSeq))
					groupForCur = groupList.lastElement();
				else {
					groupForCur = new Vector<SequenceData>();
					groupList.add(groupForCur);
				}
				groupForCur.add(curSeq);

				lastSeq = curSeq;
			}
		}
	}

	// called by groupSNPs -- 
	private void buildConsensusSequence() {
		if (seqData != null || baseInfo == null)
			return;

		// Make sure the SNP data has been generated
		updateStats();

		// Use the SNP data at each position to "vote" for the base
		String consensus = "";
		for (int i = getLowIndex(), j = 0; i <= nHighIdx; ++i, ++j)
			consensus += baseInfo[j].getMajorityBase();

		seqData = new SequenceData("groupSNP");
		seqData.setSequence(consensus);
	}
	
   /*********  CODING REGION - ORF ***************************************/        
	// called by PairwiseAlignmentData DPtheAAseq
	public int getBestCodingFrame() {
		if (longestORF==null) return 0;
		return longestORF.getFrame();
	}
	
	/**
	 * Computes/returns largest ORF - called from JPAveDBWrapper to set from database
	 */
	public void setLargestCoding (CodingRegion coding) {
		longestORF = coding;
		frame = coding.getFrame();
	}
	public int getORFframe() {
		if (longestORF==null) return 0;
		return longestORF.getFrame();
	}
	public CodingRegion getORFCoding() {
		return longestORF; 
	}
	public void setFrame(int f) { frame = f;}
	public int getFrame() { return frame; }
	
	// called by ContigPanel setupCodingPanels 
	public CodingRegion getAlignedORFCoding() {
		return convertToAligned(getSeqData(), getORFCoding());
	}
	

	static public CodingRegion convertToAligned(SequenceData seqAligned,
			CodingRegion inRegion) 
	{
		if (inRegion == null) return null;

		CodingRegion outRegion = inRegion.cloneRegion();
		if (seqAligned.isReverseComplement()) {
			outRegion.setBegin(seqAligned.convertDNAToLocalIndex(inRegion.getEnd()));
			outRegion.setEnd(  seqAligned.convertDNAToLocalIndex(inRegion.getBegin()));
			outRegion.setFrame(-outRegion.getFrame());
		} else {
			outRegion.setBegin(seqAligned.convertDNAToLocalIndex(outRegion.getBegin()));
			outRegion.setEnd(  seqAligned.convertDNAToLocalIndex(outRegion.getEnd()));
		}
		return outRegion;
	}
	
	/************* Methods for accessing SNP data ***************************/
	public void setSNPs(Vector<SNPData> theSNPs) {
		if(theSNPs != null) {
			buildGroupList();

			if(listSNPs == null)
				listSNPs = new Vector<SNPData>();
			listSNPs.setSize(theSNPs.size());
			Collections.copy(listSNPs, theSNPs);
			
			SNPsGrouped = false;			
		}
	}
	// SNPtable
	public Vector<SNPData> getSNPCandidates() {
		updateStats();
		prepareSNPs();
		if(!SNPsGrouped)
			groupSNPs();

		Vector<SNPData> temp = new Vector<SNPData>();
		temp.setSize(listSNPs.size());
		Collections.copy(temp, listSNPs);
		return temp;
	}
	// ContigPanel highlightSNPS
	public boolean maybeSNPAt(int i) {
		updateStats();
		Iterator<SNPData> iter = listSNPs.iterator();
		while (iter.hasNext()) {
			SNPData snp =  iter.next();
			if (snp.getPosition() == i)
				return snp.maybeSNP();
		}
		return false;
	}
	
	private void generateSNPData() {
		baseInfo = new SNPData[getTotalBaseWidth()];
		listSNPs = new Vector<SNPData>();
		int nSequenceType = SNPData.TYPE_DNA;
		if (!getSequenceAt(0).isNT())
			nSequenceType = SNPData.TYPE_AMINO_ACIDS;

		// Pass 1: Count character frequencies at each consensus base

		for (int i = getLowIndex(), j = 0; i <= nHighIdx; ++i, ++j) {
			baseInfo[j] = new SNPData(nSequenceType);
			baseInfo[j].setPosition(i);

			// Count each letter in the current position
			Iterator<SequenceData> iter = estList.iterator();
			while (iter.hasNext()) {
				SequenceData seq = iter.next();

				if (seq.isBuried())
					continue; // skip buried clones, they slow down SNP
								// calculation

				char chBase = seq.safeGetBaseAt(i);

				if (chBase != ' ' && chBase != Globals.gapCh // gap condition
						&& !seq.isLowQualityAt(i)) // quality condition
					baseInfo[j].addToCounts(chBase);
				else
					// We're off one of the ends of the EST
					baseInfo[j].incrementMissingCount();
			}
		}

		// Pass 2: turn on the SNP flag iff
		// a) SNP redundancy >= 2
		// b) There is a perfectly matched window >= threshold

		int nHalfWindow = (thresholds.getSNPPerfectWindow() - 1) / 2;
		SNPData lastCandidate = null;

		for (int j = 0; j < baseInfo.length; ++j) {
			// Count up the current sequence of perfectly matched bases
			int nPerfectCount = 0;
			for (; j < baseInfo.length && baseInfo[j].isPerfectMatch(); ++j)
				++nPerfectCount;

			// If we made the threshold, make the current candidate a SNP and
			// start a new one
			if (j < baseInfo.length && nHalfWindow <= nPerfectCount) {
				if (lastCandidate != null
						&& lastCandidate.getSNPRedundancy(thresholds) > 0) {
					lastCandidate.setMaybeSNP(true);

					listSNPs.add(lastCandidate);
				}
				lastCandidate = baseInfo[j];
			} else
				lastCandidate = null;
		}
	}

	private void calcSNPStats() {
		nSNPInserts = 0;
		nSNPDeletes = 0;
		nSNPInDels = 0;

		for (int i = 0; i < listSNPs.size(); ++i) {
			SNPData curSNP =  listSNPs.get(i);

			nSNPDeletes += curSNP.getGapCount();

			char chRefBase = seqData.safeGetBaseAt(curSNP
					.getPosition());
			if (chRefBase == Globals.gapCh || chRefBase == ' ')
				nSNPInserts += curSNP.getNonGapCount();
		}
		
		nSNPInDels = nSNPInserts + nSNPDeletes;
	}

	private void cosegregateSNPs() {
		// Sort the SNP vectors so that the ones with the least missing data are first
		Collections.sort(listSNPs, new Comparator<SNPData>() {
			public int compare(SNPData o1, SNPData o2) {
				SNPData snp1 =  o1;
				SNPData snp2 =  o2;
				// Got null pointer on getMissingCountg, 
				// but can't reproduce, so at least checking for it
				if (snp2==null || snp1==null) {
					System.err.println("cosegratateSNPs: null pointer, return 0");
					return 0; 
				}
				return snp1.getMissingCount() - snp2.getMissingCount();
			}

			public boolean equals(Object obj) {
				return false;
			}
		});

		// Group SNPs
		int nGroup = 0;
		for (int i = 0; i < listSNPs.size(); ++i) {
			SNPData curSNP =  listSNPs.get(i);
			if (curSNP.getCoSegregationGroup() < 1) {
				// Found an ungrouped SNP, group it with all matches
				++nGroup;
				curSNP.setCoSegregationGroup(nGroup);

				Vector<SNPData> coSeqGroup = new Vector<SNPData>();
				coSeqGroup.add(curSNP);

				for (int j = i + 1; j < listSNPs.size(); ++j) {
					SNPData otherSNP = listSNPs.get(j);
					if (otherSNP.getCoSegregationGroup() >= 1)
						continue;

					// See if the current SNP matches all others in the group
					boolean bAllEqual = true;
					Iterator<SNPData> k = coSeqGroup.iterator();
					while (bAllEqual && k.hasNext()) {
						SNPData otherSNP2 = k.next();
						bAllEqual = bAllEqual
								&& sameCoSegregation(otherSNP2, otherSNP);
					}

					// If it matches all of them, add it
					if (bAllEqual) {
						coSeqGroup.add(otherSNP);
						otherSNP.setCoSegregationGroup(nGroup);
					}
				}
			}
		}

		SNPData.sortByCoSegregationGroup(listSNPs);

		// Set cosegregation scores
		int nLastGroup = -1;
		int nLastCount = 0;

		for (int i = 0; i <= listSNPs.size(); ++i) {
			SNPData curSNP = null;
			if (i < listSNPs.size())
				curSNP =  listSNPs.get(i);

			if (curSNP != null && curSNP.getCoSegregationGroup() == nLastGroup)
				++nLastCount;
			else {
				// Set the count for all members of the last group
				for (int j = i - 1; j >= 0; --j) {
					SNPData otherSNP =  listSNPs.get(j);

					// Break out of the loop if we've reached the end of the group
					if (otherSNP.getCoSegregationGroup() != nLastGroup)
						break;

					otherSNP.setCoSegregationScore(nLastCount);
				}

				// Setup for new group
				if (curSNP != null) {
					nLastGroup = curSNP.getCoSegregationGroup();
					nLastCount = 1;
				}
			}
		}

		SNPData.sortByPosition(listSNPs);

		// Find the densest SNP position
		int nBestMissing = Integer.MAX_VALUE;
		for (int i = 0; i < listSNPs.size(); ++i) {
			SNPData curSNP =  listSNPs.get(i);
			if (curSNP.getMissingCount() < nBestMissing) {
				nBestMissing = curSNP.getMissingCount();
			}
		}
	}

	private boolean sameCoSegregation(SNPData s1, SNPData s2) {
		int nPos1 = s1.getPosition();
		int nPos2 = s2.getPosition();

		int nLast = 0;

		int patt1[] = { -1, -1, -1, -1, -1, -1, -1, -1 };
		int patt2[] = { -1, -1, -1, -1, -1, -1, -1, -1 };

		for (int i = 0; i < groupList.size(); ++i) {
			// If either read has an N or has no read at the position,
			// skip it (consider n/missing wild cards)
			char chBase1 = safeGetGroupBaseAt(i, nPos1);
			char chBase2 = safeGetGroupBaseAt(i, nPos2);
			if (chBase1 == 'N' || chBase2 == 'N' || chBase1 == ' '
					|| chBase2 == ' ')
				continue;

			// See if the current bases follow the pattern
			int x1 = dnaToIndex(chBase1);
			int x2 = dnaToIndex(chBase2);
			if (patt1[x1] != patt2[x2])
				return false;

			// If the this is the first time seeing the base in
			// both vectors, save the ordering
			if (patt1[x1] < 1) {
				++nLast;
				patt1[x1] = nLast;
				patt2[x2] = nLast;
			}
		}
		// See if we actually have a pattern or just all wild card matches
		return nLast >= 2;
	}

	private void groupAlleles() {
		// Early exit if no SNPs
		if (listSNPs.isEmpty()) return;

		Vector<Vector<SequenceData>> tempList = new Vector<Vector<SequenceData>>();
		tempList.setSize(groupList.size());
		Collections.copy(tempList, groupList);

		// Sort the clones so that the ones with the least missing information
		// are first
		Collections.sort(tempList, new Comparator<Vector<SequenceData>>() {
			public int compare(Vector<SequenceData> group1,
					Vector<SequenceData> group2) {
				return getGroupCoverage(group2) - getGroupCoverage(group1);
			}

			public boolean equals(Object obj) {
				return false;
			}
		});

		// Group alles using a greedy method (this won't necessarily
		// create the best possible grouping)
		int nGroup = 0;
		for (int i = 0; i < tempList.size(); ++i) {
			Vector<SequenceData> curClone = tempList.get(i);
			if (getGroupGroup(curClone) < 1) {
				// Found an ungrouped clone create a new allele group for it
				++nGroup;
				setGroupGroup(curClone, nGroup);

				Vector<Vector<SequenceData>> alleleGroup = new Vector<Vector<SequenceData>>();
				alleleGroup.add(curClone);

				// Try to match all other ungrouped clones with it
				for (int j = i + 1; j < tempList.size(); ++j) {
					Vector<SequenceData> otherClone = tempList.get(j);
					if (getGroupGroup(otherClone) >= 1)
						continue;

					// See if the current clone matches all of the others
					// already grouped with the allele
					boolean bKeep = false;
					Iterator<Vector<SequenceData>> k = alleleGroup.iterator();
					while (k.hasNext()) {
						Vector<SequenceData> alleleClone = k.next();
						int nMatch = wildCardCompareSNPs(alleleClone,
								otherClone);
						if (nMatch == ALLELE_DISAGREE) {
							bKeep = false;
							break;
						} else if (nMatch == ALLELE_AGREE_BOTH_ALL_WILD) {
							// This is the "all wild" allele group...
							// No need to compare the others
							bKeep = true;
							break;
						} else if (nMatch == ALLELE_AGREE_HAVE_MATCHES)
							bKeep = true;
					}

					if (bKeep) {
						// Add the clone to the allele group
						setGroupGroup(otherClone, nGroup);
						alleleGroup.add(otherClone);
					}
				}
			}
		}
	}

	private final static int ALLELE_DISAGREE = -1;
	private final static int ALLELE_AGREE_HAVE_MATCHES = 1;
	private final static int ALLELE_AGREE_NO_INTERSECTION = 2;
	private final static int ALLELE_AGREE_BOTH_ALL_WILD = 3;

	private int wildCardCompareSNPs(Vector<SequenceData> clone1,
			Vector<SequenceData> clone2) {
		int nSNPs = 0;
		int nWild1 = 0;
		int nWild2 = 0;
		int nMatches = 0;

		// Compare all SNP positions
		Iterator<SNPData> iter = listSNPs.iterator();
		while (iter.hasNext()) {
			SNPData curSNP = iter.next();
			++nSNPs;

			char chBase1 = safeGetGroupBaseAt(clone1, curSNP.getPosition());
			char chBase2 = safeGetGroupBaseAt(clone2, curSNP.getPosition());

			if (!wildCardCompare(chBase1, chBase2))
				return ALLELE_DISAGREE;

			if (isWildCard(chBase1))
				++nWild1;
			if (isWildCard(chBase2))
				++nWild2;
			if (!isWildCard(chBase1) && !isWildCard(chBase2))
				++nMatches;
		}

		// One of three cases:
		if ((nSNPs == nWild1 && nSNPs == nWild2))
			return ALLELE_AGREE_BOTH_ALL_WILD; // only wild SNP positions
		else if (nMatches > 0)
			return ALLELE_AGREE_HAVE_MATCHES; // At least 1 non-wild SNP match
		else
			return ALLELE_AGREE_NO_INTERSECTION; // No intersecting, non-wild
													// SNPs
	}
	private Comparator<Vector<SequenceData>> createSNPSorter(
			Vector<SNPData> sortSNPS, int nStartSNP, boolean bAscending) {
		final int holdIndex = nStartSNP;
		final int holdPosition = (sortSNPS.get(holdIndex)).getPosition();
		final Vector<SNPData> holdSNPS = sortSNPS;
		final boolean holdAscending = bAscending;

		return new Comparator<Vector<SequenceData>>() {
			public int compare(Vector<SequenceData> o1, Vector<SequenceData> o2) {
				Vector<SequenceData> group1 = o1;
				Vector<SequenceData> group2 = o2;

				char ch1 = safeGetGroupBaseAt(group1, holdPosition);
				char ch2 = safeGetGroupBaseAt(group2, holdPosition);

				if (ch1 == ch2) {
					// Try to break the tie by checking the other SNP
					// positions round-robin
					if (listSNPs.size() > 0)
						for (int i = (holdIndex + 1) % holdSNPS.size(); i != holdIndex; i = (i + 1)
								% holdSNPS.size()) {
							int nCurPos = (holdSNPS.get(i)).getPosition();
							ch1 = safeGetGroupBaseAt(group1, nCurPos);
							ch2 = safeGetGroupBaseAt(group2, nCurPos);

							if (ch1 != ch2)
								break;
						}
				}
				if (holdAscending)
					return baseCompare(ch1, ch2);
				else
					return -baseCompare(ch1, ch2);
			}

			public boolean equals(Object obj) {
				return false;
			}
		};
	}
	
	private void groupSNPs() {
		// Note: all of the below methods in the depend on the data in
		// the baseInfo array which is filled in by "generateSNPData"
		cosegregateSNPs();
		groupAlleles();
		// Generate a consensus sequence if we don't have one
		buildConsensusSequence();

		// Note: needs baseInfo array and consensus sequence
		calcSNPStats();	
		
		//Make sure they are not grouped again
		SNPsGrouped = true;
	}
	
	public void prepareSNPs() {
		if (listSNPs == null || groupList==null) { 
			
			buildGroupList();
			generateSNPData();

			SNPsGrouped = false;
		
			nConsensusBases = seqData.newSeqDataNoGap().getNumBases();
			bHasNs = seqData.hasNs();
		}

		// Release the base info array, as it uses up a large amount of memory
		baseInfo = null;
	}

	/***** DNA routines *****/
	static private int dnaToIndex(char chBase) {
		switch (chBase) {
		case 'A': return 0;
		case 'C': return 1;
		case 'G': return 2;
		case 'T': return 3;
		case Globals.gapCh: return 4;
		case 'N': // # 3.12. Guanine or adenine or thymine or cytosine: N
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
			return 5;
		case ' ':
			return 7;
		default:
			return 6;
		}
	}

	static private boolean wildCardCompare(char chBase1, char chBase2) {
		// Consider N or ' ' equal to anything
		return (chBase1 == chBase2 || isWildCard(chBase1) || isWildCard(chBase2));
	}

	static private boolean isWildCard(char ch) {
		switch (ch) {
		case 'N': // # 3.12. Guanine or adenine or thymine or cytosine: N
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
		case ' ':
			return true;
		}
		return false;
	}

	static private int baseCompare(char chBase1, char chBase2) {
		return dnaToIndex(chBase1) - dnaToIndex(chBase2);
	}

	private void updateStats() {
		if (!bStatsDirty) return;
		
		if (seqData != null) {
			nHighIdx = Math.max(nHighIdx, seqData.getHighIndex());
			nLowIdx = Math.min(nLowIdx, seqData.getLowIndex());
		}
	
		if (estNameMap == null || estList == null) {
			bStatsDirty = false;
			return;
		}
	
		// estList contains all nonBuried ESTs (or all if 
		// the option has been selected to show buried)
		int nLongest = 0;
		int nonBuried = 0;
		
		// The 5' and 3' are set in database, but loner is 0 in database
		for (SequenceData curSeq : estList) {

			// Adjust the min/max indexes for the current sequence
			nHighIdx = Math.max(nHighIdx, curSeq.getHighIndex());
			nLowIdx = Math.min(nLowIdx, curSeq.getLowIndex());

			// See if the sequence has an EST mate is in the contig;
			// set the grouped left position appropriately
			SequenceData curMate = getSequenceMate(curSeq);
			if (curMate != null && curSeq.isBuried() == curMate.isBuried()) {
				curSeq.setGroupedLeftPos(
					Math.min(curSeq.getLeftPos(), curMate.getLeftPos()));
		
			//Check if 3' is buried, if so, sort by 5'
			} else if (curSeq.isBuried() && curMate != null){
				curSeq.setGroupedLeftPos(curMate.getLeftPos());
				++nESTLoners; 
			} else {
				curSeq.setGroupedLeftPos(curSeq.getLeftPos());
				++nESTLoners;
			}

			if (!curSeq.isBuried()) ++nonBuried;

			/* CAS311 computed in assembly
			if (curSeq.getLength() > nLongest) {
				nLongest = curSeq.getLength();
				strLongest = curSeq.getName();
			}
			*/
		}
		// if the buried have not been shown, they are not in the list
		nBuried = nCount - nonBuried;

		bStatsDirty = false;
	}
	
	private void computeGC () {
		String strSeq = seqData.getSequence();
		for (int i = 0; i < strSeq.length(); i++) {
			char c = strSeq.toUpperCase().charAt(i);
			if (c == 'G' || c == 'C')
				nGCCount++;
		}
	}

	public void clearAnno() {
		bestEvalBlastObj = null;
		bestAnnoBlastObj = null;
	}
	public void clear() {
		if (longestORF != null) longestORF.clear();
		if (listSNPs != null) listSNPs.clear();

		if (bestEvalBlastObj != null) bestEvalBlastObj.clear();
		if (bestAnnoBlastObj != null) bestAnnoBlastObj.clear();
		if (estList != null) {
			Iterator<SequenceData> iter = estList.iterator();
			while (iter.hasNext())
				iter.next().clear();
			estList.clear();
		}
		if (estNameMap != null) {
			Set<String> keyVals = estNameMap.keySet();
			Iterator<String> keyIter = keyVals.iterator();
			while (keyIter.hasNext())
				estNameMap.get(keyIter.next()).clear();
			estNameMap.clear();
		}
		if (groupList != null) {
			Iterator<Vector<SequenceData>> vectorIter = groupList.iterator();
			while (vectorIter.hasNext()) {
				Iterator<SequenceData> iter = vectorIter.next().iterator();
				while (iter.hasNext())
					iter.next().clear();
			}
			groupList.clear();
		}
	}

	final public static int SORT_BY_RANDOM = 4;
	final public static int SORT_BY_GROUPED_LEFT_POS = 2;
	final public static int SORT_BY_LEFT_POS = 1;
	final public static int SORT_BY_NAME = 0;
	final public static int SORT_BY_ALLELE = 3;

	// Contig information
	private int CTGID = -1;
	private String strContigID = "";
	private String strTCWNotes = "", strUserNotes=""; 
	private boolean bStatsDirty = false;
	private int nHighIdx = 1;
	private int nLowIdx = 1;
	private int nSortOrder = SORT_BY_RANDOM;
	
	private CodingRegion longestORF = null;
	private int frame = 0;
	
	// Stats
	private int nEST5Prime = 0;
	private int nEST3Prime = 0;
	private int nESTMatePairs = 0;
	private int nESTLoners = 0;
	private int nConsensusBases = 0;
	private boolean bHasNs = false;

	private int nGCCount = 0;
	private float GCratio = 0;
	private String strLongest = "";
	private boolean bRecap = false;
	private boolean SNPsGrouped = true;

	// SNP information
	private SNPData baseInfo[] = null;
	private Vector<SNPData> listSNPs = null;
	private int nSNPInserts = 0;
	private int nSNPDeletes = 0;
	private int nSNPInDels = 0;
	private int nSNPCount = -1; 

	// hits to external databases, used by annotator 
	private String strBestMatch = ""; // goes with bestHit1
	BlastHitData bestEvalBlastObj = null;
	BlastHitData bestAnnoBlastObj = null;
	// for viewer
	private ArrayList <SequenceData> seqDataHitList = null;
	private int cntGene=0;
	private int cntSpec=0;
	private int cntOlap=0;
	private int cntSwiss=0;
	private int cntTrembl=0;
	private int cntNT = 0;
	private int cntGI = 0;
	private int cntDBs = 0;
	private int cntAnnodbs = 0;
	private String listDBs = "";
	private int cntPairwise = 0;
	private int nGO=0; 
	private int nGOTree = 0; 
	private int nAssignGO=0; 
	private String strGO="";
	
	int nNumFramesHits = 0;
	SequenceData seqData = null;
	
	// EST information
	private int nCount = 0, nBuried = 0;
	private Vector<SequenceData> estList = null;
	private TreeMap<String, SequenceData> estNameMap = new TreeMap<String, SequenceData>();
	private Vector<Vector<SequenceData>> groupList = null;

	// Thresholds
	SNPthresholds thresholds = new SNPthresholds();
	private static final long serialVersionUID = 1;
}
