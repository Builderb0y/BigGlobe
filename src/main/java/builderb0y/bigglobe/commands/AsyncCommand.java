package builderb0y.bigglobe.commands;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;

public abstract class AsyncCommand implements Runnable, Thread.UncaughtExceptionHandler {

	public final ServerCommandSource source;

	public AsyncCommand(ServerCommandSource source) {
		this.source = source;
	}

	public ScriptedColumn newScriptedColumn() {
		return (
			(BigGlobeScriptedChunkGenerator)(
				this
				.source
				.getWorld()
				.getChunkManager()
				.getChunkGenerator()
			)
		)
		.newColumn(this.source.getWorld(), 0, 0, Purpose.GENERIC);
	}

	public void start(String input) {
		Thread thread = new Thread(this, '[' + this.source.getName() + "]: " + input);
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(this);
		thread.start();
	}

	/**
	returns true if we can still send feedback to our {@link #source}.
	a return value of false indicates that there is
	no need for this command to continue executing.
	*/
	public boolean isValid() {
		//getServer() and isServerStopped() just return fields,
		//and are never overridden. therefore, they are thread-safe.
		if (this.source.getServer().isStopped()) {
			return false;
		}
		//getEntity() also just returns a field, and is never overridden.
		Entity entity = this.source.getEntity();
		if (entity == null) {
			return true; //server console is always valid.
		}
		//allow dead players to continue executing, as long as they haven't disconnected.
		if (entity instanceof ServerPlayerEntity) {
			//hasDisconnected() returns a field, and is never overridden.
			//therefore, it is thread-safe.
			return !((ServerPlayerEntity)(entity)).isDisconnected();
		}
		else {
			//isAlive() returns a field by default, but is overridden by LivingEntity.
			//LivingEntity queries getHealth(), which queries the EntityDataManager.
			//however, the relevant methods in EntityDataManager are guarded by
			//ReadWriteLock (or more specifically, ReentrantReadWriteLock).
			//therefore, these methods are thread-safe too.
			return entity.isAlive();
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		BigGlobeMod.LOGGER.error("Error running command: " + thread.getName(), throwable);
		this.source.getServer().execute(() -> {
			if (this.isValid()) {
				this.source.sendError(Text.literal(throwable.toString()));
			}
		});
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " started by " + this.source.getName();
	}
}