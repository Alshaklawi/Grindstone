package com.grinder.ui.editor;

import java.awt.Component;
import java.awt.Container;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class GoalTypePickerTest {
    @Test
    public void everyGoalTypeIsReachableAndEditingKeepsThePickerInSync() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> model = new JComboBox<>();
            for (int i = 0; i < 22; i++) {
                model.addItem("Type " + i);
            }
            GoalTypePicker picker = new GoalTypePicker(model);
            JComboBox<?> selection = findSelection(picker);
            Set<Integer> reachable = new HashSet<>();
            visitGroups(picker, model, selection, reachable);
            assertEquals(22, reachable.size());
            for (int i = 0; i < 22; i++) {
                model.setSelectedIndex(i);
                assertEquals(i, selection.getSelectedItem());
            }
        });
    }

    private static void visitGroups(Container parent, JComboBox<String> model, JComboBox<?> selection, Set<Integer> reachable) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JToggleButton) {
                ((JToggleButton) child).doClick();
                assertTrue(((JToggleButton) child).isSelected());
                for (int i = 0; i < selection.getItemCount(); i++) {
                    selection.setSelectedIndex(i);
                    int kind = (Integer) selection.getSelectedItem();
                    assertEquals(kind, model.getSelectedIndex());
                    reachable.add(kind);
                }
            } else if (child instanceof Container && !(child instanceof JComboBox)) {
                visitGroups((Container) child, model, selection, reachable);
            }
        }
    }

    private static JComboBox<?> findSelection(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JComboBox) {
                return (JComboBox<?>) child;
            }
            if (child instanceof Container) {
                JComboBox<?> found = findSelection((Container) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
