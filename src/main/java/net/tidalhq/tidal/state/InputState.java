package net.tidalhq.tidal.state;

import java.util.HashSet;
import java.util.Set;

public final class InputState {

    private InputState() {}

    private static final Set<String> mouseLocks    = new HashSet<>();
    private static final Set<String> keyboardLocks = new HashSet<>();

    public static void lockMouse(String owner)      { mouseLocks.add(owner); }
    public static void unlockMouse(String owner)    { mouseLocks.remove(owner); }
    public static void lockKeyboard(String owner)   { keyboardLocks.add(owner); }
    public static void unlockKeyboard(String owner) { keyboardLocks.remove(owner); }

    public static void lock(String owner) {
        lockMouse(owner);
        lockKeyboard(owner);
    }

    public static void unlock(String owner) {
        unlockMouse(owner);
        unlockKeyboard(owner);
    }

    public static boolean isMouseLocked()    { return !mouseLocks.isEmpty(); }
    public static boolean isKeyboardLocked() { return !keyboardLocks.isEmpty(); }

    public static void reset() {
        mouseLocks.clear();
        keyboardLocks.clear();
    }

    public static Set<String> getMouseLocks()    { return Set.copyOf(mouseLocks); }
    public static Set<String> getKeyboardLocks() { return Set.copyOf(keyboardLocks); }
}