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
        String capturedCatchUp = "Progress since the previous Grindstone reading:\n\n" + String.join("\n", catchUp) + "\n\nUpdated from your character's available game data when you returned to RuneLite. Exact achievement times and gains across routine resets cannot be reconstructed. Open the collection log, bank or relevant activity interface to refresh data that is not loaded yet.";
        return new SidebarStatusSnapshot(Collections.unmodifiableMap(new LinkedHashMap<>(snapshotStatuses)), Collections.unmodifiableMap(new LinkedHashMap<>(snapshotTips)), Collections.unmodifiableSet(new LinkedHashSet<>(missingSnapshots)), dueCount, catchUp.size(), capturedCatchUp);
    }

    private static void prepareSnapshotStatuses(Client client, GrindstoneProfile active, Map<String, String> statuses, Map<String, String> tips, Set<String> missing) {
        Map<PlayerStorageSource, Instant> seen = active == null ? Collections.emptyMap() : active.getPlayerStorage().getLastSeen();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
        for (PlayerStorageSource source : new PlayerStorageSource[]{PlayerStorageSource.INVENTORY, PlayerStorageSource.EQUIPMENT, PlayerStorageSource.BANK}) {
            Instant captured = seen.get(source);
            statuses.put(source.toString(), captured == null ? "Not loaded" : time.format(captured));
            if (captured == null) {
                missing.add(source.toString());
            }
            String refresh = source == PlayerStorageSource.BANK ? "Open your bank to refresh its contents." : "Updated automatically while logged in.";
            tips.put(source.toString(), "<html>" + refresh + "<br>" + (captured == null ? "No snapshot saved yet." : "Last updated " + time.format(captured) + (active.getPlayerStorage().isLoadedThisSession(source) ? " this login." : ". Saved from an earlier session; reopen to refresh.")) + "<br>Only this character's known contents are counted.</html>");
        }
        int known = active == null ? 0 : active.getCollectionLogHistory().getKnownUnlockCount();
        boolean live = active != null && client.getGameState() == GameState.LOGGED_IN && client.getAccountHash() == active.getHash();
        int total = live && client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX) > 0 ? client.getVarpValue(VarPlayerID.COLLECTION_COUNT) : -1;
        boolean complete = active != null && active.getCollectionLogHistory().hasSnapshot();
        boolean current = complete && (total < 0 || known == total);
        String logStatus = total >= 0 && known > total ? known + " known · mismatch" : current ? time.format(Instant.ofEpochMilli(active.getCollectionLogHistory().getSnapshotAt())) : known > 0 || total >= 0 ? known + (total < 0 ? " known" : " / " + total) + " · partial" : "Not loaded";
        statuses.put("Collection log", logStatus);
        if (!current) {
            missing.add("Collection log");
        }
        String captured = complete ? "<br>Last full capture: " + time.format(Instant.ofEpochMilli(active.getCollectionLogHistory().getSnapshotAt())) + "." : "<br>No full snapshot yet; collection milestones remain unavailable.";
        tips.put("Collection log", "<html>Open your in-game collection log to capture available unlocks.<br>" + (current ? "Full snapshot verified: " + known + " unlocks." : "Incomplete data: " + known + " known unlocks" + (total < 0 ? "." : " out of " + total + ".") + "<br>The game may not expose every unlock just by opening the log.<br>Its Search interface can expose further collection data.") + captured + "<br>Reopen after playing on another device.</html>");
        int loaded = 0;
        Instant latestOther = null;
        StringBuilder other = new StringBuilder("<html>Open each storage in game to refresh it. Last known snapshots are saved between sessions; reopen storage to refresh.");
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
