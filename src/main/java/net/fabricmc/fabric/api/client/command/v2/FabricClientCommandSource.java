package net.fabricmc.fabric.api.client.command.v2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public interface FabricClientCommandSource extends SharedSuggestionProvider {

	Minecraft getClient();

	LocalPlayer getPlayer();

	void sendFeedback(Component message);

	void sendError(Component message);
}
