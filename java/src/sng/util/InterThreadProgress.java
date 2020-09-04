package sng.util;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.*;


import sng.dataholders.MultiCtgData;
import sng.viewer.STCWFrame;
import util.ui.UIHelpers;

public class InterThreadProgress
{
    public interface WorkerThread 
    {
        public void run () throws Throwable;
    }
    
    public InterThreadProgress ( STCWFrame inFrame )
    {
        theFrame = inFrame;
        progressBar = new JProgressBar();
		progressBar.setMaximumSize(new Dimension(400, 
				(int) progressBar.getPreferredSize().getHeight()));
		progressBar.setStringPainted(true);
		progressBar.setString("");
		progressBar.setIndeterminate(true);

		theLabel = new JLabel("   ");
		theLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		btnClose = new JButton("Cancel");
		btnClose.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleCloseButton();
			}
		});
        
        progressPanel = new Tab(null, null);
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		progressPanel.add(Box.createHorizontalGlue());
		progressPanel.add(Box.createVerticalGlue());
		progressPanel.add(theLabel);
		progressPanel.add(Box.createVerticalStrut(50));
		progressPanel.add(progressBar);
		
		progressPanel.add(Box.createVerticalGlue());
    }
    
    public Tab/*JPanel*/ getProgressPanel ( ) { return progressPanel; }
    
    public Tab getCancelPanel ( ) { return cancelPanel; } 
    
    public boolean wasCanceled ( ) { return bCanceled; }
    
    public void swapInTabAndStartThread (Tab oldTab,
                                       	String strTabName, 
                                        WorkerThread theWorker )
    {
        // Add the tab to the frame
        if (oldTab != null) {
			theFrame.swapInTab(oldTab, null, progressPanel);
			progressPanel.setTitle(strTabName);
		}
        else {
 
        	theFrame.tabbedPane.addTab( strTabName, progressPanel );
        }   
        // Startup the thread
        runWorkerThread ( theWorker );
    }
    
    public void runWorkerThread ( WorkerThread theWorker )
    {
        // Create the second thread
        theWorkerThread = theWorker;
        theThread = new Thread(new Runnable() {
			public void run() {
				try {
					theWorkerThread.run();
				} catch (Throwable err) {
					//err.printStackTrace();
					swapToExceptionTab(null, err);
				}
				theFrame.updateStatus(0); 
			}
		});
        
        // Startup the second thread
        theFrame.updateStatus(1); 
        theThread.setPriority(Thread.MIN_PRIORITY); // make queries run lower priority than GUI
        theThread.start();
    }
    
    public void swapToExceptionTab ( String strMsg, Throwable err )
    {
        final Throwable holdErr = err;
        
        EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (isRunInTab())
					theFrame.swapToExceptionTab(progressPanel, holdErr);
				else
					theFrame.swapToExceptionTab(null,  holdErr);
			}
		});
    }
    
    public void setProgressLabel ( String strLabel )
    {
        final String holdLabel = strLabel;
        
        EventQueue.invokeLater(new Runnable() {
			public void run() {
				theLabel.setText(holdLabel);
			}
		});
    }
    
    public void setProgressValue ( int nValue )
    {
        final int holdVal = nValue;
        
        EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(holdVal);
			}
		});
    }

    public void setProgressString ( String str )
    {
        final String strProgMsg = str;
        
        EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setStringPainted(true);
				progressBar.setString(strProgMsg);
			}
		});
    }
    
    public void swapOutProgress(final Tab newTab) {
		swapOutProgress(progressPanel.getTitle(), newTab); 
	}
    
    public void swapOutProgress(final String strTabTitle, final Tab newTab) {
		if (bCanceled)
			return;

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				theFrame.swapInTab(progressPanel, strTabTitle, newTab);
				progressPanel = null;
			}
		});
	}    
    
    public void swapOutProgressWithCAP3Tab(MultiCtgData theCluster,
    		int nRecordNum, String strTabTitle)
    {  	
		if (bCanceled) return;

		final MultiCtgData holdCluster = theCluster;
		final int holdRecordNum = nRecordNum; 
		final String holdTabTitle = strTabTitle;

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				theFrame.swapInCAP3Tab(
						progressPanel, 
						holdCluster,
						holdRecordNum, 
						holdTabTitle);
				progressPanel = null;
			}
		});
	}
    
    private void handleCloseButton() {
		// Kill the thread
		bCanceled = true;
		theThread.interrupt();
		theFrame.updateStatus(0); 
		
		if (isRunInTab()) {
			cancelPanel = new CenteredMessageTab("Canceled");
			theFrame.swapInTab(progressPanel, "Canceled", cancelPanel); 
		}
	}
    
    private boolean isRunInTab() {
		Frame parent = UIHelpers.findParentFrame(progressPanel);
		return parent == theFrame;
	}
    
    WorkerThread theWorkerThread = null;
	private Thread theThread = null;
	private STCWFrame theFrame = null;
	private Tab progressPanel = null; 
	private Tab cancelPanel = null; 
	private JButton btnClose = null;
	private JLabel theLabel = null;
	private JProgressBar progressBar = null;
	private boolean bCanceled = false;
}
