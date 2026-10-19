package com.grinder.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ItemGridTest {
    @Test
    public void nativeItemSpriteIsNotStretchedOrClipped() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage sprite = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D source = sprite.createGraphics();
            source.setColor(Color.RED);
            source.fillRect(0, 0, 36, 32);
            source.dispose();
            ItemComponent tile = new ItemComponent(null, null, sprite, 1, "Item", null);
            tile.setSize(40, 40);
            BufferedImage result = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = result.createGraphics();
            tile.paint(graphics);
            graphics.dispose();
            int redPixels = 0;
            for (int y = 0; y < 40; y++) {
                for (int x = 0; x < 40; x++) {
                    if (result.getRGB(x, y) == Color.RED.getRGB()) {
                        redPixels++;
                    }
                }
            }
            assertEquals(36 * 32, redPixels);
            assertEquals(Color.RED.getRGB(), result.getRGB(2, 4));
            assertEquals(Color.RED.getRGB(), result.getRGB(37, 35));
        });
    }

    @Test
    public void previewExpandsWithoutNestedScrollingAndCanCollapseAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemContainerComponent container = filledContainer(23);
            layout(container, 172);
            JPanel grid = (JPanel) container.getComponent(0);
            JButton toggle = (JButton) container.getComponent(1);
            assertEquals(20, grid.getComponentCount());
            assertTrue(toggle.isVisible());
            int previewHeight = container.getPreferredSize().height;
            toggle.doClick();
            layout(container, 172);
            assertEquals(23, grid.getComponentCount());
            assertTrue(container.getPreferredSize().height > previewHeight);
            toggle.doClick();
            assertEquals(20, grid.getComponentCount());
        });
    }

    @Test
    public void narrowGridUsesFewerColumnsInsteadOfSquashingItems() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemContainerComponent container = filledContainer(23);
            layout(container, 126);
            JPanel grid = (JPanel) container.getComponent(0);
            grid.doLayout();
            assertEquals(10, grid.getComponentCount());
            for (Component tile : grid.getComponents()) {
                assertTrue(tile.getWidth() >= ItemComponent.CELL_SIZE);
                assertEquals(ItemComponent.CELL_SIZE, tile.getHeight());
            }
            layout(container, 172);
            assertEquals(20, grid.getComponentCount());
        });
    }

    @Test
    public void shortAndEmptyGridsDoNotKeepAFullPreviewHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemContainerComponent container = filledContainer(3);
            layout(container, 172);
            assertEquals(40, container.getPreferredSize().height);
            assertFalse(container.getComponent(1).isVisible());
            container.clear();
            assertTrue(container.getItems().isEmpty());
            assertTrue(container.getPreferredSize().height < 40);
        });
    }

    private static ItemContainerComponent filledContainer(int count) {
        List<ItemComponent> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new ItemComponent(null, null));
        }
        ItemContainerComponent container = new ItemContainerComponent();
        container.set(items);
        return container;
    }

    private static void layout(ItemContainerComponent container, int width) {
        container.setSize(width, 1);
        container.setSize(width, container.getPreferredSize().height);
        container.doLayout();
    }
}
