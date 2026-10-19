
package com.grinder.content.goal;

import com.grinder.config.GrinderConfig;
import com.grinder.ui.Icon;
import com.grinder.ui.components.GoalComponent;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public interface Goal {
    default boolean supportsTarget() {
        return false;
    }

    default void setTarget(long target) {
        throw new UnsupportedOperationException("This goal tracks completion rather than a target");
    }

    default String getTitle(Client client) {
        return getClass().getSimpleName();
    }

    default String getDescription() {
        return "";
    }

    default String getCardDescription() {
        return getDescription();
    }

    default BufferedImage getIcon(Client client) {
        return Icon.MAIN.getImage();
    }

    default GoalStatus getStatus() {
        return GoalStatus.NOT_STARTED;
    }

    default GoalCategory getCategory() {
        return GoalCategory.fromName(getClass().getSimpleName());
    }

    default boolean hasProgress() {
        return false;
    }

    default long getProgressCurrent() {
        return 0;
    }

    default long getProgressMaximum() {
        return 0;
    }

    default String getProgressText() {
        return hasProgress() ? getProgressCurrent() + " / " + getProgressMaximum() : "";
    }

    default GoalComponent build(Client client) {
        GoalComponent component = new GoalComponent();
        component.setTitle(getTitle(client));
        component.setDescription(getCardDescription());
        component.setIcon(getIcon(client));

        if (hasProgress()) {
            component.setProgress(getProgressCurrent(), getProgressMaximum(), getProgressText());
        } else {
            component.setProgressStatus(getStatus().name());
        }

        return component;
    }

    default GoalComponent build(Client client, GrinderConfig config) {
        GoalComponent component = build(client);
        component.setIconVisible(config == null || config.showGoalIcons());
        component.setProgressBarVisible(config == null || config.showProgressBar());
        return component;
    }

    default void onItemContainerUpdated(Client client, ItemContainer container) {
    }

    default boolean refresh(Client client) {
        return false;
    }

    default boolean isAffectedBy(ItemContainer container) {
        return container != null;
    }
}
