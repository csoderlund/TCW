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
	 * CAS322 Highlight pvalue; 323 changed 1,2,3 
	 */
	static final int NCUTS=4, NSCH=4;
	// light blue to purple
	static private final Color [] colSch1 = {new Color(220, 230, 250),new Color(185, 191, 255),new Color(216, 150,255),new Color(174, 96, 184)};
	// yellow, green, brown
	static private final Color [] colSch2 = {new Color(248, 255, 168),new Color(199, 255, 164),new Color(250, 216, 137),new Color(219, 161, 99)};
	// blues 
	static private final Color [] colSch3 = {new Color(196, 216, 243), new Color(171, 196, 245),new Color(140, 165, 247),new Color(109, 127, 242)};
	// brown, pink, red
	static private final Color [] colSch4 = {new Color(215, 192, 177),new Color(191, 154, 130),Color.pink,new Color(205, 92, 92)};
	
	static private boolean bHighPval=true;
	static private String colScheme="1";
	static private Color [] selC=colSch1;
	static private double [] pCuts = {0.05, 0.01, 0.001, 0.0001};
	
	/******** Preferences ************************/
	static public String getPvalCutPrefID() {return "pvalCutoff";}
	public static String getPvalCutPrefString(){
		int b = (bHighPval) ? 1 : 0;
		return b + "," + colScheme + "," + pCuts[0] + "," + pCuts[1] + "," + pCuts[2] + "," + pCuts[3];
	} 
	static public void setPvalCutPrefs(String str) {
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
	/*** Set static values from interface **/
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
    private static void setPcuts(double [] cuts) {
    	for (int i=0; i<NCUTS; i++ ) 
    		pCuts[i] = cuts[i];
    }
    static private void setHighPval(boolean b) {bHighPval=b;}
    /************************************************
     * CAS322 called by highlighting
     */
	public static boolean isHighPval() { return bHighPval;}
    public static Color getPvalColor(double pval) {
    	if (!bHighPval) return null;
    	double theVal = Math.abs(pval);
    	if (theVal<pCuts[3]) 		return selC[3];
    	else if (theVal<pCuts[2]) 	return selC[2];
		else if (theVal<pCuts[1]) 	return selC[1];
		else if (theVal<pCuts[0])	return selC[0];
		else return null;
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
		JLabel txtDesc2 = new JLabel("See Help section on Round-off Errors!");
		txtDesc2.setFont(new Font(f.getName(), Font.ITALIC, f.getSize()));
		txtDesc2.setAlignmentX(Component.LEFT_ALIGNMENT);
		textRow2.add(txtDesc2);
		page.add(textRow2);
		page.add(Box.createVerticalStrut(30));
		page.add(Box.createVerticalGlue());
		page.setMaximumSize(page.getPreferredSize());
		
		page.add(createRound());
		
		if (sf.getMetaData().hasDE()) // CAS323
			page.add(createColor());
		
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
		fldNumDec =  Static.createTextField(String.valueOf(DisplayFloat.Num_dec),2, true); 
		fldNumSigs = Static.createTextField(String.valueOf(DisplayFloat.Num_sig),2, true);
		
		fldNumDec.getDocument().addDocumentListener(roundTextListener);
		fldNumSigs.getDocument().addDocumentListener(roundTextListener);
		
		placeRow.add(new JLabel("Significant figures: "));
		placeRow.add(fldNumSigs);		placeRow.add(Box.createHorizontalStrut(5));
		placeRow.add(new JLabel("Decimal places: "));
		placeRow.add(fldNumDec);			placeRow.add(Box.createHorizontalGlue());
		
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
		
		JPanel highRow = Static.createRowPanel();
		highRow.add(Box.createHorizontalStrut(5));
		String msg="Highlight good p-values ";
		chkHighPval = Static.createCheckBox(msg, bHighPval); 
		chkHighPval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setHighPval(chkHighPval.isSelected());
			}
		});
		highRow.add(chkHighPval);	highRow.add(Box.createHorizontalStrut(5));
		page.add(highRow);
		page.add(Box.createVerticalStrut(15));
		
		JPanel pvalRow = Static.createRowPanel();
		pvalRow.add(Box.createHorizontalStrut(20));
		pvalRow.add(new JLabel("Cutoffs: ")); pvalRow.add(Box.createHorizontalStrut(5));
		for (int i=0; i<NCUTS; i++) {
			pvalRow.add(new JLabel(p[i]));	pvalRow.add(Box.createHorizontalStrut(1));
			
			txtPval[i] = Static.createTextField(pCuts[i]+"", 5);
			
			pvalRow.add(txtPval[i]); 		        pvalRow.add(Box.createHorizontalStrut(1));
			if (i<3) pvalRow.add(new JLabel(">"));	pvalRow.add(Box.createHorizontalStrut(5));
		}
		JButton apply = Static.createButton("Apply Cutoffs", true);
		apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				apply();
			}
		});
		pvalRow.add(apply);
		page.add(pvalRow);
		page.add(Box.createVerticalStrut(15));
		
		JPanel colRow = Static.createRowPanel();
		colRow.add(Box.createHorizontalStrut(20));
		colRow.add(new JLabel("Color Scheme [1-" + NSCH + "]: ")); colRow.add(Box.createHorizontalStrut(5));
		txtColSch = Static.createTextField(colScheme, 2);
		txtColSch.getDocument().addDocumentListener(colorTextListener);
		colRow.add(txtColSch); colRow.add(Box.createHorizontalStrut(3));
		page.add(colRow);
		page.add(Box.createVerticalStrut(10));
		
	    page.add(createHigh("1.", colSch1)); page.add(Box.createVerticalStrut(10));
	    page.add(createHigh("2.", colSch2)); page.add(Box.createVerticalStrut(10));
	    page.add(createHigh("3.", colSch3)); page.add(Box.createVerticalStrut(10));
	    page.add(createHigh("4.", colSch4)); page.add(Box.createVerticalStrut(10));
	   
	    page.add(Box.createVerticalGlue());
	    page.setMaximumSize(page.getPreferredSize());
		return page;
	}
	ActionListener roundActionListener = new ActionListener() 
	{
        public void actionPerformed(ActionEvent ae) {updateRoundingTextFields();}        
    };
    DocumentListener roundTextListener = new DocumentListener() 
    {
    	public void changedUpdate(DocumentEvent e) {updateRoundingTextFields();}
    	public void removeUpdate(DocumentEvent e) {updateRoundingTextFields();}
    	public void insertUpdate(DocumentEvent e) {updateRoundingTextFields();}
    };  
    private void updateRoundingTextFields() {
	    try{
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
		    
			DisplayFloat.setRoundingFields(mode, sigFigs, dec, lg, sm, frame.getPreferencesRoot());
	    }
	    catch(Exception e) { ; // in the middle of editing
	    }
    }
    /*************************************************************************/
    /************************************************************************
	 * Color scheme and pvalue cutoffs
	 */
	private JPanel createHigh(String msg, Color [] c1) {
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
	// Change Color scheme
	private DocumentListener colorTextListener = new DocumentListener() {
    	public void changedUpdate(DocumentEvent e) {chgColScheme();}
    	public void removeUpdate(DocumentEvent e) {}
    	public void insertUpdate(DocumentEvent e) {chgColScheme();}
	}; 
	private void chgColScheme() {
		boolean b=true;
		try {
			String n = txtColSch.getText().trim();
			int x = Integer.parseInt(n);
			if (x>0 && x<=NSCH) {
				colScheme=n;
				setColScheme(n);
				flushColorPrefs();
			}
			else {b=false;}
		}
		catch (Exception ex) {b=false;}
		if (!b) {
			UserPrompt.showError ("Value must be integer between 1-" + NSCH); 
		}
	}
	// Change pvalues
	private void apply() {
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
			
			setPcuts(cuts);
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Saving p-values");
		}
	}
	
	private void flushColorPrefs() {
		try {
			Preferences prefs = frame.getPreferencesRoot();
			if (prefs!=null) {
				prefs.put(getPvalCutPrefID(), getPvalCutPrefString());
				prefs.flush();
			}
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Internal error: setting color preferences");
		}
	}
	private STCWFrame frame = null;
	private JTextField fldNumSigs=null, fldNumDec=null;
	private JTextField fldLarge=null, fldSmall=null;
	private JRadioButton chkUseJava=null, chkUseFormat=null;
	
	private JCheckBox chkHighPval = null;
	private JTextField txtColSch;
	private JTextField [] txtPval = new JTextField [NCUTS];
	private String [] p = {"A ", "B ", "C ", "D "};
}
