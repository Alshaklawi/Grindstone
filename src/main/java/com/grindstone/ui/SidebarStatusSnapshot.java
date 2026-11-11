package com.grindstone.ui;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.storage.PlayerStorageSource;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.util.HtmlUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Value
final class SidebarStatusSnapshot {
    Map<String, String> statuses;
    Map<String, String> tips;
    Set<String> missing;
    long dueCount;
    int catchUpCount;
    String catchUpText;

    static SidebarStatusSnapshot capture(Client client, GrindstoneProfile active, boolean showDeadlineReminders) {
        Map<String, String> snapshotStatuses = new LinkedHashMap<>();
        Map<String, String> snapshotTips = new LinkedHashMap<>();
        Set<String> missingSnapshots = new LinkedHashSet<>();
        prepareSnapshotStatuses(client, active, snapshotStatuses, snapshotTips, missingSnapshots);
        long dueCount = active == null || !showDeadlineReminders ? 0 : active.getGoals().stream().filter(goal -> {
            MilestoneMetadata metadata = active.getMetadata(goal);
            return metadata.isDeadlineReminder() && metadata.getDeadlineAt() > 0 && metadata.getDeadlineAt() - System.currentTimeMillis() <= 86_400_000L && (metadata.getRecurring().isActive() ? metadata.getRecurring().getStatus() : goal.getStatus()) != MilestoneStatus.COMPLETE;
        }).count();
        List<String> catchUp = new ArrayList<>();
        if (active != null) {
            for (Milestone goal : active.getGoals()) {
                String reading = active.getMetadata(goal).getCatchUpText();
                if (reading != null) {
                    catchUp.add(reading);
                }
            }
        }
        String capturedCatchUp = "Progress since the previous Grindstone reading:\n\n" + String.join("\n", catchUp) + "\n\nUpdated when you returned to RuneLite. Open your collection log, bank or activity screen to refresh missing data.";
        return new SidebarStatusSnapshot(Collections.unmodifiableMap(new LinkedHashMap<>(snapshotStatuses)), Collections.unmodifiableMap(new LinkedHashMap<>(snapshotTips)), Collections.unmodifiableSet(new LinkedHashSet<>(missingSnapshots)), dueCount, catchUp.size(), capturedCatchUp);
    }

    private static void prepareSnapshotStatuses(Client client, GrindstoneProfile active, Map<String, String> statuses, Map<String, String> tips, Set<String> missing) {
        Map<PlayerStorageSource, Instant> seen = active == null ? Collections.emptyMap() : active.getPlayerStorage().getLastSeen();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
        Instant bankCaptured = seen.get(PlayerStorageSource.BANK);
        statuses.put("Bank", bankCaptured == null ? "Not loaded" : time.format(bankCaptured));
        if (bankCaptured == null) {
            missing.add("Bank");
        }
        tips.put("Bank", "<html>Open your bank to refresh.<br>" + (bankCaptured == null ? "No saved contents yet." : "Last updated " + time.format(bankCaptured) + (active.getPlayerStorage().isLoadedThisSession(PlayerStorageSource.BANK) ? " this login." : " in a previous login.")) + "</html>");
        int known = active == null ? 0 : active.getCollectionLogHistory().getKnownUnlockCount();
        boolean live = active != null && client.getGameState() == GameState.LOGGED_IN && client.getAccountHash() == active.getHash();
        int total = live && client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX) > 0 ? client.getVarpValue(VarPlayerID.COLLECTION_COUNT) : -1;
        boolean complete = active != null && active.getCollectionLogHistory().hasSnapshot();
        boolean current = complete && (total < 0 || known == total);
        DateTimeFormatter verificationTime = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss").withZone(ZoneId.systemDefault());
        String logStatus = total >= 0 && known > total ? known + " known · mismatch" : current ? verificationTime.format(Instant.ofEpochMilli(active.getCollectionLogHistory().getSnapshotAt())) : known > 0 || total >= 0 ? known + (total < 0 ? " known" : " / " + total) + " · partial" : "Not loaded";
        statuses.put("Collection log", logStatus);
        if (!current) {
            missing.add("Collection log");
        }
        String captured = complete ? "<br>Last checked " + verificationTime.format(Instant.ofEpochMilli(active.getCollectionLogHistory().getSnapshotAt())) + "." : "<br>Load all unlocks to use collection milestones.";
        tips.put("Collection log", "<html>" + (current ? known + " unlocks loaded. Reopen your collection log to refresh." : known + (total < 0 ? " unlocks loaded." : " / " + total + " unlocks loaded.") + "<br>Open your collection log. Use its Search if unlocks are missing.") + captured + "</html>");
        int loaded = 0;
        Instant latestOther = null;
        StringBuilder other = new StringBuilder("<html>Open storage in game to refresh its saved contents.");
        for (PlayerStorageSource source : PlayerStorageSource.values()) {
            if (source == PlayerStorageSource.INVENTORY || source == PlayerStorageSource.EQUIPMENT || source == PlayerStorageSource.BANK) {
                continue;
            }
            Instant capturedAt = seen.get(source);
            if (capturedAt != null) {
                loaded++;
                if (latestOther == null || capturedAt.isAfter(latestOther)) {
                    latestOther = capturedAt;
                }
            }
            other.append("<br>").append(HtmlUtils.escape(source.toString())).append(": ").append(capturedAt == null ? "Not loaded" : time.format(capturedAt));
        }
        statuses.put("Other storage", latestOther == null ? "Not loaded" : time.format(latestOther));
        if (loaded < PlayerStorageSource.values().length - 3) {
            missing.add("Other storage");
        }
        tips.put("Other storage", other.append("</html>").toString());
    }
}
