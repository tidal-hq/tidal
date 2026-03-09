package net.tidalhq.tidal.event.impl;

import net.minecraft.particle.ParticleEffect;
import net.tidalhq.tidal.event.Event;

public class ParticleSpawnedEvent implements Event {
    private final ParticleEffect parameters;
    private final boolean force;
    private final boolean important;
    private final double x;
    private final double y;
    private final double z;
    private final double velocityX;
    private final double velocityY;
    private final double velocityZ;

    public ParticleSpawnedEvent(ParticleEffect parameters, boolean force, boolean important, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        this.parameters = parameters;
        this.force = force;
        this.important = important;
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    public ParticleEffect getParameters() {
        return parameters;
    }

    public boolean isForce() {
        return force;
    }
    public boolean isImportant() {
        return important;
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public double getZ() {
        return z;
    }

    public double getVelocityX() {
        return velocityX;
    }
    public double getVelocityY() {
        return velocityY;
    }
    public double getVelocityZ() {
        return velocityZ;
    }
}
