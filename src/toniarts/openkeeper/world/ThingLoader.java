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
package toniarts.openkeeper.world;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.game.trigger.door.DoorTriggerLogicController;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerLogicController;
import toniarts.openkeeper.game.trigger.party.PartyTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.creature.CreatureLoader;
import toniarts.openkeeper.world.creature.Party;
import toniarts.openkeeper.world.door.DoorControl;
import toniarts.openkeeper.world.door.DoorLoader;
import toniarts.openkeeper.world.listener.CreatureListener;
import toniarts.openkeeper.world.listener.ObjectListener;
import toniarts.openkeeper.world.object.GoldObjectControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.object.SpellBookObjectControl;
import toniarts.openkeeper.world.trap.TrapControl;
import toniarts.openkeeper.world.trap.TrapLoader;

/**
 * Loads things, all things
 *
 * @author ArchDemon
 */
@Deprecated
public class ThingLoader {
    
    private static final Logger logger = System.getLogger(ThingLoader.class.getName());

    private final WorldState worldState;
    private final CreatureLoader creatureLoader;
    private final ObjectLoader objectLoader;
    private final DoorLoader doorLoader;
    private final TrapLoader trapLoader;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final Node root;
    private final Node nodeCreatures;
    private final Node nodeObjects;
    private final Node nodeDoors;
    private final Node nodeTraps;
    private final int maxLooseGoldPerPile;

    /**
     * List of creatures in the world
     */
    private final Set<CreatureControl> creatures = new LinkedHashSet<>();

    /**
     * List of freeform objects in the world, not room property etc.<br>
     * TODO: should we have these based on location, or owner, owner since
     * pickuppable objects should be scanned only by the owner (a.k.a. whose
     * tile they are on)
     */
    private final Set<ObjectControl> objects = new LinkedHashSet<>();
    private final Map<Point, DoorControl> doors = new HashMap<>();
    private final Map<Point, TrapControl> traps = new HashMap<>();
    private Map<Short, List<CreatureListener>> creatureListeners;
    private final Map<Integer, Party> creatureParties = new HashMap<>();
    private List<ObjectListener> objectListeners;

    public ThingLoader(WorldState worldHandler, KwdFile kwdFile, AssetManager assetManager) {
        this.worldState = worldHandler;
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        maxLooseGoldPerPile = (int) worldHandler.getGameState().getLevelVariable(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY);
        creatureLoader = new CreatureLoader(kwdFile, worldState) {

            @Override
            public void onDie(CreatureControl creature) {

                // Remove the creature
                creatures.remove(creature);

                // Notify listeners
                if (creatureListeners != null && creatureListeners.containsKey(creature.getOwnerId())) {
                    for (CreatureListener listener : creatureListeners.get(creature.getOwnerId())) {
                        listener.onDie(creature);
                    }
                }
            }

            @Override
            public void onSpawn(CreatureControl creature) {

                // Notify listeners
                if (creatureListeners != null && creatureListeners.containsKey(creature.getOwnerId())) {
                    for (CreatureListener listener : creatureListeners.get(creature.getOwnerId())) {
                        listener.onSpawn(creature);
                    }
                }
            }

            @Override
            public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {

                // Notify listeners
                if (creatureListeners != null && creatureListeners.containsKey(creature.getOwnerId())) {
                    for (CreatureListener listener : creatureListeners.get(creature.getOwnerId())) {
                        listener.onStateChange(creature, newState, oldState);
                    }
                }
            }

        };
        objectLoader = new ObjectLoader(kwdFile, worldState);
        doorLoader = new DoorLoader(kwdFile, worldState);
        trapLoader = new TrapLoader(kwdFile, worldState);

        // Create the scene graph
        root = new Node("Things");
        nodeCreatures = new Node("Creatures");
        nodeObjects = new Node("Objects");
        nodeDoors = new Node("Doors");
        nodeTraps = new Node("Traps");
    }

