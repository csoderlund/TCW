package util.methods;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.database.Globalx;
import util.ui.MenuMapper;
import util.ui.UserPrompt;

public class Static {
	
	/******** Interface ***********/
	static public void center(JPanel panel) {
		panel.setMinimumSize(panel.getPreferredSize());
		panel.setMaximumSize(panel.getPreferredSize());
		panel.setAlignmentX(Component.CENTER_ALIGNMENT);
	}
	static public void border(JPanel panel) {
		panel.setMinimumSize(panel.getPreferredSize());
		panel.setMaximumSize(panel.getPreferredSize());
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}
	static public void setSize(JPanel panel) {
		panel.setMinimumSize(panel.getPreferredSize());
		panel.setMaximumSize(panel.getPreferredSize());
	}
	static public JPanel createPagePanel() {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.PAGE_AXIS)); // Y_AXIS
		row.setBackground(Globalx.BGCOLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		return row;
	}
	static public JPanel createPageTopPanel() {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.PAGE_AXIS)); // Y_AXIS
		row.setBackground(Globalx.BGCOLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setAlignmentY(Component.TOP_ALIGNMENT);
		return row;
	}
	static public JPanel createPageCenterPanel() {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.PAGE_AXIS)); // Y_AXIS
		row.setBackground(Globalx.BGCOLOR);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		row.setAlignmentY(Component.TOP_ALIGNMENT);
		return row;
	}
	
	static public JPanel createRowPanel() {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS)); // X_AXIS
		row.setBackground(Globalx.BGCOLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		return row;
	}
	static public JPanel createRowCenterPanel() {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS)); // X_AXIS
		row.setBackground(Globalx.BGCOLOR);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		row.setAlignmentY(Component.TOP_ALIGNMENT);
		return row;
	}
	static public JLabel createLabel(String label) {
		JLabel tmp = new JLabel(label);
		tmp.setBackground(Color.white);
		tmp.setOpaque(true);  // allows the color to be changed on Mac
		tmp.setEnabled(true);
		return tmp;
	}
	static public JLabel createLabel(String label, boolean enable) {
		JLabel tmp = new JLabel(label);
		tmp.setBackground(Color.white);
		tmp.setOpaque(true);  // allows the color to be changed on Mac
		tmp.setEnabled(enable);
		return tmp;
	}
	static public JLabel createLabel(String label, Color c) {
		JLabel tmp = new JLabel(label);
		tmp.setBackground(c);
		tmp.setOpaque(true);  // allows the color to be changed on Mac
		tmp.setEnabled(false);
		return tmp;
	}
	static public JButton createButton(String label, boolean enable, Color color) {
		JButton button = new JButton(label);
		if (color!=null) button.setBackground(color); // create obique background by default
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setEnabled(enable);
		return button;
	}
	static public JButton createButton(String label, boolean enable) {
		JButton button = new JButton(label);
		button.setBackground(Color.white);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setEnabled(enable);
		return button;
	}
	static public JButton createButtonPlain(String label, boolean enable) {
		JButton button = new JButton(label);
		button.setBackground(Color.white);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setFont(new Font(button.getFont().getName(),Font.PLAIN,10));
		button.setEnabled(enable);
		return button;
	}
	static public JCheckBox createCheckBox(String label) {
		JCheckBox chkBox = new JCheckBox(label);
		chkBox.setBackground(Color.white);
		chkBox.setSelected(false);
		return chkBox;
	}
	static public JCheckBox createCheckBox(String label, boolean check) {
		JCheckBox chkBox = new JCheckBox(label);
		chkBox.setBackground(Color.white);
		chkBox.setSelected(check);
		return chkBox;
	}
	static public JCheckBox createCheckBox(String label, boolean check, boolean enable) {
		JCheckBox chkBox = new JCheckBox(label);
		chkBox.setBackground(Color.white);
		chkBox.setSelected(check);
		chkBox.setEnabled(enable);
		return chkBox;
	}
	static public JRadioButton createRadioButton(String label, boolean enable) {
		JRadioButton radio = new JRadioButton(label);
		radio.setBackground(Color.white);
		radio.setSelected(enable);
		return radio;
	}
	
	// for things like status
	static public JTextField createTextFieldNoEdit(int width) {
		JTextField tmp = new JTextField(width);
		tmp.setBackground(Color.white);
		tmp.setMaximumSize(tmp.getPreferredSize());
		tmp.setMinimumSize(tmp.getPreferredSize());
		tmp.setBorder(BorderFactory.createEmptyBorder());
		tmp.setEditable(false);
	 	tmp.setAlignmentX(Component.LEFT_ALIGNMENT);
    	
		return tmp;
	}
	static public JTextField createTextField(String def, int width) {
		JTextField tmp = new JTextField(width);
		tmp.setBackground(Color.white);
		tmp.setMaximumSize(tmp.getPreferredSize());
		tmp.setMinimumSize(tmp.getPreferredSize());
		tmp.setText(def);
		return tmp;
	}
	
	static public JTextField createTextField(String def, int width, boolean enable) {
		JTextField tmp = new JTextField(width);
		tmp.setBackground(Color.white);
		tmp.setMaximumSize(tmp.getPreferredSize());
		tmp.setMinimumSize(tmp.getPreferredSize());
		tmp.setText(def);
		tmp.setEnabled(enable);
		return tmp;
	}
	static public JLabel createTitleLabel(String label) {
		JLabel lblTitle = new JLabel(label);
		lblTitle.setBackground(Color.white);
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		return lblTitle;
	}
	static public JTextArea createTextArea(String text) {
		JTextArea txtDesc = new JTextArea(text);
		Font f = txtDesc.getFont();
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);
		txtDesc.setMaximumSize(txtDesc.getPreferredSize());
		return txtDesc;
	}
	static public void addHorzBox(JPanel row, JLabel label, int width) {
		if (width>0 && width > label.getPreferredSize().width) 
			row.add(Box.createHorizontalStrut(width-label.getPreferredSize().width));
	}
	static public JLabel createCenteredLabel ( String str )
	{
		JLabel label = new JLabel ( str );
		label.setAlignmentX( Component.CENTER_ALIGNMENT );
		return label;
	}
   static public void centerScreen( Window win ) 
    {
          Dimension dim = win.getToolkit().getScreenSize();
          Rectangle abounds = win.getBounds();
          win.setLocation((dim.width - abounds.width) / 2,
              (dim.height - abounds.height) / 2);
    }
   static public JSeparator getSeparator() {
		return new JSeparator();
		/**
		JSeparator separate = new JSeparator(JSeparator.HORIZONTAL);
		Dimension d = separate.getPreferredSize();
		d.width = separate.getMaximumSize().width;
		d.height = separate.getMaximumSize().height;
		separate.setMaximumSize( d );
		separate.setMinimumSize( d );
		return separate;
		**/
	}
   static public JComboBox<MenuMapper> createZoom() { // for single contig display
	   	JComboBox <MenuMapper> menuZoom = new JComboBox<MenuMapper> ();
		menuZoom.addItem( new MenuMapper ( "Zoom 1:1", 1 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:2", 2 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:3", 3 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:4", 4 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:5", 5 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:6", 6 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:7", 7 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:8", 8 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:9", 9 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:10", 10 ) );
		menuZoom.setBackground(Globalx.BGCOLOR);
		menuZoom.setSelectedIndex(0);
		
		Dimension dim = new Dimension ( (int)(menuZoom.getPreferredSize().getWidth()), 
				(int)menuZoom.getPreferredSize().getHeight() );
		menuZoom.setPreferredSize( dim );
		menuZoom.setMaximumSize ( dim );
		
		return menuZoom;
   }
   static public JComboBox<MenuMapper> createZoom2() { // for sng pair and all cmp pair display
	   	JComboBox <MenuMapper> menuZoom = new JComboBox<MenuMapper> ();
		
		menuZoom.addItem( new MenuMapper ( "Zoom 5:1", -5 ) );
	   	menuZoom.addItem( new MenuMapper ( "Zoom 4:1", -4 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 3:1", -3 ) );
	   	menuZoom.addItem( new MenuMapper ( "Zoom 2:1", -2 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:1", 1 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:2", 2 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:3", 3 ) );
		menuZoom.addItem( new MenuMapper ( "Zoom 1:4", 4 ) );
		
		menuZoom.setBackground(Globalx.BGCOLOR);
		menuZoom.setSelectedIndex(4); // if change this, Change sng.PairViewPanel
		
		Dimension dim = new Dimension ( (int)(menuZoom.getPreferredSize().getWidth()), 
				(int)menuZoom.getPreferredSize().getHeight() );
		menuZoom.setPreferredSize( dim );
		menuZoom.setMaximumSize ( dim );
		
		return menuZoom;
  }
 
   static public JComboBox<String> createCombo(String [] labels, boolean enable) {
	   	JComboBox<String> cbox = createCombo(labels);
	   	cbox.setEnabled(enable);
	   
		return cbox;
  }
   static public JComboBox<String> createCombo(String [] labels) {
	   	JComboBox<String> cbox = new JComboBox<String>();
	   	cbox.setBackground(Globalx.BGCOLOR);
	  
		for (int i=0; i<labels.length; i++) 
			cbox.addItem(labels[i]); 
		cbox.setSelectedIndex(0);
		
		Dimension dim = new Dimension ( (int)(cbox.getPreferredSize().getWidth()), 
				(int)cbox.getPreferredSize().getHeight() );
		cbox.setPreferredSize( dim );
		cbox.setMaximumSize ( dim );
		return cbox;
   }
	/********** Converters *******************/
	static public boolean isDouble(String val) {
		try {
			Double.parseDouble(val);
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
	static public boolean isInteger(String val) {
		try {
			Integer.parseInt(val);
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
	static public boolean isDouble(String msg, String d) {
		try {
			Double.parseDouble(d);
			return true;
		}
		catch (Exception e) {
			UserPrompt.showError("Incorrect  " + msg + ": " + d + "\nMust be float, e.g. 1E-10");
			return false;
		}
	}
	static public boolean isInteger(String msg, String d) {
		try {
			Integer.parseInt(d);
			return true;
		}
		catch (Exception e) {
			UserPrompt.showError("Incorrect  " + msg + ": " + d + "\nMust be digits, e.g. 123");
			return false;
		}
	}
	static public int getInteger(String val) {
		try {
			int i = Integer.parseInt(val);
			return i;
		}
		catch(Exception e) {
			return Integer.MIN_VALUE;
		}
	}
	static public double getDouble(String val) {
		try {
			double i = Double.parseDouble(val);
			return i;
		}
		catch(Exception e) {
			return Double.MAX_VALUE;
		}
	}
	static public String addQuote(String s) {
  		String ss=s;
  		if (s.contains("'")) ss= '"' + ss + '"';
  		else ss= "'" + ss + "'";
  		
  		return ss;
	}
	// makes necessary changes for mysql query
	static public String addQuoteDB(String s) {
	  		String ss=s;
			ss = ss.replace("'", "\\'");
	  		ss = "'" + ss + "'";
	  		return ss;
	}
	static public String addWildDB(String s) {
  		String ss=s.substring(1, s.length()-1);
  		ss = ss.replace("_", "\\_");
  		ss = "'%" + ss + "%'";
  		return ss;
}
	static public String addQuoteDBList(Vector <String> list) {
	  		String ss="";
	  		for (String s : list) {
	  			s = s.replace("'", "\\'");
	  			if (ss=="") ss = "'" + s + "'";
	  			else ss += ",'" + s + "'";
	  		}
	  		return ss;
	}
	static public String strMerge(String t1, String t2) {
		if (t1!="" && t2!="") return t1 + ", " + t2;
		if (t1!="") return t1;
		return t2;
	}
	/** The next 4 are also in Out.java **/
	static public int percent(int x, int y) {
		return (int) (((double) x/(double) y) * 100.0);
	}
	static public String perText(int cnt, int nCtg) {
		double p = 0;
	 	if (cnt>0) p = ( ( ( ( (double) cnt/ (double) nCtg) ) *100.0) + 0.5);
	 	if (p<1.0) return String.format("(<1%s)", "%");
	 	int i = (int) p;
	 	return String.format("(%d%s)",  i, "%");
	}
	static public String perText(long cnt, long nCtg) {
		double p = 0;
	 	if (cnt>0) p = ( ( ( ( (double) cnt/ (double) nCtg) ) *100.0) + 0.5);
	 	if (p<1.0) return String.format("(<1%s)", "%");
	 	int i = (int) p;
	 	return String.format("(%d%s)",  i, "%");
	}
	/**/
	static public String combineBool(String left, String right) {
		if(left == null || left.length() == 0) return right;
		if(right == null || right.length() == 0) return left;
		return left + " AND " + right;
	}
	static public String combineBool(String left, String right, boolean isAND) {
		if(left == null || left.length() == 0) return right;
		if(right == null || right.length() == 0) return left;
		if(isAND) return left + " AND " + right;
		return left + " OR " + right;
	}
	
	static public String combineSummary(String left, String right) {
		if(left == null || left.length() == 0) return right;
		if(right == null || right.length() == 0) return left;
		return left + ", " + right;
	}
	static public String combineSummary(String left, String right, String delim) {
		if(left == null || left.length() == 0) return right;
		if(right == null || right.length() == 0) return left;
		return left + delim + right;
	}
	static public void log(String msg) {
		System.out.println("LOG: " + msg);
	}
	// Unique_hit goList is GO:N:EC; 
	static public Vector <Integer> goList (String msg, String goStr) {
		Vector <Integer> goSet = new Vector <Integer> ();
		if (goStr==null || goStr.trim()=="") return goSet;
		
		String [] gos = goStr.split(";");
		for (String go : gos) {
			String [] tok = go.split(":");
			try {int gonum = Integer.parseInt(tok[1]);goSet.add(gonum);}
			catch (Exception e) {}
		}
		return goSet;
	}
	/** Utilities ***/
	static public String strVectorJoin(java.util.Vector<String> sa, String delim)
	{
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < sa.size(); i++)
		{
			out.append(sa.get(i));
			if (i < sa.size() - 1)
				out.append(delim);
		}
		return out.toString();
	}
 
    static public Vector<String> split ( String str, String strDelims )
    {
        if ( str == null )
            return null;
        
        Vector<String> list = new Vector<String> ();
        StringTokenizer toker = new StringTokenizer ( str, strDelims );
        while ( toker.hasMoreTokens() )
            list.add ( toker.nextToken() ); 
        
        return list;
    }
    
    public static String join ( Object [] theList, String strDelim )
    {
        String str = "";
        for ( int i = 0; i < theList.length; ++i )
        {
            if ( !isEmpty(str) )
                str += strDelim;
            str += theList[i].toString();
        }
        return str;
    }
    
    public static <T> String join ( Iterator<T> iter, String strDelim )
    {
        String str = "";
        while ( iter.hasNext() )
        {
            if ( !isEmpty(str) )
                str += strDelim;
            str += iter.next().toString();
        }
        return str;
    }
    
    static public boolean isEmpty ( String str )
    {
        return str == null || str.trim().length() == 0;
    }
    static public boolean isNotEmpty ( String str )
    {
        return str != null && str.trim().length() > 0;
    }
    static String stripQuotes ( String str )
    {
        if ( str.length() >= 2 &&
                (str.charAt(0) == '\'' || str.charAt(0) == '\"') &&
                    (str.charAt(str.length()-1) == '\'' || str.charAt(str.length()-1) == '\"' ) )
            return str.substring( 1, str.length()-1 );
        else
            return str;
    }
    // CAS322 
    public static HashMap <String, String> getGOtermMap() {
    	HashMap <String, String> goMap = new HashMap <String, String>  ();
    	
    	for (int i=0; i<Globalx.GO_TERM_LIST.length; i++) {
    		goMap.put(Globalx.GO_TERM_LIST[i], Globalx.GO_TERM_ABBR[i]);
    	}
    	return goMap;
    }
    // CAS324
    public static String goFormat(int gonum) {
    	return String.format(Globalx.GO_FORMAT, gonum);
    }
    // CAS334 
    public static boolean isOlap(int s1, int e1, int s2, int e2) {
    	if (s1>=s2 && s1<e2) return true;
    	if (s2>=s1 && s2<e1) return true;
    	return false;
    }
    public static boolean isS1InS2(int s1, int e1, int s2, int e2) {
    	return (s1>=s2 && e1 <= e2); 
    }
    public static String dFormat(int num) {
    	DecimalFormat df = new DecimalFormat("#,###,###");
    	return df.format(num);
    }
}
