package dzuchun.paper.slimeores;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import dzuchun.paper.slimeores.command.KillVeinsExecutor;
import dzuchun.paper.slimeores.command.SetVeinExecutor;
import dzuchun.paper.slimeores.command.SpawnOreCommandExecutor;
import dzuchun.paper.slimeores.command.SpawnVeinsExecutor;
import dzuchun.paper.slimeores.event.EntityEventHandler;
import dzuchun.paper.slimeores.event.WorldEventHandler;
import dzuchun.paper.slimeores.world.OreChunksSystem;

public class SlimeOres extends JavaPlugin {
	private static SlimeOres instance;

	public final Logger LOG;

	public static SlimeOres getInstance() {
		return instance;
	}

	public SlimeOres() {
		LOG = this.getLogger();
	}

	private static final String savedOreChunksPath = "plugins/SlimeOres/ore_chunks.slimeores";

	private static BukkitScheduler scheduler;

	@Override
	public void onEnable() {
		LOG.info("SlimeOres enabled");
		instance = this;
		scheduler = Bukkit.getScheduler();
		LOG.info("Setting up config");
		this.reloadConfig();
		Config.init();
		this.saveDefaultConfig();
		LOG.info("Reading ore chunks");
		File file = new File(savedOreChunksPath);
		try {
			FileInputStream input = new FileInputStream(file);
			OreChunksSystem.readFrom(input);
			input.close();
		} catch (FileNotFoundException e) {
			LOG.warning(String.format(
					"ore_chunks.slimeores file was not found, so plugin will assume there are no ore chunks among already generated ones. Mentioned file will be created if server shuts down properly."));
		} catch (IOException e) {
			LOG.warning(String.format("Error while reading ore chunks: %s at %s", e, e.getStackTrace()[0].toString()));
		}
		LOG.info("Registering listeners");
		PluginManager manager = this.getServer().getPluginManager();
		manager.registerEvents(new EntityEventHandler(), this);
		manager.registerEvents(new WorldEventHandler(), this);
		LOG.info("Registering commands");
		// spawnore
		String spawnOreName = "spawnore";
		PluginCommand spawnOreCommand = this.getCommand(spawnOreName);
		CommandExecutor spawnOreExecutor = new SpawnOreCommandExecutor();
		spawnOreCommand.setExecutor(spawnOreExecutor);
		// killveins
		String killVeinsName = "killveins";
		PluginCommand killVeinsCommand = this.getCommand(killVeinsName);
		CommandExecutor killVeinsExecutor = new KillVeinsExecutor();
		killVeinsCommand.setExecutor(killVeinsExecutor);
		// setvein
		String setVeinName = "setvein";
		PluginCommand setVeinCommand = this.getCommand(setVeinName);
		CommandExecutor setVeinExecutor = new SetVeinExecutor();
		setVeinCommand.setExecutor(setVeinExecutor);
		// spawnveins
		String spawnVeinsName = "spawnveins";
		PluginCommand spawnVeinsCommand = this.getCommand(spawnVeinsName);
		CommandExecutor spawnVeinsExecutor = new SpawnVeinsExecutor();
		spawnVeinsCommand.setExecutor(spawnVeinsExecutor);
		LOG.info("Staring spawnore tasks...");
		final long interval = Config.RESPAWN_CHECK_INTERVAL.get();
		@SuppressWarnings({ "deprecation", "unused" })
		int a = scheduler.scheduleAsyncRepeatingTask(this, () -> OreChunksSystem.checkAndSpawnVeins(), interval,
				interval);
	}

	@Override
	public void onDisable() {
		LOG.info("Canceling scheduled tasks...");
		scheduler.cancelTasks(this);
		LOG.info("Saving ore-suppliying chunks...");
		File file = new File(savedOreChunksPath);
		FileOutputStream output;
		try {
			file.createNewFile();
			output = new FileOutputStream(file);
			OreChunksSystem.writeTo(output);
			output.flush();
			output.close();
		} catch (Exception e) {
			LOG.warning(String.format("Exception while writing chunks (data might be lost): %s at %s", e,
					e.getStackTrace()[0].toString()));
		}
		LOG.info("Saving config");
		this.saveConfig();
	}

}
