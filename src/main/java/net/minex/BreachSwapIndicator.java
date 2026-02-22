package net.minex;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreachSwapIndicator implements ModInitializer {
	public static final String MOD_ID = "breach-swap-indicator";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Breach Swap Indicator Initialized");
	}
}