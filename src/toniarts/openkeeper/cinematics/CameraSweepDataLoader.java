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
package toniarts.openkeeper.cinematics;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.export.binary.BinaryImporter;
import java.io.IOException;

/**
 * Just a resource loader for the camera sweep data files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CameraSweepDataLoader implements AssetLoader {

    public static final String FILE_EXTENSION = "csd";

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        BinaryImporter importer = BinaryImporter.getInstance();
        return importer.load(assetInfo);
    }
}
