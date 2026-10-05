package platform.client.features.commands;

import platform.api.annotation.Command;

/** Алиас `.config` для `.cfg` — как просил пользователь. */
@Command(a = "config")
public class ConfigAliasCommand extends ConfigCommand {
}
