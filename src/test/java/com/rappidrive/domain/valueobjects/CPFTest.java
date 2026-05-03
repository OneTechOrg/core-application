package com.rappidrive.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPFTest {

    @Test
    void shouldCreateValidCpf() {
        CPF cpf = new CPF("12345678909");
        assertEquals("12345678909", cpf.getValue());
    }

    @Test
    void shouldAcceptFormattedCpf() {
        CPF cpf = new CPF("123.456.789-09");
        assertEquals("12345678909", cpf.getValue());
    }

    @Test
    void shouldFormatCpfCorrectly() {
        CPF cpf = new CPF("12345678909");
        assertEquals("123.456.789-09", cpf.getFormatted());
    }

    @Test
    void shouldAcceptAnotherValidCpf() {
        CPF cpf = new CPF("111.444.777-35");
        assertEquals("11144477735", cpf.getValue());
    }

    @Test
    void shouldRejectNullCpf() {
        assertThrows(IllegalArgumentException.class, () -> new CPF(null));
    }

    @Test
    void shouldRejectEmptyCpf() {
        assertThrows(IllegalArgumentException.class, () -> new CPF(""));
    }

    @Test
    void shouldRejectBlankCpf() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("   "));
    }

    @Test
    void shouldRejectCpfWithLessThan11Digits() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("123456789"));
    }

    @Test
    void shouldRejectCpfWithMoreThan11Digits() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("123456789012"));
    }

    @Test
    void shouldRejectSequentialCpf111() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("111.111.111-11"));
    }

    @Test
    void shouldRejectSequentialCpf000() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("000.000.000-00"));
    }

    @Test
    void shouldRejectSequentialCpf999() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("999.999.999-99"));
    }

    @Test
    void shouldRejectCpfWithInvalidFirstCheckDigit() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("123.456.789-00"));
    }

    @Test
    void shouldRejectCpfWithInvalidSecondCheckDigit() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("123.456.789-08"));
    }

    @Test
    void shouldAcceptCpfWithCheckDigitZero() {
        // Valid CPF that results in check digit 0
        CPF cpf = new CPF("000.000.001-91");
        assertEquals("00000000191", cpf.getValue());
    }

    @Test
    void shouldHaveEqualityBasedOnValue() {
        CPF cpf1 = new CPF("123.456.789-09");
        CPF cpf2 = new CPF("12345678909");
        CPF cpf3 = new CPF("111.444.777-35");

        assertEquals(cpf1, cpf2);
        assertNotEquals(cpf1, cpf3);
    }

    @Test
    void shouldHaveSameHashCodeForEqualCpfs() {
        CPF cpf1 = new CPF("123.456.789-09");
        CPF cpf2 = new CPF("12345678909");

        assertEquals(cpf1.hashCode(), cpf2.hashCode());
    }

    @Test
    void shouldReturnFormattedCpfInToString() {
        CPF cpf = new CPF("12345678909");
        assertEquals("***.***.***-09", cpf.toString());
    }
}
