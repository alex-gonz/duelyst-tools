package sdk.duelyst;

import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdk.duelyst.ui.ControlPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DuelystTools implements Runnable {
	public static Path imageFolder = Paths.get("images");
	public static Map<Faction, Map<Integer, Collection<Rating>>> ratings;

	private static final Logger logger = LoggerFactory.getLogger(DuelystTools.class);

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			logger.error("Error setting up UIManager", e);
		}

		try {
			Files.createDirectory(imageFolder);
		} catch (FileAlreadyExistsException ignored) {
		} catch (IOException e) {
			logger.error("Error creating file directory", e);
		}

		// http://stackoverflow.com/questions/20269083/make-a-swing-thread-that-show-a-please-wait-jdialog
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() {
				try {
					DuelystLibrary.load(imageFolder);
				} catch (Exception e) {
					logger.error("Error loading card library", e);
					JOptionPane.showMessageDialog(null, "Error loading card library: " + e.getMessage());
					return false;
				}

				try {
					ratings = GauntletDataZelda.load(DuelystLibrary.cardsById.values());
				} catch (Exception e) {
					logger.error("Error loading gauntlet ratings", e);
					JOptionPane.showMessageDialog(null, "Error loading gauntlet ratings: " + e.getMessage());
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
		} catch (IOException e) {
			logger.error("Error setting icon of duelyst helper", e);
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
				EventQueue.invokeLater(new DuelystTools());
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Error instantiating Duelyst Tools", e);
		}
	}

	@Override
	public void run() {
		try {
			new ControlPanel(ratings);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error creating GUI: " + e.getMessage());
			System.exit(1);
		}
	}

	// Bunch of BS to set an icon
	public static Image getIcon() throws IOException {
		List<ICOImage> images = ICODecoder.readExt(imageFolder.resolve("icon.ico").toFile());
		return images.get(4).getImage(); // Second smallest, looks alright in both window and taskbar
	}
}