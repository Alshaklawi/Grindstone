package com.grindstone.repository;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.quest.QuestMilestone;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.ui.editor.CompletionMilestoneChoices;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public class FracturedArchiveMilestoneTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void raidPickerCreatesMilestoneAndReadsCompletionCounter() {
        TrackerMilestone milestone = (TrackerMilestone) CompletionMilestoneChoices.create(null).get(23).get("The Fractured Archive").get();
        assertEquals(TrackerEntry.THE_FRACTURED_ARCHIVE, milestone.getTracker());
        assertEquals(MilestoneCategory.RAIDS, milestone.getCategory());
        milestone.setTarget(10);
        AtomicInteger completions = new AtomicInteger(8);
        Client client = createClient(completions);
        assertTrue(milestone.refresh(client));
        assertEquals(8, milestone.getProgressCurrent());
        assertEquals(MilestoneStatus.IN_PROGRESS, milestone.getStatus());
        assertFalse(milestone.refresh(client));
        completions.set(10);
        assertTrue(milestone.refresh(client));
        assertEquals(MilestoneStatus.COMPLETE, milestone.getStatus());
    }

    @Test
    public void savesAndReloadsRaidTargetsAndRecurringProgress() throws Exception {
        long now = Instant.parse("2026-11-04T12:00:00Z").toEpochMilli();
        AtomicInteger completions = new AtomicInteger(30);
        Client client = createClient(completions);
        GrindstoneProfile profile = new GrindstoneProfile(42);
        TrackerMilestone milestone = new TrackerMilestone(TrackerEntry.THE_FRACTURED_ARCHIVE, 100);
        profile.add(milestone);
        milestone.refresh(client);
        RecurringMilestone routine = profile.getMetadata(milestone).getRecurring();
        routine.configure(RepeatSchedule.WEEKLY, 5, now);
        routine.observe(milestone, now);
        completions.set(32);
        milestone.refresh(client);
        routine.observe(milestone, now + 1000);
        assertEquals(2, routine.getProgress());
        Filepath root = Filepath.Unchecked.getRooted(temporary.newFolder().toPath());
        TestProfiles.REPOSITORY.save(root, TestProfiles.REPOSITORY.toJson(profile));
        GrindstoneProfile restored = TestProfiles.REPOSITORY.load(root, 42);
        assertEquals(1, restored.getGoals().size());
        TrackerMilestone saved = (TrackerMilestone) restored.getGoals().get(0);
        assertEquals(TrackerEntry.THE_FRACTURED_ARCHIVE, saved.getTracker());
        assertEquals(100, saved.getProgressMaximum());
        RecurringMilestone savedRoutine = restored.getMetadata(saved).getRecurring();
        assertEquals(RepeatSchedule.WEEKLY, savedRoutine.getSchedule());
        assertEquals(5, savedRoutine.getTarget());
        assertEquals(2, savedRoutine.getProgress());
        completions.set(33);
        saved.refresh(client);
        savedRoutine.observe(saved, now + 2000);
        assertEquals(3, savedRoutine.getProgress());
    }

    @Test
    public void miniquestIsAvailableAsASeparateQuestMilestone() {
        QuestMilestone miniquest = (QuestMilestone) CompletionMilestoneChoices.create(null).get(9).get(Quest.THE_FRACTURED_ARCHIVE.getName()).get();
        assertEquals(Quest.THE_FRACTURED_ARCHIVE.name(), miniquest.selectionKey());
        assertEquals(MilestoneCategory.QUEST, miniquest.getCategory());
    }

    private Client createClient(AtomicInteger completions) {
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class}, (proxy, method, arguments) -> {
            if ("getVarpValue".equals(method.getName())) {
                assertEquals(VarPlayerID.TOTAL_COMPLETED_THEFRACTUREDARCHIVE, arguments[0]);
                return completions.get();
            }
            throw new AssertionError("Unexpected client read: " + method.getName());
        });
    }
}
