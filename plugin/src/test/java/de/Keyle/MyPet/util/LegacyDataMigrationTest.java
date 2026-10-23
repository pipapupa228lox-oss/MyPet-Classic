package de.Keyle.MyPet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyDataMigrationTest {

    @TempDir
    Path tempDir;

    private Path write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    @Test
    void copiesFileThatDoesNotExistYet() throws IOException {
        Path source = write(tempDir.resolve("legacy/config.yml"), "old");
        Path target = tempDir.resolve("new/config.yml");

        assertEquals(1, LegacyDataMigration.copyFile(source, target));
        assertEquals("old", Files.readString(target));
    }

    @Test
    void neverOverwritesAnExistingFile() throws IOException {
        Path source = write(tempDir.resolve("legacy/config.yml"), "old");
        Path target = write(tempDir.resolve("new/config.yml"), "new");

        assertEquals(0, LegacyDataMigration.copyFile(source, target));
        assertEquals("new", Files.readString(target));
    }

    @Test
    void copiesMissingFilesFromADirectoryAndKeepsExistingOnes() throws IOException {
        write(tempDir.resolve("legacy/locale/de_DE.txt"), "de");
        write(tempDir.resolve("legacy/locale/en_US.txt"), "en");
        Path kept = write(tempDir.resolve("new/locale/de_DE.txt"), "keep");

        assertEquals(1, LegacyDataMigration.copyDirectory(
                tempDir.resolve("legacy/locale"),
                tempDir.resolve("new/locale")));
        assertEquals("keep", Files.readString(kept));
        assertEquals("en", Files.readString(tempDir.resolve("new/locale/en_US.txt")));
    }

    @Test
    void doesNothingWithoutALegacyFolder() {
        assertFalse(LegacyDataMigration.migrate(tempDir.resolve("plugins/MyPetClassic")));
    }
}
