package com.leon.bugreport.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ReportArchivedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final int reportId;

	public ReportArchivedEvent(Player player, int reportId) {
		this.player = player;
		this.reportId = reportId;
	}

	public Player getPlayer() {
		return player;
	}

	public int getReportId() {
		return reportId;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
