package sdk.duelyst.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

import sdk.duelyst.DeckTracker;
import sdk.duelyst.DuelystTools;
import sdk.duelyst.Faction;
import sdk.duelyst.GauntletDataCyno;
import sdk.duelyst.console.DuelystConsole;
import sdk.duelyst.console.DuelystConsoleListener;
import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.GauntletOptionsMessage;
import sdk.duelyst.console.message.MessageType;
import sdk.utility.ChromeUtil;
import sdk.utility.ChromeWsUrl;
import sdk.utility.MapUtil;

import com.neovisionaries.ws.client.WebSocketException;

public class ControlPanel extends JPanel implements ActionListener, DuelystConsoleListener {
	private static final long serialVersionUID = -7966114779784215280L;
	
	private Timer timer = new Timer();
	
	private List<ChromeWsUrl> wsUrls = new ArrayList<ChromeWsUrl>();
	private ChromeWsUrl selectedWsUrl = null;
	public Faction selectedFaction = Faction.LYONAR;
	
	private DuelystConsole duelyst;
	private GauntletOptionsMessage lastGauntletOptions;
	
	private JFrame frame;	
	private JButton btnChrome, btnConnect;
	private JComboBox<String> cmbTabs, cmbFactions;
	private JCheckBox chkShowTracker, chkCompactTracker, chkShowOverlay;
	private JLabel lblGauntlet, lblTracker;
	private JSeparator sepGauntlet, sepTracker;
	
	private GauntletOverlayPanel overlay;
	private DeckTracker tracker;

