package dev.eagertest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import dev.eagerlib.core.EagerLib;
import dev.eagerlib.core.HudManager;
import dev.eagerlib.core.Teams;
import dev.eagerlib.platform.Platform;

public final class AutoTest {
	private static int ticks = -1;

	private AutoTest() {}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		ticks++;
		try {
			switch (ticks) {
				case 40 -> setup(mc);
				case 80 -> shot(mc, "hud");
				case 85 -> openMod("crossbow_tags");
				case 112 -> shotIf(mc, "crossbow", "crossbow_tags");
				case 115 -> openMod("knockbackdisplay");
				case 142 -> shotIf(mc, "knockback", "knockbackdisplay");
				case 145 -> EagerLib.openPage("appearance");
				case 150 -> expandAccent();
				case 172 -> shot(mc, "appearance");
				case 175 -> search("flash");
				case 195 -> shot(mc, "search");
				case 200 -> EagerLib.openPage("keybinds");
				case 225 -> shot(mc, "keybinds");
				case 230 -> EagerLib.openPage("changelog");
				case 240 -> ((dev.eagerlib.ui.UiRoot) ui()).mouseScrolled(600, 300, 0, -2);
				case 255 -> shot(mc, "changelog");
				case 260 -> EagerLib.openPage("allmods");
				case 285 -> shot(mc, "allmods");
				case 290 -> EagerLib.openPage("teams");
				case 315 -> shot(mc, "teams");
				case 320 -> EagerLib.openHudEditor(null);
				case 345 -> shot(mc, "hudeditor");
				case 350 -> { Platform.setScreen(null); Platform.setScreen(new InventoryScreen(mc.player)); }
				case 375 -> shot(mc, "inventory");
				case 385 -> { System.out.println("[EagerSmokeTest] DONE"); mc.stop(); }
				default -> {}
			}
		} catch (Throwable t) {
			System.out.println("[EagerSmokeTest] FAILED at tick " + ticks);
			t.printStackTrace(System.out);
			mc.stop();
		}
	}

	private static void setup(Minecraft mc) throws Exception {
		var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> piercing = lookup.getOrThrow(Enchantments.PIERCING);
		Holder<Enchantment> multishot = lookup.getOrThrow(Enchantments.MULTISHOT);
		Holder<Enchantment> quickCharge = lookup.getOrThrow(Enchantments.QUICK_CHARGE);
		Holder<Enchantment> knockback = lookup.getOrThrow(Enchantments.KNOCKBACK);

		Inventory inv = mc.player.getInventory();
		inv.setItem(0, ench(Items.NETHERITE_SWORD, knockback, 2, null, 0));
		inv.setItem(1, ench(Items.CROSSBOW, piercing, 4, quickCharge, 3));
		inv.setItem(2, ench(Items.CROSSBOW, multishot, 1, quickCharge, 3));
		inv.setItem(3, new ItemStack(Items.CROSSBOW));
		inv.setItem(4, ench(Items.STICK, knockback, 1, null, 0));
		inv.setItem(9, ench(Items.CROSSBOW, piercing, 1, null, 0));
		inv.setItem(10, ench(Items.CROSSBOW, multishot, 1, null, 0));
		select(mc, 0);
		for (HudManager.Entry e : HudManager.entries()) e.enabled = true;
		if (Teams.all().isEmpty()) {
			Teams.Team t = Teams.add();
			t.name = "Red Team";
			t.setMembers("Steve, Alex");
			Teams.save();
		}
		System.out.println("[EagerSmokeTest] inventory prepared");
	}

	private static ItemStack ench(Item item, Holder<Enchantment> a, int la, Holder<Enchantment> b, int lb) {
		ItemStack s = new ItemStack(item);
		ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		m.set(a, la);
		if (b != null) m.set(b, lb);
		s.set(DataComponents.ENCHANTMENTS, m.toImmutable());
		return s;
	}

	private static void openMod(String id) {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id)) EagerLib.open(id);
	}

	private static void shotIf(Minecraft mc, String name, String id) throws Exception {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id)) shot(mc, name);
	}

	private static Object ui() throws Exception {
		Object screen = Platform.screen();
		Field root = screen.getClass().getDeclaredField("root");
		root.setAccessible(true);
		return root.get(screen);
	}

	@SuppressWarnings("unchecked")
	private static void expandAccent() throws Exception {
		Object ui = ui();
		Field viewF = ui.getClass().getDeclaredField("view");
		viewF.setAccessible(true);
		Object view = viewF.get(ui);
		Field exp = view.getClass().getDeclaredField("expanded");
		exp.setAccessible(true);
		((java.util.Set<Object>) exp.get(view)).add(dev.eagerlib.core.Settings.ACCENT);
	}

	private static void search(String q) throws Exception {
		Object ui = ui();
		Method m = ui.getClass().getDeclaredMethod("updateQuery", String.class);
		m.setAccessible(true);
		m.invoke(ui, q);
	}

	private static void select(Minecraft mc, int slot) throws Exception {
		Inventory inv = mc.player.getInventory();
		try {
			Inventory.class.getMethod("setSelectedSlot", int.class).invoke(inv, slot);
		} catch (NoSuchMethodException e) {
			Field f = Inventory.class.getField("selected");
			f.setInt(inv, slot);
		}
	}

	private static void shot(Minecraft mc, String name) throws Exception {
		Class<?> ss = Class.forName("net.minecraft.client.Screenshot");
		Consumer<Component> log = c -> System.out.println("[EagerSmokeTest] " + name + ": " + c.getString());
		try {
			Method target = Minecraft.class.getMethod("getMainRenderTarget");
			Class<?> rt = Class.forName("com.mojang.blaze3d.pipeline.RenderTarget");
			ss.getMethod("grab", File.class, String.class, rt, Consumer.class).invoke(null, mc.gameDirectory, name + ".png", target.invoke(mc), log);
		} catch (NoSuchMethodException e) {
			try {
				Method target = Minecraft.class.getMethod("getMainRenderTarget");
				Class<?> rt = Class.forName("com.mojang.blaze3d.pipeline.RenderTarget");
				ss.getMethod("grab", File.class, String.class, rt, int.class, Consumer.class).invoke(null, mc.gameDirectory, name + ".png", target.invoke(mc), 1, log);
			} catch (NoSuchMethodException e2) {
				ss.getMethod("grab", Minecraft.class, boolean.class).invoke(null, mc, true);
			}
		}
		System.out.println("[EagerSmokeTest] screenshot " + name + " at tick " + ticks + " fps=" + mc.getFps() + " fills=" + dev.eagerlib.render.Gfx.lastFrameFills());
	}
}
