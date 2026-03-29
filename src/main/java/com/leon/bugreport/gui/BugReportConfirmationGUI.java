package com.leon.bugreport.gui;

import com.leon.bugreport.BugReportDatabase;
import com.leon.bugreport.BugReportLanguage;
import com.leon.bugreport.PendingRewardNotifier;
import com.leon.bugreport.discord.LinkDiscord;
import com.leon.bugreport.extensions.EcoHook;
import com.leon.bugreport.keys.guiTextures;
import com.leon.bugreport.listeners.ReportArchivedEvent;
import com.leon.bugreport.listeners.ReportDeletedEvent;
import com.leon.bugreport.logging.ErrorMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportLanguage.getKeyFromTranslation;
import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.BugReportSettings.createCustomPlayerHead;
import static com.leon.bugreport.gui.bugreportGUI.openBugReportDetailsGUI;

public class BugReportConfirmationGUI {
	public static void openConfirmationGUI(@NotNull Player player, @NotNull Boolean isArchived) {
		player.openInventory(getConfirmationGUI(isArchived));
	}

	public static @NotNull Inventory getConfirmationGUI(boolean isArchived) {
		String guiTitle;

		if (isArchived) {
			guiTitle = ChatColor.YELLOW + Objects.requireNonNull(BugReportLanguage.getValueFromLanguageFile("buttonNames.confirmationArchive", "Archive Bug Report?"));
		} else {
			guiTitle = ChatColor.YELLOW + Objects.requireNonNull(BugReportLanguage.getValueFromLanguageFile("buttonNames.confirmationDelete", "Delete Bug Report?"));
		}

		Inventory gui = Bukkit.createInventory(null, 27, guiTitle);
		ItemStack backButton = createButton(Material.BARRIER, ChatColor.YELLOW + BugReportLanguage.getValueFromLanguageFile("buttonNames.back", "Back"));
		gui.setItem(15, backButton);

		if (isArchived) {
			ItemStack archiveButton = createCustomPlayerHead(guiTextures.archiveTexture, ChatColor.YELLOW + BugReportLanguage.getValueFromLanguageFile("buttonNames.archive", "Archive"), 16);
			gui.setItem(11, archiveButton);
		} else {
			ItemStack deleteButton = createCustomPlayerHead(guiTextures.deleteTexture, ChatColor.YELLOW + BugReportLanguage.getValueFromLanguageFile("buttonNames.delete", "Delete"), 18);
			gui.setItem(11, deleteButton);
		}

		return gui;
	}

