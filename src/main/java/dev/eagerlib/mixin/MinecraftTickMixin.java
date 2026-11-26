package dev.eagerlib.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.eagerlib.core.EagerLib;

@Mixin(Minecraft.class)
abstract class MinecraftTickMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void eagerlib$tick(CallbackInfo ci) {
		EagerLib.tick();
	}
}
