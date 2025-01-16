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
package toniarts.openkeeper.world.effect;

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Light;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Color;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.TileData;

/**
 * An effect & its elements, or a tree of effects & their elements. The logic is
 * quite here now, how the effects are chained and what they do, but:<br>
 * TODO
 * <ul>
 * <li>We probably need our own particle emitter, the stock wont probably do..
 * Just pass the Effect & EffectElement to our custom one and boom</li>
 * <li>Maybe cache the emitters?</li>
 * </ul>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class VisualEffect {
    
    private static final Logger logger = System.getLogger(VisualEffect.class.getName());

    private final Effect effect;
    private final Map<EffectElement, Spatial> effectElements;
    private final List<VisualEffect> effects;
    private final Node effectNode;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final EffectManagerState effectManagerState;
    private boolean infinite;
    private PointLight light;

    public VisualEffect(EffectManagerState effectManagerState, Node node, Effect effect) {
        this(effectManagerState, node, null, effect, false);
    }

    public VisualEffect(EffectManagerState effectManagerState, Node node, Vector3f location, Effect effect, boolean infinite) {
        this.effect = effect;
        this.kwdFile = effectManagerState.getKwdFile();
        this.assetManager = effectManagerState.getAssetManger();
        this.effectManagerState = effectManagerState;
        this.infinite = infinite;

        // Create the lists
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            effects = new ArrayList<>();
            effectElements = HashMap.newHashMap(effect.getGenerateIds().size());
        } else {
            effects = new ArrayList<>(effect.getGenerateIds().size());
            effectElements = Collections.emptyMap();
        }

        // Attach to scene graph
        effectNode = new Node(effect.getName());
        if (location != null) {
            effectNode.setLocalTranslation(location);
        }
        node.attachChild(effectNode); // We need to attach before emiting, it doesn't work otherwise

        // Load the effect
        load();
    }

    private void load() {
        ArtResource resource = effect.getArtResource();

        Spatial model = new Node();
        if (resource != null) {
            switch (resource.getType()) {
                case MESH:
                case ANIMATING_MESH:
                    model = AssetUtils.loadModel(assetManager, resource.getName(), resource);
                    break;

                case PROCEDURAL_MESH:
                    model = AssetUtils.createProceduralMesh(resource);
                    break;

                case ALPHA:
                case ADDITIVE_ALPHA:
                case SPRITE:
                    EffectGeometry g = new EffectGeometry("effect");
                    g.setFrames(Math.max(1, resource.getData(ArtResource.KEY_FRAMES)));

                    Material material = AssetUtils.createParticleMaterial(resource, assetManager);
                    g.setMaterial(material);

                    ((Node) model).attachChild(g);
                    break;

                default:
                    logger.log(Level.WARNING, "Not supported effect type {0}", resource.getType());
            }

            if (resource.getType() == ArtResourceType.MESH) {
                model.setLocalScale(resource.getData(ArtResource.KEY_SCALE));

            } else if (resource.getType() == ArtResourceType.ANIMATING_MESH) {

                AnimControl animControl = (AnimControl) model.getControl(AnimControl.class);
                if (animControl != null) {
//                    AnimChannel channel = animControl.getChannel(0);
//                    channel.setAnim(ANIM_NAME);
//                    resource.getData(ArtResource.KEY_FPS);
//                    resource.getData(ArtResource.KEY_FRAMES);
//                    channel.setSpeed(speed);
//                    channel.setTime(time);
                    animControl.setEnabled(true);
                }
            }

            model.addControl(new EffectControl(effect) {

                @Override
                public void onDie(Vector3f location) {
                    if (effect.getDeathEffectId() != 0) {
                        VisualEffect.this.addEffect(effect.getDeathEffectId(), location);
                    }
                }

                @Override
                public void onHit(Vector3f location) {
                    TileData tile = effectManagerState.getWorldState().getMapData().getTile(WorldUtils.vectorToPoint(location));
                    if (tile == null) {
                        logger.log(Level.WARNING, "Effect hit error");
                        return;
                    }

                    if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.LAVA)
                            && effect.getHitLavaEffectId() != 0) {
                        VisualEffect.this.addEffect(effect.getHitLavaEffectId(), location);
                    } else if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.WATER)
                            && effect.getHitWaterEffectId() != 0) {
                        VisualEffect.this.addEffect(effect.getHitWaterEffectId(), location);
                    } else if (effect.getHitSolidEffectId() != 0) {
                        // && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                        if (effect.getFlags().contains(Effect.EffectFlag.DIE_WHEN_HIT_SOLID)) {
                            onDie(location);
                        } else {
                            VisualEffect.this.addEffect(effect.getHitSolidEffectId(), location);
                        }
                    }
                }
            });
            effectNode.attachChild(model);
        }

        // Light
        light = getLight(effect.getLight());
        if (light != null) {
            model.addLight(light);
        }

        // Elements/effects
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            for (Integer id : effect.getGenerateIds()) {
                addEffectElement(id, null);
            }
        } else if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECTS)) {
            for (Integer id : effect.getGenerateIds()) {
                addEffect(id, null);
            }
        }

        // The next effect is chaining the effects, they'll start immediately
        if (effect.getNextEffectId() != 0) {
            addEffect(effect.getNextEffectId(), null);
        }
    }

    private void addEffect(Integer id, Vector3f location) {
        VisualEffect visualEffect = new VisualEffect(effectManagerState, effectNode, location, kwdFile.getEffect(id), false);
        effects.add(visualEffect);
        effectNode.attachChild(visualEffect.effectNode);
    }

    private void addEffectElement(Integer id, Vector3f location) {
        EffectElement effectElement = kwdFile.getEffectElement(id);
        Spatial emitter = loadElement(effectElement);
        if (emitter != null) {
            if (location != null) {
                emitter.setLocalTranslation(location);
            }
            effectElements.put(effectElement, emitter);
            effectNode.attachChild(emitter);
            if (emitter instanceof ParticleEmitter) {
                ((ParticleEmitter) emitter).emitAllParticles();
            }
        }

        // The next effect is chaining the effects, they'll start immediately
        if (effectElement.getNextEffectId() != 0) {
            addEffect(effectElement.getNextEffectId(), null);
        }
    }

    private Spatial loadElement(EffectElement element) {
        ArtResource resource = element.getArtResource();

        if (effect.getGenerationType() == Effect.GenerationType.CUBE_GEN) {
            //emitter.setShape(new EmitterSphereShape(new Vector3f(), 1));
        } else if (effect.getGenerationType() == Effect.GenerationType.NONE) {
            return null;
        }

        if (resource == null) {
            return null;
        }

        switch (resource.getType()) {
            case ALPHA:
            case ADDITIVE_ALPHA:
            case SPRITE: {
                ParticleEmitter emitter = new ParticleEmitter(element.getName(),
                        ParticleMesh.Type.Triangle,
                        effect.getElementsPerTurn());
                emitter.setParticlesPerSec(0);
                Material material = AssetUtils.createParticleMaterial(resource, assetManager);
                emitter.setMaterial(material);
                emitter.setImagesX(Math.max(1, resource.getData(ArtResource.KEY_FRAMES)));
                emitter.setImagesY(1);
                emitter.setSelectRandomImage(resource.getFlags().contains(ArtResource.ArtResourceFlag.RANDOM_START_FRAME));
                emitter.setInWorldSpace(false);

                Color color = element.getColor();
                float alpha = 1f;
                if (element.getFlags().contains(EffectElement.EffectElementFlag.FADE)) {
                    alpha -= element.getFadePercentage() / 100;
                }

                emitter.setStartColor(new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f));
                emitter.setEndColor(new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha));
                //
                Vector3f velocity = EffectControl.calculateVelocity(element);
                emitter.getParticleInfluencer().setInitialVelocity(velocity);
                //
                float scaleRatio = element.getScaleRatio() == 0 ? 1 : element.getScaleRatio();
                if (element.getFlags().contains(EffectElement.EffectElementFlag.SHRINK)) {
                    emitter.setEndSize(element.getMaxScale());
                    emitter.setStartSize(element.getMinScale());
                } else {
                    emitter.setStartSize(element.getMaxScale());
                    emitter.setEndSize(element.getMinScale());
                }
                //
                emitter.setFacingVelocity(element.getFlags().contains(EffectElement.EffectElementFlag.ROTATE_TO_MOVEMENT_DIRECTION));
                //
                emitter.setGravity(0, element.getMass() * element.getAirFriction(), 0);
                emitter.setLowLife(element.getMinHp() / 10f);
                emitter.setHighLife(element.getMaxHp() / 10f);
                //
                float delta = Math.max((element.getMaxSpeedXy() - element.getMinSpeedXy()) / (element.getMaxSpeedXy() + 1),
                        (element.getMaxSpeedYz() - element.getMinSpeedYz()) / (element.getMaxSpeedYz() + 1));
                emitter.getParticleInfluencer().setVelocityVariation(delta);

                return emitter;
            }

            case MESH:
            case ANIMATING_MESH:
            case PROCEDURAL_MESH: {
                EffectEmitter emitter = new EffectEmitter(element, effect) {

                    @Override
                    public void onDeath(Vector3f location) {
                        if (element.getDeathElementId() != 0) {
                            VisualEffect.this.addEffectElement(element.getDeathElementId(), location);
                        }
                    }

                    @Override
                    public void onHit(Vector3f location) {
                        TileData tile = effectManagerState.getWorldState().getMapData().getTile(WorldUtils.vectorToPoint(location));
                        if (tile == null) {
                            logger.log(Level.WARNING, "Effect hit error");
                            return;
                        }

                        if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.LAVA)
                                && element.getHitLavaElementId() != 0) {
                            VisualEffect.this.addEffectElement(element.getHitLavaElementId(), location);
                        } else if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.WATER)
                                && element.getHitWaterElementId() != 0) {
                            VisualEffect.this.addEffectElement(element.getHitWaterElementId(), location);
                        } else if (element.getHitSolidElementId() != 0) {
                            // && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                            if (element.getFlags().contains(EffectElement.EffectElementFlag.DIE_WHEN_HIT_SOLID)) {
                                onDeath(location);
                            } else {
                                VisualEffect.this.addEffectElement(element.getHitSolidElementId(), location);
                            }
                        }
                    }
                };

                Node model;
                if (resource.getType() == ArtResourceType.PROCEDURAL_MESH) {
                    model = (Node) AssetUtils.createProceduralMesh(resource);
                } else {
                    model = (Node) AssetUtils.loadModel(assetManager, resource.getName(), resource);
                }

                if (resource.getType() == ArtResourceType.MESH) {
                    model.setLocalScale(resource.getData(ArtResource.KEY_SCALE));

                } else if (resource.getType() == ArtResourceType.ANIMATING_MESH) {

                    AnimControl animControl = (AnimControl) model.getControl(AnimControl.class);
                    if (animControl != null) {
//                        AnimChannel channel = animControl.getChannel(0);
//                        channel.setAnim(ANIM_NAME);
//                        resource.getData(ArtResource.KEY_FPS);
//                        resource.getData(ArtResource.KEY_FRAMES);
//                        channel.setSpeed(speed);
//                        channel.setTime(time);
                        animControl.setEnabled(true);
                    }
                }
                emitter.setSpatial(model);
                return emitter;
            }

            default:
                logger.log(Level.WARNING, "Not supported effect element type {0}", resource.getType());
        }

        return null;
    }

    private PointLight getLight(Light effectLight) {
        if (effectLight == null) {
            return null;
        }

        PointLight realLight = new PointLight();
        realLight.setColor(new ColorRGBA(effectLight.getColor().getRed() / 255f,
                effectLight.getColor().getGreen() / 255f,
                effectLight.getColor().getBlue() / 255f,
                effectLight.getColor().getAlpha() / 255f));
        realLight.setRadius(effectLight.getRadius());
        realLight.setPosition(new Vector3f(effectLight.getmKPos().x, effectLight.getmKPos().y, effectLight.getmKPos().z));
        return realLight;
    }

    /**
     * Regular style update, but also signals whether this effect has come to an
     * end
     *
     * @param tpf the update time
     * @return true if the effect is still valid, false if the effect has died
     */
    public boolean update(float tpf) {

        // Update the child effects
        Iterator<VisualEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            VisualEffect visualEffect = iterator.next();
            if (!visualEffect.update(tpf)) {
                iterator.remove();
            }
        }

        // Check the elements
        Iterator<Entry<EffectElement, Spatial>> iter = effectElements.entrySet().iterator();
        List<Integer> deathEffectElements = null;
        while (iter.hasNext()) {
            Entry<EffectElement, Spatial> entry = iter.next();
            if (entry.getValue() instanceof ParticleEmitter) {
                if (((ParticleEmitter) entry.getValue()).getNumVisibleParticles() == 0) {

                    // Kill
                    entry.getValue().removeFromParent();
                    iter.remove();

                    // Attach on death element
                    if (entry.getKey().getDeathElementId() != 0) {
                        if (deathEffectElements == null) {
                            deathEffectElements = new ArrayList<>();
                        }
                        deathEffectElements.add(entry.getKey().getDeathElementId());
                    }
                }
            }
        }

        // Init the death elements
        if (deathEffectElements != null) {
            for (Integer id : deathEffectElements) {
                addEffectElement(id, null);
            }
        }

        // If the whole effect has died, create the death effect
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS) && effectElements.isEmpty()) {
            if (effect.getDeathEffectId() != 0) {
                addEffect(effect.getDeathEffectId(), null);
            }
        }

        // If no children at all, remove us
        if (effectElements.isEmpty() && effects.isEmpty() && effectNode.getQuantity() == 0) {

            // If infitine, just restart
            if (infinite) {
                if (light != null) {
                    effectNode.removeLight(light);
                }
                load();
            } else {
                effectNode.removeFromParent();
                return false;
            }
        }
        return true;
    }

    public void removeEffect() {
        effects.clear();
        effectElements.clear();
        infinite = false;
    }
}
