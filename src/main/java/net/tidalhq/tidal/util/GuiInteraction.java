package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.tidalhq.tidal.Tidal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GuiInteraction {

    private static final int STEP_TIMEOUT = 100;
    private static final Random RANDOM = new Random();
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public record Step(String name, Predicate<Screen> condition, Consumer<Screen> action, int minDelay, int maxDelay) {}

    private final List<Step> steps       = new ArrayList<>();
    private int              repeatCount = 1;
    private Runnable         onDone      = () -> {};
    private Consumer<String> onFail      = reason -> {};

    private int     stepIndex   = 0;
    private int     repeatsDone = 0;
    private int     waitTicks   = 0;
    private int delayTicks = 0;
    private boolean running     = false;
    private boolean done        = false;
    private boolean failed      = false;
    private boolean actionFiredThisTick = false;

    private GuiInteraction() {}

    public static GuiInteraction begin() { return new GuiInteraction(); }

    public GuiInteraction waitFor(String name, Predicate<Screen> condition, Consumer<Screen> action, int delay) {
        return waitFor(name, condition, action, delay, delay);
    }

    public GuiInteraction waitFor(String name, Predicate<Screen> condition, Consumer<Screen> action, int minDelay, int maxDelay) {
        steps.add(new Step(name, condition, action, minDelay, maxDelay));
        return this;
    }

    public GuiInteraction waitFor(String name, Predicate<Screen> condition, Consumer<Screen> action) {
        return waitFor(name, condition, action, 0, 0);
    }

    public GuiInteraction waitFor(String titleContains, Consumer<Screen> action, int minDelay, int maxDelay) {
        return waitFor(titleContains, s -> s.getTitle().getString().contains(titleContains), action, minDelay, maxDelay);
    }

    public GuiInteraction waitFor(String titleContains, Consumer<Screen> action) {
        return waitFor(titleContains, action, 0, 0);
    }

    public GuiInteraction repeat(int times) {
        this.repeatCount = Math.max(1, times);
        return this;
    }

    public GuiInteraction onDone(Runnable callback)         { this.onDone = callback; return this; }
    public GuiInteraction onFail(Consumer<String> callback) { this.onFail = callback; return this; }
    public GuiInteraction onFail(Runnable callback)         { this.onFail = r -> callback.run(); return this; }

    public GuiInteraction start() {
        if (steps.isEmpty()) {
            return this;
        }
        stepIndex           = 0;
        repeatsDone         = 0;
        waitTicks           = 0;
        running             = true;
        done                = false;
        failed              = false;
        actionFiredThisTick = false;
        return this;
    }

    public void stop()         { running = false; }
    public boolean isRunning() { return running; }
    public boolean isDone()    { return done; }
    public boolean isFailed()  { return failed; }

    public void tick() {
        if (!running) return;

        if (actionFiredThisTick) {
            actionFiredThisTick = false;
            waitTicks = 0;
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }


        Screen screen  = client.currentScreen;
        Step   current = steps.get(stepIndex);

        if (screen != null && current.condition().test(screen)) {
            current.action().accept(screen);
            actionFiredThisTick = true;
            delayTicks = current.minDelay() + RANDOM.nextInt(Math.max(1, current.maxDelay() - current.minDelay() + 1));
            stepIndex++;

            if (stepIndex >= steps.size()) {
                repeatsDone++;
                if (repeatsDone >= repeatCount) {
                    finish();
                } else {
                    stepIndex = 0;
                }
            }
        } else {
            if (++waitTicks >= STEP_TIMEOUT) {
                fail(String.format("timed out on '%s' after %d ticks — screen was: %s",
                        current.name(),
                        waitTicks,
                        screen == null ? "null" : screen.getTitle().getString()));
            }
        }
    }

    private void finish() {
        running = false;
        done    = true;
        onDone.run();
    }

    private void fail(String reason) {
        running = false;
        failed  = true;
        onFail.accept(reason);
    }
}