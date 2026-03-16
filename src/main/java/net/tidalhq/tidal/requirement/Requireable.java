package net.tidalhq.tidal.requirement;

public interface Requireable {
    default RequirementSet requirements() {
        return RequirementSet.EMPTY;
    }
}