package spl.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmitterTest {
    @Test
    void emitsLinesInOrderWithCrLfSeparators() {
        Emitter emitter = new Emitter();
        emitter.emit("first");
        emitter.emit("second");

        assertEquals("first\r\nsecond\r\n", emitter.toString());
        assertTrue(emitter.toString().endsWith("\r\n"));
    }

    @Test
    void mineModeRejectsCommentTerminatorAndAcceptsOtherText() {
        Emitter emitter = new Emitter(true);
        emitter.emit("/ *");
        emitter.emit("[if]");
        boolean rejected = false;

        try {
            emitter.emit("*/");
        } catch (IllegalStateException exception) {
            rejected = true;
        }
        assertTrue(rejected);
        assertEquals("/ *\r\n[if]\r\n", emitter.toString());
    }

    @Test
    void normalModeAcceptsCommentTerminator() {
        Emitter emitter = new Emitter();
        emitter.emit("*/");

        assertEquals("*/\r\n", emitter.toString());
    }
}
