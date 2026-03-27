package com.leon.bugreport;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PendingRewardNotifier {
	private static final String FILE_NAME = "pending_rewards.yml";
	private static File file;
	private static YamlConfiguration config;

	public static void init(@NotNull File dataFolder) {
		file = new File(dataFolder, FILE_NAME);
		config = YamlConfiguration.loadConfiguration(file);
	}

	public static void addPendingMessage(@NotNull UUID playerUUID, @NotNull String message) {
		String key = playerUUID.toString();
		List<String> messages = new ArrayList<>(config.getStringList(key));
		messages.add(message);
		config.set(key, messages);
		save();
	}

	public static @NotNull List<String> getAndClearPendingMessages(@NotNull UUID playerUUID) {
		String key = playerUUID.toString();
		List<String> messages = config.getStringList(key);
		if (!messages.isEmpty()) {
			config.set(key, null);
			save();
		}
		return messages;
	}

	private static void save() {
		try {
			config.save(file);
		} catch (IOException e) {
			BugReportPlugin.getPlugin().getLogger().warning("Failed to save pending_rewards.yml: " + e.getMessage());
		}
	}
}
