package com.grinder.content.goal;

import com.grinder.content.utils.Icon;
import com.grinder.ui.GoalComponent;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public interface Goal {

    default String getTitle(Client client) {
        return "Unknown goal name.";
    }

    default BufferedImage getIcon(Client client) {
        return Icon.MAIN.getImage();
    }

    default LocalDateTime getStart() {
        return LocalDateTime.now();
    }

    default LocalDateTime getEnd() {
        return LocalDateTime.now();
    }

    default GoalStatus getStatus() {
        return GoalStatus.NOT_STARTED;
    }


    default GoalComponent build(Client client) {
        final var component = new GoalComponent();
        component.getContent().add(new JLabel(getTitle(client) + "|" + getClass().getSimpleName()));
        return component;
    }

    default void onItemContainerUpdated(Client client, ItemContainer container) {

    }
}
