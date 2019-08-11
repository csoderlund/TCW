package sng.util;

import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.net.URL;

import sng.viewer.STCWFrame;
import util.ui.TCWEditorPane;

/**
 * @author matt
 * Opens an HTML page for Instructions. First Option on Left
 */

public class StyleTextTab extends Tab
{
	private static final long serialVersionUID = 1043240071484012170L;

	public StyleTextTab ( STCWFrame parentFrame, Tab parentTab, String strURL )
	{
		super(parentFrame, parentTab);
		TCWEditorPane editorPane =null;
		URL url = getClass().getResource(strURL);
		if (url != null) 
		{
		    try 
		    {		
		    	editorPane = new TCWEditorPane(url);
		
		    } 
		    catch (Exception e) 
		    {
		    	url = null;
		    }
		}

		JScrollPane scrollPane = new JScrollPane(editorPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
		add(scrollPane);
	}
}