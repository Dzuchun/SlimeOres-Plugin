package dzuchun.paper.slimeores.data;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;

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
import dzuchun.paper.slimeores.util.WeightedRandomHolder;
import net.kyori.adventure.text.Component;

public class VeinPersistedDataType implements PersistentDataType<byte[], VeinPersistedDataType.VeinType> {
	public static void config() {
		VEIN_HEALTH_MULTIPLIERS = Config.VEIN_HEALTH_MULTIPLIER.get();
	}

	public static final VeinPersistedDataType INSTANCE = new VeinPersistedDataType();

	private static Map<VeinType, Double> VEIN_HEALTH_MULTIPLIERS;
	private static final String HEALTH_MULTIPLIER_NAME = "veintype_multiplier";

	private static final WeightedRandomHolder<Integer> SIZE_GENERATOR = (new WeightedRandomHolder<Integer>() {
		private static final long serialVersionUID = 1L;
		{
			this.put(1, 50);
			this.put(2, 25);
			this.put(3, 15);
			this.put(4, 10);
		}
	}).compile();

	private static String[] defaultVeinNamesRU(String suffix) {
		return new String[] { String.format("очень малая (эу, это имя вообще не существует) жила %s", suffix),
				String.format("малая жила %s", suffix), String.format("средняя жила %s", suffix),
				String.format("большая жила %s", suffix), String.format("гиганская жила %s", suffix) };
	}

	public static enum VeinType {
		NONE(defaultVeinNamesRU("")), RAW_IRON(defaultVeinNamesRU("железа"));

		private VeinType(String[] veinNamesIn) {
			this.veinNames = veinNamesIn;
		}

		private AttributeModifier healthMultiplier;

		public AttributeModifier getHealthMultiplier() {
			if (healthMultiplier == null) {
				double mult = VEIN_HEALTH_MULTIPLIERS.getOrDefault(this, 1.0d) - 1.0d;
				healthMultiplier = new AttributeModifier(HEALTH_MULTIPLIER_NAME, mult, Operation.MULTIPLY_SCALAR_1);
			}
			return healthMultiplier;
		}

		public int generateSize(Random rand) {
			return SIZE_GENERATOR.getForRandom(rand);
		}

		private String[] veinNames;

		// TODO optimize - store components initially (so they are not created every
		// time)
		public Component getNameForSize(int size) {
			if (size < 1) {
				return Component.text("vein of too small size");
			}
			if (size > 4) {
				return Component.text("vein of too large size");
			}
			return Component.text(this.veinNames[size]);
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

	private final ByteBuffer toPrimBuffer = ByteBuffer.allocate(Integer.BYTES);

	@Override
	public @NonNull byte[] toPrimitive(@NotNull VeinType complex, @NotNull PersistentDataAdapterContext context) {
		int n = complex.ordinal();
		toPrimBuffer.rewind();
		toPrimBuffer.putInt(n);
		toPrimBuffer.rewind();
		return toPrimBuffer.array();
	}

	private final ByteBuffer fromPrimBuffer = ByteBuffer.allocate(Integer.BYTES);

	@Override
	public @NotNull VeinType fromPrimitive(@NotNull byte[] primitive, @NotNull PersistentDataAdapterContext context) {
		fromPrimBuffer.rewind();
		fromPrimBuffer.put(primitive);
		fromPrimBuffer.rewind();
		int n = fromPrimBuffer.getInt();
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
