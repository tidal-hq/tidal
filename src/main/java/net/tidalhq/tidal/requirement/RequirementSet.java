package net.tidalhq.tidal.requirement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class RequirementSet {

    public static final RequirementSet EMPTY = new RequirementSet(List.of());

    private final List<Requirement> requirements;

    private RequirementSet(List<Requirement> requirements) {
        this.requirements = List.copyOf(requirements);
    }

    public static RequirementSet of(Requirement... requirements) {
        return new RequirementSet(List.of(requirements));
    }

    public static RequirementSet of(List<Requirement> requirements) {
        return new RequirementSet(requirements);
    }

    public boolean allMet() {
        return requirements.stream().allMatch(Requirement::isMet);
    }

    public String firstFailure() {
        return requirements.stream()
                .filter(r -> !r.isMet())
                .findFirst()
                .map(Requirement::failureMessage)
                .orElse("");
    }

    public List<Requirement> failures() {
        return requirements.stream()
                .filter(r -> !r.isMet())
                .collect(Collectors.toList());
    }

    public List<Requirement> all() { return requirements; }
    public boolean isEmpty()       { return requirements.isEmpty(); }

    @Override
    public String toString() {
        return requirements.stream().map(Requirement::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}