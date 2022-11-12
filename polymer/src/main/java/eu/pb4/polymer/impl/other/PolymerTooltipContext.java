package eu.pb4.polymer.impl.other;

import net.minecraft.client.item.TooltipContext;

public record PolymerTooltipContext(boolean advanced, boolean creative) implements TooltipContext {
    public static final TooltipContext BASIC = new PolymerTooltipContext(false, false);
    public static final TooltipContext ADVANCED = new PolymerTooltipContext(true, false);

    @Override
    public boolean isAdvanced() {
        return this.advanced;
    }

    @Override
    public boolean isCreative() {
        return this.creative;
    }

    public PolymerTooltipContext withCreative() {
        return new PolymerTooltipContext(this.advanced, true);
    }

    public static PolymerTooltipContext of(TooltipContext context) {
        return new PolymerTooltipContext(context.isAdvanced(), context.isCreative());
    }
}
