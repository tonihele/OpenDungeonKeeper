/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.world.creature;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.Cohesion;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.proximities.InfiniteProximity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.gui.CursorFactory.CursorType;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.Attributes;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Thing.DeadBody;
import toniarts.openkeeper.tools.convert.map.Thing.GoodCreature;
import toniarts.openkeeper.tools.convert.map.Thing.KeeperCreature;
import toniarts.openkeeper.tools.convert.map.Thing.NeutralCreature;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.animation.AnimationControl;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IHaulable;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.control.IUnitFlowerControl;
import toniarts.openkeeper.world.control.LandingControl;
import toniarts.openkeeper.world.control.UnitFlowerControl;
import toniarts.openkeeper.world.creature.steering.AbstractCreatureSteeringControl;
import toniarts.openkeeper.world.creature.steering.CreatureSteeringCreator;
import toniarts.openkeeper.world.listener.CreatureListener;
import toniarts.openkeeper.world.object.GoldObjectControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.pathfinding.PathFindable;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * Controller for creature. Bridge between the visual object and AI.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class CreatureControl extends AbstractCreatureSteeringControl implements IInteractiveControl, CreatureListener, AnimationControl, IUnitFlowerControl, PathFindable, IHaulable {

    public enum AnimationType {

        MOVE, WORK, IDLE, ATTACK, DYING, STUNNED, OTHER;
    }

    private enum Moods {
        HAPPY, UNHAPPY, ANGRY
    }

    // Attributes
    private String name;
    private final String bloodType;
    protected int gold = 0;
    protected int level = 1;
    protected int health = 1;
    protected int experience = 0;
    protected short ownerId;
    protected float height;
    protected int maxHealth;
    protected int fear;
    protected int threat;
    protected int meleeDamage;
    protected int pay;
    protected int maxGoldHeld;
    protected int hungerFill;
    protected int manaGenPrayer;
    protected int experienceToNextLevel;
    protected int experiencePerSecond;
    protected int experiencePerSecondTraining;
    protected int researchPerSecond;
    protected int manufacturePerSecond;
    protected int decomposeValue;
    protected float speed;
    protected float runSpeed;
    protected float tortureTimeToConvert;
    protected int posessionManaCost;
    protected int ownLandHealthIncrease;
    protected float distanceCanHear;
    protected float meleeRecharge;
    private CreatureAttack executingAttack;
    private static final int MAX_CREATURE_LEVEL = 10;
    private Moods mood = Moods.HAPPY;
    private int efficiencyPersentage = 80;
    //

    protected final StateMachine<CreatureControl, CreatureState> stateMachine;
    private final WorldState worldState;
    private float timeInState;
    private CreatureState state;
    private boolean animationPlaying = false;
    private float lastAttributeUpdateTime = 0;
    private float lastStateUpdateTime = 0;
    private float timeAwake = 0;
    private Task assignedTask;
    private AnimationType playingAnimationType = AnimationType.IDLE;
    private ObjectControl creatureLair;
    private CreatureControl followTarget;
    private final List<CreatureAttack> attacks;
    private boolean hasPortalGem = false;
    private ObjectiveType playerObjective;
    private short objectiveTargetPlayerId;
    private IHaulable hauling;
    private boolean workNavigationRequired = true;

    /**
     * Things we hear & see per tick
     */
    private final Set<CreatureControl> visibilityList = new HashSet<>();
    private boolean visibilityListUpdated = false;
    private Integer ourThreat;
    private Integer enemyThreat;
    private Integer fellowFighters;
    private CreatureControl attackTarget;
    private boolean attacked = false;

    // Good creature specific stuff
    private Party party;
    private Thing.HeroParty.Objective objective;
    private ActionPoint objectiveTargetActionPoint;
    private EnumSet<Thing.Creature.CreatureFlag> flags;
    //

    public CreatureControl(Thing.Creature creatureInstance, Creature creature, WorldState worldState, short playerId, short level) {
        super(creature);
        stateMachine = new DefaultStateMachine<CreatureControl, CreatureState>(this) {

            @Override
            public void changeState(CreatureState newState) {
                super.changeState(newState);

                // Notify
                onStateChange(CreatureControl.this, newState, getPreviousState());

            }

        };
        this.worldState = worldState;

        // Attributes
        name = Utils.generateCreatureName();
        bloodType = Utils.generateBloodType();
        this.level = level;
        ownerId = playerId;
        setAttributesByLevel();
        health = maxHealth;
        if (creatureInstance != null) {
            gold = creatureInstance.getGoldHeld();
            if (creatureInstance instanceof KeeperCreature) {
                health = (int) (((KeeperCreature) creatureInstance).getInitialHealth() / 100f * health);
                this.level = ((KeeperCreature) creatureInstance).getLevel();
                ownerId = ((KeeperCreature) creatureInstance).getPlayerId();
            } else if (creatureInstance instanceof GoodCreature) {
                health = (int) (((GoodCreature) creatureInstance).getInitialHealth() / 100f * health);
                this.level = ((GoodCreature) creatureInstance).getLevel();
                ownerId = Player.GOOD_PLAYER_ID;
                objective = ((GoodCreature) creatureInstance).getObjective();
                objectiveTargetPlayerId = ((GoodCreature) creatureInstance).getObjectiveTargetPlayerId();
                if (((GoodCreature) creatureInstance).getObjectiveTargetActionPointId() != 0) {
                    //objectiveTargetActionPoint = worldState.getGameState().getActionPointState().getActionPoint(((GoodCreature) creatureInstance).getObjectiveTargetActionPointId());
                }
                flags = ((GoodCreature) creatureInstance).getFlags();
            } else if (creatureInstance instanceof NeutralCreature) {
                health = (int) (((NeutralCreature) creatureInstance).getInitialHealth() / 100f * health);
                this.level = ((NeutralCreature) creatureInstance).getLevel();
                ownerId = Player.NEUTRAL_PLAYER_ID;
            } else if (creatureInstance instanceof DeadBody) {
                ownerId = ((DeadBody) creatureInstance).getPlayerId();
            }
        }

        // Create the attacks
        attacks = new ArrayList<>(creature.getSpells().size() + 1);
        attacks.add(new CreatureAttack(this)); // Melee
        for (Creature.Spell spell : creature.getSpells()) {
            attacks.add(new CreatureAttack(this, spell, worldState.getGameState().getLevelData())); // Spells
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        // Set the appropriate animation
        playStateAnimation();

        // If hauling, update the haulable position
        if (hauling != null) {
            hauling.updatePosition(getSpatial().getWorldTranslation());
        }
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        visibilityList.clear();
        visibilityListUpdated = false;
        ourThreat = null;
        enemyThreat = null;
        fellowFighters = null;
        if (stateMachine.getCurrentState() == null) {
            initState();
        }

        // Update attacks
        for (CreatureAttack attack : attacks) {
            attack.recharge(tpf);
        }

        // Update attributes
        if (stateMachine.getCurrentState() != null
                && stateMachine.getCurrentState() != CreatureState.PICKED_UP) {
            updateAttributes(tpf);
        }

        // Set the time in state
        if (stateMachine.getCurrentState() != null) {
            if (stateMachine.getCurrentState().equals(state)) {
                timeInState += tpf;
            } else {
                state = stateMachine.getCurrentState();
                timeInState = 0f;
            }

            // Time awake
            if (!(stateMachine.isInState(CreatureState.SLEEPING)
                    || stateMachine.isInState(CreatureState.RECUPERATING))) {
                timeAwake += tpf;
            } else if (stateMachine.isInState(CreatureState.SLEEPING)) {
                timeAwake = 0;
            }
        }

        // Set the attacked flag, when no threats visible, assume attack over
        if (attacked && getEnemyThreat() == 0) {
            attacked = false;
        }

        // Update state machine
        stateMachine.update();
    }

    /**
     * Sets up initial state
     */
    private void initState() {
        if (!detectImprisoningAndTorturing()) {
            stateMachine.changeState(CreatureState.IDLE);
        }
    }

    /**
     * Detects if creature should be imprisoned or tortured and sets the state
     * accordingly
     *
     * @return true if the state has been set
     */
    private boolean detectImprisoningAndTorturing() {
        return detectImprisoningAndTorturing(getCreatureCoordinates());
    }

    /**
     * Detects if creature should be imprisoned or tortured and sets the state
     * accordingly
     *
     * @param p the point to test upon
     * @return true if the state has been set
     */
    private boolean detectImprisoningAndTorturing(Point p) {
        GenericRoom room = worldState.getRoomAtPoint(p);
        if (room != null && room.getRoomInstance().getOwnerId() != getOwnerId()) {

            // See if tortured or imprisoned
            if (room.hasObjectControl(GenericRoom.ObjectType.PRISONER)) {
                room.getObjectControl(GenericRoom.ObjectType.PRISONER).addItem(1, p, null, this);
                stateMachine.changeState(CreatureState.IMPRISONED);
                return true;
            }
            if (room.hasObjectControl(GenericRoom.ObjectType.TORTUREE)) {
                room.getObjectControl(GenericRoom.ObjectType.TORTUREE).addItem(1, p, null, this);
                stateMachine.changeState(CreatureState.TORTURED);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public void navigateToRandomPoint() {
        Point p = worldState.findRandomAccessibleTile(WorldUtils.vectorToPoint(getSpatial().getWorldTranslation()), 10, this);
        if (p != null) {

            SteeringBehavior<Vector2> steering = CreatureSteeringCreator.navigateToPoint(worldState, this, this, p);
            if (steering != null) {
                steering.setEnabled(!isAnimationPlaying());
                setSteeringBehavior(steering);
            }
        }
    }

    public void navigateToRandomPointAroundTarget(CreatureControl target, int radius) {
        Point p = worldState.findRandomAccessibleTile(WorldUtils.vectorToPoint(target.getSpatial().getWorldTranslation()), radius, this);
        if (p != null && p != getCreatureCoordinates()) {

            SteeringBehavior<Vector2> steering = CreatureSteeringCreator.navigateToPoint(worldState, this, this, p);
            if (steering != null) {
                steering.setEnabled(!isAnimationPlaying());
                setSteeringBehavior(steering);
            }
        }
    }

    public boolean idleTimeExceeded() {
        return ((creature.getAttributes().getIdleDuration() < 0.1f ? 1f : creature.getAttributes().getIdleDuration()) < timeInState);
    }

    public StateMachine<CreatureControl, CreatureState> getStateMachine() {
        return stateMachine;
    }

    private void playAnimation(ArtResource anim) {
        animationPlaying = true;
        AnimationLoader.playAnimation(getSpatial(), anim, worldState.getAssetManager());
    }

    /**
     * Should the current animation stop?
     *
     * @return stop or not
     */
    @Override
    public boolean isStopAnimation() {
        // FIXME: not very elegant to check this way
        if (!enabled) {
            return false;
        }
        switch (playingAnimationType) {
            case IDLE: {
                return (stateMachine.getCurrentState() != CreatureState.IDLE || steeringBehavior != null);
            }
            case MOVE: {
                return (steeringBehavior == null || !steeringBehavior.isEnabled());
            }
            case WORK: {
                return (steeringBehavior != null || !isAssignedTaskValid());
            }
            default: {
                return true;
            }
        }
    }

    private boolean isAnimationPlaying() {
        return animationPlaying;
    }

    /**
     * Current animation has stopped
     */
    @Override
    public void onAnimationStop() {
        animationPlaying = false;

        // If steering is set, enable it
        if (steeringBehavior != null && !steeringBehavior.isEnabled()) {
            steeringBehavior.setEnabled(true);
        }
        if (stateMachine.getCurrentState() == CreatureState.ENTERING_DUNGEON) {
            Vector3f offset = ConversionUtils.convertVector(creature.getAnimationOffsets(Creature.OffsetType.PORTAL_ENTRANCE));
            spatial.move(offset);
            stateMachine.changeState(CreatureState.IDLE);

        } else if (stateMachine.getCurrentState() == CreatureState.SLAPPED) {
            playAnimation(creature.getAnimation(Creature.AnimationType.GET_UP));
            playingAnimationType = AnimationType.OTHER;
            // Return to previous state
            stateMachine.revertToPreviousState();
        } else if (playingAnimationType == AnimationType.DYING) {

            // TODO: should show the pose for awhile I guess
            if (stateMachine.getCurrentState() == CreatureState.DEAD) {
                removeCreature();
            }
        } else {
            playStateAnimation();
        }
    }

    /**
     * An animation cycle is finished
     */
    @Override
    public void onAnimationCycleDone() {

        if (isStopped() && stateMachine.getCurrentState() == CreatureState.WORK
                && playingAnimationType == AnimationType.WORK
                && isAssignedTaskValid() && !workNavigationRequired) {

            // Different work based reactions
//            assignedTask.executeTask(this);
        }
    }

    private void playStateAnimation() {
        if (!animationPlaying) {
            if (playingAnimationType == AnimationType.STUNNED && !stateMachine.isInState(CreatureState.STUNNED)) {
                playAnimation(creature.getAnimGetUpResource());
                playingAnimationType = AnimationType.OTHER;
            } else if (steeringBehavior != null && steeringBehavior.isEnabled()) {
                playAnimation(creature.getAnimWalkResource());
                playingAnimationType = AnimationType.MOVE;
            } else if (stateMachine.getCurrentState() == CreatureState.WORK) {

                // Different work animations
                playingAnimationType = AnimationType.WORK;
//                if (assignedTask != null && assignedTask.getTaskAnimation(this) != null) {
//                    playAnimation(assignedTask.getTaskAnimation(this));
//                } else {
//                    onAnimationCycleDone();
//                }
            } else if (stateMachine.getCurrentState() == CreatureState.ENTERING_DUNGEON) {
                playAnimation(creature.getAnimEntranceResource());
                playingAnimationType = AnimationType.OTHER;
            } else if (stateMachine.getCurrentState() == CreatureState.FIGHT) {
                CreatureAttack executeAttack = executingAttack;
                if (executeAttack != null && executeAttack.isPlayAnimation()) {
                    if (executeAttack.isMelee()) {
                        List<ArtResource> meleeAttackAnimations = new ArrayList<>(2);
                        if (creature.getAnimMelee1Resource() != null) {
                            meleeAttackAnimations.add(creature.getAnimMelee1Resource());
                        }
                        if (creature.getAnimMelee2Resource() != null) {
                            meleeAttackAnimations.add(creature.getAnimMelee2Resource());
                        }
                        ArtResource attackAnim = meleeAttackAnimations.get(0);
                        if (meleeAttackAnimations.size() > 1) {
                            attackAnim = Utils.getRandomItem(meleeAttackAnimations);
                        }
                        playAnimation(attackAnim);
                    } else {
                        playAnimation(creature.getAnimMagicResource());
                    }
                }
                playingAnimationType = AnimationType.ATTACK;
            } else if (stateMachine.getCurrentState() == CreatureState.DEAD
                    || stateMachine.getCurrentState() == CreatureState.UNCONSCIOUS) {

                //TODO: Dying direction
                if (playingAnimationType != AnimationType.DYING) {
                    playAnimation(creature.getAnimDieResource());
                    playingAnimationType = AnimationType.DYING;
                    showUnitFlower(Integer.MAX_VALUE);
                } else if (stateMachine.getCurrentState() == CreatureState.DEAD) {
                    removeCreature();
                }
            } else if (stateMachine.isInState(CreatureState.STUNNED)) {
                if (playingAnimationType != AnimationType.STUNNED) {
                    playAnimation(creature.getAnimation(Creature.AnimationType.STUNNED));
                    playingAnimationType = AnimationType.STUNNED;
                }
            } else if (stateMachine.isInState(CreatureState.DRAGGED)) {
                playAnimation(creature.getAnimDraggedPoseResource());
                playingAnimationType = AnimationType.OTHER;
            } else {
                List<ArtResource> idleAnimations = new ArrayList<>(3);
                if (creature.getAnimIdle1Resource() != null) {
                    idleAnimations.add(creature.getAnimIdle1Resource());
                }
                if (creature.getAnimIdle2Resource() != null) {
                    idleAnimations.add(creature.getAnimIdle2Resource());
                }
                ArtResource idleAnim = idleAnimations.get(0);
                if (idleAnimations.size() > 1) {
                    idleAnim = Utils.getRandomItem(idleAnimations);
                }
                playAnimation(idleAnim);
                playingAnimationType = AnimationType.IDLE;
            }

        }
    }

    @Override
    public String getTooltip(short playerId) {
        String tooltip;
        if (ownerId == playerId) {
            tooltip = Utils.getMainTextResourceBundle().getString("2841");
        } else {
            tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(creature.getTooltipStringId()));
        }
        return formatString(tooltip);
    }

    private String formatString(String string) {
        return string.replaceAll("%29", name)
                .replaceAll("%30", creature.getName())
                .replaceAll("%31", getStatusText())
                //.replaceAll("%32", statusText) // FIXME
                .replaceAll("%33", mood.toString().toLowerCase())
                .replaceAll("%74%", String.valueOf(efficiencyPersentage));

    }

    private String getStatusText() {
        if (state != null) {
            switch (state) {
                case IDLE: {
                    return Utils.getMainTextResourceBundle().getString("2599");
                }
                case WORK: {
//                    if (assignedTask != null) {
//                        return assignedTask.getTooltip();
//                    }
                    return null;
                }
                case WANDER: {
                    return Utils.getMainTextResourceBundle().getString("2628");
                }
                case DEAD: {
                    return Utils.getMainTextResourceBundle().getString("2598");
                }
                case FLEE: {
                    return Utils.getMainTextResourceBundle().getString("2658");
                }
                case FIGHT: {
                    return Utils.getMainTextResourceBundle().getString("2651");
                }
                case DRAGGED:
                case UNCONSCIOUS: {
                    return Utils.getMainTextResourceBundle().getString("2655");
                }
                case STUNNED: {
                    return Utils.getMainTextResourceBundle().getString("2597");
                }
                case FOLLOW: {
                    return Utils.getMainTextResourceBundle().getString("2675");
                }
                case IMPRISONED: {
                    return Utils.getMainTextResourceBundle().getString("2674");
                }
                case TORTURED: {
                    return Utils.getMainTextResourceBundle().getString("2635");
                }
                case SLEEPING: {
                    return Utils.getMainTextResourceBundle().getString("2672");
                }
                case RECUPERATING: {
                    return Utils.getMainTextResourceBundle().getString("2667");
                }
            }
        }
        return "";
    }

    private boolean slap(short playerId) {
        // TODO: Direction & sound
        if (isSlappable(playerId)) {
            steeringBehavior = null;
            if (!applyDamage(creature.getAttributes().getSlapDamage())) {
                stateMachine.changeState(CreatureState.SLAPPED);
                playAnimation(creature.getAnimation(Creature.AnimationType.FALLBACK));
                playingAnimationType = AnimationType.OTHER;
            }

            // TODO: Listeners, telegrams, or just like this? I don't think nobody else needs to know this so this is the simplest...
//            worldState.getGameState().getPlayer(playerId).getStatsControl().creatureSlapped(creature);
            return true;
        }
        return false;
    }

    public void die() {
        stop();
        dropPosession();

        // Notify
        onDie(CreatureControl.this);
    }

    private void removeCreature() {

        // Unassing any tasks
        unassingCurrentTask();

        // Remove lair
        if (creatureLair != null) {
            creatureLair.removeObject();
        }

        Spatial us = getSpatial();
        us.removeFromParent();
    }

    private void updateAttributes(float tpf) {
        lastAttributeUpdateTime += tpf;
        if (lastAttributeUpdateTime >= 1) {
            lastAttributeUpdateTime -= 1;

            // Experience gaining, I don't know how accurate this should be, like clock the time in work animation etc.
            if (level < MAX_CREATURE_LEVEL) {
                if (playingAnimationType == AnimationType.WORK && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER) || stateMachine.isInState(CreatureState.FIGHT)) {
                    if (worldState.getLevelData().getImp().equals(creature)) {
                        experience += worldState.getLevelVariable(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND);
                    } else {
                        experience += experiencePerSecond;
                    }
                }
                if (experience >= experienceToNextLevel) {
                    experience -= experienceToNextLevel;
                    level++;
                    setAttributesByLevel();
                }
            }

            // Health
            if (ownLandHealthIncrease > 0 && !isIncapacitated() && isOnOwnLand()) {
                health += ownLandHealthIncrease; // FIXME, need to detect prev & current pos ??
                health = Math.min(health, maxHealth);
            } else if (stateMachine.isInState(CreatureState.TORTURED)) {
                applyDamage(Math.abs(creature.getAttributes().getTortureHpChange()));
            } else if (stateMachine.isInState(CreatureState.IMPRISONED)) {
                applyDamage((int) Math.abs(worldState.getLevelVariable(Variable.MiscVariable.MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND)));
            } else if (stateMachine.isInState(CreatureState.SLEEPING)
                    || stateMachine.isInState(CreatureState.RECUPERATING)) {
                health += (int) Math.abs(worldState.getLevelVariable(Variable.MiscVariable.MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND));
                health = Math.min(health, maxHealth);
            }
        }

        // Dying counter :)
        if (stateMachine.isInState(CreatureState.UNCONSCIOUS)
                && timeInState > worldState.getLevelVariable(Variable.MiscVariable.MiscType.DEAD_BODY_DIES_AFTER_SECONDS)) {
            stateMachine.changeState(CreatureState.DEAD);
        } else if (stateMachine.isInState(CreatureState.STUNNED)
                && timeInState > creature.getAttributes().getStunDuration()) {
            stateMachine.changeState(CreatureState.IDLE);
        }

        // Conversion counter
        if (stateMachine.isInState(CreatureState.TORTURED) && timeInState > tortureTimeToConvert) {
            convertCreature(worldState.getMapData().getTile(getCreatureCoordinates()).getPlayerId());
        }
    }

    /**
     * Convert the creature to player
     *
     * @param playerId the new owner
     */
    private void convertCreature(short playerId) {

    }

    private void setAttributesByLevel() {
        Map<Variable.CreatureStats.StatType, Variable.CreatureStats> stats = worldState.getLevelData().getCreatureStats(level);
        Attributes attributes = creature.getAttributes();
        height = attributes.getHeight() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.HEIGHT_TILES).getValue() : 100) / 100);
        int prevMaxHealth = maxHealth;
        maxHealth = attributes.getHp() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.HEALTH).getValue() : 100) / 100);
        health += Math.abs(maxHealth - prevMaxHealth); // Abs for some weird level configs :)
        fear = attributes.getFear() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.FEAR).getValue() : 100) / 100);
        threat = attributes.getThreat() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.THREAT).getValue() : 100) / 100);
        pay = attributes.getPay() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.PAY).getValue() : 100) / 100);
        maxGoldHeld = attributes.getMaxGoldHeld() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MAX_GOLD_HELD).getValue() : 100) / 100);
        hungerFill = attributes.getHungerFill() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.HUNGER_FILL_CHICKENS).getValue() : 100) / 100);
        manaGenPrayer = attributes.getManaGenPrayer() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MANA_GENERATED_BY_PRAYER_PER_SECOND).getValue() : 100) / 100);
        experienceToNextLevel = attributes.getExpForNextLevel() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_FOR_NEXT_LEVEL).getValue() : 100) / 100);
        experiencePerSecond = attributes.getExpPerSecond() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_PER_SECOND).getValue() : 100) / 100);
        experiencePerSecondTraining = attributes.getExpPerSecondTraining() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_FROM_TRAINING_PER_SECOND).getValue() : 100) / 100);
        researchPerSecond = attributes.getResearchPerSecond() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.RESEARCH_POINTS_PER_SECOND).getValue() : 100) / 100);
        manufacturePerSecond = attributes.getManufacturePerSecond() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MANUFACTURE_POINTS_PER_SECOND).getValue() : 100) / 100);
        decomposeValue = attributes.getDecomposeValue() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.DECOMPOSE_VALUE).getValue() : 100) / 100);
        speed = attributes.getSpeed() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.SPEED_TILES_PER_SECOND).getValue() : 100) / 100);
        runSpeed = attributes.getRunSpeed() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.RUN_SPEED_TILES_PER_SECOND).getValue() : 100) / 100);
        tortureTimeToConvert = attributes.getTortureTimeToConvert() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.TORTURE_TIME_TO_CONVERT_SECONDS).getValue() : 100) / 100);
        posessionManaCost = attributes.getPossessionManaCost() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.POSSESSION_MANA_COST_PER_SECOND).getValue() : 100) / 100);
        ownLandHealthIncrease = attributes.getOwnLandHealthIncrease() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.OWN_LAND_HEALTH_INCREASE_PER_SECOND).getValue() : 100) / 100);
        distanceCanHear = attributes.getDistanceCanHear() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.DISTANCE_CAN_HEAR_TILES).getValue() : 100) / 100);
        // TODO initialGoldHeld = attributes.getInitialGoldHeld() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.INITIAL_GOLD_HELD).getValue() : 100) / 100);

        meleeDamage = creature.getMeleeDamage() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MELEE_DAMAGE).getValue() : 100) / 100);
        meleeRecharge = creature.getMeleeRecharge() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS).getValue() : 100) / 100);

        // FIXME: We should know when we run and when we walk and set the speed
        // Steering
        setMaxLinearSpeed(speed);
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    @Override
    public short getOwnerId() {
        return ownerId;
    }

    /**
     * Navigate to the assigned task
     *
     * @return true if can navigate to assigned task
     */
    public boolean navigateToAssignedTask() {

//        Vector2f loc = assignedTask.getTarget(this);
//        workNavigationRequired = false;
//
//        if (loc != null) {
//            SteeringBehavior<Vector2> steering = CreatureSteeringCreator.navigateToPoint(worldState,
//                    this, this, WorldUtils.vectorToPoint(loc),
//                    assignedTask.isFaceTarget() ? assignedTask.getTaskLocation() : null);
//            if (steering != null) {
//                steering.setEnabled(!isAnimationPlaying());
//                setSteeringBehavior(steering);
//                return true;
//            }
//        }
        return false;
    }

    public void setAssignedTask(Task task) {

        // Unassign previous task
        unassingCurrentTask();

        assignedTask = task;
        workNavigationRequired = true;
    }

    public void unassingCurrentTask() {
//        if (assignedTask != null) {
//            assignedTask.unassign(this);
//        }
        assignedTask = null;
    }

    public Creature getCreature() {
        return creature;
    }

    public boolean isAtAssignedTaskTarget() {
        // FIXME: not like this, universal solution
//        return (assignedTask != null && assignedTask.getTarget(this) != null
//                && !workNavigationRequired && steeringBehavior == null
//                && isNear(assignedTask.getTarget(this)));
        return false;
    }

    public boolean isWorkNavigationRequired() {
        return workNavigationRequired;
    }

    public boolean isAssignedTaskValid() {
        // FIXME: yep
//        return (assignedTask != null && assignedTask.isValid(this));
        return false;
    }

    public boolean isStopped() {
        return (steeringBehavior == null);
    }

    private boolean isNear(Vector2f target) {
        return (target.distanceSquared(getSpatial().getWorldTranslation().x, getSpatial().getWorldTranslation().z) < 0.5f);
    }

    private boolean isSlappable(short playerId) {
        return playerId == ownerId && creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_SLAPPED) && !isIncapacitated();
    }

    public boolean isTooMuchGold() {
        return gold >= maxGoldHeld && isWorker();
    }

    public boolean dropGoldToTreasury() {
        if (gold > 0 && isWorker()) {
//            if (worldState.getTaskManager().assignGoldToTreasuryTask(this)) {
//                navigateToAssignedTask();
//                return true;
//            }
        }

        return false;
    }

    public void dropGold() {
        if (gold > 0) {

            // See if there is any gold at our feet to merge to
            // FIXME: What would be the best way...
            final int goldToSet = gold;
            for (ObjectControl objectControl : worldState.getThingLoader().getObjects()) {
                if (objectControl instanceof GoldObjectControl && objectControl.getState() == ObjectControl.ObjectState.NORMAL) {

                    // See distance
                    if (getSpatial().getWorldBound().collideWith(objectControl.getSpatial().getWorldBound()) > 0) {
                        GoldObjectControl goldObjectControl = (GoldObjectControl) objectControl;
                        worldState.getGameState().getApplication().enqueue(() -> {
                            goldObjectControl.setGold(goldObjectControl.getGold() + goldToSet);
                        });
                        gold = 0;
                        return;
                    }
                }
            }

            // No merging, just add loose gold
            Vector3f pos = spatial.getLocalTranslation();
            worldState.getGameState().getApplication().enqueue(() -> {
                worldState.getThingLoader().addLooseGold(new Vector2f(pos.x, pos.z), ownerId, goldToSet);
            });
            gold = 0;
        }
    }

    /**
     * Get the creature coordinates, in tile coordinates
     *
     * @return the tile coordinates
     */
    @Nullable
    public Point getCreatureCoordinates() {
        if (stateMachine.getCurrentState() != CreatureState.PICKED_UP) {
            Vector3f translation = getSpatial().getWorldTranslation();
            if (translation != null) {
                return WorldUtils.vectorToPoint(translation);
            }
        }
        return null;
    }

    /**
     * Get current creature gold amount
     *
     * @return the posessed gold
     */
    public int getGold() {
        return gold;
    }

    /**
     * Add gold to creature
     *
     * @param gold the amount of gold to add
     */
    public void addGold(int gold) {
        this.gold += gold;
    }

    /**
     * Remove gold from the creature
     *
     * @param gold the amount of gold to remove
     */
    public void substractGold(int gold) {
        this.gold -= gold;
    }

    public boolean isWorker() {
        return creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER);
    }

    public boolean findWork() {

        // See if we have some available work
        if (isWorker()) {
//            return (worldState.getTaskManager().assignTask(this, false));
        }

        // See that is there a prefered job for us
        // FIXME: moods
        List<Creature.JobPreference> jobs = new ArrayList<>();
        if (creature.getHappyJobs() != null) {
            for (Creature.JobPreference jobPreference : creature.getHappyJobs()) {
//                if (worldState.getTaskManager().isTaskAvailable(this, jobPreference.getJobType())) {
//                    jobs.add(jobPreference);
//                }
            }
        }

        // Choose
        if (!jobs.isEmpty()) {
//            return (worldState.getTaskManager().assignTask(this, chooseOnWeight(jobs).getJobType()));
        }

        return false;
    }

    private static Creature.JobPreference chooseOnWeight(List<Creature.JobPreference> items) {
        double completeWeight = 0.0;
        for (Creature.JobPreference item : items) {
            completeWeight += item.getChance();
        }
        double r = FastMath.rand.nextDouble() * completeWeight;
        double countWeight = 0.0;
        for (Creature.JobPreference item : items) {
            countWeight += item.getChance();
            if (countWeight >= r) {
                return item;
            }
        }
        return null;
    }

    /**
     * Finds a space for a lair, a task really
     *
     * @return true if a lair task is found
     */
    public boolean findLair() {
//        return (worldState.getTaskManager().assignClosestRoomTask(this, GenericRoom.ObjectType.LAIR));
        return false;
    }

    /**
     * Intsruct the creature to go to sleep, a task really
     *
     * @return true if a sleep task is found
     */
    public boolean goToSleep() {
//        return (worldState.getTaskManager().assignSleepTask(this));
        return false;
    }

    /**
     * Does the creature need a lair
     *
     * @return needs a lair
     */
    public boolean needsLair() {
        return creature.getAttributes().getTimeSleep() > 0;
    }

    /**
     * Does the creature have a lair
     *
     * @return has a lair
     */
    public boolean hasLair() {
        return creatureLair != null;
    }

    public void removeObject(ObjectControl object) {
        // TODO: basically we don't own except lair, but if we do, we need similar controls as the rooms have
        if (object.equals(creatureLair)) {
            creatureLair = null;
        }
    }

    public void setCreatureLair(ObjectControl creatureLair) {
        this.creatureLair = creatureLair;
        if (creatureLair != null) {
            creatureLair.setCreature(this);
        }
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public boolean hasObjective() {
        return objective != null && objective != Thing.HeroParty.Objective.NONE;
    }

    public Thing.HeroParty.Objective getObjective() {
        return objective;
    }

    public void setObjective(Thing.HeroParty.Objective objective) {
        this.objective = objective;
    }

    public void setObjectiveTargetPlayerId(short objectivePlayerId) {
        this.objectiveTargetPlayerId = objectivePlayerId;
    }

    public short getObjectiveTargetPlayerId() {
        return objectiveTargetPlayerId;
    }

    public ActionPoint getObjectiveTargetActionPoint() {
        return objectiveTargetActionPoint;
    }

    public void setObjectiveTargetActionPoint(ActionPoint objectiveTargetActionPoint) {
        this.objectiveTargetActionPoint = objectiveTargetActionPoint;
    }

    public boolean followObjective() {

        // See if we have some available work
//        return (worldState.getTaskManager().assignObjectiveTask(this, objective));
        return false;
    }

    /**
     * Get creature instance specifig flags
     *
     * @return set of creature flags
     */
    public EnumSet<Thing.Creature.CreatureFlag> getFlags() {
        return flags;
    }

    /**
     * Set follow mode
     *
     * @param target the target to follow
     * @return true if it is valid to follow it
     */
    public boolean followTarget(CreatureControl target) {
        if (target != null) {
            PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this);

            // Create proximity
            Array<CreatureControl> creatures;
            if (party != null) {
                creatures = new Array<>(party.getActualMembers().size());
                for (CreatureControl cr : party.getActualMembers()) {
                    creatures.add(cr);
                }
            } else {
                creatures = new Array<>(2);
                creatures.add(target);
                creatures.add(this);
            }

            // Hmm, proximity should be the same instance? Gotten from the party?
            // FIXME creature always moves on target and can`t stop
            Cohesion<Vector2> cohersion = new Cohesion<>(this, new InfiniteProximity<Vector2>(this, creatures));
            prioritySteering.add(cohersion);

            setSteeringBehavior(prioritySteering);
            return true;
        }
        return false;
    }

    public void setFollowTarget(CreatureControl followTarget) {
        this.followTarget = followTarget;
    }

    public CreatureControl getFollowTarget() {
        return followTarget;
    }

    @Override
    public int getHealth() {
        return health;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public float getHeight() {
        return height;
    }

    public Task getAssignedTask() {
        return assignedTask;
    }

    /**
     * Sets the target to follow to null. A cleanup method.
     */
    public void resetFollowTarget() {
        followTarget = null;
    }

    public void hideUnitFlower() {
        UnitFlowerControl.hideUnitFlower(this);
    }

    public void showUnitFlower() {
        showUnitFlower(null);
    }

    public void showUnitFlower(Integer seconds) {
        UnitFlowerControl.showUnitFlower(this, seconds);
    }

    @Override
    public void onHover() {
        showUnitFlower();
    }

    @Override
    public boolean isPickable(short playerId) {
        return (playerId == ownerId && !isIncapacitated()
                && creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_PICKED_UP))
                || ((stateMachine.isInState(CreatureState.IMPRISONED)
                || stateMachine.isInState(CreatureState.TORTURED))
                && worldState.getMapData().getTile(getCreatureCoordinates()).getPlayerId() == playerId);
    }

    @Override
    public boolean isInteractable(short playerId) {
        return isSlappable(playerId);
    }

    @Override
    public IInteractiveControl pickUp(short playerId) {

        // Stop everything
        stateMachine.changeState(CreatureState.PICKED_UP);
        unassingCurrentTask();
        steeringBehavior = null;
        setEnabled(false);

        // Remove from view
        getSpatial().removeFromParent();

        // TODO: Listeners, telegrams, or just like this? I don't think nobody else needs to know this so this is the simplest...
//        worldState.getGameState().getPlayer(playerId).getStatsControl().creaturePickedUp(creature);
        return this;
    }

    @Override
    public boolean interact(short playerId) {
        return slap(playerId);
    }

    @Override
    public CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    public ArtResource getInHandMesh() {
        return creature.getAnimInHandResource();
    }

    @Override
    public ArtResource getInHandIcon() {
        return creature.getIcon1Resource();
    }

    @Override
    public DroppableStatus getDroppableStatus(TileData tile, short playerId) {
        return (tile.getPlayerId() == playerId
                && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && !tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID) ? DroppableStatus.DROPPABLE : DroppableStatus.NOT_DROPPABLE);
    }

    @Override
    public void drop(TileData tile, Vector2f coordinates, IInteractiveControl control) {
        //TODO TileData tile = worldState.getMapData().getTile(coordinates);
        // TODO: actual dropping & being stunned, & evict (Imp to DHeart & creature to portal)
        CreatureLoader.setPosition(spatial, coordinates);
        worldState.getThingLoader().attachCreature(getSpatial());

        if (!detectImprisoningAndTorturing(tile.getLocation())) {
            spatial.addControl(new LandingControl(coordinates) {

                @Override
                public void onLanded() {
                    if (creature.getAttributes().getStunDuration() > 0) {
                        stateMachine.changeState(CreatureState.STUNNED);
                    } else {
                        stateMachine.changeState(CreatureState.IDLE);
                    }

                    CreatureControl.this.enabled = true;
                    CreatureControl.this.animationPlaying = false;
                }
            });

            // TODO: Listeners, telegrams, or just like this? I don't think nobody else needs to know this so this is the simplest...
//            worldState.getGameState().getPlayer(ownerId).getStatsControl().creatureDropped(creature);
        } else {
            CreatureControl.this.enabled = true;
            CreatureControl.this.animationPlaying = false;
        }
    }

    /**
     * Give an object to the creature
     *
     * @param obj the object to give
     * @return true if the creature accepts the object
     */
    public boolean giveObject(ObjectControl obj) {
        if (obj instanceof GoldObjectControl) {

            // Gold we gladly accept
            gold += ((GoldObjectControl) obj).getGold();
            return true;
        }

        // TODO: chickens etc..??
        return false;
    }

    public Set<CreatureControl> getVisibleCreatures() {
        if (!visibilityListUpdated) {

            // Get creatures we sense
            Point currentPoint = getCreatureCoordinates();
            // TODO: Every creature has hearing & vision 4, so I can just cheat this in, but should fix eventually
            // https://github.com/tonihele/OpenKeeper/issues/261
            if (currentPoint != null) {
                TileData tile = worldState.getMapData().getTile(currentPoint);
                if (tile != null) {
                    visibilityList.addAll(tile.getCreatures());
                    addVisibleCreatures(currentPoint, (int) creature.getAttributes().getDistanceCanHear());
                }
            }
            visibilityList.remove(this);
            visibilityListUpdated = true;
        }
        return visibilityList;
    }

    private void addVisibleCreatures(Point p, int range) {
        TileData tile = worldState.getMapData().getTile(p);
        if (tile == null || tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)
                || range-- < 0) {
            return;
        }

        visibilityList.addAll(tile.getCreatures());

        addVisibleCreatures(new Point(p.x + 1, p.y), range);
        addVisibleCreatures(new Point(p.x - 1, p.y), range);
        addVisibleCreatures(new Point(p.x, p.y + 1), range);
        addVisibleCreatures(new Point(p.x, p.y - 1), range);
    }

    /**
     * Get creature facing direction in map directions. FIXME: I don't really
     * like this enum to be used here, wrap it
     *
     * @return map facing direction
     */
    public Thing.Room.Direction getFacingDirection() {
        float orientation = getOrientation();
        return Thing.Room.Direction.NORTH;
    }

    /**
     * Should the creature flee or attack. Sets the AI state accordingly
     *
     * @return true if the creature should take a part in any of these actions
     */
    public boolean shouldFleeOrAttack() {

        // Check fleeing, TODO: Always flee?
        boolean inDHeart = false;
        if (!creature.getFlags().contains(Creature.CreatureFlag.IS_FEARLESS)) {
            int threatToUs = getEnemyThreat();
            int threatCaused = creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE)
                    || isHealthAtCriticalLevel() ? threat : getOurThreat();
            if (threatToUs - threatCaused > fear && (getFellowFighters() == 0
                    || creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE))) {

                // No longer flee from DHeart
                RoomInstance roomInstance = worldState.getMapLoader().getRoomCoordinates().get(getCreatureCoordinates());
                if (roomInstance == null || !worldState.getMapLoader().getRoomActuals().get(roomInstance).isDungeonHeart()) {
                    if (!stateMachine.isInState(CreatureState.FLEE)) {

                        stateMachine.changeState(CreatureState.FLEE);
                    }
                    return true;
                } else {
                    inDHeart = true;
                }
            }
        }

        // Should we attack, try to avoid i.e. imps engaging in a fight
        if ((creature.getFightStyle()
                != Creature.FightStyle.NON_FIGHTER || inDHeart) && getAttackTarget() != null) {
            if (!stateMachine.isInState(CreatureState.FIGHT)) {
                stateMachine.changeState(CreatureState.FIGHT);
            }
            return true;
        }

        return false;
    }

    private int getFellowFighters() {
        if (fellowFighters == null) {
            fellowFighters = 0;
            for (CreatureControl c : getVisibleCreatures()) {
                if (isAlly(c) && !c.isIncapacitated() && !c.equals(this) && c.getCreature().getFightStyle() != Creature.FightStyle.NON_FIGHTER) {
                    fellowFighters++;
                }
            }
        }
        return fellowFighters;
    }

    /**
     * Gets the total threat caused by the enemies visible
     *
     * @return total enemy threat
     */
    private int getEnemyThreat() {
        if (enemyThreat == null) {
            enemyThreat = 0;
            for (CreatureControl c : getVisibleCreatures()) {
                if (isEnemy(c) && !c.isIncapacitated()) {
                    enemyThreat += c.threat;
                }
            }
        }
        return enemyThreat;
    }

    /**
     * Gets the total threat caused by us. Meaning the band of brothers visible
     * to us
     *
     * @return total threat caused by us
     */
    private int getOurThreat() {
        if (ourThreat == null) {
            ourThreat = 0;
            for (CreatureControl c : getVisibleCreatures()) {
                if (isAlly(c) && !c.isIncapacitated()) {
                    ourThreat += c.threat;
                }
            }
        }
        return ourThreat;
    }

    public CreatureControl getAttackTarget() {
        if (attackTarget == null || attackTarget.isIncapacitated()
                || attackTarget.getStateMachine().getCurrentState() == CreatureState.FLEE) {

            // Pick a new target
            // TODO: is there any preference? Now just take the nearest
            CreatureControl nearestEnemy = null;
            float nearestDistance = Float.MAX_VALUE;
            for (CreatureControl c : getVisibleCreatures()) {
                if (isEnemy(c) && (!c.isIncapacitated() || c.getStateMachine().getCurrentState() == CreatureState.FLEE)) {
                    float distance = getDistanceToCreature(c);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestEnemy = c;
                    }
                }
            }
            attackTarget = nearestEnemy;
        }
        return attackTarget;
    }

    private boolean isEnemy(CreatureControl creature) {
        return worldState.getGameState().getPlayer(ownerId).isEnemy(creature.getOwnerId());
    }

    private boolean isAlly(CreatureControl creature) {
        return worldState.getGameState().getPlayer(ownerId).isAlly(creature.getOwnerId());
    }

    /**
     * Is the creature able to function
     *
     * @return true if the creature is down
     */
    public boolean isIncapacitated() {
        return getStateMachine().getCurrentState() == CreatureState.DEAD
                || getStateMachine().getCurrentState() == CreatureState.PICKED_UP
                || getStateMachine().getCurrentState() == CreatureState.UNCONSCIOUS
                || getStateMachine().getCurrentState() == CreatureState.IMPRISONED
                || getStateMachine().getCurrentState() == CreatureState.TORTURED
                || getStateMachine().getCurrentState() == CreatureState.DRAGGED
                || getStateMachine().getCurrentState() == CreatureState.RECUPERATING;
    }

    /**
     * Get distance to given creature
     *
     * @param creature the creature to measure distance to
     * @return the distance yo creature
     */
    public float getDistanceToCreature(CreatureControl creature) {

        // FIXME: now just direct distance, should be perhaps real distance that the creature needs to traverse to reach the target
        if (getPosition() == null || creature.getPosition() == null) {
            return Float.MAX_VALUE;
        }
        return getPosition().dst2(creature.getPosition());
    }

    public void navigateToAttackTarget(CreatureControl target) {
        Point p = WorldUtils.vectorToPoint(target.getSpatial().getWorldTranslation());
        //Point p = worldState.findRandomAccessibleTile(pTarget, 0, this);
        if (p != null) {

            SteeringBehavior<Vector2> steering = CreatureSteeringCreator.navigateToPoint(worldState, this, this, p);
            if (steering != null) {
                steering.setEnabled(!isAnimationPlaying());
                setSteeringBehavior(steering);
            }
        }
        /*
        PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this);

        // Seek to approach the target
        Seek<Vector2> seek = new Seek<>(this, target);
        prioritySteering.add(seek);

        setSteeringBehavior(prioritySteering);
         */
    }

    /**
     * See if we are withing attack distance of the given target
     *
     * @param target our target
     * @return if the distance is short enough for us to deliver a blow
     */
    public boolean isWithinAttackDistance(CreatureControl target) {
        float distanceNeeded = attacks.get(0).getRange(); // The melee range, the shortest range
        if (creature.getFightStyle() == Creature.FightStyle.SUPPORT) {

            // Get max distance we can cast all spells, and hopefully stay safe
            Float shortestDistance = null;
            for (CreatureAttack attack : attacks) {
                if (!attack.isMelee() && attack.isAvailable() && attack.isAttacking()) {
                    if (shortestDistance == null) {
                        shortestDistance = attack.getRange();
                    } else {
                        shortestDistance = Math.min(shortestDistance, attack.getRange());
                    }
                }
            }
            if (shortestDistance != null) {
                distanceNeeded = shortestDistance;
            }
        }
        return distanceNeeded >= getDistanceToCreature(target);
    }

    /**
     * Stop the creature from moving
     */
    public void stop() {
        setSteeringBehavior(null);
    }

    /**
     * Makes the creature attack the given target
     *
     * @param target target to attack
     */
    public void executeAttack(CreatureControl target) {

        // Select attack to execute
        // Hmm, just first one we can?
        float distanceToTarget = getDistanceToCreature(target);
        for (CreatureAttack attack : attacks) {
            if (attack.isExecutable() && attack.isAttacking() && distanceToTarget <= attack.getRange()) {
                executingAttack = attack;
                attack.execute(); // Mark for executing

                // FIXME: perhaps telegrams or whatnot and not instant like this, is it the shot delay that governs this?
                target.damage(this, attack.getDamage());
            }
        }
    }

    private void damage(CreatureControl damagedBy, float damage) {
        applyDamage((int) damage);
        attacked = true;

        // If we are not in a fight mode, see what we should do
        if (!isIncapacitated() && stateMachine.getCurrentState() != CreatureState.FIGHT) {
            attackTarget = damagedBy;
            if (stateMachine.getCurrentState() != CreatureState.FLEE) {
                shouldFleeOrAttack();
            }
        }
    }

    /**
     * Apply damage
     *
     * @param damage amount of damage
     * @return true if we dieded
     */
    private boolean applyDamage(int damage) {
        health -= Math.min(damage, health);
        if (health < 1) {

            // If we are in a party as a leader, we should give up our position
            if (party != null) {
                party.partyMemberIncapacitated(this);
            }

            // Die :(
            // If we are the objective to kill, we'll die immidiately
            if (stateMachine.isInState(CreatureState.TORTURED)
                    || stateMachine.isInState(CreatureState.IMPRISONED)) {
                stateMachine.changeState(CreatureState.DEAD);
                if (stateMachine.isInState(CreatureState.IMPRISONED)) {
                    // FIXME: Create a skeleton, IF we have the capacity
                }
            } else if ((getPlayerObjective() == null || getPlayerObjective() != ObjectiveType.KILL)
                    && creature.getFlags().contains(Creature.CreatureFlag.GENERATE_DEAD_BODY)) {
                stateMachine.changeState(CreatureState.UNCONSCIOUS);
            } else {
                stateMachine.changeState(CreatureState.DEAD);
            }

            return true;
        }
        return false;
    }

    /**
     * If we see enemies, avoid them, try to navigate to our dungeon heart
     */
    public void flee() {
        //PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this, 0.0001f);

        // Get the nearest enemy
        // FIXME: method naming if truly nearest enemy, and perhaps we should flee from our assailant
//        CreatureControl target = getAttackTarget();
//
//        // Flee from the enemy
//        if (target != null) {
//            Flee<Vector2> flee = new Flee<>(this, target);
//            prioritySteering.add(flee);
//        }
        // FIXME: For now just flee towards the dungeon heart or random tiles
        GenericRoom room = null;
//                worldState.getGameState().getPlayer(ownerId).getRoomControl().getDungeonHeart();
        if (room != null) {
            SteeringBehavior<Vector2> steering = CreatureSteeringCreator.navigateToPoint(worldState, this, this, room.getRoomInstance().getCoordinates().get(0));
            if (steering != null) {
                steering.setEnabled(!isAnimationPlaying());
                setSteeringBehavior(steering);
                return;
            }
        }

        navigateToRandomPoint();

        // Try to find our dungeon heart etc. safety haven
        //setSteeringBehavior(prioritySteering);
    }

    /**
     * Checks if we should fear death
     *
     * @return true if we have critically low health level
     */
    public boolean isHealthAtCriticalLevel() {
        return worldState.getLevelVariable(Variable.MiscVariable.MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX) > getHealthPercentage();
    }

    /**
     * Is the creature on own land
     *
     * @return is on own land
     */
    protected boolean isOnOwnLand() {
        Point p = getCreatureCoordinates();
        if (p != null) {
            TileData tileData = worldState.getMapData().getTile(p);
            if (tileData != null) {
                return tileData.getPlayerId() == getOwnerId();
            }
        }
        return false;
    }

    /**
     * Attaches portal gem to the creature
     */
    public void attachPortalGem() {
        hasPortalGem = true;

        // I don't really know how this is determined, but this is the easiest way and needs no creatures IDs etc.
        // Set the lord of the land name, you can't even edit is in the editor, so official campaign only
        if (worldState.getGameState().getLevelData().getGameLevel().getHeroName() != null) {
            name = worldState.getGameState().getLevelData().getGameLevel().getHeroName();
        }
    }

    /**
     * Has the crature got the portal gem
     *
     * @return true if posesses the portal gem
     */
    public boolean isPortalGem() {
        return hasPortalGem;
    }

    /**
     * Drops the creature posession
     */
    private void dropPosession() {
        dropGold();
        dropPortalGem();
    }

    private void dropPortalGem() {
        if (hasPortalGem) {
            Point p = getCreatureCoordinates();
            worldState.getGameState().getApplication().enqueue(() -> {
                worldState.getThingLoader().addObject(p, worldState.getLevelData().getLevelGem().getObjectId(), ownerId);
            });
        }
    }

    /**
     * Sets the creature as a player objective
     *
     * @param objectiveType the objective type
     */
    public void setPlayerObjective(ObjectiveType objectiveType) {
        playerObjective = objectiveType;
    }

    /**
     * Get player objective for this creature
     *
     * @return player objective
     */
    public ObjectiveType getPlayerObjective() {
        return playerObjective;
    }

    /**
     * Is the creature dead
     *
     * @return dead or alive
     */
    public boolean isDead() {
        return stateMachine.isInState(CreatureState.DEAD);
    }

    /**
     * Is the creature picked up
     *
     * @return hanging
     */
    public boolean isPickedUp() {
        return stateMachine.isInState(CreatureState.PICKED_UP);
    }

    /**
     * Is the creature unconscious, or rather dying
     *
     * @return hanging on a thread
     */
    public boolean isUnconscious() {
        return stateMachine.isInState(CreatureState.UNCONSCIOUS);
    }

    /**
     * Is the creature stunned
     *
     * @return seeing stars
     */
    public boolean isStunned() {
        return stateMachine.isInState(CreatureState.STUNNED);
    }

    /**
     * Is the creature imprisoned
     *
     * @return being imprisoned
     */
    public boolean isImprisoned() {
        return stateMachine.isInState(CreatureState.IMPRISONED);
    }

    /**
     * Is the creature being tortured, hopefully by those sexy pixelly
     * mistresses
     *
     * @return being tortured
     */
    public boolean isTortured() {
        return stateMachine.isInState(CreatureState.TORTURED);
    }

    /**
     * Is the creature being attacked
     *
     * @return being attacked
     */
    public boolean isAttacked() {
        return attacked;
    }

    @Override
    public boolean canFly() {
        return creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY);
    }

    @Override
    public boolean canWalkOnWater() {
        return creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_WATER);
    }

    @Override
    public boolean canWalkOnLava() {
        return creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA);
    }

    @Override
    public boolean canMoveDiagonally() {
        return true;
    }

    /**
     * Get creature lair location
     *
     * @return the creature lair location
     */
    public Point getLairLocation() {
        if (creatureLair != null) {
            return creatureLair.getTile().getLocation();
        }
        return null;
    }

    /**
     * Is the creature being dragged
     *
     * @return is dragged
     */
    public boolean isDragged() {
        return stateMachine.isInState(CreatureState.DRAGGED);
    }

    /**
     * Set the creature to sleep until healed
     */
    public void sleepUntilHealed() {
        animationPlaying = false;
        hideUnitFlower();
        stateMachine.changeState(CreatureState.RECUPERATING);
    }

    /**
     * Sets an object for us to haul
     *
     * @param haulable the haulable object, null for cancelling the hauling
     */
    public void setHaulable(IHaulable haulable) {
        if (haulable != null && !haulable.equals(hauling)) {
            haulable.haulingStarted();
        } else if (hauling != null && !hauling.equals(haulable)) {
            hauling.haulingEnded();
        }
        hauling = haulable;
    }

    /**
     * Get the object that the creature is hauling
     *
     * @return hauled object
     */
    public IHaulable getHaulable() {
        return hauling;
    }

    @Override
    public void haulingStarted() {
        stateMachine.changeState(CreatureState.DRAGGED);
    }

    @Override
    public void haulingEnded() {
        if (stateMachine.isInState(CreatureState.DRAGGED)) {
            stateMachine.revertToPreviousState();
        }
    }

    @Override
    public void updatePosition(Vector3f position) {
        getSpatial().setLocalTranslation(position);
        setPositionFromSpatial();
    }

    /**
     * Is the creature in full health
     *
     * @return true if fully healed
     */
    public boolean isFullHealth() {
        return health >= maxHealth;
    }

    /**
     * Has the creature slept enough
     *
     * @return should we wake up
     */
    public boolean isEnoughSleep() {
        return timeInState >= creature.getAttributes().getTimeSleep() && isFullHealth();
    }

    /**
     * Has the creature been awake for too long
     *
     * @return should we go to sleep
     */
    public boolean isNeedForSleep() {
        return needsLair() && (timeAwake >= creature.getAttributes().getTimeAwake()
                || worldState.getLevelVariable(Variable.MiscVariable.MiscType.CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH) > getHealthPercentage());
    }

    /**
     * Instructs the creature to sleep
     */
    public void sleep() {
        stateMachine.changeState(CreatureState.SLEEPING);
    }

    /**
     * Sets the creature as imprisoned
     */
    public void imprison() {
        hideUnitFlower();
        animationPlaying = false;
        health = (int) Math.floor(maxHealth * 0.2f); // Return health to 20%
        stateMachine.changeState(CreatureState.IMPRISONED);
    }

    public void levelUp() {
        experience = experienceToNextLevel;
    }

    public void levelMax() {
        if (level < MAX_CREATURE_LEVEL) {
            level = MAX_CREATURE_LEVEL;
            setAttributesByLevel();
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
