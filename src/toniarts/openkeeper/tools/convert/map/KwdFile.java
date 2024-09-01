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
package toniarts.openkeeper.tools.convert.map;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.tools.convert.map.Creature.Attraction;
import toniarts.openkeeper.tools.convert.map.Creature.JobAlternative;
import toniarts.openkeeper.tools.convert.map.Creature.OffsetType;
import toniarts.openkeeper.tools.convert.map.Creature.Spell;
import toniarts.openkeeper.tools.convert.map.Creature.X1323;
import toniarts.openkeeper.tools.convert.map.Door.DoorFlag;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevFlag;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevelReward;
import toniarts.openkeeper.tools.convert.map.GameLevel.TextTable;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.MAP;
import toniarts.openkeeper.tools.convert.map.Thing.ActionPoint;
import toniarts.openkeeper.tools.convert.map.Thing.ActionPoint.ActionPointFlag;
import toniarts.openkeeper.tools.convert.map.Thing.GoodCreature;
import toniarts.openkeeper.tools.convert.map.Thing.HeroParty;
import toniarts.openkeeper.tools.convert.map.Thing.KeeperCreature;
import toniarts.openkeeper.tools.convert.map.Thing.NeutralCreature;
import toniarts.openkeeper.tools.convert.map.Variable.Availability;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureFirstPerson;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureStats;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureStats.StatType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.PlayerAlliance;
import toniarts.openkeeper.tools.convert.map.Variable.Sacrifice;
import toniarts.openkeeper.utils.Color;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Reads a DK II map file, the KWD is the file name of the main map identifier,
 * reads the KLDs actually<br>
 * The files are LITTLE ENDIAN I might say<br>
 * Some values are 3D coordinates or scale values presented in fixed point
 * integers. They are automatically converted to floats (divided by 2^12 = 4096
 * or 2^16 = 65536)<br>
 * Many parts adapted from C code by:
 * <li>George Gensure (werkt)</li>
 * And another C code implementation by:
 * <li>Thomasz Lis</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class KwdFile {
    
    private static final Logger logger = System.getLogger(KwdFile.class.getName());

    // These are needed in various places, I don't know how to else recognize these
    private final static short ROOM_PORTAL_ID = 3;
    private final static short ROOM_DUNGEON_HEART_ID = 5;
    private final static short TRIGGER_GENERIC = 213;
    private final static short TRIGGER_ACTION = 214;

    private final static short THING_OBJECT = 194;
    private final static short THING_TRAP = 195;
    private final static short THING_DOOR = 196;
    private final static short THING_ACTION_POINT = 197;
    private final static short THING_NEUTRAL_CREATURE = 198;
    private final static short THING_GOOD_CREATURE = 199;
    private final static short THING_KEEPER_CREATURE = 200;
    private final static short THING_HERO_PARTY = 201;
    private final static short THING_DEAD_BODY = 202;
    private final static short THING_EFFECT_GENERATOR = 203;
    private final static short THING_ROOM = 204;
    private final static short THING_CAMERA = 205;

    private final static short HEADER_SIZE = 25603;
    private final static long CREATURE_SIZE = 5536L;

    private GameLevel gameLevel;
    private GameMap map;
    private Map<Short, Player> players;
    private Map<Short, Terrain> terrainTiles;
    private Map<Short, Door> doors;
    private Map<Short, Trap> traps;
    private Map<Short, Room> rooms;
    private Map<Short, Room> roomsByTerrainId; // Maps have rooms by the terrain ID
    private Map<Short, Creature> creatures;
    private Map<Short, GameObject> objects;
    private Map<Short, CreatureSpell> creatureSpells;
    private Map<Integer, EffectElement> effectElements;
    private Map<Integer, Effect> effects;
    private Map<Short, KeeperSpell> keeperSpells;
    private Map<Class<? extends Thing>, List<? extends Thing>> thingsByType;
    private Map<Short, Shot> shots;
    private Map<Integer, Trigger> triggers;
    // Variables
    private List<Availability> availabilities;
    private Map<Integer, Map<Integer, CreaturePool>> creaturePools;
    private Map<Integer, Map<StatType, CreatureStats>> creatureStatistics;
    private Map<Integer, Map<StatType, CreatureFirstPerson>> creatureFirstPersonStatistics;
    private Map<MiscVariable.MiscType, MiscVariable> variables;
    private Set<Sacrifice> sacrifices;
    private Set<PlayerAlliance> playerAlliances;
    private Set<Variable.Unknown> unknownVariables;
    //
    private boolean loaded = false;
    private Creature imp;
    private Creature dwarf;
    private final String basePath;
    private GameObject levelGem;

    private final Object loadingLock = new Object();

    /**
     * Constructs a new KWD file reader<br>
     * Reads the whole map and its catalogs (either standard ones or custom
     * ones)
     *
     * @param basePath path to DK II main path (or where ever is the "root")
     * @param file the KWD file to read
     */
    public KwdFile(String basePath, Path file) {
        this(basePath, file, true);
    }

    /**
     * Constructs a new KWD file reader<br>
     *
     * @param basePath path to DK II main path (or where ever is the "root")
     * @param file the KWD file to read
     * @param load whether to actually load the map data, or just get the
     * general info
     */
    public KwdFile(String basePath, Path file, boolean load) {

        // Load the actual main map info (paths to catalogs most importantly)
        // Read the file
        try {
            readFileContents(file);
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
        this.basePath = PathUtils.fixFilePath(basePath);

        // See if we need to load the actual data
        if (load) {
            load();
        } else {

            // We need map width & height if not loaded fully, I couldn't figure out where, except the map data
            try (ISeekableResourceReader data = new FileResourceReader(PathUtils.getRealFileName(basePath, gameLevel.getFile(MAP)))) {
                KwdHeader header = readKwdHeader(data);
                map = new GameMap(header.getWidth(), header.getHeight());
            } catch (Exception e) {

                //Fug
                throw new RuntimeException("Failed to read the file " + gameLevel.getFile(MAP) + "!", e);
            }
        }
    }

    private void readFileContents(Path file) throws IOException {
        try (ISeekableResourceReader data = new FileResourceReader(file)) {
            while (data.getFilePointer() < data.length()) {

                // Read header (and put the file pointer to the data start)
                KwdHeader header = readKwdHeader(data);
                readFileContents(header, data);
            }

            if (data.getFilePointer() != data.length()) {
                throw new RuntimeException("Failed to parse file");
            }
        }
    }

    /**
     * Loads the map data
     *
     * @throws RuntimeException level file fails to parse
     */
    public void load() throws RuntimeException {
        if (!loaded) {
            synchronized (loadingLock) {
                if (!loaded) {

                    // Read the map data first (we store some data to the map)
                    for (FilePath path : gameLevel.getPaths()) {
                        if (path.getId() == MapDataTypeEnum.MAP) {
                            readFilePath(path);
                            break;
                        }
                    }

                    // Now we have the paths, read all of those in order
                    for (FilePath path : gameLevel.getPaths()) {
                        if (path.getId() != MapDataTypeEnum.MAP) {
                            // Open the file
                            readFilePath(path);
                        }
                    }
                    loaded = true;
                }
            }
        }
    }

    private void readFilePath(FilePath path) {
        Path file = null;
        try {
            file = Paths.get(PathUtils.getRealFileName(basePath, path.getPath()));
            readFileContents(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find the map file " + file + "!", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
    }

    /**
     * Reads the common KWD header
     *
     * @param data the data file
     * @return the header
     * @throws IOException may fail reading
     */
    private KwdHeader readKwdHeader(ISeekableResourceReader data) throws IOException {
        long startOffset = data.getFilePointer();
        int headerSize = 28;

        KwdHeader header = new KwdHeader();
        IResourceChunkReader reader = data.readChunk(8);
        header.setId(reader.readIntegerAsEnum(MapDataTypeEnum.class));
        int size = reader.readUnsignedInteger(); // Bytes in the real size indicator, well seems to be 4 always
        reader = data.readChunk(size);
        int dataSize = 0;
        if (size == 2) {
            dataSize = reader.readUnsignedShort();
            headerSize -= 2;
        } else if (size == 4) {
            dataSize = reader.readUnsignedInteger();
        }
        header.setSize(dataSize);
        reader = data.readChunk(8);
        header.setCheckOne(reader.readUnsignedInteger());
        header.setHeaderEndOffset(reader.readUnsignedInteger());

        // Read the actual header
        int headerPayloadSize = (int) (header.getHeaderEndOffset() - (data.getFilePointer() - startOffset - headerSize));
        reader = data.readChunk(headerPayloadSize);

        header.setHeaderSize(headerSize + headerPayloadSize - 8); // Minus the check & data size
        switch (header.getId()) {
            case MAP:
                header.setWidth(reader.readUnsignedInteger());
                header.setHeight(reader.readUnsignedInteger());
                break;

            case TRIGGERS:
                header.setItemCount(reader.readUnsignedInteger() + reader.readUnsignedInteger());
                header.setUnknown(reader.readUnsignedInteger());

                header.setDateCreated(reader.readTimestamp());
                header.setDateModified(reader.readTimestamp());
                break;

            case LEVEL:
                header.setItemCount(reader.readUnsignedShort());
                header.setHeight(reader.readUnsignedShort());
                header.setUnknown(reader.readUnsignedInteger());

                header.setDateCreated(reader.readTimestamp());
                header.setDateModified(reader.readTimestamp());
                break;
            default:
                header.setItemCount(reader.readUnsignedInteger());
                header.setUnknown(reader.readUnsignedInteger());

                header.setDateCreated(reader.readTimestamp());
                header.setDateModified(reader.readTimestamp());
                break;
        }

        if (data.getFilePointer() != startOffset + header.getHeaderSize()) {
            logger.log(Level.WARNING, "Incorrect parsing of file header");
        }

        // Not part of the header, part of the data really
        header.setCheckTwo(reader.readUnsignedInteger());
        header.setDataSize(reader.readUnsignedInteger());

        return header;
    }

    private void readFileContents(KwdHeader header, IResourceReader data) throws IOException {

        // Handle all the cases (we kinda skip the globals with this logic, so no need)
        // All readers must read the whole data they intend to read
        switch (header.getId()) {
            case LEVEL:
                // check header.getCheckOne() != 221 || header.getCheckTwo() != 223
                readMapInfo(header, data);
                break;

            case CREATURES:
                // check header.getCheckOne() != 171 || header.getCheckTwo() != 172
                readCreatures(header, data);
                break;

            case CREATURE_SPELLS:
                if (header.getCheckOne() != 161 || header.getCheckTwo() != 162) {
                    throw new RuntimeException("Creature spells file is corrupted");
                }
                readCreatureSpells(header, data);
                break;

            case DOORS:
                // check header.getCheckOne() != 141 || header.getCheckTwo() != 142
                readDoors(header, data);
                break;

            case EFFECTS:
                // check header.getCheckOne() != 271 || header.getCheckTwo() != 272
                readEffects(header, data);
                break;

            case EFFECT_ELEMENTS:
                // check header.getCheckOne() != 251 || header.getCheckTwo() != 252
                readEffectElements(header, data);
                break;

            case KEEPER_SPELLS:
                // check header.getCheckOne() != 151 || header.getCheckTwo() != 152
                readKeeperSpells(header, data);
                break;

            case MAP:
                // check header.getCheckOne() != 101 || header.getCheckTwo() != 102
                readMap(header, data);
                break;

            case OBJECTS:
                // check header.getCheckOne() != 241 || header.getCheckTwo() != 242
                readObjects(header, data);
                break;

            case PLAYERS:
                // check header.getCheckOne() != 181 || header.getCheckTwo() != 182
                readPlayers(header, data);
                break;

            case ROOMS:
                // check header.getCheckOne() != 121 || header.getCheckTwo() != 122
                readRooms(header, data);
                break;

            case SHOTS:
                // check header.getCheckOne() != 261 || header.getCheckTwo() != 262
                readShots(header, data);
                break;

            case TERRAIN:
                // check header.getCheckOne() != 111 || header.getCheckTwo() != 112
                readTerrain(header, data);
                break;

            case THINGS:
                // check header.getCheckOne() != 191 || header.getCheckTwo() != 192
                readThings(header, data);
                break;

            case TRAPS:
                // check header.getCheckOne() != 131 || header.getCheckTwo() != 132
                readTraps(header, data);
                break;

            case TRIGGERS:
                // check header.getCheckOne() != 211 || header.getCheckTwo() != 212
                readTriggers(header, data);
                break;

            case VARIABLES:
                // check header.getCheckOne() != 231 || header.getCheckTwo() != 232
                readVariables(header, data);
                break;

            default:
                logger.log(Level.WARNING, "File type {0} have no reader", header.getId());
                break;
        }
    }

    /**
     * Reads the *Map.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readMap(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested MAP file
        logger.log(Level.INFO, "Reading map!");
        if (map == null) {
            map = new GameMap(header.getWidth(), header.getHeight());
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = new Tile();
                tile.setTerrainId(reader.readUnsignedByte());
                tile.setPlayerId(reader.readUnsignedByte());
                tile.setFlag(reader.readByteAsEnum(Tile.BridgeTerrainType.class));
                tile.setUnknown(reader.readUnsignedByte());
                map.setTile(x, y, tile);
            }
        }
    }

    /**
     * Reads the *Players.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readPlayers(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested PLAYER file
        if (players == null) {
            logger.log(Level.INFO, "Reading players!");
            players = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides players!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int playerIndex = 0; playerIndex < header.getItemCount(); playerIndex++) {
            long offset = reader.position();
            Player player = new Player();
            player.setStartingGold(reader.readInteger());
            player.setAi(reader.readInteger() == 1);

            AI ai = new AI();
            ai.setAiType(reader.readByteAsEnum(AI.AIType.class));
            ai.setSpeed(reader.readUnsignedByte());
            ai.setOpenness(reader.readUnsignedByte());
            ai.setRemoveCallToArmsIfTotalCreaturesLessThan(reader.readUnsignedByte());
            ai.setBuildLostRoomAfterSeconds(reader.readUnsignedByte());
            short[] unknown1 = new short[3];
            for (int i = 0; i < unknown1.length; i++) {
                unknown1[i] = reader.readUnsignedByte();
            }
            ai.setUnknown1(unknown1);
            ai.setCreateEmptyAreasWhenIdle(reader.readInteger() == 1);
            ai.setBuildBiggerLairAfterClaimingPortal(reader.readInteger() == 1);
            ai.setSellCapturedRoomsIfLowOnGold(reader.readInteger() == 1);
            ai.setMinTimeBeforePlacingResearchedRoom(reader.readUnsignedByte());
            ai.setDefaultSize(reader.readUnsignedByte());
            ai.setTilesLeftBetweenRooms(reader.readUnsignedByte());
            ai.setDistanceBetweenRoomsThatShouldBeCloseMan(reader.readByteAsEnum(AI.Distance.class));
            ai.setCorridorStyle(reader.readByteAsEnum(AI.CorridorStyle.class));
            ai.setWhenMoreSpaceInRoomRequired(reader.readByteAsEnum(AI.RoomExpandPolicy.class));
            ai.setDigToNeutralRoomsWithinTilesOfHeart(reader.readUnsignedByte());
            List<Short> buildOrder = new ArrayList<>(15);
            for (int i = 0; i < 15; i++) {
                buildOrder.add(reader.readUnsignedByte());
            }
            ai.setBuildOrder(buildOrder);
            ai.setFlexibility(reader.readUnsignedByte());
            ai.setDigToNeutralRoomsWithinTilesOfClaimedArea(reader.readUnsignedByte());
            ai.setRemoveCallToArmsAfterSeconds(reader.readUnsignedShort());
            ai.setBoulderTrapsOnLongCorridors(reader.readInteger() == 1);
            ai.setBoulderTrapsOnRouteToBreachPoints(reader.readInteger() == 1);
            ai.setTrapUseStyle(reader.readUnsignedByte());
            ai.setDoorTrapPreference(reader.readUnsignedByte());
            ai.setDoorUsage(reader.readByteAsEnum(AI.DoorUsagePolicy.class));
            ai.setChanceOfLookingToUseTrapsAndDoors(reader.readUnsignedByte());
            ai.setRequireMinLevelForCreatures(reader.readInteger() == 1);
            ai.setRequireTotalThreatGreaterThanTheEnemy(reader.readInteger() == 1);
            ai.setRequireAllRoomTypesPlaced(reader.readInteger() == 1);
            ai.setRequireAllKeeperSpellsResearched(reader.readInteger() == 1);
            ai.setOnlyAttackAttackers(reader.readInteger() == 1);
            ai.setNeverAttack(reader.readInteger() == 1);
            ai.setMinLevelForCreatures(reader.readUnsignedByte());
            ai.setTotalThreatGreaterThanTheEnemy(reader.readUnsignedByte());
            ai.setFirstAttemptToBreachRoom(reader.readByteAsEnum(AI.BreachRoomPolicy.class));
            ai.setFirstDigToEnemyPoint(reader.readByteAsEnum(AI.DigToPolicy.class));
            ai.setBreachAtPointsSimultaneously(reader.readUnsignedByte());
            ai.setUsePercentageOfTotalCreaturesInFirstFightAfterBreach(reader.readUnsignedByte());
            ai.setManaValue(reader.readUnsignedShort());
            ai.setPlaceCallToArmsWhereThreatValueIsGreaterThan(reader.readUnsignedShort());
            ai.setRemoveCallToArmsIfLessThanEnemyCreatures(reader.readUnsignedByte());
            ai.setRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles(reader.readUnsignedByte());
            ai.setPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements(reader.readInteger() == 1);
            ai.setThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue(reader.readUnsignedByte());
            ai.setSpellStyle(reader.readUnsignedByte());
            ai.setAttemptToImprisonPercentageOfEnemyCreatures(reader.readUnsignedByte());
            ai.setIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple(reader.readUnsignedByte());
            ai.setGoldValue(reader.readUnsignedShort());
            ai.setTryToMakeUnhappyOnesHappy(reader.readInteger() == 1);
            ai.setTryToMakeAngryOnesHappy(reader.readInteger() == 1);
            ai.setDisposeOfAngryCreatures(reader.readInteger() == 1);
            ai.setDisposeOfRubbishCreaturesIfBetterOnesComeAlong(reader.readInteger() == 1);
            ai.setDisposalMethod(reader.readByteAsEnum(AI.CreatureDisposalPolicy.class));
            ai.setMaximumNumberOfImps(reader.readUnsignedByte());
            ai.setWillNotSlapCreatures(reader.readUnsignedByte() == 0);
            ai.setAttackWhenNumberOfCreaturesIsAtLeast(reader.readUnsignedByte());
            ai.setUseLightningIfEnemyIsInWater(reader.readInteger() == 1);
            ai.setUseSightOfEvil(reader.readByteAsEnum(AI.SightOfEvilUsagePolicy.class));
            ai.setUseSpellsInBattle(reader.readUnsignedByte());
            ai.setSpellsPowerPreference(reader.readUnsignedByte());
            ai.setUseCallToArms(reader.readByteAsEnum(AI.CallToArmsUsagePolicy.class));
            short[] unknown2 = new short[2];
            for (int i = 0; i < unknown2.length; i++) {
                unknown2[i] = reader.readUnsignedByte();
            }
            ai.setUnknown2(unknown2);
            ai.setMineGoldUntilGoldHeldIsGreaterThan(reader.readUnsignedShort());
            ai.setWaitSecondsAfterPreviousAttackBeforeAttackingAgain(reader.readUnsignedShort());
            ai.setStartingMana(reader.readUnsignedInteger());
            ai.setExploreUpToTilesToFindSpecials(reader.readUnsignedShort());
            ai.setImpsToTilesRatio(reader.readUnsignedShort());
            ai.setBuildAreaStartX(reader.readUnsignedShort());
            ai.setBuildAreaStartY(reader.readUnsignedShort());
            ai.setBuildAreaEndX(reader.readUnsignedShort());
            ai.setBuildAreaEndY(reader.readUnsignedShort());
            ai.setLikelyhoodToMovingCreaturesToLibraryForResearching(reader.readByteAsEnum(AI.MoveToResearchPolicy.class));
            ai.setChanceOfExploringToFindSpecials(reader.readUnsignedByte());
            ai.setChanceOfFindingSpecialsWhenExploring(reader.readUnsignedByte());
            ai.setFateOfImprisonedCreatures(reader.readByteAsEnum(AI.ImprisonedCreatureFatePolicy.class));
            player.setAiAttributes(ai);

            player.setTriggerId(reader.readUnsignedShort());
            player.setPlayerId(reader.readUnsignedByte());
            player.setStartingCameraX(reader.readUnsignedShort());
            player.setStartingCameraY(reader.readUnsignedShort());

            player.setName(reader.readString(32).trim());

            // Add to the hash by the player ID
            players.put(player.getPlayerId(), player);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the Terrain.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readTerrain(KwdHeader header, IResourceReader file) throws RuntimeException, IOException {

        // Read the terrain catalog
        if (terrainTiles == null) {
            logger.log(Level.INFO, "Reading terrain!");
            terrainTiles = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides terrain!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            Terrain terrain = new Terrain();

            terrain.setName(reader.readString(32).trim());
            terrain.setCompleteResource(readArtResource(reader));
            terrain.setSideResource(readArtResource(reader));
            terrain.setTopResource(readArtResource(reader));
            terrain.setTaggedTopResource(readArtResource(reader));
            terrain.setStringIds(readStringId(reader));
            terrain.setDepth(reader.readIntegerAsFloat());
            terrain.setLightHeight(reader.readIntegerAsFloat());
            terrain.setFlags(reader.readIntegerAsFlag(Terrain.TerrainFlag.class));
            terrain.setDamage(reader.readUnsignedShort());
            terrain.setEditorTextureId(reader.readUnsignedShort());
            terrain.setUnk198(reader.readUnsignedShort());
            terrain.setGoldValue(reader.readUnsignedShort());
            terrain.setManaGain(reader.readUnsignedShort());
            terrain.setMaxManaGain(reader.readUnsignedShort());
            terrain.setTooltipStringId(reader.readUnsignedShort());
            terrain.setNameStringId(reader.readUnsignedShort());
            terrain.setMaxHealthEffectId(reader.readUnsignedShort());
            terrain.setDestroyedEffectId(reader.readUnsignedShort());
            terrain.setGeneralDescriptionStringId(reader.readUnsignedShort());
            terrain.setStrengthStringId(reader.readUnsignedShort());
            terrain.setWeaknessStringId(reader.readUnsignedShort());
            int[] unk1ae = new int[16];
            for (int x = 0; x < unk1ae.length; x++) {
                unk1ae[x] = reader.readUnsignedShort();
            }
            terrain.setUnk1ae(unk1ae);
            terrain.setWibbleH(reader.readUnsignedByte());
            short[] leanH = new short[3];
            for (int x = 0; x < leanH.length; x++) {
                leanH[x] = reader.readUnsignedByte();
            }
            terrain.setLeanH(leanH);
            terrain.setWibbleV(reader.readUnsignedByte());
            short[] leanV = new short[3];
            for (int x = 0; x < leanV.length; x++) {
                leanV[x] = reader.readUnsignedByte();
            }
            terrain.setLeanV(leanV);
            terrain.setTerrainId(reader.readUnsignedByte());
            terrain.setStartingHealth(reader.readUnsignedShort());
            terrain.setMaxHealthTypeTerrainId(reader.readUnsignedByte());
            terrain.setDestroyedTypeTerrainId(reader.readUnsignedByte());
            terrain.setTerrainLight(readColor(reader));
            terrain.setTextureFrames(reader.readUnsignedByte());

            terrain.setSoundCategory(reader.readString(32).trim());
            terrain.setMaxHealth(reader.readUnsignedShort());
            terrain.setAmbientLight(readColor(reader));

            terrain.setSoundCategoryFirstPerson(reader.readString(32).trim());
            terrain.setUnk224(reader.readUnsignedInteger());

            // Add to the hash by the terrain ID
            terrainTiles.put(terrain.getTerrainId(), terrain);

            // See that we have water & lava set
            if (map.getWater() == null && terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
                map.setWater(terrain);
            }
            if (map.getLava() == null && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                map.setLava(terrain);
            }

            // Check file offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads and parses an ArtResource object from the current file location (84
     * bytes)
     *
     * @param reader the resource reader
     * @return an ArtResource
     */
    private ArtResource readArtResource(IResourceChunkReader reader) throws IOException {
        ArtResource artResource = new ArtResource();

        // Read the data
        artResource.setName(reader.readString(64).trim());
        artResource.setFlags(reader.readIntegerAsFlag(ArtResource.ArtResourceFlag.class));

        reader.mark();
        reader.position(reader.position() + 12);
        artResource.setType(reader.readByteAsEnum(ArtResource.ArtResourceType.class));
        if (artResource.getType() == ArtResourceType.ANIMATING_MESH) {
            artResource.setData(ArtResource.KEY_START_AF, reader.readUnsignedByte()); // if HAS_START_ANIMATION
            artResource.setData(ArtResource.KEY_END_AF, reader.readUnsignedByte()); // if HAS_END_ANIMATION
        } else {
            artResource.setData("unknown_n", reader.readUnsignedShort());
        }
        artResource.setSometimesOne(reader.readUnsignedByte());

        reader.reset();
        switch (artResource.getType()) {
            case NONE: // skip empty type
                reader.readAndCheckNull(12);
                break;

            case SPRITE: // And alphas and images probably share the same attributes
            case ALPHA:
            case ADDITIVE_ALPHA:  // Images of different type
                artResource.setData(ArtResource.KEY_WIDTH, reader.readIntegerAsFloat());
                artResource.setData(ArtResource.KEY_HEIGHT, reader.readIntegerAsFloat());
                artResource.setData(ArtResource.KEY_FRAMES, reader.readUnsignedInteger()); // if (ANIMATING_TEXTURE)
                break;

            case TERRAIN_MESH:
                artResource.setData("unknown_1", reader.readUnsignedInteger());
                artResource.setData("unknown_2", reader.readUnsignedInteger());
                artResource.setData("unknown_3", reader.readUnsignedInteger());
                break;

            case MESH:
                artResource.setData(ArtResource.KEY_SCALE, reader.readIntegerAsFloat());
                artResource.setData(ArtResource.KEY_FRAMES, reader.readUnsignedInteger()); // if (ANIMATING_TEXTURE)
                artResource.setData("unknown_1", reader.readUnsignedInteger());
                break;

            case ANIMATING_MESH:
                artResource.setData(ArtResource.KEY_FRAMES, reader.readUnsignedInteger());
                artResource.setData(ArtResource.KEY_FPS, reader.readUnsignedInteger());
                artResource.setData(ArtResource.KEY_START_DIST, reader.readUnsignedShort());
                artResource.setData(ArtResource.KEY_END_DIST, reader.readUnsignedShort());
                break;

            case PROCEDURAL_MESH:
                artResource.setData(ArtResource.KEY_ID, reader.readUnsignedInteger());
                artResource.setData("unknown_1", reader.readUnsignedInteger());
                artResource.setData("unknown_2", reader.readUnsignedInteger());
                break;

            case MESH_COLLECTION: // FIXME nothing todo ?! has just the name, reference to GROP meshes probably
            case UNKNOWN:
                artResource.setData("unknown_1", reader.readUnsignedInteger());
                artResource.setData("unknown_2", reader.readUnsignedInteger());
                artResource.setData("unknown_3", reader.readUnsignedInteger());
                break;

            default:
                reader.readAndCheckNull(12);
                logger.log(Level.WARNING, "Unknown artResource type {0}", artResource.getType());
                break;
        }
        reader.skipBytes(4);

        // If it has no name or the type is not known, return null
        if (artResource.getName().isEmpty() || artResource.getType() == null) {
            return null;
        }

        return artResource;
    }

    /**
     * Reads color from file
     *
     * @param reader the file stream to parse from
     * @return a Color
     */
    private Color readColor(IResourceChunkReader reader) {
        return new Color(reader.readUnsignedByte(), reader.readUnsignedByte(), reader.readUnsignedByte());
    }

    /**
     * Reads and parses an StringId object from the current file location
     *
     * @param reader the file stream to parse from
     * @return an StringId
     */
    private StringId readStringId(IResourceChunkReader reader) {

        // Read the IDs
        int[] ids = new int[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = reader.readUnsignedInteger();
        }

        // And the unknowns
        short[] x14 = new short[4];
        for (int i = 0; i < x14.length; i++) {
            x14[i] = reader.readUnsignedByte();
        }

        return new StringId(ids, x14);
    }

    /**
     * Reads the Doors.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readDoors(KwdHeader header, IResourceReader file) throws IOException {

        // Read the doors catalog
        if (doors == null) {
            logger.log(Level.INFO, "Reading doors!");
            doors = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides doors!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            Door door = new Door();

            door.setName(reader.readString(32).trim());
            door.setMesh(readArtResource(reader));
            door.setGuiIcon(readArtResource(reader));
            door.setEditorIcon(readArtResource(reader));
            door.setFlowerIcon(readArtResource(reader));
            door.setOpenResource(readArtResource(reader));
            door.setCloseResource(readArtResource(reader));
            door.setHeight(reader.readIntegerAsFloat());
            door.setHealthGain(reader.readUnsignedShort());
            door.setUnknown1(reader.readUnsignedShort());
            door.setUnknown2(reader.readUnsignedInteger());
            door.setResearchTime(reader.readUnsignedShort());
            door.setMaterial(reader.readByteAsEnum(Material.class));
            door.setTrapTypeId(reader.readUnsignedByte());
            door.setFlags(reader.readIntegerAsFlag(DoorFlag.class));
            door.setHealth(reader.readUnsignedShort());
            door.setGoldCost(reader.readUnsignedShort());
            short[] unknown3 = new short[2];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = reader.readUnsignedByte();
            }
            door.setUnknown3(unknown3);
            door.setDeathEffectId(reader.readUnsignedShort());
            door.setManufToBuild(reader.readUnsignedInteger());
            door.setManaCost(reader.readUnsignedShort());
            door.setTooltipStringId(reader.readUnsignedShort());
            door.setNameStringId(reader.readUnsignedShort());
            door.setGeneralDescriptionStringId(reader.readUnsignedShort());
            door.setStrengthStringId(reader.readUnsignedShort());
            door.setWeaknessStringId(reader.readUnsignedShort());
            door.setDoorId(reader.readUnsignedByte());
            door.setOrderInEditor(reader.readUnsignedByte());
            door.setManufCrateObjectId(reader.readUnsignedByte());
            door.setKeyObjectId(reader.readUnsignedByte());

            door.setSoundCategory(reader.readString(32).trim());

            doors.put(door.getDoorId(), door);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the Traps.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readTraps(KwdHeader header, IResourceReader file) throws IOException {

        // Read the traps catalog
        if (traps == null) {
            logger.log(Level.INFO, "Reading traps!");
            traps = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides traps!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            Trap trap = new Trap();

            trap.setName(reader.readString(32).trim());
            trap.setMeshResource(readArtResource(reader));
            trap.setGuiIcon(readArtResource(reader));
            trap.setEditorIcon(readArtResource(reader));
            trap.setFlowerIcon(readArtResource(reader));
            trap.setFireResource(readArtResource(reader));
            trap.setHeight(reader.readIntegerAsFloat());
            trap.setRechargeTime(reader.readIntegerAsFloat());
            trap.setChargeTime(reader.readIntegerAsFloat());
            trap.setThreatDuration(reader.readIntegerAsFloat());
            trap.setManaCostToFire(reader.readUnsignedInteger());
            trap.setIdleEffectDelay(reader.readIntegerAsFloat());
            trap.setTriggerData(reader.readUnsignedInteger());
            trap.setShotData1(reader.readUnsignedInteger());
            trap.setShotData2(reader.readUnsignedInteger());
            trap.setResearchTime(reader.readUnsignedShort());
            trap.setThreat(reader.readUnsignedShort());
            trap.setFlags(reader.readIntegerAsFlag(Trap.TrapFlag.class));
            trap.setHealth(reader.readUnsignedShort());
            trap.setGoldCost(reader.readUnsignedShort());
            trap.setPowerlessEffectId(reader.readUnsignedShort());
            trap.setIdleEffectId(reader.readUnsignedShort());
            trap.setDeathEffectId(reader.readUnsignedShort());
            trap.setManufToBuild(reader.readUnsignedShort());
            trap.setGeneralDescriptionStringId(reader.readUnsignedShort());
            trap.setStrengthStringId(reader.readUnsignedShort());
            trap.setWeaknessStringId(reader.readUnsignedShort());
            trap.setManaUsage(reader.readUnsignedShort());
            short[] unknown4 = new short[2];
            for (int x = 0; x < unknown4.length; x++) {
                unknown4[x] = reader.readUnsignedByte();
            }
            trap.setUnknown4(unknown4);
            trap.setTooltipStringId(reader.readUnsignedShort());
            trap.setNameStringId(reader.readUnsignedShort());
            trap.setShotsWhenArmed(reader.readUnsignedByte());
            trap.setTriggerType(reader.readByteAsEnum(Trap.TriggerType.class));
            trap.setTrapId(reader.readUnsignedByte());
            trap.setShotTypeId(reader.readUnsignedByte());
            trap.setManufCrateObjectId(reader.readUnsignedByte());

            trap.setSoundCategory(reader.readString(32).trim());
            trap.setMaterial(reader.readByteAsEnum(Material.class));
            trap.setOrderInEditor(reader.readUnsignedByte());
            trap.setShotOffset(reader.readIntegerAsFloat(),
                    reader.readIntegerAsFloat(),
                    reader.readIntegerAsFloat());
            trap.setShotDelay(reader.readIntegerAsFloat());
            trap.setHealthGain(reader.readUnsignedShort());

            traps.put(trap.getTrapId(), trap);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the Rooms.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readRooms(KwdHeader header, IResourceReader file) throws RuntimeException, IOException {

        // Read the rooms catalog
        if (rooms == null) {
            logger.log(Level.INFO, "Reading rooms!");
            rooms = HashMap.newHashMap(header.getItemCount());
            roomsByTerrainId = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides rooms!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            Room room = new Room();

            room.setName(reader.readString(32).trim());
            room.setGuiIcon(readArtResource(reader));
            room.setEditorIcon(readArtResource(reader));
            room.setCompleteResource(readArtResource(reader));
            room.setStraightResource(readArtResource(reader));
            room.setInsideCornerResource(readArtResource(reader));
            room.setUnknownResource(readArtResource(reader));
            room.setOutsideCornerResource(readArtResource(reader));
            room.setWallResource(readArtResource(reader));
            room.setCapResource(readArtResource(reader));
            room.setCeilingResource(readArtResource(reader));
            room.setCeilingHeight(reader.readIntegerAsFloat());
            room.setResearchTime(reader.readUnsignedShort());
            room.setTorchIntensity(reader.readUnsignedShort());
            room.setFlags(reader.readIntegerAsFlag(Room.RoomFlag.class));
            room.setTooltipStringId(reader.readUnsignedShort());
            room.setNameStringId(reader.readUnsignedShort());
            room.setCost(reader.readUnsignedShort());
            room.setFightEffectId(reader.readUnsignedShort());
            room.setGeneralDescriptionStringId(reader.readUnsignedShort());
            room.setStrengthStringId(reader.readUnsignedShort());
            room.setTorchHeight(reader.readShortAsFloat());
            List<Integer> roomEffects = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                int effectId = reader.readUnsignedShort();
                roomEffects.add(effectId);
            }
            room.setEffects(roomEffects);
            room.setRoomId(reader.readUnsignedByte());
            room.setReturnPercentage(reader.readUnsignedByte());
            room.setTerrainId(reader.readUnsignedByte());
            room.setTileConstruction(reader.readByteAsEnum(Room.TileConstruction.class));
            room.setCreatedCreatureId(reader.readUnsignedByte());
            room.setTorchColor(readColor(reader)); // This is the editor is rather weird
            List<Short> roomObjects = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                short objectId = reader.readUnsignedByte();
                roomObjects.add(objectId);
            }
            room.setObjects(roomObjects);

            room.setSoundCategory(reader.readString(32).trim());
            room.setOrderInEditor(reader.readUnsignedByte());
            room.setTorchRadius(reader.readIntegerAsFloat());
            room.setTorch(readArtResource(reader));
            room.setRecommendedSizeX(reader.readUnsignedByte());
            room.setRecommendedSizeY(reader.readUnsignedByte());
            room.setHealthGain(reader.readShort());

            // Add to the hash by the room ID
            rooms.put(room.getRoomId(), room);

            // And by the terrain ID
            roomsByTerrainId.put(room.getTerrainId(), room);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the *.kwd
     *
     * @param file the original map KWD file
     * @throws RuntimeException reading may fail
     */
    private void readMapInfo(KwdHeader header, IResourceReader data) throws IOException {

        // Additional header data
        if (gameLevel == null) {
            logger.log(Level.INFO, "Reading level info!");
            gameLevel = new GameLevel();
        } else {
            logger.log(Level.WARNING, "Overrides level!");
        }

        // Property data
        IResourceChunkReader reader = data.readChunk(header.dataSize + 8);
        String name = reader.readStringUtf16(64).trim();
        if (name != null && !name.isEmpty() && name.toLowerCase().endsWith(".kwd")) {
            name = name.substring(0, name.length() - 4);
        }
        gameLevel.setName(name);
        gameLevel.setDescription(reader.readStringUtf16(1024).trim());
        gameLevel.setAuthor(reader.readStringUtf16(64).trim());
        gameLevel.setEmail(reader.readStringUtf16(64).trim());
        gameLevel.setInformation(reader.readStringUtf16(1024).trim());

        gameLevel.setTriggerId(reader.readUnsignedShort());
        gameLevel.setTicksPerSec(reader.readUnsignedShort());
        short[] x01184 = new short[520];
        for (int x = 0; x < x01184.length; x++) {
            x01184[x] = reader.readUnsignedByte();
        }
        gameLevel.setX01184(x01184);
        // I don't know if we need the index, level 19 & 3 has messages, but they are rare
        List<String> messages = new ArrayList<>();
        for (int x = 0; x < 512; x++) {
            String message = reader.readStringUtf16(20).trim();
            if (!message.isEmpty()) {
                messages.add(message);
            }
        }
        gameLevel.setMessages(messages);

        gameLevel.setLvlFlags(reader.readShortAsFlag(LevFlag.class));
        gameLevel.setSoundCategory(reader.readString(32).trim());
        gameLevel.setTalismanPieces(reader.readUnsignedByte());

        for (int x = 0; x < 4; x++) {
            LevelReward reward = reader.readByteAsEnum(LevelReward.class);
            gameLevel.addRewardPrev(reward);
        }

        for (int x = 0; x < 4; x++) {
            LevelReward reward = reader.readByteAsEnum(LevelReward.class);
            gameLevel.addRewardNext(reward);
        }

        gameLevel.setSoundTrack(reader.readUnsignedByte());
        gameLevel.setTextTableId(reader.readByteAsEnum(TextTable.class));
        gameLevel.setTextTitleId(reader.readUnsignedShort());
        gameLevel.setTextPlotId(reader.readUnsignedShort());
        gameLevel.setTextDebriefId(reader.readUnsignedShort());
        gameLevel.setTextObjectvId(reader.readUnsignedShort());
        gameLevel.setX063c3(reader.readUnsignedShort());
        gameLevel.setTextSubobjctvId1(reader.readUnsignedShort());
        gameLevel.setTextSubobjctvId2(reader.readUnsignedShort());
        gameLevel.setTextSubobjctvId3(reader.readUnsignedShort());
        gameLevel.setSpeclvlIdx(reader.readUnsignedShort());

        // Swap the arrays for more convenient reader format
        short[] textIntrdcOverrdObj = new short[8];
        for (int x = 0; x < textIntrdcOverrdObj.length; x++) {
            textIntrdcOverrdObj[x] = reader.readUnsignedByte();
        }
        int[] textIntrdcOverrdId = new int[8];
        for (int x = 0; x < textIntrdcOverrdId.length; x++) {
            textIntrdcOverrdId[x] = reader.readUnsignedShort();
        }
        Map<Short, Integer> introductionOverrideTextIds = HashMap.newHashMap(8);
        for (int x = 0; x < textIntrdcOverrdObj.length; x++) {
            if (textIntrdcOverrdObj[x] > 0) {
                // Over 0 is a valid creature ID
                introductionOverrideTextIds.put(textIntrdcOverrdObj[x], textIntrdcOverrdId[x]);
            }
        }
        gameLevel.setIntroductionOverrideTextIds(introductionOverrideTextIds);

        gameLevel.setTerrainPath(reader.readString(32).trim());
        // Some very old readers are smaller, namely the FrontEnd3DLevel map in some version
        if (header.dataSize > HEADER_SIZE) {
            gameLevel.setOneShotHornyLev(reader.readUnsignedByte());
            gameLevel.setPlayerCount(reader.readUnsignedByte());
            gameLevel.addRewardPrev(reader.readByteAsEnum(LevelReward.class));
            gameLevel.addRewardNext(reader.readByteAsEnum(LevelReward.class));
            gameLevel.setSpeechHornyId(reader.readUnsignedShort());
            gameLevel.setSpeechPrelvlId(reader.readUnsignedShort());
            gameLevel.setSpeechPostlvlWin(reader.readUnsignedShort());
            gameLevel.setSpeechPostlvlLost(reader.readUnsignedShort());
            gameLevel.setSpeechPostlvlNews(reader.readUnsignedShort());
            gameLevel.setSpeechPrelvlGenr(reader.readUnsignedShort());
            gameLevel.setHeroName(reader.readStringUtf16(32).trim());
        }

        // Paths and the unknown array
        int checkThree = reader.readUnsignedInteger();
        if (checkThree != 222) {
            throw new RuntimeException("Level reader is corrupted");
        }

        // The last part of reader has size contentSize
        int contentSize = reader.readUnsignedInteger();
        reader = data.readChunk(contentSize);
        boolean customOverrides = false;
        List<FilePath> paths = new ArrayList<>(header.getItemCount());
        for (int x = 0; x < header.getItemCount(); x++) {
            FilePath readerPath = new FilePath();
            readerPath.setId(reader.readIntegerAsEnum(MapDataTypeEnum.class));
            readerPath.setUnknown2(reader.readInteger());
            String path = reader.readString(64).trim();

            // Tweak the paths
            // Paths are relative to the base path, may or may not have an extension (assume kwd if none found)
            path = PathUtils.convertFileSeparators(path);
            if (!".".equals(path.substring(path.length() - 4, path.length() - 3))) {
                path = path.concat(".kwd");
            }

            // See if the globals are present
            if (readerPath.getId() == MapDataTypeEnum.GLOBALS) {
                customOverrides = true;
                logger.log(Level.INFO, "The map uses custom overrides!");
            }

            readerPath.setPath(path);

            paths.add(readerPath);
        }
        gameLevel.setPaths(paths);

        // Hmm, seems that normal maps don't refer the effects nor effect elements
        if (!customOverrides) {
            FilePath filePath = new FilePath(MapDataTypeEnum.EFFECTS, PathUtils.DKII_EDITOR_FOLDER + "Effects.kwd");
            if (!gameLevel.getPaths().contains(filePath)) {
                gameLevel.getPaths().add(filePath);
            }

            filePath = new FilePath(MapDataTypeEnum.EFFECT_ELEMENTS, PathUtils.DKII_EDITOR_FOLDER + "EffectElements.kwd");
            if (!gameLevel.getPaths().contains(filePath)) {
                gameLevel.getPaths().add(filePath);
            }
        }

        int[] unknown = new int[header.getHeight()];
        for (int x = 0; x < unknown.length; x++) {
            unknown[x] = reader.readUnsignedInteger();
        }
        gameLevel.setUnknown(unknown);
    }

    /**
     * Reads the Creatures.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readCreatures(KwdHeader header, IResourceReader file) throws IOException {

        // Read the creatures catalog
        if (creatures == null) {
            logger.log(Level.INFO, "Reading creatures!");
            creatures = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides creatures!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            Creature creature = new Creature();

            creature.setName(reader.readString(32).trim());
            // 39 ArtResources (with IMPs these are not 100% same)
            creature.setUnknown1Resource(reader.read(84));  // all 0: Maiden Of The Nest, Prince Balder, Horny. Other the same
            creature.setAnimation(AnimationType.WALK, readArtResource(reader));
            creature.setAnimation(AnimationType.RUN, readArtResource(reader));
            creature.setAnimation(AnimationType.DRAGGED, readArtResource(reader));
            creature.setAnimation(AnimationType.RECOIL_FORWARDS, readArtResource(reader));
            creature.setAnimation(AnimationType.MELEE_ATTACK, readArtResource(reader));
            creature.setAnimation(AnimationType.CAST_SPELL, readArtResource(reader));
            creature.setAnimation(AnimationType.DIE, readArtResource(reader));
            creature.setAnimation(AnimationType.HAPPY, readArtResource(reader));
            creature.setAnimation(AnimationType.ANGRY, readArtResource(reader));
            creature.setAnimation(AnimationType.STUNNED, readArtResource(reader));
            creature.setAnimation(AnimationType.IN_HAND, readArtResource(reader));
            creature.setAnimation(AnimationType.SLEEPING, readArtResource(reader));
            creature.setAnimation(AnimationType.EATING, readArtResource(reader));
            creature.setAnimation(AnimationType.RESEARCHING, readArtResource(reader));
            creature.setAnimation(AnimationType.NULL_2, readArtResource(reader));
            creature.setAnimation(AnimationType.NULL_1, readArtResource(reader));
            creature.setAnimation(AnimationType.TORTURED_WHEEL, readArtResource(reader));
            creature.setAnimation(AnimationType.NULL_3, readArtResource(reader));
            creature.setAnimation(AnimationType.DRINKING, readArtResource(reader));
            creature.setAnimation(AnimationType.IDLE_1, readArtResource(reader));
            creature.setAnimation(AnimationType.RECOIL_BACKWARDS, readArtResource(reader));
            creature.setAnimation(AnimationType.MANUFACTURING, readArtResource(reader));
            creature.setAnimation(AnimationType.PRAYING, readArtResource(reader));
            creature.setAnimation(AnimationType.FALLBACK, readArtResource(reader));
            creature.setAnimation(AnimationType.TORTURED_CHAIR, readArtResource(reader));
            creature.setAnimation(AnimationType.TORTURED_CHAIR_SKELETON, readArtResource(reader));
            creature.setAnimation(AnimationType.GET_UP, readArtResource(reader));
            creature.setAnimation(AnimationType.DANCE, readArtResource(reader));
            creature.setAnimation(AnimationType.DRUNK, readArtResource(reader));
            creature.setAnimation(AnimationType.ENTRANCE, readArtResource(reader));
            creature.setAnimation(AnimationType.IDLE_2, readArtResource(reader));
            creature.setAnimation(AnimationType.SPECIAL_1, readArtResource(reader));
            creature.setAnimation(AnimationType.SPECIAL_2, readArtResource(reader));
            creature.setAnimation(AnimationType.DRUNKED_WALK, readArtResource(reader));
            creature.setAnimation(AnimationType.ROAR, readArtResource(reader)); // FIXME
            creature.setAnimation(AnimationType.NULL_4, readArtResource(reader));

            creature.setIcon1Resource(readArtResource(reader));
            creature.setIcon2Resource(readArtResource(reader));
            //
            creature.setUnkcec(reader.readUnsignedShort());
            creature.setUnkcee(reader.readUnsignedInteger());
            creature.setUnkcf2(reader.readUnsignedInteger());
            creature.setOrderInEditor(reader.readUnsignedByte());
            creature.setAngerStringIdGeneral(reader.readUnsignedShort());
            creature.setShotDelay(reader.readIntegerAsFloat());
            creature.setOlhiEffectId(reader.readUnsignedShort());
            creature.setIntroductionStringId(reader.readUnsignedShort());
            creature.getAttributes().setPerceptionRange(reader.readIntegerAsFloat());
            creature.setAngerStringIdLair(reader.readUnsignedShort());
            creature.setAngerStringIdFood(reader.readUnsignedShort());
            creature.setAngerStringIdPay(reader.readUnsignedShort());
            creature.setAngerStringIdWork(reader.readUnsignedShort());
            creature.setAngerStringIdSlap(reader.readUnsignedShort());
            creature.setAngerStringIdHeld(reader.readUnsignedShort());
            creature.setAngerStringIdLonely(reader.readUnsignedShort());
            creature.setAngerStringIdHatred(reader.readUnsignedShort());
            creature.setAngerStringIdTorture(reader.readUnsignedShort());

            creature.setTranslationSoundGategory(reader.readString(32).trim());
            creature.getAttributes().setShuffleSpeed(reader.readIntegerAsFloat());
            creature.setCloneCreatureId(reader.readUnsignedByte());
            creature.setFirstPersonGammaEffect(reader.readByteAsEnum(Creature.GammaEffect.class));
            creature.setFirstPersonWalkCycleScale(reader.readUnsignedByte());
            creature.setIntroCameraPathIndex(reader.readUnsignedByte());
            creature.setUnk2e2(reader.readUnsignedByte());
            creature.setPortraitResource(readArtResource(reader));
            creature.setLight(readLight(reader));
            Attraction[] attractions = new Attraction[2];
            for (int x = 0; x < attractions.length; x++) {
                Attraction attraction = creature.new Attraction();
                attraction.setPresent(reader.readUnsignedInteger());
                attraction.setRoomId(reader.readUnsignedShort());
                attraction.setRoomSize(reader.readUnsignedShort());
                attractions[x] = attraction;
            }
            creature.setAttractions(attractions);
            creature.setFirstPersonWaddleScale(reader.readIntegerAsFloat());
            creature.setFirstPersonOscillateScale(reader.readIntegerAsFloat());
            List<Spell> spells = new ArrayList<>(3);
            for (int x = 0; x < 3; x++) {
                Spell spell = creature.new Spell();
                spell.setShotOffset(reader.readIntegerAsFloat(),
                        reader.readIntegerAsFloat(),
                        reader.readIntegerAsFloat());
                spell.setX0c(reader.readUnsignedByte());
                spell.setPlayAnimation(reader.readUnsignedByte() == 1);
                spell.setX0e(reader.readUnsignedByte()); // This value can changed when you not change anything on map, only save it
                spell.setX0f(reader.readUnsignedByte());
                spell.setShotDelay(reader.readIntegerAsFloat());
                spell.setX14(reader.readUnsignedByte());
                spell.setX15(reader.readUnsignedByte());
                spell.setCreatureSpellId(reader.readUnsignedByte());
                spell.setLevelAvailable(reader.readUnsignedByte());
                if (spell.getCreatureSpellId() != 0) {
                    spells.add(spell);
                }
            }
            creature.setSpells(spells);
            Creature.Resistance[] resistances = new Creature.Resistance[4];
            for (int x = 0; x < resistances.length; x++) {
                Creature.Resistance resistance = creature.new Resistance();
                resistance.setAttackType(reader.readByteAsEnum(Creature.AttackType.class));
                resistance.setValue(reader.readUnsignedByte());
                resistances[x] = resistance;
            }
            creature.setResistances(resistances);
            creature.setHappyJobs(readJobPreferences(3, creature, reader));
            creature.setUnhappyJobs(readJobPreferences(2, creature, reader));
            creature.setAngryJobs(readJobPreferences(3, creature, reader));
            Creature.JobType[] hateJobs = new Creature.JobType[2];
            for (int x = 0; x < hateJobs.length; x++) {
                hateJobs[x] = reader.readIntegerAsEnum(Creature.JobType.class);
            }
            creature.setHateJobs(hateJobs);
            JobAlternative[] alternatives = new JobAlternative[3];
            for (int x = 0; x < alternatives.length; x++) {
                JobAlternative alternative = creature.new JobAlternative();
                alternative.setJobType(reader.readIntegerAsEnum(Creature.JobType.class));
                alternative.setMoodChange(reader.readUnsignedShort());
                alternative.setManaChange(reader.readUnsignedShort());
                alternatives[x] = alternative;
            }
            creature.setAlternativeJobs(alternatives);
            creature.setAnimationOffsets(OffsetType.PORTAL_ENTRANCE,
                    reader.readIntegerAsFloat(),
                    reader.readIntegerAsFloat(),
                    reader.readIntegerAsFloat()
            );
            creature.setUnkea0(reader.readInteger());
            creature.getAttributes().setHeight(reader.readIntegerAsFloat());
            creature.setUnkea8(reader.readIntegerAsFloat());
            creature.setUnk3ab(reader.readUnsignedInteger());
            creature.getAttributes().setEyeHeight(reader.readIntegerAsFloat());
            creature.getAttributes().setSpeed(reader.readIntegerAsFloat());
            creature.getAttributes().setRunSpeed(reader.readIntegerAsFloat());
            creature.getAttributes().setHungerRate(reader.readIntegerAsFloat());
            creature.getAttributes().setTimeAwake(reader.readUnsignedInteger());
            creature.getAttributes().setTimeSleep(reader.readUnsignedInteger());
            creature.getAttributes().setDistanceCanSee(reader.readIntegerAsFloat());
            creature.getAttributes().setDistanceCanHear(reader.readIntegerAsFloat());
            creature.getAttributes().setStunDuration(reader.readIntegerAsFloat());
            creature.getAttributes().setGuardDuration(reader.readIntegerAsFloat());
            creature.getAttributes().setIdleDuration(reader.readIntegerAsFloat());
            creature.getAttributes().setSlapFearlessDuration(reader.readIntegerAsFloat());
            creature.setUnkee0(reader.readInteger());
            creature.setUnkee4(reader.readInteger());
            creature.getAttributes().setPossessionManaCost(reader.readShort());
            creature.getAttributes().setOwnLandHealthIncrease(reader.readShort());
            creature.setMeleeRange(reader.readIntegerAsFloat());
            creature.setUnkef0(reader.readUnsignedInteger());
            creature.getAttributes().setTortureTimeToConvert(reader.readIntegerAsFloat());
            creature.setMeleeRecharge(reader.readIntegerAsFloat());
            // The flags is actually very big, pushing the boundaries, a true uint32, need to -> long
            creature.setFlags(reader.readIntegerAsFlag(Creature.CreatureFlag.class));
            creature.getAttributes().setExpForNextLevel(reader.readUnsignedShort());
            creature.setJobClass(reader.readByteAsEnum(Creature.JobClass.class));
            creature.setFightStyle(reader.readByteAsEnum(Creature.FightStyle.class));
            creature.getAttributes().setExpPerSecond(reader.readUnsignedShort());
            creature.getAttributes().setExpPerSecondTraining(reader.readUnsignedShort());
            creature.getAttributes().setResearchPerSecond(reader.readUnsignedShort());
            creature.getAttributes().setManufacturePerSecond(reader.readUnsignedShort());
            creature.getAttributes().setHp(reader.readUnsignedShort());
            creature.getAttributes().setHpFromChicken(reader.readUnsignedShort());
            creature.getAttributes().setFear(reader.readUnsignedShort());
            creature.getAttributes().setThreat(reader.readUnsignedShort());
            creature.setMeleeDamage(reader.readUnsignedShort());
            creature.getAttributes().setSlapDamage(reader.readUnsignedShort());
            creature.getAttributes().setManaGenPrayer(reader.readUnsignedShort());
            creature.setUnk3cb(reader.readUnsignedShort());
            creature.getAttributes().setPay(reader.readUnsignedShort());
            creature.getAttributes().setMaxGoldHeld(reader.readUnsignedShort());
            creature.setUnk3cc(reader.readShortAsFloat());
            creature.getAttributes().setDecomposeValue(reader.readUnsignedShort());
            creature.setNameStringId(reader.readUnsignedShort());
            creature.setTooltipStringId(reader.readUnsignedShort());
            creature.getAttributes().setAngerNoLair(reader.readShort());
            creature.getAttributes().setAngerNoFood(reader.readShort());
            creature.getAttributes().setAngerNoPay(reader.readShort());
            creature.getAttributes().setAngerNoWork(reader.readShort());
            creature.getAttributes().setAngerSlap(reader.readShort());
            creature.getAttributes().setAngerInHand(reader.readShort());
            creature.getAttributes().setInitialGoldHeld(reader.readShort());
            creature.setEntranceEffectId(reader.readUnsignedShort());
            creature.setGeneralDescriptionStringId(reader.readUnsignedShort());
            creature.setStrengthStringId(reader.readUnsignedShort());
            creature.setWeaknessStringId(reader.readUnsignedShort());
            creature.setSlapEffectId(reader.readUnsignedShort());
            creature.setDeathEffectId(reader.readUnsignedShort());
            creature.setMelee1Swipe(reader.readByteAsEnum(Creature.Swipe.class));
            creature.setMelee2Swipe(reader.readByteAsEnum(Creature.Swipe.class));
            creature.setMelee3Swipe(reader.readByteAsEnum(Creature.Swipe.class));
            creature.setSpellSwipe(reader.readByteAsEnum(Creature.Swipe.class));
            creature.setFirstPersonSpecialAbility1(reader.readByteAsEnum(Creature.SpecialAbility.class));
            creature.setFirstPersonSpecialAbility2(reader.readByteAsEnum(Creature.SpecialAbility.class));
            short[] unkf48 = new short[3];
            for (int x = 0; x < unkf48.length; x++) {
                unkf48[x] = reader.readUnsignedByte();
            }
            creature.setUnkf48(unkf48);
            creature.setCreatureId(reader.readUnsignedByte());
            short[] unk3ea = new short[2];
            for (int x = 0; x < unk3ea.length; x++) {
                unk3ea[x] = reader.readUnsignedByte();
            }
            creature.setUnk3ea(unk3ea);
            creature.getAttributes().setHungerFill(reader.readUnsignedByte());
            creature.getAttributes().setUnhappyThreshold(reader.readUnsignedByte());
            creature.setMeleeAttackType(reader.readByteAsEnum(Creature.AttackType.class));
            creature.setUnk3eb2(reader.readUnsignedByte());
            creature.setLairObjectId(reader.readUnsignedByte());
            creature.setUnk3f1(reader.readUnsignedByte());
            creature.setDeathFallDirection(reader.readByteAsEnum(Creature.DeathFallDirection.class));
            creature.setUnk3f2(reader.readUnsignedByte());

            creature.setSoundCategory(reader.readString(32).trim());
            creature.setMaterial(reader.readByteAsEnum(Material.class));
            creature.setFirstPersonFilterResource(readArtResource(reader));
            creature.setUnkfcb(reader.readUnsignedShort());
            creature.setUnk4(reader.readIntegerAsFloat());
            creature.setAnimation(AnimationType.DRUNKED_IDLE, readArtResource(reader));
            creature.setSpecial1Swipe(reader.readByteAsEnum(Creature.Swipe.class));
            creature.setSpecial2Swipe(reader.readByteAsEnum(Creature.Swipe.class));
            creature.setFirstPersonMeleeResource(readArtResource(reader));
            creature.setUnk6(reader.readUnsignedInteger());
            creature.getAttributes().setTortureHpChange(reader.readShort());
            creature.getAttributes().setTortureMoodChange(reader.readShort());
            creature.setAnimation(AnimationType.SWIPE, readArtResource(reader));
            creature.setAnimation(AnimationType.IDLE_3, readArtResource(reader));
            creature.setAnimation(AnimationType.IDLE_4, readArtResource(reader));
            creature.setAnimation(AnimationType.IDLE_3_1, readArtResource(reader));
            creature.setAnimation(AnimationType.IDLE_4_1, readArtResource(reader));
            creature.setAnimation(AnimationType.DIG, readArtResource(reader));

            OffsetType[] offsetTypes = new OffsetType[]{OffsetType.FALL_BACK_GET_UP,
                OffsetType.PRAYING, OffsetType.CORPSE, OffsetType.OFFSET_5,
                OffsetType.OFFSET_6, OffsetType.OFFSET_7, OffsetType.OFFSET_8};
            for (OffsetType type : offsetTypes) {
                creature.setAnimationOffsets(type,
                        reader.readIntegerAsFloat(),
                        reader.readIntegerAsFloat(),
                        reader.readIntegerAsFloat()
                );
            }
            creature.setAnimation(AnimationType.BACK_OFF, readArtResource(reader));
            X1323[] x1323s = new X1323[48];
            for (int x = 0; x < x1323s.length; x++) {
                X1323 x1323 = creature.new X1323();
                x1323.setX00(reader.readUnsignedShort());
                x1323.setX02(reader.readUnsignedShort());
                x1323s[x] = x1323;
            }
            creature.setX1323(x1323s);
            creature.setAnimation(AnimationType.STAND_STILL, readArtResource(reader));
            creature.setAnimation(AnimationType.STEALTH_WALK, readArtResource(reader));
            creature.setAnimation(AnimationType.DEATH_POSE, readArtResource(reader));
            creature.setUniqueNameTextId(reader.readUnsignedShort());
            int[] x14e1 = new int[2];
            for (int x = 0; x < x14e1.length; x++) {
                x14e1[x] = reader.readUnsignedInteger();
            }
            creature.setX14e1(x14e1);
            creature.setFirstPersonSpecialAbility1Count(reader.readUnsignedInteger());
            creature.setFirstPersonSpecialAbility2Count(reader.readUnsignedInteger());
            creature.setUniqueResource(readArtResource(reader));
            creature.setFlags3(reader.readIntegerAsFlag(Creature.CreatureFlag3.class));

            // The normal reader stops here, but if it is the bigger one, continue
            if (header.getItemSize() > CREATURE_SIZE) {
                short[] unknownExtraBytes = new short[80];
                for (int x = 0; x < unknownExtraBytes.length; x++) {
                    unknownExtraBytes[x] = reader.readUnsignedByte();
                }
                creature.setUnknownExtraBytes(unknownExtraBytes);
                creature.setFlags2(reader.readIntegerAsFlag(Creature.CreatureFlag2.class));
                creature.setUnknown(reader.readUnsignedShort());
                creature.setUnknown_1(reader.readShortAsFloat());
            }

            // Add to the hash by the creature ID
            creatures.put(creature.getCreatureId(), creature);

            // Set the imp
            if (imp == null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER)
                    && creature.getFlags().contains(Creature.CreatureFlag.IS_EVIL)) {
                imp = creature;
            }

            // Set the dwarf
            if (dwarf == null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER)
                    && !creature.getFlags().contains(Creature.CreatureFlag.IS_EVIL)) {
                dwarf = creature;
            }

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Read job preferences for a creature
     *
     * @param count amount of job preference records
     * @param creature creature instance, just for creating a job preference
     * instance
     * @param file the file to read the data from
     * @return job preferences
     */
    private Creature.JobPreference[] readJobPreferences(int count, Creature creature, IResourceChunkReader reader) {
        Creature.JobPreference[] preferences = new Creature.JobPreference[count];
        for (int x = 0; x < preferences.length; x++) {
            Creature.JobPreference jobPreference = creature.new JobPreference();
            jobPreference.setJobType(reader.readIntegerAsEnum(Creature.JobType.class));
            jobPreference.setMoodChange(reader.readUnsignedShort());
            jobPreference.setManaChange(reader.readUnsignedShort());
            jobPreference.setChance(reader.readUnsignedByte());
            jobPreference.setX09(reader.readUnsignedByte());
            jobPreference.setX0a(reader.readUnsignedByte());
            jobPreference.setX0b(reader.readUnsignedByte());
            preferences[x] = jobPreference;
        }
        return preferences;
    }

    /**
     * Reads and parses an Light object from the current file location (24
     * bytes)
     *
     * @param reader the file stream to parse from
     * @return a Light
     */
    private Light readLight(IResourceChunkReader reader) {
        Light light = new Light();

        // Read the data
        light.setmKPos(reader.readIntegerAsFloat(),
                reader.readIntegerAsFloat(),
                reader.readIntegerAsFloat());
        light.setRadius(reader.readIntegerAsFloat());
        light.setFlags(reader.readIntegerAsFlag(Light.LightFlag.class));
        light.setColor(reader.readUnsignedByte(), reader.readUnsignedByte(),
                reader.readUnsignedByte(), reader.readUnsignedByte());

        return light;
    }

    /**
     * Reads the Objects.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readObjects(KwdHeader header, IResourceReader file) throws IOException {

        // Read the objects catalog
        if (objects == null) {
            logger.log(Level.INFO, "Reading objects!");
            objects = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides objects!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            GameObject object = new GameObject();

            object.setName(reader.readString(32).trim());
            object.setMeshResource(readArtResource(reader));
            object.setGuiIconResource(readArtResource(reader));
            object.setInHandIconResource(readArtResource(reader));
            object.setInHandMeshResource(readArtResource(reader));
            object.setkUnknownResource(readArtResource(reader));
            List<ArtResource> additionalResources = new ArrayList<>(4);
            for (int x = 0; x < 4; x++) {
                ArtResource resource = readArtResource(reader);
                if (resource != null) {
                    additionalResources.add(resource);
                }
            }
            object.setAdditionalResources(additionalResources);
            object.setLight(readLight(reader));
            object.setWidth(reader.readIntegerAsFloat());
            object.setHeight(reader.readIntegerAsFloat());
            object.setMass(reader.readIntegerAsFloat());
            object.setSpeed(reader.readIntegerAsFloat());
            object.setAirFriction(reader.readIntegerAsDouble());
            object.setMaterial(reader.readByteAsEnum(Material.class));
            short[] unknown3 = new short[3];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = reader.readUnsignedByte();
            }
            object.setUnknown3(unknown3);
            object.setFlags(reader.readIntegerAsFlag(GameObject.ObjectFlag.class));
            object.setHp(reader.readUnsignedShort());
            object.setMaxAngle(reader.readUnsignedShort());
            object.setX34c(reader.readUnsignedShort());
            object.setManaValue(reader.readUnsignedShort());
            object.setTooltipStringId(reader.readUnsignedShort());
            object.setNameStringId(reader.readUnsignedShort());
            object.setSlapEffectId(reader.readUnsignedShort());
            object.setDeathEffectId(reader.readUnsignedShort());
            object.setMiscEffectId(reader.readUnsignedShort());
            object.setObjectId(reader.readUnsignedByte());
            object.setStartState(reader.readByteAsEnum(GameObject.State.class));
            object.setRoomCapacity(reader.readUnsignedByte());
            object.setPickUpPriority(reader.readUnsignedByte());

            object.setSoundCategory(reader.readString(32).trim());

            // Add to the hash by the object ID
            objects.put(object.getObjectId(), object);

            // See special objects
            if (levelGem == null && object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_LEVEL_GEM)) {
                levelGem = object;
            }

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the CreatureSpells.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readCreatureSpells(KwdHeader header, IResourceReader file) throws IOException {

        // Read the creature spells catalog
        if (creatureSpells == null) {
            logger.log(Level.INFO, "Reading creature spells!");
            creatureSpells = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides creature spells!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            CreatureSpell creatureSpell = new CreatureSpell();

            creatureSpell.setName(reader.readString(32).trim());
            creatureSpell.setEditorIcon(readArtResource(reader));
            creatureSpell.setGuiIcon(readArtResource(reader));
            creatureSpell.setShotData1(reader.readUnsignedInteger());
            creatureSpell.setShotData2(reader.readUnsignedInteger());
            creatureSpell.setRange(reader.readIntegerAsFloat());
            creatureSpell.setFlags(reader.readIntegerAsFlag(CreatureSpell.CreatureSpellFlag.class));
            creatureSpell.setCombatPoints(reader.readUnsignedShort());
            creatureSpell.setSoundEvent(reader.readUnsignedShort());
            creatureSpell.setNameStringId(reader.readUnsignedShort());
            creatureSpell.setTooltipStringId(reader.readUnsignedShort());
            creatureSpell.setGeneralDescriptionStringId(reader.readUnsignedShort());
            creatureSpell.setStrengthStringId(reader.readUnsignedShort());
            creatureSpell.setWeaknessStringId(reader.readUnsignedShort());
            creatureSpell.setCreatureSpellId(reader.readUnsignedByte());
            creatureSpell.setShotTypeId(reader.readUnsignedByte());
            creatureSpell.setAlternativeShotId(reader.readUnsignedByte());
            creatureSpell.setAlternativeRoomId(reader.readUnsignedByte());
            creatureSpell.setRechargeTime(reader.readIntegerAsFloat());
            creatureSpell.setAlternativeShot(reader.readByteAsEnum(CreatureSpell.AlternativeShot.class));
            short[] data3 = new short[27];
            for (int x = 0; x < data3.length; x++) {
                data3[x] = reader.readUnsignedByte();
            }
            creatureSpell.setUnused(data3);

            // Add to the list
            creatureSpells.put(creatureSpell.getCreatureSpellId(), creatureSpell);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the EffectElements.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readEffectElements(KwdHeader header, IResourceReader file) throws IOException {

        // Read the effect elements catalog
        if (effectElements == null) {
            logger.log(Level.INFO, "Reading effect elements!");
            effectElements = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides effect elements!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            EffectElement effectElement = new EffectElement();

            effectElement.setName(reader.readString(32).trim());
            effectElement.setArtResource(readArtResource(reader));
            effectElement.setMass(reader.readIntegerAsFloat());
            effectElement.setAirFriction(reader.readIntegerAsDouble());
            effectElement.setElasticity(reader.readIntegerAsDouble());
            effectElement.setMinSpeedXy(reader.readIntegerAsFloat());
            effectElement.setMaxSpeedXy(reader.readIntegerAsFloat());
            effectElement.setMinSpeedYz(reader.readIntegerAsFloat());
            effectElement.setMaxSpeedYz(reader.readIntegerAsFloat());
            effectElement.setMinScale(reader.readIntegerAsFloat());
            effectElement.setMaxScale(reader.readIntegerAsFloat());
            effectElement.setScaleRatio(reader.readIntegerAsFloat());
            effectElement.setFlags(reader.readIntegerAsFlag(EffectElement.EffectElementFlag.class));
            effectElement.setEffectElementId(reader.readUnsignedShort());
            effectElement.setMinHp(reader.readUnsignedShort());
            effectElement.setMaxHp(reader.readUnsignedShort());
            effectElement.setDeathElementId(reader.readUnsignedShort());
            effectElement.setHitSolidElementId(reader.readUnsignedShort());
            effectElement.setHitWaterElementId(reader.readUnsignedShort());
            effectElement.setHitLavaElementId(reader.readUnsignedShort());
            effectElement.setColor(readColor(reader));
            effectElement.setRandomColorIndex(reader.readUnsignedByte());
            effectElement.setTableColorIndex(reader.readUnsignedByte());
            effectElement.setFadePercentage(reader.readUnsignedByte());
            effectElement.setNextEffectId(reader.readUnsignedShort());

            // Add to the hash by the effect element ID
            effectElements.put(effectElement.getEffectElementId(), effectElement);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the Effects.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readEffects(KwdHeader header, IResourceReader file) throws IOException {

        // Read the effects catalog
        if (effects == null) {
            logger.log(Level.INFO, "Reading effects!");
            effects = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides effects!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            Effect effect = new Effect();

            effect.setName(reader.readString(32).trim());
            effect.setArtResource(readArtResource(reader));
            effect.setLight(readLight(reader));
            effect.setMass(reader.readIntegerAsFloat());
            effect.setAirFriction(reader.readIntegerAsDouble());
            effect.setElasticity(reader.readIntegerAsDouble());
            effect.setRadius(reader.readIntegerAsFloat());
            effect.setMinSpeedXy(reader.readIntegerAsFloat());
            effect.setMaxSpeedXy(reader.readIntegerAsFloat());
            effect.setMinSpeedYz(reader.readIntegerAsFloat());
            effect.setMaxSpeedYz(reader.readIntegerAsFloat());
            effect.setMinScale(reader.readIntegerAsFloat());
            effect.setMaxScale(reader.readIntegerAsFloat());
            effect.setFlags(reader.readIntegerAsFlag(Effect.EffectFlag.class));
            effect.setEffectId(reader.readUnsignedShort());
            effect.setMinHp(reader.readUnsignedShort());
            effect.setMaxHp(reader.readUnsignedShort());
            effect.setFadeDuration(reader.readUnsignedShort());
            effect.setNextEffectId(reader.readUnsignedShort());
            effect.setDeathEffectId(reader.readUnsignedShort());
            effect.setHitSolidEffectId(reader.readUnsignedShort());
            effect.setHitWaterEffectId(reader.readUnsignedShort());
            effect.setHitLavaEffectId(reader.readUnsignedShort());
            List<Integer> generateIds = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                int id = reader.readUnsignedShort();
                if (id > 0) {
                    generateIds.add(id);
                }
            }
            effect.setGenerateIds(generateIds);
            effect.setOuterOriginRange(reader.readUnsignedShort());
            effect.setLowerHeightLimit(reader.readUnsignedShort());
            effect.setUpperHeightLimit(reader.readUnsignedShort());
            effect.setOrientationRange(reader.readUnsignedShort());
            effect.setSpriteSpinRateRange(reader.readUnsignedShort());
            effect.setWhirlpoolRate(reader.readUnsignedShort());
            effect.setDirectionalSpread(reader.readUnsignedShort());
            effect.setCircularPathRate(reader.readUnsignedShort());
            effect.setInnerOriginRange(reader.readUnsignedShort());
            effect.setGenerateRandomness(reader.readUnsignedShort());
            effect.setMisc2(reader.readUnsignedShort());
            effect.setMisc3(reader.readUnsignedShort());
            effect.setGenerationType(reader.readByteAsEnum(Effect.GenerationType.class));
            effect.setElementsPerTurn(reader.readUnsignedByte());
            effect.setUnknown3(reader.readUnsignedShort());

            // Add to the hash by the effect ID
            effects.put(effect.getEffectId(), effect);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the KeeperSpells.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readKeeperSpells(KwdHeader header, IResourceReader file) throws IOException {

        // Read the keeper spells catalog
        if (keeperSpells == null) {
            logger.log(Level.INFO, "Reading keeper spells!");
            keeperSpells = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides keeper spells!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();
            KeeperSpell keeperSpell = new KeeperSpell();

            keeperSpell.setName(reader.readString(32).trim());
            keeperSpell.setGuiIcon(readArtResource(reader));
            keeperSpell.setEditorIcon(readArtResource(reader));
            keeperSpell.setXc8(reader.readInteger());
            keeperSpell.setRechargeTime(reader.readIntegerAsFloat());
            keeperSpell.setShotData1(reader.readInteger());
            keeperSpell.setShotData2(reader.readInteger());
            keeperSpell.setResearchTime(reader.readUnsignedShort());
            keeperSpell.setTargetRule(reader.readByteAsEnum(KeeperSpell.TargetRule.class));
            keeperSpell.setOrderInEditor(reader.readUnsignedByte());
            keeperSpell.setFlags(reader.readIntegerAsFlag(KeeperSpell.KeeperSpellFlag.class));
            keeperSpell.setXe0Unreferenced(reader.readUnsignedShort());
            keeperSpell.setManaDrain(reader.readUnsignedShort());
            keeperSpell.setTooltipStringId(reader.readUnsignedShort());
            keeperSpell.setNameStringId(reader.readUnsignedShort());
            keeperSpell.setGeneralDescriptionStringId(reader.readUnsignedShort());
            keeperSpell.setStrengthStringId(reader.readUnsignedShort());
            keeperSpell.setWeaknessStringId(reader.readUnsignedShort());
            keeperSpell.setKeeperSpellId(reader.readUnsignedByte());
            keeperSpell.setCastRule(reader.readByteAsEnum(KeeperSpell.CastRule.class));
            keeperSpell.setShotTypeId(reader.readUnsignedByte());

            keeperSpell.setSoundCategory(reader.readString(32).trim());
            keeperSpell.setBonusRTime(reader.readUnsignedShort());
            keeperSpell.setBonusShotTypeId(reader.readUnsignedByte());
            keeperSpell.setBonusShotData1(reader.readInteger());
            keeperSpell.setBonusShotData2(reader.readInteger());
            keeperSpell.setManaCost(reader.readInteger());
            keeperSpell.setBonusIcon(readArtResource(reader));

            keeperSpell.setSoundCategoryGui(reader.readString(32).trim());
            keeperSpell.setHandAnimId(reader.readByteAsEnum(KeeperSpell.HandAnimId.class));
            keeperSpell.setNoGoHandAnimId(reader.readByteAsEnum(KeeperSpell.HandAnimId.class));

            // Add to the hash by the keeper spell ID
            keeperSpells.put(keeperSpell.getKeeperSpellId(), keeperSpell);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the *Things.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readThings(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested Things file
        if (thingsByType == null) {
            logger.log(Level.INFO, "Reading things!");
            thingsByType = HashMap.newHashMap(12);
        } else {
            logger.log(Level.WARNING, "Overrides things!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            Thing thing = null;
            int[] thingTag = new int[2];
            for (int x = 0; x < thingTag.length; x++) {
                thingTag[x] = reader.readUnsignedInteger();
            }
            long offset = reader.position();

            // Figure out the type
            switch (thingTag[0]) {
                case THING_OBJECT: {

                    // Object (door & trap crates, objects...)
                    thing = new Thing.Object();
                    ((Thing.Object) thing).setPosX(reader.readInteger());
                    ((Thing.Object) thing).setPosY(reader.readInteger());
                    short unknown1[] = new short[4];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = reader.readUnsignedByte();
                    }
                    ((Thing.Object) thing).setUnknown1(unknown1);
                    ((Thing.Object) thing).setKeeperSpellId(reader.readInteger());
                    ((Thing.Object) thing).setMoneyAmount(reader.readInteger());
                    ((Thing.Object) thing).setTriggerId(reader.readUnsignedShort());
                    ((Thing.Object) thing).setObjectId(reader.readUnsignedByte());
                    ((Thing.Object) thing).setPlayerId(reader.readUnsignedByte());

                    addThing((Thing.Object) thing);
                    break;
                }
                case THING_TRAP: {

                    // Trap
                    thing = new Thing.Trap();
                    ((Thing.Trap) thing).setPosX(reader.readInteger());
                    ((Thing.Trap) thing).setPosY(reader.readInteger());
                    ((Thing.Trap) thing).setUnknown1(reader.readInteger());
                    ((Thing.Trap) thing).setNumberOfShots(reader.readUnsignedByte());
                    ((Thing.Trap) thing).setTrapId(reader.readUnsignedByte());
                    ((Thing.Trap) thing).setPlayerId(reader.readUnsignedByte());
                    ((Thing.Trap) thing).setUnknown2(reader.readUnsignedByte());

                    addThing((Thing.Trap) thing);
                    break;
                }
                case THING_DOOR: {

                    // Door
                    thing = new Thing.Door();
                    ((Thing.Door) thing).setPosX(reader.readInteger());
                    ((Thing.Door) thing).setPosY(reader.readInteger());
                    ((Thing.Door) thing).setUnknown1(reader.readInteger());
                    ((Thing.Door) thing).setTriggerId(reader.readUnsignedShort());
                    ((Thing.Door) thing).setDoorId(reader.readUnsignedByte());
                    ((Thing.Door) thing).setPlayerId(reader.readUnsignedByte());
                    ((Thing.Door) thing).setFlag(reader.readByteAsEnum(Thing.Door.DoorFlag.class));
                    short unknown2[] = new short[3];
                    for (int x = 0; x < unknown2.length; x++) {
                        unknown2[x] = reader.readUnsignedByte();
                    }
                    ((Thing.Door) thing).setUnknown2(unknown2);

                    addThing((Thing.Door) thing);
                    break;
                }
                case THING_ACTION_POINT: {

                    // ActionPoint
                    thing = new ActionPoint();
                    ((ActionPoint) thing).setStartX(reader.readInteger());
                    ((ActionPoint) thing).setStartY(reader.readInteger());
                    ((ActionPoint) thing).setEndX(reader.readInteger());
                    ((ActionPoint) thing).setEndY(reader.readInteger());
                    ((ActionPoint) thing).setWaitDelay(reader.readUnsignedShort());
                    ((ActionPoint) thing).setFlags(reader.readShortAsFlag(ActionPointFlag.class));
                    ((ActionPoint) thing).setTriggerId(reader.readUnsignedShort());
                    ((ActionPoint) thing).setId(reader.readUnsignedByte());
                    ((ActionPoint) thing).setNextWaypointId(reader.readUnsignedByte());

                    ((ActionPoint) thing).setName(reader.readString(32).trim());

                    addThing((Thing.ActionPoint) thing);
                    break;
                }
                case THING_NEUTRAL_CREATURE: {

                    // Neutral creature
                    thing = new Thing.NeutralCreature();
                    ((NeutralCreature) thing).setPosX(reader.readInteger());
                    ((NeutralCreature) thing).setPosY(reader.readInteger());
                    ((NeutralCreature) thing).setPosZ(reader.readInteger());
                    ((NeutralCreature) thing).setGoldHeld(reader.readUnsignedShort());
                    ((NeutralCreature) thing).setLevel(reader.readUnsignedByte());
                    ((NeutralCreature) thing).setFlags(reader.readByteAsFlag(Thing.Creature.CreatureFlag.class));
                    ((NeutralCreature) thing).setInitialHealth(reader.readInteger());
                    ((NeutralCreature) thing).setTriggerId(reader.readUnsignedShort());
                    ((NeutralCreature) thing).setCreatureId(reader.readUnsignedByte());
                    ((NeutralCreature) thing).setUnknown1(reader.readUnsignedByte());

                    addThing((Thing.NeutralCreature) thing);
                    break;
                }
                case THING_GOOD_CREATURE: {

                    // Good creature
                    thing = new Thing.GoodCreature();
                    ((GoodCreature) thing).setPosX(reader.readInteger());
                    ((GoodCreature) thing).setPosY(reader.readInteger());
                    ((GoodCreature) thing).setPosZ(reader.readInteger());
                    ((GoodCreature) thing).setGoldHeld(reader.readUnsignedShort());
                    ((GoodCreature) thing).setLevel(reader.readUnsignedByte());
                    ((GoodCreature) thing).setFlags(reader.readByteAsFlag(Thing.Creature.CreatureFlag.class));
                    ((GoodCreature) thing).setObjectiveTargetActionPointId(reader.readInteger());
                    ((GoodCreature) thing).setInitialHealth(reader.readInteger());
                    ((GoodCreature) thing).setTriggerId(reader.readUnsignedShort());
                    ((GoodCreature) thing).setObjectiveTargetPlayerId(reader.readUnsignedByte());
                    ((GoodCreature) thing).setObjective(reader.readByteAsEnum(Thing.HeroParty.Objective.class));
                    ((GoodCreature) thing).setCreatureId(reader.readUnsignedByte());
                    short unknown1[] = new short[2];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = reader.readUnsignedByte();
                    }
                    ((GoodCreature) thing).setUnknown1(unknown1);
                    ((GoodCreature) thing).setFlags2(reader.readByteAsFlag(Thing.Creature.CreatureFlag2.class));

                    addThing((Thing.GoodCreature) thing);
                    break;
                }
                case THING_KEEPER_CREATURE: {

                    // Creature
                    thing = new Thing.KeeperCreature();
                    ((KeeperCreature) thing).setPosX(reader.readInteger());
                    ((KeeperCreature) thing).setPosY(reader.readInteger());
                    ((KeeperCreature) thing).setPosZ(reader.readInteger());
                    ((KeeperCreature) thing).setGoldHeld(reader.readUnsignedShort());
                    ((KeeperCreature) thing).setLevel(reader.readUnsignedByte());
                    ((KeeperCreature) thing).setFlags(reader.readByteAsFlag(KeeperCreature.CreatureFlag.class));
                    ((KeeperCreature) thing).setInitialHealth(reader.readInteger());
                    ((KeeperCreature) thing).setObjectiveTargetActionPointId(reader.readInteger());
                    ((KeeperCreature) thing).setTriggerId(reader.readUnsignedShort());
                    ((KeeperCreature) thing).setCreatureId(reader.readUnsignedByte());
                    ((KeeperCreature) thing).setPlayerId(reader.readUnsignedByte());

                    addThing((Thing.KeeperCreature) thing);
                    break;
                }
                case THING_HERO_PARTY: {

                    // HeroParty
                    thing = new HeroParty();

                    ((HeroParty) thing).setName(reader.readString(32).trim());
                    ((HeroParty) thing).setTriggerId(reader.readUnsignedShort());
                    ((HeroParty) thing).setId(reader.readUnsignedByte());
                    ((HeroParty) thing).setX23(reader.readInteger());
                    ((HeroParty) thing).setX27(reader.readInteger());
                    List<GoodCreature> heroPartyMembers = new ArrayList<>(16);
                    for (int x = 0; x < 16; x++) {
                        GoodCreature creature = new GoodCreature();
                        creature.setPosX(reader.readInteger());
                        creature.setPosY(reader.readInteger());
                        creature.setPosZ(reader.readInteger());
                        creature.setGoldHeld(reader.readUnsignedShort());
                        creature.setLevel(reader.readUnsignedByte());
                        creature.setFlags(reader.readByteAsFlag(KeeperCreature.CreatureFlag.class));
                        creature.setObjectiveTargetActionPointId(reader.readInteger());
                        creature.setInitialHealth(reader.readInteger());
                        creature.setTriggerId(reader.readUnsignedShort());
                        creature.setObjectiveTargetPlayerId(reader.readUnsignedByte());
                        creature.setObjective(reader.readByteAsEnum(Thing.HeroParty.Objective.class));
                        creature.setCreatureId(reader.readUnsignedByte());
                        short unknown1[] = new short[2];
                        for (int index = 0; index < unknown1.length; index++) {
                            unknown1[index] = reader.readUnsignedByte();
                        }
                        creature.setUnknown1(unknown1);
                        creature.setFlags2(reader.readByteAsFlag(Thing.Creature.CreatureFlag2.class));

                        // If creature id is 0, it is safe to say this is not a valid entry
                        if (creature.getCreatureId() > 0) {
                            heroPartyMembers.add(creature);
                        }
                    }
                    ((HeroParty) thing).setHeroPartyMembers(heroPartyMembers);

                    addThing((Thing.HeroParty) thing);
                    break;
                }
                case THING_DEAD_BODY: {

                    // Dead body
                    thing = new Thing.DeadBody();
                    ((Thing.DeadBody) thing).setPosX(reader.readInteger());
                    ((Thing.DeadBody) thing).setPosY(reader.readInteger());
                    ((Thing.DeadBody) thing).setPosZ(reader.readInteger());
                    ((Thing.DeadBody) thing).setGoldHeld(reader.readUnsignedShort());
                    ((Thing.DeadBody) thing).setCreatureId(reader.readUnsignedByte());
                    ((Thing.DeadBody) thing).setPlayerId(reader.readUnsignedByte());

                    addThing((Thing.DeadBody) thing);
                    break;
                }
                case THING_EFFECT_GENERATOR: {

                    // Effect generator
                    thing = new Thing.EffectGenerator();
                    ((Thing.EffectGenerator) thing).setPosX(reader.readInteger());
                    ((Thing.EffectGenerator) thing).setPosY(reader.readInteger());
                    ((Thing.EffectGenerator) thing).setX08(reader.readInteger());
                    ((Thing.EffectGenerator) thing).setX0c(reader.readInteger());
                    ((Thing.EffectGenerator) thing).setX10(reader.readUnsignedShort());
                    ((Thing.EffectGenerator) thing).setX12(reader.readUnsignedShort());
                    List<Integer> effectIds = new ArrayList<>(4);
                    for (int x = 0; x < 4; x++) {
                        int effectId = reader.readUnsignedShort();
                        if (effectId > 0) {
                            effectIds.add(effectId);
                        }
                    }
                    ((Thing.EffectGenerator) thing).setEffectIds(effectIds);
                    ((Thing.EffectGenerator) thing).setFrequency(reader.readUnsignedByte());
                    ((Thing.EffectGenerator) thing).setId(reader.readUnsignedByte());
                    short[] pad = new short[6];
                    for (int x = 0; x < pad.length; x++) {
                        pad[x] = reader.readUnsignedByte();
                    }
                    ((Thing.EffectGenerator) thing).setPad(pad);

                    addThing((Thing.EffectGenerator) thing);
                    break;
                }
                case THING_ROOM: {

                    // Room
                    thing = new Thing.Room();
                    ((Thing.Room) thing).setPosX(reader.readInteger());
                    ((Thing.Room) thing).setPosY(reader.readInteger());
                    ((Thing.Room) thing).setX08(reader.readInteger());
                    ((Thing.Room) thing).setX0c(reader.readUnsignedShort());
                    ((Thing.Room) thing).setDirection(reader.readByteAsEnum(Thing.Room.Direction.class));
                    ((Thing.Room) thing).setX0f(reader.readUnsignedByte());
                    ((Thing.Room) thing).setInitialHealth(reader.readUnsignedShort());
                    ((Thing.Room) thing).setRoomType(reader.readByteAsEnum(Thing.Room.RoomType.class));
                    ((Thing.Room) thing).setPlayerId(reader.readUnsignedByte());

                    addThing((Thing.Room) thing);
                    break;
                }
                case THING_CAMERA: {

                    // TODO: decode values
                    thing = new Thing.Camera();
                    ((Thing.Camera) thing).setPosition(reader.readIntegerAsFloat(),
                            reader.readIntegerAsFloat(),
                            reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setPositionMinClipExtent(reader.readIntegerAsFloat(),
                            reader.readIntegerAsFloat(),
                            reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setPositionMaxClipExtent(reader.readIntegerAsFloat(),
                            reader.readIntegerAsFloat(),
                            reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setViewDistanceValue(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setViewDistanceMin(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setViewDistanceMax(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setZoomValue(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setZoomValueMin(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setZoomValueMax(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setLensValue(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setLensValueMin(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setLensValueMax(reader.readIntegerAsFloat());
                    ((Thing.Camera) thing).setFlags(reader.readIntegerAsFlag(Thing.Camera.CameraFlag.class));
                    ((Thing.Camera) thing).setAngleYaw(reader.readUnsignedShort());
                    ((Thing.Camera) thing).setAngleRoll(reader.readUnsignedShort());
                    ((Thing.Camera) thing).setAnglePitch(reader.readUnsignedShort());
                    ((Thing.Camera) thing).setId((short) reader.readUnsignedShort());

                    addThing(thing);
                    break;
                }
                default: {

                    // Just skip the bytes
                    reader.skipBytes(thingTag[1]);
                    logger.log(Level.WARNING, "Unsupported thing type {0}!", thingTag[0]);
                }
            }

            // Check reader offset
            checkOffset(thingTag[1], reader, offset);
        }
    }

    private <T extends Thing> void addThing(T thing) {
        List<T> thingList = (List<T>) thingsByType.get(thing.getClass());
        if (thingList == null) {
            thingList = new ArrayList<>();
            thingsByType.put(thing.getClass(), thingList);
        }
        thingList.add(thing);
    }

    /**
     * Reads the Shots.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readShots(KwdHeader header, IResourceReader file) throws IOException {

        // Read the shots catalog
        if (shots == null) {
            logger.log(Level.INFO, "Reading shots!");
            shots = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides shots!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = reader.position();

            // One shot is 239 bytes
            Shot shot = new Shot();

            shot.setName(reader.readString(32).trim());
            shot.setMeshResource(readArtResource(reader));
            shot.setLight(readLight(reader));
            shot.setAirFriction(reader.readIntegerAsDouble());
            shot.setMass(reader.readIntegerAsFloat());
            shot.setSpeed(reader.readIntegerAsFloat());
            shot.setData1(reader.readUnsignedInteger());
            shot.setData2(reader.readUnsignedInteger());
            shot.setShotProcessFlags(reader.readIntegerAsFlag(Shot.ShotProcessFlag.class));
            shot.setRadius(reader.readIntegerAsFloat());
            shot.setFlags(reader.readIntegerAsFlag(Shot.ShotFlag.class));
            shot.setGeneralEffectId(reader.readUnsignedShort());
            shot.setCreationEffectId(reader.readUnsignedShort());
            shot.setDeathEffectId(reader.readUnsignedShort());
            shot.setTimedEffectId(reader.readUnsignedShort());
            shot.setHitSolidEffectId(reader.readUnsignedShort());
            shot.setHitLavaEffectId(reader.readUnsignedShort());
            shot.setHitWaterEffect(reader.readUnsignedShort());
            shot.setHitThingEffectId(reader.readUnsignedShort());
            shot.setHealth(reader.readUnsignedShort());
            shot.setShotId(reader.readUnsignedByte());
            shot.setDeathShotId(reader.readUnsignedByte());
            shot.setTimedDelay(reader.readUnsignedByte());
            shot.setHitSolidShotId(reader.readUnsignedByte());
            shot.setHitLavaShotId(reader.readUnsignedByte());
            shot.setHitWaterShotId(reader.readUnsignedByte());
            shot.setHitThingShotId(reader.readUnsignedByte());
            shot.setDamageType(reader.readByteAsEnum(Shot.DamageType.class));
            shot.setCollideType(reader.readByteAsEnum(Shot.CollideType.class));
            shot.setProcessType(reader.readByteAsEnum(Shot.ProcessType.class));
            shot.setAttackCategory(reader.readByteAsEnum(Shot.AttackCategory.class));

            shot.setSoundCategory(reader.readString(32).trim());
            shot.setThreat(reader.readUnsignedShort());
            shot.setBurnDuration(reader.readIntegerAsFloat());

            // Add to the hash by the shot ID
            shots.put(shot.getShotId(), shot);

            // Check reader offset
            checkOffset(header, reader, offset);
        }
    }

    /**
     * Reads the *Triggers.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readTriggers(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested Triggers file
        if (triggers == null) {
            logger.log(Level.INFO, "Reading triggers!");
            triggers = HashMap.newHashMap(header.getItemCount());
        } else {
            logger.log(Level.WARNING, "Overrides triggers!");
        }

        IResourceChunkReader reader = file.readChunk((int) header.size - header.headerSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            Trigger trigger = null;
            int[] triggerTag = new int[2];
            for (int x = 0; x < triggerTag.length; x++) {
                triggerTag[x] = reader.readUnsignedInteger();
            }
            long offset = reader.position();

            // Figure out the type
            switch (triggerTag[0]) {
                case TRIGGER_GENERIC: {
                    reader.mark();
                    reader.skipBytes(triggerTag[1] - 2);

                    trigger = new TriggerGeneric(this);
                    ((TriggerGeneric) trigger).setType(reader.readByteAsEnum(TriggerGeneric.TargetType.class));
                    trigger.setRepeatTimes(reader.readUnsignedByte());

                    reader.reset();
                    switch (((TriggerGeneric) trigger).getType()) {
                        case AP_CONGREGATE_IN:
                        case AP_POSESSED_CREATURE_ENTERS:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // creatureId, objectId
                            trigger.setUserData("targetType", reader.readUnsignedByte()); // 3 = Creature, 6 = Object
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case AP_SLAB_TYPES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("terrainId", reader.readUnsignedByte());
                            reader.readAndCheckNull(1); // reader.skipBytes(1); // 0 = None
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case AP_TAG_PART_OF:
                        case AP_TAG_ALL_OF:
                        case AP_CLAIM_PART_OF:
                        case AP_CLAIM_ALL_OF:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            // trigger.setUserData("targetId", reader.readUnsignedByte()); // 0 = None
                            // trigger.setUserData("targetType", reader.readUnsignedByte()); // 0 = None
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PLAYER_DUNGEON_BREACHED:
                        case PLAYER_ENEMY_BREACHED:
                            trigger.setUserData("playerId", reader.readUnsignedByte()); // 0 = Any
                            reader.readAndCheckNull(7); // reader.skipBytes(7);
                            break;

                        case PLAYER_KILLED:
                            trigger.setUserData("playerId", reader.readUnsignedByte()); // 0 = Any
                            reader.readAndCheckNull(3); // reader.skipBytes(7);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // FIXME unknown value
                            break;

                        case PLAYER_CREATURE_PICKED_UP:
                        case PLAYER_CREATURE_SLAPPED:
                        case PLAYER_CREATURE_SACKED:
                            trigger.setUserData("creatureId", reader.readUnsignedByte()); // 0 = Any
                            reader.readAndCheckNull(7); // reader.skipBytes(7);
                            break;

                        case PLAYER_CREATURE_DROPPED:
                            trigger.setUserData("creatureId", reader.readUnsignedByte()); // 0 = Any
                            trigger.setUserData("roomId", reader.readUnsignedByte()); // 0 = Any
                            reader.readAndCheckNull(6); // reader.skipBytes(6);
                            break;

                        case PLAYER_CREATURES:
                        case PLAYER_HAPPY_CREATURES:
                        case PLAYER_ANGRY_CREATURES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("creatureId", reader.readUnsignedByte());
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PLAYER_CREATURES_KILLED:
                        case PLAYER_KILLS_CREATURES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // playerId
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PLAYER_ROOMS:
                        case PLAYER_ROOM_SLABS:
                        case PLAYER_ROOM_SIZE:
                        case PLAYER_ROOM_FURNITURE:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("roomId", reader.readUnsignedByte());
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PLAYER_DOORS:
                        case PLAYER_TRAPS:
                        case PLAYER_KEEPER_SPELL:
                        case PLAYER_DESTROYS:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // doorId, trapId, keeperSpellId,
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PLAYER_SLAPS:
                        case PLAYER_GOLD:
                        case PLAYER_GOLD_MINED:
                        case PLAYER_MANA:
                        case PLAYER_CREATURES_GROUPED:
                        case PLAYER_CREATURES_DYING:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            reader.readAndCheckNull(1); // reader.skipBytes(1);
                            // trigger.setUserData("targetId", reader.readUnsignedByte()); // = 0
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PLAYER_CREATURES_AT_LEVEL:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            // FIXME some bug in editor
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // = 0, must be a level
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", reader.readUnsignedByte()); // level also
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case LEVEL_PAY_DAY:
                        case CREATURE_KILLED:
                        case CREATURE_SLAPPED:
                        case CREATURE_ATTACKED:
                        case CREATURE_IMPRISONED:
                        case CREATURE_TORTURED:
                        case CREATURE_CONVERTED:
                        case CREATURE_CLAIMED:
                        case CREATURE_ANGRY:
                        case CREATURE_AFRAID:
                        case CREATURE_STEALS:
                        case CREATURE_LEAVES:
                        case CREATURE_STUNNED:
                        case CREATURE_DYING:
                        case GUI_TRANSITION_ENDS:
                        case CREATURE_PICKED_UP:
                        case CREATURE_SACKED:
                        case CREATURE_PICKS_UP_PORTAL_GEM:
                        case CREATURE_HUNGER_SATED:
                        case PARTY_CREATED:
                            reader.readAndCheckNull(8); // reader.skipBytes(8);
                            break;

                        case CREATURE_CREATED:
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // FIXME unknown value
                            break;

                        case LEVEL_PLAYED:
                        case PARTY_MEMBERS_CAPTURED:
                        case CREATURE_EXPERIENCE_LEVEL:
                        case CREATURE_GOLD_HELD:
                        case CREATURE_HEALTH:
                        case LEVEL_TIME:
                        case LEVEL_CREATURES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case PARTY_MEMBERS_KILLED:
                        case PARTY_MEMBERS_INCAPACITATED:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("unknown", reader.readUnsignedByte()); // FIXME unknown value
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case GUI_BUTTON_PRESSED:
                            // Misc Button = 0, Room = 1, Creature = 2, Door = 3, Trap = 4, Keeper Spell = 5
                            trigger.setUserData("targetType", reader.readUnsignedByte());
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // buttonId, roomId, creatureId ...
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // FIXME unknown value
                            break;

                        case FLAG:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // flagId
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Flag
                            trigger.setUserData("flagId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        case TIMER:
                            ((TriggerGeneric) trigger).setTargetValueComparison(reader.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", reader.readUnsignedByte()); // timerId
                            trigger.setUserData("flag", reader.readUnsignedByte()); // 0x1 = Value, !0x1 = Flag
                            trigger.setUserData("timerId", reader.readUnsignedByte());
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            break;

                        default:
                            reader.readAndCheckNull(8); // reader.skipBytes(8);
                            logger.log(Level.WARNING, "Unsupported Type of TriggerGeneric");
                            break;

                    }

                    trigger.setId(reader.readUnsignedShort());
                    trigger.setIdNext(reader.readUnsignedShort()); // SiblingID
                    trigger.setIdChild(reader.readUnsignedShort()); // ChildID

                    reader.skipBytes(2);
                    break;
                }
                case TRIGGER_ACTION: {

                    reader.mark();
                    reader.skipBytes(triggerTag[1] - 2);

                    trigger = new TriggerAction(this);
                    ((TriggerAction) trigger).setType(reader.readByteAsEnum(TriggerAction.ActionType.class));
                    trigger.setRepeatTimes(reader.readUnsignedByte());

                    reader.reset();
                    switch (((TriggerAction) trigger).getType()) {
                        // in levels triggers
                        case ALTER_TERRAIN_TYPE:
                            trigger.setUserData("terrainId", reader.readUnsignedByte());
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("posX", reader.readUnsignedShort());
                            trigger.setUserData("posY", reader.readUnsignedShort());
                            break;

                        case COLLAPSE_HERO_GATE:
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            trigger.setUserData("posX", reader.readUnsignedShort());
                            trigger.setUserData("posY", reader.readUnsignedShort());
                            break;

                        case CHANGE_ROOM_OWNER:
                            reader.readAndCheckNull(1); // reader.skipBytes(1);
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("posX", reader.readUnsignedShort());
                            trigger.setUserData("posY", reader.readUnsignedShort());
                            break;

                        case SET_ALLIANCE:
                            trigger.setUserData("playerOneId", reader.readUnsignedByte());
                            trigger.setUserData("playerTwoId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Create, !0 = Break
                            reader.readAndCheckNull(5); // reader.skipBytes(5);
                            break;

                        case SET_CREATURE_MOODS:
                        case SET_SYSTEM_MESSAGES:
                        case SET_TIMER_SPEECH:
                        case SET_WIDESCREEN_MODE:
                        case ALTER_SPEED:  // 0 = Walk, !0 = Run
                        case SET_FIGHT_FLAG: // 0 = Don`t Fight, !0 = Fight
                        case SET_PORTAL_STATUS: // 0 = Closed, !0 = Open
                            trigger.setUserData("available", reader.readUnsignedByte());  // 0 = Off, !0 = On
                            reader.readAndCheckNull(7); // reader.skipBytes(7);
                            break;

                        case SET_SLAPS_LIMIT:
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // limit 4 bytes, 0 = Off
                            break;

                        case INITIALIZE_TIMER:
                            trigger.setUserData("timerId", reader.readUnsignedByte()); // timerId + 1, 16 - Time Limit
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // limit 4 bytes, only for Time limit (max 100 s)
                            break;

                        case FLAG:
                            trigger.setUserData("flagId", reader.readUnsignedByte()); // flagId + 1, 128 - level score
                            trigger.setUserData("flag", reader.readUnsignedByte()); // flag = Equal = 12 | Plus = 20 | Minus = 36
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // limit 4 bytes
                            break;

                        case MAKE:
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("type", reader.readUnsignedByte()); // type = TriggerAction.MakeType.
                            trigger.setUserData("targetId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Unavailable, !0 = Available
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            break;
                        // in player triggers
                        case DISPLAY_SLAB_OWNER:
                            // FIXME Show wrong values in editor
                            trigger.setUserData("available", reader.readUnsignedByte());  // 0 = Off, !0 = On
                            //((TriggerAction) trigger).setActionTargetValue1(ConversionUtils.toUnsignedInteger(reader)); // limit 4 bytes
                            // 1635984
                            reader.readAndCheckNull(7); // reader.skipBytes(7);
                            break;

                        case DISPLAY_NEXT_ROOM_TYPE: // 0 = Off or roomId
                        case MAKE_OBJECTIVE: // 0 = Off, 1 = Kill, 2 = Imprison, 3 = Convert
                        case ZOOM_TO_ACTION_POINT: // actionPointId
                            trigger.setUserData("targetId", reader.readUnsignedByte());
                            reader.readAndCheckNull(7); // reader.skipBytes(7);
                            break;

                        case DISPLAY_OBJECTIVE:
                            trigger.setUserData("objectiveId", reader.readUnsignedInteger()); // objectiveId, limit 32767
                            trigger.setUserData("actionPointId", reader.readUnsignedByte()); // if != 0 => Zoom To AP = this
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            break;

                        case PLAY_SPEECH:
                            trigger.setUserData("speechId", reader.readUnsignedInteger()); // speechId, limit 32767
                            trigger.setUserData("text", reader.readUnsignedByte()); // 0 = Show Text, !0 = Without text
                            trigger.setUserData("introduction", reader.readUnsignedByte()); // 0 = No Introduction, !0 = Introduction
                            trigger.setUserData("pathId", reader.readUnsignedShort()); // pathId
                            break;

                        case DISPLAY_TEXT_STRING:
                            trigger.setUserData("textId", reader.readUnsignedInteger()); // textId, limit 32767
                            // FIXME Maybe Zoom to AP X
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            break;
                        // creature triggers
                        case ATTACH_PORTAL_GEM:
                        case MAKE_HUNGRY:
                        case REMOVE_FROM_MAP:
                        case ZOOM_TO:
                        case WIN_GAME:
                        case LOSE_GAME:
                        case FORCE_FIRST_PERSON:
                        case LOSE_SUBOBJECTIVE:
                        case WIN_SUBOBJECTIVE:
                            reader.readAndCheckNull(8); // reader.skipBytes(8); // no other parameters
                            break;

                        case SET_MUSIC_LEVEL: // level
                        case SHOW_HEALTH_FLOWER: // limit Seconds
                            trigger.setUserData("value", reader.readUnsignedInteger());
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            break;

                        case SET_TIME_LIMIT:
                            trigger.setUserData("timerId", reader.readUnsignedByte()); // timerId + 1, 16 - Time Limit
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // Seconds
                            break;

                        case FOLLOW_CAMERA_PATH:
                            trigger.setUserData("pathId", reader.readUnsignedByte());
                            trigger.setUserData("actionPointId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Show Ceiling, !0 = Hide Ceiling
                            reader.readAndCheckNull(5); // reader.skipBytes(5);
                            break;

                        case FLASH_BUTTON:
                            trigger.setUserData("type", reader.readUnsignedByte()); // TriggerAction.MakeType.
                            trigger.setUserData("targetId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Off, !0 & !time = Until selected
                            reader.readAndCheckNull(1); // reader.skipBytes(1);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // Seconds
                            break;

                        case FLASH_ACTION_POINT:
                            trigger.setUserData("actionPointId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Off, !0 & !time = Until switched off
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("value", reader.readUnsignedInteger()); // Seconds
                            break;

                        case REVEAL_ACTION_POINT:
                            trigger.setUserData("actionPointId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Reveal, !0 = Conceal
                            reader.readAndCheckNull(6); // reader.skipBytes(6);
                            break;

                        case ROTATE_AROUND_ACTION_POINT:
                            trigger.setUserData("actionPointId", reader.readUnsignedByte());
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Relative, !0 = Absolute
                            trigger.setUserData("angle", reader.readUnsignedShort()); // degrees
                            trigger.setUserData("time", reader.readUnsignedInteger()); // seconds
                            break;

                        case CREATE_CREATURE:
                            trigger.setUserData("creatureId", reader.readUnsignedByte());
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("level", reader.readUnsignedByte());
                            trigger.setUserData("flag", reader.readUnsignedByte()); // TriggerAction.CreatureFlag.
                            trigger.setUserData("posX", reader.readUnsignedShort());
                            trigger.setUserData("posY", reader.readUnsignedShort());
                            break;

                        case SET_OBJECTIVE:
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            trigger.setUserData("type", reader.readUnsignedByte()); // Creature.JobType
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("actionPointId", reader.readUnsignedInteger()); // for type = SEND_TO_ACTION_POINT
                            break;

                        case CREATE_HERO_PARTY:
                            trigger.setUserData("partyId", reader.readUnsignedByte()); // partyId + 1
                            trigger.setUserData("type", reader.readUnsignedByte()); // 0 = None, 1 = IP, 2 = IP Random
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("actionPointId", reader.readUnsignedByte()); //
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            break;

                        case TOGGLE_EFFECT_GENERATOR:
                            trigger.setUserData("generatorId", reader.readUnsignedByte()); // generatorId + 1
                            trigger.setUserData("available", reader.readUnsignedByte()); // 0 = Off, !0 = On
                            reader.readAndCheckNull(6); // reader.skipBytes(6);
                            break;

                        case GENERATE_CREATURE:
                            trigger.setUserData("creatureId", reader.readUnsignedByte()); // creatureId + 1
                            trigger.setUserData("level", reader.readUnsignedByte());
                            reader.readAndCheckNull(6); // reader.skipBytes(6);
                            break;

                        case INFORMATION:
                            trigger.setUserData("informationId", reader.readUnsignedInteger());
                            trigger.setUserData("actionPointId", reader.readUnsignedByte());
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            break;

                        case SEND_TO_AP:
                            reader.readAndCheckNull(4); // reader.skipBytes(4);
                            trigger.setUserData("actionPointId", reader.readUnsignedByte());
                            reader.readAndCheckNull(3); // reader.skipBytes(3);
                            break;

                        case CREATE_PORTAL_GEM:
                            trigger.setUserData("objectId", reader.readUnsignedByte());
                            trigger.setUserData("playerId", reader.readUnsignedByte());
                            reader.readAndCheckNull(2); // reader.skipBytes(2);
                            trigger.setUserData("posX", reader.readUnsignedShort()); // posX + 1
                            trigger.setUserData("posY", reader.readUnsignedShort()); // posY + 1
                            break;

                        default:
                            reader.readAndCheckNull(8); // reader.skipBytes(8);
                            logger.log(Level.WARNING, "Unsupported Type of TriggerAction");
                            break;
                    }

                    trigger.setId(reader.readUnsignedShort()); // ID
                    trigger.setIdNext(reader.readUnsignedShort()); // SiblingID
                    trigger.setIdChild(reader.readUnsignedShort()); // ChildID

                    reader.skipBytes(2);
                    break;
                }
                default: {

                    // Just skip the bytes
                    reader.skipBytes(triggerTag[1]);
                    logger.log(Level.WARNING, "Unsupported trigger type {0}!", triggerTag[0]);
                }
            }

            // Add to the list
            if (trigger != null) {
                triggers.put(trigger.getId(), trigger);
            }

            // Check reader offset
            checkOffset(triggerTag[1], reader, offset);
        }
    }

    /**
     * Reads the *Variables.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readVariables(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested VARIABLES file
        // Should be the GlobalVariables first, then the level's own
        if (variables == null) {
            logger.log(Level.INFO, "Reading variables!");
            availabilities = new ArrayList<>();
            creaturePools = HashMap.newHashMap(4);
            creatureStatistics = HashMap.newHashMap(10);
            creatureFirstPersonStatistics = HashMap.newHashMap(10);
            variables = new HashMap<>();
            sacrifices = new HashSet<>();
            playerAlliances = new HashSet<>();
            unknownVariables = new HashSet<>();
        } else {
            logger.log(Level.INFO, "Overrides variables!");
        }

        IResourceChunkReader reader = file.readChunk(header.dataSize);
        for (int i = 0; i < header.getItemCount(); i++) {
            if (!reader.hasRemaining()) {
                logger.log(Level.WARNING, "Variables end prematurely!");
                break;
            }
            int id = reader.readInteger();

            switch (id) {
                case Variable.CREATURE_POOL:
                    Variable.CreaturePool creaturePool = new Variable.CreaturePool();
                    creaturePool.setCreatureId(reader.readInteger());
                    creaturePool.setValue(reader.readInteger());
                    creaturePool.setPlayerId(reader.readInteger());

                    // Add
                    Map<Integer, CreaturePool> playerCreaturePool = creaturePools.get(creaturePool.getPlayerId());
                    if (playerCreaturePool == null) {
                        playerCreaturePool = HashMap.newHashMap(12);
                        creaturePools.put(creaturePool.getPlayerId(), playerCreaturePool);
                    }
                    playerCreaturePool.put(creaturePool.getCreatureId(), creaturePool);
                    break;

                case Variable.AVAILABILITY:
                    Variable.Availability availability = new Variable.Availability();
                    availability.setType(reader.readShortAsEnum(Variable.Availability.AvailabilityType.class));
                    availability.setPlayerId(reader.readUnsignedShort());
                    availability.setTypeId(reader.readInteger());
                    availability.setValue(reader.readIntegerAsEnum(Variable.Availability.AvailabilityValue.class));

                    // Add
                    availabilities.add(availability);
                    break;

                case Variable.SACRIFICES_ID: // not changeable (in editor you can, but changes will not save)
                    Variable.Sacrifice sacrifice = new Variable.Sacrifice();
                    sacrifice.setType1(reader.readByteAsEnum(Variable.SacrificeType.class));
                    sacrifice.setId1(reader.readUnsignedByte());
                    sacrifice.setType2(reader.readByteAsEnum(Variable.SacrificeType.class));
                    sacrifice.setId2(reader.readUnsignedByte());
                    sacrifice.setType3(reader.readByteAsEnum(Variable.SacrificeType.class));
                    sacrifice.setId3(reader.readUnsignedByte());

                    sacrifice.setRewardType(reader.readByteAsEnum(Variable.SacrificeRewardType.class));
                    sacrifice.setSpeechId(reader.readUnsignedByte());
                    sacrifice.setRewardValue(reader.readInteger());

                    // Add
                    sacrifices.add(sacrifice);
                    break;

                case Variable.CREATURE_STATS_ID:
                    Variable.CreatureStats creatureStats = new Variable.CreatureStats();
                    creatureStats.setStatId(reader.readIntegerAsEnum(Variable.CreatureStats.StatType.class));
                    creatureStats.setValue(reader.readInteger());
                    creatureStats.setLevel(reader.readInteger());

                    // Add
                    Map<StatType, CreatureStats> stats = creatureStatistics.get(creatureStats.getLevel());
                    if (stats == null) {
                        stats = HashMap.newHashMap(CreatureStats.StatType.values().length);
                        creatureStatistics.put(creatureStats.getLevel(), stats);
                    }
                    stats.put(creatureStats.getStatId(), creatureStats);
                    break;

                case Variable.CREATURE_FIRST_PERSON_ID:
                    Variable.CreatureFirstPerson creatureFirstPerson = new Variable.CreatureFirstPerson();
                    creatureFirstPerson.setStatId(reader.readIntegerAsEnum(Variable.CreatureStats.StatType.class));
                    creatureFirstPerson.setValue(reader.readInteger());
                    creatureFirstPerson.setLevel(reader.readInteger());

                    // Add
                    Map<StatType, CreatureFirstPerson> firstPersonStats = creatureFirstPersonStatistics.get(creatureFirstPerson.getLevel());
                    if (firstPersonStats == null) {
                        firstPersonStats = HashMap.newHashMap(CreatureStats.StatType.values().length);
                        creatureFirstPersonStatistics.put(creatureFirstPerson.getLevel(), firstPersonStats);
                    }
                    firstPersonStats.put(creatureFirstPerson.getStatId(), creatureFirstPerson);
                    break;

                case Variable.PLAYER_ALLIANCE:
                    Variable.PlayerAlliance playerAlliance = new Variable.PlayerAlliance();
                    playerAlliance.setPlayerIdOne(reader.readInteger());
                    playerAlliance.setPlayerIdTwo(reader.readInteger());
                    playerAlliance.setUnknown1(reader.readInteger());

                    // Add
                    playerAlliances.add(playerAlliance);
                    break;

                case Variable.UNKNOWN_17: // FIXME unknown value
                case Variable.UNKNOWN_0: // FIXME unknownn value
                case Variable.UNKNOWN_77: // FIXME unknown value
                    Variable.Unknown unknown = new Variable.Unknown();
                    unknown.setVariableId(id);
                    unknown.setValue(reader.readInteger());
                    unknown.setUnknown1(reader.readInteger());
                    unknown.setUnknown2(reader.readInteger());

                    // Add
                    unknownVariables.add(unknown);
                    break;

                default:
                    Variable.MiscVariable miscVariable = new Variable.MiscVariable();
                    miscVariable.setVariableId(ConversionUtils.parseEnum(id,
                            Variable.MiscVariable.MiscType.class));
                    miscVariable.setValue(reader.readInteger());
                    miscVariable.setUnknown1(reader.readInteger());
                    miscVariable.setUnknown2(reader.readInteger());

                    // Add
                    variables.put(miscVariable.getVariableId(), miscVariable);
                    break;
            }
        }
    }

    /**
     * Get list of different terrain tiles
     *
     * @return list of terrain tiles
     */
    public Collection<Terrain> getTerrainList() {
        return terrainTiles.values();
    }

    /**
     * Get list of different objects
     *
     * @return list of objects
     */
    public Collection<GameObject> getObjectList() {
        return objects.values();
    }

    /**
     * Get list of different creatures
     *
     * @return list of creatures
     */
    public Collection<Creature> getCreatureList() {
        return creatures.values();
    }

    /**
     * Get the player with the specified ID
     *
     * @param id the id of player
     * @return the player
     */
    public Player getPlayer(short id) {
        return players.get(id);
    }

    public Map<Short, Player> getPlayers() {
        return players;
    }

    /**
     * Get the creature with the specified ID
     *
     * @param id the id of creature
     * @return the creature
     */
    public Creature getCreature(short id) {
        return creatures.get(id);
    }

    /**
     * Bridges are a bit special, identifies one and returns the terrain that
     * should be under it
     *
     * @param type tile BridgeTerrainType
     * @param terrain the terrain tile
     * @return returns null if this is not a bridge, otherwise returns pretty
     * much either water or lava
     */
    public Terrain getTerrainBridge(Tile.BridgeTerrainType type, Terrain terrain) {
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            Room room = getRoomByTerrain(terrain.getTerrainId());
            return getTerrainBridge(type, room);
        }

        return null;
    }

    public Terrain getTerrainBridge(Tile.BridgeTerrainType type, Room room) {
        // Swap the terrain if this is a bridge
        if (room != null && !room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
            // It is a bridge
            switch (type) {
                case WATER:
                    return getMap().getWater();
                case LAVA:
                    return getMap().getLava();
            }
        }

        return null;
    }

    /**
     * Get the terrain with the specified ID
     *
     * @param id the id of terrain
     * @return the terrain
     */
    public Terrain getTerrain(short id) {
        return terrainTiles.get(id);
    }

    /**
     * Get the room with the specified terrain ID
     *
     * @param id the id of terrain
     * @return the room associated with the terrain ID
     */
    public Room getRoomByTerrain(short id) {
        return roomsByTerrainId.get(id);
    }

    /**
     * Get list of things by certain type
     *
     * @param <T> the instance type of the things you want
     * @param thingClass the class of things you want
     * @return things list of things you want
     */
    public <T extends Thing> List<T> getThings(Class<T> thingClass) {
        List<T> result = (List<T>) thingsByType.get(thingClass);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    /**
     * Get the trigger/action with the specified ID
     *
     * @param id the id of trigger/action
     * @return the trigger/action
     */
    public Trigger getTrigger(int id) {
        return triggers.get(id);
    }

    public Map<Integer, Trigger> getTriggers() {
        return triggers;
    }

    /**
     * Get the object with the specified ID
     *
     * @param id the id of object
     * @return the object
     */
    public GameObject getObject(int id) {
        return objects.get((short) id);
    }

    /**
     * Get the room with the specified ID
     *
     * @param id the id of room
     * @return the room
     */
    public Room getRoomById(int id) {
        return rooms.get((short) id);
    }

    /**
     * Get the keeper spell with the specified ID
     *
     * @param id the id of keeper spell
     * @return the keeper spell
     */
    public KeeperSpell getKeeperSpellById(int id) {
        return keeperSpells.get((short) id);
    }

    /**
     * Get the trap with the specified ID
     *
     * @param id the id of trap
     * @return the trap
     */
    public Trap getTrapById(int id) {
        return traps.get((short) id);
    }

    /**
     * Get the door with the specified ID
     *
     * @param id the id of door
     * @return the door
     */
    public Door getDoorById(int id) {
        return doors.get((short) id);
    }

    /**
     * Get the list of all rooms
     *
     * @return list of all rooms
     */
    public List<Room> getRooms() {
        List<Room> c = new ArrayList(rooms.values());
        Collections.sort(c);
        return c;
    }

    /**
     * Get the list of all keeper spells
     *
     * @return list of all keeper spells
     */
    public List<KeeperSpell> getKeeperSpells() {
        List<KeeperSpell> c = new ArrayList(keeperSpells.values());
        Collections.sort(c);
        return c;
    }

    /**
     * Get the list of all doors
     *
     * @return list of all doors
     */
    public List<Door> getDoors() {
        List<Door> c = new ArrayList(doors.values());
        Collections.sort(c);
        return c;
    }

    /**
     * Get the list of all shots
     *
     * @return list of all shots
     */
    public List<Shot> getShots() {
        List<Shot> c = new ArrayList(shots.values());
        Collections.sort(c);
        return c;
    }

    public Shot getShotById(short shotId) {
        return shots.get(shotId);
    }

    public GameMap getMap() {
        return map;
    }

    /**
     * Get the list of all traps
     *
     * @return list of all traps
     */
    public List<Trap> getTraps() {
        List<Trap> c = new ArrayList(traps.values());
        Collections.sort(c);
        return c;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public CreatureSpell getCreatureSpellById(short spellId) {
        return creatureSpells.get(spellId);
    }

    public Effect getEffect(int effectId) {
        return effects.get(effectId);
    }

    public Map<Integer, Effect> getEffects() {
        return effects;
    }

    public EffectElement getEffectElement(int effectElementId) {
        return effectElements.get(effectElementId);
    }

    public Map<Integer, EffectElement> getEffectElements() {
        return effectElements;
    }

    public Map<MiscVariable.MiscType, MiscVariable> getVariables() {
        return variables;
    }

    public List<Availability> getAvailabilities() {
        return availabilities;
    }

    public Set<PlayerAlliance> getPlayerAlliances() {
        return playerAlliances;
    }

    /**
     * Get player specific creature pool
     *
     * @param playerId the player id
     * @return the creature pool
     */
    public Map<Integer, CreaturePool> getCreaturePool(short playerId) {
        return creaturePools.get(Short.valueOf(playerId).intValue());
    }

    public Creature getImp() {
        return imp;
    }

    public Creature getDwarf() {
        return dwarf;
    }

    public Room getPortal() {
        return getRoomById(ROOM_PORTAL_ID);
    }

    public Room getDungeonHeart() {
        return getRoomById(ROOM_DUNGEON_HEART_ID);
    }

    public GameObject getLevelGem() {
        return levelGem;
    }

    /**
     * Get the creature stats by level. There might not be a record for every
     * level. Then should just default to 100% stat.
     *
     * @param level the creature level
     * @return the creature stats on given level
     */
    public Map<CreatureStats.StatType, CreatureStats> getCreatureStats(int level) {
        return creatureStatistics.get(level);
    }

    /**
     * Not all the data types are of the length that suits us, do our best to
     * ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this with the common types!</b>
     *
     * @see toniarts.openkeeper.tools.convert.FileResourceReader#checkOffset(long, long)
     * @param header the header
     * @param reader the buffer
     * @param offset the file offset before the last item was read
     */
    private void checkOffset(KwdHeader header, IResourceChunkReader reader, long offset) throws IOException {
        checkOffset(header.getItemSize(), reader, offset);
    }

    /**
     * Not all the data types are of the length that suits us, do our best to
     * ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this with the common types!</b>
     *
     * @see toniarts.openkeeper.tools.convert.FileResourceReader#checkOffset(long, long)
     * @param itemSize the item size
     * @param reader the buffer
     * @param offset the file offset before the last item was read
     */
    private void checkOffset(long itemSize, IResourceChunkReader reader, long offset) throws IOException {
        long expected = offset + itemSize;
        if (reader.position() != expected) {
            logger.log(Level.WARNING, "Record size differs from expected! Buffer offset is {0} and should be {1}!",
                    new Object[]{reader.position(), expected});
            reader.position((int) expected);
        }
    }

    /**
     * Kwd header, few different kinds, handles all
     */
    private static class KwdHeader {
        // struct kwdHeader {
        //     unsigned int id;
        //     unsigned int size;
        //     union {
        //         struct {
        //             uint16_t w08; <- width
        //             uint16_t w0a; <- height
        //         } level;
        //         unsigned int dw08; <- version?
        //     };
        //     unsigned int x0c[7];
        // };

        private MapDataTypeEnum id;
        private int headerSize = 56; // Well, header and the id data
        private long size;
        private int checkOne;
        private int itemCount;
        private int width;
        private int height;
        private int checkThree; // in level = 223
        private int dataSizeLevel; // in Level size of data exclude paths
        private int unknown; // only in Triggers and Level
        private int headerEndOffset; // 28, *Map - 8, *Triggers - 32,
        private LocalDateTime dateCreated;
        private LocalDateTime dateModified;
        private int checkTwo;
        private int dataSize;

        public KwdHeader() {
        }

        public MapDataTypeEnum getId() {
            return id;
        }

        protected void setId(MapDataTypeEnum id) {
            this.id = id;
        }

        public long getSize() {
            return size;
        }

        protected void setSize(long size) {
            this.size = size;
        }

        public int getHeaderSize() {
            return headerSize;
        }

        protected void setHeaderSize(int headerSize) {
            this.headerSize = headerSize;
        }

        public int getWidth() {
            return width;
        }

        protected void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        protected void setHeight(int height) {
            this.height = height;
        }

        public int getItemCount() {
            return itemCount;
        }

        protected void setItemCount(int itemCount) {
            this.itemCount = itemCount;
        }

        public int getUnknown() {
            return unknown;
        }

        protected void setUnknown(int unknown) {
            this.unknown = unknown;
        }

        protected void setCheckOne(int check) {
            this.checkOne = check;
        }

        public int getCheckOne() {
            return checkOne;
        }

        protected void setCheckTwo(int check) {
            this.checkTwo = check;
        }

        public int getCheckTwo() {
            return checkTwo;
        }

        public LocalDateTime getDateCreated() {
            return dateCreated;
        }

        protected void setDateCreated(LocalDateTime date) {
            this.dateCreated = date;
        }

        public LocalDateTime getDateModified() {
            return dateModified;
        }

        protected void setDateModified(LocalDateTime date) {
            this.dateModified = date;
        }

        protected void setHeaderEndOffset(int offset) {
            this.headerEndOffset = offset;
        }

        public int getHeaderEndOffset() {
            return headerEndOffset;
        }

        protected void setDataSize(int size) {
            this.dataSize = size;
        }

        public int getDataSize(int unknown) {
            return dataSize;
        }

        /**
         * Get the individiual item size (warning, does not apply to all!)
         *
         * @return
         */
        public long getItemSize() {
            return (getSize() - getHeaderSize()) / getItemCount();
        }

        @Override
        public String toString() {
            return "KwdHeader{" + "id=" + id + ", itemCount=" + itemCount + ", width=" + width + ", height=" + height + ", dateCreated=" + dateCreated + ", dateModified=" + dateModified + '}';
        }
    }
}
