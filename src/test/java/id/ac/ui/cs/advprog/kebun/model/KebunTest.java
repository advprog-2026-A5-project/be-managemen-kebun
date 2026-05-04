package id.ac.ui.cs.advprog.kebun.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KebunTest {

    @Test
    void builderShouldCreateKebunWithBasicProperties() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(12.5)
                .build();

        assertEquals("Kebun Sawit A", kebun.getName());
        assertEquals("KBNA01", kebun.getCode());
        assertEquals(12.5, kebun.getLuas());
    }

    @Test
    void builderShouldSupportDifferentBasicValues() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit B")
                .code("KBNB02")
                .luas(24.0)
                .build();

        assertEquals("Kebun Sawit B", kebun.getName());
        assertEquals("KBNB02", kebun.getCode());
        assertEquals(24.0, kebun.getLuas());
    }
}
