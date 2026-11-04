package com.factory.tecnofactory.util;

/** Cálculo de precios en pesos chilenos (enteros). */
public final class Precios {

    private Precios() {
    }

    /** Precio con el descuento aplicado y redondeado al peso. */
    public static long conDescuento(long precio, int descuento) {
        return Math.round(precio * (100 - descuento) / 100.0);
    }
}
