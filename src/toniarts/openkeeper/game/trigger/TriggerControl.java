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
package toniarts.openkeeper.game.trigger;

import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerAction.FlagTargetValueActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.WorldUtils;

/**
 *
 * @author ArchDemon
 */
public class TriggerControl extends Control {
    
    private static final Logger logger = System.getLogger(TriggerControl.class.getName());

    private static final short LEVEL_SCORE_FLAG_ID = 128;
    private static final short TIME_LIMIT_TIMER_ID = 16;

    protected TriggerGenericData trigger;
    protected final TriggerGenericData root;

    protected final ILevelInfo levelInfo;
    protected final IGameTimer gameTimer;
    protected final IGameController gameController;
    protected final IMapController mapController;
    protected final ICreaturesController creaturesController;

    public TriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId) {
        this.gameController = gameController;
        this.levelInfo = levelInfo;
        this.gameTimer = gameTimer;
        this.mapController = mapController;
        this.creaturesController = creaturesController;

        root = new TriggerLoader(levelInfo.getLevelData()).load(triggerId);
        if (root == null) {
            throw new IllegalArgumentException("trigger can not be null");
        }
        trigger = root;
    }

    @Override
    protected void updateControl(float tpf) {
        TriggerGenericData next = null;
        trigger.subRepeatTimes();

        for (int i = trigger.getLastTriggerIndex() + 1; i < trigger.getQuantity(); i++) {
            TriggerData value = trigger.getChild(i);

            switch (value) {
                case null -> logger.log(Level.WARNING, "Trigger is null!");
                case TriggerGenericData triggerGenericData -> {

                    if (next == null && isActive(triggerGenericData)) {
                        trigger.setLastTrigger((TriggerGenericData) value);
                        next = (TriggerGenericData) value;
                    }
                }
                case TriggerActionData triggerActionData -> {

                    //System.out.println(String.format("%s: %d %s", this.getClass().getSimpleName(), trigger.getId(), trigger.getType()));
                    doAction(triggerActionData);
                    if (!trigger.isRepeateable()) {
                        trigger.detachChild(value);
                        i--;
                    }
                }
                default -> {
                }
            }
        }

        if (trigger.getQuantity() == 0 && trigger.getParent() != null) {
            next = trigger.getParent();
            trigger.detachFromParent();
        }

        if (next == null) {
            trigger.setLastTrigger(null);
            trigger = (trigger.getParent() != null) ? trigger.getParent() : root;
        } else {
            trigger = next;
        }
    }

    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        int value;
        int target;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case FLAG:
                short targetId = (Short) trigger.getUserData("targetId");
                if (targetId == LEVEL_SCORE_FLAG_ID) {

                    // A special value, level score
                    target = levelInfo.getLevelScore();
                } else {
                    target = levelInfo.getFlag(targetId);
                }
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = levelInfo.getFlag((Short) trigger.getUserData("flagId"));
                }
                break;

            case TIMER:
                targetId = (short) trigger.getUserData("targetId");
                if (targetId == TIME_LIMIT_TIMER_ID) {
                    target = (levelInfo.getTimeLimit() != null ? levelInfo.getTimeLimit().intValue() : 0);
                } else {
                    target = (int) levelInfo.getTimer(targetId).getTime();
                }

                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = (int) Math.floor(levelInfo.getTimer((Short) trigger.getUserData("timerId")).getTime());
                }
                break;

            case LEVEL_TIME:
                target = (int) Math.floor(gameTimer.getGameTime());
                value = (Integer) trigger.getUserData("value");
                break;
            case LEVEL_CREATURES:
                return false;
            case LEVEL_PAY_DAY:
                return false;
            case LEVEL_PLAYED:
                return false;
            default:
                logger.log(Level.WARNING, "Target Type not supported {0}!", targetType);
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    protected void doAction(TriggerActionData trigger) {
        TriggerAction.ActionType type = trigger.getType();
        switch (type) {

            case CREATE_CREATURE:
                short creatureId = trigger.getUserData("creatureId", short.class);
                short playerId = trigger.getUserData("playerId", short.class);
                short level = trigger.getUserData("level", short.class);
                EnumSet<Creature.CreatureFlag> flags = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class), Creature.CreatureFlag.class);
                Point p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                // TODO: flags!
                creaturesController.spawnCreature(creatureId, playerId, level, WorldUtils.pointToVector2f(p), ICreaturesController.SpawnType.PLACE);
                break;

            case MAKE:
                TriggerAction.MakeType flag = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                        TriggerAction.MakeType.class);
                boolean available = trigger.getUserData("available", short.class) != 0;
                playerId = trigger.getUserData("playerId", short.class);
                KwdFile kwdFile = levelInfo.getLevelData();
                short targetId = trigger.getUserData("targetId", short.class);
                // TODO this
                switch (flag) {
                    case CREATURE:
                        getPlayerController(playerId).getCreatureControl().setTypeAvailable(kwdFile.getCreature(targetId), available);
                        break;
                    case DOOR:
                        break;
                    case KEEPER_SPELL:
                        getPlayerController(playerId).getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(targetId), available);
                        break;
                    case ROOM:
                        getPlayerController(playerId).getRoomControl().setTypeAvailable(kwdFile.getRoomById(targetId), available);
                        break;
                    case TRAP:
                        break;
                }
                break;

            case FLAG:
                short flagId = trigger.getUserData("flagId", short.class);
                EnumSet<FlagTargetValueActionType> flagType = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class),
                        FlagTargetValueActionType.class);
                int value = trigger.getUserData("value", int.class);
                if (flagType.contains(FlagTargetValueActionType.TARGET)) {
                    value = levelInfo.getFlag(value);
                }

                if (flagId == LEVEL_SCORE_FLAG_ID) {
                    levelInfo.setLevelScore(getTargetValue(levelInfo.getLevelScore(), value, flagType));
                } else {
                    int base = levelInfo.getFlag(flagId);
                    levelInfo.setFlag(flagId, getTargetValue(base, value, flagType));
                }
                break;

            case INITIALIZE_TIMER:
                short timerId = trigger.getUserData("timerId", short.class);
                if (timerId == TIME_LIMIT_TIMER_ID) {
                    value = trigger.getUserData("value", int.class);
                    levelInfo.setTimeLimit(value);
                } else {
                    levelInfo.getTimer(timerId).initialize();
                }
                break;

            case SET_TIME_LIMIT:
                timerId = trigger.getUserData("timerId", short.class);
                if (timerId == TIME_LIMIT_TIMER_ID) {
                    value = trigger.getUserData("value", int.class);
                    levelInfo.setTimeLimit(value);
                } else {
                    logger.log(Level.WARNING, "Only level time limit supported!");
                }
                break;

            case CREATE_HERO_PARTY:
                ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
                short partyId = trigger.getUserData("partyId", short.class);
                PartyType partyType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class), PartyType.class);
                creaturesController.spawnHeroParty(partyId, partyType, WorldUtils.ActionPointToVector2f(ap));
                break;

            case SET_ALLIANCE:
                short playerOneId = trigger.getUserData("playerOneId", short.class);
                short playerTwoId = trigger.getUserData("playerTwoId", short.class);
                available = trigger.getUserData("available", short.class) == 0; // 0 = Create, !0 = Break
                if (available) {
                    gameController.createAlliance(playerOneId, playerTwoId);
                } else {
                    gameController.breakAlliance(playerOneId, playerTwoId);
                }
                break;

            case ALTER_TERRAIN_TYPE:
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                short terrainId = trigger.getUserData("terrainId", short.class);
                playerId = trigger.getUserData("playerId", short.class);
                mapController.alterTerrain(p, terrainId, playerId);
                break;

            case COLLAPSE_HERO_GATE:
                // TODO this
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                break;

            case SET_CREATURE_MOODS:
                // TODO this
                available = trigger.getUserData("available", short.class) != 0;
                break;

            case SET_SYSTEM_MESSAGES:
                // TODO this
                available = trigger.getUserData("available", short.class) != 0;
                break;

            case CHANGE_ROOM_OWNER:
                // TODO this
                playerId = trigger.getUserData("playerId", short.class);
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                break;

            case SET_SLAPS_LIMIT:
                // TODO this
                value = trigger.getUserData("value", int.class); // 0 = Off
                break;

            case SET_TIMER_SPEECH:
                // TODO this
                available = trigger.getUserData("available", short.class) != 0;
                break;

            default:
                logger.log(Level.WARNING, "Trigger Action not supported!");
                break;
        }
    }

    protected boolean compare(float target, TriggerGeneric.ComparisonType compare, float value) {
        boolean result = false;
        switch (compare) {
            case EQUAL_TO:
                result = target == value;
                break;
            case GREATER_OR_EQUAL_TO:
                result = target >= value;
                break;
            case GREATER_THAN:
                result = target > value;
                break;
            case LESS_OR_EQUAL_TO:
                result = target <= value;
                break;
            case LESS_THAN:
                result = target < value;
                break;
            case NOT_EQUAL_TO:
                result = target != value;
                break;
            case NONE:
                logger.log(Level.WARNING, "Comparison Type not supported!");
                break;
        }
        return result;
    }

    private int getTargetValue(int base, int value, EnumSet<FlagTargetValueActionType> flagType) {

        if (flagType.contains(FlagTargetValueActionType.EQUAL)) {
            return value;
        } else if (flagType.contains(FlagTargetValueActionType.PLUS)) {
            return base + value;
        } else if (flagType.contains(FlagTargetValueActionType.MINUS)) {
            return base - value;
        }

        logger.log(Level.WARNING, "Unsupported target flag type {0}!", flagType);
        return 0;
    }

    protected Keeper getPlayer(short playerId) {
        if (playerId == 0) {
            return levelInfo.getPlayer(Player.KEEPER1_ID); // Current player
        } else {
            return levelInfo.getPlayer(playerId);
        }
    }

    protected IPlayerController getPlayerController(short playerId) {
        if (playerId == 0) {
            return gameController.getPlayerController(Player.KEEPER1_ID); // Current player
        } else {
            return gameController.getPlayerController(playerId);
        }
    }
}
