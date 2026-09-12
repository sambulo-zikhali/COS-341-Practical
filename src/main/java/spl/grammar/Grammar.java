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

    //first sets

    private void computeFirstSets() {
        classifySymbols();
        firstSets = new LinkedHashMap<>();
        for(String nt :nonTerminals) firstSets.put(nt, new LinkedHashSet<>());
        for(String t : getTerminals()) firstSets.put(t,new LinkedHashSet<>(Set.of(t)));

        boolean changed = true;
        while(changed) {
            changed = false;
            for(Rule r :rules) {
                Set<String> firstOfLhs = firstSets.get(r.getLhs());
                if(r.isEpsilon()) {
                    changed |= firstOfLhs.add(EPSILON);
                    continue;
                }
                boolean allNullableSoFar = true;
                for(String sym : r.getRhs()) {
                    Set<String> firstOfSym = firstSets.get(sym);
                    for(String s : firstOfSym) {
                        if(!s.equals(EPSILON)) changed |= firstOfLhs.add(s);
                    }
                    if(!firstOfSym.contains(EPSILON)) {
                        allNullableSoFar = false;
                        break;
                    }
                    }
                    if(allNullableSoFar) {
                        changed |= firstOfLhs.add(EPSILON);
                    }
                }
            }
        }

    public Set<String> firstOf(String symbol) {
        return firstSets.get(symbol);
        }

    public Set<String> firstOfSequence(List<String> symbols) {
        Set<String> result = new LinkedHashSet<>();
        boolean allNullableSoFar = true;
        for(String sym : symbols) {
            Set<String> firstOfSym = firstSets.get(sym);
            for(String s: firstOfSym) {
                if(!s.equals(EPSILON)) result.add(s);
            }
            if(!firstOfSym.contains(EPSILON)) {
                allNullableSoFar = false;
                break;
            }
        }
        if(allNullableSoFar) result.add(EPSILON);
        return result;
    }

    private  void computeFollowSets() {
        followSets = new LinkedHashMap<>();
        for(String nt : nonTerminals) followSets.put(nt, new LinkedHashSet<>());

        followSets.get(AUGMENTED_START).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for(Rule r: rules) {
                List<String> rhs = r.getRhs();
                for (int i = 0; i < rhs.size(); i++) {
                    String sym = rhs.get(i);
                    if(!nonTerminals.contains(sym)) continue;

                    List<String> rest = rhs.subList(i+1, rhs.size());
                    Set<String> firstOfRest = firstOfSequence(rest);

                    Set<String> followOfSym = followSets.get(sym);
                    for(String s : firstOfRest) {
                        if(!s.equals(EPSILON)) changed |= followOfSym.add(s);
                    }
                    if(firstOfRest.contains(EPSILON)) {
                        changed |= followOfSym.addAll(followSets.get(r.getLhs()));
                    }
                }
            }     
        }
    }
    public Set<String> followOf(String nonTerminal) {
        return  followSets.get(nonTerminal);
    }
}




