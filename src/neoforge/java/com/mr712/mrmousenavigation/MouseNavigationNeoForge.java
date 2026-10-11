package com.mr712.mrmousenavigation;

import com.mr712.mrmousenavigation.gui.MouseNavigationConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("mrmousenavigation")
public class MouseNavigationNeoForge {
    public MouseNavigationNeoForge(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, modListScreen) -> new MouseNavigationConfigScreen(modListScreen)
        );
    }
}