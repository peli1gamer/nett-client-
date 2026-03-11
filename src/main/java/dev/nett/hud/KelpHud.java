package dev.nett.hud;

import dev.nett.NettAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public class KelpHud extends HudElement {

    public static final HudElementInfo<KelpHud> INFO =
        new HudElementInfo<>(NettAddon.NETT, "kelp-counter",
            "Shows how much dried kelp is in your inventory.", KelpHud::new);

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<SettingColor> textColor = sg.add(
        new ColorSetting.Builder()
            .name("text-color")
            .description("Normal text colour.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> warningColor = sg.add(
        new ColorSetting.Builder()
            .name("warning-color")
            .description("Colour when kelp count exceeds threshold.")
            .defaultValue(new SettingColor(255, 80, 80, 255))
            .build()
    );

    private final Setting<Integer> threshold = sg.add(
        new IntSetting.Builder()
            .name("threshold")
            .description("Item count that triggers the warning colour.")
            .defaultValue(4096).min(1).sliderMax(27 * 64)
            .build()
    );

    private final Setting<Boolean> showPrefix = sg.add(
        new BoolSetting.Builder()
            .name("show-prefix")
            .description("Show 'Kelp: ' before the number.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> shadow = sg.add(
        new BoolSetting.Builder()
            .name("shadow")
            .description("Render a text shadow.")
            .defaultValue(true)
            .build()
    );

    public KelpHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        int kelp = countKelp();
        String text = showPrefix.get() ? "Kelp: " + kelp : String.valueOf(kelp);
        SettingColor color = (kelp >= threshold.get()) ? warningColor.get() : textColor.get();
        renderer.text(text, x, y, color, shadow.get());
        setSize(renderer.textWidth(text), renderer.textHeight());
    }

    private int countKelp() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;
        int total = 0;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (!stack.isEmpty() && stack.getItem() == Items.DRIED_KELP) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
