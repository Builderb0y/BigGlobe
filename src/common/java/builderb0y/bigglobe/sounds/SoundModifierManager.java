package builderb0y.bigglobe.sounds;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.audio.Channel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.ReadOnlyWorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.SoundModifierEntry;
import builderb0y.bigglobe.sounds.SoundModifier.ALResource;

import static org.lwjgl.openal.AL10.*;

public class SoundModifierManager {

	public final WeakHashMap<Level, ReadOnlyWorldWrapper> worlds = new WeakHashMap<>();
	public final Map<Holder<SoundModifier>, ALResource> soundModifierResources = new IdentityHashMap<>();

	public void applyModifiersTo(SoundInstance sound, ChannelHandle handle) {
		ClientLevel world = Minecraft.getInstance().level;
		ClientState state = ClientState.get(world);
		Entity listener = Minecraft.getInstance().getCameraEntity();
		if (state != null && state.generatorParams != null && listener != null) { //world == null implies state == null.
			SoundModifierController.Catcher controller = state.generatorParams.soundModifier;
			if (controller != null) {
				ReadOnlyWorldWrapper wrapper = this.worlds.computeIfAbsent(
					world,
					(Level world_) -> new ReadOnlyWorldWrapper(
						world_,
						new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime())),
						state.generatorParams.configuredColumnFactory(ColumnUsage.GENERIC.normalHints()),
						64
					)
				);
				SoundModifierEntry modifier = controller.modifySound(wrapper, sound, listener);
				if (modifier != null) {
					handle.execute((Channel channel) -> {
						ALResource resource = this.soundModifierResources.computeIfAbsent(modifier.entry, (Holder<SoundModifier> holder) -> {
							ALResource result = holder.value().createResource();
							checkError();
							return result;
						});
						if (resource != null) {
							resource.applyTo(channel.source);
							checkError();
						}
					});
				}
			}
		}
	}

	public void destroy() {
		this.soundModifierResources.values().forEach(ALResource::close);
		this.soundModifierResources.clear();
	}

	public static void checkError() {
		int error = alGetError();
		if (error != 0) {
			BigGlobeMod.LOGGER.warn("AL error: " + switch (error) {
				case AL_INVALID_NAME -> "AL_INVALID_NAME";
				case AL_INVALID_ENUM -> "AL_INVALID_ENUM";
				case AL_INVALID_VALUE -> "AL_INVALID_VALUE";
				case AL_INVALID_OPERATION -> "AL_INVALID_OPERATION";
				case AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY";
				default -> "Unknown error 0x" + Integer.toHexString(error);
			});
		}
	}
}