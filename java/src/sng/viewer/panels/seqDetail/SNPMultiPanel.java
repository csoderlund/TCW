package sng.viewer.panels.seqDetail;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Iterator;
import java.util.Vector;

import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import util.ui.MenuMapper;
import util.ui.UIHelpers;

/*
 * This panel holds one or more SNPTable objects each of which displays the 
 * SNP vectors for a contig.  Also the panel has drop down menus for choosing 
 * how the SNP tables are sorted.  (The sort can be both column-wise and row-wise.)
 *
 */

public class SNPMultiPanel extends JPanel 
{
	public SNPMultiPanel ( MultiCtgData inTheCluster )
	{
		theCluster = inTheCluster;

		// Panel with contig views
		listSNPTables = new Vector<SNPTable> ();
		multiSNPPanel = new JPanel ( );
		multiSNPPanel.setLayout( new BoxLayout ( multiSNPPanel, BoxLayout.Y_AXIS ) );
		multiSNPPanel.setBackground( Color.WHITE );
		multiSNPPanel.add( Box.createVerticalStrut(10) );
		super.setBackground(Color.WHITE);
		createContent();				
	}
	
	private void createContent() {
		Iterator<ContigData> iterContig = theCluster.getContigIterator();
		while ( iterContig.hasNext() )
		{
			ContigData curContig = (ContigData)iterContig.next ();
			
			SNPTable curTable = new SNPTable ( curContig );
			int nTableWidth = 0;
			int nTableHeight = 0;			
			if ( curContig.getSNPCount() > 0 )
			{
				curTable.addSortChangeListener
				(	new ActionListener ()
						{
							public void actionPerformed ( ActionEvent event )
							{
								handleMixedSort ( );
							}
						}	
				);
				
				nTableWidth = (int)curTable.getPreferredSize( ).getWidth();
				nTableHeight = (int)curTable.getPreferredSize( ).getHeight();
				nTableHeight += (int) curTable.getTableHeader().getPreferredSize().getHeight();
				nTableHeight += 8;
			}
			else
				nTableHeight += 5;				
			
			// Header for table
			String str = " Contig: " + curContig.getContigID() + "\n";
			str += " Num Seqs: " + curContig.getNumSequences() + "\n";
			str += " Num SNPs: " + curContig.getSNPCount ();
			
			JTextArea text = new JTextArea ( str );
			text.setEditable( false );
			text.setFont ( curTable.getFont() );
			int nTextWidth = (int)text.getPreferredSize().getWidth() + 5;
			int nTextHeight = (int)text.getPreferredSize().getHeight();
			int nWholeWidth = Math.max( nTextWidth, nTableWidth );
			int nWholeHeight = nTextHeight + nTableHeight;
			
			text.setMaximumSize( new Dimension ( nWholeWidth, (int)text.getPreferredSize().getHeight() ) );
			
			// Combine table/header in a single panel
			JPanel tableAndHeader = new JPanel ();
			tableAndHeader.setLayout( new BoxLayout ( tableAndHeader, BoxLayout.Y_AXIS ) );
			tableAndHeader.setBackground( Color.WHITE );
			tableAndHeader.add ( text );
			if ( curContig.getSNPCount() > 0 )
			{
				tableAndHeader.add( Box.createVerticalStrut(5) );
				tableAndHeader.add( curTable.getTableHeader() );
				tableAndHeader.add( curTable );
				
				listSNPTables.add( curTable );
			}
			tableAndHeader.setMaximumSize( new Dimension ( nWholeWidth, nWholeHeight ) );
			tableAndHeader.setPreferredSize( new Dimension ( nWholeWidth, nWholeHeight ) );
			tableAndHeader.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
			
			JPanel tableHStrip = new JPanel ();
			tableHStrip.setAlignmentX ( Component.CENTER_ALIGNMENT );
			tableHStrip.setBackground( Color.WHITE );
			tableHStrip.setLayout ( new BoxLayout ( tableHStrip, BoxLayout.X_AXIS ) );
			tableHStrip.add( Box.createHorizontalStrut(10) );
			tableHStrip.add( tableAndHeader );
			tableHStrip.add( Box.createHorizontalStrut(10) );
			tableHStrip.add( Box.createHorizontalGlue() );
			tableHStrip.setPreferredSize( new Dimension ( nWholeWidth, nWholeHeight ) );
			tableHStrip.setMaximumSize( new Dimension ( Integer.MAX_VALUE, nWholeHeight ) );
			
			// Add to the main panel
			multiSNPPanel.add( tableHStrip );
			multiSNPPanel.add( Box.createVerticalStrut(10) );
			
			invalidate();
		}
		
		multiSNPPanel.add ( Box.createVerticalGlue() );
		
		// Scroll panel
		scroller = new JScrollPane ( multiSNPPanel );
		scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scroller.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		UIHelpers.setScrollIncrements( scroller );
		
		// Add to the main panel
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ) );
		add ( createToolbar () );
		add ( Box.createVerticalStrut(5) );
		add ( scroller );
		
	}
	
	private JPanel createToolbar ( )
	{		
		menuSNPSort = new JComboBox ();
		Dimension dim = new Dimension ( 300, (int)menuSNPSort.getPreferredSize().getHeight() );
		menuSNPSort.setPreferredSize( dim );
		menuSNPSort.setMaximumSize ( dim );
		menuSNPSort.addItem( new MenuMapper ( "Sort SNPs by Position", 
				SNPTable.SORT_SNPS_BY_POSITION ) );
		menuSNPSort.addItem( new MenuMapper ( "Sort SNPs by Co-segregation Group", 
				SNPTable.SORT_SNPS_BY_COSEGREGATION ) );
		menuSNPSort.setBackground(Color.WHITE);
		menuSNPSort.addActionListener
		( 		new ActionListener () 
				{
			public void	actionPerformed(ActionEvent e)
			{
				MenuMapper selection = (MenuMapper)menuSNPSort.getSelectedItem();
				changeSNPSort ( selection.asInt() );	
			}				
				}
		);		
		
		menuESTSort = new JComboBox ();
		menuESTSort.setPreferredSize( dim );
		menuESTSort.setMaximumSize ( dim );
		menuESTSort.addItem( new MenuMapper ( "Sort Seqs by Name", 
				ContigData.SORT_BY_NAME ) );
		menuESTSort.addItem( new MenuMapper ( "Sort Seqs by Left Position", 
				ContigData.SORT_BY_LEFT_POS ) );
		menuESTSort.addItem( new MenuMapper ( "Sort Seqs by Allele", 
				ContigData.SORT_BY_ALLELE ) );
		menuESTSort.setBackground(Color.WHITE);
		menuESTSort.addActionListener
		( 		new ActionListener () 
				{
			public void	actionPerformed(ActionEvent e)
			{
				MenuMapper selection = (MenuMapper)menuESTSort.getSelectedItem();
				if ( selection != mixedItem )
					changeESTSort ( selection.asInt() );
			}				
				}
		);		
		
		// Top panel with buttons and drop-down		
		JPanel topPanel = new JPanel ( );
		topPanel.setLayout( new BoxLayout ( topPanel, BoxLayout.X_AXIS ) );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( menuESTSort );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );	
		topPanel.setBackground(Color.WHITE);
		return topPanel;
	}
	
	private void changeSNPSort ( int nNewSort )
	{
		Iterator<SNPTable> iter = listSNPTables.iterator();
		while ( iter.hasNext() )
		{
			SNPTable curTable = (SNPTable)iter.next();
			curTable.changeSNPSort( nNewSort );
		}		
	}
	
	private void changeESTSort ( int nNewSort )
	{
		// Clear the mixed sort item from menu
		if ( mixedItem != null )
			menuESTSort.removeItem( mixedItem );
		mixedItem = null;
		
		Iterator<SNPTable> iter = listSNPTables.iterator();
		while ( iter.hasNext() )
		{
			SNPTable curTable = (SNPTable)iter.next();
			curTable.changeESTSort ( nNewSort );		
		}		
	}	
	
	private void handleMixedSort ( )
	{
		if ( mixedItem == null )
		{
			if ( listSNPTables.size() == 1)
				mixedItem = new MenuMapper ( "Sort by SNP Vector", Integer.MAX_VALUE );
			else
				mixedItem = new MenuMapper ( "Sort Mixed", Integer.MAX_VALUE );		
			menuESTSort.addItem( mixedItem );
			menuESTSort.setSelectedItem( mixedItem );
		}
	}
	
	private Object mixedItem = null;
	private JScrollPane scroller;
	private Vector<SNPTable> listSNPTables;
	private JPanel multiSNPPanel;
	private MultiCtgData theCluster;
	private JComboBox menuSNPSort;
	private JComboBox menuESTSort;
    private static final long serialVersionUID = 1;
}
