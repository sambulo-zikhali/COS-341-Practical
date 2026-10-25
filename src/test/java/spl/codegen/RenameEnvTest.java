package spl.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenameEnvTest {
    @Test
    void looksUpMappedNamesAndLeavesUnmappedNamesUnchanged() {
        RenameEnv env = new RenameEnv();
        env.put("v3", "v3_c1");

        assertEquals("v3_c1", env.lookup("v3"));
        assertEquals("untouched", env.lookup("untouched"));
        assertTrue(env.has("v3"));
        assertFalse(env.has("untouched"));
    }

    @Test
    void childSeesParentEntriesAndKeepsItsOwnEntriesLocal() {
        RenameEnv parent = new RenameEnv();
        parent.put("v3", "v3_c1");
        RenameEnv child = parent.child();
        child.put("v4", "v4_c2");

        assertEquals("v3_c1", child.lookup("v3"));
        assertEquals("v4_c2", child.lookup("v4"));
        assertTrue(child.has("v3"));
        assertFalse(parent.has("v4"));
        assertEquals("v4", parent.lookup("v4"));
    }
}
