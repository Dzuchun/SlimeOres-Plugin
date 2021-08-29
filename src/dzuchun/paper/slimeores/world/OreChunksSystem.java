package dzuchun.paper.slimeores.world;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import dzuchun.paper.slimeores.Config;
import dzuchun.paper.slimeores.SlimeOres;
import dzuchun.paper.slimeores.data.VeinPersistedDataType;
import dzuchun.paper.slimeores.data.VeinPersistedDataType.VeinType;
import dzuchun.paper.slimeores.util.Util;
import dzuchun.paper.slimeores.util.WeightedRandomHolder;
import net.kyori.adventure.text.Component;

public class OreChunksSystem {
	private static final Logger LOG = SlimeOres.getInstance().LOG;

	private static final Plugin PLUGIN = SlimeOres.getInstance();

	private static final int MAX_HEIGHT_DEFFERENCE = Config.GENERATION_HEIGHT_DEVIATION.get();

	private static final Collection<Util.Point> CHECK_PATTERN = Config.CHECK_PATTERN.get();

	public static class OreState {
		public Long timeGenerated;
		public boolean isGenerated;
		public final int meanHeight;
		public final VeinType veinType;

		public OreState(Chunk chunk, VeinType typeIn) {
			this.isGenerated = false;
			this.timeGenerated = chunk.getWorld().getGameTime();
			this.meanHeight = getMeanHeight(chunk, CHECK_PATTERN.iterator());
			this.veinType = typeIn;
		}

		public OreState(long time, boolean generatedOre, int meanHightIn, VeinType veinTypeIn) {
			this.isGenerated = generatedOre;
			this.timeGenerated = time;
			this.meanHeight = meanHightIn;
			this.veinType = veinTypeIn;
		}
	}

	private static int getMeanHeight(Chunk chunk, Iterator<Util.Point> pattern) {
		int sum = 0;
		int amount = 0;
		while (pattern.hasNext()) {
			Util.Point p = pattern.next();
			int x = chunk.getX() * 16 + p.x;
			int z = chunk.getZ() * 16 + p.z;
			Block b = chunk.getWorld().getHighestBlockAt(x, z);
			amount++;
			sum += b.getY();
		}
		return sum / amount;
	}

	/**
	 * Stores time required for chunk to regenerate ore
	 */
	private static final Long ORE_RESPAWN_INTERVAL = Config.ORE_RESPAWN_COOLDOWN.get();
	// Don't bully me, i'd really appreciate any help you can provide with
	// storing this information more effectively.
	/**
	 * Stores the time all ore chunks were generated
	 */
	private static final Map<Chunk, OreState> ORE_CHUNKS_GENERATED = Maps.synchronizedBiMap(HashBiMap.create());
	private static final Object ORE_CHUNKS_KEY = new Object();

	private static Random rand = new Random();

	private static final WeightedRandomHolder<VeinType> VEIN_TYPE_GENERATOR = Config.VEIN_TYPE_GENERATOR;
	private static final WeightedRandomHolder<VeinType> COLD_VEIN_TYPE_GENERATOR = Config.COLD_VEIN_TYPE_GENERATOR;

	public static boolean checkIfOre(Chunk chunk) {
		if (ORE_CHUNKS_GENERATED.containsKey(chunk)) {
			return true;
		}
		// TODO rewrite
		if (!chunk.getWorld().getName().equals("world")) {
			return false;
		}
		Collection<Biome> biomes = Util.getBiomesInChunk(chunk, CHECK_PATTERN.iterator());
		if (Util.containsAny(biomes, FORBIDDEN_BIOMES)) {
			// Chunk intersects forbidden biome
			return false;
		}
		boolean isCold = Util.containsAny(biomes, COLD_BIOMES);
		long seed = chunk.getWorld().getSeed();
		int x = chunk.getX();
		int z = chunk.getZ();
		long randSeed = seed * (43 + 13 * x + 37 * z);
		rand.setSeed(randSeed);
		WeightedRandomHolder<VeinType> generator = isCold ? COLD_VEIN_TYPE_GENERATOR : VEIN_TYPE_GENERATOR;
		VeinType type = generator.getForRandom(rand);
		boolean isOre = type != VeinType.NONE;
		if (isOre) {
			synchronized (ORE_CHUNKS_KEY) {
				ORE_CHUNKS_GENERATED.put(chunk, new OreState(chunk, type)); // TODO parametrize type
			}
		}
//			LOG.warning(String.format("Chunk at [%d, %d] is an ore chunk: %.2f", chunk.getX(), chunk.getZ(), chance));

		return isOre;
	}

