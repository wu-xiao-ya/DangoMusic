package me.sorihiko.dango.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.Unpooled;
import me.sorihiko.dango.party.PartyNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PartyNetwork implements PartyNet.Link {
    private final FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(PartyNet.CHANNEL);
    private final ConcurrentLinkedQueue<Runnable> serverPackets = new ConcurrentLinkedQueue<>();
    private volatile NetworkManager remotePartyServer;

    PartyNetwork() {
        channel.register(this);
        FMLCommonHandler.instance().bus().register(this);
        PartyNet.INSTANCE.setLink(this);
    }

    private static FMLProxyPacket packet(String frame) {
        // One plugin message is one frame. FML's UTF8 helper limits strings to
        // 16 KiB, which an 8,000-character frame containing CJK text can exceed.
        // FML 1.7.10 sends the complete backing array, including unused bytes.
        return new FMLProxyPacket(Unpooled.wrappedBuffer(frame.getBytes(StandardCharsets.UTF_8)), PartyNet.CHANNEL);
    }

    @Override
    public boolean toServer(String frame) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getNetHandler();
        if (connection == null) return false;
        NetworkManager manager = connection.getNetworkManager();
        if (!manager.isLocalChannel() && manager != remotePartyServer) return false;
        channel.sendToServer(packet(frame));
        return true;
    }

    @Override
    public void toPlayer(UUID uuid, String frame) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (Object connected : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) connected;
            if (player.getUniqueID().equals(uuid)) {
                channel.sendTo(packet(frame), player);
                return;
            }
        }
    }

    @SubscribeEvent
    public void registration(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.side != Side.CLIENT || !event.registrations.contains(PartyNet.CHANNEL)) return;
        if ("REGISTER".equals(event.operation)) remotePartyServer = event.manager;
        else if (remotePartyServer == event.manager) remotePartyServer = null;
    }

    @SubscribeEvent
    public void receiveClient(FMLNetworkEvent.ClientCustomPacketEvent event) {
        PartyNet.clientReceived(event.packet.payload().toString(StandardCharsets.UTF_8));
    }

    @SubscribeEvent
    public void receiveServer(FMLNetworkEvent.ServerCustomPacketEvent event) {
        EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
        String frame = event.packet.payload().toString(StandardCharsets.UTF_8);
        NetworkManager connection = event.manager;
        serverPackets.add(() -> {
            if (connection.isChannelOpen()) PartyNet.serverReceived(player.getUniqueID(), frame);
        });
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Runnable packet;
        while ((packet = serverPackets.poll()) != null) packet.run();
    }

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (remotePartyServer == event.manager) remotePartyServer = null;
        PartyNet.clientDisconnected();
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PartyNet.playerLeft(event.player.getUniqueID());
    }

    void serverStopped() {
        serverPackets.clear();
        PartyNet.serverStopped();
    }
}
