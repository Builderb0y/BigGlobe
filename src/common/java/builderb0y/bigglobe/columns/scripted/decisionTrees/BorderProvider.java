package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.data.AbstractNumberData;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.NumberData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneTraits2DGetterInsnTree;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneTraits3DGetterInsnTree;
import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.binary.MultiplyInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseCoder(name = "REGISTRY", in = BorderProvider.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface BorderProvider extends SimpleDependencyView, CoderRegistryTyped<BorderProvider> {

	public static final CoderRegistry<BorderProvider> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_border_provider")) {

		@Override
		@OverrideOnly
		public <T_Encoded> @VerifyNullable BorderProvider decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			AbstractNumberData number = context.data().tryAsNumber();
			if (number != null) {
				return new ConstantBorderProvider(number.doubleValue());
			}
			else {
				return super.decode(context);
			}
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, BorderProvider> context) throws EncodeException {
			if (context.object instanceof ConstantBorderProvider constant) {
				return new NumberData(constant.value);
			}
			else {
				return super.encode(context);
			}
		}
	};
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"    ),    ConstantBorderProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("column_value"), ColumnValueBorderProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("world_trait" ),  WorldTraitBorderProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("script"      ),      ScriptBorderProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("saturate"    ),    SaturateBorderProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("negate"      ),      NegateBorderProvider.class);
	}};

	public abstract InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException;

	public static record ConstantBorderProvider(double value) implements BorderProvider {

		@Override
		public InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException {
			return ldc(this.value);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.empty();
		}
	}

	public static record ColumnValueBorderProvider(Holder<ColumnEntry> column_value) implements BorderProvider {

		@Override
		public InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException {
			ColumnEntry columnEntry = this.column_value.value();
			TypeInfo existingType = columnEntry.getTypeInfo(context.columnEntryRegistry);
			if (!existingType.isNumber()) {
				throw new ScriptParsingException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is not a floating point value.", null);
			}
			boolean is3D = columnEntry.params.is_3d();
			if (is3D && !context.is3D) {
				throw new ScriptParsingException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is 3D, but an implicit Y level is not available here.", null);
			}
			MethodCompileContext method = context.columnEntryRegistry.columnCompileContext.getCompileContext(columnEntry).mainGetter;
			return (
				invokeInstance(context.loadColumn(), method.info, is3D ? new InsnTree[] { load("y", TypeInfos.INT) } : InsnTree.ARRAY_FACTORY.empty())
				.cast(CastingSupport.dummyParser(), TypeInfos.DOUBLE, CastMode.IMPLICIT_THROW, false)
			);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.of(this.column_value);
		}
	}

	public static record WorldTraitBorderProvider(Holder<WorldTrait> world_trait) implements BorderProvider {

		@Override
		public InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException {
			WorldTrait worldTrait = this.world_trait.value();
			TypeInfo existingType = worldTrait.getTypeInfo(context.columnEntryRegistry.traitManager);
			if (!existingType.isNumber()) {
				throw new ScriptParsingException("World trait " + UnregisteredObjectException.getID(this.world_trait) + " is not a floating point value.", null);
			}
			boolean is3D = worldTrait.schema().is_3d();
			if (is3D && !context.is3D) {
				throw new ScriptParsingException("World trait " + UnregisteredObjectException.getID(this.world_trait) + " is 3D, but an implicit Y level is not available here.", null);
			}
			MethodInfo traitGetter = context.columnEntryRegistry.traitManager.infos.get(this.world_trait).getter.info;
			return (
				(
					is3D
					? new StandAloneTraits3DGetterInsnTree(context.loadColumn(), context.yArgument(), traitGetter, null)
					: new StandAloneTraits2DGetterInsnTree(context.loadColumn(), traitGetter, null)
				)
				.cast(CastingSupport.dummyParser(), TypeInfos.DOUBLE, CastMode.IMPLICIT_THROW, false)
			);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.of(this.world_trait);
		}
	}

	public static class ScriptBorderProvider implements BorderProvider, SetBasedMutableDependencyView {

		public final ScriptUsage script;
		public final transient Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

		public ScriptBorderProvider(ScriptUsage script) {
			this.script = script;
		}

		@Override
		public InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException {
			MethodCompileContext method = context.newMethod("decision_tree_border_", caller, TypeInfos.DOUBLE);
			context.columnEntryRegistry.setMethodCode(method, this.script, context.loadColumn(), context.yArgument(), null, null, this, (ExpressionParser parser) -> {
				parser.environment.mutable().addFunctionInvokeStatic("saturate", SaturateBorderProvider.SATURATE);
			});
			return invokeInstance(context.loadColumn(), method.info, context.yArguments());
		}

		@Override
		public Set<Holder<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}

	public static record SaturateBorderProvider(
		BorderProvider value,
		@DefaultFloat(value = 0.0F, mode = DefaultMode.ENCODED) BorderProvider offset,
		@DefaultFloat(value = 1.0F, mode = DefaultMode.ENCODED) BorderProvider scalar,
		@DefaultBoolean(true) boolean curve
	)
	implements BorderProvider {

		public static final MethodInfo SATURATE = MethodInfo.getMethod(FastMath.Trig.class, "fastTanh");

		@Override
		public InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException {
			InsnTree tree = this.value.emitBorder(context, caller);
			if (!(this.offset instanceof ConstantBorderProvider(double offset) && offset == 0.0D)) {
				tree = new SubtractInsnTree(tree, this.offset.emitBorder(context, caller), DSUB);
			}
			if (!(this.scalar instanceof ConstantBorderProvider(double scalar) && scalar == 1.0D)) {
				tree = new MultiplyInsnTree(tree, this.scalar.emitBorder(context, caller), DMUL);
			}
			if (this.curve) {
				tree = invokeStatic(SATURATE, tree);
			}
			return tree;
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.of(this.value, this.offset, this.scalar).flatMap(BorderProvider::streamDirectDependencies);
		}
	}

	public static record NegateBorderProvider(BorderProvider value) implements BorderProvider {

		@Override
		public InsnTree emitBorder(DecisionTreeContext context, Holder<DecisionTreeSpec> caller) throws ScriptParsingException, ConstantFormatException {
			if (this.value instanceof NegateBorderProvider(BorderProvider value)) {
				return value.emitBorder(context, caller);
			}
			else {
				return neg(this.value.emitBorder(context, caller));
			}
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return this.value.streamDirectDependencies();
		}
	}
}