	public ControlPanel() throws IOException {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(3, 3, 0, 3));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);
        c.gridy = 0;
        
        btnChrome = new JButton("Launch Chrome");
        btnChrome.setActionCommand("launchChrome");
        btnChrome.addActionListener(this);

        c.gridx = 0;
        c.gridwidth = 3;
        add(btnChrome, c);
        
        cmbTabs = new JComboBox<String>();
        cmbTabs.setActionCommand("tabSelected");
        cmbTabs.addActionListener(this);
        cmbTabs.setEnabled(false);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        add(cmbTabs, c);
        
        btnConnect = new JButton("Connect");
        btnConnect.setActionCommand("connect");
        btnConnect.addActionListener(this);
        btnConnect.setEnabled(false);

        c.gridx = 2;
        c.gridwidth = 1;
        add(btnConnect, c);
        
        // Extra spaces to handle separator length, intersects with Gauntlet Helper label otherwise
        lblTracker = new JLabel("Deck Tracker   ");
        
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        add(lblTracker, c);
        
        sepTracker = new JSeparator();
        
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        add(sepTracker, c);
        c.weightx = 0;
        
        chkShowTracker = new JCheckBox("Show Tracker");
        chkShowTracker.setActionCommand("showTracker");
        chkShowTracker.addActionListener(this);
        chkShowTracker.setEnabled(false);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        add(chkShowTracker, c);
        
        chkCompactTracker = new JCheckBox("Compact");
        chkCompactTracker.setActionCommand("compactTracker");
        chkCompactTracker.addActionListener(this);
        chkCompactTracker.setEnabled(false);

        c.gridx = 2;
        c.gridwidth = 1;
        add(chkCompactTracker, c);
        
        lblGauntlet = new JLabel("Gauntlet Helper");
        
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        add(lblGauntlet, c);
        
        sepGauntlet = new JSeparator();
        
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        add(sepGauntlet, c);
        c.weightx = 0;
        
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
        c.gridwidth = 3;
        c.gridy++;
        add(lblCyno, c);
        
        chkShowOverlay = new JCheckBox("Show Overlay");
        chkShowOverlay.setActionCommand("showOverlay");
        chkShowOverlay.addActionListener(this);
        chkShowOverlay.setEnabled(false);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        add(chkShowOverlay, c);
        
        cmbFactions = new JComboBox<String>();
        cmbFactions.setActionCommand("factionSelected");
        cmbFactions.addActionListener(this);
        cmbFactions.setEnabled(false);

        c.gridx = 2;
        c.gridwidth = 1;
        add(cmbFactions, c);
        
        @SuppressWarnings("unchecked")
		Map.Entry<Faction, Integer>[] factions = new Map.Entry[6];
        MapUtil.sortByValueDesc(GauntletDataCyno.factions).entrySet().toArray(factions);
        for (int i = 0; i < factions.length; i++) {
        	JLabel lblFaction = new JLabel(factions[i].getKey().toString() + ": " + factions[i].getValue());

        	if (i == 3) {
        		c.gridy -= 3;
        	}
        	
        	c.gridx = i < 3 ? 0 : 2;
            c.gridwidth = i < 3 ? 2 : 1;
        	c.gridy++;
        	add(lblFaction, c);
        }
        
        frame = new JFrame();
        frame.setTitle("Duelyst Tools");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        
		try {
	        frame.setIconImage(DuelystTools.getIcon());
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        frame.add(this);
        frame.pack();

        overlay = new GauntletOverlayPanel();
        try {
	        tracker = new DeckTracker();
        } catch (IOException e) {
        	throw e;
        }
        
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
        
        timer.schedule(new TimerTask() {
        	@Override
        	public void run() {
        		refreshTabs();
        	}
        }, 1000, 1000);
    }
	
	public void close() {
		frame.dispose();
	}
	
	private void updateEnables() {
		btnChrome.setEnabled(!isConnected() && wsUrls.isEmpty());
		cmbTabs.setEnabled(!isConnected() && !wsUrls.isEmpty());
		btnConnect.setEnabled(!isConnected() && !wsUrls.isEmpty() && selectedWsUrl != null);
		
		chkShowTracker.setEnabled(isConnected());
		chkCompactTracker.setEnabled(isConnected());
		
		cmbFactions.setEnabled(isConnected());
		chkShowOverlay.setEnabled(isConnected());
	}
	
	private void updateTrackerVisible() {
		tracker.setVisible(isConnected() && chkShowTracker.isSelected());
	}
	
	private void updateOverlayVisible() {
		overlay.setFrameVisible(isConnected() && chkShowOverlay.isSelected());
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
				} catch (ConnectException e) {
					cmbTabs.removeAllItems();
				} catch (Exception e) {
					e.printStackTrace();
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
		    	tracker.addListener(duelyst);
		    	duelyst.connect(selectedWsUrl.url);
		    	
		    	updateTrackerVisible();
		    	updateOverlayVisible();
		    }
		    else if ("factionSelected".equals(action)) {
		    	selectedFaction = Faction.valueOf((String)cmbFactions.getSelectedItem());
		    	
		    	if (lastGauntletOptions != null) {
		    		overlay.setCards(lastGauntletOptions, selectedFaction);
		    	}
		    }
		    else if ("showTracker".equals(action)) {
			    updateTrackerVisible();
		    }
		    else if ("compactTracker".equals(action)) {
			    tracker.setCompact(chkCompactTracker.isSelected());
		    }
		    else if ("showOverlay".equals(action)) {
			    updateOverlayVisible();
		    }
		    
		    updateEnables();
		} catch (WebSocketException e) {
		    e.printStackTrace();
		    timer.cancel();
			JOptionPane.showMessageDialog(this, "Error connecting to chrome: " + e.getMessage() + System.lineSeparator()
			+ "Program will have to be restarted to refresh correctly.");
		} catch (Exception e) {
		    e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error performing action " + action + ": " + e.getMessage());
		}
	}
	
	@Override
	public void onMessage(DuelystMessage message) {
		if (message.type == MessageType.GAUNTLET_OPTIONS) {
			overlay.setCards((GauntletOptionsMessage)message, selectedFaction);
			lastGauntletOptions = (GauntletOptionsMessage)message;
		}
		else if (message.type == MessageType.EXIT) {
			duelyst.disconnect();
			duelyst = null;
			
			wsUrls.clear();
			cmbTabs.removeAllItems();
			
			updateEnables();

	    	updateTrackerVisible();
	    	updateOverlayVisible();
		}
	}
}