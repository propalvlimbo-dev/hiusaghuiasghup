package wtf.expensive.client.ui.proxy;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import wtf.expensive.client.util.proxy.ProxyManager;

public final class ProxyScreen extends Screen {
    private final Screen parent;
    private ProxyManager.Type type;
    private boolean enabled;
    private EditBox host;
    private EditBox port;
    private EditBox username;
    private EditBox password;
    private StringWidget status;

    public ProxyScreen(Screen parent) {
        super(Component.literal("Proxy & Versions"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ProxyManager.Settings value = ProxyManager.get();
        type = value.type;
        enabled = value.enabled;

        int x = width / 2 - 100;
        int y = height / 2 - 118;

        addRenderableWidget(new StringWidget(x, y, 200, 20, title, font));

        Button enabledButton = Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
        }).bounds(x, y + 26, 98, 20).build();
        addRenderableWidget(enabledButton);

        Button typeButton = Button.builder(typeLabel(), button -> {
            type = type.next();
            button.setMessage(typeLabel());
        }).bounds(x + 102, y + 26, 98, 20).build();
        addRenderableWidget(typeButton);

        host = field(x, y + 52, 136, "Host / IP", value.host);
        port = field(x + 140, y + 52, 60, "Port", Integer.toString(value.port));
        username = field(x, y + 78, 200, "Username (optional)", value.username);
        password = field(x, y + 104, 200, "Password (optional)", value.password);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(x, y + 132, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(x + 102, y + 132, 98, 20).build());

        boolean viaLoaded = FabricLoader.getInstance().isModLoaded("viafabricplus");
        Button viaButton = Button.builder(
                Component.literal(viaLoaded ? "ViaFabricPlus 4.6.1: installed" : "ViaFabricPlus: missing"),
                button -> {
                    if (viaLoaded) minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                }
        ).bounds(x, y + 158, 200, 20).build();
        viaButton.active = viaLoaded;
        addRenderableWidget(viaButton);

        status = new StringWidget(x, y + 184, 200, 20,
                Component.literal(enabled ? "Proxy will be used for new connections" : "Direct connection is active"), font);
        addRenderableWidget(status);
    }

    private EditBox field(int x, int y, int width, String hint, String value) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.literal(hint));
        field.setHint(Component.literal(hint));
        field.setValue(value == null ? "" : value);
        addRenderableWidget(field);
        return field;
    }

    private Component enabledLabel() {
        return Component.literal("Proxy: " + (enabled ? "ON" : "OFF"));
    }

    private Component typeLabel() {
        return Component.literal("Type: " + type.name());
    }

    private void save() {
        int parsedPort;
        try {
            parsedPort = Integer.parseInt(port.getValue().trim());
        } catch (NumberFormatException exception) {
            status.setMessage(Component.literal("Invalid port"));
            return;
        }
        if (parsedPort < 1 || parsedPort > 65535) {
            status.setMessage(Component.literal("Port must be 1-65535"));
            return;
        }
        if (enabled && host.getValue().isBlank()) {
            status.setMessage(Component.literal("Enter proxy host"));
            return;
        }

        ProxyManager.Settings value = new ProxyManager.Settings();
        value.enabled = enabled;
        value.type = type;
        value.host = host.getValue();
        value.port = parsedPort;
        value.username = username.getValue();
        value.password = password.getValue();
        ProxyManager.set(value);
        ProxyManager.save();
        status.setMessage(Component.literal(enabled ? "Saved. Reconnect to apply proxy" : "Saved. Proxy disabled"));
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
