package dev.eagerlib.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.eagerlib.core.HudManager;
import dev.eagerlib.platform.GraphicsCanvas;

@Mixin(Hud.class)
abstract class HudMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void eagerlib$hud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		HudManager.render(new GraphicsCanvas(graphics));
	}
}
