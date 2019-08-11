package sng.amanager;

/*****************************************************
 * Edit Associated Counts
 */
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;


import sng.database.Globals;
import util.ui.UserPrompt;

public class EditCountPanel extends JPanel {
	private static final long serialVersionUID = 2806917997839245167L;

	private static final int COLUMN_LABEL_WIDTH = 120;
	private static final int TEXTFIELD_WIDTH = 30;
		
	public static final String EXP_LIB_SYMBOLS =  "SeqID";
	// if these are changed, also change in ManagerFrame updateExpLibEditUI, updateExpLibUI, updateTransLibEditUI, updateTransLibUI
	public static final String [] ATTRIBUTE_SYMBOLS = 
		{ "Title", "Species", "Cultivar", "Strain", "Tissue", "Stage", "Treatment",  "Year", "Source" };

	public EditCountPanel(ManagerFrame parent) {
		theParentFrame = parent;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.PAGE_AXIS));
		innerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		innerPanel.setBackground(Globals.BGCOLOR);
		
		JLabel title = new JLabel("Condition Attributes");
		title.setFont(innerPanel.getFont().deriveFont(Font.BOLD, 18));
		JPanel row = createRowPanel();
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		innerPanel.add(Box.createVerticalStrut(10));
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(20));
	
		row = createRowPanel();
		lblID = new JLabel(EXP_LIB_SYMBOLS);
		row.add(lblID);
		if(lblID.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblID.getPreferredSize().width));
		tfID = new JTextField(TEXTFIELD_WIDTH);
		row.add(tfID);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		//Attribute panel
		innerPanel.add(Box.createVerticalStrut(10));
		
		lblAttr = new JLabel [ATTRIBUTE_SYMBOLS.length];
		tfAttr = new JTextField [ATTRIBUTE_SYMBOLS.length];
		for(int x=0; x < ATTRIBUTE_SYMBOLS.length; x++) {
			row = createRowPanel();
			
			lblAttr[x] = new JLabel(ATTRIBUTE_SYMBOLS[x]);
			row.add(lblAttr[x]);
			if(lblAttr[x].getPreferredSize().width < COLUMN_LABEL_WIDTH)
				row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblAttr[x].getPreferredSize().width));
			tfAttr[x] = new JTextField(TEXTFIELD_WIDTH);
			row.add(tfAttr[x]);
			
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
		}
			
		btnKeep = new JButton("Keep");
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateExpLibUI();
				theParentFrame.tcwDBupdateLibAttributes(1);
				setVisible(false);
				theParentFrame.setMainPanelVisible(true);
				
				theParentFrame.updateUI();
				theParentFrame.saveProject();
				
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		btnDiscard = new JButton("Cancel");
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(theParentFrame.isRemoveExpLibOnDiscard()) {
					theParentFrame.clearCurrentExpLib();
					theParentFrame.updateUI();
				}
				setVisible(false);
				theParentFrame.setMainPanelVisible(true);			
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Condition Help", "html/runSingleTCW/EditExpLibPanel.html");
			}
		});

		JPanel buttonRow = createRowPanel();
		buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonRow.add(btnKeep);
		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnDiscard);
		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnHelp);
		buttonRow.add(Box.createHorizontalGlue());
		
		buttonRow.setMaximumSize(buttonRow.getPreferredSize());
		buttonRow.setMinimumSize(buttonRow.getPreferredSize());
		
		innerPanel.setMaximumSize(innerPanel.getPreferredSize());
		innerPanel.setMinimumSize(innerPanel.getPreferredSize());
		
		add(innerPanel);
		add(Box.createVerticalStrut(10));
		add(buttonRow);
		setVisible(false);
	}
	public void updateExpLibEditUI(int index, boolean removeOnDiscard) {
		ManagerData curManData = theParentFrame.getCurManData();
		nCurrentExpLibIndex = index;
		ManagerData.CountData x = curManData.getCountLibAt(index);
		setEnabled(false);
		setValue(EXP_LIB_SYMBOLS, x.getCondID());
		setValue(ATTRIBUTE_SYMBOLS[0], x.getTitle());
		setValue(ATTRIBUTE_SYMBOLS[1], x.getOrganism());
		setValue(ATTRIBUTE_SYMBOLS[2], x.getCultivar());
		setValue(ATTRIBUTE_SYMBOLS[3], x.getStrain());
		setValue(ATTRIBUTE_SYMBOLS[4], x.getTissue());
		setValue(ATTRIBUTE_SYMBOLS[5], x.getStage());
		setValue(ATTRIBUTE_SYMBOLS[6], x.getTreatment());
	
		setValue(ATTRIBUTE_SYMBOLS[7], x.getYear());
		setValue(ATTRIBUTE_SYMBOLS[8], x.getSource());
	}

	private void updateExpLibUI() {
		ManagerData curManData = theParentFrame.getCurManData();
		ManagerData.CountData x = curManData.getCountLibAt(nCurrentExpLibIndex);
		x.setCondID(getValue(EditCountPanel.EXP_LIB_SYMBOLS));
		x.setTitle(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[0])); 
		x.setOrganism(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[1]));
		x.setCultivar(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[2]));
	    x.setStrain(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[3]));
		x.setTissue(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[4]));
		x.setStage(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[5]));
		x.setTreatment(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[6]));
		x.setYear(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[7]));
		x.setSource(getValue(EditCountPanel.ATTRIBUTE_SYMBOLS[8]));
	}
	private String getValue(String symbol) {
		if (symbol.equals(EXP_LIB_SYMBOLS)) return tfID.getText();
		for(int x=0; x<ATTRIBUTE_SYMBOLS.length; x++) {
			if(ATTRIBUTE_SYMBOLS[x].equals(symbol)) {
				return tfAttr[x].getText(); 
			}
		}	
		return "";
	}
	private void setValue(String symbol, String value)  {
		if (symbol.equals(EXP_LIB_SYMBOLS)) {
			tfID.setText(value);
			return;
		}
		for(int x=0; x<ATTRIBUTE_SYMBOLS.length; x++) {
			if(ATTRIBUTE_SYMBOLS[x].equals(symbol)) {
				tfAttr[x].setText(value); 
				return;
			}
		}
	}
	public void setEnabled(boolean b) {
		lblID.setEnabled(b);
		tfID.setEnabled(b);
	}
	private CaretListener updateListener = new CaretListener() {
		public void caretUpdate(CaretEvent e) {
			
		}
	};
	
	private JPanel createRowPanel() {
		JPanel retVal = new JPanel();
		retVal.setLayout(new BoxLayout(retVal, BoxLayout.LINE_AXIS));
		retVal.setAlignmentX(Component.LEFT_ALIGNMENT);
		retVal.setBackground(Globals.BGCOLOR);
		
		return retVal;
	}
	private JPanel getInstance() { return this; }
	
	private JLabel lblID = null;
	private JTextField tfID  = null;
	
	private JLabel [] lblAttr = null;
	private JTextField [] tfAttr = null;
	
	private JButton btnKeep = null;
	private JButton btnDiscard = null;
	private JButton btnHelp = null;
	private int nCurrentExpLibIndex= -1;
	private ManagerFrame theParentFrame = null;
}
