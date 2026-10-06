package de.heyy.arcane;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import io.papermc.paper.datacomponent.DataComponentTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ArcaneGreatswordPlugin extends JavaPlugin implements Listener {

    private static final double NORMAL_DAMAGE = 24.0; // 12 hearts: 7 hearts above Sharpness III Netherite's 5 hearts
    private static final double SPECIAL_DAMAGE = 20.0; // 10 hearts
    private static final long SPECIAL_COOLDOWN_MS = 8000L;

    private final Set<UUID> cooldown = new HashSet<>();
    private org.bukkit.NamespacedKey swordKey;

    @Override
    public void onEnable() {
        swordKey = new org.bukkit.NamespacedKey(this, "arcane_greatsword");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("arcane").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) return true;
            if (!player.hasPermission("arcane.give")) return true;
            player.getInventory().addItem(createSword());
            player.sendMessage(Component.text("Du hast das ", NamedTextColor.GRAY)
                    .append(Component.text("Arcane Greatsword", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                    .append(Component.text(" erhalten!", NamedTextColor.GRAY)));
            return true;
        });
        getLogger().info("ArcaneGreatsword aktiviert.");
    }

    public ItemStack createSword() {
        ItemStack sword = new ItemStack(org.bukkit.Material.NETHERITE_SWORD);
        sword.editMeta(meta -> {
            meta.displayName(Component.text("Arcane Greatsword", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            meta.getPersistentDataContainer().set(swordKey, PersistentDataType.BYTE, (byte) 1);
        });
        // 26.2 uses the item_model data component to select the client-side resource-pack model.
        sword.setData(DataComponentTypes.ITEM_MODEL, Key.key("arcane_greatsword:arcane_greatsword"));
        return sword;
    }

    private boolean isSword(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.NETHERITE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(swordKey, PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isSword(player.getInventory().getItemInMainHand())) return;

        event.setDamage(NORMAL_DAMAGE);
        spawnHitEffects(event.getEntity().getLocation().add(0, 1, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.65f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.7f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!isSword(player.getInventory().getItemInMainHand())) return;

        // The special attack only works when the offhand is not a shield.
        if (player.getInventory().getItemInOffHand().getType() == org.bukkit.Material.SHIELD) {
            player.sendActionBar(Component.text("Spezialangriff blockiert: Schild in der Nebenhand", NamedTextColor.RED));
            return;
        }

        if (cooldown.contains(player.getUniqueId())) {
            player.sendActionBar(Component.text("Spezialangriff lädt noch...", NamedTextColor.AQUA));
            return;
        }

        RayTraceResult hit = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), 30.0,
                0.35,
                entity -> entity instanceof LivingEntity && entity != player
        );

        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity target)) {
            player.sendActionBar(Component.text("Du musst einen Gegner ansehen!", NamedTextColor.RED));
            return;
        }

        cooldown.add(player.getUniqueId());
        event.setCancelled(true);
        launchSword(player, target);

        new BukkitRunnable() {
            @Override public void run() {
                cooldown.remove(player.getUniqueId());
            }
        }.runTaskLater(this, SPECIAL_COOLDOWN_MS / 50L);
    }

    private void launchSword(Player player, LivingEntity target) {
        World world = player.getWorld();
        Location start = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().multiply(1.2));
        Location end = target.getLocation().clone().add(0, Math.max(0.8, target.getHeight() * 0.55), 0);
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        world.playSound(start, Sound.ITEM_TRIDENT_THROW, 1.3f, 0.65f);
        world.playSound(start, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.8f);

        ItemDisplay display = (ItemDisplay) world.spawnEntity(start, EntityType.ITEM_DISPLAY);
        display.setItemStack(createSword());
        display.setGlowing(true);
        display.setViewRange(1.5f);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(2.8f, 2.8f, 2.8f),
                new Quaternionf()
        ));

        new BukkitRunnable() {
            double travelled = 0;
            @Override public void run() {
                if (!display.isValid() || !target.isValid() || travelled >= distance) {
                    finishSpecial(display, target, world);
                    cancel();
                    return;
                }

                travelled += 0.9;
                Location pos = start.clone().add(direction.clone().multiply(travelled));
                display.teleport(pos);
                spawnTrail(pos);

                if (pos.distanceSquared(end) < 1.8) {
                    finishSpecial(display, target, world);
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void finishSpecial(ItemDisplay display, LivingEntity target, World world) {
        if (display.isValid()) display.remove();
        if (target.isValid()) {
            target.damage(SPECIAL_DAMAGE);
            target.setVelocity(target.getVelocity().add(new Vector(0, 0.35, 0)));
        }
        Location p = target.getLocation().add(0, Math.max(1, target.getHeight() * 0.55), 0);
        spawnHitEffects(p);
        for (int i = 0; i < 5; i++) {
            world.spawnParticle(Particle.END_ROD, p, 18, 0.8, 1.0, 0.8, 0.08);
        }
        world.playSound(p, Sound.ITEM_TRIDENT_THUNDER, 1.1f, 1.3f);
        world.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
    }

    private void spawnTrail(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(70, 220, 255), 1.2f);
        world.spawnParticle(Particle.DUST, location, 10, 0.45, 0.45, 0.45, 0.02, cyan);
        world.spawnParticle(Particle.ELECTRIC_SPARK, location, 5, 0.35, 0.35, 0.35, 0.03);
        world.spawnParticle(Particle.END_ROD, location, 2, 0.2, 0.2, 0.2, 0.01);
    }

    private void spawnHitEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(70, 220, 255), 1.4f);
        world.spawnParticle(Particle.DUST, location, 65, 0.9, 1.0, 0.9, 0.16, cyan);
        world.spawnParticle(Particle.ELECTRIC_SPARK, location, 35, 0.8, 1.0, 0.8, 0.12);
        world.spawnParticle(Particle.CRIT, location, 25, 0.7, 0.9, 0.7, 0.2);
        world.spawnParticle(Particle.END_ROD, location, 14, 0.5, 0.7, 0.5, 0.08);
    }
}
