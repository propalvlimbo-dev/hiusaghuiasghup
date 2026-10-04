package ru.rooyzee.elytrixclient;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Общий (main) entrypoint мода. Клиентский код — в ElytrixclientClient (src/client).
 */
public class Elytrixclient implements ModInitializer {
    public static final String MOD_ID = "elytrixclient";
    public static final String MOD_NAME = "ElytrixClient";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        LOG.info("{} core initialized", MOD_NAME);
    }
}
