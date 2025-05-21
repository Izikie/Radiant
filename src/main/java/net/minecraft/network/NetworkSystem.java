package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.network.NetHandlerHandshakeMemory;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NetworkSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final LazyLoadBase<NioEventLoopGroup> EVENT_LOOPS = new LazyLoadBase<>() {
        protected NioEventLoopGroup load() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyLoadBase<EpollEventLoopGroup> SERVER_EPOLL_EVENTLOOP = new LazyLoadBase<>() {
        protected EpollEventLoopGroup load() {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyLoadBase<LocalEventLoopGroup> SERVER_LOCAL_EVENTLOOP = new LazyLoadBase<>() {
        protected LocalEventLoopGroup load() {
            return new LocalEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Server IO #%d").setDaemon(true).build());
        }
    };
    private final MinecraftServer mcServer;
    public volatile boolean isAlive;
    private final List<ChannelFuture> endpoints = Collections.synchronizedList(Lists.newArrayList());
    private final List<NetworkManager> networkManagers = Collections.synchronizedList(Lists.newArrayList());

    public NetworkSystem(MinecraftServer server) {
        this.mcServer = server;
        this.isAlive = true;
    }

    public void addLanEndpoint(InetAddress address, int port) throws IOException {
        synchronized (this.endpoints) {
            Class<? extends ServerSocketChannel> oclass;
            LazyLoadBase<? extends EventLoopGroup> lazyloadbase;

            if (Epoll.isAvailable() && this.mcServer.shouldUseNativeTransport()) {
                oclass = EpollServerSocketChannel.class;
                lazyloadbase = SERVER_EPOLL_EVENTLOOP;
                LOGGER.info("Using epoll channel type");
            } else {
                oclass = NioServerSocketChannel.class;
                lazyloadbase = EVENT_LOOPS;
                LOGGER.info("Using default channel type");
            }

            this.endpoints.add((new ServerBootstrap()).channel(oclass).childHandler(new ChannelInitializer<>() {
                protected void initChannel(Channel channel) throws Exception {
                    try {
                        channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                    } catch (ChannelException var3) {
                    }

                    channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("legacy_query", new PingResponseHandler(NetworkSystem.this)).addLast("splitter", new MessageDeserializer2()).addLast("decoder", new MessageDeserializer(PacketDirection.SERVERBOUND)).addLast("prepender", new MessageSerializer2()).addLast("encoder", new MessageSerializer(PacketDirection.CLIENTBOUND));
                    NetworkManager networkmanager = new NetworkManager(PacketDirection.SERVERBOUND);
                    NetworkSystem.this.networkManagers.add(networkmanager);
                    channel.pipeline().addLast("packet_handler", networkmanager);
                    networkmanager.setNetHandler(new NetHandlerHandshakeTCP(NetworkSystem.this.mcServer, networkmanager));
                }
            }).group(lazyloadbase.getValue()).localAddress(address, port).bind().syncUninterruptibly());
        }
    }

    public SocketAddress addLocalEndpoint() {
        ChannelFuture channelfuture;

        synchronized (this.endpoints) {
            channelfuture = (new ServerBootstrap()).channel(LocalServerChannel.class).childHandler(new ChannelInitializer<>() {
                protected void initChannel(Channel channel) throws Exception {
                    NetworkManager networkManager = new NetworkManager(PacketDirection.SERVERBOUND);
                    networkManager.setNetHandler(new NetHandlerHandshakeMemory(NetworkSystem.this.mcServer, networkManager));
                    NetworkSystem.this.networkManagers.add(networkManager);
                    channel.pipeline().addLast("packet_handler", networkManager);
                }
            }).group(EVENT_LOOPS.getValue()).localAddress(LocalAddress.ANY).bind().syncUninterruptibly();
            this.endpoints.add(channelfuture);
        }

        return channelfuture.channel().localAddress();
    }

    public void terminateEndpoints() {
        this.isAlive = false;

        for (ChannelFuture channelfuture : this.endpoints) {
            try {
                channelfuture.channel().close().sync();
            } catch (InterruptedException ignore) {
                LOGGER.error("Interrupted whilst closing channel");
            }
        }
    }

    public void networkTick() {
        synchronized (this.networkManagers) {
            Iterator<NetworkManager> iterator = this.networkManagers.iterator();

            while (iterator.hasNext()) {
                final NetworkManager networkmanager = iterator.next();

                if (!networkmanager.hasNoChannel()) {
                    if (!networkmanager.isChannelOpen()) {
                        iterator.remove();
                        networkmanager.checkDisconnected();
                    } else {
                        try {
                            networkmanager.processReceivedPackets();
                        } catch (Exception exception) {
                            if (networkmanager.isLocalChannel()) {
                                CrashReport report = CrashReport.makeCrashReport(exception, "Ticking memory connection");
                                CrashReportCategory category = report.makeCategory("Ticking connection");
                                category.addCrashSectionCallable("Connection", networkmanager::toString);
                                throw new ReportedException(report);
                            }

                            LOGGER.warn("Failed to handle packet for {}", networkmanager.getRemoteAddress(), exception);
                            final ChatComponentText component = new ChatComponentText("Internal server error");
                            networkmanager.sendPacket(new S40PacketDisconnect(component), p_operationComplete_1_ -> networkmanager.closeChannel(component));
                            networkmanager.disableAutoRead();
                        }
                    }
                }
            }
        }
    }

    public MinecraftServer getServer() {
        return this.mcServer;
    }
}
