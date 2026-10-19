package com.grinder.ui.components;

import java.awt.AlphaComposite;
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
        return item == null ? -1 : item.getId();
    }

    public String getItemName() {
        return item == null ? "" : item.getName();
    }


    public void setItem(ItemComposition item) {
        this.item = item;
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
            g.setColor(hovered ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(hovered ? ColorScheme.LIGHT_GRAY_COLOR : Boolean.TRUE.equals(obtained) ? ColorScheme.PROGRESS_COMPLETE_COLOR : borderColor);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            if (image != null) {
                if (Boolean.FALSE.equals(obtained) && !hovered) {
                    g.setComposite(AlphaComposite.SrcOver.derive(0.35f));
                }
                int width = Math.min(IMAGE_WIDTH, image.getWidth());
                int height = Math.min(IMAGE_HEIGHT, image.getHeight());
                g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, null);
                g.setComposite(AlphaComposite.SrcOver);
            }
            if (Boolean.TRUE.equals(obtained)) {
                g.setColor(ColorScheme.DARKER_GRAY_COLOR);
                g.fillRect(getWidth() - 13, getHeight() - 11, 11, 9);
                g.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
                int x = getWidth() - 11;
                int y = getHeight() - 7;
                g.drawLine(x, y, x + 2, y + 2);
                g.drawLine(x + 2, y + 2, x + 7, y - 3);
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
