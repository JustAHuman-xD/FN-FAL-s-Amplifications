package ne.fnfal113.fnamplifications.gems;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import ne.fnfal113.fnamplifications.FNAmplifications;
import ne.fnfal113.fnamplifications.gems.abstracts.AbstractGem;
import ne.fnfal113.fnamplifications.gems.handlers.GemUpgrade;
import ne.fnfal113.fnamplifications.gems.handlers.OnDamageHandler;
import ne.fnfal113.fnamplifications.utils.Utils;
import ne.fnfal113.fnamplifications.utils.WeaponArmorEnum;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ShockwaveGem extends AbstractGem implements OnDamageHandler, GemUpgrade {

    private final Boolean checkMcVersion = Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_17);

    public ShockwaveGem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, 15);
    }

    @Override
    public void onDrag(Player player, SlimefunItem gem, ItemStack gemItem, ItemStack currentItem){
        if (WeaponArmorEnum.HELMET.isTagged(currentItem.getType()) || WeaponArmorEnum.CHESTPLATE.isTagged(currentItem.getType()) ||
                WeaponArmorEnum.LEGGINGS.isTagged(currentItem.getType()) || WeaponArmorEnum.BOOTS.isTagged(currentItem.getType())) {
            if(isUpgradeGem(gemItem, this.getId())) {
                upgradeGem(gem, currentItem, gemItem, player, this.getId());
            } else {
                bindGem(gem, currentItem, player, false);
            }
        } else {
            player.sendMessage(Utils.colorTranslator("&eInvalid item to socket! Gem works on armors only"));
        }
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event, ItemStack itemStack){
        if(event.isCancelled()){
            return;
        }
        if(!(event.getEntity() instanceof Player)){
            return;
        }
        if(!(event.getDamager() instanceof LivingEntity)){
            return;
        }

        Player player = (Player) event.getEntity();
        LivingEntity livingEntity = (LivingEntity) event.getDamager();

        int tier = getTier(itemStack, this.getId());
        double amount = 3.0D * (tier == 4 ? 1 : Math.abs(tier - 5)); // damage multiplier per tier

        if(ThreadLocalRandom.current().nextInt(100) < getChance() / tier){
            sendGemMessage(player, this.getItemName());
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_GATEWAY_SPAWN, 1.0F, 1.0F);
            livingEntity.damage(amount, player);
            livingEntity.setVelocity(new Vector(0, 0.8, 0));

            for (Entity entity: livingEntity.getNearbyEntities(8, 8, 8)) {
                if(entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())){
                    ((Damageable) entity).damage(amount, player);
                    entity.setVelocity(new Vector(0, 0.8, 0));
                }
            }

            AtomicInteger integer = new AtomicInteger(0);
            AtomicDouble height = new AtomicDouble(0.1);
            List<Block> blocks = new ArrayList<>();

            Bukkit.getScheduler().runTaskTimer(FNAmplifications.getInstance(), task ->{
                int rad = integer.getAndIncrement();

                for (double c = 0; c <= 360; c++) {
                    double x = rad * Math.cos(c);
                    double z = rad * Math.sin(c);
                    Block block = player.getLocation().getBlock().getRelative((int) x, -1, (int) z);

                    if(block.getRelative(BlockFace.UP).getType() == Material.AIR && !blocks.contains(block) && block.getType() != Material.AIR) {
                        blocks.add(block);
                        spawnJumpingBlock(block, height.get());
                        height.getAndAdd(0.003);
                    }

                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(x, 0.5, z), 0);
                    player.getWorld().spawnParticle(this.checkMcVersion ? Particle.ELECTRIC_SPARK : Particle.CLOUD, player.getLocation().add(x, 0.5, z), 0);
                }

                if(rad == 8){
                    task.cancel();
                }

            }, 0L, 1L);
        }
    }

    public void spawnJumpingBlock(Block blockOnGround, double height){
        Location loc = blockOnGround.getRelative(BlockFace.UP).getLocation().add(0.5, 0.0, 0.5);
        FallingBlock block = blockOnGround.getWorld().spawnFallingBlock(loc, blockOnGround.getBlockData());
        block.setDropItem(false);
        block.setVelocity(new Vector(0, height, 0));
        block.setMetadata("shockwave_gem", new FixedMetadataValue(FNAmplifications.getInstance(), "ghost_block"));
    }

}