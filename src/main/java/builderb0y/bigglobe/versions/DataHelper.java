package builderb0y.bigglobe.versions;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;

import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class DataHelper<T_Owner> {

	public final Class<T_Owner> owner;
	public final List<Entry<?>> entries = new ArrayList<>(4);

	public DataHelper(Class<T_Owner> owner) {
		this.owner = owner;
	}

	public EntryBuilder<?> begin(String name) {
		return this.new EntryBuilder<>(name);
	}

	public void read(
		T_Owner owner,
		#if MC_VERSION >= MC_1_21_6
			net.minecraft.storage.ReadView data
		#else
			NbtCompound data
		#endif
	) {
		for (Entry<?> entry : this.entries) {
			entry.read(owner, data);
		}
	}

	public void write(
		T_Owner owner,
		#if MC_VERSION >= MC_1_21_6
			net.minecraft.storage.WriteView data
		#else
			NbtCompound data
		#endif
	) {
		for (Entry<?> entry : this.entries) {
			entry.write(owner, data);
		}
	}

	public class Entry<T_Component> {

		public final String name;
		public final Codec<T_Component> codec;
		public final FieldAccessor<T_Owner, T_Component> accessor;

		public Entry(DataHelper<T_Owner>.EntryBuilder<T_Component> builder) {
			this.name = Objects.requireNonNull(builder.name, "name");
			this.codec = Objects.requireNonNull(builder.codec, "codec");
			this.accessor = Objects.requireNonNull(builder.accessor, "accessor");
		}

		#if MC_VERSION >= MC_1_21_6

			public void read(T_Owner owner, net.minecraft.storage.ReadView data) {
				this.accessor.set(owner, data.read(this.name, this.codec).orElse(null));
			}

			public void write(T_Owner owner, net.minecraft.storage.WriteView data) {
				T_Component component = this.accessor.get(owner);
				if (component != null) data.put(this.name, this.codec, component);
			}

		#else

			public void read(T_Owner owner, NbtCompound nbt) {
				NbtElement input = nbt.get(this.name);
				if (input == null) input = NbtEnd.INSTANCE;
				this.accessor.set(owner, this.codec.parse(RegistryOps.of(NbtOps.INSTANCE, BigGlobeMod.getCurrentServer().getRegistryManager()), input).resultOrPartial(BigGlobeMod.LOGGER::error).orElse(null));
			}

			public void write(T_Owner owner, NbtCompound nbt) {
				T_Component component = this.accessor.get(owner);
				if (component != null) nbt.put(this.name, this.codec.encodeStart(RegistryOps.of(NbtOps.INSTANCE, BigGlobeMod.getCurrentServer().getRegistryManager()), component).resultOrPartial(BigGlobeMod.LOGGER::error).orElse(null));
			}

		#endif
	}

	public class EntryBuilder<T_Component> {

		public String name;
		public Codec<T_Component> codec;
		public FieldAccessor<T_Owner, T_Component> accessor;

		public EntryBuilder(String name) {
			this.name = name;
		}

		public <T_NewComponent> EntryBuilder<T_NewComponent> codec(Codec<T_NewComponent> codec) {
			@SuppressWarnings("unchecked")
			EntryBuilder<T_NewComponent> builder = (EntryBuilder<T_NewComponent>)(this);
			builder.codec = codec;
			return builder;
		}

		public <T_NewComponent> EntryBuilder<T_NewComponent> codecFromType(ReifiedType<T_NewComponent> type) {
			@SuppressWarnings("unchecked")
			EntryBuilder<T_NewComponent> builder = (EntryBuilder<T_NewComponent>)(this);
			builder.codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(type);
			return builder;
		}

		public <T_NewComponent> EntryBuilder<T_NewComponent> codecFromClass(Class<T_NewComponent> clazz) {
			@SuppressWarnings("unchecked")
			EntryBuilder<T_NewComponent> builder = (EntryBuilder<T_NewComponent>)(this);
			builder.codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(clazz);
			return builder;
		}

		public <T_NewComponent> EntryBuilder<T_NewComponent> accessor(FieldAccessor<T_Owner, T_NewComponent> accessor, boolean setCodec) {
			@SuppressWarnings("unchecked")
			EntryBuilder<T_NewComponent> builder = (EntryBuilder<T_NewComponent>)(this);
			builder.accessor = accessor;
			if (setCodec) builder.codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(accessor.componentType());
			return builder;
		}

		@SuppressWarnings("unchecked")
		public EntryBuilder<?> fieldAccessor(String fieldName, boolean setCodec) {
			return this.accessor((FieldAccessor<T_Owner, T_Component>)(FieldAccessor.field(DataHelper.this.owner, fieldName)), setCodec);
		}

		@SuppressWarnings("unchecked")
		public EntryBuilder<?> pathAccessor(String path, boolean setCodec) {
			return this.accessor((FieldAccessor<T_Owner, T_Component>)(FieldAccessor.path(DataHelper.this.owner, path)), setCodec);
		}

		public DataHelper<T_Owner> add() {
			DataHelper.this.entries.add(DataHelper.this.new Entry<>(this));
			return DataHelper.this;
		}
	}

	public static interface FieldAccessor<T_Owner, T_Component> {

		public abstract T_Component get(T_Owner owner);

		public abstract void set(T_Owner owner, T_Component value);

		public abstract ReifiedType<T_Component> componentType();

		public static <T_Owner, T_Component> FieldAccessor<T_Owner, T_Component> of(ReifiedType<T_Component> componentType, Function<T_Owner, T_Component> getter, BiConsumer<T_Owner, T_Component> setter) {
			return new FieldAccessor<>() {

				@Override
				public T_Component get(T_Owner owner) {
					return owner == null ? null : getter.apply(owner);
				}

				@Override
				public void set(T_Owner owner, T_Component value) {
					setter.accept(owner, value);
				}

				@Override
				public ReifiedType<T_Component> componentType() {
					return componentType;
				}
			};
		}

		public static <T_Owner, T_Component> FieldAccessor<T_Owner, T_Component> of(ReifiedType<T_Component> componentType, VarHandle handle) {
			return new FieldAccessor<>() {

				@Override
				@SuppressWarnings("unchecked")
				public T_Component get(T_Owner owner) {
					return owner == null ? null : (T_Component)(handle.get(owner));
				}

				@Override
				public void set(T_Owner owner, T_Component value) {
					handle.set(owner, value);
				}

				@Override
				public ReifiedType<T_Component> componentType() {
					return componentType;
				}
			};
		}

		public static <T_Owner> FieldAccessor<T_Owner, ?> field(Class<T_Owner> ownerClass, String fieldName) {
			try {
				Field field = ownerClass.getField(fieldName);
				return of(ReifiedType.from(field.getAnnotatedType()), MethodHandles.lookup().unreflectVarHandle(field));
			}
			catch (ReflectiveOperationException exception) {
				throw AutoCodecUtil.rethrow(exception);
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static <T_Owner> FieldAccessor<T_Owner, ?> path(Class<T_Owner> ownerClass, String path) {
			String[] parts = path.split("\\.");
			VarHandle[] handles = new VarHandle[parts.length];
			Class<?> intermediate = ownerClass;
			Field field = null;
			for (int index = 0; index < parts.length; index++) try {
				field = intermediate.getField(parts[index]);
				handles[index] = MethodHandles.lookup().unreflectVarHandle(field);
				intermediate = field.getType();
			}
			catch (ReflectiveOperationException exception) {
				throw AutoCodecUtil.rethrow(exception);
			}
			ReifiedType<?> componentType = ReifiedType.from(field.getAnnotatedType());
			return new FieldAccessor() {

				@Override
				public Object get(Object owner) {
					Object current = owner;
					for (VarHandle handle : handles) {
						if (current == null) return null;
						current = handle.get(current);
					}
					return current;
				}

				@Override
				public void set(Object owner, Object value) {
					Object current = owner;
					int limit = handles.length - 1;
					for (int index = 0; index < limit; index++) {
						VarHandle handle = handles[index];
						current = handle.get(current);
					}
					handles[limit].set(current, value);
				}

				@Override
				public ReifiedType componentType() {
					return componentType;
				}
			};
		}
	}
}