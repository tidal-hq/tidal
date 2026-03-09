package net.tidalhq.tidal.pathfind;

import java.util.Optional;

public final class PathResult {

    public enum Status {
        SUCCESS,
        NO_PATH,
        GOAL_SOLID,
        TIMED_OUT,
        GOAL_NOT_LOADED,
    }

    private final Status status;
    private final Path path;

    private PathResult(Status status, Path path) {
        this.status = status;
        this.path   = path;
    }

    public static PathResult success(Path path) {
        return new PathResult(Status.SUCCESS, path);
    }

    public static PathResult failure(Status reason) {
        if (reason == Status.SUCCESS) throw new IllegalArgumentException("Use success()");
        return new PathResult(reason, null);
    }

    public Status getStatus() { return status; }
    public boolean isSuccess() { return status == Status.SUCCESS; }

    public Optional<Path> getPath() { return Optional.ofNullable(path); }

    @Override
    public String toString() {
        return isSuccess() ? "PathResult{SUCCESS, " + path + "}" : "PathResult{" + status + "}";
    }
}