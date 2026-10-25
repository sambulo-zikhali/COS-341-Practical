package spl.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmitterTest {
    @Test
    void emitsLinesInOrderWithCrLfSeparators() {
        Emitter emitter = new Emitter("int");
        emitter.line("first");
        emitter.line("second");

        assertEquals("first\r\nsecond\r\n", emitter.toString());
        assertTrue(emitter.toString().endsWith("\r\n"));
    }

    @Test
    void mineModeRejectsCommentDelimitersAndAcceptsOtherText() {
        Emitter emitter = new Emitter("int");
        emitter.beginMine();
        emitter.line("/ *");
        emitter.line("[if]");

        assertRejected(emitter, "*/");
        assertRejected(emitter, "/*");
        assertEquals("/ *\r\n[if]\r\n", emitter.toString());
    }

    @Test
    void normalModeAcceptsCommentTerminator() {
        Emitter emitter = new Emitter("int");
        emitter.line("*/");

        assertEquals("*/\r\n", emitter.toString());
    }

    private void assertRejected(Emitter emitter, String line) {
        boolean rejected = false;
        try {
            emitter.line(line);
        } catch (IllegalStateException exception) {
            rejected = true;
        }
        assertTrue(rejected);
    }
}
