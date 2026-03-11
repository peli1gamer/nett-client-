package dev.nett.modules;

import dev.nett.NettAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;

public class AutoKelpSellModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSell    = settings.createGroup("Sell");

    private final Setting<Integer> delay = sgGeneral.add(
        new IntSetting.Builder()
            .name("delay-ticks")
            .description("Ticks between inventory scans / sell cycles.")
            .defaultValue(40).min(5).sliderMax(200)
            .build()
    );

    private final Setting<Boolean> logInventory = sgGeneral.add(
        new BoolSetting.Builder()
            .name("log-inventory")
            .description("Print item counts to chat on each scan.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoSell = sgSell.add(
        new BoolSetting.Builder()
            .name("auto-sell")
            .description("Automatically /sell dried kelp when found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> sellSlot = sgSell.add(
        new IntSetting.Builder()
            .name("sell-slot")
            .description("Container slot index to place kelp into.")
            .defaultValue(0).min(0).sliderMax(53)
            .build()
    );

    private enum State { IDLE, SENT_SELL, MOVING_ITEMS, CONFIRMING }

    private State state       = State.IDLE;
    private int   tickCounter = 0;
    private int   waitTicks   = 0;

    public AutoKelpSellModule() {
        super(NettAddon.NETT, "auto-kelp-sell",
            "Scans inventory, logs item counts, and auto-sells dried kelp.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        tickCounter++;

        switch (state) {
            case IDLE -> {
                if (tickCounter < delay.get()) return;
                tickCounter = 0;
                Map<Item, Integer> counts = scanInventory();
                if (logInventory.get()) logCounts(counts);
                if (autoSell.get() && counts.getOrDefault(Items.DRIED_KELP, 0) > 0) {
                    sendSellCommand();
                    state     = State.SENT_SELL;
                    waitTicks = 0;
                }
            }
            case SENT_SELL -> {
                waitTicks++;
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    state     = State.MOVING_ITEMS;
                    waitTicks = 0;
                } else if (waitTicks > 60) {
                    warning("Sell GUI did not open. Back to IDLE.");
                    state = State.IDLE;
                }
            }
            case MOVING_ITEMS -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen)) {
                    state = State.IDLE;
                    return;
                }
                GenericContainerScreenHandler h =
                    (GenericContainerScreenHandler) mc.player.currentScreenHandler;
                int playerStart = h.getRows() * 9;
                int kelpSlot    = findKelp(h, playerStart);
                if (kelpSlot == -1) {
                    state = State.CONFIRMING;
                } else {
                    mc.interactionManager.clickSlot(
                        h.syncId, kelpSlot, 0,
                        SlotActionType.QUICK_MOVE, mc.player
                    );
                }
            }
            case CONFIRMING -> {
                if (mc.currentScreen != null) mc.player.closeHandledScreen();
                info("Kelp sold successfully.");
                state       = State.IDLE;
                tickCounter = 0;
            }
        }
    }

    private Map<Item, Integer> scanInventory() {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.isEmpty()) continue;
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        counts.forEach((item, count) ->
            NettAddon.LOG.debug("[Nett] {} x{}", item, count));
        return counts;
    }

    private void logCounts(Map<Item, Integer> counts) {
        if (counts.isEmpty()) { info("Inventory is empty."); return; }
        StringBuilder sb = new StringBuilder("Inventory: ");
        counts.forEach((item, count) ->
            sb.append(item.getName().getString()).append(" x").append(count).append("  "));
        info(sb.toString().trim());
    }

    private int findKelp(GenericContainerScreenHandler h, int start) {
        for (int i = start; i < h.slots.size(); i++) {
            ItemStack s = h.slots.get(i).getStack();
            if (!s.isEmpty() && s.getItem() == Items.DRIED_KELP) return i;
        }
        return -1;
    }

    private void sendSellCommand() {
        mc.player.networkHandler.sendChatCommand("sell");
        info("Sent /sell command.");
    }

    @Override
    public void onActivate() {
        state       = State.IDLE;
        tickCounter = 0;
        info("AutoKelpSell active.");
    }

    @Override
    public void onDeactivate() {
        state = State.IDLE;
    }
}
