package com.grinder.util;

import net.runelite.api.ItemComposition;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class ItemVariants {
    private ItemVariants() {
    }

    public static int normalId(ItemComposition item) {
        return item == null ? -1 : normalId(item.getId(), item.getNote(), item.getLinkedNoteId(), item.getPlaceholderTemplateId(), item.getPlaceholderId());
    }

    public static int normalId(int id, int noteTemplate, int linkedNote, int placeholderTemplate, int placeholder) {
        if (id <= 0) {
            return -1;
        }
        if (placeholderTemplate != -1) {
            return placeholder > 0 ? placeholder : -1;
        }
        return noteTemplate == -1 ? id : linkedNote > 0 ? linkedNote : -1;
    }
}
