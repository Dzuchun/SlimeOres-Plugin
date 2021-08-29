package dzuchun.paper.slimeores.data;

import java.nio.ByteBuffer;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dzuchun.paper.slimeores.Config;
import dzuchun.paper.slimeores.SlimeOres;

public class VeinPersistedDataType implements PersistentDataType<byte[], VeinPersistedDataType.VeinType> {
	public static final VeinPersistedDataType INSTANCE = new VeinPersistedDataType();

	private static final Map<VeinType, Double> VEIN_HEALTH_MULTIPLIERS = Config.VEIN_HEALTH_MULTIPLIER.get();
	private static final String HEALTH_MULTIPLIER_NAME = "veintype_multiplier";

	public static enum VeinType {
		NONE, RAW_IRON;

		private AttributeModifier healthMultiplier;

		public AttributeModifier getHealthMultiplier() {
			if (healthMultiplier == null) {
				double mult = VEIN_HEALTH_MULTIPLIERS.getOrDefault(this, 1.0d) - 1.0d;
				healthMultiplier = new AttributeModifier(HEALTH_MULTIPLIER_NAME, mult, Operation.MULTIPLY_SCALAR_1);
			}
			return healthMultiplier;
		}
	}

	public static final NamespacedKey KEY = new NamespacedKey(SlimeOres.getInstance(), "vein_type");

	private static final Class<byte[]> primitiveClass = byte[].class;

	@Override
	public @NotNull Class<byte[]> getPrimitiveType() {
		return primitiveClass;
	}

	private static final Class<VeinType> complexClass = VeinType.class;

	@Override
	public @NotNull Class<VeinType> getComplexType() {
		return complexClass;
	}

	@Override
	public @NonNull byte[] toPrimitive(@NotNull VeinType complex, @NotNull PersistentDataAdapterContext context) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		int n = complex.ordinal();
		buffer.putInt(n);
		buffer.rewind();
		return buffer.array();
	}

	@Override
	public @NotNull VeinType fromPrimitive(@NotNull byte[] primitive, @NotNull PersistentDataAdapterContext context) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.put(primitive);
		buffer.rewind();
		int n = buffer.getInt();
		return VeinType.values()[n];
	}

	public static void attachVeinType(PersistentDataHolder holder, VeinType type) {
		PersistentDataContainer container = holder.getPersistentDataContainer();
		container.set(KEY, INSTANCE, type);
	}

	@Nullable
	public static VeinType getVeinType(PersistentDataHolder holder) {
		PersistentDataContainer container = holder.getPersistentDataContainer();
		if (container.has(KEY, INSTANCE)) {
			return container.get(KEY, INSTANCE);
		} else {
			return null;
		}
	}
}
