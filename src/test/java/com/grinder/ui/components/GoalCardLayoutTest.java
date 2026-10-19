package com.grinder.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class GoalCardLayoutTest {
    @Test
    public void progressShowsPercentageAlongsideCountsWithoutColorExplanation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalComponent card = card("Checklist", true);
            assertTrue(hasLabel(card, "5 / 5 (100.0%)"));
            card.setProgress(3, 7, "3 / 7");
            assertTrue(hasLabel(card, "3 / 7 (42.9%)"));
            card.setExpanded(false);
            card.setProgressBarVisible(false);
            assertTrue(hasLabel(card, "3 / 7 (42.9%)"));
            card.setProgress(0, 7, "0 / 7");
            assertTrue(hasLabel(card, "0 / 7 (0.0%)"));
            card.setProgress(8, 7, "8 / 7");
            assertTrue(hasLabel(card, "8 / 7 (100.0%)"));
            card.setProgressStatus("Waiting for client data");
            assertTrue(hasLabel(card, "Waiting for client data"));
            card.setProgress(0, 0, "Waiting for client data");
            assertTrue(hasLabel(card, "Waiting for client data"));
            assertNull(progress(card).getToolTipText());
        });
    }

    private static boolean hasLabel(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel && text.equals(((JLabel) child).getText()) && child.isVisible()) {
                assertNull(((JLabel) child).getToolTipText());
                return true;
            }
            if (child instanceof Container && hasLabel((Container) child, text)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void replacingChecklistKeepsNeighboringCardsLeftAlignedAndFullWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel list = new GoalListPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            GoalComponent first = card("Soul Wars", false);
            GoalComponent second = card("Speedruns", false);
            GoalComponent previous = card("Checklist", true);
            for (GoalComponent card : new GoalComponent[]{first, second, previous}) {
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(card);
            }
            layout(list);
            assertAligned(list);
            GoalComponent replacement = card("Checklist", true);
            list.remove(previous);
            list.add(replacement, 2);
            layout(list);
            assertAligned(list);
            assertEquals(Component.LEFT_ALIGNMENT, replacement.getAlignmentX(), 0.0f);
        });
    }

    @Test
    public void collapsingChecklistShrinksOnlyItsDetailsAndKeepsProgressVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel list = new GoalListPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            GoalComponent checklist = card("Checklist", true);
            list.add(checklist);
            layout(list);
            int expanded = checklist.getHeight();
            JProgressBar progress = progress(checklist);
            assertNotNull(progress);
            checklist.setExpanded(false);
            layout(list);
            assertTrue(checklist.getHeight() < expanded);
            assertFalse(checklist.getContent().isVisible());
            assertTrue(progress.isVisible());
            assertTrue(progress.getHeight() > 0);
            assertAligned(list);
            checklist.setExpanded(true);
            layout(list);
            assertTrue(checklist.getContent().isVisible());
            assertEquals(expanded, checklist.getHeight());
            assertAligned(list);
        });
    }

    private static GoalComponent card(String title, boolean checklist) {
        GoalComponent card = new GoalComponent();
        card.setTitle(title);
        card.setReliability(checklist ? "Manual progress" : "Client-reported counter", "Source details");
        if (checklist) {
            card.setDescription("Personal checklist");
            for (int i = 0; i < 5; i++) {
                JCheckBox step = new JCheckBox("Step " + (i + 1), true);
                step.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.addDetail(step);
            }
        }
        card.setProgress(checklist ? 5 : 0, checklist ? 5 : 5000, checklist ? "5 / 5" : "0 / 5000");
        return card;
    }

    private static void layout(JPanel list) {
        list.invalidate();
        list.setSize(new Dimension(224, list.getPreferredSize().height));
        layoutTree(list);
    }

    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) {
                layoutTree((Container) child);
            }
        }
    }

    private static void assertAligned(JPanel list) {
        for (Component card : list.getComponents()) {
            assertEquals("Card must not shift horizontally after replacement", 0, card.getX());
            assertEquals("Card must fill the same available width", list.getWidth(), card.getWidth());
        }
    }

    private static JProgressBar progress(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JProgressBar) {
                return (JProgressBar) child;
            }
            if (child instanceof Container) {
                JProgressBar found = progress((Container) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
