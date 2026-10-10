package pl.botpanel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BotScreen extends Screen {
    private EditBox nameField, addressField, chatField;
    private Button joinButton, hudButton;
    private int scroll = 0;

    public BotScreen() { super(Component.literal("Bot Panel")); }

    @Override
    protected void init() {
        int w = Math.min(340, width - 20);
        int x = (width - w) / 2;

        nameField = new EditBox(font, x, 30, w / 2 - 4, 20, Component.literal("Nazwa"));
        nameField.setMaxLength(16);
        nameField.setValue(BotClient.name);
        addRenderableWidget(nameField);

        String addr = "";
        ServerData sd = Minecraft.getInstance().getCurrentServer();
        if (sd != null) addr = sd.ip;
        addressField = new EditBox(font, x + w / 2 + 4, 30, w / 2 - 4, 20, Component.literal("Adres"));
        addressField.setMaxLength(100);
        addressField.setValue(addr);
        addressField.setHint(Component.literal("adres serwera"));
        addRenderableWidget(addressField);

        joinButton = Button.builder(Component.literal(""), b -> toggle())
                .bounds(x, 56, w * 2 / 3 - 2, 20).build();
        addRenderableWidget(joinButton);

        hudButton = Button.builder(Component.literal(""), b -> BotClient.hud = !BotClient.hud)
                .bounds(x + w * 2 / 3 + 2, 56, w - w * 2 / 3 - 2, 20).build();
        addRenderableWidget(hudButton);

        chatField = new EditBox(font, x, height - 52, w - 54, 20, Component.literal("Czat"));
        chatField.setMaxLength(256);
        chatField.setHint(Component.literal("wiadomosc lub komenda, Enter = wyslij"));
        addRenderableWidget(chatField);

        addRenderableWidget(Button.builder(Component.literal("Wyslij"), b -> sendChat())
                .bounds(x + w - 50, height - 52, 50, 20).build());

        addRenderableWidget(Button.builder(Component.literal("/register"), b -> fill("/register haslo haslo"))
                .bounds(x, height - 28, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("/login"), b -> fill("/login haslo"))
                .bounds(x + w / 2 + 2, height - 28, w / 2 - 2, 20).build());
    }

    private void fill(String s) {
        chatField.setValue(s);
        setFocused(chatField);
    }

    private void sendChat() {
        BotClient.send(chatField.getValue());
        chatField.setValue("");
        scroll = 0;
    }

    private void toggle() {
        if (BotClient.connected()) {
            BotClient.leave();
            BotClient.add("[Bot] Wyszedl z serwera.");
        } else {
            String a = addressField.getValue().trim();
            if (a.isEmpty()) { BotClient.add("[Bot] Wpisz adres serwera."); return; }
            String host = a;
            int port = 25565;
            int i = a.lastIndexOf(':');
            if (i > 0) {
                try { port = Integer.parseInt(a.substring(i + 1)); host = a.substring(0, i); }
                catch (NumberFormatException ignored) {}
            }
            String n = nameField.getValue().trim();
            if (n.isEmpty()) n = "Bot";
            BotClient.join(host, port, n);
        }
    }

    // Enter wysyla wiadomosc. Jesli ta metoda nie kompiluje sie, usun ja: dziala tez guzik "Wyslij".
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (chatField.isFocused()
                && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            sendChat();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(300, scroll + (int) Math.signum(scrollY) * 3));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        joinButton.setMessage(Component.literal(BotClient.connected() ? "§cWyjdz z serwera" : "§a§lWEJDZ NA SERWER"));
        hudButton.setMessage(Component.literal(BotClient.hud ? "Konsola HUD: ON" : "Konsola HUD: OFF"));

        int w = Math.min(340, width - 20);
        int x = (width - w) / 2;
        int top = 84, bottom = height - 58;
        int rows = Math.max(1, (bottom - top - 6) / 10);
        if (BotClient.lines().isEmpty()) g.fill(x, top, x + w, bottom, 0x88000000);
        BotPanelMod.drawConsole(g, Minecraft.getInstance(), x, top, w, rows, scroll);
        g.drawString(font, "Nazwa bota / adres serwera", x, 20, 0xFFAAAAAA, true);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
