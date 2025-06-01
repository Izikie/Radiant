package net.minecraft.network;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetworkManager extends SimpleChannelInboundHandler<Packet<?>> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Marker LOG_MARKER_NETWORK = MarkerManager.getMarker("NETWORK");
    public static final Marker LOG_MARKER_PACKETS = MarkerManager.getMarker("NETWORK_PACKETS").addParents(LOG_MARKER_NETWORK);
    public static final AttributeKey<NetworkState> ATTR_KEY_CONNECTION_STATE = AttributeKey.valueOf("protocol");
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_NIO_EVENTLOOP = new LazyLoadBase<>() {
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client IO #%d", 0).factory(), NioIoHandler.newFactory());
        }
    };
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_EPOLL_EVENTLOOP = new LazyLoadBase<>() {
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client IO #%d", 0).factory(), EpollIoHandler.newFactory());
        }
    };
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_LOCAL_EVENTLOOP = new LazyLoadBase<>() {
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client IO #%d", 0).factory(), LocalIoHandler.newFactory());
        }
    };
    private final Queue<InboundHandlerTuplePacketListener> outboundPacketsQueue = Queues.newConcurrentLinkedQueue();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Channel channel;
    private SocketAddress socketAddress;
    private INetHandler packetListener;
    private IChatComponent terminationReason;
    private boolean isEncrypted;
    private boolean disconnected;

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
        this.socketAddress = this.channel.remoteAddress();

        try {
            this.setConnectionState(NetworkState.HANDSHAKING);
        } catch (Throwable throwable) {
            LOGGER.fatal(throwable);
        }
    }

    public void setConnectionState(NetworkState newState) {
        this.channel.attr(ATTR_KEY_CONNECTION_STATE).set(newState);
        this.channel.config().setAutoRead(true);
        LOGGER.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        this.closeChannel(new ChatComponentTranslation("disconnect.endOfStream"));
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        ChatComponentTranslation chatComponents;

        if (throwable instanceof TimeoutException) {
            chatComponents = new ChatComponentTranslation("disconnect.timeout");
        } else {
            chatComponents = new ChatComponentTranslation("disconnect.genericReason", "Internal Exception: " + throwable);
        }

        this.closeChannel(chatComponents);
    }

    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        if (this.channel.isOpen()) {
            try {
                packet.processPacket(this.packetListener);
            } catch (ThreadQuickExitException exception) {
            }
        }
    }

    public void setNetHandler(INetHandler handler) {
        Objects.requireNonNull(handler, "packetListener");
        LOGGER.debug("Set listener of {} to {}", new Object[]{this, handler});
        this.packetListener = handler;
    }

    public void sendPacket(Packet<?> packetIn) {
        if (this.isChannelOpen()) {
            this.flushOutboundQueue();
            this.dispatchPacket(packetIn, null);
        } else {
            this.readWriteLock.writeLock().lock();

            try {
                this.outboundPacketsQueue.add(new InboundHandlerTuplePacketListener(packetIn, (GenericFutureListener<? extends Future<? super Void>>) null));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    @SafeVarargs
    public final void sendPacket(Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>... listeners) {
        if (this.isChannelOpen()) {
            this.flushOutboundQueue();
            this.dispatchPacket(packetIn, ArrayUtils.insert(0, listeners, listener));
        } else {
            this.readWriteLock.writeLock().lock();

            try {
                this.outboundPacketsQueue.add(new InboundHandlerTuplePacketListener(packetIn, ArrayUtils.insert(0, listeners, listener)));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    private void dispatchPacket(Packet<?> inPacket, GenericFutureListener<? extends Future<? super Void>>[] futureListeners) {
        NetworkState packetState = NetworkState.getFromPacket(inPacket);
        NetworkState currentState = this.channel.attr(ATTR_KEY_CONNECTION_STATE).get();

        if (currentState != packetState) {
            LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            if (packetState != currentState) {
                this.setConnectionState(packetState);
            }

            ChannelFuture future = this.channel.writeAndFlush(inPacket);

            if (futureListeners != null) {
                future.addListeners(futureListeners);
            }

            future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            this.channel.eventLoop().execute(() -> {
                if (packetState != currentState) {
                    this.setConnectionState(packetState);
                }

                ChannelFuture future = this.channel.writeAndFlush(inPacket);

                if (futureListeners != null) {
                    future.addListeners(futureListeners);
                }

                future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        }
    }

    private void flushOutboundQueue() {
        if (this.channel != null && this.channel.isOpen()) {
            this.readWriteLock.readLock().lock();

            try {
                while (!this.outboundPacketsQueue.isEmpty()) {
                    InboundHandlerTuplePacketListener listener = this.outboundPacketsQueue.poll();
                    this.dispatchPacket(listener.packet, listener.futureListeners);
                }
            } finally {
                this.readWriteLock.readLock().unlock();
            }
        }
    }

    public void processReceivedPackets() {
        this.flushOutboundQueue();

        if (this.packetListener instanceof ITickable tickable) {
            tickable.update();
        }

        this.channel.flush();
    }

    public SocketAddress getRemoteAddress() {
        return this.socketAddress;
    }

    public void closeChannel(IChatComponent message) {
        if (this.channel.isOpen()) {
            this.channel.close().awaitUninterruptibly();
            this.terminationReason = message;
        }
    }

    public boolean isLocalChannel() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public static NetworkManager createNetworkManagerAndConnect(InetAddress address, int port, boolean useNativeTransport) {
        NetworkManager manager = new NetworkManager();
        Class<? extends SocketChannel> oclass;
        LazyLoadBase<? extends EventLoopGroup> lazyloadbase;

        if (Epoll.isAvailable() && useNativeTransport) {
            oclass = EpollSocketChannel.class;
            lazyloadbase = CLIENT_EPOLL_EVENTLOOP;
        } else {
            oclass = NioSocketChannel.class;
            lazyloadbase = CLIENT_NIO_EVENTLOOP;
        }

        (new Bootstrap()).group(lazyloadbase.getValue()).handler(new ChannelInitializer<>() {
            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                } catch (ChannelException exception) {
                }

                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new MessageDeserializer2()).addLast("decoder", new MessageDeserializer(PacketDirection.CLIENTBOUND)).addLast("prepender", new MessageSerializer2()).addLast("encoder", new MessageSerializer(PacketDirection.SERVERBOUND)).addLast("packet_handler", manager);
            }
        }).channel(oclass).connect(address, port).syncUninterruptibly();
        return manager;
    }

    public static NetworkManager provideLocalClient(SocketAddress address) {
        NetworkManager manager = new NetworkManager();
        (new Bootstrap()).group(CLIENT_LOCAL_EVENTLOOP.getValue()).handler(new ChannelInitializer<>() {
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast("packet_handler", manager);
            }
        }).channel(LocalChannel.class).connect(address).syncUninterruptibly();
        return manager;
    }

    public void enableEncryption(SecretKey key) {
        this.isEncrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new NettyEncryptingDecoder(CryptManager.createNetCipherInstance(2, key)));
        this.channel.pipeline().addBefore("prepender", "encrypt", new NettyEncryptingEncoder(CryptManager.createNetCipherInstance(1, key)));
    }

    public boolean getIsEncrypted() {
        return this.isEncrypted;
    }

    public boolean isChannelOpen() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean hasNoChannel() {
        return this.channel == null;
    }

    public INetHandler getNetHandler() {
        return this.packetListener;
    }

    public IChatComponent getExitMessage() {
        return this.terminationReason;
    }

    public void disableAutoRead() {
        this.channel.config().setAutoRead(false);
    }

    public void setCompressionThreshold(int threshold) {
        if (threshold >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof NettyCompressionDecoder decoder) {
                decoder.setCompressionThreshold(threshold);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new NettyCompressionDecoder(threshold));
            }

            if (this.channel.pipeline().get("compress") instanceof NettyCompressionEncoder encoder) {
                encoder.setCompressionThreshold(threshold);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new NettyCompressionEncoder(threshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof NettyCompressionDecoder) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof NettyCompressionEncoder) {
                this.channel.pipeline().remove("compress");
            }
        }
    }

    public void checkDisconnected() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (!this.disconnected) {
                this.disconnected = true;

                if (this.getExitMessage() != null) {
                    this.getNetHandler().onDisconnect(this.getExitMessage());
                } else if (this.getNetHandler() != null) {
                    this.getNetHandler().onDisconnect(new ChatComponentText("Disconnected"));
                }
            } else {
                LOGGER.warn("handleDisconnection() called twice");
            }
        }
    }

    static class InboundHandlerTuplePacketListener {
        private final Packet<?> packet;
        private final GenericFutureListener<? extends Future<? super Void>>[] futureListeners;

        @SafeVarargs
        public InboundHandlerTuplePacketListener(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>>... inFutureListeners) {
            this.packet = packet;
            this.futureListeners = inFutureListeners;
        }
    }
}
