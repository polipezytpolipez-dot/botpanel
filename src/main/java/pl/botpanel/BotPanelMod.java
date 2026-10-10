package pl.botpanel;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BotPanelMod implements ClientModInitializer {
    public static KeyMapping openKey, consoleKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("botpanel", "main"));

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.botpanel.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, category));
        consoleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.botpanel.console", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new BotScreen());
            }
            while (consoleKey.consumeClick()) {
                if (mc.screen == null) BotClient.hud = !BotClient.hud;
            }
        });

        // Widoczna konsola bota na ekranie gry (lewy gorny rog)
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("botpanel", "console"),
                (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!BotClient.hud || mc.options.hideGui) return;
                    if (mc.screen != null && !(mc.screen instanceof ChatScreen)) return;
                    drawConsole(graphics, mc, 4, 4, 280, 9, 0);
                });
    }

    /** Rysuje konsole: ostatnie 'rows' linii, przesuniete o 'scroll' linii w gore. */
    public static void drawConsole(GuiGraphics g, Minecraft mc, int x, int y, int w, int rows, int scroll) {
        List<String> lines = BotClient.lines();
        if (lines.isEmpty()) return;

        List<String> visual = new ArrayList<>();
        for (int i = Math.max(0, lines.size() - 100); i < lines.size(); i++) {
            wrap(mc.font, lines.get(i), w - 8, visual);
        }

        g.fill(x, y, x + w, y + rows * 10 + 6, 0x88000000);
        int end = Math.max(0, visual.size() - scroll);
        int start = Math.max(0, end - rows);
        int ty = y + 4;
        for (int i = start; i < end; i++) {
            g.drawString(mc.font, visual.get(i), x + 4, ty, 0xFFFFFFFF, true);
            ty += 10;
        }
    }

    private static void wrap(Font font, String s, int width, List<String> out) {
        if (s.isEmpty()) { out.add(""); return; }
        while (!s.isEmpty()) {
            int n = s.length();
            while (n > 1 && font.width(s.substring(0, n)) > width) n--;
            out.add(s.substring(0, n));
            s = s.substring(n);
        }
    }
}
