package com.strathmore.afyaearly.ml

import android.content.Context
import com.google.gson.Gson
import com.strathmore.afyaearly.data.local.entity.PatientEntity
import com.strathmore.afyaearly.data.local.entity.ScreeningRecordEntity
import kotlin.math.max
import kotlin.math.min

data class FeatureConfig(
    val feature_columns: List<String>,
    val min_values: List<Float>,
    val max_values: List<Float>,
    val data_range: List<Float>
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

    fun prepareFeatureVector(patient: PatientEntity, screening: ScreeningRecordEntity): FloatArray {
        val valuesMap = mutableMapOf<String, Float>()

        // 1. Demographics & Household
        valuesMap["age_months"] = patient.ageMonths.toFloat()
        valuesMap["household_size"] = patient.householdSize.toFloat()

        // Categorical One-Hot: County
        listOf("Turkana", "Marsabit", "Wajir", "Mandera").forEach { c ->
            valuesMap["county_$c"] = if (patient.county.equals(c, ignoreCase = true)) 1f else 0f
        }

        // Categorical One-Hot: Sex
        valuesMap["sex_Male"] = if (patient.sex.equals("Male", ignoreCase = true)) 1f else 0f
        valuesMap["sex_Female"] = if (patient.sex.equals("Female", ignoreCase = true)) 1f else 0f

        // Categorical One-Hot: Water Source
        valuesMap["water_source_Improved"] = if (patient.waterSource.equals("Improved", ignoreCase = true)) 1f else 0f
        valuesMap["water_source_Unimproved"] = if (patient.waterSource.equals("Unimproved", ignoreCase = true)) 1f else 0f

        // Categorical One-Hot: Maternal Education
        listOf("None", "Primary", "Secondary", "Higher").forEach { edu ->
            valuesMap["maternal_education_$edu"] = if (patient.maternalEducation.equals(edu, ignoreCase = true)) 1f else 0f
        }

        // 2. Anthropometry & Z-Scores
        valuesMap["height_cm"] = screening.heightCm
        valuesMap["weight_kg"] = screening.weightKg
        valuesMap["muac_cm"] = screening.muacCm
        valuesMap["haz"] = screening.haz
        valuesMap["waz"] = screening.waz
        valuesMap["whz"] = screening.whz
        valuesMap["edema"] = screening.edema.toFloat()

        valuesMap["is_stunted"] = if (screening.haz < -2.0f) 1f else 0f
        valuesMap["is_severely_stunted"] = if (screening.haz < -3.0f) 1f else 0f
        valuesMap["is_wasted"] = if (screening.whz < -2.0f) 1f else 0f
        valuesMap["is_severely_wasted"] = if (screening.whz < -3.0f) 1f else 0f
        valuesMap["is_sam_by_muac"] = if (screening.muacCm < 11.5f) 1f else 0f
        valuesMap["is_mam_by_muac"] = if (screening.muacCm in 11.5f..12.49f) 1f else 0f

        // 3. Dietary Indicators
        valuesMap["dietary_diversity_score"] = screening.ddiScore.toFloat()
        valuesMap["is_minimum_acceptable_diet"] = if (screening.ddiScore >= 5) 1f else 0f
        valuesMap["is_severely_diet_monotonous"] = if (screening.ddiScore <= 2) 1f else 0f
        valuesMap["minimum_acceptable_diet"] = screening.isMad.toFloat()

        // 4. Morbidities & Healthcare
        valuesMap["had_diarrhoea_recent"] = screening.diarrhoeaRecent.toFloat()
        valuesMap["had_fever_recent"] = screening.feverRecent.toFloat()
        valuesMap["had_cough_ari_recent"] = screening.coughRecent.toFloat()
        valuesMap["morbidity_score"] = (screening.diarrhoeaRecent + screening.feverRecent + screening.coughRecent).toFloat()
        valuesMap["vitamin_a_supplementation"] = screening.vitASupp.toFloat()
        valuesMap["dewormed_last_6m"] = screening.dewormed.toFloat()
        valuesMap["vaccination_complete_for_age"] = screening.vaccinationComplete.toFloat()

        // 5. Numerical Education & Interactions
        val eduNum = eduMap[patient.maternalEducation] ?: 0
        valuesMap["maternal_edu_level"] = eduNum.toFloat()
        valuesMap["muac_x_maternal_edu"] = screening.muacCm * (eduNum + 1)
        valuesMap["haz_x_ddi"] = screening.haz * screening.ddiScore
        valuesMap["age_x_haz"] = (patient.ageMonths / 12f) * screening.haz
        valuesMap["whz_x_morbidity"] = screening.whz * (valuesMap["morbidity_score"]!! + 1f)

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
