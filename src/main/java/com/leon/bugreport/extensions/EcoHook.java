package com.leon.bugreport.extensions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EcoHook {
	private static Economy economy = null;

	public static boolean setup() {
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}

	public static boolean isEnabled() {
		return economy != null;
	}

	public static boolean giveReward(UUID playerUUID, double amount) {
		if (economy == null || amount <= 0) return false;
		try {
			OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
			economy.depositPlayer(target, amount);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
