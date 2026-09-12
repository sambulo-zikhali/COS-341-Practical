package spl.grammar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import spl.model.ParseAction;
import spl.model.TokenType;
/**
 * Computes the LR(0) item sets ("states") for the SPL grammar and produces
 * the ACTION/GOTO table from them, using FOLLOW sets to resolve which
 * terminals trigger a reduce (i.e. this builds an SLR(1) table, which is
 * LR(0)'s item-set construction plus one-token-of-lookahead-via-FOLLOW for
 * the reduce decisions).
 *
 * This is the theory-heavy part described in the spec: you should expect to
 * hit conflicts around the nullable productions (V_DECL, F_DECL, ALGO can
 * all derive epsilon) and around the shared USER-DEFINED-NAME prefix of
 * ASSIGN / CALL / TERM. Rather than silently overwriting one action with
 * another, this builder records every conflict it finds in
 * {@link #getConflicts()} so they can be inspected and resolved deliberately
 * (e.g. by rewriting the grammar, or by picking an explicit disambiguation
 * rule) instead of by accident.
*/
public class ParsingTableBuilder {

    /** describes one shift/reduce or deruce/reduce 
    *conflict found during table construction. */

    public static final class Conflict {
        public final int state;
        public final TokenType onToken;
        public final ParseAction kept;
        public final ParseAction discard;

        public Conflict(int state,TokenType onToken,ParseAction kept,ParseAction discard) {
            this.discard = discard;
            this.state = state;
            this.kept = kept;
            this.onToken = onToken;
        }

        @Override 
        public String toString() {
            return "Conflict in state "+state+" on token "+onToken
            +": kept ["+kept+"], discard ["+discard+"]";
        }
    }

    private final Grammar grammar;
    private final List<Set<LRItem>> states = new ArrayList<>();
    private final Map<Integer, Map<String,Integer>> transitions = new LinkedHashMap<>();
    private final List<Conflict> conflicts = new ArrayList<>();

    public ParsingTableBuilder(Grammar grammar) {
        this.grammar = grammar;
    }

    // Step 1: canonical collection of LR(0) item sets
    private Set<LRItem> closure(Set<LRItem>items) {
        Set<LRItem> result = new LinkedHashSet<>(items);
        Deque<LRItem> worklist = new ArrayDeque<>(items);

        while (!worklist.isEmpty()) {
            LRItem item = worklist.poll();
            String next = item.symbolAfterDot();
            if(next == null || grammar.isTerminal(next)) continue;

            for(Rule prod : grammar.getProductions(next)) {
                LRItem newItem = new LRItem(prod,0);
                if(result.add(newItem)) {
                    worklist.add(newItem);
                }
            }
        }
        return result;
    }

    private Set<LRItem> gotoSet(Set<LRItem> items, String symbol) {
        Set<LRItem> moved = new LinkedHashSet<>();
        for(LRItem item:items) {
            if(symbol.equals(item.symbolAfterDot())) {
                moved.add(item.advanced());
            }
        }
        return closure(moved);
    }

    private void buildCanonicalCollection() {
        Rule startRule = grammar.getRule(0); // I'm pretty sure this is for the augmented startstate 
        Set<LRItem> startState = closure(Set.of(new LRItem(startRule,0)));
        states.add(startState);

        Map<Set<LRItem>, Integer> indexOf = new LinkedHashMap<>();
        indexOf.put(startState, 0);

        Deque<Integer> worklist = new ArrayDeque<>();
        worklist.add(0);

        List<String> allSymbols = new ArrayList<>();
        allSymbols.addAll(grammar.getTerminals());
        allSymbols.addAll(grammar.getNonTerminals());

        while(!worklist.isEmpty()) {
            int stateId = worklist.poll();
            Set<LRItem> items = states.get(stateId);

            for(String symbol: allSymbols) {
                Set<LRItem> target = gotoSet(items, symbol);
                if(target.isEmpty()) continue;

                Integer targetId = indexOf.get(target);
                if(targetId == null) {
                    targetId = states.size();
                    states.add(target);
                    indexOf.put(target, targetId);
                    worklist.add(targetId);
                }
                transitions.computeIfAbsent(stateId, k -> new LinkedHashMap<>()).put(symbol,targetId);
            }
        }
    }
    //step 2 action/goto tables form the item sets

    public ParsingTable build() {
        buildCanonicalCollection();

        Map<Integer,Map<TokenType,ParseAction>> actionTable = new LinkedHashMap<>();
        Map<Integer,Map<String,Integer>> gotoTable = new LinkedHashMap<>();

        for (int stateId = 0; stateId< states.size(); stateId++) {
            Set<LRItem> items = states.get(stateId);
            Map<String,Integer> stateTransitions = transitions.getOrDefault(stateId,Map.of());      
            
            
            Map<TokenType,ParseAction> actionRow = actionTable.computeIfAbsent(stateId, k -> new LinkedHashMap<>());
            Map<String, Integer> gotoRow = gotoTable.computeIfAbsent(stateId, k -> new LinkedHashMap<>());

            //SHIFT action and GOTO entries from transitions

            for(Map.Entry<String, Integer> e : stateTransitions.entrySet()) {
                String symbol = e.getKey();
                int target = e.getValue();
                if(grammar.isTerminal(symbol)) {
                    TokenType tt = TokenType.fromGrammarSymbol(symbol);
                    setAction(actionRow,stateId,tt,ParseAction.shift(target));
                }
                else {
                    gotoRow.put(symbol,target);
                }
            }
            //reduee
            for(LRItem item :items) {
                if(!item.isComplete()) continue;

                if(item.rule.getId() == 0) {
                    //augmented rule matched 
                    setAction(actionRow,stateId,TokenType.EOF, ParseAction.accept());
                    continue;
                }

                for(String followSymbol : grammar.followOf(item.rule.getLhs())) {
                    TokenType tt = TokenType.fromGrammarSymbol(followSymbol);
                    setAction(actionRow,stateId,tt,ParseAction.reduce(item.rule.getId()));
                }
            }
        }
        return new ParsingTable(0, actionTable, gotoTable, grammar.getRules());
    }
    //does an action and like records it and  first seen action idk 
    private void setAction(Map<TokenType,ParseAction> row, int stateId,TokenType token, ParseAction candidate) {
        ParseAction existing = row.get(token);
        if(existing == null) {
            row.put(token, candidate);
            return;
        }
        if(existing.toString().equals(candidate.toString())) return;

        ParseAction kept = existing;
        ParseAction discarded = candidate;
        if(existing.kind == ParseAction.Kind.REDUCE && candidate.kind == ParseAction.Kind.SHIFT) {
            kept = candidate;
            discarded = existing;
            row.put(token, candidate);
        }
        conflicts.add(new Conflict(stateId, token, kept, discarded));
    }

    public List<Conflict> geConflicts() {
        return conflicts;
    }
    public int numberOfStates() {
        return states.size();
    }

    //for debugging
    public String describeState(int stateId) {
        StringBuilder sb = new StringBuilder("State "+stateId+":\n");
        for(LRItem item : states.get(stateId)) {
            sb.append(" ").append(item).append('\n');
        }
        return sb.toString();
    }
}

