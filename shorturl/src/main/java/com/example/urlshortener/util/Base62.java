package com.example.urlshortener.util;

/**
 * Base62 encoding of a numeric id into a short code using [0-9A-Za-z].
 *
 * Why Base62 over a random string?
 *   - We encode a monotonically increasing DB id, so the mapping is 1:1 and
 *     collisions are impossible by construction — no "generate, check, retry" loop.
 *   - Keyspace: a 7-char code holds 62^7 ≈ 3.5 trillion URLs, and 6 chars already
 *     gives ~56.8 billion. So codes stay short for a very long time.
 *
 * The alternative (random code + DB uniqueness check + retry) is where the
 * birthday-paradox reasoning applies: collisions stay rare until the keyspace
 * fills, at which point the retry loop degrades. We avoid that entirely here,
 * but see README for when the random approach is preferable (e.g. you must not
 * leak how many URLs exist).
 */
public final class Base62 {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length(); // 62

    private Base62() {
    }

    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String code) {
        long value = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = ALPHABET.indexOf(code.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("illegal Base62 char: " + code.charAt(i));
            }
            value = value * BASE + digit;
        }
        return value;
    }
}
