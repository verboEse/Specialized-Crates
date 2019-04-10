package me.ztowne13.customcrates.interfaces.items;

import me.ztowne13.customcrates.logging.StatusLogger;
import me.ztowne13.customcrates.logging.StatusLoggerEvent;
import me.ztowne13.customcrates.utils.ChatUtils;
import me.ztowne13.customcrates.utils.FileHandler;
import me.ztowne13.customcrates.utils.NMSUtils;
import me.ztowne13.customcrates.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class SaveableItemBuilder extends ItemBuilder implements SaveableItem
{
    public SaveableItemBuilder(ItemStack fromStack)
    {
        super(fromStack);
    }

    public SaveableItemBuilder(DynamicMaterial m, int amnt)
    {
        super(m, amnt);
    }

    @Override
    public void saveItem(FileHandler fileHandler, String prefix)
    {
        FileConfiguration fc = fileHandler.get();
        fc.set(prefix + ".material", DynamicMaterial.fromItemStack(getStack()).name());
        fc.set(prefix + ".name", getDisplayNameStripped());
        fc.set(prefix + ".glow", isGlowing());
        fc.set(prefix + ".amount", getStack().getAmount());
        fc.set(prefix + ".player-head-name", getPlayerHeadName());

        // Enchantments
        if (!getEnchantments().isEmpty())
        {
            ArrayList<String> enchants = new ArrayList<>();
            for (CompressedEnchantment enchant : getEnchantments())
                enchants.add(enchant.toString());

            fc.set(prefix + ".enchantments", enchants);
        }
        else
            fc.set(prefix + ".enchantments", null);

        // Potion Effects
        if (!getPotionEffects().isEmpty())
        {
            ArrayList<String> parsedPots = new ArrayList<>();
            for (CompressedPotionEffect potion : getPotionEffects())
                parsedPots.add(potion.toString());

            fc.set(prefix + ".potion-effects", parsedPots);
        }
        else
            fc.set(prefix + ".potion-effects", null);

        // Lore
        if (!getLore().isEmpty())
            fc.set(prefix + ".lore", getLore());
        else
            fc.set(prefix + ".lore", null);

        // NBT Tags
        if (NMSUtils.Version.v1_12.isServerVersionOrEarlier() && NMSUtils.Version.v1_8.isServerVersionOrLater())
        {
            if (!getNBTTags().isEmpty())
                fc.set(prefix + ".nbt-tags", getNBTTags());
            else
                fc.set(prefix + ".nbt-tags", null);
        }

    }

    @Override
    public boolean loadItem(FileHandler fileHandler, String prefix, StatusLogger statusLogger, StatusLoggerEvent itemFailure,
                            StatusLoggerEvent improperEnchant, StatusLoggerEvent improperPotion,
                            StatusLoggerEvent improperGlow, StatusLoggerEvent improperAmount)
    {
        FileConfiguration fc = fileHandler.get();

        // material
        if (!fc.contains(prefix + ".material"))
        {
            itemFailure.log(statusLogger, new String[]{"The '" + prefix + ".material' value does not exist."});
            return false;
        }
        else
        {
            String mat = fc.getString(prefix + ".material");
            try
            {
                DynamicMaterial dynamicMaterial = DynamicMaterial.fromString(mat);
                setStack(dynamicMaterial.parseItem());
            }
            catch (Exception exc)
            {
                itemFailure.log(statusLogger, new String[]{mat + " is not a valid material."});
                return false;
            }
        }

        // Name
        if (!fc.contains(prefix + ".name"))
        {
            itemFailure.log(statusLogger, new String[]{"The '" + prefix + ".name' value does not exist."});
            return false;
        }
        setDisplayName(fc.getString(prefix + ".name"));

        // Lore
        if (fc.contains(prefix + ".lore"))
            for (String line : fc.getStringList(prefix + ".lore"))
                addLore(line);

        // Enchantments
        convertOldEnchantType(fileHandler, prefix);
        if (fc.contains(prefix + ".enchantments"))
        {
            for (String unparsedEnchant : fc.getStringList(prefix + ".enchantments"))
            {
                try
                {
                    CompressedEnchantment compressedEnchantment = CompressedEnchantment.fromString(unparsedEnchant);
                    addEnchantment(compressedEnchantment);
                }
                catch (Exception exc)
                {
                    improperEnchant.log(statusLogger, new String[]{unparsedEnchant +
                            " is not formatted enchant;level or either the enchant is not a valid enchantment or the level is not a number."});
                }
            }
        }

        // Potion Effects
        if (fc.contains(prefix + ".potion-effects"))
        {
            for (String unparsedPotion : fc.getStringList(prefix + ".potion-effects"))
            {
                try
                {
                    CompressedPotionEffect compressedPotionEffect = CompressedPotionEffect.fromString(unparsedPotion);
                    addPotionEffect(compressedPotionEffect);
                }
                catch (Exception exc)
                {
                    improperPotion.log(statusLogger, new String[]{unparsedPotion +
                            " is not formatted potiontype;duration;amplifier or either the potion is not a valid potion type or the duration/amplifier is not a number."});
                }
            }
        }

        // NBT Tags
        if (fc.contains(prefix + ".nbt-tags"))
            for (String line : fc.getStringList(prefix + ".nbt-tags"))
                addNBTTag(line);

        // Glow

        if (fc.contains(prefix + ".glow"))
        {
            String unparsedGlow = fc.getString(prefix + ".glow");
            if (Utils.isBoolean(unparsedGlow))
                setGlowing(Boolean.parseBoolean(unparsedGlow));
            else
                improperGlow
                        .log(statusLogger, new String[]{"The '" + prefix + ".glow' value is not a proper true/false value"});
        }

        // Amount

        if (fc.contains(prefix + ".amount"))
        {
            String unparsedAmount = fc.getString(prefix + ".amount");
            if (Utils.isInt(unparsedAmount))
                getStack().setAmount(Integer.parseInt(unparsedAmount));
            else
                improperAmount.log(statusLogger, new String[]{"The '" + prefix + ".amount' value is not a valid number."});
        }

        // Player Head Name

        if (fc.contains(prefix + ".player-head-name"))
            setPlayerHeadName(fc.getString(prefix + ".player-head-name"));

        return true;
    }

    public void convertOldEnchantType(FileHandler fileHandler, String prefix)
    {
        FileConfiguration fc = fileHandler.get();
        if (fc.contains(prefix + ".enchantment") && !fc.contains(prefix + ".enchantments"))
        {
            ChatUtils.log("Converting old enchantment format to new enchantment format...");
            try
            {
                boolean isSet = !fc.getStringList(prefix + ".enchantment").isEmpty();
                ArrayList<String> newEnchs = new ArrayList<String>();
                if (isSet)
                {
                    for (String line : fc.getStringList(prefix + ".enchantment"))
                    {
                        try
                        {
                            CompressedEnchantment compressedEnchantment = CompressedEnchantment.fromString(line);
                            newEnchs.add(compressedEnchantment.toString());
                        }
                        catch (Exception exc)
                        {

                        }
                    }
                }
                else
                {
                    String enchant = fc.getString(prefix + ".enchantment");
                    CompressedEnchantment compressedEnchantment = CompressedEnchantment.fromString(enchant);
                    newEnchs.add(compressedEnchantment.toString());
                }

                fc.set(prefix + ".enchantment", null);
                fc.set(prefix + ".enchantments", newEnchs);
                ChatUtils.log("Success. Saving...");
                fileHandler.save();
                ChatUtils.log("Saved.");
            }
            catch (Exception exc)
            {
                ChatUtils.log("Failed.");
                return;
            }
        }
    }
}