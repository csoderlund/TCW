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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import sng.database.Globals;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.DisplayFloat;
import util.ui.UserPrompt;

public class DecimalNumbersTab  extends Tab {
	private static final long serialVersionUID = -6819426351584345568L;
	// the preferences for rounding is set in FieldContigData; its read here
	private String roundPrefID() {return "rounding";	}
	private final String HTML = "html/viewSingleTCW/DecimalNumbersTab.html";
	public DecimalNumbersTab(STCWFrame sf)
	{
		super(sf, null);
		super.setBackground(Color.WHITE);
		super.setAlignmentX(Component.LEFT_ALIGNMENT);
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
		setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		
		frame = sf;
		
		JPanel topRow = Static.createRowPanel();
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), 
						"Decimal Numbers", HTML);
			}
		});
		topRow.add(Box.createHorizontalGlue());
		topRow.add(btnHelp);
		add(topRow);
		add(Box.createVerticalStrut(20));
		
		JLabel lblTitle = new JLabel("Decimal Numbers");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		JTextArea txtDesc = new JTextArea(
				"Set how decimal numbers are displayed for all tables.");
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);
		
		add(lblTitle);
		add(txtDesc);
		add(Box.createVerticalStrut(15));
		
		JPanel page = Static.createPagePanel();
   		JPanel textRow = Static.createRowPanel();

		fldNumDec = Static.createTextField(String.valueOf(DisplayFloat.Num_dec),2, true); 
		fldNumSigs = Static.createTextField(String.valueOf(DisplayFloat.Num_sig),2, true);
		
		fldNumSigs.getDocument().addDocumentListener(roundTextListener);
		fldNumDec.getDocument().addDocumentListener(roundTextListener);
		
		textRow.add(new JLabel("Significant figures: "));
		textRow.add(fldNumSigs);
		textRow.add(Box.createHorizontalStrut(5));
		textRow.add(new JLabel("Decimal places: "));
		textRow.add(fldNumDec);
		textRow.add(Box.createHorizontalGlue());
		
		page.add(textRow);
		page.add(Box.createVerticalStrut(15));
		
		
		JPanel row1 = Static.createRowPanel();
		chkUseFormat = new JRadioButton(); chkUseFormat.setBackground(Color.white); 
		chkUseFormat.addActionListener(roundActionListener);
		row1.add(chkUseFormat);
		row1.add(Box.createHorizontalStrut(10));
		
		row1.add(new JLabel("E-notation if absolute value > "));
		row1.add(Box.createHorizontalStrut(1));
		fldLarge = Static.createTextField(String.valueOf(DisplayFloat.Largest),7, true); 
		fldLarge.getDocument().addDocumentListener(roundTextListener);
		row1.add(fldLarge);
		row1.add(Box.createHorizontalStrut(3));
		row1.add(new JLabel(" or <  "));
		row1.add(Box.createHorizontalStrut(1));
		fldSmall = Static.createTextField(String.valueOf(DisplayFloat.Smallest),7, true);
		fldSmall.getDocument().addDocumentListener(roundTextListener);
		row1.add(fldSmall);
		row1.add(Box.createHorizontalGlue());
		
		page.add(row1);
		page.add(Box.createVerticalStrut(15));
		
		JPanel row2 = Static.createRowPanel();
		chkUseBigDec = new JRadioButton(); chkUseBigDec.setBackground(Color.white); 
		chkUseBigDec.addActionListener(roundActionListener);
		row2.add(chkUseBigDec);
		row2.add(Box.createHorizontalStrut(5));
		row2.add(new JLabel("Java defaults for when to display as E-notation"));
		row2.add(Box.createHorizontalGlue());
		page.add(row2);
		
		add(page);
		add(Box.createVerticalStrut(500));
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(chkUseFormat);
		bg.add(chkUseBigDec);
		
		if (DisplayFloat.Use_mode == DisplayFloat.USE_FORMAT) 
			 chkUseFormat.setSelected(true);
		else chkUseBigDec.setSelected(true);
	}
	
	ActionListener roundActionListener = new ActionListener() 
	{
        public void actionPerformed(ActionEvent ae) 
        {
        		updateRoundingTextFields();
        }        
    };
    DocumentListener roundTextListener = new DocumentListener() 
    {
	    	public void changedUpdate(DocumentEvent e) 
	    	{
	    		updateRoundingTextFields();
	    	}
	    	public void removeUpdate(DocumentEvent e) 
	    	{
	    		updateRoundingTextFields();
	    	}
	    	public void insertUpdate(DocumentEvent e) 
	    	{
	    		updateRoundingTextFields();
	    	}
    };  
    private void updateRoundingTextFields()
    {
	    	try
	    	{
			int sigFigs = Integer.parseInt(fldNumSigs.getText());
			if (sigFigs < 1)
			{
				sigFigs=DisplayFloat.Num_sig;
				fldNumSigs.setText(Integer.toString(sigFigs));
			}
			int dec = Integer.parseInt(fldNumDec.getText());
			if (dec < 1)
			{
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
			if (chkUseBigDec.isSelected()) mode = 2;
		    
			DisplayFloat.setFields(mode, sigFigs, dec, lg, sm);
			flushRoundingPrefs(); // must be after setFields
	    	}
	    	catch(Exception e) { ; // in the middle of editing
	    	}
    }
   
	public void flushRoundingPrefs() 
	{
		try
		{
			Preferences prefs = frame.getPreferencesRoot();
			if (prefs!=null) {
				prefs.put(roundPrefID(), DisplayFloat.getPrefString());
				prefs.flush();
			}
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Internal error in FieldTab: setting preferences");
		}
	}
	
	private STCWFrame frame = null;
	private JTextField fldNumSigs=null, fldNumDec=null;
	private JTextField fldLarge=null, fldSmall=null;
	private JRadioButton chkUseBigDec=null, chkUseFormat=null;
}
