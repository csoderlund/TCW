package sng.viewer.panels.seqDetail;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import java.util.prefs.Preferences;

/*
 * Used to create the panel of CAP3 options
 */
public class CAP3LineInputPanel extends JPanel
{
	public CAP3LineInputPanel ( String inSuffix )
	{	
		optionLayout = new GridBagLayout ();
		setLayout( optionLayout );
		gridConstraints = new GridBagConstraints();
		gridConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridConstraints.insets = new Insets ( 0, 0, 0, 10 ); 
		
		strSuffix = inSuffix;
		if ( strSuffix == null ) strSuffix = "";
		
		optionList = new Vector<CAP3LineParameter> ();
		widgetArray = new Vector<JComponent> ();
		labelArray = new Vector<JLabel> ();
	}
	
	public void restoreDefaults ( )
	{
		for ( int i = 0; i < nNumRows; ++i )
		{
			getParameterAt(i).setWidgetToDefault( getWidgetAt(i) );
			getLabelAt(i).setForeground( Color.BLACK );
		}
	}
	
	private static String createPrefsKey ( String strSuffix, String strOption )
	{
		return strOption + strSuffix;
	}
	
	public boolean getUserPrefs ( Preferences inUserPrefs ) throws Exception
	{
		if (inUserPrefs==null) return false;
		
		if ( getOptionString ( ) == null ) return false;// Validate the data first

		for ( int i = 0; i < nNumRows; ++i )
		{
			getParameterAt(i).getUserPrefFromWidget( getWidgetAt(i), inUserPrefs, strSuffix );
		}
		inUserPrefs.flush();
		
		return true;
	}
	
	public void setUserPrefs ( Preferences inUserPrefs )
	{	
		for ( int i = 0; i < nNumRows; ++i )
		{
			getParameterAt(i).setWidgetToUserPref( inUserPrefs, strSuffix, getWidgetAt(i) );
			getLabelAt(i).setForeground( Color.BLACK );
		}
	}
	
	public void setOptions( String options )
	{
		String[] tokens = options.split("[->]"); // possible "> /dev/null 2>&1" at end of line
		
		for (int i = 0;  i < tokens.length;  i++)
		{
			String[] parts = tokens[i].split(" ");
			
			if (parts.length >= 2 && !parts[0].equals("") && !parts[1].equals("")) {
				for (int j = 0;  j < nNumRows;  ++j)
				{
					CAP3LineParameter curParam = getParameterAt(j);
					JComponent curWidget = getWidgetAt(j);
					
					String name = curParam.getOptionName( );
					if (name.equals(parts[0]))
						curParam.setOptionValue(curWidget, parts[1]);
				}
			}
		}
	}
	
	public String getOptionString ( )
	{
		boolean bCurOK = false;
		boolean bAllOK = true;
		String str = "";
		
		for ( int i = 0; i < nNumRows; ++i )
		{
			CAP3LineParameter curParam = getParameterAt (i);
			JComponent curWidget = getWidgetAt (i);
			
			// Validate the current row's value
			bCurOK = doValidation ( getLabelAt (i), curWidget, curParam, null );
			
			// If OK add to list of parameters
			if ( bCurOK )
			{
				// Append the current option switch to the string
				// (The object may return null if it is still at its default value)
				String strSwitch = curParam.getOption( curWidget );
				if ( !(strSwitch == null || strSwitch.length() == 0))
					str += " " + strSwitch;
			}
			else
			{
				bAllOK = false;
			}
		}

		if ( bAllOK )	return str;
		else return null;
	}
    
    public void addIntOptionRow ( String strOption, String strDes, int nMin, int nMax, int nDefault )
	{
		addOptionRow ( new IntegerParameter ( strOption, strDes, nMin, nMax, nDefault ) );
	}
	
	protected void addOptionRow ( final CAP3LineParameter theParam )
	{	
		final JComponent widget = theParam.getUIWidget( );

		int nRow = nNumRows;
		
		// Set grid constraints to current row
		gridConstraints.gridy = nRow;
		
		// Add widget for editing
		gridConstraints.gridx = 0;
		gridConstraints.weightx = 1.0;
		optionLayout.setConstraints( widget, gridConstraints );
		add ( widget );
		
		final JLabel label = new JLabel ( theParam.getLabelText() );
		gridConstraints.gridx = 1;
		gridConstraints.weightx = 4.0;
		optionLayout.setConstraints( label, gridConstraints );
		add ( label );
		
		optionList.add ( theParam );
		widgetArray.add ( widget );
		labelArray.add ( label );
		
		++nNumRows;		
	}
	
