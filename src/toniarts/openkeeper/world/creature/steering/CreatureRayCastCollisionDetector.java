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
package toniarts.openkeeper.world.creature.steering;

import com.badlogic.gdx.ai.utils.Collision;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import java.awt.Point;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;

/**
 * Collision detector for movement<br>
 * Reference:
 * http://www.codeproject.com/Articles/15604/Ray-casting-in-a-D-tile-based-environment
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureRayCastCollisionDetector implements RaycastCollisionDetector<Vector2> {

    private final WorldState worldState;

    public CreatureRayCastCollisionDetector(WorldState worldState) {
        this.worldState = worldState;
    }

    @Override
    public boolean collides(Ray<Vector2> ray) {
        return findCollision(null, ray);
    }

    @Override
    public boolean findCollision(Collision<Vector2> outputCollision, Ray<Vector2> inputRay) {

        if (!inputRay.start.epsilonEquals(inputRay.end, MathUtils.FLOAT_ROUNDING_ERROR)) {
            return false;
        }

        // Don't do a real ray cast, 2D this
        Point collisionPoint = getCollisionWall((int) Math.floor(inputRay.start.x), (int) Math.floor(inputRay.start.y), (int) Math.floor(inputRay.end.x + 1), (int) Math.floor(inputRay.end.y + 1));

        if (collisionPoint != null) {

            Line line = new Line(new Vector3f(inputRay.start.x, 0.25f, inputRay.start.y), new Vector3f(inputRay.end.x, 0.25f, inputRay.end.y));

            Material orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            orange.setColor("Color", ColorRGBA.Red);
            orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            orange.getAdditionalRenderState().setLineWidth(2);

            Geometry geometry = new Geometry("Bullet", line);
            geometry.setCullHint(Spatial.CullHint.Never);
            geometry.setMaterial(orange);
            worldState.getWorld().attachChild(geometry);

            if (outputCollision != null) {

                // FIXME: we could somehow 2D this, or 3D physics this
                com.jme3.math.Ray r = new com.jme3.math.Ray();
                r.setOrigin(new Vector3f(inputRay.start.x, 0.5f, inputRay.start.y));
                Vector2 v = inputRay.end.sub(inputRay.start);
                r.setDirection(new Vector3f(v.x, 0, v.y));
                CollisionResults collisions = new CollisionResults();

                Spatial tile = worldState.getMapLoader().getTileWall(collisionPoint);
                tile.collideWith(r, collisions);
//                r.collideWith(geometry, result);

//                outputCollision.point = new Vector2(result.getClosestCollision().getContactPoint().x, result.getClosestCollision().getContactPoint().z);
//                outputCollision.normal = new Vector2(result.getClosestCollision().getContactNormal().x, result.getClosestCollision().getContactNormal().z);
//                line = new Line(new Vector3f(outputCollision.point.x, 1.25f, outputCollision.point.y), new Vector3f(outputCollision.point.x + 1, 1.25f, outputCollision.point.y));
//                line = new Line(new Vector3f(collisionPoint.x1, 1.05f, collisionPoint.y1), new Vector3f(collisionPoint.x2, 1.05f, collisionPoint.y2));
//                line.setLineWidth(2);
                CollisionResult result = collisions.getClosestCollision();
                if (result != null) {
                    outputCollision.point = new Vector2(result.getContactPoint().x, result.getContactPoint().z);
                    outputCollision.normal = new Vector2(result.getContactNormal().x, result.getContactNormal().z);
                    if (Main.isDebug()) {
                        worldState.getApp().enqueue(() -> {
                            Material m = ((Geometry)tile).getMaterial();
                            m.setColor("Ambient", ColorRGBA.Orange);
                            m.setBoolean("UseMaterialColors", true);
                        });
                    }
                }
            }

            return true;
        } else {
            Line line = new Line(new Vector3f(inputRay.start.x, 0.25f, inputRay.start.y), new Vector3f(inputRay.end.x, 0.25f, inputRay.end.y));

            Material orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            orange.setColor("Color", ColorRGBA.Green);
            orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            orange.getAdditionalRenderState().setLineWidth(2);

            Geometry geometry = new Geometry("Bullet", line);
            geometry.setCullHint(Spatial.CullHint.Never);
            geometry.setMaterial(orange);
//            world.getWorld().attachChild(geometry);
        }

        return false;
    }

    private Point getCollisionWall(int x1, int y1, final int x2, final int y2) {

        // Don't analyze the current point
        // Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int diff = dx - dy;

        int xStep = (x1 < x2 ? 1 : -1);
        int yStep = (y1 < y2 ? 1 : -1);

        int prevX = x1;
        int prevY = y1;

        while ((x1 != x2) || (y1 != y2)) {

            int p = 2 * diff;
            if (p > -dy) {
                diff = diff - dy;
                x1 += xStep;
            }
            if (p < dx) {
                diff = diff + dx;
                y1 += yStep;
            }

            // Return the point if it is not accessible
            if (!isPointAccessible(x1, y1)) {
                return new Point(x1, y1);
            }
            prevX = x1;
            prevY = y1;
        }
        return null;
    }

    private boolean isPointAccessible(int x, int y) {
        TileData tile = worldState.getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }

        return !tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

}
