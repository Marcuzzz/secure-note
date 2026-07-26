package com.example.securenote.util

import java.security.SecureRandom

data class PasswordOptions(
    val length: Int = 20,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val avoidAmbiguous: Boolean = false,
)

object PasswordGenerator {

    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.?/~"
    private const val AMBIGUOUS = "O0oIl1|`'\"" + '\\'.toString() + "/"

    private val random = SecureRandom()

    fun generate(options: PasswordOptions): String {
        val pools = buildList {
            if (options.includeUppercase) add(UPPER)
            if (options.includeLowercase) add(LOWER)
            if (options.includeDigits) add(DIGITS)
            if (options.includeSymbols) add(SYMBOLS)
        }.map { if (options.avoidAmbiguous) it.filter { c -> c !in AMBIGUOUS } else it }
            .filter { it.isNotEmpty() }

        if (pools.isEmpty() || options.length <= 0) return ""

        val length = options.length
        val chars = CharArray(length)

        // Guarantee at least one from each selected pool (when possible).
        val required = pools.take(length)
        for ((i, pool) in required.withIndex()) {
            chars[i] = pool[random.nextInt(pool.length)]
        }

        val all = pools.joinToString(separator = "")
        for (i in required.size until length) {
            chars[i] = all[random.nextInt(all.length)]
        }

        // Shuffle in place with SecureRandom (Fisher–Yates).
        for (i in chars.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }
        return String(chars)
    }

    /** Rough strength estimate (bits) using pool size and length. */
    fun estimateStrengthBits(options: PasswordOptions): Double {
        var poolSize = 0
        if (options.includeUppercase) poolSize += 26
        if (options.includeLowercase) poolSize += 26
        if (options.includeDigits) poolSize += 10
        if (options.includeSymbols) poolSize += SYMBOLS.length
        if (poolSize == 0 || options.length <= 0) return 0.0
        return options.length * (Math.log(poolSize.toDouble()) / Math.log(2.0))
    }
}
