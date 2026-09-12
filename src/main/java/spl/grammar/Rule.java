package spl.grammar;


import java.util.Collection;
import java.util.Collections;
import java.util.List;



public class Rule {
    private final int id;
    private final String lhs;
    private final List<String> rhs;

    public Rule(int id,String lhs,List<String> rhs) {
        this.id=id;
        this.lhs=lhs;
        this.rhs=Collections.unmodifiableList(rhs);
    }

    public int getId() {
        return id;
    }

    public String getLhs() {
        return lhs;
    }

    public List<String> getRhs() {
        return rhs;
    }

    public int length() {
        return rhs.size();
    }

    public boolean isEpsilon() {
        return rhs.isEmpty();
    }

    @Override 
    public String toString() {
        return lhs +" -> "+(rhs.isEmpty() ? "epsilon": String.join(" ", rhs));
    }

    @Override 
    public boolean equals(Object o) {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof Rule))
        {
            return false;
        }
        Rule other = (Rule) o;
        return id == other.id;
    }

    @Override 
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
