package com.grinder.ui.components;

import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.util.UiTypography;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class TypographyTest {
    @Test
    public void sectionAndEntryTitlesUseAnExplicitConsistentScale() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalCategorySectionComponent section = new GoalCategorySectionComponent("Collection Log", null);
            GoalComponent entry = new GoalComponent();
            entry.setTitle("Chambers of Xeric");
            assertSame(UiTypography.SECTION, label(section, "Collection Log").getFont());
            assertSame(UiTypography.TITLE, label(entry, "Chambers of Xeric").getFont());
            assertEquals(UiTypography.TITLE.getSize2D(), UiTypography.SECTION.getSize2D(), 0.0f);
        });
    }

    @Test
    public void statusAndItemControlsUseReadableBodyText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalComponent entry = new GoalComponent();
            entry.setGoalStatus(GoalStatus.NOT_STARTED);
            assertSame(UiTypography.BODY, label(entry, "Not started").getFont());
            ItemContainerComponent items = new ItemContainerComponent();
            assertSame(UiTypography.BODY, label(items, "No items to display").getFont());
            JButton more = (JButton) children(items).stream().filter(child -> child instanceof JButton).findFirst().get();
            assertSame(UiTypography.BODY, more.getFont());
            assertTrue(more.getPreferredSize().height >= more.getFontMetrics(more.getFont()).getHeight());
        });
    }

    private JLabel label(Container container, String text) {
        return (JLabel) children(container).stream().filter(child -> child instanceof JLabel && text.equals(((JLabel) child).getText())).findFirst().get();
    }

    private List<Component> children(Container container) {
        List<Component> result = new ArrayList<>();
        for (Component child : container.getComponents()) {
            result.add(child);
            if (child instanceof Container) {
                result.addAll(children((Container) child));
            }
        }
        return result;
    }
}
