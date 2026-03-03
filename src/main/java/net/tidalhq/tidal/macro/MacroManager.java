package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Subscribe
    public void onClientReceiveGameMessage(ClientReceiveGameMessageEvent event) {
        Pattern pattern = Pattern.compile("☠ You (?<reason>.+)");
        Matcher matcher = pattern.matcher(event.getMessageContent());

        if (matcher.find()) {
            if (enabled && activeMacro != null) {
                activeMacro.onDeath();
            }
        }
    }
}