package com.strath.finalpr0ject.ml

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class AnthroZScores(
    val haz: Float,
    val waz: Float,
    val whz: Float,
    val isStunted: Boolean,
    val isSeverelyStunted: Boolean,
    val isWasted: Boolean,
    val isSeverelyWasted: Boolean,
    val isUnderweight: Boolean
)

object WHOAnthroCalculator {

    // Official WHO 2006 Reference Milestone Points (Months : Height cm)
    private val WHO_HAZ_BOYS = listOf(
        0f to 49.9f, 1f to 54.7f, 2f to 58.4f, 3f to 61.4f, 6f to 67.6f, 9f to 72.0f,
        12f to 75.7f, 18f to 82.3f, 24f to 87.8f, 36f to 96.1f, 48f to 103.3f, 60f to 110.0f
    )
    private val WHO_HAZ_GIRLS = listOf(
        0f to 49.1f, 1f to 53.7f, 2f to 57.1f, 3f to 59.8f, 6f to 65.7f, 9f to 70.1f,
        12f to 74.0f, 18f to 80.7f, 24f to 86.4f, 36f to 95.1f, 48f to 102.7f, 60f to 109.4f
    )

    // Months : Weight kg
    private val WHO_WAZ_BOYS = listOf(
        0f to 3.3f, 1f to 4.5f, 2f to 5.6f, 3f to 6.4f, 6f to 7.9f, 9f to 8.9f,
        12f to 9.6f, 18f to 10.9f, 24f to 12.2f, 36f to 14.3f, 48f to 16.3f, 60f to 18.3f
    )
    private val WHO_WAZ_GIRLS = listOf(
        0f to 3.2f, 1f to 4.2f, 2f to 5.1f, 3f to 5.8f, 6f to 7.3f, 9f to 8.2f,
        12f to 8.9f, 18f to 10.2f, 24f to 11.5f, 36f to 13.9f, 48f to 16.1f, 60f to 18.2f
    )

    private fun interpolate(x: Float, table: List<Pair<Float, Float>>): Float {
        if (x <= table.first().first) return table.first().second
        if (x >= table.last().first) return table.last().second
        for (i in 0 until table.size - 1) {
            val (x0, y0) = table[i]
            val (x1, y1) = table[i + 1]
            if (x in x0..x1) {
                val t = (x - x0) / (x1 - x0)
                return y0 + t * (y1 - y0)
            }
        }
        return table.last().second
    }

    private fun calculateZ(value: Float, l: Float, m: Float, s: Float): Float {
        if (value <= 0f || m <= 0f || s <= 0f) return 0f
        return try {
            val z = if (kotlin.math.abs(l) < 0.001f) {
                ln(value / m) / s
            } else {
                ((value / m).pow(l) - 1f) / (l * s)
            }
            max(-6f, min(6f, z))
        } catch (e: Exception) {
            0f
        }
    }

    fun computeAllZScores(
        ageMonths: Int,
        isMale: Boolean,
        heightCm: Float,
        weightKg: Float
    ): AnthroZScores {
        val age = ageMonths.toFloat()

        // HAZ
        val hazTable = if (isMale) WHO_HAZ_BOYS else WHO_HAZ_GIRLS
        val mHaz = interpolate(age, hazTable)
        val sHaz = 0.038f + 0.0001f * age
        val haz = calculateZ(heightCm, 1.0f, mHaz, sHaz)

        // WAZ
        val wazTable = if (isMale) WHO_WAZ_BOYS else WHO_WAZ_GIRLS
        val mWaz = interpolate(age, wazTable)
        val lWaz = if (isMale) -0.15f else -0.20f
        val sWaz = 0.125f + 0.0003f * age
        val waz = calculateZ(weightKg, lWaz, mWaz, sWaz)

        // WHZ
        val hClamped = max(45f, min(120f, heightCm))
        val mWhz = if (isMale) {
            2.4f + 0.0018f * (hClamped - 45f).pow(2.2f) + 0.18f * (hClamped - 45f)
        } else {
            2.4f + 0.0017f * (hClamped - 45f).pow(2.2f) + 0.17f * (hClamped - 45f)
        }
        val lWhz = if (isMale) -0.35f else -0.38f
        val sWhz = 0.088f + 0.0002f * (hClamped - 45f)
        val whz = calculateZ(weightKg, lWhz, mWhz, sWhz)

        return AnthroZScores(
            haz = (Math.round(haz * 100f) / 100f),
            waz = (Math.round(waz * 100f) / 100f),
            whz = (Math.round(whz * 100f) / 100f),
            isStunted = haz < -2.0f,
            isSeverelyStunted = haz < -3.0f,
            isWasted = whz < -2.0f,
            isSeverelyWasted = whz < -3.0f,
            isUnderweight = waz < -2.0f
        )
    }
}
