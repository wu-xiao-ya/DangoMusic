package me.sorihiko.dango.forge;

import io.netty.buffer.Unpooled;
import me.sorihiko.dango.party.PartyNet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/**
 * 一起听歌经当前服务器同步的网络包（局域网 / 内网穿透联机时，服务器就是房主的游戏）。
 * 只负责把一段段文字收发出去，拆包、拼包和房间数据都在 {@link PartyNet}。
 * 通道是可选的：没装这个 mod 的玩家照样能进服务器，只是不会走这条路。
 */
public final class PartyNetwork {
    private static FMLEventChannel channel;
    private static volatile NetworkManager partyServer;

    private PartyNetwork() {}

    private static FMLProxyPacket packet(String frame) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeString(frame);
        return new FMLProxyPacket(buf, PartyNet.CHANNEL);
    }

    private static String read(FMLProxyPacket packet) {
        return new PacketBuffer(packet.payload()).readString(32767);
    }

    /** 只注册一次：同一个通道注册两次，新版本的加载器会直接让游戏启动失败 */
    private static boolean registered;

    static void register() {
        if (registered) return;
        registered = true;
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(PartyNet.CHANNEL);
        PartyNetwork handler = new PartyNetwork();
        channel.register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
        PartyNet.INSTANCE.setLink(new PartyNet.Link() {
            @Override
            public boolean toServer(String frame) {
                NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
                if (connection == null) return false;
                NetworkManager manager = connection.getNetworkManager();
                if (!manager.isLocalChannel() && manager != partyServer) return false;
                channel.sendToServer(packet(frame));
                return true;
            }

            @Override
            public void toPlayer(UUID player, String frame) {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                EntityPlayerMP p = server == null ? null : server.getPlayerList().getPlayerByUUID(player);
                if (p != null) channel.sendTo(packet(frame), p);
            }
        });
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        String frame = read(event.getPacket());
        player.getServerWorld().addScheduledTask(() -> {
            if (player.connection.netManager.isChannelOpen()) PartyNet.serverReceived(player.getUniqueID(), frame);
        });
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        PartyNet.clientReceived(read(event.getPacket()));
    }

    @SubscribeEvent
    public void onRegistration(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.getSide() == Side.CLIENT && event.getRegistrations().contains(PartyNet.CHANNEL)) {
            if ("REGISTER".equals(event.getOperation())) partyServer = event.getManager();
            else if (partyServer == event.getManager()) partyServer = null;
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (partyServer == event.getManager()) partyServer = null;
        PartyNet.clientDisconnected();
    }

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PartyNet.playerLeft(event.player.getUniqueID());
    }
}
