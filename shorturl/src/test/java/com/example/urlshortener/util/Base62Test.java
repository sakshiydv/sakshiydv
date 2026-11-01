package com.example.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62Test {

    @Test
    void encodesZero() {
        assertEquals("0", Base62.encode(0));
    }

    @Test
    void encodeThenDecodeIsIdentity() {
        long[] samples = {1, 61, 62, 63, 125, 1_000_000_000L, Long.MAX_VALUE};
        for (long value : samples) {
            assertEquals(value, Base62.decode(Base62.encode(value)));
        }
    }

    @Test
    void encodesKnownValue() {
        // 62 -> "10" (1*62 + 0)
        assertEquals("10", Base62.encode(62));
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
    }

    @Test
    void rejectsIllegalChar() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("!!"));
    }
}
