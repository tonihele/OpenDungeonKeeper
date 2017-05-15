package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.RoomUtils;
import toniarts.openkeeper.world.MapLoader;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.when;

/**
 * Created by wietse on 13/05/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( AssetUtils.class )
public class TempleTest {


    private AssetManager assetManager;

    @Test
    public void verifyThatAFullSquareResultsInAllTwos() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", true,true, true, true, true,true, true, true, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple13");
        expected.add("Temple13");
        expected.add("Temple13");
        expected.add("Temple13");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void verifyNorthAndEast() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", true,false, true, false, false,false, false, false, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple0");
        expected.add("Temple2");
        expected.add("Temple1");
        expected.add("Temple0");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(FastMath.HALF_PI);
        rotationAnglesExpected.add(FastMath.PI);
        rotationAnglesExpected.add(FastMath.PI);
        rotationAnglesExpected.add(FastMath.PI);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void verifyNorthAndEastAndNorthEast() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", true,true, true, false, false,false, false, false, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple0");
        expected.add("Temple2");
        expected.add("Temple1");
        expected.add("Temple0");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(FastMath.HALF_PI);
        rotationAnglesExpected.add(-FastMath.HALF_PI);
        rotationAnglesExpected.add(FastMath.PI);
        rotationAnglesExpected.add(FastMath.PI);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void verifySouthAndWest() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", false,false, false, false, true,false, true, false, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple0");
        expected.add("Temple1");
        expected.add("Temple2");
        expected.add("Temple0");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(-FastMath.HALF_PI);
        rotationAnglesExpected.add(-FastMath.HALF_PI);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,-MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(-MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));
        vectorsExpected.add(new Vector3f(MapLoader.TILE_WIDTH / 4,0,MapLoader.TILE_WIDTH / 4));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void testLShapedTemple() {
        boolean[][] ground = {{true, true, true, true, true, true},
                              {true, true, true, true, true, true},
                              {true, true, true, true, true, true},
                              {true, true, true, false, false, false},
                              {true, true, true, false, false, false},
                              {true, true, true, false, false, false}};

        boolean[][] waterAreaExpected = {{false, false, false, false, false, false},
                                         {false, true, true, true, true, false},
                                         {false, true, false, false, false, false},
                                         {false, true, false, false, false, false},
                                         {false, true, false, false, false, false},
                                         {false, false, false, false, false, false}};



        boolean[][] waterAreaActual = RoomUtils.calculateWaterArea(ground);

        for(int i = 0; i < waterAreaActual.length; ++i ) {
            for(int j = 0; j < waterAreaActual[0].length; ++j ) {
                System.out.print(waterAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(waterAreaExpected,waterAreaActual);
    }

    @Test
    public void testThreeByThreeSquareWithAHoleTempleHasNoWater() {
        boolean[][] ground = {{true, true, true},
                              {true, false, true},
                              {true, true, true}};

        boolean[][] waterAreaExpected = {{false, false, false},
                                         {false, false, false},
                                         {false, false, false}};



        boolean[][] waterAreaActual = RoomUtils.calculateWaterArea(ground);

        for(int i = 0; i < waterAreaActual.length; ++i ) {
            for(int j = 0; j < waterAreaActual[0].length; ++j ) {
                System.out.print(waterAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(waterAreaExpected,waterAreaActual);
    }

    @Test
    public void testThreeByThreeSquareTempleHasOneWaterInTheCenter() {
        boolean[][] ground = {{true, true, true},
                              {true, true, true},
                              {true, true, true}};

        boolean[][] waterAreaExpected = {{false, false, false},
                                         {false, true, false},
                                         {false, false, false}};



        boolean[][] waterAreaActual = RoomUtils.calculateWaterArea(ground);

        for(int i = 0; i < waterAreaActual.length; ++i ) {
            for(int j = 0; j < waterAreaActual[0].length; ++j ) {
                System.out.print(waterAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(waterAreaExpected,waterAreaActual);
    }

    @Test
    public void testFourByFourSquareTempleHasTwoByTwoWaterInTheCenter() {
        boolean[][] ground = {{true, true, true, true},
                              {true, true, true, true},
                              {true, true, true, true},
                              {true, true, true, true}};

        boolean[][] waterAreaExpected = {{false, false, false, false},
                                         {false, true, true, false},
                                         {false, true, true, false},
                                         {false, false, false, false}};



        boolean[][] waterAreaActual = RoomUtils.calculateWaterArea(ground);

        for(int i = 0; i < waterAreaActual.length; ++i ) {
            for(int j = 0; j < waterAreaActual[0].length; ++j ) {
                System.out.print(waterAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(waterAreaExpected,waterAreaActual);
    }

    @Test
    public void testFourByThreeWithTwoEmptyTilesRectangleTempleHasOneByOneWater() {
        boolean[][] ground = {{true, false, false},
                              {true, true, true},
                              {true, true, true},
                              {true, true, true}};

        boolean[][] waterAreaExpected = {{false, false, false},
                                         {false, false, false},
                                         {false, true, false},
                                         {false, false, false}};



        boolean[][] waterAreaActual = RoomUtils.calculateWaterArea(ground);

        for(int i = 0; i < waterAreaActual.length; ++i ) {
            for(int j = 0; j < waterAreaActual[0].length; ++j ) {
                System.out.print(waterAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(waterAreaExpected,waterAreaActual);
    }

    @Test
    public void testThreeByThreeSquareHasBorder() {
        boolean[][] ground = {{true, true, true},
                              {true, true, true},
                              {true, true, true}};

        boolean[][] waterArea = {{false, false, false},
                                 {false, true, false},
                                 {false, false, false}};

        boolean[][] borderAreaExpected = {{true, true, true},
                                          {true, false, true},
                                          {true, true, true}};

        boolean[][] borderAreaActual = RoomUtils.calculateBorderArea(ground, waterArea);

        for(int i = 0; i < borderAreaActual.length; ++i ) {
            for(int j = 0; j < borderAreaActual[0].length; ++j ) {
                System.out.print(borderAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(borderAreaExpected,borderAreaActual);
    }

    @Test
    public void testFourByThreeSquareHasAThreeByThreeBorder() {
        boolean[][] ground = {{true, false, false},
                              {true, true, true},
                              {true, true, true},
                              {true, true, true}};

        boolean[][] waterArea = {{false, false, false},
                                 {false, false, false},
                                 {false, true, false},
                                 {false, false, false}};

        boolean[][] borderAreaExpected = {{false, false, false},
                                          {true, true, true},
                                          {true, false, true},
                                          {true, true, true}};

        boolean[][] borderAreaActual = RoomUtils.calculateBorderArea(ground, waterArea);

        for(int i = 0; i < borderAreaActual.length; ++i ) {
            for(int j = 0; j < borderAreaActual[0].length; ++j ) {
                System.out.print(borderAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(borderAreaExpected,borderAreaActual);
    }

    @Test
    public void testFourByFourSquareTempleFourByFourBorder() {
        boolean[][] ground = {{true, true, true, true},
                              {true, true, true, true},
                              {true, true, true, true},
                              {true, true, true, true}};

        boolean[][] waterArea = {{false, false, false, false},
                                 {false, true, true, false},
                                 {false, true, true, false},
                                 {false, false, false, false}};

        boolean[][] borderAreaExpected = {{true, true, true, true},
                                          {true, false, false, true},
                                          {true, false, false, true},
                                          {true, true, true, true}};


        boolean[][] borderAreaActual = RoomUtils.calculateBorderArea(ground, waterArea);

        for(int i = 0; i < borderAreaActual.length; ++i ) {
            for(int j = 0; j < borderAreaActual[0].length; ++j ) {
                System.out.print(borderAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(borderAreaExpected,borderAreaActual);
    }

    @Test
    public void testLShapedTempleHasLShapedBorder() {
        boolean[][] ground = {{true, true, true, true, true, true},
                {true, true, true, true, true, true},
                {true, true, true, true, true, true},
                {true, true, true, false, false, false},
                {true, true, true, false, false, false},
                {true, true, true, false, false, false}};

        boolean[][] waterArea = {{false, false, false, false, false, false},
                {false, true, true, true, true, false},
                {false, true, false, false, false, false},
                {false, true, false, false, false, false},
                {false, true, false, false, false, false},
                {false, false, false, false, false, false}};

        boolean[][] borderAreaExpected = {  {true, true, true, true, true, true},
                                            {true, false, false, false, false, true},
                                            {true, false, true, true, true, true},
                                            {true, false, true, false, false, false},
                                            {true, false, true, false, false, false},
                                            {true, true, true, false, false, false}};



        boolean[][] borderAreaActual = RoomUtils.calculateBorderArea(ground, waterArea);

        for(int i = 0; i < borderAreaActual.length; ++i ) {
            for(int j = 0; j < borderAreaActual[0].length; ++j ) {
                System.out.print(borderAreaActual[i][j] + " ");
            }
            System.out.println();
        }

        Assert.assertArrayEquals(borderAreaExpected,borderAreaActual);
    }

}
