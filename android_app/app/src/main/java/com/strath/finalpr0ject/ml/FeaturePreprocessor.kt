package com.strath.finalpr0ject.ml

import android.content.Context
import com.google.gson.Gson
import com.strath.finalpr0ject.data.model.ChildProfile
import kotlin.math.max
import kotlin.math.min

data class FeatureConfig(
    val feature_columns: List<String>,
    val min_values: List<Float>,
    val max_values: List<Float>,
    val data_range: List<Float>
)

data class ScreeningInput(
    val child: ChildProfile,
    val weightKg: Float,
    val heightCm: Float,
    val muacCm: Float,
    val haz: Float,
    val waz: Float,
    val whz: Float,
    val edema: Int = 0,
    val ddiScore: Int,
    val isMad: Int,
    val diarrhoeaRecent: Int,
    val feverRecent: Int,
    val coughRecent: Int,
    val dewormed: Int,
    val vitASupp: Int,
    val vaccinationComplete: Int
)

class FeaturePreprocessor(private val context: Context) {

    private val config: FeatureConfig by lazy {
        val json = context.assets.open("feature_config.json").bufferedReader().use { it.readText() }
        Gson().fromJson(json, FeatureConfig::class.java)
    }

    private val eduMap = mapOf(
        "None" to 0,
        "Primary" to 1,
        "Secondary" to 2,
        "Higher" to 3
    )

    fun prepareFeatureVector(input: ScreeningInput): FloatArray {
        val valuesMap = mutableMapOf<String, Float>()
        val p = input.child

        // 1. Demographics & Household
        valuesMap["age_months"] = p.ageMonths.toFloat()
        valuesMap["household_size"] = p.householdSize.toFloat()

        // Categorical One-Hot: County
        listOf("Turkana", "Marsabit", "Wajir", "Mandera").forEach { c ->
            valuesMap["county_$c"] = if (p.county.equals(c, ignoreCase = true)) 1f else 0f
        }

        // Categorical One-Hot: Sex
        valuesMap["sex_Male"] = if (p.sex.equals("Male", ignoreCase = true)) 1f else 0f
        valuesMap["sex_Female"] = if (p.sex.equals("Female", ignoreCase = true)) 1f else 0f

        // Categorical One-Hot: Water Source
        valuesMap["water_source_Improved"] = if (p.waterSource.equals("Improved", ignoreCase = true)) 1f else 0f
        valuesMap["water_source_Unimproved"] = if (p.waterSource.equals("Unimproved", ignoreCase = true)) 1f else 0f

        // Categorical One-Hot: Maternal Education
        listOf("None", "Primary", "Secondary", "Higher").forEach { edu ->
            valuesMap["maternal_education_$edu"] = if (p.maternalEducation.equals(edu, ignoreCase = true)) 1f else 0f
        }

        // 2. Anthropometry & Z-Scores
        valuesMap["height_cm"] = input.heightCm
        valuesMap["weight_kg"] = input.weightKg
        valuesMap["muac_cm"] = input.muacCm
        valuesMap["haz"] = input.haz
        valuesMap["waz"] = input.waz
        valuesMap["whz"] = input.whz
        valuesMap["edema"] = input.edema.toFloat()

        valuesMap["is_stunted"] = if (input.haz < -2.0f) 1f else 0f
        valuesMap["is_severely_stunted"] = if (input.haz < -3.0f) 1f else 0f
        valuesMap["is_wasted"] = if (input.whz < -2.0f) 1f else 0f
        valuesMap["is_severely_wasted"] = if (input.whz < -3.0f) 1f else 0f
        valuesMap["is_sam_by_muac"] = if (input.muacCm < 11.5f) 1f else 0f
        valuesMap["is_mam_by_muac"] = if (input.muacCm in 11.5f..12.49f) 1f else 0f

        // 3. Dietary Indicators
        valuesMap["dietary_diversity_score"] = input.ddiScore.toFloat()
        valuesMap["is_minimum_acceptable_diet"] = if (input.ddiScore >= 5) 1f else 0f
        valuesMap["is_severely_diet_monotonous"] = if (input.ddiScore <= 2) 1f else 0f
        valuesMap["minimum_acceptable_diet"] = input.isMad.toFloat()

        // 4. Morbidities & Healthcare
        valuesMap["had_diarrhoea_recent"] = input.diarrhoeaRecent.toFloat()
        valuesMap["had_fever_recent"] = input.feverRecent.toFloat()
        valuesMap["had_cough_ari_recent"] = input.coughRecent.toFloat()
        valuesMap["morbidity_score"] = (input.diarrhoeaRecent + input.feverRecent + input.coughRecent).toFloat()
        valuesMap["vitamin_a_supplementation"] = input.vitASupp.toFloat()
        valuesMap["dewormed_last_6m"] = input.dewormed.toFloat()
        valuesMap["vaccination_complete_for_age"] = input.vaccinationComplete.toFloat()

        // 5. Numerical Education & Interactions
        val eduNum = eduMap[p.maternalEducation] ?: 0
        valuesMap["maternal_edu_level"] = eduNum.toFloat()
        valuesMap["muac_x_maternal_edu"] = input.muacCm * (eduNum + 1)
        valuesMap["haz_x_ddi"] = input.haz * input.ddiScore
        valuesMap["age_x_haz"] = (p.ageMonths / 12f) * input.haz
        valuesMap["whz_x_morbidity"] = input.whz * (valuesMap["morbidity_score"]!! + 1f)

        // 6. Assemble aligned & scaled vector
        val vector = FloatArray(config.feature_columns.size)
        for (i in config.feature_columns.indices) {
            val col = config.feature_columns[i]
            val raw = valuesMap[col] ?: 0f
            val minV = config.min_values[i]
            val rangeV = config.data_range[i]
            val scaled = if (rangeV > 0f) (raw - minV) / rangeV else 0f
            vector[i] = max(0f, min(1f, scaled))
        }
        return vector
    }
}
