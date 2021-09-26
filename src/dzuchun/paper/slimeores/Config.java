package dzuchun.paper.slimeores;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;

import dzuchun.paper.slimeores.data.VeinPersistedDataType.VeinType;
import dzuchun.paper.slimeores.util.Util;
import dzuchun.paper.slimeores.util.WeightedRandomHolder;
import dzuchun.paper.slimeores.world.OreChunksSystem.ChunkType;

public class Config {

	public static class Instance<T, U> {
		private static Function<?, ?> DO_NOTHING = o -> o;

		public final String key;
		public final T defaultValue;
		private T value;
		private FileConfiguration cfg;
		private final Function<T, U> serializer;
		private Function<U, T> deserializer;

		@SuppressWarnings("unchecked")
		public Instance(String keyIn, T defaultValueIn, Function<T, U> serializerIn, Function<U, T> deserializerIn) {
			this.key = keyIn;
			this.defaultValue = defaultValueIn;
			value = defaultValueIn;
			this.serializer = (serializerIn == null) ? (Function<T, U>) DO_NOTHING : serializerIn;
			this.deserializer = (deserializerIn == null) ? (Function<U, T>) DO_NOTHING : deserializerIn;
		}

		@SuppressWarnings("unchecked")
		public void bind(FileConfiguration cfgIn) {
			this.cfg = cfgIn;
			if (cfg.contains(key)) {
				this.value = this.deserializer.apply((U) cfg.get(key));
			} else {
				cfg.set(key, serializer.apply(defaultValue));
			}
		}

		public T get() {
			return this.value;
		}
	}

