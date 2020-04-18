/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.network;

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector2f;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.service.HostedService;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rpc.RpcHostedService;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.server.EntityDataHostedService;
import com.simsilica.ethereal.EtherealHost;
import java.awt.Point;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.reflections.Reflections;
import toniarts.openkeeper.game.network.chat.ChatHostedService;
import toniarts.openkeeper.game.network.game.GameHostedService;
import toniarts.openkeeper.game.network.lobby.LobbyHostedService;
import toniarts.openkeeper.game.network.session.AccountHostedService;
import toniarts.openkeeper.game.network.streaming.StreamingHostedService;

/**
 *
 * @author ArchDemon
 */
public class NetworkServer {

    private static boolean initialized = false;
    private final String host;
    private final int port;
    private String name;

    private Server server = null;
    private long start;

    public NetworkServer(String name, int port) throws UnknownHostException {
        this.host = InetAddress.getLocalHost().getCanonicalHostName();
        this.name = name;
        this.port = port;
    }

    private static void initialize() throws InstantiationException, IllegalAccessException {
        if (!initialized) {
            initialized = true;

            Reflections reflections = new Reflections("toniarts.openkeeper");

            for (Class<?> c : reflections.getTypesAnnotatedWith(Transferable.class)) {
                Transferable a = c.getAnnotation(Transferable.class);
                Serializer.registerClass(c, a.value().newInstance());
            }

            // Needed for the game
            Serializer.registerClass(Vector2f.class, new FieldSerializer());
            Serializer.registerClass(Vector2.class, new FieldSerializer());
            Serializer.registerClass(Point.class, new FieldSerializer());
        }
    }

    public <T extends HostedService> T getService(Class<T> type) {
        return server.getServices().getService(type);
    }

    public void start() throws IOException, InstantiationException, IllegalAccessException {
        if (server == null) {
            server = Network.createServer(NetworkConstants.GAME_NAME, NetworkConstants.PROTOCOL_VERSION, port, port);
        }
        server.addChannel(port + 1); // Lobby
        server.addChannel(port + 2); // Chat
        server.addChannel(port + 3); // ES object data

        initialize();
        server.addConnectionListener(new ServerConnectionListener(this));

        server.getServices().addServices(new RpcHostedService(),
                new RmiHostedService(),
                new StreamingHostedService(),
                new AccountHostedService(name),
                new LobbyHostedService(),
                new ChatHostedService(),
                new GameHostedService()
        );

        // Add the SimEtheral host that will serve object sync updates to
        // the clients.
        EtherealHost ethereal = new EtherealHost(NetworkConstants.OBJECT_PROTOCOL,
                NetworkConstants.ZONE_GRID,
                NetworkConstants.ZONE_RADIUS);
        server.getServices().addService(ethereal);

        // The ES objects
        server.getServices().addService(new EntityDataHostedService(NetworkConstants.ES_CHANNEL, new DefaultEntityData(), false));

        server.start();

        start = System.nanoTime();
    }

    public void close() {
        if (server != null && server.isRunning()) {

            // Close the client connections gracefully
            for (HostedConnection conn : server.getConnections()) {
                conn.close("Server closing!");
            }

            server.close();

            // FIXME: Really, I'm sure this is not meant to be
            // https://hub.jmonkeyengine.org/t/solved-for-now-serializer-locked-error-what-does-it-mean-version-jme-3-1/33671
            Serializer.setReadOnly(false);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public Server getServer() {
        return server;
    }

    public long getGameTime() {
        return System.nanoTime() - start;
    }

}
