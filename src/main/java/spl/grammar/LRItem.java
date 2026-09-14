package spl.grammar;

import java.util.Objects;

final class LRItem {
    final Rule rule;
    final int dot;

    LRItem(Rule rule, int dot){
        this.rule=rule;
        this.dot = dot;
    }

    boolean isComplete() {
        return dot>=rule.length();
    }

    String symbolAfterDot() {
        return isComplete() ? null: rule.getRhs().get(dot);
    }

    LRItem advanced() {
        return new LRItem(rule,dot+1);
    }

    @Override 
    public boolean equals(Object o) {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof LRItem))
        {
            return false;
        }
        LRItem other = (LRItem)o;
        return dot == other.dot && rule.getId()== other.rule.getId();
    }

    @Override 
    public int hashCode() {
        return Objects.hash(rule.getId(),dot);
    }

    @Override 
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.getLhs()).append(" -> ");
        var rhs = rule.getRhs();
        for(int i=0;i<rhs.size();i++) {
            if(i == dot)
            {
                sb.append(". ");
            }
            sb.append(rhs.get(i)).append(' ');
        }
        if(dot == rhs.size())
        {
            sb.append('.');
        }
        return sb.toString().trim();
    }
}
