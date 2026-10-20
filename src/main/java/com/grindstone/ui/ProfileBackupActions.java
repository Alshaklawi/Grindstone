package com.grindstone.ui;

import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.repository.GrindstoneRepository;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
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
    private final GrindstoneRepository repository;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicInteger generation = new AtomicInteger();
    private volatile Future<?> task;

    @Inject
    public ProfileBackupActions(ClientThread clientThread, ScheduledExecutorService executor, GrindstoneRepository repository) {
        this.clientThread = clientThread;
        this.executor = executor;
        this.repository = repository;
    }

    public void cancel() {
        generation.incrementAndGet();
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        busy.set(false);
    }

    public void show(Component owner, GrindstoneProfile active, BooleanSupplier live, Runnable refresh, boolean importing) {
        if (active == null) {
            JOptionPane.showMessageDialog(DialogPlacement.getOwner(owner), "Log in before exporting or restoring a profile.");
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            JOptionPane.showMessageDialog(DialogPlacement.getOwner(owner), "A backup operation is already running.");
            return;
        }
        int token = generation.get();
        Filepath.Chooser chooser = new Filepath.Chooser().setAcceptsFiles().addExtensionFilter("Grindstone JSON profile", "json").setDefaultExtension("json").setDialogTitle(importing ? "Import Grindstone backup" : "Export Grindstone backup");
        if (importing) {
            chooser.setIsOpen();
        } else {
            chooser.setIsSave().setFileName("grindstone-backup.json");
        }
        List<Filepath> selected;
        try {
            selected = chooser.showDialog(DialogPlacement.getOwner(owner));
        } catch (RuntimeException failure) {
            fail(owner, live, token, failure);
            return;
        }
        if (selected == null || selected.isEmpty() || !live.getAsBoolean()) {
            busy.set(false);
            return;
        }
        Filepath file = selected.get(0);
        if (importing) {
            task = executor.submit(() -> {
                try {
                    GrindstoneProfile restored = repository.importBackup(repository.readText(file, 5_000_000), active.getHash());
                    SwingUtilities.invokeLater(() -> {
                        if (token != generation.get() || !live.getAsBoolean()) {
                            return;
                        }
                        String prompt = "Merge " + restored.getGoals().size() + " saved milestones? Existing matches and edited targets are kept. Observed progress from other accounts is not copied.";
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(owner), prompt, "Restore missing milestones", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                            busy.set(false);
                            return;
                        }
                        clientThread.invokeLater(() -> {
                            if (token != generation.get() || !live.getAsBoolean()) {
                                return;
                            }
                            int added = active.mergeBackup(restored);
                            refresh.run();
                            finish(owner, live, token, added + " missing milestones restored. Existing matches were kept.");
                        });
                    });
                } catch (Exception failure) {
                    fail(owner, live, token, failure);
                }
            });
        } else {
            task = executor.submit(() -> {
                boolean exists = file.exists();
                SwingUtilities.invokeLater(() -> {
                    if (token != generation.get() || !live.getAsBoolean()) {
                        return;
                    }
                    if (exists && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(owner), "Replace the selected backup file?", "Overwrite backup", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                        busy.set(false);
                        return;
                    }
                    export(owner, active, live, token, file, exists);
                });
            });
        }
    }

    private void export(Component owner, GrindstoneProfile active, BooleanSupplier live, int token, Filepath file, boolean overwrite) {
        clientThread.invokeLater(() -> {
            if (token != generation.get() || !live.getAsBoolean()) {
                return;
            }
            try {
                String snapshot = repository.exportBackup(active);
                if (snapshot.length() > 5_000_000 || snapshot.getBytes(StandardCharsets.UTF_8).length > 5_000_000) {
                    throw new IllegalArgumentException("Backup exceeds the 5 MB safety limit.");
                }
                task = executor.submit(() -> {
                    if (token != generation.get()) {
                        return;
                    }
                    try {
                        if (overwrite) {
                            file.write(snapshot, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                        } else {
                            file.write(snapshot, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
                        }
                        finish(owner, live, token, "Backup exported. Keep it private: it includes account-linked milestones and history.");
                    } catch (Exception failure) {
                        fail(owner, live, token, failure);
                    }
                });
            } catch (Exception failure) {
                fail(owner, live, token, failure);
            }
        });
    }

    private void finish(Component owner, BooleanSupplier live, int token, String message) {
        SwingUtilities.invokeLater(() -> {
            if (token != generation.get()) {
                return;
            }
            busy.set(false);
            if (live.getAsBoolean()) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(owner), message);
            }
        });
    }

    private void fail(Component owner, BooleanSupplier live, int token, Exception failure) {
        finish(owner, live, token, "Backup operation failed: " + failure.getMessage());
    }
}
