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
package toniarts.openkeeper.game.controller;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.CasinoController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.DoubleQuadController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.controller.room.HatcheryController;
import toniarts.openkeeper.game.controller.room.HeroGateFrontEndController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.LairController;
import toniarts.openkeeper.game.controller.room.LibraryController;
import toniarts.openkeeper.game.controller.room.NormalRoomController;
import toniarts.openkeeper.game.controller.room.PrisonController;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.game.controller.room.ThreeByThreeController;
import toniarts.openkeeper.game.controller.room.TortureChamberController;
import toniarts.openkeeper.game.controller.room.TrainingRoomController;
import toniarts.openkeeper.game.controller.room.TreasuryController;
import toniarts.openkeeper.game.controller.room.WorkshopController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * A factory class you can use to build buildings
 *
 * @author ArchDemon
 */
public final class RoomControllerFactory {

    private static final Logger logger = System.getLogger(RoomControllerFactory.class.getName());

    private RoomControllerFactory() {
        // Nope
    }

    public static IRoomController constructRoom(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer) {
        String roomName = roomInstance.getRoom().getName();

        switch (roomInstance.getRoom().getTileConstruction()) {
            case _3_BY_3 -> {
                return new ThreeByThreeController(kwdFile, roomInstance, objectsController);
            }
            case HERO_GATE, HERO_GATE_FRONT_END -> {
                return new HeroGateFrontEndController(kwdFile, roomInstance, objectsController);
            }
            case HERO_GATE_2_BY_2 -> {
                return new NormalRoomController(kwdFile, roomInstance, objectsController);
            }
            case _5_BY_5_ROTATED -> {
                return new FiveByFiveRotatedController(kwdFile, roomInstance, objectsController, gameSettings, gameTimer);
            }
            case NORMAL -> {
                return constructNormal(roomName, kwdFile, roomInstance, objectsController, gameTimer, gameSettings);
            }
            case DOUBLE_QUAD -> {
                return constructDoubleQuad(roomName, kwdFile, roomInstance, objectsController, gameTimer);
            }
            default -> {
                // TODO
                logger.log(Level.WARNING, "Room {0} not exist", roomName);
                return new NormalRoomController(kwdFile, roomInstance, objectsController);
            }
        }
    }

    private static IRoomController constructDoubleQuad(String roomName, KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        if (roomName.equalsIgnoreCase("Prison")) {
            return new PrisonController(kwdFile, roomInstance, objectsController, gameTimer);
        } else if (roomName.equalsIgnoreCase("Combat Pit")) {
            return new CombatPitController(kwdFile, roomInstance, objectsController);
        } else if (roomName.equalsIgnoreCase("Temple")) {
            return new TempleController(kwdFile, roomInstance, objectsController);
        }

        return new DoubleQuadController(kwdFile, roomInstance, objectsController);
    }

    private static IRoomController constructNormal(String roomName, KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        if (roomName.equalsIgnoreCase("Lair")) {
            return new LairController(kwdFile, roomInstance, objectsController, gameTimer);
        } else if (roomName.equalsIgnoreCase("Library")) {
            return new LibraryController(kwdFile, roomInstance, objectsController, gameTimer);
        } else if (roomName.equalsIgnoreCase("Training Room")) {
            return new TrainingRoomController(kwdFile, roomInstance, objectsController, gameTimer);
        } else if (roomName.equalsIgnoreCase("Work Shop")) {
            return new WorkshopController(kwdFile, roomInstance, objectsController);
//                } else if (roomName.equalsIgnoreCase("Guard Room")) {
//                    return new GuardRoom(assetManager, roomInstance, objectLoader, worldState, effectManager);
        } else if (roomName.equalsIgnoreCase("Casino")) {
            return new CasinoController(kwdFile, roomInstance, objectsController);
//                } else if (roomName.equalsIgnoreCase("Graveyard")) {
//                    return new Graveyard(assetManager, roomInstance, objectLoader, worldState, effectManager);
        } else if (roomName.equalsIgnoreCase("Torture Chamber")) {
            return new TortureChamberController(kwdFile, roomInstance, objectsController, gameTimer);
        } else if (roomName.equalsIgnoreCase("Treasury")) {
            return new TreasuryController(kwdFile, roomInstance, objectsController, gameSettings, gameTimer);
        } else if (roomName.equalsIgnoreCase("Hatchery")) {
            return new HatcheryController(kwdFile, roomInstance, objectsController, gameTimer);
        }

        return new NormalRoomController(kwdFile, roomInstance, objectsController);
    }
}
