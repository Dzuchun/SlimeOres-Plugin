package dzuchun.paper.slimeores.world;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

public class OreChunksSystem {
	private static final Logger LOG = SlimeOres.getInstance().LOG;

	private static final Plugin PLUGIN = SlimeOres.getInstance();

	private static final int MAX_HEIGHT_DEFFERENCE = Config.GENERATION_HEIGHT_DEVIATION.get();

	private static final Collection<Util.Point> CHECK_PATTERN = Config.CHECK_PATTERN.get();

	public static class OreState {
		public static final int BYTES = Long.BYTES + 1 + Integer.BYTES * 2;

		public Long timeGenerated;
		public boolean isGenerated;
		public final int meanHeight;
		public final ChunkType chunkType;

		public OreState(ChunkPos chunk, ChunkType typeIn) {
			this.isGenerated = false;
			this.timeGenerated = chunk.world.getGameTime() + (long) (rand.nextDouble() * ORE_RESPAWN_INTERVAL);
			this.meanHeight = getMeanHeight(chunk, CHECK_PATTERN.iterator());
			this.chunkType = typeIn;
		}

		public OreState(ByteBuffer source) {
			this(source.getLong(), source.get() == 1 ? true : false, source.getInt(),
					ChunkType.values()[source.getInt()]);
		}

		private OreState(long time, boolean generatedOre, int meanHightIn, ChunkType veinTypeIn) {
			this.isGenerated = generatedOre;
			this.timeGenerated = time;
			this.meanHeight = meanHightIn;
			this.chunkType = veinTypeIn;
		}

		public void toBuffer(ByteBuffer buffer) {
			buffer.putLong(this.timeGenerated);
			buffer.put((byte) (this.isGenerated ? 1 : 0));
			buffer.putInt(this.meanHeight);
			buffer.putInt(this.chunkType.ordinal());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof OreState) {
				OreState state = (OreState) obj;
				return (this.timeGenerated == state.timeGenerated) && (this.isGenerated == state.isGenerated)
						&& (this.meanHeight == state.meanHeight) && (this.chunkType == state.chunkType);
			} else {
				return super.equals(obj);
			}
		}
	}

	private static int getMeanHeight(ChunkPos chunk, Iterator<Util.Point> pattern) {
		int sum = 0;
		int amount = 0;
		while (pattern.hasNext()) {
			Util.Point p = pattern.next();
			int x = chunk.x * 16 + p.x;
			int z = chunk.z * 16 + p.z;
			Block b = chunk.world.getHighestBlockAt(x, z);
			amount++;
			sum += b.getY();
		}
		return sum / amount;
	}

	public static enum ChunkType {
		NO_ORE, ORE, COLD_ORE;

		private ChunkType() {
		}

		private WeightedRandomHolder<VeinType> veinTypeDeterminer;

		public VeinType determineVein(Random rand) {
			if (veinTypeDeterminer == null) {
				veinTypeDeterminer = getHolderForType(this);
			}
			return veinTypeDeterminer.getForRandom(rand);
		}

		private static WeightedRandomHolder<VeinType> getHolderForType(ChunkType type) {
			switch (type) {
			case NO_ORE:
				return Config.NO_ORE_VEIN_GENERATOR;
			case ORE:
				return Config.ORE_VEIN_GENERATOR;
			case COLD_ORE:
				return Config.COLD_ORE_VEIN_GENERATOR;
			}
			LOG.warning(String.format(
					"Tried to get vein generator for chunk type %s, which has no assignes generator. Please, contact support.",
					type.toString()));
			return null;
		}
	}

	public static class ChunkPos {
		public final int x;
		public final int z;
		public final World world;

		/**
		 *
		 * @param chunkIn != null
		 */
		public ChunkPos(Chunk chunkIn) {
			this.x = chunkIn.getX();
			this.z = chunkIn.getZ();
			lastChunk = chunkIn;
			world = chunkIn.getWorld();
		}

		/**
		 *
		 * @param xIn
		 * @param zIn
		 * @param worldIn != null
		 */
		public ChunkPos(int xIn, int zIn, World worldIn) {
			this.x = xIn;
			this.z = zIn;
			world = worldIn;
		}

		private Chunk lastChunk = null;

		/**
		 * Uses {@link World#getChunkAt(int, int)}, which is VERY SLOW
		 *
		 * @param worldIn Uses this world to get the chunk, if no world set.
		 * @return
		 */
		public Chunk get() {
			if (lastChunk == null) {
				lastChunk = world.getChunkAt(x, z);
			}
			return lastChunk;
		}

		public Chunk getNew() {
			lastChunk = world.getChunkAt(x, z);
			return lastChunk;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ChunkPos)) {
				return false;
			}
			ChunkPos pos = (ChunkPos) obj;
			return (pos.x == this.x) && (pos.z == this.z) && (pos.world == this.world);
		}
	}

	// Don't bully me, i'd really appreciate any help you can provide with
	// storing this information more effectively.
	/**
	 * Stores the time all ore chunks were generated
	 */
	private static final Map<ChunkPos, OreState> ORE_CHUNKS = Maps.synchronizedBiMap(HashBiMap.create());

