package com.grindstone.ui.components;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ItemComponent extends JPanel {
    public static final int CELL_SIZE = 40;
    private static final int IMAGE_WIDTH = 36;
    private static final int IMAGE_HEIGHT = 32;
    private static final Color DEFAULT_BORDER_COLOR = ColorScheme.BORDER_COLOR;
    private static final Font AMOUNT_FONT = FontManager.getRunescapeSmallFont();

    private final ItemManager itemManager;
    @Getter
    private ItemComposition item;
    @Getter
    private int amount = 1;
    @Getter
    private Color borderColor = DEFAULT_BORDER_COLOR;
    private int renderedItemId = -1;
    private int itemId = -1;
    private String itemName = "";
    private String amountText = "";
    @Getter
    private BufferedImage image;
    private Boolean obtained;
    private boolean hovered;

    public ItemComponent(ItemManager itemManager, ItemComposition item) {
        this(itemManager, item, null, 1, item == null ? null : item.getName(), null);
    }

    public ItemComponent(ItemManager itemManager, ItemComposition item, BufferedImage image, int amount, String tooltip, Color borderColor) {
        this.itemManager = itemManager;
        setOpaque(false);
        setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        setMinimumSize(new Dimension(CELL_SIZE, CELL_SIZE));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, CELL_SIZE));
        setItem(item);
        if (image != null) {
            setImage(image);
        }
        setAmount(amount);
        setBorderColor(borderColor);
        setTooltip(tooltip);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }
        });
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public ItemComponent(int itemId, String itemName, BufferedImage image, String tooltip, boolean obtained) {
        this(null, null, image, 1, tooltip, null);
        this.itemId = itemId;
        this.itemName = itemName;
        getAccessibleContext().setAccessibleName(itemName);
        setObtained(obtained);
    }


    public void setItem(ItemComposition item) {
        this.item = item;
        itemId = item == null ? -1 : item.getId();
        itemName = item == null ? "" : item.getName();
        int nextId = getItemId();
        if (renderedItemId != nextId) {
            renderedItemId = nextId;
            setImage(itemManager == null || nextId <= 0 ? null : itemManager.getImage(nextId));
        }
        getAccessibleContext().setAccessibleName(getItemName());
        repaint();
    }

    public void setImage(BufferedImage image) {
        if (this.image == image) {
            return;
        }
        this.image = image;
        if (image instanceof AsyncBufferedImage) {
            ((AsyncBufferedImage) image).onLoaded(() -> SwingUtilities.invokeLater(this::repaint));
        }
        repaint();
    }

    public void setObtained(boolean obtained) {
        if (this.obtained != null && this.obtained == obtained) {
            return;
        }
        this.obtained = obtained;
        getAccessibleContext().setAccessibleDescription(obtained ? "Obtained collection-log item" : "Missing collection-log item");
        repaint();
    }

    public void setAmount(int amount) {
        int next = Math.max(1, amount);
        if (this.amount == next) {
            return;
        }
        this.amount = next;
        amountText = formatAmount(next);
        repaint();
    }

    public void setTooltip(String tooltip) {
        setToolTipText(tooltip);
    }

    public void setBorderColor(Color borderColor) {
        Color next = borderColor == null ? DEFAULT_BORDER_COLOR : borderColor;
        if (next.equals(this.borderColor)) {
            return;
        }
        this.borderColor = next;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered ? ColorScheme.DARKER_GRAY_HOVER_COLOR : Boolean.TRUE.equals(obtained) ? new Color(36, 47, 39) : ColorScheme.DARKER_GRAY_COLOR);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g.setColor(hovered ? ColorScheme.LIGHT_GRAY_COLOR : Boolean.TRUE.equals(obtained) ? new Color(65, 87, 69) : borderColor);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            if (image != null) {
                if (Boolean.FALSE.equals(obtained) && !hovered) {
                    g.setComposite(AlphaComposite.SrcOver.derive(0.55f));
                }
                int width = Math.min(IMAGE_WIDTH, image.getWidth());
                int height = Math.min(IMAGE_HEIGHT, image.getHeight());
                g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, null);
                g.setComposite(AlphaComposite.SrcOver);
            }
            if (Boolean.TRUE.equals(obtained)) {
                int x = getWidth() - 14;
                int y = getHeight() - 14;
                g.setColor(new Color(28, 41, 31));
                g.fillOval(x, y, 12, 12);
                g.setColor(new Color(134, 190, 139));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x + 3, y + 6, x + 5, y + 8);
                g.drawLine(x + 5, y + 8, x + 9, y + 4);
            }
            if (amount > 1) {
                g.setFont(AMOUNT_FONT);
                g.setColor(Color.BLACK);
                g.drawString(amountText, 4, 13);
                g.setColor(Color.YELLOW);
                g.drawString(amountText, 3, 12);
            }
        } finally {
            g.dispose();
        }
    }

    private static String formatAmount(int amount) {
        if (amount >= 1_000_000) {
            return (amount / 1_000_000) + "M";
        }
        if (amount >= 100_000) {
            return (amount / 1_000) + "K";
        }
        return Integer.toString(amount);
    }
}
