package dev.eagertest.mixin;

import dev.eagertest.AutoTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftTickMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void eagerTest$tick(CallbackInfo ci) {
		AutoTest.tick((Minecraft) (Object) this);
	}
}
