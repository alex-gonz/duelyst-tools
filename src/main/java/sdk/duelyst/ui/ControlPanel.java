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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdk.duelyst.*;
import sdk.duelyst.console.DuelystConsole;
import sdk.duelyst.console.DuelystConsoleListener;
import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.GauntletOptionsMessage;
import sdk.duelyst.console.message.MessageType;
import sdk.utility.ChromeUtil;
import sdk.utility.ChromeWsUrl;

import com.neovisionaries.ws.client.WebSocketException;

public class ControlPanel extends JPanel implements ActionListener, DuelystConsoleListener {
	private static final long serialVersionUID = -7966114779784215280L;
	public static final String PROPERTIES_FILE_NAME = "duelyst-tools.properties";
	private final Map<Faction, Map<Integer, Collection<Rating>>> ratings;

	private Properties properties = new Properties();
	private String chromePath = "";

	private Timer timer = new Timer();

	private List<ChromeWsUrl> wsUrls = new ArrayList<ChromeWsUrl>();
	private ChromeWsUrl selectedWsUrl = null;
	public Faction selectedFaction = Faction.LYONAR;

	private DuelystConsole duelyst;
	private GauntletOptionsMessage lastGauntletOptions;

	private JFrame frame;
	private JButton btnChrome, btnConnect;
	private JComboBox<String> cmbTabs, cmbFactions;
	private JCheckBox chkShowTracker, chkExpandTracker, chkShowOverlay;
	private JLabel lblGauntlet, lblTracker;
	private JSeparator sepGauntlet, sepTracker;

	private GauntletOverlayPanel overlay;
	private DeckTracker tracker;

	private final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

	public ControlPanel(Map<Faction, Map<Integer, Collection<Rating>>> ratings) throws IOException {
		super(new GridBagLayout());

		this.ratings = ratings;

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

		chkExpandTracker = new JCheckBox("Expanded");
		chkExpandTracker.setActionCommand("expandTracker");
		chkExpandTracker.addActionListener(this);
		chkExpandTracker.setEnabled(false);

		c.gridx = 2;
		c.gridwidth = 1;
		add(chkExpandTracker, c);

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
		final JLabel lblZelda = new JLabel("<HTML><B><U>zelda6525's Gauntlet Rankings</U></B></HTML>", JLabel.CENTER);
		lblZelda.setForeground(Color.BLUE);
		lblZelda.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lblZelda.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 0) {
					if (Desktop.isDesktopSupported()) {
						Desktop desktop = Desktop.getDesktop();

						try {
							URI uri = new URI(GauntletDataZelda.ZELDA_GUIDE_URL);
							desktop.browse(uri);
						} catch (IOException | URISyntaxException ex) {
							logger.error(ex.toString());
						}
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				lblZelda.setForeground(Color.RED);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				lblZelda.setForeground(Color.BLUE);
			}
		});

		c.gridx = 0;
		c.gridwidth = 3;
		c.gridy++;
		add(lblZelda, c);

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


		frame = new JFrame();
		frame.setTitle("Duelyst Tools");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setAlwaysOnTop(true);
		frame.setResizable(false);

		try {
			frame.setIconImage(DuelystTools.getIcon());
		} catch (IOException e) {
			logger.error("Error setting icon", e);
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

		try {
			loadProperties();
		} catch (URISyntaxException e) {
			logger.error("Error loading properties file: " + PROPERTIES_FILE_NAME, e);
		}
	}

	public void close() {
		frame.dispose();
	}

	private void loadProperties() throws FileNotFoundException, IOException, URISyntaxException {
		File propFile = getPropFile();
		if (!propFile.exists()) {
			saveProperties();
		} else {
			try (FileInputStream stream = new FileInputStream(propFile)) {
				properties.load(stream);

				chromePath = properties.getProperty("chrome-path");
				chkShowTracker.setSelected(Boolean.parseBoolean(properties.getProperty("tracker-shown")));
				chkExpandTracker.setSelected(Boolean.parseBoolean(properties.getProperty("tracker-expand")));

				onAction("showTracker");
				onAction("expandTracker");
			}
		}
	}

	private void saveProperties() throws URISyntaxException, IOException {
		File propFile = getPropFile();
		try (FileOutputStream stream = new FileOutputStream(propFile)) {
			properties.setProperty("chrome-path", chromePath);
			properties.setProperty("tracker-shown", Boolean.toString(chkShowTracker.isSelected()));
			properties.setProperty("tracker-expand", Boolean.toString(chkExpandTracker.isSelected()));

			properties.store(stream, null);
		}
	}

	private File getPropFile() throws URISyntaxException {
		return new File(System.getProperty("user.dir"), PROPERTIES_FILE_NAME);
	}

	private void updateEnables() {
		btnChrome.setEnabled(!isConnected() && wsUrls.isEmpty());
		cmbTabs.setEnabled(!isConnected() && !wsUrls.isEmpty());
		btnConnect.setEnabled(!isConnected() && !wsUrls.isEmpty() && selectedWsUrl != null);

		chkShowTracker.setEnabled(isConnected());
		chkExpandTracker.setEnabled(isConnected());

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

					frame.pack();
				} catch (ConnectException e) {
					cmbTabs.removeAllItems();
				} catch (Exception e) {
					logger.error("Error refreshing tabs", e);
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
				chromePath = ChromeUtil.getChromePath(chromePath);

				if (chromePath.equals("")) {
					JFileChooser chooser = new JFileChooser();
					if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
						chromePath = chooser.getSelectedFile().getAbsolutePath();
					}
				}

				if (!chromePath.equals("")) {
					saveProperties();
					DuelystConsole.launchDebug(chromePath);
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
					overlay.setCards(lastGauntletOptions, selectedFaction, ratings);
				}
			}
			else if ("showTracker".equals(action)) {
				updateTrackerVisible();
				saveProperties();
			}
			else if ("expandTracker".equals(action)) {
				tracker.setCompact(!chkExpandTracker.isSelected());
				saveProperties();
			}
			else if ("showOverlay".equals(action)) {
				updateOverlayVisible();
			}

			updateEnables();
		} catch (WebSocketException e) {
			logger.error("Error with websocket when connecting to chrome", e);
			timer.cancel();
			JOptionPane.showMessageDialog(this, "Error connecting to chrome: " + e.getMessage() + System.lineSeparator()
							+ "Program will have to be restarted to refresh correctly.");
		} catch (Exception e) {
			logger.error("Error performing action: " + action, e);
			JOptionPane.showMessageDialog(this, "Error performing action " + action + ": " + e.getMessage());
		}
	}

	@Override
	public void onMessage(DuelystMessage message) {
		if (message.type == MessageType.GAUNTLET_OPTIONS) {
			overlay.setCards((GauntletOptionsMessage)message, selectedFaction, ratings);
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