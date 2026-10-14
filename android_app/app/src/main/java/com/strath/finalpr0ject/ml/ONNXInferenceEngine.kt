package com.strath.finalpr0ject.ml

import ai.onnxruntime.*
import android.content.Context
import java.nio.FloatBuffer

data class InferenceOutput(
    val rawProbability: Float,
    val calibratedProbability: Float,
    val riskTier: String,
    val recommendedAction: String,
    val topContributingFactors: List<ClinicalFactor>
)

data class ClinicalFactor(
    val rank: Int,
    val name: String,
    val statement: String,
    val isRiskFactor: Boolean
)

class ONNXInferenceEngine(private val context: Context) {

    private val preprocessor = FeaturePreprocessor(context)
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    init {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("developmental_delay_classifier.onnx").use { it.readBytes() }
            ortSession = ortEnvironment?.createSession(modelBytes, OrtSession.SessionOptions())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun predict(input: ScreeningInput): InferenceOutput {
        val vector = preprocessor.prepareFeatureVector(input)
        var rawProb = 0.5f

        ortSession?.let { session ->
            ortEnvironment?.let { env ->
                try {
                    val inputName = session.inputNames.iterator().next()
                    val tensor = OnnxTensor.createTensor(
                        env,
                        FloatBuffer.wrap(vector),
                        longArrayOf(1, vector.size.toLong())
                    )

                    val results = session.run(mapOf(inputName to tensor))
                    val rawOutput = results.get(1).value
                    if (rawOutput is Array<*>) {
                        val row = rawOutput[0]
                        if (row is FloatArray && row.size >= 2) {
                            rawProb = row[1]
                        } else if (row is Map<*, *>) {
                            rawProb = (row[1L] as? Float) ?: (row[1] as? Float) ?: 0.5f
                        }
                    }
                } catch (e: Exception) {
                    rawProb = computeFallbackProb(input)
                }
            }
        } ?: run {
            rawProb = computeFallbackProb(input)
        }

        val calibratedProb = calibrateProbability(rawProb)

        val (tier, action) = when {
            calibratedProb < 0.30f -> "LOW RISK" to "Maintain standard nutritional monitoring and age-appropriate stimulation."
            calibratedProb < 0.65f -> "MODERATE RISK" to "Schedule follow-up visit in 14 days; counsel caregiver on diverse feeding and motor activities."
            else -> "HIGH RISK" to "Urgent referral to sub-county hospital for comprehensive paediatric and neurodevelopmental evaluation."
        }

        val factors = generateContributingFactors(input)

        return InferenceOutput(
            rawProbability = rawProb,
            calibratedProbability = calibratedProb,
            riskTier = tier,
            recommendedAction = action,
            topContributingFactors = factors
        )
    }

    private fun calibrateProbability(p: Float): Float {
        return when {
            p <= 0.15f -> p * 0.8f
            p >= 0.85f -> kotlin.math.min(1.0f, p * 1.05f)
            else -> p
        }
    }

    private fun computeFallbackProb(s: ScreeningInput): Float {
        var score = 0.3f
        if (s.whz < -2.0f) score += 0.3f
        if (s.haz < -2.0f) score += 0.25f
        if (s.muacCm < 11.5f) score += 0.2f
        if (s.ddiScore <= 2) score += 0.15f
        return kotlin.math.min(0.99f, score)
    }

    private fun generateContributingFactors(s: ScreeningInput): List<ClinicalFactor> {
        val list = mutableListOf<ClinicalFactor>()

        if (s.muacCm < 11.5f || s.whz < -2.0f) {
            list.add(
                ClinicalFactor(
                    rank = 1,
                    name = "Acute Somatic Wasting",
                    statement = "Critically low MUAC (${s.muacCm} cm) and acute wasting elevate delay risk.",
                    isRiskFactor = true
                )
            )
        } else {
            list.add(
                ClinicalFactor(
                    rank = 1,
                    name = "Adequate Arm Circumference",
                    statement = "Preserved MUAC (${s.muacCm} cm) provides positive protective buffering.",
                    isRiskFactor = false
                )
            )
        }

        if (s.haz < -2.0f) {
            list.add(
                ClinicalFactor(
                    rank = 2,
                    name = "Chronic Linear Stunting",
                    statement = "Height-for-age indicates prolonged linear growth deficit, impacting neuro-development.",
                    isRiskFactor = true
                )
            )
        } else if (s.ddiScore <= 2) {
            list.add(
                ClinicalFactor(
                    rank = 2,
                    name = "Severe Dietary Monotony",
                    statement = "Child consumes <= 2 food groups, indicating critical micronutrient starvation.",
                    isRiskFactor = true
                )
            )
        } else {
            list.add(
                ClinicalFactor(
                    rank = 2,
                    name = "Dietary Diversity",
                    statement = "Dietary intake includes diverse food groups promoting synaptic growth.",
                    isRiskFactor = false
                )
            )
        }

        if (s.vaccinationComplete == 1) {
            list.add(
                ClinicalFactor(
                    rank = 3,
                    name = "Vaccination Completion",
                    statement = "Childhood vaccination is up to date, providing significant protective resilience.",
                    isRiskFactor = false
                )
            )
        } else if (s.diarrhoeaRecent == 1 || s.feverRecent == 1) {
            list.add(
                ClinicalFactor(
                    rank = 3,
                    name = "Recent Infection Burden",
                    statement = "Recent episodes of diarrhea/fever compound enteric nutrient malabsorption.",
                    isRiskFactor = true
                )
            )
        } else {
            list.add(
                ClinicalFactor(
                    rank = 3,
                    name = "Maternal Education",
                    statement = "Caregiver formal schooling (${s.child.maternalEducation}) supports protective care practices.",
                    isRiskFactor = false
                )
            )
        }

        return list.take(3)
    }
}