	private static void promptEcoAmount(@NotNull Player player, @NotNull Consumer<Double> callback) {
		if (!EcoHook.isEnabled()) {
			callback.accept(0.0);
			return;
		}

		player.sendMessage(returnStartingMessage(ChatColor.YELLOW)
				+ " Enter the reward amount for the bug reporter (or type NONE/0 to skip):");

		Listener chatListener = new Listener() {
			@EventHandler
			public void onChat(AsyncPlayerChatEvent event) {
				if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;

				event.setCancelled(true);
				HandlerList.unregisterAll(this);

				String input = event.getMessage().trim();
				double amount = 0.0;

				if (!input.equalsIgnoreCase("NONE") && !input.equals("0")) {
					try {
						double parsed = Double.parseDouble(input);
						if (parsed > 0) {
							amount = parsed;
						}
					} catch (NumberFormatException ignored) {
						Bukkit.getScheduler().runTask(plugin, () ->
								player.sendMessage(returnStartingMessage(ChatColor.RED)
										+ " Invalid amount. No reward will be given."));
					}
				}

				final double finalAmount = amount;
				Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalAmount));
			}
		};

		Bukkit.getPluginManager().registerEvents(chatListener, plugin);
	}

	private static void giveEcoRewardToReporter(@NotNull Player admin, int reportIDGUI,
												 @NotNull Map<String, String> reportData, double amount) {
		if (amount <= 0 || !EcoHook.isEnabled()) return;

		String uuidStr = reportData.get("UUID");
		String username = reportData.get("Username");
		if (uuidStr == null) return;

		try {
			UUID reporterUUID = UUID.fromString(uuidStr);
			boolean success = EcoHook.giveReward(reporterUUID, amount);
			if (success) {
				admin.sendMessage(returnStartingMessage(ChatColor.GREEN)
						+ " Awarded $" + amount + " to " + username + " for bug report #" + reportIDGUI + ".");
				String rewardMessage = returnStartingMessage(ChatColor.GREEN)
						+ " You received $" + amount + " as a reward for your bug report #" + reportIDGUI + "!";
				Player reporter = Bukkit.getPlayer(reporterUUID);
				if (reporter != null) {
					reporter.sendMessage(rewardMessage);
				} else {
					PendingRewardNotifier.addPendingMessage(reporterUUID, rewardMessage);
				}
			}
		} catch (IllegalArgumentException ignored) {}
	}

	public record BugReportConfirmationListener(
			Inventory gui,
			Integer reportIDGUI,
			Boolean fromArchivedGUI
	) implements Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String displayName = event.getView().getTitle();
			if (debugMode) {
				plugin.getLogger().info("Clicked inventory: " + displayName);
			}

			String customDisplayName = getKeyFromTranslation(displayName);
			if (customDisplayName == null || customDisplayName.equals(" ")) {
				return;
			}

			boolean isArchivedDetails = customDisplayName.equals("buttonNames.confirmationArchive");
			boolean isDeletedDetails = customDisplayName.equals("buttonNames.confirmationDelete");

			if (!isArchivedDetails && !isDeletedDetails) {
				String errorMessage = ErrorMessages.getErrorMessage(43);

				plugin.getLogger().severe(errorMessage);
				logErrorMessage(errorMessage);

				return;
			}

			event.setCancelled(true);

			Player player = (Player) event.getWhoClicked();
			Inventory clickedInventory = event.getClickedInventory();
			if (clickedInventory == null) {
				return;
			}

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) {
				return;
			}

			ItemMeta itemMeta = clickedItem.getItemMeta();
			if (itemMeta == null || !itemMeta.hasDisplayName()) {
				return;
			}

			String itemDisplayName = itemMeta.getDisplayName();
			String customItemDisplayName = getKeyFromTranslation(itemDisplayName);
			if (customItemDisplayName == null || customItemDisplayName.equals(" ")) {
				return;
			}

			if (debugMode) {
				plugin.getLogger().info("Clicked item: " + customItemDisplayName);
			}

			if (isArchivedDetails) {
				if (debugMode) {
					plugin.getLogger().info("Opening archived confirmation GUI.");
				}

				switch (customItemDisplayName) {
					case "buttonNames.archive" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Archiving report: " + reportIDGUI);
						}

						player.closeInventory();
						HandlerList.unregisterAll(this);

						// Fetch report data async, then prompt for eco amount on main thread,
						// then do DB write async, then open GUI back on main thread.
						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
							Map<String, String> reportData = BugReportDatabase.getBugReportById(reportIDGUI);
							String bugReportDiscordWebhookID = config.getBoolean("enableDiscordWebhook", true)
									? BugReportDatabase.getBugReportDiscordWebhookMessageID(reportIDGUI)
									: null;

							Bukkit.getScheduler().runTask(plugin, () ->
								promptEcoAmount(player, ecoAmount ->
									Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
										BugReportDatabase.updateBugReportArchive(reportIDGUI, 1);
										Bukkit.getScheduler().runTask(plugin, () -> {
											String rawUUID = reportData != null ? reportData.get("UUID") : null;
											String rawUsername = reportData != null ? reportData.get("Username") : null;
											java.util.UUID reporterUUID = null;
											try { if (rawUUID != null) reporterUUID = java.util.UUID.fromString(rawUUID); } catch (IllegalArgumentException ignored) {}
											Bukkit.getPluginManager().callEvent(new ReportArchivedEvent(player, reportIDGUI, ecoAmount, reporterUUID, rawUsername));

											if (config.getBoolean("enableDiscordWebhook", true)) {
												if (bugReportDiscordWebhookID != null) {
													if (debugMode) plugin.getLogger().info("Sending archive notification to Discord...");

													String Username = reportData.get("Username");
													String UUID = reportData.get("UUID");
													String World = reportData.get("World");
													String FullMessage = reportData.get("Full Message");
													String Category_ID = reportData.get("Category ID");
													if (Category_ID == null || Category_ID.equals("Unknown") || Category_ID.equals("null")) Category_ID = "0";
													Integer FinalCategory = Integer.valueOf(Category_ID);
													String Location = reportData.get("Location");
													String Gamemode = reportData.get("Gamemode");
													String Status = reportData.get("Status");
													String ServerName = reportData.get("Server Name");

													LinkDiscord.modifyNotification(
															Username, UUID, World, Location, Gamemode, Status,
															FinalCategory, ServerName, FullMessage,
															bugReportDiscordWebhookID, Color.ORANGE,
															"Bug Report #" + reportIDGUI + " has been archived."
													);
												} else {
													String errorMessage = ErrorMessages.getErrorMessage(25);
													plugin.getLogger().warning(errorMessage);
													logErrorMessage(errorMessage);
												}
											}

											player.sendMessage(returnStartingMessage(ChatColor.RED)
													+ " Bug Report #" + reportIDGUI + " has been archived.");
											player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage) : getBugReportGUI(localCurrentPage));
											giveEcoRewardToReporter(player, reportIDGUI, reportData, ecoAmount);
										});
									})
								)
							);
						});
					}
					case "buttonNames.back" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Going back to bug reports.");
						}
						returnFromConfirmationGUI(player);
					}
				}
			}

			if (isDeletedDetails) {
				if (debugMode) {
					plugin.getLogger().info("Opening delete confirmation GUI.");
				}

				switch (Objects.requireNonNull(customItemDisplayName)) {
					case "buttonNames.delete" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Deleting report: " + reportIDGUI);
						}

						player.closeInventory();
						HandlerList.unregisterAll(this);

						// Fetch report data async, then prompt for eco amount on main thread,
						// then do DB write async, then open GUI back on main thread.
						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
							Map<String, String> reportData = BugReportDatabase.getBugReportById(reportIDGUI);
							String bugReportDiscordWebhookID = config.getBoolean("enableDiscordWebhook", true)
									? BugReportDatabase.getBugReportDiscordWebhookMessageID(reportIDGUI)
									: null;

							Bukkit.getScheduler().runTask(plugin, () ->
								promptEcoAmount(player, ecoAmount ->
									Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
										BugReportDatabase.deleteBugReport(reportIDGUI);
										Bukkit.getScheduler().runTask(plugin, () -> {
											List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
											reports.removeIf(report -> report.contains("Report ID: " + reportIDGUI));

											String rawUUIDDel = reportData != null ? reportData.get("UUID") : null;
											String rawUsernameDel = reportData != null ? reportData.get("Username") : null;
											java.util.UUID reporterUUIDDel = null;
											try { if (rawUUIDDel != null) reporterUUIDDel = java.util.UUID.fromString(rawUUIDDel); } catch (IllegalArgumentException ignored) {}
											Bukkit.getPluginManager().callEvent(new ReportDeletedEvent(player, reportIDGUI, ecoAmount, reporterUUIDDel, rawUsernameDel));

											if (config.getBoolean("enableDiscordWebhook", true)) {
												if (bugReportDiscordWebhookID != null) {
													if (debugMode) plugin.getLogger().info("Sending deletion notification to Discord...");

													String Username = reportData.get("Username");
													String UUID = reportData.get("UUID");
													String World = reportData.get("World");
													String FullMessage = reportData.get("Full Message");
													String Category_ID = reportData.get("Category ID");
													if (Category_ID == null || Category_ID.equals("Unknown") || Category_ID.equals("null")) Category_ID = "0";
													Integer FinalCategory = Integer.valueOf(Category_ID);
													String Location = reportData.get("Location");
													String Gamemode = reportData.get("Gamemode");
													String Status = reportData.get("Status");
													String ServerName = reportData.get("Server Name");

													LinkDiscord.modifyNotification(
															Username, UUID, World, Location, Gamemode, Status,
															FinalCategory, ServerName, FullMessage,
															bugReportDiscordWebhookID, Color.RED,
															"Bug Report #" + reportIDGUI + " has been deleted."
													);
												} else {
													String errorMessage = ErrorMessages.getErrorMessage(25);
													plugin.getLogger().warning(errorMessage);
													logErrorMessage(errorMessage);
												}
											}

											player.sendMessage(returnStartingMessage(ChatColor.RED)
													+ " Bug Report #" + reportIDGUI + " has been deleted.");
											player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage) : getBugReportGUI(localCurrentPage));
											giveEcoRewardToReporter(player, reportIDGUI, reportData, ecoAmount);
										});
									})
								)
							);
						});
					}
					case "buttonNames.back" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Going back to archived reports.");
						}
						returnFromConfirmationGUI(player);
					}
				}
			}
		}

		private void returnFromConfirmationGUI(@NotNull Player player) {
			player.openInventory(getBugReportGUI(localCurrentPage));

			List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
			String report = reports.stream().filter(reportString -> reportString.contains("Report ID: " + reportIDGUI)).findFirst().orElse(null);

			openBugReportDetailsGUI(player, report, reportIDGUI, false);

			HandlerList.unregisterAll(this);
		}
	}
}
