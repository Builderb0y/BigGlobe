package builderb0y.bigglobe.scripting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import builderb0y.scripting.util.PrintSink;

@Environment(EnvType.CLIENT)
public class ClientPrintSink implements PrintSink {

	@Override
	public void println(int value) {
		this.println(String.valueOf(value));
	}

	@Override
	public void println(long value) {
		this.println(String.valueOf(value));
	}

	@Override
	public void println(float value) {
		this.println(String.valueOf(value));
	}

	@Override
	public void println(double value) {
		this.println(String.valueOf(value));
	}

	@Override
	public void println(char value) {
		this.println(String.valueOf(value));
	}

	@Override
	public void println(boolean value) {
		this.println(String.valueOf(value));
	}

	@Override
	public void println(Object value) {
		this.println(String.valueOf(value));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void println(String value) {
		if (MinecraftClient.getInstance().getServer() != null) {
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null) {
				player.sendMessage(Text.literal("[Big Globe/Scripting]: " + value));
			}
			else {
				ScriptLogger.LOGGER.info(value);
			}
		}
	}
}