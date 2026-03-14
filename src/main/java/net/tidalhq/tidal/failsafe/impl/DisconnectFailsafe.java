package net.tidalhq.tidal.failsafe.impl;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.FailsafeTriggerEvent;
import net.tidalhq.tidal.event.impl.ServerDisconnectEvent;
import net.tidalhq.tidal.failsafe.Failsafe;
import net.tidalhq.tidal.failsafe.FailsafeContext;

public class DisconnectFailsafe extends Failsafe {

    public DisconnectFailsafe(FailsafeContext ctx) {
        super(ctx);
    }

    @Override
    public void onTick() {

    }

    @Override
    public String getName()        { return "Disconnect Failsafe"; }

    @Override
    public String getDescription() { return "Stops macro on disconnect from the server"; }

    @Override
    public String getId()          { return "disconnect_failsafe"; }

    @Subscribe
    public void onServerDisconnect(ServerDisconnectEvent event) {
        EventBus.getInstance().post(new FailsafeTriggerEvent(this));
    }

    @Override
    public void onMacroStopped() {}
}