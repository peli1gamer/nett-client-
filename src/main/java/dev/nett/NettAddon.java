package dev.nett;

import dev.nett.hud.KelpHud;
import dev.nett.modules.AutoKelpSellModule;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettAddon extends MeteorAddon {

    public static final Logger LOG    = LoggerFactory.getLogger("Nett");
    public static final Category NETT = new Category("Nett");

    @Override
    public void onInitialize() {
        LOG.info("Nett addon initializing...");
        Modules.get().add(new AutoKelpSellModule());
        Hud.get().register(KelpHud.INFO);
        LOG.info("Nett addon ready.");
    }

    @Override
    public String getPackage() {
        return "dev.nett";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("peli1gamer", "nett-client-");
    }
}
