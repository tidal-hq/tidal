package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;
import java.util.HashMap;
import java.util.Map;

public class MacroManager {
    private static MacroManager instance;
    private final Map<String, Macro> macros;
    private Macro activeMacro;
    private boolean enabled = false;

    private final EventBus eventBus;

    private MacroManager() {
        macros = new HashMap<>();

        eventBus = EventBus.getInstance();
        eventBus.register(this);

        registerMacros();
    }

    public static MacroManager getInstance() {
        if (instance == null) {
            instance = new MacroManager();
        }
        return instance;
    }

    private void registerMacros() {

        SShapeMushroomSDSMacro mushroomMacro = new SShapeMushroomSDSMacro();
        macros.put("ssdsmushroom", mushroomMacro);
        eventBus.register(mushroomMacro);
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

    @Subscribe
    public void onClientTickEvent(ClientTickEvent event) {
        if (enabled && activeMacro != null) {
            activeMacro.onTick();
            activeMacro.updateState();
            activeMacro.invokeState();
        }
    }

    public void onDeath() {
        if (enabled && activeMacro != null) {
            activeMacro.onDeath();
        }
    }
}