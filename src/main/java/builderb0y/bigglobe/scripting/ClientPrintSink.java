package builderb0y.bigglobe.scripting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import builderb0y.scripting.util.PrintSink;

@Environment(EnvType.CLIENT)
public class ClientPrintSink implements PrintSink {

	@Override
	public void println(int value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(long value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(float value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(double value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(char value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(boolean value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(String value) {
		ScriptLogger.LOGGER.info(value);
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}

	@Override
	public void println(Object value) {
		ScriptLogger.LOGGER.info(String.valueOf(value));
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
		}
	}
}