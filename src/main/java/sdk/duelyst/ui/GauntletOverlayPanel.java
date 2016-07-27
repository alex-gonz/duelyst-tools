package sdk.duelyst.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import sdk.duelyst.Card;
import sdk.duelyst.Faction;
import sdk.duelyst.Rating;
import sdk.duelyst.console.message.GauntletOptionsMessage;

public class GauntletOverlayPanel extends JPanel {
	private static final long serialVersionUID = 6380945472077843651L;

	private static final int CARD_WIDTH = 350;
	private static final int CARD_SYMBOLS_HEIGHT = 200;
	private static final int CARD_RATING_HEIGHT = 25;
	private static final int CARD_MARGIN = 2;

	public boolean hasCards = false;

	private JWindow window;
	private JTextPane txtNotes1, txtNotes2, txtNotes3, txtRating1, txtRating2, txtRating3;

	private int lastMousePressX = 0;
	private int lastMousePressY = 0;
	public GauntletOverlayPanel() {
		super(null);

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

		window.setSize((CARD_WIDTH * 3) - (CARD_MARGIN * 2), 500);
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
		txt.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				lastMousePressX = e.getX();
				lastMousePressY = e.getY();
			}
		});

		txt.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Point p = window.getLocation();
				window.setLocation((int) p.getX() + e.getX() - lastMousePressX, (int) p.getY() + e.getY() - lastMousePressY);
			}
		});
	}

	public void setFrameVisible(boolean visible) {
		if (window.isVisible() != visible) {
			window.setVisible(visible);
		}
	}

	public void setCards(GauntletOptionsMessage message, Faction faction, Map<Faction, Map<Integer, Collection<Rating>>> ratings) {
		setCard(message.option1, faction, txtRating1, txtNotes1, ratings);
		setCard(message.option2, faction, txtRating2, txtNotes2, ratings);
		setCard(message.option3, faction, txtRating3, txtNotes3, ratings);

		hasCards = true;
	}

	private void setCard(Card card, Faction faction, JTextPane txtRating, JTextPane txtNotes, Map<Faction, Map<Integer, Collection<Rating>>> zeldaRatings) {
		Collection<Rating> ratings = zeldaRatings.get(faction).get(card.id);

		if (ratings != null) {
			txtRating.setText(
				card.name + ": " + ratings.stream()
					.map(rating -> rating.rating)
					.distinct()
					.map(Object::toString)
					.collect(Collectors.joining(",")));

			txtNotes.setText(
				ratings.stream()
					.map(rating -> rating.notes)
					.distinct()
					.collect(Collectors.joining("\n")));
		} else {
			txtRating.setText("");
			txtNotes.setText("");
		}
	}
}