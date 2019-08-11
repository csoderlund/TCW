package cmp.align;
/**********************************************
 * Brings up a dialog to show the PairAlignData alignment
 * This is all copied from jpave.viewer.panels.MainToolAlignPanel
 * 
 * FIXME: after chopping, make coordinates start-end at chop coords (instead of 1)
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import cmp.align.ScoreNT;
import util.methods.Out;
import util.methods.Static;
import util.ui.UIHelpers;

public class PairAlignText extends JDialog {
	private static final long serialVersionUID = 6152973237315914324L;
	private static final int width = 75;
	
	private static final int Align_ok = 1;
	public static final int  Align_cancel= 2;
	
	/******************************************************
	 * Create popup with options
	 */
    	public PairAlignText(boolean isCDS, boolean isNT, boolean isUTR) {
    		this.isNT = isNT;
    		this.isCDS = isCDS;
    		this.isUTR = isUTR;
    		
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		String         msg = "AA";
    		if (isCDS)     msg="CDS";
    		else if (isNT) msg="NT"; // NT and UTR
    		setTitle("Align " + msg + "...");
	
    		JPanel selectPanel = Static.createPagePanel();
    		
    		JPanel row = Static.createRowPanel();
    		chkRemoveHang = Static.createCheckBox("Remove hanging sequence from ends");
    		chkRemoveHang.setSelected(true);
    		row.add(chkRemoveHang);
    		selectPanel.add(row);
    		selectPanel.add(Box.createVerticalStrut(3));
    		
	    	selectPanel.add(new JSeparator());
	    	
	    selectPanel.add(createDisplay());
	    selectPanel.add(Box.createVerticalStrut(5));
	    if (isCDS) selectPanel.add(createCDS());
	    else if (isNT && !isUTR) selectPanel.add(createNT());
	 	selectPanel.add(new JSeparator());
	 	
        JPanel mainPanel = Static.createPagePanel();
        	mainPanel.add(selectPanel);
        	mainPanel.add(Box.createVerticalStrut(15));
        	mainPanel.add(createButtons());
        		
        	mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        	add(mainPanel);
        		
        	pack();
        	this.setResizable(false);
        	UIHelpers.centerScreen(this);
    	}
    	/******************************************************/
    	private JPanel createDisplay() {
    		JPanel row = Static.createRowPanel();
    		JLabel label = new JLabel("Display:");
	    	row.add(label);
	    	row.add(Box.createHorizontalStrut(width-label.getPreferredSize().width));
	    	
	    	JRadioButton len60 =  Static.createRadioButton("60", true);
    		len60.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        lineLen=60;
		    }
    		});
	    row.add(len60); 
	    row.add(Box.createHorizontalStrut(5));
	 	
	    JRadioButton len90 =  Static.createRadioButton("90", false);
		len90.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        lineLen=90;
		    }
		});
		row.add(len90); row.add(Box.createHorizontalStrut(5));
	    
	    ButtonGroup grp1 = new ButtonGroup();
		grp1.add(len60); grp1.add(len90);
		
		if (!isNT && !isCDS) { // amino acid gets 20 so can compare with 60 nt
		 	JRadioButton len20 =  Static.createRadioButton("20", false);
			len20.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent arg0) {
			        lineLen=20;
			    }
			});
			row.add(len20); row.add(Box.createHorizontalStrut(5));
			grp1.add(len20);
		}
    		return row;
    	}
    	/*************************************************************/
    	private JPanel createCDS() {
    		JPanel page = Static.createPagePanel();
    		JPanel row = Static.createRowPanel();
    		JLabel la = new JLabel("Annotate:");
    		row.add(la); 
    	 	row.add(Box.createHorizontalStrut(width-la.getPreferredSize().width));
    		
    		JRadioButton match =  Static.createRadioButton("Match", true);
    		match.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_MATCH;
		    }
    		});
	    row.add(match); row.add(Box.createHorizontalStrut(5));
	
    		JRadioButton aa =  Static.createRadioButton("Amino Acid", false);
    		aa.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_AA;
		    }
    		});
	    row.add(aa); row.add(Box.createHorizontalStrut(5));
	    page.add(row);
	    page.add(Box.createVerticalStrut(5));
	    
	    row = Static.createRowPanel();
	    JLabel lb = new JLabel("");
		row.add(lb); 
	 	row.add(Box.createHorizontalStrut(width-lb.getPreferredSize().width));
	 	
	    JRadioButton nd =  Static.createRadioButton("Degenerate", false);
    		nd.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_ND;
		    }
    		});
	    row.add(nd); row.add(Box.createHorizontalStrut(5));
	    
	    JRadioButton tv =  Static.createRadioButton("ts/tv", false);
    		tv.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_TV;
		    }
    		});
	    row.add(tv); row.add(Box.createHorizontalStrut(5));
    
	    JRadioButton cpg =  Static.createRadioButton("CpG", false);
		cpg.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent arg0) {
	        cdsMode = Share.CDS_CpG;
	    }
		});
		row.add(cpg); row.add(Box.createHorizontalStrut(5));
		page.add(row);
		
	    ButtonGroup grp2 = new ButtonGroup();
		grp2.add(match); grp2.add(aa); grp2.add(nd);  grp2.add(tv); grp2.add(cpg);
			
    		return page;
    	}
    	/*************************************************************/
    	private JPanel createNT() {
    		JPanel page = Static.createPagePanel();
    		
    		JPanel row = Static.createRowPanel();
    		
		JLabel la = new JLabel("Annotate:");
		row.add(la); 
		row.add(Box.createHorizontalStrut(width-la.getPreferredSize().width));
		
		JRadioButton match =  Static.createRadioButton("Match", true);
		match.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_MATCH;
		    }
		});
		row.add(match); row.add(Box.createHorizontalStrut(5));
    
	    JRadioButton tv =  Static.createRadioButton("ts/tv", false);
		tv.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_TV;
		    }
		});
	    row.add(tv); row.add(Box.createHorizontalStrut(5));

	    JRadioButton cpg =  Static.createRadioButton("CpG", false);
		cpg.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
		        cdsMode = Share.CDS_CpG;
		    }
		});
		row.add(cpg); row.add(Box.createHorizontalStrut(5));

		ButtonGroup grp3 = new ButtonGroup();
		grp3.add(match); grp3.add(cpg);  grp3.add(tv);
		page.add(row);
		
    		return page;
    	}
    	/****************************************************/
    	private JPanel createButtons() {
    	    JPanel buttonPanel = Static.createRowPanel();
    		JButton btnOK = new JButton("OK");
    		btnOK.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				setVisible(false);
    			}
    		});
    		JButton btnCancel = new JButton("Cancel");
    		btnCancel.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				nMode = Align_cancel;
    				setVisible(false);
    			}
    		});
    		
    		btnOK.setPreferredSize(btnCancel.getPreferredSize());
    		btnOK.setMaximumSize(btnCancel.getPreferredSize());
    		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    	
    	    	buttonPanel.add(btnOK);
    	    	buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(btnCancel);
        buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
    		return buttonPanel;
    	}
    /*******************************************************
     * XXX Display alignment
     */
    	/*****************************************************************/
    	public Vector <String> alignPopupCDS(PairAlignData aDataObj) {
    		isNT=true;
    		origAlignObj = aDataObj;
    		setHang();
    		return new viewByCodon().getText();
    	}
    /**************************************************************/
    	public Vector <String> alignPopup(PairAlignData aDataObj) {
    		this.origAlignObj = aDataObj;
		setHang();
		return new viewByChar().getText();
    	}
    public int getSelection() { return nMode; }
    	
    /**************************************************************
     * Utilities
     * **************************************************************/
    	private void setFormat(String name1, String name2) {
    		// create info for left label
    		int max = Math.max(name1.length(), name2.length());
    		int max2 = Math.max(nEndHang1, nEndHang2);
    		int y=5;
    		if (max2<999) y=3;
    		else if (max2<9999) y=4;
    		format =  "%" + max + "s " + "%" + y + "d  ";
    		format1 = "%" + max + "s " + "%" + y + "s  ";
    	}
    	/**************************************************************
    	 * This is used by all NT, AA, CDS, UTR
    	 */
    	private void getHangMethod() {
    		int crop = origAlignObj.getAlignCropSeq1().length(); 
    		int full = origAlignObj.getAlignFullSeq1().length();
    		int hang = full-crop;
    		
    		String cropStr,fullStr,hangStr;
    		if (isCDS) {
    			cropStr = String.format("%6s (%4s)", Out.df(crop), Out.df(crop/3)); 
    			fullStr = String.format("%6s (%4s)", Out.df(full), Out.df(full/3)); 
    			int aa = (hang>3) ? hang/3 : 0;
    			hangStr =  String.format("%6s (%4s)", Out.df(hang), Out.df(aa)); 
    		}
    		else {
    			cropStr = String.format("%6s", Out.df(crop)); 
    			fullStr = String.format("%6s", Out.df(full)); 
    			hangStr =  String.format("%6s", Out.df(hang)); 
    		}
    		hangCol[2] =  "Hang: " + hangStr;
    		if (chkRemoveHang.isSelected()) {
    			hangCol[0]="CROP: " + cropStr;
    			hangCol[1]="Full: " + fullStr;
    		}
    		else {
    			hangCol[0]="FULL: " + fullStr;
    			hangCol[1]="Crop: " + cropStr;
    		}
     }
    	/*****************************************************************/
    	private class viewByCodon {
    		private Vector <String> getText() {
        		// create info for left label
        		String name1 = origAlignObj.getSeqID1();
        		String name2 = origAlignObj.getSeqID2();
        		setFormat(name1, name2);
        		
        		Vector <String> lines = new Vector <String> ();
        		getHangMethod();
        		
        		String crop1 = origAlignObj.getAlignCropSeq1();
        		String crop2 = origAlignObj.getAlignCropSeq2();
        		
        		scoreCDSobj.addHeader(cdsMode, crop1, crop2, lines, hangCol); // computes all counts 
        		lines.add("");
        		
        		// alignment
        		int x,k;
        		StringBuffer sb = new StringBuffer (lineLen);
        		// seqOff1 is relative to ungapped sequence.
        		int seqOff1=nStartHang1, seqOff2=nStartHang2, nEndAlign=alignSeq1.length(); 
        		     		
	    		for (int offset=0; offset<nEndAlign; offset+=lineLen) {
	    			// 1st of pair
	    			if (seqOff1 <= -lineLen) 
	    				 sb.append(String.format(format1, name1, "-")); 
	    			else sb.append(String.format(format, name1, seqOff1));
	    			
	    			for (x=k=0; x<lineLen && (x+offset)<nEndAlign; x++, k++) {
	    				char c = alignSeq1.charAt(x+offset);
	    				sb.append(c);
	    				if (c!=Share.gapCh)  seqOff1++;
	    				else if (seqOff1<=0) seqOff1++; 
	    				if (k==2) {
	    					sb.append(" ");
	    					k=-1;
	    				}
	    			}
	    			if (x<lineLen) 
	    				for (k=0; x<lineLen;x++, k++) {
	    					sb.append(" ");
	    					if (k==2) {
		    					sb.append(" ");
		    					k=-1;
		    				}
	    				}
	    			sb.append(String.format("   %4d %4d", seqOff1-1, offset+x));
	    			
	    			lines.add(sb.toString());
	    			sb.delete(0, sb.length());
	    			
	    			// match line - computes here
	    			sb.append(String.format(format1, "",""));
	    			for (x=0; x<lineLen && (x+offset)<=nEndAlign-3; x+=3) {
	    				int y = x+offset;
	    				String codon1 = alignSeq1.substring(y, y+3);
	    				String codon2 = alignSeq2.substring(y, y+3);
	    				String match =  scoreCDSobj.codonMatch(cdsMode, codon1, codon2)+ " ";
	    				sb.append(match);
	    			}
	    			lines.add(sb.toString());
	    			sb.delete(0, sb.length());
	    			
	    			// 2nd of pair
	    			if (seqOff2 <= -lineLen) 
	    				 sb.append(String.format(format1, name2, "-")); 
	    			else sb.append(String.format(format, name2, seqOff2));
	    			
	    			for (x=k=0; x<lineLen && (x+offset)<nEndAlign; x++, k++) {
	    				char c = alignSeq2.charAt(x+offset);
	    				sb.append(c);
	    				if (c!=Share.gapCh)  seqOff2++;
	    				else if (seqOff2<=0) seqOff2++; 
	    				if (k==2) {
	    					sb.append(" ");
	    					k=-1;
	    				}
	    			}
	    			if (x<lineLen) 
	    				for (k=0; x<lineLen;x++, k++) {
	    					sb.append(" ");
	    					if (k==2) {
		    					sb.append(" ");
		    					k=-1;
		    				}
	    				}
	    			sb.append(String.format("   %4d", seqOff2-1));
	    			
	    			lines.add(sb.toString());
	    			sb.delete(0, sb.length());
	    			
	    			lines.add("-------");
	    		}
	    		scoreCDSobj.legendCDS(cdsMode, lines);
        		return lines;
    		}
    	}
    	/*****************************************************************/
    	private class viewByChar {
    		private Vector <String> getText() {
    			String name1 = origAlignObj.getSeqID1();
    			String name2 = origAlignObj.getSeqID2();
    			setFormat(name1, name2);
    			
    			// header and computes alignMatch 
    			Vector <String> lines = new Vector <String> ();
    			String alignMatch="";
    			if (isNT) alignMatch = addHeaderNT(lines);   
    			else  alignMatch = addHeaderAA(lines); 
    			alignMatch = addPadding(alignMatch);
    			
    			// alignment
    			int x;
    			StringBuffer sb = new StringBuffer (lineLen);
    			
    			int [] orfs = origAlignObj.getORFs();
        		int sOrf1=orfs[0], eOrf1=orfs[1]-2, sOrf2=orfs[2], eOrf2=orfs[3]-2;
        		
        		// seqOff is relative to ungapped sequence; used for coordinate
    			int seqOff1=nStartHang1, seqOff2=nStartHang2, nEndAlign=alignSeq1.length(); 
    			
    			if (isUTR) {
    				sOrf1=sOrf2= -100000000;
    				eOrf1=eOrf2=  100000000;
    			}
    			
    			for (int offset=0; offset<nEndAlign; offset+=lineLen) { 
    				// 1st of pair
	    			if (seqOff1 <= -lineLen) sb.append(String.format(format1, name1, "-"));  
	    			else sb.append(String.format(format, name1, seqOff1));
	    			
    				for (x=0; x<lineLen && (x+offset)<nEndAlign; x++) {
    					char c = alignSeq1.charAt(x+offset);
    					if (c!=Share.gapCh) {
    						if (seqOff1<sOrf1 || seqOff1>=eOrf1) c = Character.toUpperCase(c);
    						seqOff1++;
    					} else if (seqOff1<=0) seqOff1++; // for overhang
    					
    					sb.append(c);
    				}
    				if (x<lineLen) for (;x<lineLen;x++) sb.append(" ");
    		
    				sb.append(String.format("   %4d %4d", seqOff1-1, offset+x));
    				lines.add(sb.toString());
    				sb.delete(0, sb.length());
    				
    				// match
    				sb.append(String.format(format1, "",""));
    				for (int i=0; i<lineLen && (i+offset)<nEndAlign; i++) {
    					sb.append(alignMatch.charAt(i+offset));
    				}
    				lines.add(sb.toString());
    				sb.delete(0, sb.length());
    				
    				// seq2
    				if (seqOff2 <= -lineLen) sb.append(String.format(format1, name2, "-")); 
	    			else sb.append(String.format(format, name2, seqOff2));
    				
    				for (x=0; x<lineLen && (x+offset)<nEndAlign; x++) {
    					char c = alignSeq2.charAt(x+offset);
    					if (c!=Share.gapCh) {
    						if (seqOff2<sOrf2 || seqOff2>=eOrf2) c = Character.toUpperCase(c);
    						seqOff2++;
    					} 
    					else if (seqOff2<=0) seqOff2++; // for overhang
    					
    					sb.append(c);
    				}
    				if (x<lineLen) {
    					for (;x<lineLen;x++) sb.append(" ");
    				}
    				sb.append(String.format("   %4d", seqOff2-1));
    				lines.add(sb.toString());
    				sb.delete(0, sb.length());
    				
    				lines.add("------");
    
	    			nStartHang1+=lineLen;
	    			nStartHang2+=lineLen;
    			}
    			if (isUTR) {
    				lines.add("LEGEND: ");
    				lines.add("        " + Share.NT_MM + " = NT mis-match");
    			}
    			else if (isNT) scoreNTobj.legendNT(cdsMode,lines);
    			else scoreAAobj.legendAA(lines);
    			return lines;
    		}
    		private String addPadding(String alignMatch) {
    			if (!chkRemoveHang.isSelected()) {
    				String front="", end="";
    				for (int i=0; i<alignSeq1.length(); i++) {
    					char c1 = alignSeq1.charAt(i);
    					char c2 = alignSeq2.charAt(i);
    					if (c1==Share.gapCh || c2==Share.gapCh) front += " ";
    					else break;
    				}
    				for (int i=alignSeq1.length()-1; i>0; i--) {
    					char c1 = alignSeq1.charAt(i);
    					char c2 = alignSeq2.charAt(i);
    					if (c1==Share.gapCh || c2==Share.gapCh) end += " ";
    					else break;
    				}
    				alignMatch = front + alignMatch + end;	
    			}
    			return alignMatch;
    		}
    		
    	 	private String addHeaderAA(Vector <String> lines) {
        		getHangMethod();
        		
        		String crop1 = origAlignObj.getAlignCropSeq1();
        		String crop2 = origAlignObj.getAlignCropSeq2();
        		
        		String matchLine = scoreAAobj.aaMatch(crop1, crop2); 
        		scoreAAobj.addHeader(lines, hangCol);
        		return matchLine;
        	}
    		
        	private String addHeaderNT(Vector <String> lines) {
        		getHangMethod();
        		
        		String crop1 = origAlignObj.getAlignCropSeq1();
        		String crop2 = origAlignObj.getAlignCropSeq2();
        		
        		String matchLine = scoreNTobj.score(cdsMode, crop1, crop2);
        		scoreNTobj.addHeader(lines, hangCol);
        		return matchLine;
        	}
    	}
    
    	/**************************************************************
    	 * Get alignment - either already aligned or align with new gap/extend
    	 */
   
	private void setHang() {
		if (chkRemoveHang.isSelected()) {
			alignSeq1 = origAlignObj.getAlignCropSeq1();
			alignSeq2 = origAlignObj.getAlignCropSeq2();
		}
		else {
			alignSeq1 = origAlignObj.getAlignFullSeq1();
			alignSeq2 = origAlignObj.getAlignFullSeq2();
		}
		int [] hangEnds = origAlignObj.getHangEnds();
		
		nStartHang1=hangEnds[0];  
		nStartHang2=hangEnds[2]; 
		nEndHang1 = nEndHang2 = 1000000; // ends are not used for chop, but need to have a value
		
    		if (!chkRemoveHang.isSelected()) {// one will start with '1'
    			int offset=1;
    			if (nStartHang1!=1 && nStartHang2==1) offset=nStartHang1;
    			if (nStartHang1==1 && nStartHang2!=1) offset=nStartHang2;
    			
    			nStartHang1 = (nStartHang1-offset)+1;
    			nStartHang2 = (nStartHang2-offset)+1;
    			
    			nEndHang1=  hangEnds[1]; // TODO not working
    			nEndHang2=  hangEnds[3];	
    		}
	 }
    	/************************************************************/
    	// interface
    private JCheckBox	chkRemoveHang = null;
    private int lineLen = 60;
      	
    private int nMode =   Align_ok; // alignment parameter
	private int cdsMode = Share.CDS_MATCH;  // CDS display && NT display

    	// alignPopup
    private	PairAlignData origAlignObj;
    	private String format, format1;
    	
    	private boolean isNT, isCDS, isUTR;
  
    	// set in setAlign; alignSeq can be AA, NT or CDS -- already aligned when passed in
    	private String [] hangCol = new String [3];
    	private String alignSeq1, alignSeq2; // hang/no hang depends on chkRemoveHang.isSelected()
	private int nStartHang1, nEndHang1, nStartHang2, nEndHang2;
	private ScoreCDS scoreCDSobj = new ScoreCDS();
	private ScoreAA scoreAAobj = new ScoreAA();
	private ScoreNT scoreNTobj = new ScoreNT();
}

