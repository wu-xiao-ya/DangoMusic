package me.sorihiko.dango.forge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

@Mod(modid = "dangomusic", name = "Dango Music",
        acceptedMinecraftVersions = "[1.7.10]", acceptableRemoteVersions = "*")
public final class DangoMusicMod {
    @SidedProxy(clientSide = "me.sorihiko.dango.forge.ClientProxy",
            serverSide = "me.sorihiko.dango.forge.DangoMusicMod$Proxy")
    public static Proxy proxy;

    // A dedicated server can load the jar without resolving Minecraft client classes.
    public static class Proxy {
        public void init() {}
        public void serverStopped() {}
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped();
    }
}
