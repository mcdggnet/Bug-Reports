package com.leon.bugreport.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ReportViewedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final int reportId;
	private final boolean isArchived;

	public ReportViewedEvent(Player player, int reportId, boolean isArchived) {
		this.player = player;
		this.reportId = reportId;
		this.isArchived = isArchived;
	}

	public Player getPlayer() {
		return player;
	}

	public int getReportId() {
		return reportId;
	}

	public boolean isArchived() {
		return isArchived;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
