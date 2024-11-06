package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.*;
import ac.grim.grimac.manager.init.Initable;
import co.aikar.commands.PaperCommandManager;

import java.util.Locale;

public class CommandRegister implements Initable {

    @Override
    public void start() {
        // This does not make Grim require paper
        // It only enables new features such as asynchronous tab completion on paper
        PaperCommandManager commandManager = new PaperCommandManager(GrimAPI.INSTANCE.getPlugin());
        commandManager.getLocales().setDefaultLocale(Locale.forLanguageTag("pt"));

        commandManager.registerCommand(new PerfSubCommand());
        commandManager.registerCommand(new DebugSubCommand());

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBoolean("alerts.built-in")) {
            commandManager.registerCommand(new AlertsSubCommand());
        }

        commandManager.registerCommand(new ProfileSubCommand());
        commandManager.registerCommand(new ReloadSubCommand());
        commandManager.registerCommand(new LogSubCommand());
        commandManager.registerCommand(new VerboseSubCommand());
        commandManager.registerCommand(new ToggleSubCommand());
    }

}
