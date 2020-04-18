/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Calculates mana for all players. TODO: maybe listener based, that only reacts
 * to changes? TODO: Posession, Players, Entity based?
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ManaCalculatorLogic implements IGameLogicUpdatable {

    private float tick = 0;
    private final Map<Short, PlayerManaControl> manaControls = new HashMap<>(4);
    private final Map<Short, PlayerCreatureControl> creatureControls = new HashMap<>(4);
    private final Map<Short, Integer> manaGains;
    private final Map<Short, Integer> manaLosses;
    private final IMapController mapController;
    private final int manaGainBase;
    private final static int MANA_LOSE_PER_IMP = 7;  // I don't find in Creature.java

    public ManaCalculatorLogic(Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, Collection<IPlayerController> playerControllers, IMapController mapController) {
        this.mapController = mapController;
        for (IPlayerController playerController : playerControllers) {
            PlayerManaControl manaControl = playerController.getManaControl();
            if (manaControl != null) {
                manaControls.put(playerController.getKeeper().getId(), manaControl);
                creatureControls.put(playerController.getKeeper().getId(), playerController.getCreatureControl());
            }
        }
        manaGains = new HashMap<>(manaControls.size());
        manaLosses = new HashMap<>(manaControls.size());
        manaGainBase = (int) gameSettings.get(Variable.MiscVariable.MiscType.DUNGEON_HEART_MANA_GENERATION_INCREASE_PER_SECOND).getValue();
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        tick += tpf;
        if (tick >= 1) {
            reset();
            calculateGainFromMapTiles();
            calculateLooseFromCreatures();
            updateManaControls(tpf);
            tick -= 1;
        }
    }

    private void reset() {
        for (Short playerId : manaControls.keySet()) {
            manaGains.put(playerId, manaGainBase);
            manaLosses.put(playerId, 0);
        }
    }

    private void calculateGainFromMapTiles() {
        MapData mapData = mapController.getMapData();
        for (MapTile tile : mapData) {
            if (manaGains.containsKey(tile.getOwnerId())) {
                manaGains.put(tile.getOwnerId(), manaGains.get(tile.getOwnerId()) + tile.getManaGain());
            }
        }
    }

    private void calculateLooseFromCreatures() {
        for (Short playerId : manaControls.keySet()) {
            manaLosses.put(playerId, creatureControls.get(playerId).getImpCount() * MANA_LOSE_PER_IMP);
        }
    }

    private void updateManaControls(float tpf) {
        for (Map.Entry<Short, PlayerManaControl> entry : manaControls.entrySet()) {
            entry.getValue().updateMana(manaGains.getOrDefault(entry.getKey(), 0), manaLosses.getOrDefault(entry.getKey(), 0));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
