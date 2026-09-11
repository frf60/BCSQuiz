package com.faysal.bcsquiz.util

import com.faysal.bcsquiz.data.model.Category
import com.faysal.bcsquiz.data.model.SubCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CategorySeeder {
    suspend fun seedAllCategories() {
        val db = FirebaseFirestore.getInstance()
        
        val data = mapOf(
            "bangla" to Pair("বাংলা ভাষা ও সাহিত্য", listOf(
                "bangla-language" to "বাংলা ভাষা",
                "bangla-literature-ancient-medieval" to "প্রাচীন ও মধ্যযুগ",
                "bangla-literature-modern" to "আধুনিক যুগ"
            )),
            "english" to Pair("English Language and Literature", listOf(
                "parts-of-speech" to "Parts of Speech",
                "idioms-and-phrases" to "Idioms & Phrases",
                "clauses" to "Clauses",
                "corrections" to "Corrections",
                "sentences-and-transformations" to "Sentences & Transformations",
                "words-and-vocabulary" to "Words & Vocabulary",
                "composition" to "Composition",
                "english-literature" to "English Literature"
            )),
            "bangladesh" to Pair("বাংলাদেশ বিষয়াবলি", listOf(
                "national-affairs-history" to "জাতীয় বিষয়াবলি ও ইতিহাস",
                "agricultural-resources" to "কৃষিজ সম্পদ",
                "demographics-ethnic-groups" to "জনসংখ্যা ও ক্ষুদ্র নৃগোষ্ঠী",
                "economy" to "বাংলাদেশের অর্থনীতি",
                "industry-and-trade" to "শিল্প ও বাণিজ্য",
                "constitution" to "বাংলাদেশের সংবিধান",
                "political-system" to "রাজনৈতিক ব্যবস্থা",
                "government-system" to "সরকার ব্যবস্থা",
                "achievements-personalities-institutions" to "জাতীয় অর্জন, ব্যক্তিত্ব ও প্রতিষ্ঠান"
            )),
            "international" to Pair("আন্তর্জাতিক বিষয়াবলি", listOf(
                "global-history-geopolitics" to "বৈশ্বিক ইতিহাস, আঞ্চলিক ও আন্তর্জাতিক ব্যবস্থা",
                "international-security" to "আন্তর্জাতিক নিরাপত্তা ও শক্তি সম্পর্ক",
                "recent-current-affairs" to "সাম্প্রতিক ও চলমান ঘটনাপ্রবাহ",
                "environmental-issues-diplomacy" to "আন্তর্জাতিক পরিবেশগত ইস্যু ও কূটনীতি",
                "international-organizations" to "আন্তর্জাতিক ও বৈশ্বিক সংস্থা"
            )),
            "geography" to Pair("ভূগোল, পরিবেশ ও দুর্যোগ ব্যবস্থাপনা", listOf(
                "geographical-location-boundaries" to "ভৌগোলিক অবস্থান ও সীমানা",
                "physical-environment-resources" to "ভৌত পরিবেশ ও সম্পদের বণ্টন",
                "environment-and-challenges" to "বাংলাদেশের পরিবেশ ও চ্যালেঞ্জ",
                "climate-change-impact" to "জলবায়ু পরিবর্তন ও প্রভাব",
                "disaster-management" to "প্রাকৃতিক দুর্যোগ ও ব্যবস্থাপনা"
            )),
            "science" to Pair("সাধারণ বিজ্ঞান", listOf(
                "physical-science" to "ভৌত বিজ্ঞান",
                "biological-science" to "জীব বিজ্ঞান",
                "modern-science" to "আধুনিক বিজ্ঞান"
            )),
            "computer" to Pair("কম্পিউটার ও তথ্য প্রযুক্তি", listOf(
                "computer-technology" to "কম্পিউটার প্রযুক্তি",
                "information-technology" to "তথ্যপ্রযুক্তি"
            )),
            "math" to Pair("গাণিতিক যুক্তি", listOf(
                "arithmetic" to "পাটিগণিত",
                "algebra" to "বীজগণিত",
                "exponents-logarithms-series" to "সূচক ও লগারিদম, ধারা",
                "geometry-and-mensuration" to "জ্যামিতি ও পরিমিতি",
                "sets-permutations-probability" to "সেট, বিন্যাস, সমাবেশ ও সম্ভাব্যতা"
            )),
            "mental-ability" to Pair("মানসিক দক্ষতা", listOf(
                "verbal-reasoning" to "ভাষাগত যৌক্তিক বিচার",
                "problem-solving" to "সমস্যা সমাধান",
                "spelling-and-language" to "বানান ও ভাষা",
                "mechanical-reasoning" to "যান্ত্রিক দক্ষতা",
                "space-relation" to "স্থানাঙ্ক সম্পর্ক",
                "numerical-ability" to "সংখ্যাগত ক্ষমতা"
            )),
            "ethics" to Pair("নৈতিকতা, মূল্যবোধ ও সু-শাসন", listOf(
                "definition-and-relation" to "মূল্যবোধ ও সু-শাসনের সংজ্ঞা ও সম্পর্ক",
                "societal-national-impact" to "ব্যক্তি, সমাজ ও জাতীয় জীবনে প্রভাব",
                "implementation-and-benefits" to "সু-শাসন প্রতিষ্ঠা ও মূল্যবোধ চর্চা"
            ))
        )

        for ((slug, info) in data) {
            val (name, subcats) = info
            // Save main category
            db.collection("categories").document(slug).set(Category(id = slug, name = name)).await()
            
            // Save sub-categories
            for ((subSlug, subName) in subcats) {
                db.collection("categories").document(slug)
                    .collection("subcategories").document(subSlug)
                    .set(SubCategory(id = subSlug, parentCategoryId = slug, name = subName)).await()
            }
        }
    }
}