    /**
     * Load all the initial things from the level KWD file
     *
     * @param creatureTriggerState the creature trigger state to assign the
     *                             created creatures to triggers
     * @param objectTriggerState
     * @param doorTriggerState
     * @param partyTriggerState
     * @return the things node
     */
    public Node loadAll(CreatureTriggerLogicController creatureTriggerState, ObjectTriggerLogicController objectTriggerState, DoorTriggerLogicController doorTriggerState, PartyTriggerLogicController partyTriggerState) {

        // Load the thing
        try {
            for (Thing.HeroParty obj : kwdFile.getThings(Thing.HeroParty.class)) {

                Thing.HeroParty partyThing = (Thing.HeroParty) obj;
                Party party = new Party(partyThing);
                if (partyThing.getTriggerId() != 0) {
//                        partyTriggerState.addParty(partyThing.getTriggerId(), party);
                }
                creatureParties.put(party.getId(), party);
            }
            for (Thing.GoodCreature creature : kwdFile.getThings(Thing.GoodCreature.class)) {
                CreatureControl creatureControl = spawnCreature(creature, null, null);

                // Also add to the creature trigger control
                if (creature.getTriggerId() != 0) {
                    //creatureTriggerState.setThing(creature.getTriggerId(), creatureControl);
                }
            }
            for (Thing.NeutralCreature creature : kwdFile.getThings(Thing.NeutralCreature.class)) {
                CreatureControl creatureControl = spawnCreature(creature, null, null);

                // Also add to the creature trigger control
                if (creature.getTriggerId() != 0) {
                    //creatureTriggerState.setThing(creature.getTriggerId(), creatureControl);
                }
            }
            for (Thing.KeeperCreature creature : kwdFile.getThings(Thing.KeeperCreature.class)) {
                CreatureControl creatureControl = spawnCreature(creature, null, null);

                // Also add to the creature trigger control
                if (creature.getTriggerId() != 0) {
                    //creatureTriggerState.setThing(creature.getTriggerId(), creatureControl);
                }
            }
            for (Thing.Object objectThing : kwdFile.getThings(Thing.Object.class)) {

                Spatial object = objectLoader.load(assetManager, objectThing);
                ObjectControl objectControl = object.getControl(ObjectControl.class);
                objects.add(objectControl);
                nodeObjects.attachChild(object);

                // Trigger
                if (objectThing.getTriggerId() != 0) {
                    //objectTriggerState.setThing(objectThing.getTriggerId(), objectControl);
                }

                notifyOnObjectAdded(objectControl);
            }
            for (Thing.Door doorThing : kwdFile.getThings(Thing.Door.class)) {

                Spatial door = doorLoader.load(assetManager, doorThing);
                DoorControl doorControl = door.getControl(DoorControl.class);
                doors.put(new Point(doorThing.getPosX(), doorThing.getPosY()), doorControl);
                nodeDoors.attachChild(door);

                // Trigger
                if (doorThing.getTriggerId() != 0) {
                    //doorTriggerState.setThing(doorThing.getTriggerId(), null);
                }
            }
            for (Thing.Trap trapThing : kwdFile.getThings(Thing.Trap.class)) {

                Spatial trap = trapLoader.load(assetManager, trapThing);
                TrapControl trapControl = trap.getControl(TrapControl.class);
                traps.put(new Point(trapThing.getPosX(), trapThing.getPosY()), trapControl);
                nodeTraps.attachChild(trap);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not load Thing.", ex);
        }

        root.attachChild(nodeCreatures);
        root.attachChild(nodeObjects);
        root.attachChild(nodeDoors);
        root.attachChild(nodeTraps);
        return root;
    }

    /**
     * Spawn a creature
     *
     * @param cr       creature data
     * @param position the position to spawn to, may be {@code null}
     * @param app      if the app is set, the creature is attached in the next
     *                 render
     *                 loop, may be {@code null}. <strong>You need this parameter if calling
     *                 from outside the render loop!</strong>
     * @return the actual spawned creature
     */
    public CreatureControl spawnCreature(Thing.Creature cr, Vector2f position, Application app) {
        Spatial creature = creatureLoader.load(assetManager, cr);
        return spawnCreature(creature, position, false, app);
    }

    /**
     * Spawn a creature
     *
     * @param creatureId the creature ID to generate
     * @param playerId   the owner
     * @param level      the creature level
     * @param position   the position to spawn to, may be {@code null}
     * @param entrance   whether this an enrance for the creature (coming out of
     *                   a
     *                   portal)
     * @param app        if the app is set, the creature is attached in the next
     *                   render
     *                   loop, may be {@code null}. <strong>You need this parameter if calling
     *                   from outside the render loop!</strong>
     * @return the actual spawned creature
     */
    public CreatureControl spawnCreature(short creatureId, short playerId, short level, Vector2f position, boolean entrance, Application app) {
        Spatial creature = creatureLoader.load(assetManager, creatureId, playerId, level);
        return spawnCreature(creature, position, entrance, app);
    }

    private CreatureControl spawnCreature(Spatial creature, Vector2f position, boolean entrance, Application app) {
        if (position != null) {
            CreatureLoader.setPosition(creature, position);
        }
        CreatureControl creatureControl = creature.getControl(CreatureControl.class);
        if (entrance) {
            creatureControl.getStateMachine().changeState(CreatureState.ENTERING_DUNGEON);
        }
        creatures.add(creatureControl);

        // Enqueue if app is set
        if (app != null) {

            app.enqueue(() -> {

                // Spawn the creature
                attachCreature(creature);

                return null;
            });
        } else {
            attachCreature(creature);
        }

        // Notify spawn
        creatureControl.onSpawn(creatureControl);

        return creatureControl;
    }

    /**
     * Attachs a creature back to the creatures node
     *
     * @param creature the creature spatial
     */
    public void attachCreature(Spatial creature) {
        nodeCreatures.attachChild(creature);
    }

    /**
     * Add room type gold, does not add the object to the object registry
     *
     * @param p             the point to add
     * @param playerId      the player id, the owner
     * @param initialAmount the amount of gold
     * @param maxAmount     the max gold amount
     * @return the gold object
     */
    @Nullable
    public GoldObjectControl addRoomGold(Point p, short playerId, int initialAmount, int maxAmount) {
        if (initialAmount == 0) {
            return null;
        }
        // TODO: the room gold object id..
        Spatial object = objectLoader.load(assetManager, p, 0, initialAmount, 0,
                ObjectLoader.OBJECT_GOLD_PILE_ID, playerId, maxAmount);
        GoldObjectControl control = object.getControl(GoldObjectControl.class);
        nodeObjects.attachChild(object);
        return control;
    }

    /**
     * Add loose type gold
     *
     * @param coordinates   coordinated inside the tile
     * @param playerId      the player id, the owner
     * @param initialAmount the amount of gold
     * @return the gold object
     */
    public GoldObjectControl addLooseGold(Vector2f coordinates, short playerId, int initialAmount) {
        // TODO: the gold object id..
        Spatial object = objectLoader.load(assetManager, coordinates,
                0, initialAmount, 0, ObjectLoader.OBJECT_GOLD_ID, playerId, maxLooseGoldPerPile);
        GoldObjectControl control = object.getControl(GoldObjectControl.class);
        objects.add(control);
        nodeObjects.attachChild(object);
        notifyOnObjectAdded(control);

        return control;
    }

    public GoldObjectControl addLooseGold(Point p, short playerId, int initialAmount) {
        Vector2f coordinates = WorldUtils.pointToVector2f(p);

        return addLooseGold(coordinates, playerId, initialAmount);
    }

    /**
     * Add an object, does not add the object to the object registry
     *
     * @param p        the point to add
     * @param objectId the object id
     * @param playerId the player id, the owner
     * @return the object contol
     */
    public ObjectControl addObject(Point p, short objectId, short playerId) {
        Spatial object = objectLoader.load(assetManager, p, 0, 0, 0, objectId, playerId, 0);
        ObjectControl control = object.getControl(ObjectControl.class);
        nodeObjects.attachChild(object);
        return control;
    }

    /**
     * Add an object, does not add the object to the object registry
     *
     * @param p        the point to add
     * @param spell    the spell
     * @param playerId the player id, the owner
     * @return the object contol
     */
    public SpellBookObjectControl addRoomSpellBook(Point p, PlayerSpell spell, short playerId) {
        // FIXME: The object ID
        Spatial object = objectLoader.load(assetManager, p, spell, 0, 0,
                ObjectLoader.OBJECT_SPELL_BOOK_ID, playerId, 0);
        object.move(0, MapLoader.FLOOR_HEIGHT, 0);
        SpellBookObjectControl control = object.getControl(SpellBookObjectControl.class);
        nodeObjects.attachChild(object);
        return control;
    }

    public void onObjectRemoved(ObjectControl object) {
        objects.remove(object);
        if (objectListeners != null) {
            for (ObjectListener listener : objectListeners) {
                listener.onRemoved(object);
            }
        }
    }

    public List<CreatureControl> getCreatures() {
        return new ArrayList<>(creatures);
    }

    public List<ObjectControl> getObjects() {
        return new ArrayList<>(objects);
    }

    /**
     * If you want to get notified about the creature changes
     *
     * @param playerId the player id of which creatures you want to assign the
     *                 listener to
     * @param listener the listener
     */
    public void addListener(short playerId, CreatureListener listener) {
        if (creatureListeners == null) {
            creatureListeners = new HashMap<>();
        }
        List<CreatureListener> listeners = creatureListeners.get(playerId);
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
        creatureListeners.put(playerId, listeners);
    }

    /**
     * If you want to get notified about the object changes
     *
     * listener to
     *
     * @param listener the listener
     */
    public void addListener(ObjectListener listener) {
        if (objectListeners == null) {
            objectListeners = new ArrayList<>();
        }
        objectListeners.add(listener);
    }

    /**
     * Typically you should add objects through add object so that they are
     * added to the global list, but for rooms etc. you can use the object
     * loader directly
     *
     * @return the object loader
     */
    protected ObjectLoader getObjectLoader() {
        return objectLoader;
    }

    private void notifyOnObjectAdded(ObjectControl object) {
        if (objectListeners != null) {
            for (ObjectListener listener : objectListeners) {
                listener.onAdded(object);
            }
        }
    }

    /**
     * Get a door/barricade on given point
     *
     * @param point point
     * @return door or {@code null} if not found
     */
    public DoorControl getDoor(Point point) {
        return doors.get(point);
    }

    /**
     * Get party instance by ID
     *
     * @param id the party id
     * @return party
     */
    public Party getParty(int id) {
        return creatureParties.get(id);
    }

}
