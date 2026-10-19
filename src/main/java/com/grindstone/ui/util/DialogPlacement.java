package com.grindstone.ui.util;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import javax.swing.SwingUtilities;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class DialogPlacement {
    private DialogPlacement() {
    }

    public static Component owner(Component component) {
        if (component == null) {
            return null;
        }
        if (component instanceof Window) {
            return component;
        }
        Window window = SwingUtilities.getWindowAncestor(component);
        return window == null ? component : window;
    }

    public static Rectangle usableBounds(Component component) {
        Component owner = owner(component);
        GraphicsConfiguration configuration = owner == null ? null : owner.getGraphicsConfiguration();
        if (configuration == null) {
            configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        Rectangle bounds = new Rectangle(configuration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    public static void center(Window dialog, Component component) {
        Rectangle bounds = usableBounds(component);
        dialog.setSize(Math.min(dialog.getWidth(), Math.max(1, bounds.width - 40)), Math.min(dialog.getHeight(), Math.max(1, bounds.height - 40)));
        dialog.setLocationRelativeTo(owner(component));
        dialog.setLocation(Math.max(bounds.x, Math.min(dialog.getX(), bounds.x + bounds.width - dialog.getWidth())), Math.max(bounds.y, Math.min(dialog.getY(), bounds.y + bounds.height - dialog.getHeight())));
    }
}
