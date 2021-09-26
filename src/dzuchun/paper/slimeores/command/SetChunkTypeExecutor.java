package dzuchun.paper.slimeores.command;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import dzuchun.paper.slimeores.world.OreChunksSystem;
import dzuchun.paper.slimeores.world.OreChunksSystem.ChunkPos;
import dzuchun.paper.slimeores.world.OreChunksSystem.ChunkType;
import net.kyori.adventure.text.Component;

public class SetChunkTypeExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (sender instanceof Entity) {
			Chunk chunk = ((Entity) sender).getChunk();
			String typeName = args[0];
			ChunkType type = ChunkType.valueOf(typeName);
			OreChunksSystem.setChunkType(new ChunkPos(chunk), type);
			return true;
		} else {
			sender.sendMessage(Component.text("This command should be executed by an entity"));
			return false;
		}
	}

}
