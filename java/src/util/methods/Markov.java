package util.methods;

/**************************************************************
 * annotator/DoORF.java computes the two hash maps during annotating
 * and stores the results in the database table tuple_usage.
 */
import java.util.HashMap;

public class Markov {
	public final int fnMarkov=0, fnCodon=1; 	// used as indexes and type
	public final String BAD="***";
	/********************************************
	 * DoORF
	 */
	public Markov(HashMap <String, Double> codonLogMap, HashMap <String, Double> hexLogMap) {
		this.codonLogMap=codonLogMap;
		this.hexLogMap=hexLogMap;
	}
	
	/************************************************
	 * SeqFramePanel
	 */
	public Markov(HashMap <String, Double> tupleMap) {
		for (String tuple : tupleMap.keySet()) {
			double ll = tupleMap.get(tuple);
			if (tuple.contains("-")) hexLogMap.put(tuple, ll); // FRAME-0, etc
			else codonLogMap.put(tuple, ll);
		}
	}
	public String makeTextForSeqFrame(double fc1, double mc1, int frame, int oStart, int oEnd, String sequence) {
		String seq = sequence.toLowerCase();
		if (frame<0) seq = revComp(seq);
		String orf = seq.substring(oStart-1, oEnd);
		
		String text = "6-frame scores for ORF frame " + frame + ":\n\n";
		text += scoreSeq("Markov", fnMarkov, orf);
		text += scoreSeq("Codon", fnCodon, orf);
		
		return text;
	}
	
	public String scoreSeq(String tag, int type, String seq) {
		scoreSeqAllFrames(type, seq);
		return String.format(" %-6s %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f %s\n",
				tag, score[0], score[1], score[2],score[3], score[4], score[5], allFramesStr);
	}
	
	public boolean isGood() {return isGood;}
	public int nScore() { return nScore;}
	public double dScore() { return allFramesScore;}
	/***************************************************
	 * DoORF for writing to Frames.txt
	 * SeqFramePanel for 'Score' view
	 */
	private void scoreSeqAllFrames(int type, String seq) {
		try {
			if (type==fnMarkov) {
				int markov_order=5;
		    
			    	score[0] = markovScoreThisFrame(seq, markov_order, true);				
				score[1] = markovScoreThisFrame(seq.substring(1), markov_order, true);
				score[2] = markovScoreThisFrame(seq.substring(2), markov_order, true);
		
				String revSeq = revComp(seq);
					
				score[3] = markovScoreThisFrame(revSeq, markov_order, true);
				score[4] = markovScoreThisFrame(revSeq.substring(1), markov_order, true);
				score[5] = markovScoreThisFrame(revSeq.substring(2), markov_order, true);
			}
			else {
				score[0] = codonScoreThisFrame(seq);				
				score[1] = codonScoreThisFrame(seq.substring(1));
				score[2] = codonScoreThisFrame(seq.substring(2));
		
				String revSeq = revComp(seq);
					
				score[3] = codonScoreThisFrame(revSeq);
				score[4] = codonScoreThisFrame(revSeq.substring(1));
				score[5] = codonScoreThisFrame(revSeq.substring(2));
			}
			isGood = false;
			int b=0;
			for (int i=1; i<6; i++) if (score[i]>score[b]) b=i;
			allFramesScore=score[0];
			
			if (score[0]<0 && b!=0) { // <0 and not best
				nScore=0;
				allFramesStr="(Neg & !best)";
			}
			else if (score[0]>=0 && b!=0) { // >0 and not best
				nScore=1;
				allFramesStr="(Pos & !best)";
			}
			else if (score[0]<0 && b==0) { // <0 and best
				nScore=2;
				allFramesStr="(Neg & best)";
			}
			else if (score[0]>=0 && b==0) { // >0 and best
				nScore=3;
				allFramesStr="(Pos & best) Good score";
				isGood=true;
			}
			else {
				allFramesStr="???";
			}
		}
		catch (Exception e) {e.printStackTrace(); return;}
	}
	/***************************************************************/
	private double codonScoreThisFrame(String seq) {
		double score=0.0, ld=0.0;
		
		for (int ix=0; ix<seq.length()-3; ix+=3) {
			String codon = seq.substring(ix, ix+3);
			if (codonLogMap.containsKey(codon)) { 
				score += codonLogMap.get(codon); 
				ld+=expectedCodonLog;
			} 		
		}
		return (score-ld); 
	}
	private double markovScoreThisFrame(String seq, int markov_order, boolean stopAtStop) {
		int seq_length = seq.length();
		if (seq_length < markov_order + 1) return(0);
	    
	    double score = 0;
	    
	    for (int i = 0; i <= markov_order; i++) {
	    		int frame = i % 3;
		       
	        String kmer = seq.substring(0, i+1); 
	        String key = kmer + "-" + frame;
	        
	        double loglikelihood = (hexLogMap.containsKey(key)) ? hexLogMap.get(key) : 0;
	        score += loglikelihood;
	    }
	    markov_order++;
	    for (int i = 0; i < seq_length-markov_order; i++) {
	        int frame = i % 3;
	        int s=i+1;
	        
	        String kmer = seq.substring(s, s+markov_order); 
	       
	        if (stopAtStop) {
		        if (i == seq_length - 2 - 1  && frame == 0) {
		        		String codon = seq.substring(i, i+3);
					if (codon.equals("taa") || codon.equals("tag") || codon.equals("tga")) {
						break;
					}
		        }
	        }
	        String key = kmer + "-" + frame;    
	       
	        double loglikelihood = (hexLogMap.containsKey(key)) ? hexLogMap.get(key) : 0;
	        score += loglikelihood;
	    }  
	    return score;
	}
	
	private String revComp(String seq) { 
		StringBuilder builder = new StringBuilder ();
		for (int i = seq.length() - 1; i >= 0; --i) {
			   builder.append(getBaseComplement(seq.charAt(i)));
		   	}
		seq = builder.toString();
		return seq;
	}
	static private char getBaseComplement(char chBase) {
		switch (chBase) {
		case 'a': return 't'; case 'A': return 'T'; 
		case 't': return 'a'; case 'T': return 'A';
		case 'c': return 'g'; case 'C': return 'G';
		case 'g': return 'c'; case 'G': return 'C'; 
		case 'N': case 'n': 
		default: return chBase;
		}
	}
	private boolean isGood=false;
	private int nScore=3;
	private String allFramesStr="";
	private double allFramesScore=0;
	private double [] score = new double [6];
	private double expectedCodonLog = Math.log(1.0/64.0);
	private HashMap <String, Double>  codonLogMap =   new HashMap <String, Double> ();
	private HashMap <String, Double> hexLogMap = new HashMap <String, Double> ();
}
