package builderb0y.bigglobe.scripting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
		if (Minecraft.getInstance().getSingleplayerServer() != null) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				Minecraft.getInstance().execute(() -> {
					player.sendSystemMessage(Component.literal("[Big Globe/Scripting]: " + value));
				});
			}
			else {
				ScriptLogger.LOGGER.info(value);
			}
		}
	}
}