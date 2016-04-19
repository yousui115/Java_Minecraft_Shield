package yousui115.shield;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;

public class EnchantmentGuard extends Enchantment
{

    protected EnchantmentGuard(Rarity rarityIn, EnumEnchantmentType typeIn, EntityEquipmentSlot[] slots)
    {
        super(rarityIn, typeIn, slots);
    }

    @Override
    public int getMinLevel() { return 1; }
    @Override
    public int getMaxLevel() { return 2; }

    @Override
    public int getMinEnchantability(int enchantmentLevel) { return super.getMinEnchantability(enchantmentLevel); }
    @Override
    public int getMaxEnchantability(int enchantmentLevel) { return super.getMaxEnchantability(enchantmentLevel); }

    @Override
    public boolean isTreasureEnchantment() { return true; }

}