	/**
	 * Contains biomes ore chunks should not spawn in
	 */
	private static final List<Biome> FORBIDDEN_BIOMES = Arrays.asList(Biome.OCEAN, Biome.COLD_OCEAN,
			Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN,
			Biome.DEEP_WARM_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN, Biome.RIVER,
			Biome.FROZEN_RIVER, Biome.DESERT_LAKES);
	/**
	 * Contains biomes that have 75% have ore instead of 50%.
	 */
	private static final List<Biome> COLD_BIOMES = Arrays.asList(Biome.TAIGA, Biome.TAIGA_HILLS, Biome.TAIGA_MOUNTAINS,
			Biome.GIANT_SPRUCE_TAIGA, Biome.GIANT_SPRUCE_TAIGA_HILLS, Biome.GIANT_TREE_TAIGA,
			Biome.GIANT_TREE_TAIGA_HILLS, Biome.SNOWY_TAIGA, Biome.SNOWY_TAIGA_HILLS, Biome.SNOWY_TAIGA_MOUNTAINS,
			Biome.SNOWY_BEACH, Biome.SNOWY_MOUNTAINS, Biome.SNOWY_TUNDRA);

	public static boolean isOre(Chunk chunk) {
		return ORE_CHUNKS_GENERATED.containsKey(chunk);

	}

	public static void readFrom(InputStream inpu) throws IOException {
		if (!ORE_CHUNKS_GENERATED.isEmpty()) {
			LOG.warning("Attempted to read ore chunks once more (not permitted), returning");
			return;
		}
		Server server = PLUGIN.getServer();
		if (inpu.available() > Integer.BYTES) {
			int dims = Util.readInt(inpu);
			for (int i = 0; i < dims; i++) {
				int thisWorldSize = Util.readInt(inpu);
				byte[] bytes = inpu.readNBytes(thisWorldSize);
				ByteBuffer tmp = ByteBuffer.wrap(bytes);
				tmp.rewind();
				String dimName = Util.readStringFromBuffer(tmp);
				World world = server.getWorld(dimName);
				int chunks = tmp.getInt();
				Chunk chunk = null;
				try {
					for (int j = 0; j < chunks; j++) {
						int x = tmp.getInt();
						int z = tmp.getInt();
						chunk = world.getChunkAt(x, z);
						long generatedTime = tmp.getLong();
						boolean isGenerated = (tmp.get()) == 1;
						int n = tmp.getInt();
						VeinType veinType = VeinType.values()[n];
						int meanHeight = tmp.getInt();
						synchronized (ORE_CHUNKS_GENERATED) {
							ORE_CHUNKS_GENERATED.put(chunk,
									new OreState(generatedTime, isGenerated, meanHeight, veinType));
						}
					}
				} catch (BufferUnderflowException e) {
					LOG.warning(String.format(
							"Error while reading %s(my be not a current chunk, if error occured on reading chunk coords) chunk data: %s at %s",
							chunk, e.toString(), e.getStackTrace()[0].toString()));
				}
				LOG.info(String.format("For world %s readed %d chunks", world, chunks));
			}
		} else {
			LOG.warning("Supplied stream has no int to indicate worlds amount - file is invalid");
		}
	}

	public static void writeTo(OutputStream output) throws IOException {
		// Sorting chunks
		Map<World, List<Entry<Chunk, OreState>>> sortedChunks = sortedChunks(e -> true);
		// Writing chunks
		Util.writeInt(output, sortedChunks.size());
		sortedChunks.entrySet().forEach(e -> {
			World world = e.getKey();
			try {
				String worldName = world.getName();
				List<Entry<Chunk, OreState>> chunks = e.getValue();
				int chunksSize = chunks.size();
				int thisWorldBufferSize = Integer.BYTES + worldName.getBytes().length + // For world name
				Integer.BYTES + // For chunks number
				// For each chunk:
				chunksSize * (2 * Integer.BYTES + // For chunk coords
				Long.BYTES + 1 + Integer.BYTES + // For OreState
				Integer.BYTES); // For mean height
				ByteBuffer tmp = ByteBuffer.allocate(thisWorldBufferSize);
				Util.writeStringToBuf(tmp, worldName);
				tmp.putInt(chunksSize);
				LOG.info(String.format("For world %s writing %d ore chunks", world, chunks.size()));
				Chunk chunk = null;
				try {
					for (Entry<Chunk, OreState> entry : chunks) {
						chunk = entry.getKey();
						tmp.putInt(chunk.getX());
						tmp.putInt(chunk.getZ());
						OreState state = entry.getValue();
						tmp.putLong(state.timeGenerated);
						tmp.put((byte) (state.isGenerated ? 1 : 0));
						tmp.putInt(state.veinType.ordinal());
						tmp.putInt(state.meanHeight);
					}
				} catch (BufferUnderflowException | BufferOverflowException e1) {
					LOG.warning(String.format("Failed to write chunk \"%s\": %s at %s", chunk, e1.toString(),
							e1.getStackTrace()[0].toString()));
				}
				tmp.rewind();
				byte[] tmpBytes = tmp.array();
				Util.writeInt(output, tmpBytes.length);
				output.write(tmpBytes);
			} catch (IOException e1) {
				LOG.warning(String.format("Failed to write world \"%s\": %s at %s", world.getName(), e1.toString(),
						e1.getStackTrace()[0].toString()));
			}
		});
	}

