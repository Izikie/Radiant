package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.INetHandlerLoginServer;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.login.server.S03PacketEnableCompression;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.network.CryptManager;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.util.ITickable;
import org.apache.commons.lang3.Validate;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class NetHandlerLoginServer implements INetHandlerLoginServer, ITickable {
    private static final AtomicInteger AUTHENTICATOR_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LoggerFactory.getLogger(NetHandlerLoginServer.class);
    private static final Random RANDOM = new Random();
    private final byte[] verifyToken = new byte[4];
    private final MinecraftServer server;
    public final NetworkManager networkManager;
    private LoginState currentLoginState = LoginState.HELLO;
    private int connectionTimer;
    private GameProfile loginGameProfile;
    private final String serverId = "";
    private SecretKey secretKey;
    private EntityPlayerMP player;

    public NetHandlerLoginServer(MinecraftServer serverIn, NetworkManager networkManagerIn) {
        this.server = serverIn;
        this.networkManager = networkManagerIn;
        RANDOM.nextBytes(this.verifyToken);
    }

    @Override
    public void update() {
        if (this.currentLoginState == LoginState.READY_TO_ACCEPT) {
            this.tryAcceptPlayer();
        } else if (this.currentLoginState == LoginState.DELAY_ACCEPT) {
            EntityPlayerMP entityplayermp = this.server.getConfigurationManager().getPlayerByUUID(this.loginGameProfile.getId());

            if (entityplayermp == null) {
                this.currentLoginState = LoginState.READY_TO_ACCEPT;
                this.server.getConfigurationManager().initializeConnectionToPlayer(this.networkManager, this.player);
                this.player = null;
            }
        }

        if (this.connectionTimer++ == 600) {
            this.closeConnection("Took too long to log in");
        }
    }

    public void closeConnection(String reason) {
        try {
            LOGGER.info("Disconnecting {}: {}", this.getConnectionInfo(), reason);
            ChatComponentText chatcomponenttext = new ChatComponentText(reason);
            this.networkManager.sendPacket(new S00PacketDisconnect(chatcomponenttext));
            this.networkManager.closeChannel(chatcomponenttext);
        } catch (Exception exception) {
            LOGGER.error("Error whilst disconnecting player", exception);
        }
    }

    public void tryAcceptPlayer() {
        if (!this.loginGameProfile.isComplete()) {
            this.loginGameProfile = this.getOfflineProfile(this.loginGameProfile);
        }

        String s = this.server.getConfigurationManager().allowUserToConnect(this.networkManager.getRemoteAddress(), this.loginGameProfile);

        if (s != null) {
            this.closeConnection(s);
        } else {
            this.currentLoginState = LoginState.ACCEPTED;

            if (this.server.getNetworkCompressionTreshold() >= 0 && !this.networkManager.isLocalChannel()) {
                this.networkManager.sendPacket(new S03PacketEnableCompression(this.server.getNetworkCompressionTreshold()), (ChannelFutureListener) p_operationComplete_1_ -> NetHandlerLoginServer.this.networkManager.setCompressionThreshold(NetHandlerLoginServer.this.server.getNetworkCompressionTreshold()));
            }

            this.networkManager.sendPacket(new S02PacketLoginSuccess(this.loginGameProfile));
            EntityPlayerMP entityplayermp = this.server.getConfigurationManager().getPlayerByUUID(this.loginGameProfile.getId());

            if (entityplayermp != null) {
                this.currentLoginState = LoginState.DELAY_ACCEPT;
                this.player = this.server.getConfigurationManager().createPlayerForUser(this.loginGameProfile);
            } else {
                this.server.getConfigurationManager().initializeConnectionToPlayer(this.networkManager, this.server.getConfigurationManager().createPlayerForUser(this.loginGameProfile));
            }
        }
    }

    @Override
    public void onDisconnect(IChatComponent reason) {
        LOGGER.info("{} lost connection: {}", this.getConnectionInfo(), reason.getUnformattedText());
    }

    public String getConnectionInfo() {
        return this.loginGameProfile != null ? this.loginGameProfile + " (" + this.networkManager.getRemoteAddress().toString() + ")" : String.valueOf(this.networkManager.getRemoteAddress());
    }

    @Override
    public void processLoginStart(C00PacketLoginStart packet) {
        Validate.validState(this.currentLoginState == LoginState.HELLO, "Unexpected hello packet");
        this.loginGameProfile = packet.getProfile();

        if (this.server.isServerInOnlineMode() && !this.networkManager.isLocalChannel()) {
            this.currentLoginState = LoginState.KEY;
            this.networkManager.sendPacket(new S01PacketEncryptionRequest(this.serverId, this.server.getKeyPair().getPublic(), this.verifyToken));
        } else {
            this.currentLoginState = LoginState.READY_TO_ACCEPT;
        }
    }

    @Override
    public void processEncryptionResponse(C01PacketEncryptionResponse packet) {
        Validate.validState(this.currentLoginState == LoginState.KEY, "Unexpected key packet");
        PrivateKey privatekey = this.server.getKeyPair().getPrivate();

        if (!Arrays.equals(this.verifyToken, packet.getVerifyToken(privatekey))) {
            throw new IllegalStateException("Invalid nonce!");
        } else {
            this.secretKey = packet.getSecretKey(privatekey);
            this.currentLoginState = LoginState.AUTHENTICATING;
            this.networkManager.enableEncryption(this.secretKey);
            (new Thread("User Authenticator #" + AUTHENTICATOR_THREAD_ID.incrementAndGet()) {
                @Override
                public void run() {
                    GameProfile gameprofile = loginGameProfile;

                    try {
                        String s = (new BigInteger(CryptManager.getServerIdHash(serverId, server.getKeyPair().getPublic(), secretKey))).toString(16);
                        loginGameProfile = server.getMinecraftSessionService().hasJoinedServer(new GameProfile(null, gameprofile.getName()), s, this.getAddress());

                        if (loginGameProfile != null) {
                            LOGGER.info("UUID of player {} is {}", loginGameProfile.getName(), loginGameProfile.getId());
                            currentLoginState = LoginState.READY_TO_ACCEPT;
                        } else if (server.isSinglePlayer()) {
                            LOGGER.warn("Failed to verify username but will let them in anyway!");
                            loginGameProfile = getOfflineProfile(gameprofile);
                            currentLoginState = LoginState.READY_TO_ACCEPT;
                        } else {
                            closeConnection("Failed to verify username!");
                            LOGGER.error("Username '{}' tried to join with an invalid session", loginGameProfile.getName());
                        }
                    } catch (AuthenticationUnavailableException exception) {
                        if (server.isSinglePlayer()) {
                            LOGGER.warn("Authentication servers are down but will let them in anyway!");
                            loginGameProfile = getOfflineProfile(gameprofile);
                            currentLoginState = LoginState.READY_TO_ACCEPT;
                        } else {
                            closeConnection("Authentication servers are down. Please try again later, sorry!");
                            NetHandlerLoginServer.LOGGER.error("Couldn't verify username because servers are unavailable");
                        }
                    }
                }

                private InetAddress getAddress() {
                    SocketAddress address = networkManager.getRemoteAddress();

                    if (address instanceof InetSocketAddress socket) {
                        return socket.getAddress();
                    }

                    return null;
                }

            }).start();
        }
    }

    protected GameProfile getOfflineProfile(GameProfile original) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + original.getName()).getBytes(StandardCharsets.UTF_8));
        return new GameProfile(uuid, original.getName());
    }

    enum LoginState {
        HELLO,
        KEY,
        AUTHENTICATING,
        READY_TO_ACCEPT,
        DELAY_ACCEPT,
        ACCEPTED
    }
}
