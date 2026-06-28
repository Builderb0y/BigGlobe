package builderb0y.bigglobe.spawning;

import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.EntityTypeEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.SpawnTweakerTag;
import builderb0y.bigglobe.spawning.SpawnMap.SpawnParams;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record SpawnTweaker(
	@DefaultDouble(0.0D) double order,
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
	public int compareTo(@NotNull SpawnTweaker that) {
		return Double.compare(this.order, that.order);
	}

	public static void apply(
		MobCategory category,
		ScriptedColumn column,
		int y,
		BiomeEntry biome,
		RandomGenerator random,
		SpawnMap spawnMap,
		SpawnParams spawnParams,
		SpawnTweakerTag tag
	) {
		for (SpawnTweaker tweaker : tag.list.objectList()) {
			MobCategory category2 = tweaker.getCategory();
			if (category2 == null || category2 == category) {
				tweaker.script.tweak(category, column, y, biome, random, spawnMap, tweaker.primary_entity() != null ? spawnMap._get(tweaker.primary_entity().value()) : null);
			}
		}
	}

	public interface SpawnTweakerScript extends ColumnScript {

		public abstract void tweak(
			MobCategory category,
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
				.addFunction(
					Handlers
					.methodBuilder(SpawnTweaker.class, "apply")
					.addImplicitArgument(load("category", type(MobCategory.class)))
					.addImplicitArgumentOfType(load(parameters.actualColumn), ScriptedColumn.class)
					.addImplicitArgument(load("y", type(int.class)))
					.addImplicitArgument(load("biome", type(BiomeEntry.class)))
					.addImplicitArgument(load("random", type(RandomGenerator.class)))
					.addImplicitArgument(load("spawnMap", type(SpawnMap.class)))
					.addImplicitArgument(load("spawnParams", type(SpawnParams.class)))
					.addRequiredArgument(SpawnTweakerTag.class)
					.buildFunction()
				)
				;
			}

			@Override
			public Class<SpawnTweakerScript> getScriptClass() {
				return SpawnTweakerScript.class;
			}

			@Override
			public void tweak(
				MobCategory category,
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
					this.script.tweak(category, column, y, biome, random, spawnMap, spawnParams);
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