package de.Keyle.MyPet.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilVersionCompareTest {

    @Test
    void equalVersionsCompareToZero() {
        assertEquals(0, Util.versionCompare("1.0.0", "1.0.0"));
    }

    @Test
    void missingComponentsAreTreatedAsZero() {
        assertTrue(Util.versionCompare("1.0", "1.0.1") < 0);
        assertTrue(Util.versionCompare("1.0.1", "1.0") > 0);
    }

    @Test
    void componentsAreComparedNumerically() {
        assertTrue(Util.versionCompare("1.10.0", "1.9.0") > 0);
        assertTrue(Util.versionCompare("1.9.0", "1.10.0") < 0);
    }

    @Test
    void olderMajorVersionIsNotNewer() {
        // MyPet 3 builds still exist on Modrinth, so a 1.x install must never see them as an update.
        assertTrue(Util.versionCompare("3.14.3", "1.0.0") > 0);
        assertTrue(Util.versionCompare("1.0.0", "3.14.3") < 0);
    }
}
