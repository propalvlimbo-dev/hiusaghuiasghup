package platform.client.utils.bridge.discord;

import platform.api.system.interfaces.NativeMethodLookup;
import platform.client.Delta;

import platform.api.system.configs.BaseProcessor;
import platform.client.utils.bridge.discord.ActivityType;

import platform.api.annotation.Compile;
import java.io.IOException;
import lombok.Generated;

public class DiscordProcessor extends BaseProcessor {
    private Object b;

    @Override
    @Compile
    public void setup() {
    }

    @Override
    public void unSetup() {
    }

    static {
        NativeMethodLookup.lookup(DiscordProcessor.class, 25);
    }

    @Generated
    public Object a() {
        return this.b;
    }

    public void a(Void result, Throwable ex) {
    }
}



