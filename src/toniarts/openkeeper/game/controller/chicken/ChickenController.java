/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.controller.chicken;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Controls an entity with {@link ChickenAi} component. Basically supports the
 * AI state machine.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ChickenController extends EntityController implements IChickenController {
    
    private static final Logger logger = System.getLogger(ChickenController.class.getName());

    private final INavigationService navigationService;
    private final IGameTimer gameTimer;
    // TODO: All the data is not supposed to be on entities as they become too big, but I don't want these here either
    private final GameObject eggObject;
    private final GameObject chickenObject;
    private final StateMachine<IChickenController, ChickenState> stateMachine;
    private float motionless = 0;

    public ChickenController(EntityId entityId, EntityData entityData, GameObject eggObject, GameObject chickenObject,
            INavigationService navigationService, IGameTimer gameTimer, IObjectsController objectsController,
            IMapController mapController) {
        super(entityId, entityData, objectsController, mapController);

        this.navigationService = navigationService;
        this.eggObject = eggObject;
        this.chickenObject = chickenObject;
        this.gameTimer = gameTimer;
        this.stateMachine = new DefaultStateMachine<>(this);
    }

    @Override
    public void navigateToRandomPoint() {
        final Position position = entityData.getComponent(entityId, Position.class);
        final Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        final Owner owner = entityData.getComponent(entityId, Owner.class);
        final RoomStorage roomStorage = entityData.getComponent(entityId, RoomStorage.class);
        if (position != null && mobile != null && owner != null) {
            Point start = WorldUtils.vectorToPoint(position.position);
            Point destination = null;
            if (roomStorage != null) {

                // Koppi-kana, not a happy chicken
                destination = navigationService.findRandomTileInRoom(start, 3, this);
            } else {

                // Free-range chicken
                destination = navigationService.findRandomAccessibleTile(start, 3, this);
            }
            if (destination != null) {
                createNavigation(start, destination, null);
            }
        }
    }

    @Override
    public StateMachine<IChickenController, ChickenState> getStateMachine() {
        return stateMachine;
    }

    @Override
    public void growIntoChicken() {
        objectsController.transformToChicken(entityId);
    }

    @Override
    public boolean isStopped() {
        return entityData.getComponent(entityId, Navigation.class) == null;
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget) {
        GraphPath<IMapTileInformation> path = navigationService.findPath(currentLocation, destination, this);
        if (path == null) {
            logger.log(Level.WARNING, "No path from {0} to {1}", new Object[]{getChickenCoordinates(), destination});
            return true;
        }
        entityData.setComponent(entityId, new Navigation(destination, faceTarget, SteeringUtils.pathToList(path)));
        return false;
    }

    @Override
    public void stop() {
        // Note that this is the updatable stop, not the creature stop...
    }

    private void initState() {
        stateMachine.changeState(entityData.getComponent(entityId, ChickenAi.class).getChickenState());
    }

    @Override
    public boolean canFly() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canFly;
    }

    @Override
    public boolean canWalkOnWater() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canWalkOnWater;
    }

    @Override
    public boolean canWalkOnLava() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canWalkOnLava;
    }

    @Override
    public boolean canMoveDiagonally() {
        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void processTick(float tpf, double gameTime) {

        /**
         * Hmm, I'm not sure how to do this, this is not ideal either, how to
         * control the state machine outside the controller. Should it be
         * allowed and should we just check that the current state matches the
         * state in the entity component
         */
        //CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (stateMachine.getCurrentState() == null) {
            initState();
        }

        /**
         * The creatures have these time motionless stuff in different states,
         * they seem to equal to kind of re-evaluate what to do. We should
         * figure out a proper way to utilize these. We could also use the
         * delayed telegram stuff. The hard part is just to kind of figure out
         * the motionless part and not re-send messages always etc. We could
         * probably go with pretty much event driven AI.
         */
        if (isStopped()) {
            motionless += tpf;
        } else {
            motionless = 0;
        }

        stateMachine.update();

        // Also change our state component
        ChickenAi chickenAi = entityData.getComponent(entityId, ChickenAi.class);
        if (chickenAi == null || stateMachine.getCurrentState() != chickenAi.getChickenState()) {
            entityData.setComponent(entityId, new ChickenAi(gameTimer.getGameTime(), stateMachine.getCurrentState()));
        }
    }

    @Override
    public boolean isTimeToReEvaluate() {

        // See that we have been motionless for enough time, per state
        // TODO: now just 5 seconds, it is the default for imps
        return motionless >= 5f;
    }

    @Override
    public void resetReEvaluationTimer() {
        motionless = 0;
    }

    @Override
    public boolean isStateTimeExceeded() {
        double timeSpent = gameTimer.getGameTime() - entityData.getComponent(entityId, ChickenAi.class).stateStartTime;

        switch (stateMachine.getCurrentState()) {
            case HATCHING_START:
                return timeSpent >= getAnimationTime(eggObject, chickenObject, stateMachine.getCurrentState());
            case HATCHING_END:
                return timeSpent >= getAnimationTime(eggObject, chickenObject, stateMachine.getCurrentState());
            case PECKING:
                return timeSpent >= getAnimationTime(eggObject, chickenObject, stateMachine.getCurrentState()) * 2;
        }
        return false;
    }

    private static double getAnimationTime(GameObject eggObject, GameObject chickenObject, ChickenState state) {
        // TODO: we could cache and calculate these for all centrally, also include the starting and ending animation
        ArtResource animationResource = null;

        switch (state) {
            case HATCHING_START:
                animationResource = eggObject.getMeshResource();
                break;
            case HATCHING_END:
                animationResource = eggObject.getAdditionalResources().get(0);
                break;
            case PECKING:
                animationResource = chickenObject.getAdditionalResources().get(0);
                break;
        }

        int frames = animationResource.getData("frames");
        int fps = animationResource.getData("fps");
        return frames / (double) fps;
    }

    private Point getChickenCoordinates() {
        return WorldUtils.vectorToPoint(getPosition(entityData, entityId));
    }

}
