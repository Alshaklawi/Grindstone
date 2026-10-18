package com.grinder.content;

import lombok.Getter;
import com.grinder.ui.Icon;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum TrackerEntry {
    VETION("Vet'ion kills", VarPlayerID.TOTAL_VETION_KILLS, Icon.COLLECTION_LOG),
    VENENATIS("Venenatis kills", VarPlayerID.TOTAL_VENENATIS_KILLS, Icon.COLLECTION_LOG),
    CHAMBERS_OF_XERIC("Chambers of Xeric completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS, Icon.COLLECTION_LOG),
    WINTERTODT("Wintertodt kills", VarPlayerID.TOTAL_WINTERTODT_KILLS, Icon.SKILLING),
    CORPOREAL_BEAST("Corporeal Beast kills", VarPlayerID.TOTAL_CORP_KILLS, Icon.COLLECTION_LOG);

    TrackerEntry(final String displayName, final int id, final Icon icon) {
        this.displayName = displayName;
        this.id = id;
        this.icon = icon;
    }

    @Getter
    private final int id;

    @Getter
    private final String displayName;

    @Getter
    private final Icon icon;

    public int getCount(Client client) {
        return client.getVarpValue(id);
    }
}
