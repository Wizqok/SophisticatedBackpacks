package net.p3pp3rf1y.sophisticatedbackpacks.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IPickupResponseUpgrade {
	ItemStack pickup(Level world, ItemStack stack, boolean simulate);
}