	private boolean doValidation ( JLabel label, JComponent widget, CAP3LineParameter theParam, Component toBeFocused )
	{
		if ( !theParam.isDataValid(widget) )
		{
			// Set label to red and return focus to the widget
			label.setForeground( Color.RED );
			label.setText( theParam.getLabelText() + "    <== " + theParam.getDataValidMsg(widget) );
			
			// Return focus to the widget unless a button was pressed.  This
			// prevents that annoying scenario of being forced to fix the value when
			// canceling or reset the default values.
			if (!( toBeFocused instanceof JButton ))
				widget.requestFocus(); 
	
			return false;
		}
		else
		{
			label.setForeground( Color.BLACK );						
			label.setText( theParam.getLabelText() );
			return true;
		}		
	}
	private CAP3LineParameter getParameterAt ( int i )
	{
		return (CAP3LineParameter)optionList.get(i);		
	}
	private JComponent getWidgetAt ( int i )
	{
		return (JComponent)widgetArray.get(i);
	}
	private JLabel getLabelAt ( int i )
	{
		return (JLabel)labelArray.get(i);
	}
   
	protected class IntegerParameter implements CAP3LineParameter
	{
		public IntegerParameter ( String inOption, String inDes, int inMin, int inMax, int inDefault )
		{
			strOption = inOption;
			strDes = inDes;
			nMin = inMin;
			nMax = inMax;
			nDefault = inDefault;
		}
		
		public JComponent getUIWidget ( )
		{
			return new JTextField ( String.valueOf( nDefault ) );
		}
		
		public String getOption ( JComponent widget )
		{
			JTextField edit = (JTextField)widget;
			int nVal = Integer.parseInt( edit.getText() );
			if ( nVal == nDefault ) return null;
			else
				return "-" + strOption + " " + nVal;
		}
		public String getOptionName( ){return strOption;}

		public void setOptionValue( JComponent widget, String value )
		{
			JTextField textfield = (JTextField)widget;
			textfield.setText(value);
		}
		
		public String getLabelText ( )
		{
			String str = strDes + " ( ";
			if ( nMin != Integer.MIN_VALUE ) str += String.valueOf( nMin ) + " < " ;
			str += "N";
			if ( nMax != Integer.MAX_VALUE ) str += " < " + String.valueOf( nMax );		
			str += " )";
			return str;
		}
		
		public boolean isDataValid ( JComponent widget )
		{
			return getDataValidMsg ( widget ) == null;
		}
		
		public String getDataValidMsg ( JComponent widget )
		{
			int nCur = Integer.MAX_VALUE;
			JTextField edit = (JTextField)widget;
		
			// Convert the current row's value to an int
			try
			{
				nCur = Integer.valueOf( edit.getText() ).intValue();
			}
			catch (Exception err)
			{
				return "\"" + edit.getText() + "\" is not a valid integer.";
			}
			
			// Validate it's range
			if ( nMin < nCur && nCur < nMax ) return null;
			else return "\"" + edit.getText() + "\" is out of range.";
		}		
		public void setWidgetToDefault ( JComponent widget )
		{
			JTextField edit = (JTextField)widget;
			edit.setText ( String.valueOf(nDefault) );
		}		
		public void setWidgetToUserPref ( Preferences userPrefs, String strSuffix, JComponent widget )
		{
			// Set with user preferences if available.  Otherwise use the default.
			int  nVal = userPrefs.getInt ( createPrefsKey ( strSuffix, strOption ), nDefault );	
			JTextField edit = (JTextField)widget;		
			edit.setText ( String.valueOf(nVal) );
		}
		public void getUserPrefFromWidget ( JComponent widget, Preferences userPrefs, String strSuffix )
		{
			JTextField edit = (JTextField)widget;		
			userPrefs.putInt ( createPrefsKey ( strSuffix, strOption ), Integer.valueOf( edit.getText() ).intValue() );
		}
		
        public void getDefault ( Preferences userPrefs, String strSuffix )
        {   
            userPrefs.putInt( createPrefsKey ( strSuffix, strOption ), nDefault );
        }
		
		String strOption;
		String strDes;
		int nMin = Integer.MIN_VALUE;
		int nMax = Integer.MAX_VALUE;
		int nDefault;
	}

	public interface CAP3LineParameter 
	{
		public JComponent getUIWidget ( );
		public String getOption ( JComponent widget );
		public String getOptionName ( );
		public void setOptionValue( JComponent widget, String value );
		public String getLabelText ( );
		public boolean isDataValid ( JComponent widget );
		public String getDataValidMsg ( JComponent widget );
		public void setWidgetToDefault ( JComponent widget );
		public void getDefault ( Preferences userPrefs, String strSuffix ); 
		public void setWidgetToUserPref ( Preferences userPrefs, String strSuffix, JComponent widget );
		public void getUserPrefFromWidget ( JComponent widget, Preferences userPrefs, String strSuffix );
	};
	
	private String strSuffix = "";

	// Arrays holding all of the option data for each row
	private Vector<CAP3LineParameter> optionList = null; // CommandlineParameter objects
	private Vector<JComponent> widgetArray = null; // JComponent objects
	private Vector<JLabel> labelArray = null; // JLabel objects
	private int nNumRows = 0;
	private GridBagConstraints gridConstraints;
	private GridBagLayout optionLayout;
    
    private static final long serialVersionUID = 1;
}
