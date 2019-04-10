package me.ztowne13.customcrates.interfaces.items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CompressedPotionEffect extends PotionEffect
{
    public CompressedPotionEffect(PotionEffectType potionEffectType, int duration, int amplifier)
    {
        super(potionEffectType, duration, amplifier);
    }

    ItemStack applyTo(ItemStack stack)
    {
        if(stack.getItemMeta() instanceof PotionMeta)
        {
            PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();

            potionMeta.addCustomEffect(this, true);
            stack.setItemMeta(potionMeta);
        }

        return stack;
    }

    @Override
    public String toString()
    {
        return getType().getName() + ";" + getDuration() + ";" + getAmplifier();
    }

    public static CompressedPotionEffect fromString(String value)
    {
        String[] split = value.split(";");

        PotionEffectType potionEffectType = PotionEffectType.getByName(split[0]);
        int duration = Integer.parseInt(split[1]);
        int amplifier = Integer.parseInt(split[2]);

        return new CompressedPotionEffect(potionEffectType, duration, amplifier);
    }
}