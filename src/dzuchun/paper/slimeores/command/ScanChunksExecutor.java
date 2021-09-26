package dzuchun.paper.slimeores.command;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import dzuchun.paper.slimeores.world.OreChunksSystem;
import net.kyori.adventure.text.Component;

public class ScanChunksExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String literal,
			@NotNull String[] args) {
		if (args.length < 1) {
			sender.sendMessage(Component.text("Please provide radius"));
			return false;
		}
		int x0 = 0;
		int z0 = 0;
		World world = null;
		if (sender instanceof Entity) {
			Entity entity = (Entity) sender;
			Chunk chunk = entity.getChunk();
			x0 = chunk.getX();
			z0 = chunk.getZ();
			world = entity.getWorld();
		} else if (sender instanceof BlockCommandSender) {
			BlockCommandSender block = (BlockCommandSender) sender;
			Chunk chunk = block.getBlock().getChunk();
			x0 = chunk.getX();
			z0 = chunk.getZ();
			world = chunk.getWorld();
		} else {
			if (args.length < 4) {
				sender.sendMessage(Component.text(
						"This type of executor required additional args as <chunk x>, <chunk z> and <world name>"));
				return false;
			}
		}
		if (args.length > 3) {
			x0 = Integer.parseInt(args[1]);
			z0 = Integer.parseInt(args[2]);
			world = sender.getServer().getWorld(args[3]);
			if (world == null) {
				sender.sendMessage(Component.text("Your worldname might be incorrect"));
			}
		}
		int radius = Integer.parseInt(args[0]);
		OreChunksSystem.testChunks(world, x0, z0, radius, ()->{
			sender.sendMessage(Component.text("Scan finished"));
		});
		return true;
	}

}
