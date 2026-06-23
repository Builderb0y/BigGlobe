package builderb0y.bigglobe.spawning;

import java.util.random.RandomGenerator;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.EntityTypeEntry;
import builderb0y.bigglobe.spawning.SpawnMap.SpawnParams;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record SpawnTweaker(
	@DefaultDouble(0.0D) double order,
	DelayedEntryList<Biome> biomes,
	@VerifyNullable MobCategory category,
	@VerifyNullable Holder<EntityType<?>> primary_entity,
	SpawnTweakerScript.Catcher script
)
implements Comparable<SpawnTweaker> {

	public @Nullable MobCategory getCategory() {
		if (this.category != null) return this.category;
		if (this.primary_entity != null) return this.primary_entity.value().getCategory();
		return null;
	}

	@Override
	public int compareTo(@NonNull SpawnTweaker that) {
		return Double.compare(this.order, that.order);
	}

	public interface SpawnTweakerScript extends ColumnScript {

		public abstract void tweak(
			ScriptedColumn column,
			int y,
			BiomeEntry biome,
			RandomGenerator random,
			SpawnMap spawnMap,
			SpawnParams spawnParams
		);

		@Wrapper
		public static class Catcher extends BaseCatcher<SpawnTweakerScript> implements SpawnTweakerScript {

			public Catcher(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void addExtraFunctionsToEnvironment(ImplParameters parameters, ExpressionParser parser) {
				super.addExtraFunctionsToEnvironment(parameters, parser);
				parser
				.environment
				.mutable()
				.addVariableLoad("biome", BiomeEntry.TYPE)
				.addType("SpawnMap", SpawnMap.INFO.type)
				.addVariableLoad("spawnMap", SpawnMap.INFO.type)
				.addVariableLoad("spawnParams", SpawnParams.INFO.type)
				.addMethodInvoke(SpawnMap.INFO.get)
				.addMethodInvoke(SpawnMap.INFO.put)
				.addMethodInvoke(SpawnMap.INFO.remove)
				.addMethodInvoke(SpawnMap.INFO.clear)
				.addMethod(new MethodHandler.Named(
					type(SpawnMap.class),
					"",
					"spawnMap.(key)",
					null,
					(ExpressionParser parser_, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree key = ScriptEnvironment.castArgument(parser_, "", EntityTypeEntry.TYPE, CastMode.IMPLICIT_THROW, arguments);
						return new CastResult(
							NormalListMapGetterInsnTree.from(receiver, SpawnMap.INFO.get, key, SpawnMap.INFO.put, "SpawnMap", mode),
							key != arguments[0]
						);
					}
				))
				.addType("SpawnParams", SpawnParams.INFO.type)
				.addQualifiedConstructor(SpawnParams.CONSTRUCTUR)
				.addFieldGet(SpawnParams.INFO.weight)
				.addFieldGet(SpawnParams.INFO.count)
				;
			}

			@Override
			public Class<SpawnTweakerScript> getScriptClass() {
				return SpawnTweakerScript.class;
			}

			@Override
			public void tweak(
				ScriptedColumn column,
				int y,
				BiomeEntry biome,
				RandomGenerator random,
				SpawnMap spawnMap,
				SpawnParams spawnParams
			) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					this.script.tweak(column, y, biome, random, spawnMap, spawnParams);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
				}
				finally {
					manager.used = used;
				}
			}
		}
	}
}