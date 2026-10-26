package dev.zero.pvpthings.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("imageWidth")
	int zeropvp$getImageWidth();

	@Accessor("imageHeight")
	int zeropvp$getImageHeight();

	@Accessor("hoveredSlot")
	Slot zeropvp$getHoveredSlot();
}
