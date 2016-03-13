package sdk.duelyst.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JWindow;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import sdk.duelyst.Card;
import sdk.duelyst.Player;
import sdk.duelyst.console.DuelystConsole;

public class DeckTrackerPanel extends JPanel {
	private static final long serialVersionUID = 452902188644703086L;
	
	private final JWindow window;
	private final PlayerPanel playerPanel;
    private final CardPanel[] cardPanels = new CardPanel[DuelystConsole.DECK_SIZE];
	
	public boolean empty = true;
	
	public DeckTrackerPanel() throws IOException {
		super(null);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        window = new JWindow();

		playerPanel = new PlayerPanel(window);
		add(playerPanel);

		for (int i = 0; i < cardPanels.length; i++) {
			CardPanel panel = new CardPanel(window);
			cardPanels[i] = panel;
			add(panel);
		}
        
        window.setBackground(new Color(0, 0, 0, 0));
        window.setAlwaysOnTop(true);

        this.setBackground(new Color(0, 0, 0, 0));
        window.add(this);
		window.pack();
        window.setLocationRelativeTo(null);
	}

	public void setWindowVisible(boolean visible) {
		window.setVisible(visible);
	}
	
	public void setCompact(boolean compact) {
		for (CardPanel cardPanel : cardPanels) {
			cardPanel.setCompact(compact);
		}
		window.pack();
	}

	public void update(Player player) {
		Collections.sort(player.deck, new CardComparator());
		
		Map<Card, Integer> deckMap = new HashMap<Card, Integer>();
		for (Card card : player.deck) {
			int count = deckMap.containsKey(card) ? deckMap.get(card) : 0;
			deckMap.put(card, count + 1);
		}
		
		playerPanel.setPlayer(player);
		setDeck(player.deck, deckMap);
	}
	
	public void clear() {
		playerPanel.setPlayer(null);
		setDeck(null, null);
		
		empty = true;
	}
	
	public void close() {
		window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
	}
	
	private void setDeck(List<Card> deck, Map<Card, Integer> deckMap) {
		int i = 0;
		
		if (deck != null) {
			List<Card> usedCards = new ArrayList<Card>();
			for (Card card : deck) {
				if (!usedCards.contains(card)) {
					usedCards.add(card);
					
					cardPanels[i].setCard(card, deckMap.get(card));
					cardPanels[i].setVisible(true);
					i++;
				}
			}
		
			while (i < cardPanels.length) {
				cardPanels[i].setCard(null, 1);
				cardPanels[i].setVisible(false);
				i++;
			}
		} else {
			for (int j = 0; j < cardPanels.length; j++) {
				cardPanels[j].setCard(null, 1);
				cardPanels[j].setVisible(j < 13);
			}
		}
		
		window.pack();
	}
}

class DraggablePanel extends JPanel {
	private static final long serialVersionUID = 3674652881669357163L;
	
	public static final int WIDTH = 224;
	public static final int HEIGHT = 48;
    
    protected static BufferedImage getImage(String fileName) throws IOException {
    	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream input = classLoader.getResourceAsStream(fileName);
		return ImageIO.read(input);
    }
	
	private final Component parent;
    private final Point offset = new Point();
    
    public DraggablePanel(Component parent) {
    	this.parent = parent;
    	addListeners();
    }
	
	private void addListeners() {
	    addMouseListener(new MouseAdapter() {
	        @Override
	        public void mousePressed(final MouseEvent e) {
	            offset.setLocation(e.getPoint());
	        }
	    });
	    
	    addMouseMotionListener(new MouseMotionAdapter() {
	        @Override
	        public void mouseDragged(final MouseEvent e) {
	        	parent.setLocation(e.getXOnScreen() - offset.x - getX(), e.getYOnScreen() - offset.y - getY());
	        }
	    });
	}
	
	protected void drawString(Graphics g, String text, Font font, Color color, Rectangle bounds) {
		drawString(g, text, font, color, bounds, false);
	}

    protected void drawString(Graphics g, String text, Font font, Color color, Rectangle bounds, boolean rightJustify) {
        g.setFont(font);
        g.setColor(color);
        
        FontMetrics fm = g.getFontMetrics();
        int x = bounds.x + (rightJustify ? bounds.width - fm.stringWidth(text) : (bounds.width - fm.stringWidth(text)) / 2);
        int y = bounds.y + ((bounds.height - fm.getHeight()) / 2) + fm.getAscent();
        
        g.drawString(text, x, y);
	}
}

class PlayerPanel extends DraggablePanel {
	private static final long serialVersionUID = 5970168237150923454L;

	private static final int MARGIN_LEFT = 0;
	private static final int MARGIN_RIGHT = 9;
	
	private static BufferedImage imgBackground;
    private static IOException imageLoadingException = null;
    
    static {
    	try {
			imgBackground = getImage("card_entity.png");
		} catch (IOException e) {
			imageLoadingException = e;
		}
    }
	
	private BufferedImage imgFaction;
	private Player player;

