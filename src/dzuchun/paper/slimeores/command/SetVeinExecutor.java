package dzuchun.paper.slimeores.command;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import dzuchun.paper.slimeores.data.VeinPersistedDataType.VeinType;
import dzuchun.paper.slimeores.world.OreChunksSystem;
import net.kyori.adventure.text.Component;

public class SetVeinExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (sender instanceof Entity) {
			Chunk chunk = ((Entity) sender).getChunk();
			String typeName = args[0];
			VeinType type = VeinType.valueOf(typeName);
			OreChunksSystem.setVeinType(chunk, type);
			return true;
		} else {
			sender.sendMessage(Component.text("This command should be executed by an entity"));
			return false;
		}
	}

}
