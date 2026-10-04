package wtf.expensive.client.ui.alt;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AltConfig {
    private AltConfig() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getGameDir().resolve("expensive").resolve("altManager.exp");
    }

    public static void load() {
        Account.ACCOUNTS.clear();
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String[] parts = line.split(":");
                if (parts.length < 2) {
                    continue;
                }
                try {
                    Account.ACCOUNTS.add(new Account(parts[0], Long.parseLong(parts[1])));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            List<String> lines = new ArrayList<>();
            for (Account account : Account.ACCOUNTS) {
                lines.add(account.name + ":" + account.added);
            }
            Files.write(path, lines);
        } catch (IOException ignored) {
        }
    }
}
