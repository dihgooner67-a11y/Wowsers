package com.leafia.cursed.fuel;

/** Static properties of one rod type (port of LeafiaRodItem's fields). */
public final class RodType {
    public final String label, functionId;
    public final double life, meltingPoint, reactivity, emission;
    public final String decayProduct; // functionId of the depleted rod, or null
    public final boolean splitIntoFast, splitWithFast, splitWithAny;

    public RodType(String label, double life, double meltingPoint, double reactivity, double emission,
                   String decayProduct, boolean moderated, boolean preferFast, boolean preferAny) {
        this.label = label;
        this.functionId = label.replace("-", "").replace(" ", "").toLowerCase();
        this.life = life;
        this.meltingPoint = meltingPoint;
        this.reactivity = reactivity;
        this.emission = emission;
        this.decayProduct = decayProduct;
        this.splitIntoFast = !moderated;
        this.splitWithFast = preferFast;
        this.splitWithAny = preferAny;
    }

    /** Neutron flux this rod receives from a neighbour rod (addon getFlux(stack, moderated)). */
    public double fluxFrom(RodType neighbour, RodState neighbourState, boolean moderated) {
        double compat = 0.5;
        if (splitWithAny || moderated != splitWithFast) compat = 3.0;
        compat = compat * neighbour.emission * reactivity;
        return Math.max(neighbourState.heat, 20.0) * compat;
    }
}
