package dev.zero.pvpthings.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Integer slider with a label and a value formatter. */
public class IntSlider extends AbstractSliderButton {
	private final Component label;
	private final int min;
	private final int max;
	private final IntConsumer onChange;
	private final IntFunction<String> format;

	public IntSlider(int x, int y, int width, int height, Component label, int min, int max, int value, IntFunction<String> format, IntConsumer onChange) {
		super(x, y, width, height, Component.empty(), (value - min) / (double) Math.max(1, max - min));
		this.label = label;
		this.min = min;
		this.max = max;
		this.format = format;
		this.onChange = onChange;
		updateMessage();
	}

	public int intValue() {
		return min + (int) Math.round(value * (max - min));
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.empty().append(label).append(": " + format.apply(intValue())));
	}

	@Override
	protected void applyValue() {
		onChange.accept(intValue());
	}
}
