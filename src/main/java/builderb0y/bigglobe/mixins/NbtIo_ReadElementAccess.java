package builderb0y.bigglobe.mixins;

import java.io.DataInput;
import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;

@Mixin(NbtIo.class)
public interface NbtIo_ReadElementAccess {

	@Invoker("read")
	public static NbtElement bigglobe_read(DataInput input, int depth, NbtTagSizeTracker tracker) throws IOException {
		throw new IllegalStateException("Mixin not applied");
	}
}