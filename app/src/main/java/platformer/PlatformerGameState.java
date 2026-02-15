package platformer;

import dev.lwjgl.audio.AudioManager;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.opengl.GL11.glClearColor;

import dev.lwjgl.UIWindow;

/**
 * Game state manager for the platformer game.
 * Handles input, shop interactions, and coordinates rendering.
 */
public class PlatformerGameState {
    private final UIWindow window;
    private final PlatformerModel model;
    private final double[] cursor = new double[2];
    
    // Key state tracking for continuous input
    private final boolean[] keyStates = new boolean[512];

    private boolean[] prevKeyStates = new boolean[512];

    public PlatformerGameState(UIWindow window) {
        this.window = window;
        this.model = new PlatformerModel(window.getWinW(), window.getWinH());
    }

    public void update(double delta) {
        window.getCursorPos(cursor);
        handleContinuousInput();
        model.update((float) delta);
    }

    private void handleContinuousInput() {
        var keyMap = KeyboardControlManager.getInstance().getKeyMap();
        var player = model.getPlayer();
        var shop = model.getShopSystem();

        // ---------- MOVE LEFT ----------
        if (keyStates[keyMap.get("LEFT")] && !PlatformerModel.freezeTime) {
            player.moveHoriz(-1, false);
        } else if (keyStates[keyMap.get("LEFT")] && !shop.isShopOpenMovedisabldNoifcation()) {
            model.showMessage("You cant move while shop is open. To close shop press enter");
            shop.setShopOpenMovedisabldNoifcation(true);
        }

        // ---------- MOVE RIGHT ----------
        if (keyStates[keyMap.get("RIGHT")] && !PlatformerModel.freezeTime) {
            player.moveHoriz(1, true);
        } else if (keyStates[keyMap.get("RIGHT")] && !shop.isShopOpenMovedisabldNoifcation()) {
            model.showMessage("You cant move while shop is open. To close shop press enter");
            shop.setShopOpenMovedisabldNoifcation(true);
        }

        // ---------- CHARACTER SKILL ----------
        int skillKey = keyMap.get("CHARATER SKILL");

        boolean skillPressed  =  keyStates[skillKey] && !prevKeyStates[skillKey];
        boolean skillHeld     =  keyStates[skillKey];
        boolean skillReleased = !keyStates[skillKey] &&  prevKeyStates[skillKey];

        if (skillHeld) {
            if (player.whichcharacter == 0) {
                player.dash();
            }
            else if (player.whichcharacter == 1 && shop.canGamble() && !player.skipGambling) {
                shop.setCanGamble(false);
                model.performGambling();
            }
            else if (player.whichcharacter == 2) {
                player.stab();
            }
            else if (player.whichcharacter == 3) {
                player.prayHold();   // HOLD behavior
            }
        }

        if (skillReleased) {
            if (player.whichcharacter == 3) {
                player.prayRelease(); // RELEASE behavior (heal here)
            }
        }
        if (skillPressed){
            if (player.whichcharacter == 4) {
                player.windBurst();
            }
        }
        // ---------- JUMP CHARGE ----------
        if (keyStates[keyMap.get("UP")]) {
            player.jump();
        }

        // ---------- FALL (DOWN) ----------
        if (keyStates[keyMap.get("DOWN")]) { //?????????????????????????????????????????????
            player.fall();
        }
            // PlatformerModel.gainLife(); // move this to prayRelease or elsewhere

        // ---------- SAVE PREVIOUS STATES ----------
        System.arraycopy(keyStates, 0, prevKeyStates, 0, keyStates.length);
    }

    public void keys() {
        model.showKeys(false);
    }
    public void render() {
        glClearColor(0.5f, 0.8f, 1f, 0);
        model.render();
        model.renderUI();
    }

    public void onKey(int key, int action) {
        var keyMap = KeyboardControlManager.getInstance().getKeyMap();
        var player = model.getPlayer();
        // Update key state
        if (action == GLFW.GLFW_PRESS) {
            keyStates[key] = true;
            if (key == keyMap.get("UP")) {
                player.jumpAir();
            }
            if (key == keyMap.get("CHARATER SKILL") && model.getPlayer().whichcharacter == 1) {
                model.getShopSystem().setCanGamble(true);
            }
        } else if(action == GLFW.GLFW_REPEAT){
            keyStates[key] = true;
        }else if (action == GLFW.GLFW_RELEASE) {
            keyStates[key] = false;
            
            // Handle key release events
            if (key == keyMap.get("UP")) {
                if (Player.canReleaseJump){
                    player.jumpReleased();
                }
            }
        }
        
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            window.requestClose();
            return;
        }

        // Handle shop toggle
        if (key == GLFW.GLFW_KEY_ENTER) {
            handleShopToggle(action);
        }
        // ---------- CHANGE CHARACTER SKILL ----------
        if (keyStates[keyMap.get("CHANGE CHARATER SKILL")] && !PlatformerModel.freezeTime) {
            if (player.whichcharacter == 0) {
                if (!player.skipGambling) {
                    player.whichcharacter = 1;
                    model.showMessage("Your charator skil is now gambling");
                } else {
                    player.whichcharacter = 2;
                    model.showMessage("Your charator skil is now stabbing");
                }
            } else if (player.whichcharacter == 1) {
                player.whichcharacter = 0;
                model.showMessage("Your charator skil is now dash");
            } else if (player.whichcharacter == 2) {
                player.whichcharacter = 3;
                model.showMessage("Your charator skil is now praying");
            }else if (player.whichcharacter == 3) {
                player.whichcharacter = 4;
                model.showMessage("Your charator skil is now wind burst");
            } else if (player.whichcharacter == 4){
                player.whichcharacter = 0;
                model.showMessage("Your charator skil is now dash");
            }
        }