	public PlayerPanel(Component parent) throws IOException {
		super(parent);
		
		if (imageLoadingException != null) {
			throw imageLoadingException;
		}
		
		setSize(getPreferredSize());
	}
	
	public void setPlayer(Player player) {
		this.player = player;
		
		// Not the end of the world is this fails, too annoying to handle it
		try {
			if (player == null || player.generalId == -1) {
				imgFaction = null;
			} else {
				imgFaction = getImage(player.generalId + "_idle.png");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		repaint();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.drawImage(imgBackground, (getWidth() - imgBackground.getWidth()) / 2, (getHeight() - imgBackground.getHeight()) / 2, this);
		
		if (player != null) {
			if (imgFaction != null) {
				g.drawImage(imgFaction, MARGIN_LEFT, (getHeight() - imgFaction.getHeight()) / 2, this);
			}
			
			drawString(g, player.name, new Font("default", Font.BOLD, 24), Color.WHITE, new Rectangle(0, 0, getWidth() - MARGIN_RIGHT, getHeight()), true);
		}
	}
}

class CardPanel extends DraggablePanel {
	private static final long serialVersionUID = -2269874531212908045L;

	private static final int HEIGHT_COMPACT = 28;
	
    private static final int MANA_SIZE = 28;
    private static final int ATK_HP_SIZE = 24;
    private static final int MARGIN = 4;
    private static final int MINION_SHIFT = -16;
	
    private static BufferedImage imgBackground, imgMana, imgAtk, imgHp;
    private static Font font = new Font("default", Font.BOLD, 12);
    
    private static IOException imageLoadingException = null;
    
    static {
		try {
			imgBackground = getImage("card.png");
			imgMana = Scalr.resize(getImage("icon_mana.png"), Method.ULTRA_QUALITY, MANA_SIZE);
			imgAtk = Scalr.resize(getImage("icon_atk.png"), Method.ULTRA_QUALITY, ATK_HP_SIZE);
			imgHp = Scalr.resize(getImage("icon_hp.png"), Method.ULTRA_QUALITY, ATK_HP_SIZE);
		} catch (IOException e) {
			imageLoadingException = e;
		}
    }
    
    private Rectangle bndName, bndMana, bndAtk, bndHp;
    private BufferedImage imgIcon;
    private boolean compact;
    
    private Card card = null;
    private int count = 1;
	
	public CardPanel(Component parent) throws IOException {
		super(parent);
		
		if (imageLoadingException != null) {
			throw imageLoadingException;
		}

		setSize(getPreferredSize());
		setVisible(false);
		
		bndName = new Rectangle(0, 0, getWidth() - imgMana.getWidth() - MARGIN, imgMana.getHeight());
		bndMana = new Rectangle(getWidth() - imgMana.getWidth(), 0, imgMana.getWidth(), imgMana.getHeight());
		bndAtk = new Rectangle(getWidth() - (imgAtk.getWidth() + imgHp.getWidth()), getHeight() - imgAtk.getHeight(), imgAtk.getWidth(), imgAtk.getHeight());
		bndHp = new Rectangle(getWidth() - imgHp.getWidth(), getHeight() - imgHp.getHeight(), imgHp.getWidth(), imgHp.getHeight());
	}
	
	public void setCompact(boolean compact) {
		this.compact = compact;
		setSize(getPreferredSize());
		repaint();
	}
	
	public void setCard(Card card, int count) {
		this.card = card;
		this.count = count;
		
		// Not the end of the world is this fails, too annoying to handle it
		try {
			imgIcon = card == null ? null : getImage(card.id + "_idle.png");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		repaint();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, compact ? HEIGHT_COMPACT : HEIGHT);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.drawImage(imgBackground, (getWidth() - imgBackground.getWidth()) / 2, (getHeight() - imgBackground.getHeight()) / 2, this);
		g.drawImage(imgMana, bndMana.x, bndMana.y, this);
		
		if (card != null) {
			if (card.isMinion() && !compact) {
				g.drawImage(imgAtk, bndAtk.x, bndAtk.y, this);
				g.drawImage(imgHp, bndHp.x, bndHp.y, this);
			}
			
			if (imgIcon != null) {
				g.drawImage(imgIcon, card.isMinion() ? MINION_SHIFT + MARGIN : MARGIN, (getHeight() - imgIcon.getHeight()) / 2, this);
			}
			
			drawString(g, card.name + (count > 1 ? " x" + count : ""), font, Color.WHITE, bndName, true);
			drawString(g, Integer.toString(card.manaCost), font, Color.BLACK, bndMana);
			
			if (card.isMinion() && !compact) {
				drawString(g, Integer.toString(card.attack), font, Color.YELLOW, bndAtk);
				drawString(g, Integer.toString(card.health), font, Color.RED, bndHp);
			}
		}
	}
}

class CardComparator implements Comparator<Card> {
	@Override
    public int compare(Card card1, Card card2) {
        if (card1.manaCost == card2.manaCost) {
        	return card1.name.compareTo(card2.name);
        } else {
        	return card1.manaCost - card2.manaCost;
        }
    }
}
