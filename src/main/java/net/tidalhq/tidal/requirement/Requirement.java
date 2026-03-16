package net.tidalhq.tidal.requirement;

import java.util.function.Supplier;

public final class Requirement {

    private final String   description;
    private final Supplier<Boolean> condition;

    private Requirement(String description, Supplier<Boolean> condition) {
        this.description = description;
        this.condition   = condition;
    }

    public static Requirement of(String description, Supplier<Boolean> condition) {
        return new Requirement(description, condition);
    }

    public boolean isMet()           { return condition.get(); }
    public String  getDescription()  { return description; }

    public String failureMessage() {
        return isMet() ? "" : "Requirement not met: " + description;
    }

    @Override
    public String toString() { return description + " [" + (isMet() ? "OK" : "FAIL") + "]"; }
}