	private static final Map<Material, Double> DEFAULT_PICKAXES = new LinkedHashMap<>() {
		private static final long serialVersionUID = 1L;
		{
			this.put(Material.WOODEN_PICKAXE, 1.0d);
			this.put(Material.STONE_PICKAXE, 2.0d);
			this.put(Material.GOLDEN_PICKAXE, 3.0d);
			this.put(Material.IRON_PICKAXE, 4.0d);
			this.put(Material.DIAMOND_PICKAXE, 5.0d);
			this.put(Material.NETHERITE_PICKAXE, 6.0d);
		}
	};
	/**
	 * Represents items that count as pickaxe (required to damage vein)
	 */
	public static final Instance<Map<Material, Double>, Collection<String>> PICKAXES = new Config.Instance<>(
			"Pickaxes (represent items that can damage veins and their power)", DEFAULT_PICKAXES,
			picks -> collectionToStringCollection(picks.entrySet(),
					entry -> String.format("%s power %.2f", entry.getKey().name(), entry.getValue())),
			array -> mapFromEntryCollection(collectionFromStringCollection(array, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				Material item = Enum.valueOf(Material.class, name);
				sc.next();
				double power = sc.nextDouble();
				sc.close();
				return Map.entry(item, power);
			})));

	private static final List<Material> DEFAULT_ALLOWED_SPAWN_MATERIALS = Arrays.asList(Material.STONE, Material.DIRT,
			Material.GRAVEL, Material.SAND, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.ANDESITE,
			Material.DIORITE, Material.GRANITE);
	/**
	 * Represents blocks veins can spawn on.
	 */
	public static final Instance<Collection<Material>, Collection<String>> ALLOWED_SPAWN_MATERIALS = new Instance<>(
			"Allowed spawn materials (represent blocks, veins can spawn on)", DEFAULT_ALLOWED_SPAWN_MATERIALS,
			l -> collectionToStringCollection(l, enumSerializer(Material.class)),
			b -> collectionFromStringCollection(b, enumDeserializer(Material.class)));

	private static final List<Material> DEFAULT_ALLOWED_SPAWN_COVERS = Arrays.asList(Material.AIR, Material.CAVE_AIR,
			Material.VOID_AIR, Material.SNOW, Material.TALL_GRASS, Material.GRASS, Material.FERN, Material.LARGE_FERN);
	/**
	 * Represents blocks veins can spawn under.
	 */
	public static final Instance<Collection<Material>, Collection<String>> ALLOWED_SPAWN_COVERS = new Instance<>(
			"Allowed spawn covers (represent blocks, veins can spawn under)", DEFAULT_ALLOWED_SPAWN_COVERS,
			l -> collectionToStringCollection(l, enumSerializer(Material.class)),
			b -> collectionFromStringCollection(b, enumDeserializer(Material.class)));

	private static <T extends Enum<T>> Function<T, String> enumSerializer(Class<T> clazz) {
		return Enum<T>::name;
	}

	private static <T extends Enum<T>> Function<String, T> enumDeserializer(Class<T> clazz) {
		return s -> Enum.valueOf(clazz, s);
	}

	/**
	 * If this is enabled, newly generated veins will get .setGlowing(true)
	 */
	public static final Instance<Boolean, Boolean> VEINS_GLOW = new Instance<>(
			"Veins glow (if enabled, newly spawned veins will glow, so they are easier to spot)", false, null, null);

	/**
	 * Sets maximum deviation from pre-generated mean height of a chunk
	 */
	public static final Instance<Integer, Integer> GENERATION_HEIGHT_DEVIATION = new Instance<>(
			"Vein height deviation (from mean height, calculated on chunk generation)", 4, null, null);

	private static final List<Util.Point> DEFAULT_CHECK_PATTERN = Arrays.asList(new Util.Point(0, 0),
			new Util.Point(0, 4), new Util.Point(0, 8), new Util.Point(0, 12), new Util.Point(4, 0),
			new Util.Point(4, 4), new Util.Point(4, 8), new Util.Point(4, 12), new Util.Point(8, 0),
			new Util.Point(8, 4), new Util.Point(8, 8), new Util.Point(8, 12), new Util.Point(12, 0),
			new Util.Point(12, 4), new Util.Point(12, 8), new Util.Point(12, 12));
	/**
	 * Represents pattern to use while detecting biomes and heights in chunk.
	 */
	public static final Instance<Collection<Util.Point>, Collection<String>> CHECK_PATTERN = new Instance<>(
			"Check pattern (represents points on the chunk that are used to determine biomes and mean height)",
			DEFAULT_CHECK_PATTERN,
			list -> collectionToStringCollection(list, p -> String.format("x= %d y= %d", p.x, p.z)),
			list -> collectionFromStringCollection(list, s -> {

				Scanner sc = new Scanner(s);
				sc.next();
				int x = sc.nextInt();
				sc.next();
				int z = sc.nextInt();
				sc.close();
				return new Util.Point(x, z);
			}));

	private static final <T> Collection<String> collectionToStringCollection(Collection<T> collection,
			Function<T, String> toString) {
		Collection<String> res = new ArrayList<>();
		Iterator<T> iter = collection.iterator();
		while (iter.hasNext()) {
			T t = iter.next();
			String s = toString.apply(t);
			res.add(s);
		}
		return res;
	}

	private static final <T> Collection<T> collectionFromStringCollection(Collection<String> array,
			Function<String, T> fromString) {
		Collection<T> res = new ArrayList<>(0);
		for (String s : array) {
			T t = fromString.apply(s);
			res.add(t);
		}
		return res;
	}

	private static <T, U> Map<T, U> mapFromEntryCollection(Collection<Entry<T, U>> entryCollection) {
		final Map<T, U> res = new LinkedHashMap<>(0);
		entryCollection.forEach(entry -> res.put(entry.getKey(), entry.getValue()));
		return res;
	}

	/**
	 * Respawn check ticks interval (once in this interval server will iterate
	 * through all ore chunks to spawn new veins); default: 15s
	 */
	public static final Instance<Long, Integer> RESPAWN_CHECK_INTERVAL = new Instance<>(
			"Respawn check ticks interval (once in this interval server will iterate through all ore chunks to spawn new veins)",
			20L * 15L, n -> n.intValue(), n -> (long) n);

	/**
	 * Ore respawn interval ticks (once in this interval ore chunks will attempt to
	 * spawn veins); default: 2h
	 */
	public static final Instance<Long, Integer> ORE_RESPAWN_COOLDOWN = new Instance<>(
			"Ore respawn interval ticks (once in this interval ore chunks will attempt to spawn veins)",
			20L * 60L * 60L * 2L, n -> n.intValue(), n -> (long) n);

	private static final Map<VeinType, Double> DEFAULT_VEIN_HEALTH_MULTIPLIER = new LinkedHashMap<>(0) {
		private static final long serialVersionUID = 1L;

		{
			this.put(VeinType.RAW_IRON, 3.0d);
		}
	};

	public static final Instance<Map<VeinType, Double>, Collection<String>> VEIN_HEALTH_MULTIPLIER = new Instance<>(
			"Vein health multipliers", DEFAULT_VEIN_HEALTH_MULTIPLIER,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s multiplied %.2f times", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				VeinType type = Enum.valueOf(VeinType.class, name);
				sc.next();
				double multiplier = sc.nextDouble();
				sc.close();
				return Map.entry(type, multiplier);
			})));

	private static final Map<ChunkType, Double> DEFAULT_CHUNK_TYPE_COOLDOWN_MODIFIER = new LinkedHashMap<>(0) {
		private static final long serialVersionUID = 1L;

		{
			this.put(ChunkType.COLD_ORE, 0.5d);
		}
	};

	public static final Instance<Map<ChunkType, Double>, Collection<String>> CHUNK_TYPE_COOLDOWN_MULTIPLIER = new Instance<>(
			"Chunk type cooldown multipliers", DEFAULT_CHUNK_TYPE_COOLDOWN_MODIFIER,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s multiplied %.2f times", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				ChunkType type = Enum.valueOf(ChunkType.class, name);
				sc.next();
				double multiplier = sc.nextDouble();
				sc.close();
				return Map.entry(type, multiplier);
			})));

	private static final Map<ChunkType, Integer> DEFAULT_CHUNK_GENERATION_WEIGHTS = new LinkedHashMap<>(0) {
		private static final long serialVersionUID = 1L;

		{
			this.put(ChunkType.NO_ORE, 1);
			this.put(ChunkType.ORE, 1);
		}
	};

	private static final Instance<Map<ChunkType, Integer>, Collection<String>> CHUNK_GENERATION_WEIGHTS = new Instance<>(
			"Chunk type generation weights (theese are proportional to probabillity that a newly generated chunk will be a corresponsing type)",
			DEFAULT_CHUNK_GENERATION_WEIGHTS,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s weight %d", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				ChunkType type = Enum.valueOf(ChunkType.class, name);
				sc.next();
				int weight = sc.nextInt();
				sc.close();
				return Map.entry(type, weight);
			})));

	public static final WeightedRandomHolder<ChunkType> CHUNK_TYPE_GENERATOR;

	private static final Map<ChunkType, Integer> DEFAULT_COLD_CHUNK_TYPE_GENERATION_WEIGHTS = new LinkedHashMap<>(0) {
		private static final long serialVersionUID = 1L;

		{
			this.put(ChunkType.NO_ORE, 1);
			this.put(ChunkType.COLD_ORE, 2);
		}
	};

	private static final Instance<Map<ChunkType, Integer>, Collection<String>> COLD_CHUNK_TYPE_GENERATION_WEIGHTS = new Instance<>(
			"Chunk type generation weights used for cold biomes", DEFAULT_COLD_CHUNK_TYPE_GENERATION_WEIGHTS,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s weight %d", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				ChunkType type = Enum.valueOf(ChunkType.class, name);
				sc.next();
				int weight = sc.nextInt();
				sc.close();
				return Map.entry(type, weight);
			})));

	public static final WeightedRandomHolder<ChunkType> COLD_CHUNK_TYPE_GENERATOR;

	private static final Map<VeinType, Integer> DEFAULT_NO_ORE_VEIN_GENERATION_WEIGHTS = new LinkedHashMap<>(0);

	private static final Instance<Map<VeinType, Integer>, Collection<String>> NO_ORE_VEIN_GENERATION_WEIGHTS = new Instance<>(
			"Vein generation weights used for chunk with no ore (logically is empty)",
			DEFAULT_NO_ORE_VEIN_GENERATION_WEIGHTS,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s weight %d", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				VeinType type = Enum.valueOf(VeinType.class, name);
				sc.next();
				int weight = sc.nextInt();
				sc.close();
				return Map.entry(type, weight);
			})));

	public static final WeightedRandomHolder<VeinType> NO_ORE_VEIN_GENERATOR;

	private static final Map<VeinType, Integer> DEFAULT_ORE_VEIN_GENERATION_WEIGHTS = new LinkedHashMap<>(0) {
		private static final long serialVersionUID = 1L;

		{
			this.put(VeinType.RAW_IRON, 1);
		}
	};

	private static final Instance<Map<VeinType, Integer>, Collection<String>> ORE_VEIN_GENERATION_WEIGHTS = new Instance<>(
			"Vein generation weights used for regular ore chunks", DEFAULT_ORE_VEIN_GENERATION_WEIGHTS,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s weight %d", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				VeinType type = Enum.valueOf(VeinType.class, name);
				sc.next();
				int weight = sc.nextInt();
				sc.close();
				return Map.entry(type, weight);
			})));

	public static final WeightedRandomHolder<VeinType> ORE_VEIN_GENERATOR;

	private static final Map<VeinType, Integer> DEFAULT_COLD_ORE_VEIN_GENERATION_WEIGHTS = new LinkedHashMap<>(0) {
		private static final long serialVersionUID = 1L;

		{
			this.put(VeinType.RAW_IRON, 1);
		}
	};

	private static final Instance<Map<VeinType, Integer>, Collection<String>> COLD_ORE_VEIN_GENERATION_WEIGHTS = new Instance<>(
			"Vein generation weights used for ore chunks generated in cold biomes",
			DEFAULT_COLD_ORE_VEIN_GENERATION_WEIGHTS,
			multipliers -> collectionToStringCollection(multipliers.entrySet(),
					entry -> String.format("%s weight %d", entry.getKey().name(), entry.getValue())),
			collection -> mapFromEntryCollection(collectionFromStringCollection(collection, string -> {
				Scanner sc = new Scanner(string);
				String name = sc.next();
				VeinType type = Enum.valueOf(VeinType.class, name);
				sc.next();
				int weight = sc.nextInt();
				sc.close();
				return Map.entry(type, weight);
			})));

	public static final WeightedRandomHolder<VeinType> COLD_ORE_VEIN_GENERATOR;

	/**
	 * Contains biomes ore chunks should not spawn in
	 */
	private static final List<Biome> DEFAULT_FORBIDDEN_BIOMES = Arrays.asList(Biome.OCEAN, Biome.COLD_OCEAN,
			Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN,
			Biome.DEEP_WARM_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN, Biome.RIVER,
			Biome.FROZEN_RIVER, Biome.DESERT_LAKES);

	public static final Instance<Collection<Biome>, Collection<String>> FORBIDDEN_BIOMES = new Instance<>(
			"Forbidden biomes for vein generation (veins will never spawn in them)", DEFAULT_FORBIDDEN_BIOMES,
			b -> collectionToStringCollection(b, enumSerializer(Biome.class)),
			s -> collectionFromStringCollection(s, enumDeserializer(Biome.class)));

	/**
	 * Contains biomes that have 66% to have ore instead of 50%.
	 */
	private static final List<Biome> DEFAULT_COLD_BIOMES = Arrays.asList(Biome.TAIGA, Biome.TAIGA_HILLS,
			Biome.TAIGA_MOUNTAINS, Biome.GIANT_SPRUCE_TAIGA, Biome.GIANT_SPRUCE_TAIGA_HILLS, Biome.GIANT_TREE_TAIGA,
			Biome.GIANT_TREE_TAIGA_HILLS, Biome.SNOWY_TAIGA, Biome.SNOWY_TAIGA_HILLS, Biome.SNOWY_TAIGA_MOUNTAINS,
			Biome.SNOWY_BEACH, Biome.SNOWY_MOUNTAINS, Biome.SNOWY_TUNDRA);

	public static final Instance<Collection<Biome>, Collection<String>> COLD_BIOMES = new Instance<>(
			"Cold biomes for vein generation (veins spawn differently in them)", DEFAULT_COLD_BIOMES,
			b -> collectionToStringCollection(b, enumSerializer(Biome.class)),
			s -> collectionFromStringCollection(s, enumDeserializer(Biome.class)));

	private static final List<Instance<?, ?>> CONFIGS = Arrays.asList(PICKAXES, ALLOWED_SPAWN_MATERIALS,
			ALLOWED_SPAWN_COVERS, VEINS_GLOW, GENERATION_HEIGHT_DEVIATION, CHECK_PATTERN, RESPAWN_CHECK_INTERVAL,
			ORE_RESPAWN_COOLDOWN, VEIN_HEALTH_MULTIPLIER, CHUNK_TYPE_COOLDOWN_MULTIPLIER, CHUNK_GENERATION_WEIGHTS,
			COLD_CHUNK_TYPE_GENERATION_WEIGHTS, NO_ORE_VEIN_GENERATION_WEIGHTS, ORE_VEIN_GENERATION_WEIGHTS,
			COLD_ORE_VEIN_GENERATION_WEIGHTS, FORBIDDEN_BIOMES, COLD_BIOMES);
