package net.p3pp3rf1y.sophisticatedbackpacks.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SettingsTabBase;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.utils.Position;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntConsumer;

public abstract class SettingsTab<C extends SettingsContainerBase<?>> extends SettingsTabBase<SettingsScreen> {
	private final C settingsContainer;

	protected SettingsTab(C settingsContainer, Position position, SettingsScreen screen, Component tabLabel, List<FormattedText> tooltip,
			List<FormattedText> openTooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		super(position, screen, tabLabel, tooltip, openTooltip, getTabButton);
		this.settingsContainer = settingsContainer;
	}

	protected C getSettingsContainer() {
		return settingsContainer;
	}

	public abstract Optional<Integer> getSlotOverlayColor(int slotNumber);

	public abstract void handleSlotClick(Slot slot, int mouseButton);
}