//	private static final Object ORE_CHUNKS_KEY = new Object();
	private static OreState getOreState(ChunkPos pos) {
		synchronized (ORE_CHUNKS) {
			Optional<ChunkPos> found = ORE_CHUNKS.keySet().stream().filter(cPos -> cPos.equals(pos)).findAny();
			if (found.isPresent()) {
				return ORE_CHUNKS.get(found.get());
			} else {
				LOG.warning("getOreChunk returned null; contact support.");
				return null;
			}
		}
	}

	private static Random rand = new Random();

	private static final WeightedRandomHolder<ChunkType> VEIN_TYPE_GENERATOR = Config.CHUNK_TYPE_GENERATOR;
	private static final WeightedRandomHolder<ChunkType> COLD_VEIN_TYPE_GENERATOR = Config.COLD_CHUNK_TYPE_GENERATOR;

	private static final Collection<Biome> FORBIDDEN_BIOMES = Config.FORBIDDEN_BIOMES.get();
	private static final Collection<Biome> COLD_BIOMES = Config.COLD_BIOMES.get();

	/**
	 * Not nesessary to invoke in main thread
	 *
	 * @param chunk chunk to test
	 * @return is chunk is an ore chunk (all required actions are already commited).
	 */
	public static boolean checkIfOre(ChunkPos chunkPos) {
		if (ORE_CHUNKS.containsKey(chunkPos)) {
			return true;
		}
		// TODO restricts for dimension
		if (!chunkPos.world.getName().equals("world")) {
			return false;
		}
		Collection<Biome> biomes = Util.getBiomesInChunk(chunkPos, CHECK_PATTERN.iterator());
		if (Util.containsAny(biomes, FORBIDDEN_BIOMES)) {
			// Chunk intersects forbidden biome
			return false;
		}
		boolean isCold = Util.containsAny(biomes, COLD_BIOMES);
		long seed = chunkPos.world.getSeed();
		int x = chunkPos.x;
		int z = chunkPos.z;
		long randSeed = seed * (43 + 13 * x + 37 * z);
		rand.setSeed(randSeed);
		WeightedRandomHolder<ChunkType> generator = isCold ? COLD_VEIN_TYPE_GENERATOR : VEIN_TYPE_GENERATOR;
		ChunkType type = generator.getForRandom(rand);
		boolean isOre = type != ChunkType.NO_ORE;
		if (isOre) {
			final OreState state = new OreState(chunkPos, type);
			synchronized (ORE_CHUNKS) {
				ORE_CHUNKS.put(chunkPos, state);
			}
		}
//			LOG.warning(String.format("Chunk at [%d, %d] is an ore chunk: %.2f", chunk.getX(), chunk.getZ(), chance));

		return isOre;
	}

	public static boolean isOre(ChunkPos chunk) {
		return ORE_CHUNKS.containsKey(chunk);

	}

	public static void readFrom(InputStream input) throws IOException {
		long beginTime = System.currentTimeMillis();
		if (!ORE_CHUNKS.isEmpty()) {
			LOG.warning("Attempted to read ore chunks once more (not permitted), returning");
			return;
		}
		Server server = PLUGIN.getServer();
		if (input.available() > Integer.BYTES) {
			int dims = Util.readInt(input);
			for (int i = 0; i < dims; i++) {
				int thisWorldSize = Util.readInt(input);
				byte[] bytes = input.readNBytes(thisWorldSize);
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
						ChunkPos pos = new ChunkPos(x, z, world);
						OreState state = new OreState(tmp);
						ORE_CHUNKS.put(pos, state);
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
		LOG.finer(String.format("Readed chunks in %dms", System.currentTimeMillis() - beginTime));
	}

	public static void writeTo(OutputStream output) throws IOException {
//		LOG.warning(String.format("There are %d ore chunks now", ORE_CHUNKS.size()));
		// Sorting chunks
		Map<World, List<Entry<ChunkPos, OreState>>> sortedChunks = sortedChunks(e -> true);
		// Writing chunks
		Util.writeInt(output, sortedChunks.size());
		sortedChunks.entrySet().forEach(e -> {
			World world = e.getKey();
			try {
				String worldName = world.getName();
				List<Entry<ChunkPos, OreState>> chunks = e.getValue(); // TODO remove?
				int chunksSize = chunks.size();
				int thisWorldBufferSize = Integer.BYTES + worldName.getBytes().length + // For world name
				Integer.BYTES + // For chunks number
				// For each chunk:
				chunksSize * (2 * Integer.BYTES // For chunk coords
						+ OreState.BYTES); // For ore state
				ByteBuffer tmp = ByteBuffer.allocate(thisWorldBufferSize);
				Util.writeStringToBuf(tmp, worldName);
				tmp.putInt(chunksSize);
				LOG.fine(String.format("For world %s writing %d ore chunks", world, chunks.size()));
				ChunkPos chunk = null;
				try {
					for (Entry<ChunkPos, OreState> entry : chunks) {
						chunk = entry.getKey();
						tmp.putInt(chunk.x);
						tmp.putInt(chunk.z);
						entry.getValue().toBuffer(tmp);
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

	private static Map<World, List<Entry<ChunkPos, OreState>>> sortedChunks(
			Predicate<Entry<ChunkPos, OreState>> condition) {
		final Map<World, List<Entry<ChunkPos, OreState>>> res = Maps.newHashMap();
		PLUGIN.getServer().getWorlds().forEach(w -> res.put(w, new ArrayList<>()));
		synchronized (ORE_CHUNKS) {
			ORE_CHUNKS.entrySet().forEach(entry -> {
				if (condition.test(entry)) {
					ChunkPos chunkPos = entry.getKey();
					res.get(chunkPos.world).add(entry);
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
	 * A random object used to determine type of the vein and generation position
	 */
	private static Random spawnRand = new Random();

	/**
	 * @return a map containing locations and type server should spawn veins later
	 *         on
	 */
	private static Map<Location, VeinType> getVeinsSpawnMap() {
		final Map<Location, VeinType> res = new LinkedHashMap<>();
		// Sorting chunks
		Map<World, List<Entry<ChunkPos, OreState>>> sortedChunks = sortedChunks(e -> !e.getValue().isGenerated);
		sortedChunks.entrySet().forEach(e -> {
			World world = e.getKey();
			// Filtering chunks, so only ones need spawning will remain
			// Sorting chunks, so the most recent will be first
			List<Entry<ChunkPos, OreState>> chunks = e.getValue().parallelStream()
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
				Entry<ChunkPos, OreState> entry = chunks.get(i);
				OreState state = entry.getValue();
				if (gameTime < state.timeGenerated) {
					break;
				}
				ChunkPos chunk = entry.getKey();
				Location genLoc = getVeinSpawnLoc(chunk, state);
				if (genLoc != null) {
					ChunkType chunkType = state.chunkType;
					VeinType type = chunkType.determineVein(spawnRand);
					res.put(genLoc, type);
					state.isGenerated = true;
					gens++;
				} else {
					setHarvested(state, gameTime);
					fails++;
				}
			}
			LOG.fine(String.format("For dimension \"%s\" generated ore in %d chunks, %d chunks failed", world.getName(),
					gens, fails));
		});
		return res;
	}

	private static final Collection<Material> ALLOWED_MATERIALS = Config.ALLOWED_SPAWN_MATERIALS.get();
	private static final Collection<Material> ALLOWED_COVERS = Config.ALLOWED_SPAWN_COVERS.get();

	private static Predicate<Block> spawnBlockPredicate(OreState state) {
		return b -> b.isSolid() && ALLOWED_MATERIALS.contains(b.getType())
				&& ((Math.abs(b.getY() - state.meanHeight) <= MAX_HEIGHT_DEFFERENCE));
	}

	private static final boolean VEINS_GLOW = Config.VEINS_GLOW.get();

	private static Location getVeinSpawnLoc(ChunkPos chunk, OreState state) {
		World world = chunk.world;
		int x = chunk.x * 16 + spawnRand.nextInt(15);
		int z = chunk.z * 16 + spawnRand.nextInt(15);
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
		int size = type.generateSize(spawnRand);
		slime.setSize(size);
		slime.customName(type.getNameForSize(size));
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

	/**
	 * Stores time required for chunk to regenerate ore
	 */
	private static final Long ORE_RESPAWN_INTERVAL = Config.ORE_RESPAWN_COOLDOWN.get();
	/**
	 * Stores multipliers applied to {@link ORE_RESPAWN_INTERVAL} according to chunk
	 * type
	 */
	private static final Map<ChunkType, Double> COOLDOWN_MULTIPLIERS = Config.CHUNK_TYPE_COOLDOWN_MULTIPLIER.get();

	public static void setHarvested(Chunk chunk) {
//			LOG.warning(String.format("Settings chunk at [%d, %d] as harvested", chunk.getX(), chunk.getZ()));
		OreState state = getOreState(new ChunkPos(chunk));
		if (state != null) {
			long gameTime = chunk.getWorld().getGameTime();
			setHarvested(state, gameTime);
		}

	}

	public static void setHarvested(OreState state, long gameTime) {
		state.isGenerated = false;
		state.timeGenerated = (long) (gameTime
				+ ORE_RESPAWN_INTERVAL * COOLDOWN_MULTIPLIERS.getOrDefault(state.chunkType, 1.0d));
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

	public static void setChunkType(ChunkPos chunk, ChunkType type) {
		if (type == ChunkType.NO_ORE) {
			synchronized (ORE_CHUNKS) {
				ORE_CHUNKS.remove(chunk);
			}
		} else {
			final OreState state = new OreState(chunk, type);
			synchronized (ORE_CHUNKS) {
				ORE_CHUNKS.put(chunk, state);
			}
		}
	}

	/**
	 * Not nesessary to invoke in main thread
	 *
	 * @param chunks iterator that contains all chunks to test
	 * @return chunks that were added to ore chunks list
	 */
	public static Collection<ChunkPos> checkAllIfOre(Iterator<ChunkPos> chunks) {
		final Map<ChunkPos, OreState> newChunks = new LinkedHashMap<>(0);
		while (chunks.hasNext()) {
			ChunkPos chunk = chunks.next();
			// TODO restricts for dimension
			if (ORE_CHUNKS.containsKey(chunk) || !chunk.world.getName().equals("world")) {
				continue;
			}
			Collection<Biome> biomes = Util.getBiomesInChunk(chunk, CHECK_PATTERN.iterator());
			if (Util.containsAny(biomes, FORBIDDEN_BIOMES)) {
				// Chunk intersects forbidden biome
				continue;
			}
			boolean isCold = Util.containsAny(biomes, COLD_BIOMES);
			long seed = chunk.world.getSeed();
			int x = chunk.x;
			int z = chunk.z;
			long randSeed = seed * (43 + 13 * x + 37 * z);
			rand.setSeed(randSeed);
			WeightedRandomHolder<ChunkType> generator = isCold ? COLD_VEIN_TYPE_GENERATOR : VEIN_TYPE_GENERATOR;
			ChunkType type = generator.getForRandom(rand);
			boolean isOre = type != ChunkType.NO_ORE;
			if (isOre) {
				final OreState state = new OreState(chunk, type);
				newChunks.put(chunk, state);
			}
		}
		synchronized (ORE_CHUNKS) {
			LOG.info(String.format("Scan found %d chunks", newChunks.size()));
			newChunks.forEach((c, s) -> ORE_CHUNKS.put(c, s));
		}

		return newChunks.keySet();
	}

	public static void testChunks(World world, int x0, int z0, int radius, Runnable onEnd) {
		SCHEDULER.runTaskAsynchronously(PLUGIN, () -> {
			synchronized (ORE_CHUNKS) {
				ORE_CHUNKS.forEach((c, s) -> {
					setChunkType(c, null);
				});
				ORE_CHUNKS.clear();
			}
			final int lastX = x0 + radius;
			final int lastZ = z0 + radius;
			checkAllIfOre(new Iterator<ChunkPos>() {
				int x = x0 - radius;
				int z = z0 - radius;
				boolean hasNextInternal = true;
				long lastReport = 0;
				double totalChunks = (2 * radius + 1) * (2 * radius + 1);
				int scannedChunks = 0;

				@Override
				public boolean hasNext() {
					return hasNextInternal;
				}

				@Override
				public ChunkPos next() {
					ChunkPos res = new ChunkPos(x, z, world);
					z++;
					if (z > lastZ) {
						z = z0 - radius;
						x++;
					}
					if (x > lastX) {
						hasNextInternal = false;
					}
					long current = System.currentTimeMillis();
					if ((current - lastReport) > 60000L) {
						lastReport = current;
						LOG.info(String.format("Chunks cranning is %.1f percent done",
								100.0d * scannedChunks / totalChunks));
					}
					scannedChunks++;
					return res;
				}
			});
			// TODO ?????
//			for (int x = x0 - radius; x < lastX; x++) {
//				for (int z = z0 - radius; z < lastZ; z++) {
//					checkIfOre(world.getChunkAt(x, z));
//				}
//			}
			if (onEnd != null) {
				onEnd.run();
			}
		});
	}
}
