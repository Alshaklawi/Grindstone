package com.grinder.ui.util;

import java.awt.Component;
import javax.swing.JComponent;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class UiTooltips {
    private UiTooltips() {
    }

    public static void set(JComponent component, String text) {
        String previous = component.getToolTipText();
        component.setToolTipText(text);
        for (Component child : component.getComponents()) {
            if (child instanceof JComponent) {
                JComponent nested = (JComponent) child;
                String existing = nested.getToolTipText();
                if (existing == null || java.util.Objects.equals(existing, previous)) {
                    set(nested, text);
                }
            }
        }
    }
}
