package net.minecraft.client.network;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkState;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Formatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class OldServerPinger {
    private static final Splitter PING_RESPONSE_SPLITTER = Splitter.on('\u0000').limit(6);
    private static final Logger LOGGER = LoggerFactory.getLogger(Object.class);
    private final List<NetworkManager> pingDestinations = Collections.synchronizedList(new ArrayList<>());

    public void ping(final ServerData server) throws UnknownHostException {
        ServerAddress address = ServerAddress.fromString(server.serverIP);
        final NetworkManager networkManager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(address.getIP()), address.getPort(), false);
        this.pingDestinations.add(networkManager);
        server.serverMOTD = "Pinging...";
        server.pingToServer = -1L;
        server.playerList = null;
        networkManager.setNetHandler(new INetHandlerStatusClient() {
            private boolean field_147403_d = false;
            private boolean field_183009_e = false;
            private long field_175092_e = 0L;

            public void handleServerInfo(S00PacketServerInfo packet) {
                if (this.field_183009_e) {
                    networkManager.closeChannel(new ChatComponentText("Received unrequested status"));
                } else {
                    this.field_183009_e = true;
                    ServerStatusResponse response = packet.getResponse();

                    if (response.getMOTD() != null) {
                        server.serverMOTD = response.getMOTD().getFormattedText();
                    } else {
                        server.serverMOTD = "";
                    }

                    if (response.getProtocolVersionInfo() != null) {
                        server.gameVersion = response.getProtocolVersionInfo().name();
                        server.version = response.getProtocolVersionInfo().protocol();
                    } else {
                        server.gameVersion = "Old";
                        server.version = 0;
                    }

                    if (response.getPlayerCountData() != null) {
                        server.populationInfo = Formatting.GRAY + "" + response.getPlayerCountData().getOnlinePlayers() + Formatting.DARK_GRAY + "/" + Formatting.GRAY + response.getPlayerCountData().getMaxPlayers();

                        if (ArrayUtils.isNotEmpty(response.getPlayerCountData().getPlayers())) {
                            StringBuilder builder = new StringBuilder();

                            for (GameProfile gameprofile : response.getPlayerCountData().getPlayers()) {
                                if (!builder.isEmpty()) {
                                    builder.append("\n");
                                }

                                builder.append(gameprofile.getName());
                            }

                            if (response.getPlayerCountData().getPlayers().length < response.getPlayerCountData().getOnlinePlayers()) {
                                if (!builder.isEmpty()) {
                                    builder.append("\n");
                                }

                                builder.append("... and ").append(response.getPlayerCountData().getOnlinePlayers() - response.getPlayerCountData().getPlayers().length).append(" more ...");
                            }

                            server.playerList = builder.toString();
                        }
                    } else {
                        server.populationInfo = Formatting.DARK_GRAY + "???";
                    }

                    if (response.getFavicon() != null) {
                        String icon = response.getFavicon();

                        if (icon.startsWith("data:image/png;base64,")) {
                            server.setBase64EncodedIconData(icon.substring("data:image/png;base64,".length()));
                        } else {
                            LOGGER.error("Invalid server icon (unknown format)");
                        }
                    } else {
                        server.setBase64EncodedIconData(null);
                    }

                    this.field_175092_e = Minecraft.getSystemTime();
                    networkManager.sendPacket(new C01PacketPing(this.field_175092_e));
                    this.field_147403_d = true;
                }
            }

            public void handlePong(S01PacketPong packet) {
                long i = this.field_175092_e;
                long j = Minecraft.getSystemTime();
                server.pingToServer = j - i;
                networkManager.closeChannel(new ChatComponentText("Finished"));
            }

            public void onDisconnect(IChatComponent reason) {
                if (!this.field_147403_d) {
                    OldServerPinger.LOGGER.error("Can't ping {}: {}", server.serverIP, reason.getUnformattedText());
                    server.serverMOTD = Formatting.DARK_RED + "Can't connect to server.";
                    server.populationInfo = "";
                    OldServerPinger.this.tryCompatibilityPing(server);
                }
            }
        });

        try {
            networkManager.sendPacket(new C00Handshake(47, address.getIP(), address.getPort(), NetworkState.STATUS));
            networkManager.sendPacket(new C00PacketServerQuery());
        } catch (Throwable throwable) {
            LOGGER.error("Handshake failed", throwable);
        }
    }

    private void tryCompatibilityPing(ServerData server) {
        ServerAddress address = ServerAddress.fromString(server.serverIP);
        (new Bootstrap()).group(NetworkManager.CLIENT_NIO_EVENTLOOP.getValue()).handler(new ChannelInitializer<>() {
            protected void initChannel(Channel channel) throws Exception {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                } catch (ChannelException exception) {
                }

                channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        super.channelActive(ctx);
                        ByteBuf byteBuf = Unpooled.buffer();

                        try {
                            byteBuf.writeByte(254);
                            byteBuf.writeByte(1);
                            byteBuf.writeByte(250);
                            char[] achar = "MC|PingHost".toCharArray();
                            byteBuf.writeShort(achar.length);

                            for (char c0 : achar) {
                                byteBuf.writeChar(c0);
                            }

                            byteBuf.writeShort(7 + 2 * address.getIP().length());
                            byteBuf.writeByte(127);
                            achar = address.getIP().toCharArray();
                            byteBuf.writeShort(achar.length);

                            for (char c1 : achar) {
                                byteBuf.writeChar(c1);
                            }

                            byteBuf.writeInt(address.getPort());
                            ctx.channel().writeAndFlush(byteBuf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                        } finally {
                            byteBuf.release();
                        }
                    }

                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf p_channelRead0_2_) {
                        short short1 = p_channelRead0_2_.readUnsignedByte();

                        if (short1 == 255) {
                            String s = new String(p_channelRead0_2_.readBytes(p_channelRead0_2_.readShort() * 2).array(), StandardCharsets.UTF_16BE);
                            String[] astring = Iterables.toArray(OldServerPinger.PING_RESPONSE_SPLITTER.split(s), String.class);

                            if ("\u00a71".equals(astring[0])) {
                                int i = MathHelper.parseIntWithDefault(astring[1], 0);
                                String s1 = astring[2];
                                String s2 = astring[3];
                                int j = MathHelper.parseIntWithDefault(astring[4], -1);
                                int k = MathHelper.parseIntWithDefault(astring[5], -1);
                                server.version = -1;
                                server.gameVersion = s1;
                                server.serverMOTD = s2;
                                server.populationInfo = Formatting.GRAY + "" + j + Formatting.DARK_GRAY + "/" + Formatting.GRAY + k;
                            }
                        }

                        ctx.close();
                    }

                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) throws Exception {
                        ctx.close();
                    }
                });
            }
        }).channel(NioSocketChannel.class).connect(address.getIP(), address.getPort());
    }

    public void pingPendingNetworks() {
        synchronized (this.pingDestinations) {
            Iterator<NetworkManager> iterator = this.pingDestinations.iterator();

            while (iterator.hasNext()) {
                NetworkManager networkmanager = iterator.next();

                if (networkmanager.isChannelOpen()) {
                    networkmanager.processReceivedPackets();
                } else {
                    iterator.remove();
                    networkmanager.checkDisconnected();
                }
            }
        }
    }

    public void clearPendingNetworks() {
        synchronized (this.pingDestinations) {
            Iterator<NetworkManager> iterator = this.pingDestinations.iterator();

            while (iterator.hasNext()) {
                NetworkManager networkmanager = iterator.next();

                if (networkmanager.isChannelOpen()) {
                    iterator.remove();
                    networkmanager.closeChannel(new ChatComponentText("Cancelled"));
                }
            }
        }
    }
}
