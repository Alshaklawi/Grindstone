
package com.grinder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import lombok.Getter;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

public class ItemComponent extends JPanel
{
    private static final int ITEM_SIZE = 40;
    private static final int IMAGE_WIDTH = 36;
    private static final int IMAGE_HEIGHT = 32;
    private static final Color DEFAULT_BORDER_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color AMOUNT_COLOR = Color.WHITE;
    private static final Font AMOUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);

    private final ItemManager itemManager;
    private final ImageComponent imageComponent;

    @Getter
    private ItemComposition item;

    @Getter
    private int amount = 1;

    @Getter
    private Color borderColor = DEFAULT_BORDER_COLOR;

    private int renderedItemId = -1;
    private String amountText = "";
    private String tooltip;
    private BufferedImage image;

    public ItemComponent(ItemManager itemManager, ItemComposition item)
    {
        this(itemManager, item, null, 1, item == null ? null : item.getName(), null);
    }

    public ItemComponent(ItemManager itemManager, ItemComposition item, BufferedImage image, int amount, String tooltip, Color borderColor)
    {
        this.itemManager = itemManager;
        this.imageComponent = new ImageComponent(ITEM_SIZE, ITEM_SIZE);
        setOpaque(false);
        setLayout(null);
        setPreferredSize(new Dimension(ITEM_SIZE, ITEM_SIZE));
        setMinimumSize(new Dimension(ITEM_SIZE, ITEM_SIZE));
        setMaximumSize(new Dimension(ITEM_SIZE, ITEM_SIZE));
        setBorder(BorderFactory.createEmptyBorder());
        add(imageComponent);
        this.image = image;
        this.amount = Math.max(1, amount);
        this.amountText = formatAmount(this.amount);
        this.tooltip = tooltip;
        this.borderColor = borderColor == null ? DEFAULT_BORDER_COLOR : borderColor;
        setItem(item);
        setTooltip(tooltip);
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
        return image;
    }

    public void setItem(ItemComposition item)
    {
        int nextId = item == null ? -1 : item.getId();
        boolean changed = this.item != item || renderedItemId != nextId;

        this.item = item;

        if (renderedItemId != nextId)
        {
            renderedItemId = nextId;
            image = item == null || itemManager == null ? null : itemManager.getImage(nextId);
            imageComponent.setImage(image);
            revalidate();
            repaint();
        }
        else if (changed)
        {
            repaint();
        }

        if (item != null && tooltip == null)
        {
            setToolTipText(item.getName());
        }
    }

    public void setImage(BufferedImage image)
    {
        if (this.image == image)
        {
            return;
        }

        this.image = image;
        imageComponent.setImage(image);
        repaint();
    }

    public void setAmount(int amount)
    {
        int nextAmount = Math.max(1, amount);

        if (this.amount == nextAmount)
        {
            return;
        }

        this.amount = nextAmount;
        this.amountText = formatAmount(nextAmount);
        repaint();
    }

    public void setTooltip(String tooltip)
    {
        if (java.util.Objects.equals(this.tooltip, tooltip))
        {
            return;
        }

        this.tooltip = tooltip;
        setToolTipText(tooltip);
    }

    public void setBorderColor(Color borderColor)
    {
        Color nextColor = borderColor == null ? DEFAULT_BORDER_COLOR : borderColor;

        if (this.borderColor.equals(nextColor))
        {
            return;
        }

        this.borderColor = nextColor;
        repaint();
    }

    @Override
    public void doLayout()
    {
        imageComponent.setBounds((getWidth() - IMAGE_WIDTH) / 2, (getHeight() - IMAGE_HEIGHT) / 2, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(ColorScheme.DARKER_GRAY_COLOR);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

            g.setColor(borderColor);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

            if (amount > 1)
            {
                g.setFont(AMOUNT_FONT);
                g.setColor(AMOUNT_COLOR);
                g.drawString(amountText, 3, getHeight() - 4);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    private static String formatAmount(int amount)
    {
        if (amount >= 1_000_000)
        {
            return String.format(java.util.Locale.ROOT, "%.1fm", amount / 1_000_000.0);
        }

        if (amount >= 100_000)
        {
            return (amount / 1_000) + "k";
        }

        if (amount >= 10_000)
        {
            return String.format(java.util.Locale.ROOT, "%.1fk", amount / 1_000.0);
        }

        if (amount >= 1_000)
        {
            return String.format(java.util.Locale.ROOT, "%.1fk", amount / 1_000.0);
        }

        return Integer.toString(amount);
    }
}
