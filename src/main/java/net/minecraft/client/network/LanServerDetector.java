package net.minecraft.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ThreadLanServerPing;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LanServerDetector {
    private static final AtomicInteger field_148551_a = new AtomicInteger(0);
    private static final Logger LOGGER = LoggerFactory.getLogger(LanServerDetector.class);

    public static class LanServer {
        private final String lanServerMotd;
        private final String lanServerIpPort;
        private long timeLastSeen;

        public LanServer(String motd, String address) {
            this.lanServerMotd = motd;
            this.lanServerIpPort = address;
            this.timeLastSeen = Minecraft.getSystemTime();
        }

        public String getServerMotd() {
            return this.lanServerMotd;
        }

        public String getServerIpPort() {
            return this.lanServerIpPort;
        }

        public void updateLastSeen() {
            this.timeLastSeen = Minecraft.getSystemTime();
        }
    }

    public static class LanServerList {
        private final List<LanServer> listOfLanServers = new ArrayList<>();
        boolean wasUpdated;

        public synchronized boolean getWasUpdated() {
            return this.wasUpdated;
        }

        public synchronized void setWasNotUpdated() {
            this.wasUpdated = false;
        }

        public synchronized List<LanServer> getLanServers() {
            return Collections.unmodifiableList(this.listOfLanServers);
        }

        public synchronized void func_77551_a(String p_77551_1_, InetAddress p_77551_2_) {
            String s = ThreadLanServerPing.getMotdFromPingResponse(p_77551_1_);
            String s1 = ThreadLanServerPing.getAdFromPingResponse(p_77551_1_);

            if (s1 != null) {
                s1 = p_77551_2_.getHostAddress() + ":" + s1;
                boolean flag = false;

                for (LanServer lanserverdetector$lanserver : this.listOfLanServers) {
                    if (lanserverdetector$lanserver.getServerIpPort().equals(s1)) {
                        lanserverdetector$lanserver.updateLastSeen();
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    this.listOfLanServers.add(new LanServer(s, s1));
                    this.wasUpdated = true;
                }
            }
        }
    }

    public static class ThreadLanServerFind extends Thread {
        private final LanServerList localServerList;
        private final InetSocketAddress multicastGroupAddress;
        private final NetworkInterface networkInterface;
        private final MulticastSocket socket;

        public ThreadLanServerFind(LanServerList p_i1320_1_) throws IOException {
            super("LanServerDetector #" + LanServerDetector.field_148551_a.incrementAndGet());
            this.localServerList = p_i1320_1_;
            this.setDaemon(true);
            this.socket = new MulticastSocket(4445);
            this.multicastGroupAddress = new InetSocketAddress(InetAddress.getByName("224.0.2.60"), 4445);
            this.socket.setSoTimeout(5000);
            this.networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            this.socket.joinGroup(multicastGroupAddress, networkInterface);
        }

        @Override
        public void run() {
            byte[] abyte = new byte[1024];

            while (!this.isInterrupted()) {
                DatagramPacket datagrampacket = new DatagramPacket(abyte, abyte.length);

                try {
                    this.socket.receive(datagrampacket);
                } catch (SocketTimeoutException exception) {
                    continue;
                } catch (IOException exception) {
                    LanServerDetector.LOGGER.error("Couldn't ping server", exception);
                    break;
                }

                String s = new String(datagrampacket.getData(), datagrampacket.getOffset(), datagrampacket.getLength());
                LanServerDetector.LOGGER.debug("{}: {}", datagrampacket.getAddress(), s);
                this.localServerList.func_77551_a(s, datagrampacket.getAddress());
            }

            try {
                this.socket.leaveGroup(this.multicastGroupAddress, networkInterface);
            } catch (IOException exception) {
            }

            this.socket.close();
        }
    }
}
