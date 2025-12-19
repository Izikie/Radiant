package net.minecraft.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.network.NetHandlerHandshakeMemory;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.network.encoder.MessageDeserializer;
import net.minecraft.network.encoder.MessagePrepender;
import net.minecraft.network.encoder.MessageSerializer;
import net.minecraft.network.encoder.MessageSplitter;
import net.minecraft.network.packet.api.PacketDirection;
import net.minecraft.network.packet.impl.play.server.S40PacketDisconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.chat.ChatComponentText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkSystem.class);

    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> LAN_EVENT_GROUP = new LazyLoadBase<>() {
        @Override
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Server IO #%d", 0).factory(), NioIoHandler.newFactory());
        }
    };
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> LOCAL_EVENT_GROUP = new LazyLoadBase<>() {
        @Override
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Local Server IO #%d", 0).factory(), LocalIoHandler.newFactory());
        }
    };

    private final List<ChannelFuture> endpoints = Collections.synchronizedList(new ArrayList<>());
    private final List<NetworkManager> networkManagers = Collections.synchronizedList(new ArrayList<>());
    private final MinecraftServer server;
    public volatile boolean isAlive;

    public NetworkSystem(MinecraftServer server) {
        this.server = server;
        this.isAlive = true;
    }

    public void addLanEndpoint(InetAddress address, int port) throws IOException {
        synchronized (this.endpoints) {
            ChannelFuture future = new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new LanChannelInitializer(server, networkManagers, this))
                    .group(LAN_EVENT_GROUP.getValue())
                    .localAddress(address, port)
                    .bind()
                    .syncUninterruptibly();

            this.endpoints.add(future);
        }
    }

    public SocketAddress addLocalEndpoint() {
        ChannelFuture future;

        synchronized (this.endpoints) {
            future = new ServerBootstrap()
                    .channel(LocalServerChannel.class)
                    .childHandler(new LocalChannelInitializer(server, networkManagers))
                    .group(LOCAL_EVENT_GROUP.getValue())
                    .localAddress(LocalAddress.ANY)
                    .bind()
                    .syncUninterruptibly();

            this.endpoints.add(future);
        }

        return future.channel().localAddress();
    }

    public void terminateEndpoints() {
        this.isAlive = false;

        for (ChannelFuture future : this.endpoints) {
            try {
                future.channel().close().sync();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted whilst closing channel {}", future.channel(), e);
            }
        }
    }

    public void networkTick() {
        synchronized (this.networkManagers) {
            this.networkManagers.removeIf(manager -> {
                if (manager.hasNoChannel()) {
                    return false;
                }

                if (!manager.isChannelOpen()) {
                    manager.checkDisconnected();
                    return true;
                }

                processManagerPackets(manager);
                return false;
            });
        }
    }

    private void processManagerPackets(NetworkManager manager) {
        try {
            manager.processReceivedPackets();
        } catch (Exception exception) {
            handlePacketProcessingException(manager, exception);
        }
    }

    private void handlePacketProcessingException(NetworkManager manager, Exception exception) {
        if (manager.isLocalChannel()) {
            CrashReport report = CrashReport.makeCrashReport(exception, "Ticking memory connection");
            CrashReportCategory category = report.makeCategory("Ticking connection");
            category.addCrashSectionCallable("Connection", manager::toString);
            throw new ReportedException(report);
        }

        LOGGER.warn("Failed to handle packet for {}", manager.getRemoteAddress(), exception);
        ChatComponentText component = new ChatComponentText("Internal server error");
        manager.sendPacket(new S40PacketDisconnect(component), _ -> manager.closeChannel(component));
        manager.disableAutoRead();
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    static class LanChannelInitializer extends ChannelInitializer<Channel> {

        private final MinecraftServer server;
        private final List<NetworkManager> networkManagers;
        private final NetworkSystem networkSystem;

        public LanChannelInitializer(MinecraftServer server, List<NetworkManager> networkManagers, NetworkSystem networkSystem) {
            this.server = server;
            this.networkManagers = networkManagers;
            this.networkSystem = networkSystem;
        }

        @Override
        protected void initChannel(Channel channel) {
            try {
                channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
            } catch (ChannelException e) {
                NetworkSystem.LOGGER.error("Failed to set TCP_NODELAY option", e);
            }

            channel.pipeline()
                    .addLast("timeout", new ReadTimeoutHandler(30))
                    .addLast("legacy_query", new PingResponseHandler(networkSystem))
                    .addLast("splitter", new MessageSplitter())
                    .addLast("decoder", new MessageDeserializer(PacketDirection.OUTGOING))
                    .addLast("prepender", new MessagePrepender())
                    .addLast("encoder", new MessageSerializer(PacketDirection.INCOMING));

            NetworkManager manager = new NetworkManager();
            this.networkManagers.add(manager);
            channel.pipeline().addLast("packet_handler", manager);
            manager.setNetHandler(new NetHandlerHandshakeTCP(this.server, manager));
        }
    }

    static class LocalChannelInitializer extends ChannelInitializer<Channel> {

        private final MinecraftServer server;
        private final List<NetworkManager> networkManagers;

        public LocalChannelInitializer(MinecraftServer server, List<NetworkManager> networkManagers) {
            this.server = server;
            this.networkManagers = networkManagers;
        }

        @Override
        protected void initChannel(Channel channel) {
            NetworkManager manager = new NetworkManager();
            manager.setNetHandler(new NetHandlerHandshakeMemory(this.server, manager));
            this.networkManagers.add(manager);
            channel.pipeline().addLast("packet_handler", manager);
        }
    }
}
