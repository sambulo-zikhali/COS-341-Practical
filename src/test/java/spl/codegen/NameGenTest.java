package spl.codegen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameGenTest {
    @Test
    void generatedNamesAreUniqueAndShareOneCounter() {
        NameGen names = new NameGen();

        String temp = names.temp();
        String label = names.label();
        String capture = names.capture();
        String ret = names.ret();
        String cont = names.cont();
        Set<String> generated = new HashSet<>();
        generated.add(temp);
        generated.add(label);
        generated.add(capture);
        generated.add(ret);
        generated.add(cont);

        assertEquals("t0", temp);
        assertEquals("L1", label);
        assertEquals(5, generated.size());
        for (String name : generated) {
            assertFalse(name.startsWith("v"));
        }
    }

    @Test
    void rejectsPrefixesReservedForSymbolNames() {
        NameGen names = new NameGen();
        boolean rejected = false;

        try {
            names.next("v");
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        assertTrue(rejected);
    }
}