//	private static final List<Instance<?, ?>> CONFIGS = Arrays.asList(PICKAXES);

	static {
		CHUNK_TYPE_GENERATOR = new WeightedRandomHolder<>(CHUNK_GENERATION_WEIGHTS.get());
		COLD_CHUNK_TYPE_GENERATOR = new WeightedRandomHolder<>(COLD_CHUNK_TYPE_GENERATION_WEIGHTS.get());

		NO_ORE_VEIN_GENERATOR = new WeightedRandomHolder<>(NO_ORE_VEIN_GENERATION_WEIGHTS.get());
		ORE_VEIN_GENERATOR = new WeightedRandomHolder<>(ORE_VEIN_GENERATION_WEIGHTS.get());
		COLD_ORE_VEIN_GENERATOR = new WeightedRandomHolder<>(COLD_ORE_VEIN_GENERATION_WEIGHTS.get());
	}

	public static void init() {
		final FileConfiguration cfg = SlimeOres.getInstance().getConfig();
		CONFIGS.forEach(config -> config.bind(cfg));
		// Compiling chunk type generators
		CHUNK_TYPE_GENERATOR.compile();
		COLD_CHUNK_TYPE_GENERATOR.compile();
		// Compiling vein type generators
		NO_ORE_VEIN_GENERATOR.compile();
		ORE_VEIN_GENERATOR.compile();
		COLD_ORE_VEIN_GENERATOR.compile();
	}
}
