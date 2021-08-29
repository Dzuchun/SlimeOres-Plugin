package dzuchun.paper.slimeores.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import dzuchun.paper.slimeores.world.OreChunksSystem;

public class KillVeinsExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		OreChunksSystem.killAllVeins();
		return true;
	}

}
