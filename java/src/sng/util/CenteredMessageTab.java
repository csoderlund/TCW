package sng.util;
import javax.swing.*;

import java.awt.*;
import util.ui.CenteredMessagePanel;


public class CenteredMessageTab extends Tab
{
	public CenteredMessageTab ( String strMessage )
	{
		super(null, null);
		
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ) );
		add( BorderLayout.CENTER, new CenteredMessagePanel( strMessage ) );
	}
    
    private static final long serialVersionUID = 1;
}
