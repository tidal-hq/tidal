package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;
import java.util.HashMap;
import java.util.Map;

public class MacroManager {
    private static MacroManager instance;
    private final Map<String, Macro> macros;
    private Macro activeMacro;
    private boolean enabled = false;

    private MacroManager() {
        macros = new HashMap<>();
        registerMacros();
    }

    public static MacroManager getInstance() {
        if (instance == null) {
            instance = new MacroManager();
        }
        return instance;
    }

    private void registerMacros() {
        macros.put("ssdsmushroom", new SShapeMushroomSDSMacro());
    }

    public Map<String, Macro> getMacros() {
        return macros;
    }

    public Macro getActiveMacro() {
        return activeMacro;
    }

    public void setActiveMacro(String name) {
        this.activeMacro = macros.get(name);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (activeMacro != null) {
            if (enabled) {
                activeMacro.onEnable();
            }
        }
    }

    public void onTick() {
        if (enabled && activeMacro != null) {
            activeMacro.onTick();
            activeMacro.updateState();
            activeMacro.invokeState();
        }
    }
}