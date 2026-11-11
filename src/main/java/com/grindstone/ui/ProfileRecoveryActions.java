package com.grindstone.ui;

import com.google.gson.JsonObject;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.GrindstoneRepository;
import com.grindstone.repository.ProfilePersistence;
import com.grindstone.ui.util.DialogPlacement;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public final class ProfileRecoveryActions {
    private final ProfilePersistence persistence;
    private final GrindstoneRepository repository;
    private final ClientThread clientThread;
    private boolean busy;

    @Inject
    public ProfileRecoveryActions(ProfilePersistence persistence, GrindstoneRepository repository, ClientThread clientThread) {
        this.persistence = persistence;
        this.repository = repository;
        this.clientThread = clientThread;
    }

    public void show(Component owner, long hash, BooleanSupplier live, Consumer<GrindstoneProfile> activate) {
        if (busy || !live.getAsBoolean()) {
            return;
        }
        busy = true;
        persistence.getRecoveryState(hash).whenComplete((state, failure) -> SwingUtilities.invokeLater(() -> {
            busy = false;
            if (!live.getAsBoolean()) {
                return;
            }
            if (failure != null) {
                fail(owner, failure);
                return;
            }
            List<Object> choices = new ArrayList<>(state.getOptions());
            choices.add("Import backup...");
            Object selected = JOptionPane.showInputDialog(DialogPlacement.getOwner(owner), "Choose a profile version. Both versions are kept as recovery copies.", "Recover profile", JOptionPane.QUESTION_MESSAGE, null, choices.toArray(), choices.get(0));
            if (selected == null || !live.getAsBoolean()) {
                return;
            }
            if (selected instanceof ProfilePersistence.RecoveryOption) {
                confirm(owner, hash, state, (ProfilePersistence.RecoveryOption) selected, live, activate);
            } else {
                Filepath.Chooser chooser = new Filepath.Chooser().setAcceptsFiles().setIsOpen().addExtensionFilter("Grindstone JSON profile", "json").setDialogTitle("Import recovery backup");
                List<Filepath> files;
                try {
                    files = chooser.showDialog(DialogPlacement.getOwner(owner));
                } catch (RuntimeException problem) {
                    fail(owner, problem);
                    return;
                }
                if (files == null || files.isEmpty() || !live.getAsBoolean()) {
                    return;
                }
                busy = true;
                persistence.enqueue(() -> repository.importBackup(repository.readText(files.get(0), 5_000_000), hash)).whenComplete((restored, problem) -> SwingUtilities.invokeLater(() -> {
                    busy = false;
                    if (!live.getAsBoolean()) {
                        return;
                    }
                    if (problem != null) {
                        fail(owner, problem);
                    } else {
                        clientThread.invokeLater(() -> {
                            JsonObject snapshot = repository.toJson(restored);
                            SwingUtilities.invokeLater(() -> confirm(owner, hash, state, new ProfilePersistence.RecoveryOption("Imported backup", snapshot), live, activate));
                        });
                    }
                }));
            }
        }));
    }

    private void confirm(Component owner, long hash, ProfilePersistence.RecoveryState state, ProfilePersistence.RecoveryOption option, BooleanSupplier live, Consumer<GrindstoneProfile> activate) {
        if (!live.getAsBoolean() || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(owner), "Use " + option + "?\nThe selected version will become active. Existing versions will be kept as recovery copies.", "Confirm recovery", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION || !live.getAsBoolean()) {
            return;
        }
        busy = true;
        persistence.recover(hash, option.getSnapshot(), state.getRemote()).whenComplete((restored, failure) -> {
            SwingUtilities.invokeLater(() -> {
                busy = false;
                if (failure != null && live.getAsBoolean()) {
                    fail(owner, failure);
                }
            });
            if (failure == null) {
                clientThread.invokeLater(() -> {
                    if (live.getAsBoolean()) {
                        activate.accept(restored);
                    }
                });
            }
        });
    }

    private static void fail(Component owner, Throwable failure) {
        Throwable cause = failure instanceof CompletionException ? failure.getCause() : failure;
        JOptionPane.showMessageDialog(DialogPlacement.getOwner(owner), "Recovery could not finish: " + cause.getMessage(), "Recover profile", JOptionPane.ERROR_MESSAGE);
    }
}
