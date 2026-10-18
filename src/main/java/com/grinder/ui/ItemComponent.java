
package com.grinder.ui;

import lombok.Getter;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ItemComponent extends JPanel {
    private static final int SIZE = 40;
    private static final int ICON_WIDTH = 36;
    private static final int ICON_HEIGHT = 32;
    private static final int CORNER_RADIUS = 4;

    private static final Color BACKGROUND_COLOR = ColorScheme.DARK_GRAY_COLOR;
    private static final Color DEFAULT_BORDER_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color AMOUNT_COLOR = Color.WHITE;
    private static final Color AMOUNT_SHADOW_COLOR = Color.BLACK;

    private final ItemManager itemManager;
    private final ImageComponent imageComponent = new ImageComponent(ICON_WIDTH, ICON_HEIGHT);

    private ItemComposition item;
    private int amount = 1;
    private String tooltip;

    @Getter
    private Color borderColor = DEFAULT_BORDER_COLOR;

    public ItemComponent(ItemManager itemManager, ItemComposition item) {
        this(itemManager, item, 1);
    }

    public ItemComponent(ItemManager itemManager, ItemComposition item, int amount) {
        this.itemManager = itemManager;
        this.amount = Math.max(0, amount);
        initialize();
        setItem(item);
    }

    public ItemComponent(ItemComposition item, BufferedImage image) {
        this(item, image, 1);
    }

    public ItemComponent(ItemComposition item, BufferedImage image, int amount) {
        this.itemManager = null;
        this.item = item;
        this.amount = Math.max(0, amount);
        initialize();
        setImage(image);
        updateTooltip();
    }

    public ItemComponent(ItemComposition item, BufferedImage image, int amount, String tooltip, Color borderColor) {
        this(item, image, amount);
        setTooltip(tooltip);
        setBorderColor(borderColor);
    }

    private void initialize() {
        setLayout(null);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        setOpaque(false);
        add(imageComponent);
    }

    public ItemComposition getItem() {
        return item;
    }

    public void setItem(ItemComposition item) {
        this.item = item;

        if (itemManager != null) {
            setImage(item == null ? null : itemManager.getImage(item.getId()));
        }

        updateTooltip();
        repaint();
    }

    public int getItemId() {
        return item == null ? -1 : item.getId();
    }

    public String getItemName() {
        return item == null ? "Unknown item" : item.getName();
    }

    public BufferedImage getImage() {
        return imageComponent.getImage();
    }

    public void setImage(BufferedImage image) {
        imageComponent.setImage(image);
        repaint();
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
        repaint();
    }

    public String getTooltip() {
        return tooltip;
    }

    /**
     * Sets the tooltip exactly as supplied.
     * Passing null clears the tooltip.
     */
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
        updateTooltip();
    }

    /**
     * Sets an explicit border color.
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor == null ? DEFAULT_BORDER_COLOR : borderColor;
        repaint();
    }

    private String format(int amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fm", amount / 1_000_000.0);
        }

        if (amount >= 100_000) {
            return String.format(Locale.ROOT, "%.0fk", amount / 1_000.0);
        }

        if (amount >= 10_000) {
            return String.format(Locale.ROOT, "%.1fk", amount / 1_000.0);
        }

        return NumberFormat.getIntegerInstance(Locale.ROOT).format(amount);
    }

    private void updateTooltip() {
        setToolTipText(tooltip);
    }

    @Override
    public void doLayout() {
        imageComponent.setBounds((getWidth() - ICON_WIDTH) / 2, (getHeight() - ICON_HEIGHT) / 2, ICON_WIDTH, ICON_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(BACKGROUND_COLOR);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);

            g.setColor(borderColor);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);

            if (amount > 1) {
                String text = format(amount);
                g.setFont(getFont().deriveFont(Font.BOLD, 10f));

                FontMetrics metrics = g.getFontMetrics();
                int x = getWidth() - metrics.stringWidth(text) - 3;
                int y = getHeight() - metrics.getDescent() - 2;

                g.setColor(AMOUNT_SHADOW_COLOR);
                g.drawString(text, x + 1, y + 1);

                g.setColor(AMOUNT_COLOR);
                g.drawString(text, x, y);
            }
        } finally {
            g.dispose();
        }
    }
}
