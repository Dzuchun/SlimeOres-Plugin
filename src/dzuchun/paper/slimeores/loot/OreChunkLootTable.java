package dzuchun.paper.slimeores.loot;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import dzuchun.paper.slimeores.SlimeOres;

public class OreChunkLootTable implements LootTable {
	@SuppressWarnings("unused")
	private static final Logger LOG = SlimeOres.getInstance().LOG;

	public static final OreChunkLootTable INSTANCE = new OreChunkLootTable();

	private OreChunkLootTable() {
	}

	public static final NamespacedKey KEY = new NamespacedKey(SlimeOres.getInstance(), "orechunk_loottable");

	@Override
	public @NotNull NamespacedKey getKey() {
		return KEY;
	}

	private Random rand = new Random();

	public @NotNull Collection<ItemStack> populateLoot(@NotNull LootContext context) {
		return populateLoot(rand, context);
	}

	@Override
	public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
//		LOG.warning("Populating loot");
		Entity e = context.getLootedEntity();
		Slime slime = (Slime) e;
		int dropAmount = 0;
		double rd = rand.nextDouble();
		switch (slime.getSize()) {
		case 1:
			dropAmount = lootLerp(0, 1, rd);
			break;
		case 2:
			dropAmount = lootLerp(1, 2, rd);
			break;
		case 3:
			dropAmount = lootLerp(2, 3, rd);
			break;
		case 4:
			dropAmount = lootLerp(3, 5, rd);
			break;
		}
		int fortune = context.getLootingModifier();
		dropAmount += fortune / 2;
		if ((fortune != 0) && (fortune % 2 == 0)) {
			dropAmount += random.nextBoolean() ? 1 : 0;
		}
//		LOG.info(String.format("Returning %d raw iron", dropAmount));
		return Collections.singletonList(new ItemStack(Material.RAW_IRON, dropAmount));
	}

	private int lootLerp(int min, int max, double part) {
		double dif = max - min + 1;
		return Math.min((int) (dif * part + min), max);
	}

	@Override
	public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
	}

}
