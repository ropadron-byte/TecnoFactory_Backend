package com.factory.tecnofactory.validation;

/**
 * Datos mínimos para validar un RUN con su dígito verificador.
 * Lo implementan los DTO que llevan RUN, para que {@link RunValido} sirva en más de uno.
 */
public interface ConRun {

    Integer run();

    String runDv();
}
