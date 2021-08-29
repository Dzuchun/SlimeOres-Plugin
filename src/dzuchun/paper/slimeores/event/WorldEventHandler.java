package dzuchun.paper.slimeores.event;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;

import dzuchun.paper.slimeores.Config;
import dzuchun.paper.slimeores.SlimeOres;
import dzuchun.paper.slimeores.world.OreChunksSystem;

public class WorldEventHandler implements Listener {
	@SuppressWarnings("unused")
	private static final Logger LOG = SlimeOres.getInstance().LOG;

	private static final BukkitScheduler SCHEDULER = Bukkit.getScheduler();
	private static final Plugin SLIME_ORES = SlimeOres.getInstance();

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if (event.isNewChunk()) {
			final Chunk chunk = event.getChunk();
			SCHEDULER.runTaskAsynchronously(SLIME_ORES, () -> OreChunksSystem.checkIfOre(chunk));
		}
	}

	private static final Long CHECK_INTERVAL = Config.RESPAWN_CHECK_INTERVAL.get();

	@EventHandler
	public void onServerTickEnd(ServerTickEndEvent event) {
		long tick = event.getTickNumber();
		if ((tick % CHECK_INTERVAL) == 0) {
//			OreChunksSystem.checkAndSpawnOre();
		}
	}
}
