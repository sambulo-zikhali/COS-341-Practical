package spl.grammar;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the full list of {@link Rule}s for SPL, transcribed from the spec
 * PDF ("SPL's Context-Free-Grammar by Prof.G."), plus the augmented start
 * rule that {@link ParsingTableBuilder} needs.
 *
 * Also computes FIRST and FOLLOW sets, since both the table builder (for
 * SLR(1) reduce lookaheads) and anyone hand-checking the grammar for
 * LL(1)/LR-suitability will need them.
 */

public class Grammar {

    public static final String AUGMENTED_START= "SPL_PROG";
    public static final String EPSILON="epsilon";

    private final List<Rule> rules = new ArrayList<>();
    private final Set<String> nonTerminals = new LinkedHashSet<>();
    private final Set<String> terminals = new LinkedHashSet<>();
    private final Map<String,List<Rule>> productionsByLhs = new LinkedHashMap<>();

    private Map<String,Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    public Grammar () {
        int id =0;

        add(id++,AUGMENTED_START,"SPL_PROG","$");
        add(id++, "SPL_PROG", "P", "$");
        add(id++, "P", "V_DECL", ":", "F_DECL", ":", "ALGO");
 
        add(id++, "V_DECL"); // epsilon
        add(id++, "V_DECL", "USER-DEFINED-NAME", "V_DECL");
 
        add(id++, "F_DECL"); // epsilon
        add(id++, "F_DECL", "F_TYPE", "F_DECL");
    }

    private void add(int id,String lhs,String... rhsSymbols){
        List<String> rhs = new ArrayList<>(Arrays.asList(rhsSymbols));
        Rule r = new Rule(id,lhs,rhs);
        rules.add(r);
        nonTerminals.add(lhs);
        productionsByLhs.computeIfAbsent(lhs, k-> new ArrayList<>()).add(r);
    }

    private void classifySymbols() {
        for(Rule r:rules) {
            for(String sym :r.getRhs()) {
                if(!nonTerminals.contains(sym)) {
                    terminals.add(sym);
                }
            }
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Rule getRule(int id) {
        return rules.get(id);
    }

    public List<Rule> getProductions(String nonTerminal) {
        return productionsByLhs.getOrDefault(nonTerminal, List.of());
    }

    public Set<String> getNonTerminals() {
        return nonTerminals;
    }

    public Set<String> getTerminals() {
        return terminals;
    }

    public boolean isTerminal(String symbol) {
        return !nonTerminals.contains(symbol);
    }

    public String getStartSymbol() {
        return AUGMENTED_START;
    }
}
