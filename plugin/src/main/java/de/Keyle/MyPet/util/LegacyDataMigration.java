/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Copies the data of a previous MyPet 3 installation from {@code plugins/MyPet} into the new
 * {@code plugins/MyPetClassic} folder on first start.
 * <p>
 * Only runs when the new folder has neither a config nor a pets database, so an existing
 * installation is never overwritten. The old folder is left untouched as a fallback.
 */
public final class LegacyDataMigration {

    public static final String LEGACY_FOLDER_NAME = "MyPet";

    private static final String[] FILES = {
            "config.yml",
            "pet-config.yml",
            "exp-config.yml",
            "hooks-config.yml",
            "worldgroups.yml",
            "pet-shops.yml",
            "exp.js",
            "pets.db",
            "My.Pets",
            "My.Pets.old"
    };

    private static final String[] DIRECTORIES = {
            "skilltrees",
            "locale"
    };

    private LegacyDataMigration() {
    }

    /**
     * @return {@code true} if any file was copied
     */
    public static boolean migrate(Path dataFolder) {
        Path pluginsFolder = dataFolder.toAbsolutePath().getParent();
        if (pluginsFolder == null) {
            return false;
        }
        Path legacyFolder = pluginsFolder.resolve(LEGACY_FOLDER_NAME);
        if (!Files.isDirectory(legacyFolder)) {
            return false;
        }
        // never mix two installations: only migrate into a completely fresh data folder
        if (Files.exists(dataFolder.resolve("config.yml")) || Files.exists(dataFolder.resolve("pets.db"))) {
            return false;
        }

        int copied = 0;
        try {
            for (String file : FILES) {
                copied += copyFile(legacyFolder.resolve(file), dataFolder.resolve(file));
            }
            for (String directory : DIRECTORIES) {
                copied += copyDirectory(legacyFolder.resolve(directory), dataFolder.resolve(directory));
            }
        } catch (IOException exception) {
            MyPetApi.getLogger().warning("Could not migrate data from plugins/" + LEGACY_FOLDER_NAME + ": " + exception.getMessage());
            return copied > 0;
        }

        if (copied > 0) {
            MyPetApi.getLogger().info("Migrated " + copied + " file(s) from plugins/" + LEGACY_FOLDER_NAME
                    + " to plugins/" + dataFolder.getFileName() + ".");
            MyPetApi.getLogger().info("The old folder was left untouched; remove it after verifying that your pets and settings were imported.");
        }
        return copied > 0;
    }

    static int copyFile(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source) || Files.exists(target)) {
            return 0;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        return 1;
    }

    static int copyDirectory(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return 0;
        }
        int copied = 0;
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else if (!Files.exists(destination)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
                    copied++;
                }
            }
        }
        return copied;
    }
}
