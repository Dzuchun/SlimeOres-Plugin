package dzuchun.paper.slimeores.event;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;

import dzuchun.paper.slimeores.Config;
import dzuchun.paper.slimeores.SlimeOres;
import dzuchun.paper.slimeores.loot.OreChunkLootTable;
import dzuchun.paper.slimeores.util.Util;
import dzuchun.paper.slimeores.world.OreChunksSystem;

public class EntityEventHandler implements Listener {

	@SuppressWarnings("unused")
	private static final Logger LOG = SlimeOres.getInstance().LOG;

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		Entity e = event.getEntity();
		if (e instanceof Slime) {
			// Entity is a slime
			Slime slime = (Slime) e;
			if (Util.isVein(slime)) {
				// Entity is a slime acting like an ore chunk
				slime.setCustomName(null);
				Player player = slime.getKiller();
				if (player != null) {
//					LOG.info("Ore chunk killed by player");
					onOreChunkCollect(event);
				} else {
					event.getDrops().clear();
				}
				// Setting time chunk harvested and harvested status
				OreChunksSystem.setHarvested(slime.getChunk());
			}
		}
	}

	private void onOreChunkCollect(EntityDeathEvent event) {
		final List<ItemStack> drops = event.getDrops();
		drops.clear();
		LivingEntity entity = event.getEntity();
		LootContext.Builder builder = new LootContext.Builder(entity.getLocation());
		builder.lootedEntity(entity);
		Player killer = entity.getKiller();
		builder.killer(killer);
//		builder.luck((float) killer.getAttribute(Attribute.GENERIC_LUCK).getValue());
		builder.lootingModifier(
				killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS));
		LootContext ctx = builder.build();
		Collection<ItemStack> loot = OreChunkLootTable.INSTANCE.populateLoot(ctx);
//		LOG.warning("Loot: " + loot);
		loot.forEach(stack -> drops.add(stack));
		event.setDroppedExp(0);
	}

	@EventHandler
	public void onSlimeSplit(SlimeSplitEvent event) {
		Slime slime = event.getEntity();
		if (Util.isVein(slime)) {
			event.setCancelled(true);
		}
	}

	private static final Map<Material, Double> PICKAXES = Config.PICKAXES.get();

	@EventHandler
	public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
		Entity target = event.getEntity();
		if (target instanceof Slime) {
			// Target is a slime
			final Slime slime = (Slime) target;
			if (Util.isVein(slime)) {
				// Slime acts as ore
				Entity damager = event.getDamager();
				if (damager instanceof Player) {
					// Damager is a player
					Player player = (Player) damager;
					ItemStack item = player.getInventory().getItemInMainHand();
					if (PICKAXES.keySet().contains(item.getType())) {
						// Player holds a pickaxe
						float strength = player.getCooledAttackStrength(0);
						if (strength == 1.0f) {
							double basePickDamage = PICKAXES.get(item.getType());
							int efficienyEnchantment = item.getEnchantmentLevel(Enchantment.DIG_SPEED);
							// TODO modify
							double resultDamage = basePickDamage * (1 + efficienyEnchantment / 2.0d);
							event.setDamage(resultDamage);
						} else {
							event.setCancelled(true);
						}
					} else {
						event.setCancelled(true);
					}
				} else {
					event.setCancelled(true);
				}
			}
		}
	}

}
