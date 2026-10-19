package com.grinder.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.components.IconTextField;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class SearchableListTest {
    @Test
    public void searchesAcrossNamesAndCategoriesAndClearsUnavailableSelections() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<String> selected = new ArrayList<>();
            SearchableList<String> picker = picker(selected);
            picker.setItems(Arrays.asList("Vorkath kills", "Slayer points", "Slayer streak"));
            assertEquals("Vorkath kills", picker.getSelectedValue());
            IconTextField search = find(picker, IconTextField.class);
            search.setText("POINTS slayer");
            assertEquals("Slayer points", picker.getSelectedValue());
            assertEquals(1, find(picker, JList.class).getModel().getSize());
            search.setText("missing entry");
            assertNull(picker.getSelectedValue());
            assertNull(selected.get(selected.size() - 1));
            picker.setSelectedValue("Vorkath kills");
            assertEquals("", search.getText());
            assertEquals("Vorkath kills", picker.getSelectedValue());
        });
    }

    @Test
    public void categoryFilteringAndReusingTheModelNeverKeepStaleValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SearchableList<String> picker = picker(new ArrayList<>());
            picker.setItems(Arrays.asList("Vorkath kills", "Slayer points", "Slayer streak"));
            JComboBox<?> category = find(picker, JComboBox.class);
            assertTrue(category.isVisible());
            category.setSelectedItem("Slayer");
            assertEquals(2, find(picker, JList.class).getModel().getSize());
            assertTrue(picker.getSelectedValue().startsWith("Slayer"));
            picker.setItems(Collections.singletonList("Vorkath kills"));
            assertFalse(category.isVisible());
            assertEquals("Vorkath kills", picker.getSelectedValue());
            picker.setItems(Collections.emptyList());
            assertNull(picker.getSelectedValue());
            assertEquals(0, find(picker, JList.class).getModel().getSize());
        });
    }

    private static SearchableList<String> picker(List<String> selected) {
        return new SearchableList<String>(selected::add) {
            @Override
            protected String getText(String value) {
                return value;
            }

            @Override
            protected String getCategory(String value) {
                return value.startsWith("Slayer") ? "Slayer" : "Bossing";
            }
        };
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container) {
                T found = find((Container) component, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
