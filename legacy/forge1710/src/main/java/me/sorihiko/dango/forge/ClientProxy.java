package me.sorihiko.dango.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import me.sorihiko.dango.legacy.LegacyDango;
import me.sorihiko.dango.legacy.LegacyKeyBindings;
import net.minecraftforge.common.MinecraftForge;

public final class ClientProxy extends DangoMusicMod.Proxy {
    private PartyNetwork party;

    @Override
    public void init() {
        LegacyKeyBindings.register();
        LegacyDango.start();
        party = new PartyNetwork();
        ForgeBridge bridge = new ForgeBridge();
        MinecraftForge.EVENT_BUS.register(bridge);
        FMLCommonHandler.instance().bus().register(bridge);
    }

    @Override
    public void serverStopped() {
        party.serverStopped();
    }
}
