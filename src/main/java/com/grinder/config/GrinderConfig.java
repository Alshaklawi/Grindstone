package com.grinder.config;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("grinder")
public interface GrinderConfig extends Config
{
	String GENERAL_SECTION = "general";
	String COLLECTION_LOG_SECTION = "collectionLog";
	String DISPLAY_SECTION = "display";

	@ConfigSection(name = "General", description = "General Grinder settings.", position = 0)
	String generalSection = GENERAL_SECTION;

	@ConfigSection(name = "Collection Log", description = "Collection log settings.", position = 1)
	String collectionLogSection = COLLECTION_LOG_SECTION;

	@ConfigSection(name = "Display", description = "Display settings.", position = 2)
	String displaySection = DISPLAY_SECTION;

	@ConfigItem(keyName = "enabled", name = "Enable Grinder", description = "Enables Grinder.", section = GENERAL_SECTION, position = 0)
	default boolean enabled()
	{
		return true;
	}

	@ConfigItem(keyName = "saveOnLogout", name = "Save on Logout", description = "Saves the active profile when logging out.", section = GENERAL_SECTION, position = 1)
	default boolean saveOnLogout()
	{
		return true;
	}

	@ConfigItem(keyName = "saveOnShutdown", name = "Save on Shutdown", description = "Saves the active profile when Grinder shuts down.", section = GENERAL_SECTION, position = 2)
	default boolean saveOnShutdown()
	{
		return true;
	}

	@ConfigItem(keyName = "collectionLogEnabled", name = "Collection Log Goals", description = "Displays collection log goals.", section = COLLECTION_LOG_SECTION, position = 0)
	default boolean collectionLogEnabled()
	{
		return true;
	}

	@ConfigItem(keyName = "showObtainedItems", name = "Show Obtained Items", description = "Displays items already obtained in the collection log.", section = COLLECTION_LOG_SECTION, position = 1)
	default boolean showObtainedItems()
	{
		return true;
	}

	@ConfigItem(keyName = "showItemTooltips", name = "Show Item Tooltips", description = "Displays item tooltips.", section = COLLECTION_LOG_SECTION, position = 2)
	default boolean showItemTooltips()
	{
		return true;
	}

	@ConfigItem(keyName = "showGoalIcons", name = "Show Goal Icons", description = "Displays goal icons.", section = DISPLAY_SECTION, position = 0)
	default boolean showGoalIcons()
	{
		return true;
	}

	@ConfigItem(keyName = "showProgressBar", name = "Show Progress Bar", description = "Displays goal progress bars.", section = DISPLAY_SECTION, position = 1)
	default boolean showProgressBar()
	{
		return true;
	}

	@Range(min = 1, max = 8)
	@ConfigItem(keyName = "itemsPerRow", name = "Items Per Row", description = "Number of items displayed per row.", section = DISPLAY_SECTION, position = 2)
	default int itemsPerRow()
	{
		return 4;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(keyName = "maxItemRows", name = "Maximum Item Rows", description = "Maximum visible item rows before scrolling.", section = DISPLAY_SECTION, position = 3)
	default int maxItemRows()
	{
		return 5;
	}
}