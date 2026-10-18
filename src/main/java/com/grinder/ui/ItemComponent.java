
package com.grinder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import lombok.Getter;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

public class ItemComponent extends JPanel
{
    private static final int SIZE = 40;
    private static final int ICON_WIDTH = 36;
    private static final int ICON_HEIGHT = 32;
    private static final int CORNER_RADIUS = 4;

    private static final Color BACKGROUND_COLOR = new Color(35, 35, 35);
    private static final Color DEFAULT_BORDER_COLOR = new Color(90, 90, 90);
    private static final Color AMOUNT_COLOR = Color.WHITE;
    private static final Color AMOUNT_SHADOW_COLOR = Color.BLACK;

    private final ItemManager itemManager;
    private final ImageComponent imageComponent = new ImageComponent(ICON_WIDTH, ICON_HEIGHT);

    @Getter
    private ItemComposition item;

    @Getter
    private int amount = 1;

    @Getter
    private Color borderColor = DEFAULT_BORDER_COLOR;

    public ItemComponent(ItemManager itemManager, ItemComposition item)
    {
        this(itemManager, item, 1);
    }

    public ItemComponent(ItemManager itemManager, ItemComposition item, int amount)
    {
        this.itemManager = itemManager;
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        add(imageComponent);
        setItem(item);
        setAmount(amount);
    }

    public ItemComponent(ItemComposition item, BufferedImage image)
    {
        this(item, image, 1);
    }

    public ItemComponent(ItemComposition item, BufferedImage image, int amount)
    {
        this(item, image, amount, null, DEFAULT_BORDER_COLOR);
    }

    public ItemComponent(ItemComposition item, BufferedImage image, int amount, String tooltip, Color borderColor)
    {
        this.itemManager = null;
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        add(imageComponent);
        this.item = item;
        imageComponent.setImage(image);
        setAmount(amount);
        setTooltip(tooltip);
        setBorderColor(borderColor);
    }

    public void setItem(ItemComposition item)
    {
        this.item = item;
        imageComponent.setImage(itemManager != null && item != null ? itemManager.getImage(item.getId()) : null);
        setToolTipText(item == null ? null : item.getName());
        repaint();
    }

    public int getItemId()
    {
        return item == null ? -1 : item.getId();
    }

    public String getItemName()
    {
        return item == null ? "" : item.getName();
    }

    public BufferedImage getImage()
    {
        return imageComponent.getImage();
    }

    public void setImage(BufferedImage image)
    {
        imageComponent.setImage(image);
        repaint();
    }

    public void setAmount(int amount)
    {
        this.amount = Math.max(1, amount);
        repaint();
    }

    public void setTooltip(String tooltip)
    {
        setToolTipText(tooltip == null || tooltip.trim().isEmpty() ? null : tooltip);
    }

    public void setBorderColor(Color borderColor)
    {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    public void doLayout()
    {
        imageComponent.setBounds((getWidth() - ICON_WIDTH) / 2, (getHeight() - ICON_HEIGHT) / 2, ICON_WIDTH, ICON_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();

        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(BACKGROUND_COLOR);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);

            if (borderColor != null)
            {
                g.setColor(borderColor);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
            }

            if (amount > 1)
            {
                String text = formatAmount(amount);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                int x = getWidth() - g.getFontMetrics().stringWidth(text) - 3;
                int y = getHeight() - 3;
                g.setColor(AMOUNT_SHADOW_COLOR);
                g.drawString(text, x + 1, y + 1);
                g.setColor(AMOUNT_COLOR);
                g.drawString(text, x, y);
            }
        }
        finally
        {
            g.dispose();
        }

        super.paintComponent(graphics);
    }

    private String formatAmount(int amount)
    {
        if (amount >= 1_000_000)
        {
            return String.format("%.1fm", amount / 1_000_000.0);
        }

        if (amount >= 100_000)
        {
            return String.format("%.0fk", amount / 1_000.0);
        }

        if (amount >= 10_000)
        {
            return String.format("%.1fk", amount / 1_000.0);
        }

        return String.valueOf(amount);
    }
}
