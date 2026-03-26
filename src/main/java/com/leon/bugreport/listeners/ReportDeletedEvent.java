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

	public ReportDeletedEvent(Player player, int reportId, double ecoRewardAmount) {
		this.player = player;
		this.reportId = reportId;
		this.ecoRewardAmount = ecoRewardAmount;
	}

	public Player getPlayer() {
		return player;
	}

	public int getReportId() {
		return reportId;
	}

	public double getEcoRewardAmount() {
		return ecoRewardAmount;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