	private static Map<World, List<Entry<Chunk, OreState>>> sortedChunks(Predicate<Entry<Chunk, OreState>> condition) {
		final Map<World, List<Entry<Chunk, OreState>>> res = Maps.newHashMap();
		PLUGIN.getServer().getWorlds().forEach(w -> res.put(w, new ArrayList<>()));
		synchronized (ORE_CHUNKS_KEY) {
			ORE_CHUNKS_GENERATED.entrySet().forEach(entry -> {
				if (condition.test(entry)) {
					Chunk chunk = entry.getKey();
					res.get(chunk.getWorld()).add(entry);
				}
			});
		}
		return res;
	}

	private static final BukkitScheduler SCHEDULER = Bukkit.getScheduler();

	public static void checkAndSpawnVeins() {
		final Map<Location, VeinType> list = getVeinsSpawnMap();
		SCHEDULER.scheduleSyncDelayedTask(PLUGIN, () -> list.forEach(OreChunksSystem::spawnVein), 0);
	}

	/**
	 * @return a map containing locations and type server should spawn veins later
	 *         on
	 */
	private static Map<Location, VeinType> getVeinsSpawnMap() {
		final Map<Location, VeinType> res = new LinkedHashMap<>();
		// Sorting chunks
		Map<World, List<Entry<Chunk, OreState>>> sortedChunks = sortedChunks(e -> !e.getValue().isGenerated);
		sortedChunks.entrySet().forEach(e -> {
			World world = e.getKey();
			// Filtering chunks, so only ones need spawning will remain
			// Sorting chunks, so the most recent will be first
			List<Entry<Chunk, OreState>> chunks = e.getValue().parallelStream()
					.filter(entry -> !(entry.getValue().isGenerated))
					.sorted((e1, e2) -> (int) (e1.getValue().timeGenerated - e2.getValue().timeGenerated)).toList();
			int size = chunks.size();
			if (size == 0) {
				// Returning immediately, if no chunk needs generation
//				LOG.warning(String.format("No chunks in dimension \"%s\" need generation, returning immediately",
//						world.getName()));
				return;
			}
			long gameTime = e.getKey().getGameTime();
			int gens = 0;
			int fails = 0;
			for (int i = 0; i < size; i++) {
				Entry<Chunk, OreState> entry = chunks.get(i);
				OreState state = entry.getValue();
				if (gameTime < state.timeGenerated) {
					break;
				}
				Chunk chunk = entry.getKey();
				Location genLoc = getVeinSpawnLoc(chunk, state);
				if (genLoc != null) {
					res.put(genLoc, state.veinType);
					state.isGenerated = true;
					gens++;
				} else {
					setHarvested(state, gameTime);
					fails++;
				}
			}
			LOG.warning(
					String.format("For dimension \"%s\" generated ore in %d chunks, %d chunks failed, %d total pending",
							world.getName(), gens, fails, size - gens));
		});
		return res;
	}

	private static final Collection<Material> ALLOWED_MATERIALS = Config.ALLOWED_SPAWN_MATERIALS.get();
	private static final Collection<Material> ALLOWED_COVERS = Config.ALLOWED_SPAWN_COVERS.get();

	private static Random spawnRand = new Random();

	private static Predicate<Block> spawnBlockPredicate(OreState state) {
		return b -> b.isSolid() && ALLOWED_MATERIALS.contains(b.getType())
				&& ((Math.abs(b.getY() - state.meanHeight) <= MAX_HEIGHT_DEFFERENCE));
	}

	private static final boolean VEINS_GLOW = Config.VEINS_GLOW.get();

