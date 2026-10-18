package com.grinder.ui;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.GrinderRepository;
import java.awt.Component;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class ProfileBackupActions {
    private final ClientThread clientThread;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicInteger generation = new AtomicInteger();
    private volatile Future<?> task;
    @Inject public ProfileBackupActions(ClientThread clientThread, ScheduledExecutorService executor) { this.clientThread = clientThread; this.executor = executor; }
    public void cancel() {
        generation.incrementAndGet();
        if (task != null) { task.cancel(true); task = null; }
        busy.set(false);
    }
    public void show(Component owner, GrinderProfile active, BooleanSupplier live, Runnable refresh, boolean importing) {
        if (active == null) { JOptionPane.showMessageDialog(owner, "Log in before exporting or restoring a profile."); return; }
        if (!busy.compareAndSet(false, true)) { JOptionPane.showMessageDialog(owner, "A backup operation is already running."); return; }
        int token = generation.get();
        Filepath.Chooser chooser = new Filepath.Chooser().setAcceptsFiles().addExtensionFilter("Grinder JSON backup", "json").setDefaultExtension("json").setDialogTitle(importing ? "Import Grinder backup" : "Export Grinder backup");
        if (importing) { chooser.setIsOpen(); } else { chooser.setIsSave().setFileName("grinder-backup.json"); }
        List<Filepath> selected;
        try { selected = chooser.showDialog(owner); }
        catch (RuntimeException exception) { fail(owner, live, token, exception); return; }
        if (selected == null || selected.isEmpty() || !live.getAsBoolean()) { busy.set(false); return; }
        Filepath file = selected.get(0);
        if (!importing && file.exists() && JOptionPane.showConfirmDialog(owner, "Replace the selected backup file?", "Overwrite backup", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { busy.set(false); return; }
        if (importing) {
            task = executor.submit(() -> {
                try {
                    if (file.size() > 5_000_000) { throw new IllegalArgumentException("Backup must be smaller than 5 MB."); }
                    StringBuilder json = new StringBuilder();
                    try (Reader reader = file.openReader()) {
                        char[] buffer = new char[8192];
                        int count;
                        while ((count = reader.read(buffer)) != -1) {
                            if (Thread.currentThread().isInterrupted()) { return; }
                            json.append(buffer, 0, count);
                            if (json.length() > 5_000_000) { throw new IllegalArgumentException("Backup must be smaller than 5 MB."); }
                        }
                    }
                    GrinderProfile restored = GrinderRepository.importBackup(json.toString(), active.getHash());
                    SwingUtilities.invokeLater(() -> {
                        if (token != generation.get() || !live.getAsBoolean()) { return; }
                        String prompt = "Merge " + restored.getGoals().size() + " saved goals? Existing matches and edited targets are kept. Observed progress from other accounts is not copied.";
                        if (JOptionPane.showConfirmDialog(owner, prompt, "Restore missing goals", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { busy.set(false); return; }
                        clientThread.invokeLater(() -> {
                            if (token != generation.get() || !live.getAsBoolean()) { return; }
                            int added = active.mergeBackup(restored);
                            refresh.run();
                            finish(owner, live, token, added + " missing goals restored. Existing matches were kept.");
                        });
                    });
                } catch (Exception exception) { fail(owner, live, token, exception); }
            });
        } else {
            clientThread.invokeLater(() -> {
                if (token != generation.get() || !live.getAsBoolean()) { return; }
                try {
                    String snapshot = GrinderRepository.exportBackup(active);
                    if (snapshot.length() > 5_000_000) { throw new IllegalArgumentException("Backup exceeds the 5 MB safety limit."); }
                    task = executor.submit(() -> {
                        try { file.write(snapshot); finish(owner, live, token, "Backup exported. Keep it private: it includes account-linked goals and history."); }
                        catch (Exception exception) { fail(owner, live, token, exception); }
                    });
                } catch (Exception exception) { fail(owner, live, token, exception); }
            });
        }
    }
    private void finish(Component owner, BooleanSupplier live, int token, String message) {
        SwingUtilities.invokeLater(() -> {
            if (token != generation.get()) { return; }
            busy.set(false);
            if (live.getAsBoolean()) { JOptionPane.showMessageDialog(owner, message); }
        });
    }
    private void fail(Component owner, BooleanSupplier live, int token, Exception exception) { finish(owner, live, token, "Backup operation failed: " + exception.getMessage()); }
}
