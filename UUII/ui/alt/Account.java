package wtf.expensive.client.ui.alt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Account {
    public final String name;
    public final long added;
    public PlayerSkin skin;

    public Account(String name, long added) {
        this.name = name;
        this.added = added;

        this.skin = DefaultPlayerSkin.get(resolveUUIDOffline(name));
        CompletableFuture.supplyAsync(() -> resolveUUID(name))
                .thenAccept(this::loadSkin);
    }

    private void loadSkin(UUID uuid) {
        var skinManager = Minecraft.getInstance().getSkinManager();
        if (skinManager == null) return;
        skinManager.get(new GameProfile(uuid, name))
                .thenAccept(result -> result.ifPresent(loaded -> skin = loaded));
    }

    public static UUID resolveUUID(String name) {
        try {
            return resolveUUIDOnline(name);
        } catch (IOException | RuntimeException e) {
            return resolveUUIDOffline(name);
        }
    }

    public static UUID resolveUUIDOnline(String name) throws IOException {
        URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + name);
        try (InputStreamReader in = new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8)) {
            JsonObject obj = new Gson().fromJson(in, JsonObject.class);
            String id = obj.get("id").getAsString();
            return UUID.fromString(id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
            ));
        }
    }

    public static UUID resolveUUIDOffline(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static final List<Account> ACCOUNTS = new ArrayList<>();
}
