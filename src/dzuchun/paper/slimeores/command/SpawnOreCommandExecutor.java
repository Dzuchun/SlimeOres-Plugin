package dzuchun.paper.slimeores.command;

import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import dzuchun.paper.slimeores.SlimeOres;
import dzuchun.paper.slimeores.data.VeinPersistedDataType.VeinType;
import dzuchun.paper.slimeores.world.OreChunksSystem;
import net.kyori.adventure.text.Component;

public class SpawnOreCommandExecutor implements CommandExecutor {
	@SuppressWarnings("unused")
	private static final Logger LOG = SlimeOres.getInstance().LOG;

	private static final Random rand = new Random();

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage(Component.text("You are not OP"));
			return false;
		}
		if (args.length < 1) {
			sender.sendMessage(Component.text("Please provide radius to generate in"));
			return false;
		}
		Double radius = 0.0d;
		try {
			radius = Double.parseDouble(args[0]);
		} catch (NumberFormatException e) {
			sender.sendMessage(Component.text("Provided radius is not a double number"));
			return false;
		}
		Location center;
		World world;
		if (sender instanceof Entity) {
			world = ((Entity) sender).getWorld();
			center = ((Entity) sender).getLocation();
		} else if (sender instanceof BlockCommandSender) {
			world = ((BlockCommandSender) sender).getBlock().getWorld();
			center = ((BlockCommandSender) sender).getBlock().getLocation();
		} else {
			world = SlimeOres.getInstance().getServer().getWorlds().iterator().next();
			center = new Location(world, 0.0d, 0.0d, 0.0d);
		}
		double azimuth = rand.nextDouble() * 2 * Math.PI, declination = Math.acos((rand.nextDouble() * 2) - 1);
		Location randomLocation = new Location(world,
				center.getX() + radius * Math.cos(azimuth) * Math.sin(declination),
				center.getY() + radius * Math.sin(azimuth) * Math.sin(declination),
				center.getZ() + radius * Math.cos(declination));
		OreChunksSystem.spawnVein(randomLocation, VeinType.RAW_IRON);
		return true;
	}

}
