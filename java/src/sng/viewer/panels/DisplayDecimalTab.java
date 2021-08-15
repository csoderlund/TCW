package sng.viewer.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import sng.database.Globals;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.DisplayFloat;
import util.ui.UserPrompt;

public class DisplayDecimalTab  extends Tab {
	private static final long serialVersionUID = -6819426351584345568L;
	
	/************************************************
	 * The parameters changed in this class are static. The rounding parameters are in DisplayFloat
	 */
	/**************************************************************
	 * CAS322 Highlight pvalue; 323 changed 1,2,3 ; CAS324 make sure all save preference, add Set Default 
	 */
	static final int NCUTS=4, NSCH=4;
	// brown, pink, red
	static private final Color [] colSch1 = {new Color(215, 192, 177),new Color(191, 154, 130),Color.pink,new Color(205, 92, 92)};
	// light blue to purple
	static private final Color [] colSch2 = {new Color(220, 230, 250),new Color(185, 191, 255),new Color(216, 150,255),new Color(174, 96, 184)};
	// blues 
	static private final Color [] colSch3 = {new Color(196, 216, 243), new Color(171, 196, 245),new Color(140, 165, 247),new Color(109, 127, 242)};
	// yellow, green, brown (cannot see highlight cell in yellow)
	static private final Color [] colSch4 = {new Color(248, 255, 168),new Color(199, 255, 164),new Color(250, 216, 137),new Color(219, 161, 99)};
	
	// setDefaults and setPrefStringPvalCut set value
	static private boolean bHighPval=true;
	static private String colScheme="1";
	static private Color [] selC=colSch1;
	static private double [] pCuts = {0.05, 0.01, 0.001, 0.0001};
	
	/********************************************
	 * Called from SeqDetail for hit table and other values
	 */
	static DisplayFloat disfl = new DisplayFloat();
	static public String formatDouble(double val) {
    	return disfl.getString(val);
	}
	
	/******** Preferences ************************/
	static public String getPrefIDPvalCut() {return "pvalCutoff";}
	public static String getPrefStringPvalCut(){
		int b = (bHighPval) ? 1 : 0;
		return b + "," + colScheme + "," + pCuts[0] + "," + pCuts[1] + "," + pCuts[2] + "," + pCuts[3];
	} 
	static public void setPrefStringPvalCut(String str) {
		if (str.trim().length()==0) return;
		String[] arr = str.trim().split(",");
		int n = arr.length;
		if (n<5) return;
		try {
			if (n>0) bHighPval = (Integer.parseInt(arr[0])==1) ? true : false;
			if (n>1) colScheme = arr[1];
			if (n>2) pCuts[0]   = Double.parseDouble(arr[2]);
			if (n>3) pCuts[1]   = Double.parseDouble(arr[3]);
			if (n>4) pCuts[2]  =  Double.parseDouble(arr[4]);
			if (n>5) pCuts[3] =   Double.parseDouble(arr[5]);
			setColScheme(colScheme);
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Invalid pvalue highlight preferences:" + str);
		}
	}
	/*** Set static values from interface and prefs **/
	private static void setColScheme(String n) {
		try {
			int x = Integer.parseInt(n);
			if (x>0 && x<=NCUTS)colScheme=n;
		}
		catch (Exception e) {}
		
		if (n.contentEquals("1")) 		selC=colSch1;
		else if (n.contentEquals("2")) 	selC=colSch2;
		else if (n.contentEquals("3")) 	selC=colSch3;
		else if (n.contentEquals("4")) 	selC=colSch4;
	}
    
    /************************************************
     * CAS322 called by highlighting
     */
	public static boolean isHighPval() { return bHighPval;}
    public static Color getPvalColor(double pval) {
    	if (!bHighPval) return null;
    	double theVal = Math.abs(pval);
    	if (theVal==Globalx.dNoDE) return null; // CAS330
    	if (theVal<pCuts[3]) 	return selC[3];
    	if (theVal<pCuts[2]) 	return selC[2];
		if (theVal<pCuts[1]) 	return selC[1];
		if (theVal<pCuts[0])	return selC[0];
		return null;
    }
    /****************************************************************/
	/**********************************************************
	 * XXX          DisplayDecimalTab class                XXX
	 */
	private final String HTML = Globals.helpDir + "DisplayDecimal.html";
	
