    package net.tidalhq.tidal.macro.impl;

    import net.tidalhq.tidal.macro.Macro;
    import net.tidalhq.tidal.macro.State;
    import net.tidalhq.tidal.util.BlockUtil;

    public class SShapeMushroomSDSMacro extends Macro {

        private boolean leftGround = false;
        @Override
        public void onEnable() {
            super.onEnable();
        }
        @Override
        public void onTick() {
            assert client.player != null;

            client.options.sneakKey.setPressed(!client.player.isOnGround());

        }
        @Override
        public void updateState() {
            if(getCurrentState() == null)
                changeState(State.NONE);

            switch (getCurrentState()) {
                case SWITCHING_LANE:
                    if (!BlockUtil.canWalkBackward(client.world, client.player)) {
                        changeState(State.NONE);
                        break;
                    }

                    break;
                case DROPPING:
                     if (!this.leftGround) {
                         this.leftGround = !client.player.isOnGround();
                         break;
                     }

                    if (client.player != null && client.player.isOnGround()) {
                        changeState(State.NONE); // force direction recalc
                        break;
                    }
                    break;
                case RIGHT:
                    if (BlockUtil.canWalkRight(client.world, client.player)) {
                        changeState(State.RIGHT);
                        break;
                    }

                    changeState(State.DROPPING);
                    break;
                case LEFT:
                    if (BlockUtil.canWalkBackward(client.world, client.player)) {
                        changeState(State.SWITCHING_LANE);
                        break;
                    }

                    if (BlockUtil.canWalkLeft(client.world, client.player)) {
                        changeState(State.LEFT);
                        break;
                    }

                    changeState(State.NONE);
                    break;

                case NONE:
                    changeState(calculateDirection());
                    break;
            }
        }
        @Override
        public void invokeState() {
            if (getCurrentState() == null) return;

            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.attackKey.setPressed(false);

            switch (getCurrentState()) {
                case SWITCHING_LANE:
                    client.options.leftKey.setPressed(true);
                    client.options.backKey.setPressed(true);
                    client.options.attackKey.setPressed(true);
                    break;
                case DROPPING:
                    client.options.rightKey.setPressed(true);
                    break;
                case LEFT:
                    client.options.attackKey.setPressed(true);
                    client.options.leftKey.setPressed(true);
                    break;
                case RIGHT:
                    client.options.attackKey.setPressed(true);
                    client.options.rightKey.setPressed(true);
                    client.options.backKey.setPressed(true);
                    break;

            }
        }

        @Override
        public State calculateDirection() {
            if (BlockUtil.canWalkLeft(client.world, client.player)) {
                return State.LEFT;
            }

            if (BlockUtil.canWalkRight(client.world, client.player)) {
                return State.RIGHT;
            }
            return State.NONE;
        }

        @Override
        public void changeState(State state) {
            super.changeState(state);
            this.leftGround = false;
        }
    }
