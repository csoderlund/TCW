package sng.viewer.panels;

import java.awt.*;
import javax.swing.*;

import sng.viewer.panels.seqDetail.DrawContigPanel;
import util.database.Globalx;

/*
 * Legend beneath contig and multiple alignment panel
 */
public class LegendPanel extends JPanel
{
	private static final long serialVersionUID = -5292053168212278988L;

	public LegendPanel ( int i)         // for contig alignment
	{
		setLayout( null );
        setBackground( Color.WHITE );
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        if (i==1) // contig alignment    width, height
        		setMinimumSize(new Dimension(500, 200));
        else if (i==2) // for pairwise alignment, need smaller box
            setMinimumSize(new Dimension(340, 120));
        else 		   // noAssembly, just showing sequence
        	 	setMinimumSize(new Dimension(330, 100));
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

		if (!isPair && hasNoAssembly) {
			drawKey(0, g2, Globalx.lowQualityBlue, 	"If nucleotide, Poor Quality Region", x+20, y+=15);
		}
		else if (isPair) {
			drawKey(0, g2, Globalx.gapGreen, 		"Gap", 		x+20, y+=15);
			drawKey(0, g2, Globalx.mismatchRed, 	"NT Mismatch or AA " + Globalx.blosumNeg, x+20, y+=15);
			drawKey(0, g2, Globalx.purple, 		"AA " + Globalx.blosumPosLegend, x+20, y+=15);
			drawKey(0, g2, Globalx.mediumGray, 	"Unknown NT or AA, or extended end", 	 x+20, y+=15);
			drawKey(1, g2, Globalx.mediumGray, 	"In Frame Stop Codons", 	 x+20, y+=15);
		}
		else {
			g2.drawString("Foreground:", x, y+=25);
			drawKey(0, g2, Globalx.mismatchRed, 	"Mismatch", 	x+20, y+=15);
			drawKey(0, g2, Globalx.gapGreen, 		"Gap (column of gaps may occur if buried not shown)", 	x+20, y+=15);
			drawKey(0, g2, Globalx.lowQualityBlue,"Poor Quality Region (phred < 20)", x+20, y+=15);
			drawKey(0, g2, Globalx.mediumGray, 	"N or extended end", x+20, y+=15);
			
			g2.drawString("Background:", x, y+=30);
			drawKey(0, g2, DrawContigPanel.contigFor, "Forward", x+20, y+=15);
			drawKey(0, g2, DrawContigPanel.contigRev, "Reverse", x+20, y+=15);
			drawKey(0, g2, DrawContigPanel.contigErr, "EST has unexpected orientation (e.g. 5' reverse complemented)", x+20, y+=15);
		}
	}
	private boolean isPair=false;
	private boolean hasNoAssembly=false;
	public void setIsPair(boolean b) {isPair=b;}
	public void setHasNoAssembly(boolean b) {hasNoAssembly=b;}
		
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
}
