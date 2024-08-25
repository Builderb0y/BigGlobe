package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.noise.*;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.update.ArgumentedObjectUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.ArgumentedObjectUpdateInsnTree.ArgumentedObjectUpdateEmitters;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class GridScriptEnvironment {

	public static final ConstantFactory GRID    = new ConstantFactory(GridScriptEnvironment.class, "getGrid",   String.class, Grid  .class);
	public static final ConstantFactory GRID_1D = new ConstantFactory(GridScriptEnvironment.class, "getGrid1D", String.class, Grid1D.class);
	public static final ConstantFactory GRID_2D = new ConstantFactory(GridScriptEnvironment.class, "getGrid2D", String.class, Grid2D.class);
	public static final ConstantFactory GRID_3D = new ConstantFactory(GridScriptEnvironment.class, "getGrid3D", String.class, Grid3D.class);

	public static final MutableScriptEnvironment BASE = (
		new MutableScriptEnvironment()

		.addType("Grid",   Grid  .class)
		.addType("Grid1D", Grid1D.class)
		.addType("Grid2D", Grid2D.class)
		.addType("Grid3D", Grid3D.class)

		.addCastConstant(GRID,    true)
		.addCastConstant(GRID_1D, true)
		.addCastConstant(GRID_2D, true)
		.addCastConstant(GRID_3D, true)

		.addFieldInvoke("minValue",   Grid.INFO.minValue)
		.addFieldInvoke("maxValue",   Grid.INFO.maxValue)
		.addFieldInvoke("dimensions", Grid.INFO.getDimensions)

		.addMethodInvoke("getValue",   Grid1D.INFO.getValue)
		.addMethodInvoke("getValuesX", Grid1D.INFO.getBulkX)

		.addMethodInvoke("getValue",   Grid2D.INFO.getValue)
		.addMethodInvoke("getValuesX", Grid2D.INFO.getBulkX)
		.addMethodInvoke("getValuesY", Grid2D.INFO.getBulkY)

		.addMethodInvoke("getValue",   Grid3D.INFO.getValue)
		.addMethodInvoke("getValuesX", Grid3D.INFO.getBulkX)
		.addMethodInvoke("getValuesY", Grid3D.INFO.getBulkY)
		.addMethodInvoke("getValuesZ", Grid3D.INFO.getBulkZ)

		.addType("NumberArray", NumberArray.class)

		.addFunctionInvokeStatic("newBooleanArray", NumberArray.INFO.allocateBooleansDirect)
		.addFunctionInvokeStatic("newByteArray",    NumberArray.INFO.allocateBytesDirect)
		.addFunctionInvokeStatic("newShortArray",   NumberArray.INFO.allocateShortsDirect)
		.addFunctionInvokeStatic("newIntArray",     NumberArray.INFO.allocateIntsDirect)
		.addFunctionInvokeStatic("newLongArray",    NumberArray.INFO.allocateLongsDirect)
		.addFunctionInvokeStatic("newFloatArray",   NumberArray.INFO.allocateFloatsDirect)
		.addFunctionInvokeStatic("newDoubleArray",  NumberArray.INFO.allocateDoublesDirect)

		.addMethodInvoke("getBoolean", NumberArray.INFO.getZ)
		.addMethodInvoke("getByte",    NumberArray.INFO.getB)
		.addMethodInvoke("getShort",   NumberArray.INFO.getS)
		.addMethodInvoke("getInt",     NumberArray.INFO.getI)
		.addMethodInvoke("getLong",    NumberArray.INFO.getL)
		.addMethodInvoke("getFloat",   NumberArray.INFO.getF)
		.addMethodInvoke("getDouble",  NumberArray.INFO.getD)

		.addMethodInvoke("setBoolean", NumberArray.INFO.setZ)
		.addMethodInvoke("setByte",    NumberArray.INFO.setB)
		.addMethodInvoke("setShort",   NumberArray.INFO.setS)
		.addMethodInvoke("setInt",     NumberArray.INFO.setI)
		.addMethodInvoke("setLong",    NumberArray.INFO.setL)
		.addMethodInvoke("setFloat",   NumberArray.INFO.setF)
		.addMethodInvoke("setDouble",  NumberArray.INFO.setD)

		.addMethod(type(NumberArray.class), "", new MethodHandler.Named(
			"Automatic-precision getter and setter for NumberArray",
			(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
				InsnTree castArgument = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
				return new CastResult(new NumberArrayGetterInsnTree(receiver, castArgument, TypeInfos.DOUBLE), castArgument != arguments[0]);
			}
		))

		.addMethodInvoke("prefix", NumberArray.INFO.prefix)
		.addMethodInvoke("sliceFromTo", NumberArray.INFO.sliceFromTo)
		.addMethodInvoke("sliceOffsetLength", NumberArray.INFO.sliceOffsetLength)
	);

	public static Consumer<MutableScriptEnvironment> create() {
		return (MutableScriptEnvironment environment) -> environment.addAll(BASE);
	}

	public static Consumer<MutableScriptEnvironment> createWithSeed(InsnTree loadSeed) {
		return (MutableScriptEnvironment environment) -> {
			environment
			.configure(create())

			.addMethod(type(Grid1D.class), "getValue",   Handlers.builder(Grid1D.class, "getValue").addReceiverArgument(Grid1D.class).addArguments(loadSeed, 'I').buildMethod())
			.addMethod(type(Grid1D.class), "getValuesX", Handlers.builder(Grid1D.class, "getBulkX").addReceiverArgument(Grid1D.class).addArguments(loadSeed, 'I', NumberArray.class).buildMethod())

			.addMethod(type(Grid2D.class), "getValue",   Handlers.builder(Grid2D.class, "getValue").addReceiverArgument(Grid2D.class).addArguments(loadSeed, "II").buildMethod())
			.addMethod(type(Grid2D.class), "getValuesX", Handlers.builder(Grid2D.class, "getBulkX").addReceiverArgument(Grid2D.class).addArguments(loadSeed, "II", NumberArray.class).buildMethod())
			.addMethod(type(Grid2D.class), "getValuesY", Handlers.builder(Grid2D.class, "getBulkY").addReceiverArgument(Grid2D.class).addArguments(loadSeed, "II", NumberArray.class).buildMethod())

			.addMethod(type(Grid3D.class), "getValue",   Handlers.builder(Grid3D.class, "getValue").addReceiverArgument(Grid3D.class).addArguments(loadSeed, "III").buildMethod())
			.addMethod(type(Grid3D.class), "getValuesX", Handlers.builder(Grid3D.class, "getBulkX").addReceiverArgument(Grid3D.class).addArguments(loadSeed, "III", NumberArray.class).buildMethod())
			.addMethod(type(Grid3D.class), "getValuesY", Handlers.builder(Grid3D.class, "getBulkY").addReceiverArgument(Grid3D.class).addArguments(loadSeed, "III", NumberArray.class).buildMethod())
			.addMethod(type(Grid3D.class), "getValuesZ", Handlers.builder(Grid3D.class, "getBulkZ").addReceiverArgument(Grid3D.class).addArguments(loadSeed, "III", NumberArray.class).buildMethod())
			;
		};
	}

	public static String precision(NumberArray array) {
		return array.getPrecision().lowerCaseName;
	}

	public static Grid1D getGrid1D(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getGrid1D(id);
	}

	public static Grid1D getGrid1D(String id) {
		return (Grid1D)(getGrid(id));
	}

	public static Grid2D getGrid2D(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getGrid2D(id);
	}

	public static Grid2D getGrid2D(String id) {
		return (Grid2D)(getGrid(id));
	}

	public static Grid3D getGrid3D(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getGrid3D(id);
	}

	public static Grid3D getGrid3D(String id) {
		return (Grid3D)(getGrid(id));
	}

	public static Grid getGrid(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getGrid(id);
	}

	public static Grid getGrid(String id) {
		if (id == null) return null;
		return (
			BigGlobeMod
			.getRegistry(BigGlobeDynamicRegistries.GRID_TEMPLATE_REGISTRY_KEY)
			.getByName(id)
			.value()
		);
	}

	public static class NumberArrayGetterInsnTree implements InsnTree {

		public final InsnTree loadArray, loadIndex;
		public final TypeInfo type;

		public NumberArrayGetterInsnTree(InsnTree loadArray, InsnTree loadIndex, TypeInfo type) {
			this.loadArray = loadArray;
			this.loadIndex = loadIndex;
			this.type = type;
		}

		public MethodInfo getter() {
			return switch (this.type.getSort()) {
				case BYTE    -> NumberArray.INFO.getB;
				case SHORT   -> NumberArray.INFO.getS;
				case INT     -> NumberArray.INFO.getI;
				case LONG    -> NumberArray.INFO.getL;
				case FLOAT   -> NumberArray.INFO.getF;
				case DOUBLE  -> NumberArray.INFO.getD;
				case BOOLEAN -> NumberArray.INFO.getZ;
				case CHAR, VOID, OBJECT, ARRAY -> throw new IllegalStateException("Invalid NumberArray type: " + this.type);
			};
		}

		public MethodInfo setter() {
			return switch (this.type.getSort()) {
				case BYTE    -> NumberArray.INFO.setB;
				case SHORT   -> NumberArray.INFO.setS;
				case INT     -> NumberArray.INFO.setI;
				case LONG    -> NumberArray.INFO.setL;
				case FLOAT   -> NumberArray.INFO.setF;
				case DOUBLE  -> NumberArray.INFO.setD;
				case BOOLEAN -> NumberArray.INFO.setZ;
				case CHAR, VOID, OBJECT, ARRAY -> throw new IllegalStateException("Invalid NumberArray type: " + this.type);
			};
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.loadArray.emitBytecode(method);
			this.loadIndex.emitBytecode(method);
			this.getter().emitBytecode(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
			if (type.isNumber() || type.getSort() == Sort.BOOLEAN) {
				return new NumberArrayGetterInsnTree(this.loadArray, this.loadIndex, type);
			}
			else {
				throw new ClassCastException("Cannot cast NumberArray element to " + type);
			}
		}

		@Override
		public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
			if (rightValue.getTypeInfo().isNumber() || rightValue.getTypeInfo().getSort() == Sort.BOOLEAN) {
				return new ArgumentedObjectUpdateInsnTree(
					order,
					op == UpdateOp.ASSIGN,
					ArgumentedObjectUpdateEmitters.forGetterSetter(
						this.loadArray,
						this.loadIndex,
						this.getter(),
						this.setter(),
						rightValue
					)
				);
			}
			else {
				throw new ScriptParsingException("Can't store " + rightValue.getTypeInfo() + " in NumberArray", parser.input);
			}
		}
	}
}