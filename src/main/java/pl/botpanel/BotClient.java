package pl.botpanel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Jedyna klasa zalezna od biblioteki MCProtocolLib. */
public class BotClient {
    public static String name = "Bot";
    public static boolean hud = true;
    private static final List<String> log = new ArrayList<>();
    private static Session session;

    public static synchronized List<String> lines() { return new ArrayList<>(log); }

    public static synchronized void add(String s) {
        log.add(s.replaceAll("§.", ""));
        while (log.size() > 300) log.remove(0);
    }

    public static boolean connected() { return session != null && session.isConnected(); }

    public static void join(String host, int port, String botName) {
        leave();
        name = botName;
        add("[Bot] Laczenie z " + host + ":" + port + " jako " + botName + "...");
        new Thread(() -> {
            try {
                MinecraftProtocol protocol = new MinecraftProtocol(botName); // tryb offline
                ClientSession s = ClientNetworkSessionFactory.factory()
                        .setRemoteSocketAddress(new InetSocketAddress(host, port))
                        .setProtocol(protocol)
                        .create();
                s.addListener(new SessionAdapter() {
                    @Override public void connected(ConnectedEvent e) { add("[Bot] Polaczono."); }

                    @Override public void packetReceived(Session ss, Packet p) {
                        if (p instanceof ClientboundLoginPacket) {
                            add("[Bot] Wszedl na serwer. Wpisz /register lub /login.");
                        } else if (p instanceof ClientboundSystemChatPacket c) {
                            add(text(c.getContent()));
                        } else if (p instanceof ClientboundPlayerChatPacket c) {
                            add("<" + text(c.getName()) + "> " + c.getContent());
                        }
                    }

                    @Override public void disconnected(DisconnectedEvent e) {
                        add("[Bot] Rozlaczono: " + text(e.getReason()));
                    }
                });
                session = s;
                s.connect();
            } catch (Throwable t) {
                add("[Bot] Blad: " + t);
            }
        }, "botpanel-connect").start();
    }

    public static void leave() {
        if (session != null) {
            session.disconnect(Component.text("bye"));
            session = null;
        }
    }

    public static void send(String msg) {
        if (!connected() || msg.isBlank()) return;
        try {
            if (msg.startsWith("/")) {
                session.send(new ServerboundChatCommandPacket(msg.substring(1)));
            } else {
                session.send((Packet) chatPacket(msg));
            }
            add("> " + msg);
        } catch (Throwable t) {
            add("[Bot] Nie udalo sie wyslac: " + t);
        }
    }

    /** Konstruktor ServerboundChatPacket zmienia sie miedzy wersjami, wiec dobieramy argumenty po typach. */
    private static Object chatPacket(String msg) throws Exception {
        for (Constructor<?> c : ServerboundChatPacket.class.getConstructors()) {
            Class<?>[] t = c.getParameterTypes();
            if (t.length == 0 || t[0] != String.class) continue;
            Object[] a = new Object[t.length];
            a[0] = msg;
            for (int i = 1; i < t.length; i++) {
                if (t[i] == long.class) a[i] = (i == 1) ? System.currentTimeMillis() : 0L;
                else if (t[i] == int.class) a[i] = 0;
                else if (t[i] == byte.class) a[i] = (byte) 0;
                else if (t[i] == BitSet.class) a[i] = new BitSet();
                else a[i] = null;
            }
            return c.newInstance(a);
        }
        throw new IllegalStateException("brak pasujacego konstruktora ServerboundChatPacket");
    }

    private static String text(Object o) {
        if (o == null) return "";
        if (!(o instanceof Component c)) return o.toString();
        StringBuilder sb = new StringBuilder();
        if (c instanceof TextComponent t) sb.append(t.content());
        else if (c instanceof TranslatableComponent tr) {
            sb.append(tr.key());
            for (Component a : tr.args()) sb.append(' ').append(text(a));
        }
        for (Component ch : c.children()) sb.append(text(ch));
        return sb.toString();
    }
}
