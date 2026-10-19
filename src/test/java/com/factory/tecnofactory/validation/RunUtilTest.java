package com.factory.tecnofactory.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunUtilTest {

    @Test
    void calculaDigitosNumericos() {
        assertEquals("5", RunUtil.calcularDv(12345678));
        assertEquals("1", RunUtil.calcularDv(11111111));
        assertEquals("2", RunUtil.calcularDv(22222222));
    }

    @Test
    void calculaK() {
        assertEquals("K", RunUtil.calcularDv(6000000));
    }

    @Test
    void calculaCero() {
        assertEquals("0", RunUtil.calcularDv(14000000));
    }

    @Test
    void calculaLosExtremos() {
        assertEquals("9", RunUtil.calcularDv(1));
        assertEquals("9", RunUtil.calcularDv(99999999));
    }
}