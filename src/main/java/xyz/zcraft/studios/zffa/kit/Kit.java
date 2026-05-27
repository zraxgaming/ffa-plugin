package xyz.zcraft.studios.zffa.kit;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public record Kit(
        String id,
        Component display,
        Material icon,
        boolean allowRegen,
        boolean allowHunger,
        double speedMultiplier,
        double maxHealth,
        List<ItemStack> items,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        List<PotionEffect> effects
) {
    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (ItemStack item : items) {
            player.getInventory().addItem(item.clone());
        }
        player.getInventory().setHelmet(cloneOrNull(helmet));
        player.getInventory().setChestplate(cloneOrNull(chestplate));
        player.getInventory().setLeggings(cloneOrNull(leggings));
        player.getInventory().setBoots(cloneOrNull(boots));
        player.setMaxHealth(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setWalkSpeed((float) Math.max(0.05, Math.min(0.8, 0.2D * speedMultiplier)));
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }

    private ItemStack cloneOrNull(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }
}
