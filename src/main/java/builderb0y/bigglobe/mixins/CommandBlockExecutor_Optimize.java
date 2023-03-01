package builderb0y.bigglobe.mixins;

import com.mojang.brigadier.ParseResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.CommandBlockExecutor;

/**
this mixin is currently DISABLED because I don't know for sure if
it causes any differences in behavior compared to vanilla or not.
I doubt that it would, but I don't want to do a whole
bunch of in-depth testing to find out. and on that topic,
I also don't know how effective this optimization is,
in the sense that I don't know how much time command blocks
spend parsing compared to executing, and I don't really want
to do in-depth speed testing to figure that out either.

with all that out of the way, the goal of this mixin is to
optimize command blocks and command block minecarts by making
them *not* parse their input every time they are triggered.
instead, the input is parsed exactly once, and the result is
cached and reused next time the command block is triggered.
*/
@Mixin(CommandBlockExecutor.class)
public class CommandBlockExecutor_Optimize {

	@Unique
	private ParseResults<ServerCommandSource> bigglobe_parsedCommand;
	@Unique
	private String bigglobe_commandWithoutSlash;

	@Inject(method = "setCommand", at = @At("HEAD"))
	private void bigglobe_clearCompiledCommand(String command, CallbackInfo callback) {
		this.bigglobe_parsedCommand = null;
		this.bigglobe_commandWithoutSlash = null;
	}

	@Inject(method = "readNbt", at = @At("HEAD"))
	private void bigglobe_clearCompiledCommand(NbtCompound nbt, CallbackInfo callback) {
		this.bigglobe_parsedCommand = null;
		this.bigglobe_commandWithoutSlash = null;
	}

	@Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;executeWithPrefix(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;)I"))
	private int bigglobe_executeCachedCommand(CommandManager receiver, ServerCommandSource source, String command) {
		if (this.bigglobe_commandWithoutSlash == null) {
			this.bigglobe_commandWithoutSlash = (
				command.length() > 1 && command.charAt(0) == '/'
				? command.substring(1)
				: command
			);
		}
		if (this.bigglobe_parsedCommand == null) {
			this.bigglobe_parsedCommand = receiver.getDispatcher().parse(this.bigglobe_commandWithoutSlash, source);
		}
		return receiver.execute(this.bigglobe_parsedCommand, this.bigglobe_commandWithoutSlash);
	}
}