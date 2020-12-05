package cmp.viewer.seq.align;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import cmp.database.Globals;
import util.database.Globalx;

public class LegendPanel extends JPanel
{
	private static final long serialVersionUID = -5292053168212278988L;
	
	public LegendPanel(boolean isAA, int colorScheme) {
		this.type= (isAA) ? Globals.AA : Globals.NT;
		this.colorScheme= (isAA) ? colorScheme : 0;
		
		setLayout( null );
        setBackground( Globals.BGCOLOR );
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        
        if (colorScheme==0) setMinimumSize(new Dimension(340, 140)); // width, height
        else setMinimumSize(new Dimension(340, 200));
        setPreferredSize(getMinimumSize());
        setMaximumSize(getMinimumSize());
	}
	public void paintComponent(Graphics g)
	{
		super.paintComponent( g );
		Graphics2D g2 = (Graphics2D)g;
	
		int y = 10;
		int x = 10;
		g2.drawString("Legend", x, y+=10);
		x += 25;
		
		g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, g2.getFont().getSize()));

		if (colorScheme==0) {
			drawKey(0, g2, BaseAlignPanel.anyGap, 	    "Gap", 							x+20, y+=15);
			if (type==Globals.AA) {
				drawKey(0, g2, BaseAlignPanel.aaLtZero, 	Globalx.blosumNeg, 		x+20, y+=15);
				drawKey(0, g2, BaseAlignPanel.aaGtZero, 	Globalx.blosumPosLegend,x+20, y+=15);
				drawKey(1, g2, BaseAlignPanel.aaStop, 		"Stop Codon	", 	 		x+20, y+=15);
			}
			else {
				drawKey(0, g2, BaseAlignPanel.ntMisMatch, 	"Mismatch", 			x+20, y+=15);
			}
			drawKey(0, g2, BaseAlignPanel.anyHang, 		"Extended end", 	 		x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.anyUnk, 		"Unknown", 	 				x+20, y+=15);
		}
		else {
			drawKey(0, g2, BaseAlignPanel.zPhobic, 		"Hydrophobic	[ILVAM]", 	 	x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.zAro, 		"Aromatic		[FWY]", 	 	x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.zPos, 		"Positive		[KRH]", 	 	x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.zNeg, 		"Negative		[DE]", 	 		x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.zPhilic, 		"Hydrophilic	[STNQ]", 	 	x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.zCon, 		"Conform...		[PG]", 	 		x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.zCys, 		"Cysteine		[C]", 	 		x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.anyGap, 		"Gap			[-]", 	 		x+20, y+=15);
			drawKey(0, g2, BaseAlignPanel.anyUnk, 		"Unknown", 	 				x+20, y+=15);
		}
	}

	private static void drawKey(int type, Graphics2D g2, Color c, String s, int x, int y) 
	{
		g2.setColor(c);
		if (type == 0)
			g2.fillRect(x, y, 10, 10);
		else {
			Polygon triangle = new Polygon();
			triangle.addPoint(x,y);
			triangle.addPoint(x+10,y);
			triangle.addPoint(x+5,y+10);
			g2.fill(triangle);
		}
		
		g2.setColor(Color.BLACK);
		g2.drawString("= "+s, x+15, y+9);
	}
	private int type=-1, colorScheme=0;
}
