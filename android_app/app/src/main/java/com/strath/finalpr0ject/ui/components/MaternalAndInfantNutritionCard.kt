package com.strath.finalpr0ject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MaternalAndInfantNutritionCard
 * ==============================
 * Implements the official Kenya Ministry of Health Maternal & Infant Nutrition Guidance:
 * 1. Healthy Eating during Pregnancy & Breastfeeding (10 Food Groups wheel breakdown)
 * 2. Mother care & newborn advice (2 extra small meals, warm wrapping, sleep under LLIN)
 * 3. Infant feeding transition:
 *    - 0 to 6 months: Exclusive Breastfeeding ONLY
 *    - 6 to 9 months: Weaning introduction (thick porridge, mashed foods, 2-3 meals)
 *    - 9 to 11 months: Finger foods, 3 meals + 1 snack
 *    - 1 to 2 years: Family foods cut into small pieces, 5 feeds
 */
@Composable
fun MaternalAndInfantNutritionCard(
    childAgeMonths: Int,
    isSwahili: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isSwahili) "Lishe ya Mama na Mtoto (MOH Kenya)" else "Maternal & Infant Care (MOH Kenya)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (childAgeMonths < 9) {
                            if (isSwahili) "Mtoto anayenyonyeshwa (Miezi 0-9)" else "Breastfeeding Infant (0-9 Months Focus)"
                        } else {
                            if (isSwahili) "Mtoto anayeachishwa kunyonya (Weaning)" else "Weaning & Growing Child"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 1: MOTHER'S NUTRITION & CARE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSwahili) "Mwongozo wa Chakula kwa Mama Anayenyonyesha" else "Mother's Nutrition While Breastfeeding",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF166534)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSwahili)
                            "• Kula milo miwili ya ziada midogo (extra small meals) kila siku ili kuzalisha maziwa ya kutosha.\n" +
                            "• Kula angalau makundi 5 kati ya 10 ya vyakula (nafaka, mboga za kijani, matunda ya manjano/machungwa, maharagwe, maziwa, mayai, nyama/samaki).\n" +
                            "• Kunywa maji safi ya kutosha, uji bora na supu za kiasili siku nzima."
                        else
                            "• Eat TWO extra small meals per day to maintain breastmilk supply and maternal stamina.\n" +
                            "• Eat from at least 5 of the 10 food groups daily (grains, pulses/legumes, dark leafy greens, orange fruits, milk, eggs, fish/meat).\n" +
                            "• Drink plenty of safe water and nutritious fluids (enriched porridge, clean soup).",
                        fontSize = 12.sp,
                        color = Color(0xFF14532D),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 2: AGE-APPROPRIATE INFANT FEEDING & WEANING
            val (title, guidelines, colorContainer, colorText) = when {
                childAgeMonths < 6 -> Quad(
                    if (isSwahili) "Kunyonya Maziwa ya Mama Pekee (Miezi 0 - 6)" else "Exclusive Breastfeeding (0 - 6 Months)",
                    if (isSwahili)
                        "• Maziwa ya mama PEKEE kwa miezi 6 ya kwanza. Usipewe maji, chai wala chakula kingine chochote.\n" +
                        "• Nyonyesha mtoto anapohitaji (on demand), angalau mara 8 ndani ya masaa 24.\n" +
                        "• Mweke mtoto joto (kofia na soksi) na alale ndani ya chandarua (LLIN)."
                    else
                        "• Give ONLY breast milk for the first 6 months. DO NOT give water, glucose, or tea.\n" +
                        "• Breastfeed on demand, day and night (at least 8 times in 24 hours).\n" +
                        "• Colostrum (first yellow milk) protects baby from early diarrhea and infections.",
                    Color(0xFFEFF6FF),
                    Color(0xFF1E40AF)
                )
                childAgeMonths in 6..8 -> Quad(
                    if (isSwahili) "Kuanza Vyakula vya Nyongeza / Weaning (Miezi 6 - 8)" else "Complementary Weaning (6 - 8 Months)",
                    if (isSwahili)
                        "• Anza na uji mzito wenye virutubisho (si mwepesi) na viazi/ndizi zilizopondwa vizuri.\n" +
                        "• Anza na vijiko 2 vikubwa mara 2 kwa siku, ongeza taratibu hadi nusu kikombe.\n" +
                        "• Endelea kumnyonyesha mara kwa mara kabla au baada ya chakula.\n" +
                        "• Tumia sahani na kijiko chake safi pekee (usafi wa hali ya juu)."
                    else
                        "• Start with thick porridge (enriched with milk/peanut paste) or well-mashed pureed foods.\n" +
                        "• Quantity: 2 tablespoons per feed, 2 times daily, increasing to half cup (125ml).\n" +
                        "• Frequent breastfeeding MUST continue as the primary food source.\n" +
                        "• Introduce one food at a time to check for allergies; do not mix more than 2 cereals.",
                    Color(0xFFFFFBEB),
                    Color(0xFF92400E)
                )
                childAgeMonths in 9..11 -> Quad(
                    if (isSwahili) "Vyakula vya Kushika kwa Vidole (Miezi 9 - 11)" else "Finger Foods & Active Feeding (9 - 11 Months)",
                    if (isSwahili)
                        "• Chakula kilichokatwa laini au kupondwa ambacho mtoto anaweza kushika kwa vidole (finger food).\n" +
                        "• Lisha mtoto mara 3 kwa siku pamoja na vitafunio (snack) 1 vya matunda/viazi.\n" +
                        "• Endelea kumnyonyesha mara kwa mara hadi miaka 2 au zaidi."
                    else
                        "• Finely chopped or mashed family foods; let baby pick up pieces with their own fingers.\n" +
                        "• Feed 3 main meals + 1 healthy snack daily (3/4 of a 250ml cup per meal).\n" +
                        "• Continue breastfeeding on demand throughout.",
                    Color(0xFFFEF3C7),
                    Color(0xFFB45309)
                )
                else -> Quad(
                    if (isSwahili) "Chakula cha Familia Kamili (Miaka 1 - 2+)" else "Family Foods & Diversity (1 - 2+ Years)",
                    if (isSwahili)
                        "• Mtoto akatwe chakula cha kawaida cha familia katika vipande vidogo laini.\n" +
                        "• Lisha mtoto mara 5 kwa siku (milo 3 mikuu na vitafunio 2 vya matunda au maziwa).\n" +
                        "• Mpe kikombe 1 cha maziwa safi kila siku na endelea kumnyonyesha hadi miaka 2."
                    else
                        "• Cut family food into small, soft pieces so child can chew comfortably.\n" +
                        "• Feed 5 times daily (3 main meals + 2 nutritious snacks) plus 1 cup of milk.\n" +
                        "• Continue frequent breastfeeding up to 2 years or beyond.",
                    Color(0xFFF1F5F9),
                    Color(0xFF334155)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colorText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = guidelines, fontSize = 12.sp, color = colorText.copy(alpha = 0.95f), lineHeight = 18.sp)
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
