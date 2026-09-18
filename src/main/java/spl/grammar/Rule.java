package spl.grammar;

import java.util.List;

public class Rule {
    List<String> rhs;

    public int getId() {
        return -1;
    }

    public String getLhs() {
        return "";
    }

    public List<String> getRhs() {
        return rhs;
    }

    public int length() {
        return -1;
    }

    public boolean isEpsilon() {
        return false;
    }

    @Override
    public String toString() {
        return "-1";
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return -1;
    }
}
