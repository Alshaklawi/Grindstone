package com.grinder.ui.item;

import com.grinder.ui.editor.CustomGoalForm;
import com.grinder.ui.editor.GoalEditorPanel;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.components.IconTextField;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ItemPickerTest {
    @Test
    public void queriesAcceptNamesIdsAndWordOrder() {
        assertTrue(ItemSearchService.isIdQuery("#" + ItemID.DRAGON_PICKAXE));
        assertTrue(ItemSearchService.isIdQuery(Integer.toString(ItemID.TWISTED_BOW)));
        assertFalse(ItemSearchService.isIdQuery("Twisted bow"));
        assertEquals(ItemID.DRAGON_PICKAXE, ItemSearchService.parseId("#" + ItemID.DRAGON_PICKAXE));
        assertEquals(-1, ItemSearchService.parseId("999999999999999999999"));
        assertTrue(ItemSearchService.matchesName("Dragon pickaxe", "PICK dragon"));
        assertFalse(ItemSearchService.matchesName("Dragon pickaxe", "dragon sword"));
        assertTrue(ItemSearchService.rank("Twisted bow", "twisted bow")
                < ItemSearchService.rank("Twisted bow (damaged)", "twisted bow"));
    }

    @Test
    public void staleResultsCannotReplaceANewerItemSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Consumer<ItemSearchService.Response>> callbacks = new ArrayList<>();
            ItemLookup lookup = (query, callback) -> {
                callbacks.add(callback);
                return () -> {
                };
            };
            ItemPickerPanel picker = new ItemPickerPanel(lookup, item -> {
            });
            IconTextField field = (IconTextField) picker.getComponent(0);
            field.setText("dragon pickaxe");
            picker.search();
            field.setText("twisted bow");
            picker.search();
            callbacks.get(1).accept(response(ItemID.TWISTED_BOW, "Twisted bow"));
            assertEquals(ItemID.TWISTED_BOW, picker.getSelectedItemId());
            callbacks.get(0).accept(response(ItemID.DRAGON_PICKAXE, "Dragon pickaxe"));
            assertEquals(ItemID.TWISTED_BOW, picker.getSelectedItemId());
            picker.stopSearch();
        });
    }

    @Test
    public void clearingQueryInvalidatesPreviousSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemLookup lookup = (query, callback) -> {
                callback.accept(response(ItemID.DRAGON_PICKAXE, "Dragon pickaxe"));
                return () -> {
                };
            };
            ItemPickerPanel picker = new ItemPickerPanel(lookup, item -> {
            });
            IconTextField field = (IconTextField) picker.getComponent(0);
            field.setText("dragon pickaxe");
            picker.search();
            assertTrue(picker.hasSelection());
            field.setText("");
            assertFalse(picker.hasSelection());
            picker.stopSearch();
        });
    }

    @Test
    public void adaptiveFormCreatesTheSelectedItemAndPreservesSkillTargets() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemLookup lookup = (query, callback) -> {
                callback.accept(response(ItemID.DRAGON_PICKAXE, "Dragon pickaxe"));
                return () -> {
                };
            };
            GoalEditorPanel form = new GoalEditorPanel(null, lookup);
            try {
                Goal skill = form.createGoal();
                assertTrue(skill instanceof SkillLevelGoal);
                assertEquals(Skill.ATTACK, ((SkillLevelGoal) skill).getSkill());
                assertEquals(99, skill.getProgressMaximum());
                JComboBox<?> type = (JComboBox<?>) descendants(form).stream()
                        .filter(component -> component instanceof JComboBox).findFirst().get();
                type.setSelectedItem("Collection item");
                assertFalse(form.isReady());
                ItemPickerPanel picker = (ItemPickerPanel) descendants(form).stream()
                        .filter(component -> component instanceof ItemPickerPanel).findFirst().get();
                IconTextField search = (IconTextField) descendants(picker).stream()
                        .filter(component -> component instanceof IconTextField).findFirst().get();
                search.setText(Integer.toString(ItemID.DRAGON_PICKAXE));
                picker.search();
                assertTrue(form.isReady());
                assertEquals(ItemID.DRAGON_PICKAXE, ((CollectionLogItemGoal) form.createGoal()).getItem());
                type.setSelectedItem("Owned item quantity");
                assertTrue(form.isReady());
                javax.swing.JSpinner quantity = (javax.swing.JSpinner) descendants(form).stream()
                        .filter(component -> component instanceof javax.swing.JSpinner).findFirst().get();
                quantity.setValue(3_000_000_000L);
                com.grinder.content.goal.impl.item.PossessedItemGoal owned =
                        (com.grinder.content.goal.impl.item.PossessedItemGoal) form.createGoal();
                assertEquals(ItemID.DRAGON_PICKAXE, owned.getItem());
                assertEquals(3_000_000_000L, owned.getAmount());
                type.setSelectedItem("Custom goal");
                assertTrue(descendants(form).stream().anyMatch(component -> component instanceof CustomGoalForm));
                assertTrue(form.isReady());
            } catch (java.text.ParseException exception) {
                throw new AssertionError(exception);
            } finally {
                form.stopSearch();
            }
        });
    }

    private static ItemSearchService.Response response(int id, String name) {
        return new ItemSearchService.Response(Collections.singletonList(
                new ItemSearchService.Result(id, name, null)), "Found", true);
    }

    private static List<Component> descendants(Container parent) {
        List<Component> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            found.add(child);
            if (child instanceof Container) {
                found.addAll(descendants((Container) child));
            }
        }
        return found;
    }
}
