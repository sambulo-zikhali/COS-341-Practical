package spl.grammar;

import org.junit.jupiter.api.Test;
import spl.model.ParseAction;
import spl.model.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParsingTableTest {

    @Test
    void augmentedStartRuleIsCorrect() {
        Grammar grammar = new Grammar();
        Rule rule0 = grammar.getRule(0);
        assertEquals(Grammar.AUGMENTED_START, rule0.getLhs());
        assertEquals(List.of("SPL_PROG"), rule0.getRhs());
    }

    @Test
    void realStartRuleIsUnchanged() {
        Grammar grammar = new Grammar();
        Rule rule1 = grammar.getRule(1);
        assertEquals("SPL_PROG", rule1.getLhs());
        assertEquals(List.of("P", "$"), rule1.getRhs());
    }

    @Test
    void tableBuildsWithoutConflicts() {
        Grammar grammar = new Grammar();
        ParsingTableBuilder builder = new ParsingTableBuilder(grammar);
        builder.build();
        assertEquals(0, builder.getConflicts().size(),
                "Expected zero shift/reduce or reduce/reduce conflicts: " + builder.getConflicts());
    }

    @Test
    void startStateShiftsUserDefinedName() {
        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();
        ParseAction action = table.get(table.getStartState(), TokenType.NAME);
        assertEquals(ParseAction.Kind.SHIFT, action.getKind());
    }

    @Test
    void reduceActionMapsToCorrectRule() {
        Grammar grammar = new Grammar();
        Rule rule = grammar.getRule(23);
        assertEquals("ASSIGN", rule.getLhs());
        assertEquals(3, rule.length());
        assertFalse(rule.isEpsilon());
    }
}