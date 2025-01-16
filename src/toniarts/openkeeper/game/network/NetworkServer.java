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
import com.jme3.network.serializing.serializers.EnumSerializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.service.HostedService;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rpc.RpcHostedService;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.server.EntityDataHostedService;
import com.simsilica.ethereal.EtherealHost;
import toniarts.openkeeper.utils.Point;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.ChickenGenerator;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureEfficiency;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.PlayerObjective;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Regeneration;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.Spellbook;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.component.ViewType;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.network.chat.ChatHostedService;
import toniarts.openkeeper.game.network.game.GameHostedService;
import toniarts.openkeeper.game.network.lobby.LobbyHostedService;
import toniarts.openkeeper.game.network.message.GameData;
import toniarts.openkeeper.game.network.message.GameLoadProgressData;
import toniarts.openkeeper.game.network.message.StreamedMessage;
import toniarts.openkeeper.game.network.session.AccountHostedService;
import toniarts.openkeeper.game.network.streaming.StreamingHostedService;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Tile;

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

    private static void initialize() {
        if (!initialized) {
            initialized = true;

            // Messages
            Serializer.registerClass(StreamedMessage.class, new FieldSerializer());
            Serializer.registerClass(GameLoadProgressData.class, new FieldSerializer());

            // Lobby
            Serializer.registerClass(ClientInfo.class, new FieldSerializer());
            Serializer.registerClass(Keeper.class, new FieldSerializer());

            // Needed for the game
            Serializer.registerClass(Vector2f.class, new FieldSerializer());
            Serializer.registerClass(Vector2.class, new FieldSerializer());
            Serializer.registerClass(Point.class, new FieldSerializer());
            Serializer.registerClass(Tile.BridgeTerrainType.class, new EnumSerializer());
            Serializer.registerClass(Thing.HeroParty.Objective.class, new EnumSerializer());
            Serializer.registerClass(ObjectiveType.class, new EnumSerializer());
            Serializer.registerClass(GameData.class, new FieldSerializer());
            //Serializer.registerClass(CreatureState.class, new EnumSerializer());
            Serializer.registerClass(AbstractRoomController.ObjectType.class, new EnumSerializer());
            Serializer.registerClass(ViewType.class, new EnumSerializer());
            Serializer.registerClass(TaskType.class, new EnumSerializer());
            Serializer.registerClass(ResearchableEntity.class, new FieldSerializer());
            Serializer.registerClass(PlayerSpell.class, new FieldSerializer());
            Serializer.registerClass(ResearchableType.class, new EnumSerializer());

            // Our entity components
            Serializer.registerClass(AttackTarget.class, new FieldSerializer());
            Serializer.registerClass(ChickenAi.class, new FieldSerializer());
            Serializer.registerClass(ChickenGenerator.class, new FieldSerializer());
            Serializer.registerClass(CreatureAi.class, new FieldSerializer());
            Serializer.registerClass(CreatureComponent.class, new FieldSerializer());
            Serializer.registerClass(CreatureEfficiency.class, new FieldSerializer());
            Serializer.registerClass(CreatureExperience.class, new FieldSerializer());
            Serializer.registerClass(CreatureFall.class, new FieldSerializer());
            Serializer.registerClass(CreatureHunger.class, new FieldSerializer());
            Serializer.registerClass(CreatureImprisoned.class, new FieldSerializer());
            Serializer.registerClass(CreatureMeleeAttack.class, new FieldSerializer());
            Serializer.registerClass(CreatureMood.class, new FieldSerializer());
            Serializer.registerClass(CreatureRecuperating.class, new FieldSerializer());
            Serializer.registerClass(CreatureSleep.class, new FieldSerializer());
            Serializer.registerClass(CreatureTortured.class, new FieldSerializer());
            Serializer.registerClass(CreatureViewState.class, new FieldSerializer());
            Serializer.registerClass(Damage.class, new FieldSerializer());
            Serializer.registerClass(Death.class, new FieldSerializer());
            Serializer.registerClass(Decay.class, new FieldSerializer());
            Serializer.registerClass(DoorComponent.class, new FieldSerializer());
            Serializer.registerClass(DoorViewState.class, new FieldSerializer());
            Serializer.registerClass(Fearless.class, new FieldSerializer());
            Serializer.registerClass(FollowTarget.class, new FieldSerializer());
            Serializer.registerClass(Food.class, new FieldSerializer());
            Serializer.registerClass(Gold.class, new FieldSerializer());
            Serializer.registerClass(HauledBy.class, new FieldSerializer());
            Serializer.registerClass(Health.class, new FieldSerializer());
            Serializer.registerClass(InHand.class, new FieldSerializer());
            Serializer.registerClass(Interaction.class, new FieldSerializer());
            Serializer.registerClass(Mana.class, new FieldSerializer());
            Serializer.registerClass(MapTile.class, new FieldSerializer());
            Serializer.registerClass(Mobile.class, new FieldSerializer());
            Serializer.registerClass(Navigation.class, new FieldSerializer());
            Serializer.registerClass(ObjectComponent.class, new FieldSerializer());
            Serializer.registerClass(ObjectViewState.class, new FieldSerializer());
            Serializer.registerClass(Objective.class, new FieldSerializer());
            Serializer.registerClass(Owner.class, new FieldSerializer());
            Serializer.registerClass(Party.class, new FieldSerializer());
            Serializer.registerClass(Placeable.class, new FieldSerializer());
            Serializer.registerClass(PlayerObjective.class, new FieldSerializer());
            Serializer.registerClass(PortalGem.class, new FieldSerializer());
            Serializer.registerClass(Position.class, new FieldSerializer());
            Serializer.registerClass(Regeneration.class, new FieldSerializer());
            Serializer.registerClass(RoomStorage.class, new FieldSerializer());
            Serializer.registerClass(Senses.class, new FieldSerializer());
            Serializer.registerClass(Slapped.class, new FieldSerializer());
            Serializer.registerClass(Spellbook.class, new FieldSerializer());
            Serializer.registerClass(TaskComponent.class, new FieldSerializer());
            Serializer.registerClass(Threat.class, new FieldSerializer());
            Serializer.registerClass(TrapComponent.class, new FieldSerializer());
            Serializer.registerClass(TrapViewState.class, new FieldSerializer());
            Serializer.registerClass(Trigger.class, new FieldSerializer());
            Serializer.registerClass(Unconscious.class, new FieldSerializer());
        }
    }

    public <T extends HostedService> T getService(Class<T> type) {
        return server.getServices().getService(type);
    }

    public void start() throws IOException {
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
