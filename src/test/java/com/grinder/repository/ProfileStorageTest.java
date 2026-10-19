package com.grinder.repository;

import com.google.gson.JsonObject;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Filepath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@PluginDescriptor(internalName = "grinder-storage-test", name = "Grinder storage tests")
public class ProfileStorageTest extends Plugin {
    private Filepath directory;

    @Before
    public void createDirectory() throws IOException {
        Filepath root = getPluginDirectory();
        root.createDirectories();
        directory = root.createTempDir("case-");
        assertTrue(directory.startsWith(root));
        assertTrue(directory.toString().replace('\\', '/').contains("/build/test-home/"));
    }

    @After
    public void removeDirectory() throws IOException {
        if (directory != null) {
            directory.deleteRecursively();
        }
    }

    private GrinderProfile profile(long target) {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal goal = new CustomGoal("Stored goal", "Keep notes", GoalCategory.OTHER, 0, target);
        profile.replace(Collections.singletonList(goal));
        profile.metadata(goal).setPinned(true);
        profile.metadata(goal).recurring().configure(RepeatSchedule.DAILY, target, 1000);
        return profile;
    }

    @Test
    public void roundTripKeepsTargetsAndMetadataAndCleansTemporaryFiles() throws Exception {
        GrinderRepository repository = TestProfiles.REPOSITORY;
        repository.save(directory, repository.toJson(profile(7)));
        GrinderProfile loaded = repository.load(directory, 42);
        assertEquals(7, loaded.getGoals().get(0).getProgressMaximum());
        assertTrue(loaded.metadata(loaded.getGoals().get(0)).isPinned());
        assertEquals(RepeatSchedule.DAILY, loaded.metadata(loaded.getGoals().get(0)).recurring().getSchedule());
        try (Stream<Filepath> files = directory.walk(1)) {
            assertFalse(files.anyMatch(file -> file.getFileName().endsWith(".tmp")));
        }
    }

    @Test
    public void corruptPrimaryRecoversPreviousValidSaveAndPreservesCorruptInput() throws Exception {
        GrinderRepository repository = TestProfiles.REPOSITORY;
        repository.save(directory, repository.toJson(profile(7)));
        repository.save(directory, repository.toJson(profile(9)));
        directory.joinSegment("42.json").write("{broken");
        assertEquals(7, repository.load(directory, 42).getGoals().get(0).getProgressMaximum());
        repository.save(directory, repository.toJson(profile(11)));
        assertEquals(11, repository.load(directory, 42).getGoals().get(0).getProgressMaximum());
        assertEquals(7, repository.readProfile(directory.joinSegment("42.json.bak"), 42).getGoals().get(0).getProgressMaximum());
        try (Stream<Filepath> files = directory.walk(1)) {
            assertTrue(files.anyMatch(file -> file.getFileName().startsWith("42.corrupt-")));
        }
    }

    @Test
    public void invalidSavedDataIsNotSilentlyReplacedByDefaults() throws Exception {
        directory.joinSegment("42.json").write("{broken");
        try {
            TestProfiles.REPOSITORY.load(directory, 42);
            fail("Invalid saves must not become default goals");
        } catch (IOException expected) {
            assertEquals("{broken", TestProfiles.REPOSITORY.readText(directory.joinSegment("42.json"), 100));
        }
    }

    @Test
    public void wrongAccountAndMalformedMetadataAreRejectedWithoutChangingSource() throws Exception {
        GrinderRepository repository = TestProfiles.REPOSITORY;
        JsonObject saved = repository.toJson(profile(7));
        directory.joinSegment("legacy.json").write(saved.toString());
        try {
            repository.readProfile(directory.joinSegment("legacy.json"), 99);
            fail("Account mismatch must be rejected");
        } catch (IOException expected) {
            assertTrue(directory.joinSegment("legacy.json").exists());
        }
        saved.getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonObject("metadata").addProperty("id", "invalid");
        directory.joinSegment("legacy.json").write(saved.toString());
        try {
            repository.readProfile(directory.joinSegment("legacy.json"), 42);
            fail("Invalid history must not be silently dropped");
        } catch (IllegalArgumentException expected) {
            assertTrue(directory.joinSegment("legacy.json").exists());
        }
    }

    @Test
    public void aCapturedSnapshotDoesNotFollowLaterManualEdits() throws Exception {
        GrinderProfile profile = profile(7);
        JsonObject snapshot = TestProfiles.REPOSITORY.toJson(profile);
        profile.getGoals().get(0).setTarget(20);
        TestProfiles.REPOSITORY.save(directory, snapshot);
        assertEquals(7, TestProfiles.REPOSITORY.load(directory, 42).getGoals().get(0).getProgressMaximum());
    }
}
