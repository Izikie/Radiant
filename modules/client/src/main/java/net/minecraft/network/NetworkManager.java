package net.minecraft.network;

import com.google.common.collect.Queues;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
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
import net.minecraft.network.encoder.*;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketDirection;
import net.minecraft.util.ITickable;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.IChatComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetworkManager extends SimpleChannelInboundHandler<Packet<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkManager.class);
    public static final Marker LOG_MARKER_NETWORK = MarkerFactory.getMarker("NETWORK");
    public static final Marker LOG_MARKER_PACKETS = MarkerFactory.getMarker("NETWORK_PACKETS");
    public static final AttributeKey<NetworkState> ATTR_KEY_CONNECTION_STATE = AttributeKey.valueOf("protocol");

    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_NIO_EVENTLOOP = new LazyLoadBase<>() {
        @Override
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client NIO IO #%d", 0).factory(), NioIoHandler.newFactory());
        }
    };
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_EPOLL_EVENTLOOP = new LazyLoadBase<>() {
        @Override
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client Epoll IO #%d", 0).factory(), EpollIoHandler.newFactory());
        }
    };
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_KQUEUE_EVENTLOOP = new LazyLoadBase<>() {
        @Override
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client KQueue IO #%d", 0).factory(), KQueueIoHandler.newFactory());
        }
    };
    public static final LazyLoadBase<MultiThreadIoEventLoopGroup> CLIENT_LOCAL_EVENTLOOP = new LazyLoadBase<>() {
        @Override
        protected MultiThreadIoEventLoopGroup load() {
            return new MultiThreadIoEventLoopGroup(0, Thread.ofVirtual().name("Netty Client Local IO #%d", 0).factory(), LocalIoHandler.newFactory());
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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
        this.socketAddress = this.channel.remoteAddress();

        try {
            this.setConnectionState(NetworkState.HANDSHAKING);
        } catch (Throwable throwable) {
            LOGGER.error("Handshake failed during channel activation", throwable);
        }
    }

    public void setConnectionState(NetworkState newState) {
        this.channel.attr(ATTR_KEY_CONNECTION_STATE).set(newState);
        this.channel.config().setAutoRead(true);
        LOGGER.debug("Enabled auto read for state: {}", newState);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.closeChannel(new ChatComponentTranslation("disconnect.endOfStream"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        ChatComponentTranslation chatComponents = (throwable instanceof TimeoutException)
                ? new ChatComponentTranslation("disconnect.timeout")
                : new ChatComponentTranslation("disconnect.genericReason", "Internal Exception: " + throwable);

        LOGGER.error("Exception caught in network pipeline", throwable);
        this.closeChannel(chatComponents);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        if (this.channel.isOpen()) {
            try {
                packet.handle(this.packetListener);
            } catch (ThreadQuickExitException e) {
                LOGGER.error("ThreadQuickExitException in network pipeline", e);
            }
        }
    }

    public void setNetHandler(INetHandler handler) {
        this.packetListener = Objects.requireNonNull(handler, "packetListener cannot be null");
        LOGGER.debug("Set listener of {} to {}", this, handler);
    }

    public void sendPacket(Packet<?> packet) {
        this.sendPacket(packet, null);
    }

    @SafeVarargs
    public final void sendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>... listeners) {
        GenericFutureListener<? extends Future<? super Void>>[] combinedListeners =
                listener == null ? listeners : ArrayUtils.insert(0, listeners, listener);

        if (this.isChannelOpen()) {
            this.flushOutboundQueue();
            this.dispatchPacket(packet, combinedListeners);
        } else {
            this.readWriteLock.writeLock().lock();
            try {
                this.outboundPacketsQueue.add(new InboundHandlerTuplePacketListener(packet, combinedListeners));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    private void dispatchPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>>[] futureListeners) {
        NetworkState packetState = NetworkState.getFromPacket(packet);
        NetworkState currentState = this.channel.attr(ATTR_KEY_CONNECTION_STATE).get();

        if (currentState != packetState) {
            LOGGER.debug("Disabled auto read due to state mismatch (Current: {}, Packet: {})", currentState, packetState);
            this.channel.config().setAutoRead(false);
        }

        Runnable task = () -> {
            if (packetState != currentState) {
                this.setConnectionState(packetState);
            }

            // TODO: flush less often likly every tick rather than per packet.
            ChannelFuture future = this.channel.writeAndFlush(packet);
            if (futureListeners != null) {
                future.addListeners(futureListeners);
            }
            future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        };

        if (this.channel.eventLoop().inEventLoop()) {
            task.run();
        } else {
            this.channel.eventLoop().execute(task);
        }
    }

    private void flushOutboundQueue() {
        if (this.isChannelOpen()) {
            this.readWriteLock.readLock().lock();

            try {
                InboundHandlerTuplePacketListener listener;
                while ((listener = this.outboundPacketsQueue.poll()) != null) {
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

        if (this.channel != null) {
            this.channel.flush();
        }
    }

    public SocketAddress getRemoteAddress() {
        return this.socketAddress;
    }

    public void closeChannel(IChatComponent message) {
        if (this.isChannelOpen()) {
            this.channel.close().awaitUninterruptibly();
            this.terminationReason = message;
        }
    }

    public boolean isLocalChannel() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public static NetworkManager createNetworkManagerAndConnect(InetAddress address, int port, boolean useNativeTransport) {
        NetworkManager manager = new NetworkManager();
        Class<? extends SocketChannel> socketChannelClass;
        LazyLoadBase<? extends EventLoopGroup> eventLoopGroup;

        if (Epoll.isAvailable() && useNativeTransport) {
            socketChannelClass = EpollSocketChannel.class;
            eventLoopGroup = CLIENT_EPOLL_EVENTLOOP;
        } else if (KQueue.isAvailable() && useNativeTransport) {
            socketChannelClass = KQueueSocketChannel.class;
            eventLoopGroup = CLIENT_KQUEUE_EVENTLOOP;
        } else {
            socketChannelClass = NioSocketChannel.class;
            eventLoopGroup = CLIENT_NIO_EVENTLOOP;
        }

        new Bootstrap()
                .group(eventLoopGroup.getValue())
                .channel(socketChannelClass)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        try {
                            channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                        } catch (ChannelException e) {
                            LOGGER.error("Failed to set TCP/IP channel options", e);
                        }

                        channel.pipeline()
                                .addLast("timeout", new ReadTimeoutHandler(30))
                                .addLast("splitter", new MessageSplitter())
                                .addLast("decoder", new MessageDeserializer(PacketDirection.INCOMING))
                                .addLast("prepender", new MessagePrepender())
                                .addLast("encoder", new MessageSerializer(PacketDirection.OUTGOING))
                                .addLast("packet_handler", manager);
                    }
                })
                .connect(address, port)
                .syncUninterruptibly();

        return manager;
    }

    public static NetworkManager provideLocalClient(SocketAddress address) {
        NetworkManager manager = new NetworkManager();
        new Bootstrap()
                .group(CLIENT_LOCAL_EVENTLOOP.getValue())
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast("packet_handler", manager);
                    }
                })
                .connect(address)
                .syncUninterruptibly();
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
        if (this.channel != null) {
            this.channel.config().setAutoRead(false);
        }
    }

    public void setCompressionThreshold(int threshold) {
        if (this.channel == null) {
            return;
        }

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
                IChatComponent exitMessage = this.getExitMessage();

                if (exitMessage != null) {
                    this.getNetHandler().onDisconnect(exitMessage);
                } else if (this.getNetHandler() != null) {
                    this.getNetHandler().onDisconnect(new ChatComponentText("Disconnected"));
                }
            } else {
                LOGGER.warn("handleDisconnection() called twice on closed channel");
            }
        }
    }

    public static class InboundHandlerTuplePacketListener {
        private final Packet<?> packet;
        private final GenericFutureListener<? extends Future<? super Void>>[] futureListeners;

        @SafeVarargs
        public InboundHandlerTuplePacketListener(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>>... inFutureListeners) {
            this.packet = packet;
            this.futureListeners = inFutureListeners;
        }
    }

    static {
        LOG_MARKER_PACKETS.add(LOG_MARKER_NETWORK);
    }
}
