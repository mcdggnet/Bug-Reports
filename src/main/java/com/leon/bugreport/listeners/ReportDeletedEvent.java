package com.leon.bugreport.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ReportDeletedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final int reportId;
	private final double ecoRewardAmount;
	private final java.util.UUID reporterUUID;
	private final String reporterName;

	public ReportDeletedEvent(Player player, int reportId, double ecoRewardAmount,
	                          java.util.UUID reporterUUID, String reporterName) {
		this.player = player;
		this.reportId = reportId;
		this.ecoRewardAmount = ecoRewardAmount;
		this.reporterUUID = reporterUUID;
		this.reporterName = reporterName;
	}

	/** The staff member who deleted the report. */
	public Player getPlayer() {
		return player;
	}

	public int getReportId() {
		return reportId;
	}

	public double getEcoRewardAmount() {
		return ecoRewardAmount;
	}

	/** UUID of the player who originally submitted the report. */
	public java.util.UUID getReporterUUID() {
		return reporterUUID;
	}

	/** Name of the player who originally submitted the report. */
	public String getReporterName() {
		return reporterName;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