	private static Location getVeinSpawnLoc(Chunk chunk, OreState state) {
		World world = chunk.getWorld();
		int x = chunk.getX() * 16 + spawnRand.nextInt(15);
		int z = chunk.getZ() * 16 + spawnRand.nextInt(15);
		Block block = getHighestBlockAt(world, x, z, spawnBlockPredicate(state));
		if (block == null) {
//			LOG.info(String.format("For chunk at [%d, %d] it's too high or too low for ore spawn", chunk.getX(),
//					chunk.getZ()));
			return null;
		}
//		if (!ALLOWED_MATERIALS.contains(block.getType())) {
//			LOG.info(String.format("For chunk at [%d, %d] was selected not allowed block: %s", chunk.getX(),
//					chunk.getZ(), block.getType().name()));
//			return false;
//		}
		Material cover = block.getRelative(BlockFace.UP).getType();
		if (!ALLOWED_COVERS.contains(cover)) {
//			LOG.info(String.format("For chunk at [%d, %d] was selected not allowed cover (block above): %s",
//					chunk.getX(), chunk.getZ(), cover.name()));
			return null;
		}
		Location loc = new Location(world, x + spawnRand.nextDouble(), block.getY() + 1, z + spawnRand.nextDouble(),
				spawnRand.nextFloat() * 360.0f, 0.0f);
		return loc;
	}

	public static void spawnVein(Location loc, VeinType type) {
		World world = loc.getWorld();
		Entity spawned = world.spawnEntity(loc, EntityType.SLIME);
		Slime slime = (Slime) spawned;
		double sizeRand = spawnRand.nextDouble();
		if (sizeRand < 0.5d) {
			// Small chunk
			slime.customName(Component.text("малая жила железа"));
			slime.setSize(1);
		} else if (sizeRand < 0.75d) {
			// Medium chunk
			slime.customName(Component.text("средняя жила железа"));
			slime.setSize(2);
		} else if (sizeRand < 0.9d) {
			// Large chunk
			slime.customName(Component.text("большая жила железа"));
			slime.setSize(3);
		} else {
			// Giant chunk
			slime.customName(Component.text("гиганская жила железа"));
			slime.setSize(4);
//			slime.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
		}
//		allignChunkWithSurface(slime);
		slime.setAI(false);
		slime.setPersistent(true);
		slime.setGlowing(VEINS_GLOW);
		slime.setMaximumNoDamageTicks(20);
		VeinPersistedDataType.attachVeinType(slime, type);
		// Setting max health according to multiplier
		AttributeInstance slimeMaxHealth = slime.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		AttributeModifier multiplier = type.getHealthMultiplier();
		slimeMaxHealth.addModifier(multiplier);
		double maxHealth = slimeMaxHealth.getValue();
		slime.setHealth(maxHealth);
	}

	private static Block getHighestBlockAt(World world, int x, int z, Predicate<Block> predicate) {
		int max = world.getMaxHeight();
		int min = world.getMinHeight();
		int y = max;
		Block block = null;
		for (; y >= min; y--) {
			block = world.getBlockAt(x, y, z);
			if (predicate.test(block)) {
				return block;
			}
		}
		return null;
	}

	public static void setHarvested(Chunk chunk) {
//			LOG.warning(String.format("Settings chunk at [%d, %d] as harvested", chunk.getX(), chunk.getZ()));
		OreState state = ORE_CHUNKS_GENERATED.get(chunk);
		if (state != null) {
			long gameTime = chunk.getWorld().getGameTime();
			setHarvested(state, gameTime);
		}

	}

	public static void setHarvested(OreState state, long gameTime) {
		state.isGenerated = false;
		state.timeGenerated = gameTime + ORE_RESPAWN_INTERVAL;
	}

	/**
	 * Kills all existing veins on server
	 */
	public static void killAllVeins() {
		SCHEDULER.scheduleSyncDelayedTask(PLUGIN, () -> {
			final List<Entity> allEntities = PLUGIN.getServer().getWorlds().stream()
					.flatMap(w -> w.getEntities().stream()).toList();
			SCHEDULER.runTaskAsynchronously(PLUGIN, () -> {
				final Stream<LivingEntity> veins = getVeins(allEntities);
				SCHEDULER.scheduleSyncDelayedTask(PLUGIN, () -> {
					veins.forEach(v -> {
						v.damage(999999.0d);
					});
				});
			});
		});
	}

	private static Stream<LivingEntity> getVeins(List<Entity> allEntities) {
		return allEntities.stream().filter(e -> ((e instanceof Slime) && (Util.isVein((Slime) e))))
				.map(e -> (LivingEntity) e);
	}

	public static void setVeinType(Chunk chunk, VeinType type) {
		if (type == VeinType.NONE) {
			synchronized (ORE_CHUNKS_KEY) {
				ORE_CHUNKS_GENERATED.remove(chunk);
			}
		} else {
			final OreState state = new OreState(chunk, type);
			synchronized (ORE_CHUNKS_KEY) {
				ORE_CHUNKS_GENERATED.put(chunk, state);
			}
		}
	}
}
