package builderb0y.bigglobe.columns.scripted.entries;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.AbstractNumberData;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.MapData;
import builderb0y.bigglobe.classes.BorderedValue;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeContext;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeException;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSpec;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractObjectUpdaterInsnTree.ObjectUpdaterEmitters;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.bytecode.tree.instructions.update.ReceiverObjectUpdaterInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment.UsageCallback;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DecisionTreeColumnEntry extends LoopColumnEntry {

	public final Holder<DecisionTreeSpec> root;
	public final @DefaultBoolean(false) boolean has_border;
	public final @VerifyNullable Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches;

	public DecisionTreeColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		boolean cache,
		Holder<DecisionTreeSpec> root,
		boolean has_border,
		@VerifyNullable Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches
	) {
		super(params, valid, cache);
		this.root = root;
		this.has_border = has_border;
		this.patches = patches;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		Stream<Holder<? extends DependencyView>> result = Stream.concat(super.streamDirectDependencies(), Stream.of(this.root));
		if (this.patches != null) {
			result = Stream.of(result, this.patches.keySet().stream(), this.patches.values().stream()).flatMap(Function.identity());
		}
		return result;
	}

	@Override
	public void populateContext(ColumnEntryRegistry registry, ColumnEntryContext context, ClassCompileContext clazz, LazyVarInfo[] maybeY) {
		if (this.has_border) {
			context.borderClass = clazz.newInnerClass(
				ACC_PUBLIC | ACC_FINAL,
				Type.getInternalName(BorderedValue.class) + "$Generated_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				BorderedValue.TYPE,
				TypeInfo.ARRAY_FACTORY.empty()
			);
			context.borderValueField = context.borderClass.newField(ACC_PUBLIC, "value", super.getTypeInfo(registry));
			context.borderConstructor = context.borderClass.addNoArgConstructor(ACC_PUBLIC);

			MethodCompileContext getValue = context.borderClass.newMethod(ACC_PUBLIC, "getValue", TypeInfos.OBJECT);
			return_(getField(
				load("this", context.borderClass.info),
				context.borderValueField.info
			))
			.emitBytecode(getValue);
			getValue.endCode();

			MethodCompileContext setValue = context.borderClass.newMethod(ACC_PUBLIC, "setValue", TypeInfos.VOID, new LazyVarInfo("value", TypeInfos.OBJECT));
			return_(putField(
				load("this", context.borderClass.info),
				context.borderValueField.info,
				new DirectCastInsnTree(
					load("value", TypeInfos.OBJECT),
					context.borderValueField.info.type,
					false
				)
			))
			.emitBytecode(setValue);
			setValue.endCode();
		}
		super.populateContext(registry, context, clazz, maybeY);
	}

	@Override
	public TypeInfo getTypeInfo(ColumnEntryRegistry registry) {
		if (this.has_border) {
			return registry.columnCompileContext.getCompileContext(this).borderClass.info;
		}
		else {
			return super.getTypeInfo(registry);
		}
	}

	@Override
	public InsnTree parseConstant(ColumnEntryRegistry registry, Data data) throws ConstantFormatException {
		if (this.has_border) {
			if (data.isEmpty()) {
				return ldc(null, this.getTypeInfo(registry));
			}
			else {
				ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
				MapData map = data.tryAsMap();
				if (map != null) {
					InsnTree value = super.parseConstant(registry, map.get("value"));
					value = new ReceiverObjectUpdaterInsnTree(
						CombinedMode.VOID_ASSIGN,
						ObjectUpdaterEmitters.forField(
							newInstance(context.borderConstructor.info),
							context.borderValueField.info,
							value
						)
					);
					AbstractNumberData border = map.get("border").tryAsNumber();
					if (border != null) {
						double actualBorder = border.doubleValue();
						if (actualBorder >= 0.0D && actualBorder <= 1.0D) {
							value = new ReceiverObjectUpdaterInsnTree(
								CombinedMode.VOID_ASSIGN,
								ObjectUpdaterEmitters.forField(
									value,
									BorderedValue.BORDER,
									ldc(actualBorder)
								)
							);
						}
						else {
							throw new ConstantFormatException("border must be between 0.0 and 1.0 (inclusive)");
						}
					}
					return value;
				}
				else {
					throw new ConstantFormatException("Not a map: " + data);
				}
			}
		}
		else {
			return super.parseConstant(registry, data);
		}
	}

	@Override
	public InsnTree makeComputer(ColumnEntryRegistry registry, ColumnEntryContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		try {
			InsnTree result = (
				new DecisionTreeContext(
					registry,
					registry.entryOf(this),
					this.patches,
					this.params.typeSpec(registry, this),
					this.params.is_3d(),
					this.has_border
				)
				.emitTree(this.root)
			);
			if (this.has_border) {
				result = seq(
					new VariableDeclareAssignInsnTree(
						new LazyVarInfo("border", BorderedValue.TYPE),
						newInstance(context.borderConstructor.info)
					),
					result,
					load("border", BorderedValue.TYPE)
				);
			}
			return result;
		}
		catch (DecisionTreeException exception) {
			throw new ScriptParsingException(exception, null);
		}
	}

	@Override
	public void setupEnvironment(ColumnEntryRegistry registry, ExpressionParser parser, ExternalEnvironmentParams params) {
		super.setupEnvironment(registry, parser, params);
		if (this.has_border) {
			ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
			UsageCallback callback = params.dependencyCallback(registry.entryOf(this));
			parser.environment.mutable().addField(Handlers.fieldBuilder(context.borderValueField.info).onUsed(callback).addReceiverArgument(context.borderClass.info).buildField());
		}
	}
}