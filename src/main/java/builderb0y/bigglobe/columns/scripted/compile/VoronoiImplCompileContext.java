package builderb0y.bigglobe.columns.scripted.compile;

import java.util.IdentityHashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiImplCompileContext extends AbstractVoronoiDataCompileContext {

	public final RegistryEntry<VoronoiSettings> voronoiSettings;
	public final Map<ColumnEntry, ColumnEntryMemory> memories;

	public VoronoiImplCompileContext(VoronoiBaseCompileContext parent, RegistryEntry<VoronoiSettings> entry) {
		super(parent);
		this.voronoiSettings = entry;
		this.flagsIndex = VoronoiDataBase.BUILTIN_FLAG_COUNT;
		this.memories = new IdentityHashMap<>(16);

		String name = internalName(UnregisteredObjectException.getID(entry), ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement());
		this.mainClass = parent.mainClass.newInnerClass(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			Type.getInternalName(VoronoiDataBase.class) + "$Generated$Impl_" + name,
			parent.mainClass.info,
			TypeInfo.ARRAY_FACTORY.empty()
		);

		LazyVarInfo column, cell;
		this.constructor = this.mainClass.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			column = new LazyVarInfo("column", parent.root().columnType()),
			cell = new LazyVarInfo("cell", type(VoronoiDiagram2D.Cell.class))
		);
		LazyVarInfo self = new LazyVarInfo("this", this.constructor.clazz.info);
		invokeInstance(
			load(self),
			new MethodInfo(
				ACC_PUBLIC,
				parent.mainClass.info,
				"<init>",
				TypeInfos.VOID,
				parent.root().columnType(),
				type(VoronoiDiagram2D.Cell.class)
			),
			load(column),
			load(cell)
		)
		.emitBytecode(this.constructor);

		MethodCompileContext id = this.mainClass.newMethod(ACC_PUBLIC, "id", TypeInfos.STRING);
		return_(ldc(UnregisteredObjectException.getID(entry).toString())).emitBytecode(id);
		id.endCode();

		MethodCompileContext toString = this.mainClass.newMethod(ACC_PUBLIC, "toString", TypeInfos.STRING);
		return_(ldc("voronoi_settings: " + UnregisteredObjectException.getID(entry))).emitBytecode(toString);
		toString.endCode();
	}

	@Override
	public Map<ColumnEntry, ColumnEntryMemory> getMemories() {
		return this.memories;
	}

	@Override
	public InsnTree loadColumn() {
		return getField(
			this.loadSelf(),
			new FieldInfo(
				ACC_PUBLIC,
				this.parent.selfType(),
				"column",
				this.root().columnType()
			)
		);
	}

	@Override
	public InsnTree loadSeed(InsnTree salt) {
		return (
			salt != null
			? VoronoiDataBase.INFO.salted_seed(this.loadSelf(), salt)
			: VoronoiDataBase.INFO.unsalted_seed(this.loadSelf())
		);
	}

	@Override
	public FieldInfo flagsField(int index) {
		return new FieldInfo(
			ACC_PUBLIC,
			(index >>> 5) == 0 ? type(VoronoiDataBase.class) : this.mainClass.info,
			"flags_" + (index >>> 5),
			TypeInfos.INT
		);
	}

	@Override
	public void prepareForCompile() {
		this.addGenericGetterAndPreComputer(this.memories);
		for (int index = 1, max = this.flagsIndex >>> 5; index <= max; index++) {
			this.mainClass.newField(ACC_PUBLIC, "flags_" + index, TypeInfos.INT);
		}
		super.prepareForCompile();
	}
}