package com.grinder.content;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum TrackerEntry {
    VETION(VarPlayerID.TOTAL_VETION_KILLS),
    VENENATIS(VarPlayerID.TOTAL_VENENATIS_KILLS),
    CHAMBERS_OF_XERIC(VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS),
    WINTERTODT(VarPlayerID.TOTAL_WINTERTODT_KILLS),
    CORPOREAL_BEAST(VarPlayerID.TOTAL_CORP_KILLS);

    TrackerEntry(final int id) {
        this.id = id;
    }

    @Getter
    private final int id;

    public int getCount(Client client) {
        return client.getVarpValue(id);
    }
}
