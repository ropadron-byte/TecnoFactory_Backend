package com.factory.tecnofactory.validation;

public final class RunUtil {

    private RunUtil() {
    }

    public static String calcularDv(int run) {
        int suma = 0;
        int multiplicador = 2;

        for (int n = run; n > 0; n /= 10) {
            suma += (n % 10) * multiplicador;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resultado = 11 - (suma % 11);

        if (resultado == 11) {
            return "0";
        }
        if (resultado == 10) {
            return "K";
        }
        return String.valueOf(resultado);
    }
}