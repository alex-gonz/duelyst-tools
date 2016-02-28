package sdk.duelyst;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOImage;
import sdk.duelyst.console.DuelystConsole;
import sdk.duelyst.console.DuelystConsoleListener;
import sdk.duelyst.console.DuelystMessage;
import sdk.duelyst.console.GauntletOptionsMessage;
import sdk.duelyst.console.MessageType;
import sdk.utility.ChromeUtil;
import sdk.utility.ChromeWsUrl;
import sdk.utility.MapUtil;

public class GauntletHelper implements Runnable {
	
	public static void main(String[] args) {
		try { 
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
		
		// http://stackoverflow.com/questions/20269083/make-a-swing-thread-that-show-a-please-wait-jdialog
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
			@Override
	        protected Boolean doInBackground() {
	        	try {
	     			DuelystLibrary.load();
	     		} catch (Exception ex) {
	     		    ex.printStackTrace();
	     			JOptionPane.showMessageDialog(null, "Error loading card library: " + ex.getMessage());
	     			return false;
	     		}
	     		
	     		try {
	     			GauntletDataCyno.load();
	     		} catch (Exception ex) {
	     		    ex.printStackTrace();
	     			JOptionPane.showMessageDialog(null, "Error loading gauntlet ratings: " + ex.getMessage());
	     			return false;
	     		}
	     		
	            return true;
			}
		};
		
		final JDialog dialog = new JDialog(null, "Gauntlet Helper", ModalityType.APPLICATION_MODAL);
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
	        public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("state")) {
					if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
						dialog.dispose();
					}
				}
			}
		});
	    worker.execute();
	    
	    dialog.setResizable(false);
	    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	    try {
	        dialog.setIconImage(getIcon());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	    
		JPanel panel = new JPanel(new GridLayout(2, 1, 3, 3));
	    JProgressBar progressBar = new JProgressBar();
	    progressBar.setPreferredSize(new Dimension(250, 20));
		progressBar.setIndeterminate(true);
		panel.add(progressBar, BorderLayout.CENTER);
		panel.add(new JLabel("Downloading card data and gauntlet tier list...", JLabel.CENTER));
		
		dialog.add(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		try {
			if (worker.get()) {
				EventQueue.invokeLater(new GauntletHelper());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void run() {
        new ControlPanel();
    }

    // Bunch of BS to set an icon
	public static Image getIcon() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream input = classLoader.getResourceAsStream("icon.ico");
		List<ICOImage> images = ICODecoder.readExt(input);
		return images.get(4).getImage(); // Second smallest, looks alright in both window and taskbar
	}
}

class ControlPanel extends JPanel implements ActionListener, DuelystConsoleListener {
	private static final long serialVersionUID = -7966114779784215280L;
	
	private Timer timer = new Timer();
	
	private List<ChromeWsUrl> wsUrls = new ArrayList<ChromeWsUrl>();
	private ChromeWsUrl selectedWsUrl = null;
	public Faction selectedFaction = Faction.LYONAR;
	
	private DuelystConsole duelyst;
	private GauntletOptionsMessage lastGauntletOptions;
	
	private JButton btnChrome, btnConnect;
	private JComboBox<String> cmbTabs, cmbFactions;
	private JCheckBox chkHideOverlay;
	private JLabel lblTabs, lblFactions;
	
	private OverlayPanel overlay;

	public ControlPanel() {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(3, 3, 0, 3));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 3, 3, 3);
        
        // http://stackoverflow.com/q/8669350
        final JLabel lblCyno = new JLabel("<HTML><B><U>Cynosure's Gauntlet Tier List</U></B></HTML>", JLabel.CENTER);
        lblCyno.setForeground(Color.BLUE);
        lblCyno.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblCyno.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		if (e.getClickCount() > 0) {
        			if (Desktop.isDesktopSupported()) {
        				Desktop desktop = Desktop.getDesktop();
        				
        				try {
        					URI uri = new URI(GauntletDataCyno.DEFAULT_URL);
        					desktop.browse(uri);
        				} catch (IOException ex) {
        					ex.printStackTrace();
        				} catch (URISyntaxException ex) {
        					ex.printStackTrace();
        				}
        			}
        		}
        	}

        	@Override
        	public void mouseEntered(MouseEvent e) {
                lblCyno.setForeground(Color.RED);
        	}

        	@Override
        	public void mouseExited(MouseEvent e) {
                lblCyno.setForeground(Color.BLUE);
        	}
        });

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(lblCyno, c);
        
        btnChrome = new JButton("Launch Chrome");
        btnChrome.setActionCommand("launchChrome");
        btnChrome.addActionListener(this);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        add(btnChrome, c);
        
        btnConnect = new JButton("Connect");
        btnConnect.setActionCommand("connect");
        btnConnect.addActionListener(this);
        btnConnect.setEnabled(false);

        c.gridx = 1;
        c.gridy = 1;
        add(btnConnect, c);
        
        lblTabs = new JLabel("Chrome Tabs:");
        lblTabs.setHorizontalAlignment(JLabel.RIGHT);
        
        c.gridx = 0;
        c.gridy = 2;
        add(lblTabs, c);
        
        cmbTabs = new JComboBox<String>();
        cmbTabs.setActionCommand("tabSelected");
        cmbTabs.addActionListener(this);
        cmbTabs.setEnabled(false);

        c.gridx = 1;
        c.gridy = 2;
        add(cmbTabs, c);
        
        lblFactions = new JLabel("Faction:");
        lblFactions.setHorizontalAlignment(JLabel.RIGHT);
        
        c.gridx = 0;
        c.gridy = 3;
        add(lblFactions, c);
        
        cmbFactions = new JComboBox<String>();
        cmbFactions.setActionCommand("factionSelected");
        cmbFactions.addActionListener(this);
        cmbFactions.setEnabled(false);

        c.gridx = 1;
        c.gridy = 3;
        add(cmbFactions, c);
        
        chkHideOverlay = new JCheckBox("Hide Overlay");
        chkHideOverlay.setActionCommand("hideOverlay");
        chkHideOverlay.addActionListener(this);
        chkHideOverlay.setEnabled(false);
        
        @SuppressWarnings("unchecked")
		Map.Entry<Faction, Integer>[] factions = new Map.Entry[6];
        MapUtil.sortByValueDesc(GauntletDataCyno.factions).entrySet().toArray(factions);
        for (int i = 0; i < factions.length; i++) {
        	JLabel lblFaction = new JLabel(factions[i].getKey().toString() + ": " + factions[i].getValue());

        	c.gridx = i / 3;
        	c.gridy = 4 + (i % 3);
        	add(lblFaction, c);
        }

        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        add(chkHideOverlay, c);
        
        JFrame frame = new JFrame();
        frame.setTitle("Gauntlet Helper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        
		try {
	        frame.setIconImage(GauntletHelper.getIcon());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
        
        frame.add(this);
        frame.pack();
        
        frame.setMinimumSize(new Dimension(225, 0));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
            	if (duelyst != null)
            		duelyst.disconnect();
            }
        });
        
        cmbFactions.addItem(Faction.LYONAR.toString());
        cmbFactions.addItem(Faction.SONGHAI.toString());
        cmbFactions.addItem(Faction.VETRUVIAN.toString());
        cmbFactions.addItem(Faction.ABYSSIAN.toString());
        cmbFactions.addItem(Faction.MAGMAR.toString());
        cmbFactions.addItem(Faction.VANAR.toString());
        
        cmbFactions.setSelectedIndex(0);
        
        overlay = new OverlayPanel(this);
        
        timer.schedule(new TimerTask() {
        	@Override
        	public void run() {
        		refreshTabs();
        	}
        }, 1000, 1000);
    }
	
	private void updateEnables() {
		btnChrome.setEnabled(!isConnected() && wsUrls.isEmpty());
		cmbTabs.setEnabled(!isConnected() && !wsUrls.isEmpty());
		btnConnect.setEnabled(!isConnected() && !wsUrls.isEmpty() && selectedWsUrl != null);
		cmbFactions.setEnabled(isConnected());
		chkHideOverlay.setEnabled(isConnected());
	}
	
	private void updateOverlayVisible() {
		overlay.setFrameVisible(isConnected() && !chkHideOverlay.isSelected() && overlay.hasCards);
	}
	
	private void refreshTabs() {
		if (!isConnected()) {
			
			if (wsUrls.size() == 1 && (wsUrls.get(0).title.equals("DUELYST") || wsUrls.get(0).title.equals("beta.duelyst.com"))) {
				onAction("connect");
			} else {
				try {
					List<ChromeWsUrl> newUrls = ChromeUtil.getWsUrl(ChromeUtil.getJsonResponse());
					
					for (int i = 0; i < newUrls.size(); i++) {
						ChromeWsUrl url = newUrls.get(i);
						
						if (!containsUrl(wsUrls, url) && !url.title.isEmpty()) {
							wsUrls.add(url);
							cmbTabs.addItem(url.title);
						}
					}
					
					for (int i = 0; i < wsUrls.size(); i++) {
						ChromeWsUrl url = wsUrls.get(i);
						
						if (!containsUrl(newUrls, url)) {
							wsUrls.remove(i);
							cmbTabs.removeItemAt(i);
							
							i--;
						}
					}
				} catch (ConnectException ex) {
					cmbTabs.removeAllItems();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			    
			    updateEnables();
			}
		}
	}
	
	private boolean containsUrl(List<ChromeWsUrl> urls, ChromeWsUrl url) {
		for (int i = 0; i < urls.size(); i++) {
			if (urls.get(i).id.equals(url.id) && urls.get(i).title.equals(url.title)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isConnected() {
		return duelyst != null && duelyst.isOpen();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		onAction(e.getActionCommand());
	}

	private void onAction(String action) {
		try {
		    if ("launchChrome".equals(action)) {
		    	String path = "";
		    	
		    	File settings = new File("gauntlet-helper.txt");
		    	if (settings.exists()) {
		    		BufferedReader reader = new BufferedReader(new FileReader(settings));
		    	    path = reader.readLine();
		    	    reader.close();
		    	}
		    	
		    	path = ChromeUtil.getChromePath(path);
		    	
		    	if (path.equals("")) {
		    		JFileChooser chooser = new JFileChooser();
		    		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
		    			path = chooser.getSelectedFile().getAbsolutePath();
		    		}
		    	}
		    	
		    	if (!path.equals("")) {
		    		BufferedWriter writer = new BufferedWriter(new FileWriter(settings));
		    	    writer.write(path);
		    	    writer.close();
		    		
			    	DuelystConsole.launchDebug(path);
		    	}
		    }
		    else if ("tabSelected".equals(action)) {
		    	if (cmbTabs.getSelectedIndex() != -1) {
		    		if (cmbTabs.getSelectedIndex() < wsUrls.size()) {
				    	selectedWsUrl = wsUrls.get(cmbTabs.getSelectedIndex());
		    		} else {
		    			// Handle weirdness when removing the selected item, have to set the selectedWsUrl because this doesn't seem to fire any event
		    			// TODO This may still not work in cases with a lot of tabs, but we'll see
		    			int selectedIndex = wsUrls.size() > 0 ? wsUrls.size() - 1 : -1;
		    			cmbTabs.setSelectedIndex(selectedIndex);
		    			
		    			if (selectedIndex != -1) {
			    			selectedWsUrl = wsUrls.get(selectedIndex);
		    			} else {
		    				selectedWsUrl = null;
		    			}
		    		}
		    	} else {
		    		selectedWsUrl = null;
		    	}
		    }
		    else if ("connect".equals(action)) {
		    	duelyst = new DuelystConsole();
		    	duelyst.addListener(this);
		    	duelyst.connect(selectedWsUrl.url);
		    	
		    	updateOverlayVisible();
		    }
		    else if ("factionSelected".equals(action)) {
		    	selectedFaction = Faction.valueOf((String)cmbFactions.getSelectedItem());
		    	
		    	if (lastGauntletOptions != null) {
		    		overlay.setCards(lastGauntletOptions);
					updateOverlayVisible();
		    	}
		    }
		    else if ("hideOverlay".equals(action)) {
			    updateOverlayVisible();
		    }
		    
		    updateEnables();
		} catch (Exception ex) {
		    ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error performing action " + action + ": " + ex.getMessage());
		}
	}
	
	@Override
	public void onMessage(DuelystMessage message) {
		if (message.type == MessageType.GAUNTLET_OPTIONS) {
			overlay.setCards((GauntletOptionsMessage)message);
			updateOverlayVisible();
			
			lastGauntletOptions = (GauntletOptionsMessage)message;
		}
		else if (message.type == MessageType.EXIT) {
			duelyst.disconnect();
			duelyst = null;
			
			wsUrls.clear();
			cmbTabs.removeAllItems();
			
			updateEnables();
			updateOverlayVisible();
		}
	}
}

class OverlayPanel extends JPanel {
	private static final long serialVersionUID = 6380945472077843651L;
	
	private static final int CARD_WIDTH = 260;
	private static final int CARD_SYMBOLS_HEIGHT = 90;
	private static final int CARD_RATING_HEIGHT = 25;
	private static final int CARD_MARGIN = 2;
	
	public boolean hasCards = false;

	private JWindow window;
	private JTextPane txtNotes1, txtNotes2, txtNotes3, txtRating1, txtRating2, txtRating3;
	
	private ControlPanel control;
	
	public OverlayPanel(ControlPanel control) {
		super(null);
		
		this.control = control;
        
        txtNotes1 = new JTextPane();
        txtRating1 = new JTextPane();
        setProperties(txtNotes1, txtRating1, 0);
        
        add(txtNotes1);
        add(txtRating1);
        
        txtNotes2 = new JTextPane();
        txtRating2 = new JTextPane();
        setProperties(txtNotes2, txtRating2, 1);

        add(txtNotes2);
        add(txtRating2);
        
        txtNotes3 = new JTextPane();
        txtRating3 = new JTextPane();
        setProperties(txtNotes3, txtRating3, 2);
        
        add(txtNotes3);
        add(txtRating3);
        
        window = new JWindow();
        window.setBackground(new Color(0, 0, 0, 0));
        window.setAlwaysOnTop(true);
        window.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);

        this.setBackground(new Color(0, 0, 0, 0));
        window.add(this);
        
        window.setSize((CARD_WIDTH * 3) - (CARD_MARGIN * 2), 440);
        window.setLocationRelativeTo(null);
    }
	
	private void setProperties(JTextPane txtNotes, JTextPane txtRating, int index) {
		setProperties(txtNotes);
		txtNotes.setBounds(CARD_WIDTH * index, 0, CARD_WIDTH - CARD_MARGIN, CARD_SYMBOLS_HEIGHT);
		
		txtNotes.setFont(new Font(txtNotes.getFont().getFontName(), Font.PLAIN, 12));
		
		setProperties(txtRating);
		txtRating.setBounds(CARD_WIDTH * index, CARD_SYMBOLS_HEIGHT, CARD_WIDTH - CARD_MARGIN, CARD_RATING_HEIGHT);
		
		txtRating.setFont(new Font(txtRating.getFont().getFontName(), Font.BOLD, 16));
		
		StyledDocument doc = txtRating.getStyledDocument();
		SimpleAttributeSet center = new SimpleAttributeSet();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		doc.setParagraphAttributes(0, doc.getLength(), center, false);
	}

	private void setProperties(JTextPane txt) {
		txt.setEditable(false);
		txt.setFocusable(false);
		txt.setBackground(Color.BLACK);
		txt.setForeground(Color.WHITE);
	}
	
	public void setFrameVisible(boolean visible) {
		if (window.isVisible() != visible) {
			window.setVisible(visible);
		}
	}
	
	public void setCards(GauntletOptionsMessage message) {
		setCard(message.option1, txtRating1, txtNotes1);
		setCard(message.option2, txtRating2, txtNotes2);
		setCard(message.option3, txtRating3, txtNotes3);
		
		hasCards = true;
	}
	
	private void setCard(Card card, JTextPane txtRating, JTextPane txtNotes) {
		Rating rating = GauntletDataCyno.ratings.get(control.selectedFaction).get(card.id);
		
		if (rating != null) {
			txtRating.setText(card.name + ": " + rating.rating);
			txtNotes.setText(rating.notes);
		} else {
			txtRating.setText("");
			txtNotes.setText("");
		}
	}
}