        // Handle shop purchases
        if (model.getShopSystem().isShopOpened()) {
            if (key == GLFW.GLFW_KEY_1 && action == GLFW.GLFW_PRESS) {
                handleShopPurchase1();
            } else if (key == GLFW.GLFW_KEY_2 && action == GLFW.GLFW_PRESS) {
                handleShopPurchase2();
            }
        }
    }

    public void onMouse(int button, int action, double x, double y) {
        // Mouse handling if needed
    }

    private void handleShopToggle(int action) {
        var shop = model.getShopSystem();

        boolean submitDown = (action == GLFW.GLFW_PRESS);

        // Detect PRESS but only when previously NOT down
        if (submitDown && !shop.isSubmitKeyPreviouslyDown()) {

            // Toggle open/close
            shop.toggleShop();
            PlatformerModel.freezeTime = shop.isShopOpened();

            if (shop.isShopOpened() || ShopSystem.reopenShop) {
                // OPEN SHOP
                showShopmessages();
            } else {
                // CLOSE SHOP
                model.showMessage("You have left the shop");
                shop.setShopOpenMovedisabldNoifcation(false);
                shop.setDelay(true); // Reset delay for next shop open
            }
        }

        // Update the previous state (VERY IMPORTANT)
        shop.setSubmitKeyPreviouslyDown(submitDown);
    }
    private void showShopmessages(){
        var shop = model.getShopSystem();
        model.getMessageSystem().clear();
        if(ShopSystem.reopenShop == false) {
            model.showMessage("You have entered the shop");
            model.showMessage("The things you can buy:");
        } else {
            model.showMessage("Here are the other thing you can buy:");
        }
        var player = model.getPlayer();
        ShopSystem.reopenShop = false;
        if (player.whichcharacter == 0) {
            model.showMessage("Press 1 for the next dash level for " + shop.getDashLevelPrice() + " score");
            if (!player.skipGambling) {
                model.showMessage("Press 2 or " + KeyboardControlManager.getkey(5) + " to change your skill to gambling");
            } else {
                model.showMessage("Press 2 or " + KeyboardControlManager.getkey(5) + " to change your skill to stabbing");
            }
        } else if (player.whichcharacter == 1) {
            if (!shop.isDealarPayedThisRound() )model.showMessage("Press 1 to pay the dealer this round for 20 score");
            model.showMessage("Press 2 or " + KeyboardControlManager.getkey(5) + " to change your skill to dash");;
        } else if (player.whichcharacter == 2){
            model.showMessage("Press 2 or " + KeyboardControlManager.getkey(5) + " to change your skill to praying");
        } else if (player.whichcharacter == 3){
            model.showMessage("Press 2 or " + KeyboardControlManager.getkey(5) + " to change your skill to wind burst");
        } else if (player.whichcharacter == 4){
            model.showMessage("Press 2 or " + KeyboardControlManager.getkey(5) + " to change your skill to dash");
        }
        model.showMessage("Or Press enter again to leave the shop");
    }


    private void handleShopPurchase1() {
        var shop = model.getShopSystem();
        var player = model.getPlayer();
        
        // Prevent immediate purchases when shop just opened
        if (shop.isDelay()) {
            return;
        }
        
        if (player.whichcharacter == 0 && model.getScore() >= shop.getDashLevelPrice()) {
            // Purchase dash level
            model.setScore(model.getScore() - shop.getDashLevelPrice());
            if (Player.testing) {
                PlatformerModel.dashLevel += 10.0f;
            } else {
                PlatformerModel.dashLevel += 1.0f;
            }
            model.showMessage("Dash level purchased!");
            PlatformerModel.dashLevelWasBrought = true;
            showShopmessages();
            if (!Player.testing) {
                shop.setDashLevelPrice((int) (PlatformerModel.dashLevel * 5));
            }
        } else if (player.whichcharacter == 1 && model.getScore() >= 20 && !shop.isDealarPayedThisRound()) {
            model.setScore(model.getScore() - 20);
            shop.setDealarPayedThisRound(true);
            model.showMessage("Dealer paid, your score is now: " + model.getScore());
            ShopSystem.reopenShop = true;
            showShopmessages();
        } else {
            model.showMessage("You're too poor to buy that");
        }
    }

    private void handleShopPurchase2() {
        var shop = model.getShopSystem();
        var player = model.getPlayer();
        
        // Prevent immediate purchases when shop just opened
        if (shop.isDelay()) {
            return;
        }

        if (player.whichcharacter == 0) {
            if (!player.skipGambling) {
                player.whichcharacter = 1;
                model.showMessage("Your charator skil is now gambling");
                ShopSystem.reopenShop = true;
                showShopmessages();
            } else {
                player.whichcharacter = 2;
                model.showMessage("Your charator skil is now stabbing");
                ShopSystem.reopenShop = true;
                showShopmessages();
            }
        } else if (player.whichcharacter == 1) {
            player.whichcharacter = 0;
            model.showMessage("Your charator skil is now dash");
            ShopSystem.reopenShop = true;
            showShopmessages();
        } else if (player.whichcharacter == 2) {
            player.whichcharacter = 3;
            model.showMessage("Your charator skil is now praying");
            ShopSystem.reopenShop = true;
            showShopmessages();
        } else if (player.whichcharacter == 3) {
            player.whichcharacter = 0;
            model.showMessage("Your charator skil is now wind burst");
            ShopSystem.reopenShop = true;
            showShopmessages();
        } else if (player.whichcharacter == 4){
            player.whichcharacter = 0;
            model.showMessage("Your charator skil is now dash");
            ShopSystem.reopenShop = true;
            showShopmessages();
        }
    }

    public PlatformerModel getModel() {
        return model;
    }

    public void cleanup() {
        model.cleanup();
    }
}