	public DisplayDecimalTab(STCWFrame sf)
	{
		super(sf, null);
		setBackground(Color.WHITE);
		setAlignmentX(Component.CENTER_ALIGNMENT);
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
		setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		
		frame = sf;
		
		JPanel page = Static.createPagePanel();
		page.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel helpRow = Static.createRowPanel();
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), "Decimal Display", HTML);
			}
		});
		helpRow.add(Box.createHorizontalGlue());
		helpRow.add(btnHelp);
		page.add(helpRow);
		page.add(Box.createVerticalStrut(10));
		
		JPanel titleRow = Static.createRowPanel();
		JLabel lblTitle = new JLabel("Decimal Display Options");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		titleRow.add(lblTitle);
		page.add(titleRow);
		page.add(Box.createVerticalStrut(5));
		
		JPanel textRow = Static.createRowPanel();
		JLabel txtDesc = new JLabel("Set how decimal numbers are displayed for all tables");
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		textRow.add(txtDesc);
		page.add(textRow);
		page.add(Box.createVerticalStrut(5));
		
		JPanel textRow2 = Static.createRowPanel();
		JLabel txtDesc2 = new JLabel("See Help section on Round-off!!");
		txtDesc2.setFont(new Font(f.getName(), Font.ITALIC, f.getSize()));
		txtDesc2.setAlignmentX(Component.LEFT_ALIGNMENT);
		textRow2.add(txtDesc2);
		page.add(textRow2);
		page.add(Box.createVerticalStrut(30));
		page.add(Box.createVerticalGlue());
		page.setMaximumSize(page.getPreferredSize());
		
		// Round
		page.add(createRound());
		
		// Colors
		JPanel cPanel = createColor(); // CAS327
		if (sf.getMetaData().hasDE()) // CAS323
			page.add(cPanel);
		
		// Default
		JPanel defRow = Static.createRowPanel();
		JButton setDefs = Static.createButton("Set Defaults", true);
		setDefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDefaults();
			}
		});
		defRow.add(setDefs);
		page.add(Box.createVerticalStrut(20));
		page.add(defRow);
		page.add(Box.createVerticalStrut(20));
		page.add(Box.createVerticalGlue());
		page.setMaximumSize(page.getPreferredSize());
		
		add(page);
		add(Box.createVerticalGlue());
	}
	/*****************************************************************/
	JPanel createRound() {
		JPanel page = Static.createPagePanel();
		
		JPanel placeRow = Static.createRowPanel();
   		placeRow.add(Box.createHorizontalStrut(7));
		
   		placeRow.add(new JLabel("E-notation: "));
		fldNumE = Static.createTextField(String.valueOf(DisplayFloat.Num_E),2, true);
		fldNumE.getDocument().addDocumentListener(roundTextListener);
		placeRow.add(fldNumE);		placeRow.add(Box.createHorizontalStrut(5));
		
   		placeRow.add(new JLabel("Significant digits (<1): "));
		fldNumSigs = Static.createTextField(String.valueOf(DisplayFloat.Num_sig),2, true);
		fldNumSigs.getDocument().addDocumentListener(roundTextListener);
		placeRow.add(fldNumSigs);		placeRow.add(Box.createHorizontalStrut(5));
		
		placeRow.add(new JLabel("Decimal places (>1): "));
		fldNumDec =  Static.createTextField(String.valueOf(DisplayFloat.Num_dec),2, true); 
		fldNumDec.getDocument().addDocumentListener(roundTextListener);
		placeRow.add(fldNumDec);		placeRow.add(Box.createHorizontalGlue());
		
		page.add(placeRow);
		page.add(Box.createVerticalStrut(10));
	
	// E-notation
		JPanel eRow = Static.createRowPanel();
		eRow.add(Box.createHorizontalStrut(5));
		chkUseFormat = new JRadioButton(); chkUseFormat.setBackground(Color.white); 
		chkUseFormat.addActionListener(roundActionListener);
		eRow.add(chkUseFormat);				eRow.add(Box.createHorizontalStrut(10));
		
		eRow.add(new JLabel("E-notation if absolute value > "));
		eRow.add(Box.createHorizontalStrut(1));
		fldLarge = Static.createTextField(String.valueOf(DisplayFloat.Largest),7, true); 
		fldLarge.getDocument().addDocumentListener(roundTextListener);
		eRow.add(fldLarge);					eRow.add(Box.createHorizontalStrut(3));
		
		eRow.add(new JLabel(" or <  "));	eRow.add(Box.createHorizontalStrut(1));
		fldSmall = Static.createTextField(String.valueOf(DisplayFloat.Smallest),7, true);
		fldSmall.getDocument().addDocumentListener(roundTextListener);
		eRow.add(fldSmall);
		eRow.add(Box.createHorizontalGlue());
		
		page.add(eRow);
		page.add(Box.createVerticalStrut(10));
	
	// Java
		JPanel javaRow = Static.createRowPanel();
		javaRow.add(Box.createHorizontalStrut(5));
		chkUseJava = new JRadioButton(); chkUseJava.setBackground(Color.white); 
		chkUseJava.addActionListener(roundActionListener);
		javaRow.add(chkUseJava);			javaRow.add(Box.createHorizontalStrut(5));
		javaRow.add(new JLabel("Java defaults for when to display as E-notation"));
		javaRow.add(Box.createHorizontalGlue());
		page.add(javaRow);
		
		page.add(Box.createVerticalStrut(30));
		page.add(Box.createVerticalGlue());
		page.setMaximumSize(page.getPreferredSize());
		    
		ButtonGroup bg = new ButtonGroup();
		bg.add(chkUseFormat);
		bg.add(chkUseJava);
		
		if (DisplayFloat.Use_mode == DisplayFloat.USE_FORMAT) 
			 chkUseFormat.setSelected(true);
		else chkUseJava.setSelected(true);
	
		return page;
	}
	/****************************************************************/
	JPanel createColor() {
		JPanel page = Static.createPagePanel();
	
	// Highlight
		JPanel highRow = Static.createRowPanel();
		highRow.add(Box.createHorizontalStrut(5));
		String msg="Highlight good p-values ";
		chkHighPval = Static.createCheckBox(msg, bHighPval); 
		chkHighPval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateChkHigh(chkHighPval.isSelected());
			}
		});
		highRow.add(chkHighPval);	highRow.add(Box.createHorizontalStrut(5));
		page.add(highRow);
		page.add(Box.createVerticalStrut(15));
	
	// Color schemes
		JPanel colRow = Static.createRowPanel();
		colRow.add(Box.createHorizontalStrut(20));
		colRow.add(new JLabel("Color Scheme [1-" + NSCH + "]: ")); colRow.add(Box.createHorizontalStrut(5));
		txtColSch = Static.createTextField(colScheme, 2);
		txtColSch.getDocument().addDocumentListener(colorTextListener); // Save
		colRow.add(txtColSch); colRow.add(Box.createHorizontalStrut(3));
		page.add(colRow);
		page.add(Box.createVerticalStrut(10));
		
	    page.add(createColorRow("1.", colSch1)); page.add(Box.createVerticalStrut(10));
	    page.add(createColorRow("2.", colSch2)); page.add(Box.createVerticalStrut(10));
	    page.add(createColorRow("3.", colSch3)); page.add(Box.createVerticalStrut(10));
	    page.add(createColorRow("4.", colSch4)); page.add(Box.createVerticalStrut(10));
	    page.add(Box.createVerticalStrut(15));
		
	 // Pvals
 		JPanel pvalRow = Static.createRowPanel();
 		pvalRow.add(Box.createHorizontalStrut(20));
 		pvalRow.add(new JLabel("Cutoffs: ")); pvalRow.add(Box.createHorizontalStrut(5));
 		for (int i=0; i<NCUTS; i++) {
 			pvalRow.add(new JLabel(p[i]));	pvalRow.add(Box.createHorizontalStrut(1));
 			
 			txtPval[i] = Static.createTextField(pCuts[i]+"", 5);
 			txtPval[i].getDocument().addDocumentListener(pvalTextListener); 
 			
 			pvalRow.add(txtPval[i]); 		        pvalRow.add(Box.createHorizontalStrut(1));
 			if (i<3) pvalRow.add(new JLabel(">"));	pvalRow.add(Box.createHorizontalStrut(5));
 		}
 		btnApply = Static.createButton("Apply Cutoffs", false);
 		btnApply.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				updatePvalsApply();											// Save
 				btnApply.setEnabled(false);
 			}
 		});
 		pvalRow.add(btnApply);
 		page.add(pvalRow);
	   
	    page.add(Box.createVerticalGlue());
	    page.setMaximumSize(page.getPreferredSize());
		return page;
	}
	/****************************************************
	 * Rounding
	 */
	private ActionListener roundActionListener = new ActionListener() 
	{
        public void actionPerformed(ActionEvent ae) {updateRoundingTextFields();}        
    };
    private DocumentListener roundTextListener = new DocumentListener() 
    {
    	public void changedUpdate(DocumentEvent e) {updateRoundingTextFields();}
    	public void removeUpdate(DocumentEvent e) {updateRoundingTextFields();}
    	public void insertUpdate(DocumentEvent e) {updateRoundingTextFields();}
    };  
    private void updateRoundingTextFields() {
	    try{
	    	int eFigs = Integer.parseInt(fldNumE.getText());
			if (eFigs < 1) {
				eFigs=DisplayFloat.Num_E;
				fldNumE.setText(Integer.toString(eFigs));
			}
			int sigFigs = Integer.parseInt(fldNumSigs.getText());
			if (sigFigs < 1) {
				sigFigs=DisplayFloat.Num_sig;
				fldNumSigs.setText(Integer.toString(sigFigs));
			}
			int dec = Integer.parseInt(fldNumDec.getText());
			if (dec < 1) {
				dec=DisplayFloat.Num_dec;
				fldNumDec.setText(Integer.toString(dec));
			}
			double sm = Double.parseDouble(fldSmall.getText());
			if (sm<0) {
				sm=DisplayFloat.Smallest;
				fldSmall.setText(Double.toString(sm));
			}
			double lg = Double.parseDouble(fldLarge.getText());
			if (lg<0) {
				lg=DisplayFloat.Largest;
				fldLarge.setText(Double.toString(lg));
			}
			int mode = 1;
			if (chkUseJava.isSelected()) mode = 2;
		    
			DisplayFloat.prefFlushRounding(mode, eFigs, sigFigs, dec, lg, sm, frame.getPreferencesRoot());
	    }
	    catch(Exception e) { ; // in the middle of editing
	    }
    }
    /*************************************************************************/
    /************************************************************************
	 * Color scheme and pvalue cutoffs
	 */
	private JPanel createColorRow(String msg, Color [] c1) {
		 JPanel highRow = Static.createRowPanel();
		 highRow.add(Box.createHorizontalStrut(20));
		 highRow.add(new JLabel(msg)); 
		 highRow.add(Box.createHorizontalStrut(5));
	     for (int i=0; i<NCUTS; i++) {
	    	highRow.add(Box.createHorizontalStrut(5));
	    	highRow.add(Static.createLabel("        " + p[i] + "       ", c1[i])); 	
	     }
	     highRow.add(Box.createHorizontalStrut(5));
	     return highRow;
	}
	private void updateChkHigh(boolean b) {
		 bHighPval=b;
		 prefFlushColor();							     
	}
	// Change Color scheme
	private DocumentListener colorTextListener = new DocumentListener() {
    	public void changedUpdate(DocumentEvent e) {updateColScheme();}
    	public void removeUpdate(DocumentEvent e) {}
    	public void insertUpdate(DocumentEvent e) {updateColScheme();}
	}; 
	private void updateColScheme() {
		boolean b=true;
		try {
			String n = txtColSch.getText().trim();
			int x = Integer.parseInt(n);
			if (x>0 && x<=NSCH) {
				colScheme=n;
				setColScheme(n);
				prefFlushColor();
			}
			else {b=false;}
		}
		catch (Exception ex) {b=false;}
		if (!b) {
			UserPrompt.showError ("Value must be integer between 1-" + NSCH); 
		}
	}
	private DocumentListener pvalTextListener = new DocumentListener() {
    	public void changedUpdate(DocumentEvent e) {btnApply.setEnabled(true);}
    	public void removeUpdate(DocumentEvent e) {}
    	public void insertUpdate(DocumentEvent e) {btnApply.setEnabled(true);}
	}; 
	// Change pvalues
	private void updatePvalsApply() {
		try {
			double [] cuts = new double [NCUTS];
			boolean b=true;
			String msg="";
			
			try {cuts[0] = Double.parseDouble(txtPval[0].getText());} 
			catch (Exception e) {b=false; msg="A is not a decimal number";}
			
			try {cuts[1] = Double.parseDouble(txtPval[1].getText());} 
			catch (Exception e) {b=false; msg="B is not a decimal number";}
			
			try {cuts[2] = Double.parseDouble(txtPval[2].getText());} 
			catch (Exception e) {b=false; msg="C is not a decimal number";}
			
			try {cuts[3] = Double.parseDouble(txtPval[3].getText());} 
			catch (Exception e) {b=false; msg="D is not a decimal number";}
			
			if (!b) {
				UserPrompt.showError (msg); 
				return;
			}
			if (cuts[0]!=0.0 && cuts[0]<cuts[1]){b=false; msg="A is not zero nor > B";}
			else if (cuts[1]!=0.0 && cuts[1]<cuts[2]){b=false; msg="B is not zero nor > C";}
			else if (cuts[0]!=0.0 && cuts[0]<cuts[1]){b=false; msg="C is not zero nor > D";}
			if (!b) {
				UserPrompt.showError (msg); 
				return;
			}
			
			for (int i=0; i<NCUTS; i++ ) pCuts[i] = cuts[i];
			prefFlushColor();
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Saving p-values");
		}
	}
	// prefRound
	private void prefFlushColor() {
		try {
			Preferences prefs = frame.getPreferencesRoot();
			if (prefs!=null) {
				prefs.put(getPrefIDPvalCut(), getPrefStringPvalCut());
				prefs.flush();
			}
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Internal error: setting color preferences");
		}
	}
	/*******************************************************
	 * set rounding defaults and save preferences
	 */
	private void setDefaults() {
		double Largest=DisplayFloat.dLargest, Smallest=DisplayFloat.dSmallest; 
		int Num_sig = DisplayFloat.dNum_sig, Num_dec = DisplayFloat.dNum_dec, Num_E = DisplayFloat.dNum_E;
		int Use_mode = DisplayFloat.dUse_mode;
		
		DisplayFloat.prefFlushRounding(Use_mode, Num_E, Num_sig, Num_dec, Largest, Smallest, frame.getPreferencesRoot());
		
		chkUseFormat.setSelected(Use_mode==1);
		fldNumE.setText(Num_E+"");
		fldNumSigs.setText(Num_sig+"");
		fldNumDec.setText(Num_dec+"");
		fldLarge.setText(Largest+"");
		fldSmall.setText(Smallest+"");
		
		// set color defaults and save preferences
		bHighPval=true;
		colScheme="1";
		selC=colSch1;
		pCuts[0] = 	0.05;
		pCuts[1] =	0.01;
		pCuts[2] =	0.001;
		pCuts[3] =	0.0001;
	
		chkHighPval.setSelected(true);
		txtColSch.setText("1");
		for (int i=0; i< NCUTS; i++)
			txtPval[i].setText(String.valueOf(pCuts[i]));
		btnApply.setEnabled(false);
		
		prefFlushColor();	
	}
	private STCWFrame frame = null;
	private JTextField fldNumSigs=null, fldNumDec=null, fldNumE=null;
	private JTextField fldLarge=null, fldSmall=null;
	private JRadioButton chkUseJava=null, chkUseFormat=null;
	
	private JCheckBox chkHighPval = null;
	private JTextField txtColSch;
	private JButton btnApply=null;
	private JTextField [] txtPval = new JTextField [NCUTS];
	private String [] p = {"A ", "B ", "C ", "D "};
}
