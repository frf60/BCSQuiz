package com.faysal.bcsquiz.util

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object DatabaseSeeder {
    fun populateBcsDatabase(onComplete: (Boolean) -> Unit) {
        val db = Firebase.firestore

        // 1. Add Categories
        val categories = listOf(
            mapOf("id" to "bangla", "name" to "বাংলা ব্যাকরণ ও সাহিত্য"),
            mapOf("id" to "english", "name" to "English Grammar"),
            mapOf("id" to "math", "name" to "গণিত"),
            mapOf("id" to "gk", "name" to "সাধারণ জ্ঞান")
        )

        var pendingOps = categories.size

        for (category in categories) {
            val catId = category["id"] as String
            db.collection("categories").document(catId)
                .set(category)
                .addOnCompleteListener { 
                    pendingOps--
                    if (pendingOps == 0) {
                        seedQuestions(onComplete)
                    }
                }
        }
    }

    private fun seedQuestions(onComplete: (Boolean) -> Unit) {
        val db = Firebase.firestore
        val questions = listOf(
            // Bangla
            mapOf(
                "subject" to "bangla",
                "question" to "চর্যাপদ কোন ছন্দে লেখা?",
                "options" to listOf("মাত্রাবৃত্ত", "অক্ষরবৃত্ত", "স্বরবৃত্ত", "মুক্তক"),
                "correctIndex" to 0,
                "explanation" to "চর্যাপদ মূলত মাত্রাবৃত্ত ছন্দে রচিত।",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            mapOf(
                "subject" to "bangla",
                "question" to "কাজী নজরুল ইসলামের 'অগ্নি-বীণা' কাব্যগ্রন্থের প্রথম কবিতা কোনটি?",
                "options" to listOf("বিদ্রোহী", "প্রলয়োল্লাস", "ধূমকেতু", "খেয়া পারের তরণী"),
                "correctIndex" to 1,
                "explanation" to "অগ্নি-বীণা কাব্যের প্রথম কবিতা 'প্রলয়োল্লাস'।",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            // English
            mapOf(
                "subject" to "english",
                "question" to "Which is the correct spelling?",
                "options" to listOf("Lieuetenant", "Lieutenant", "Lieutenent", "Leutenant"),
                "correctIndex" to 1,
                "explanation" to "The correct spelling is 'Lieutenant'.",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            mapOf(
                "subject" to "english",
                "question" to "What is the synonym of 'Prudent'?",
                "options" to listOf("Wise", "Foolish", "Careless", "Impatient"),
                "correctIndex" to 0,
                "explanation" to "'Prudent' means wise, cautious, or showing good judgment.",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            // Math
            mapOf(
                "subject" to "math",
                "question" to "১ থেকে ১০০ পর্যন্ত মৌলিক সংখ্যা কয়টি?",
                "options" to listOf("২২টি", "২৩টি", "২৪টি", "২৫টি"),
                "correctIndex" to 3,
                "explanation" to "১ থেকে ১০০ পর্যন্ত মোট ২৫টি মৌলিক সংখ্যা রয়েছে।",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            mapOf(
                "subject" to "math",
                "question" to "x + y = 6 এবং x - y = 2 হলে, xy এর মান কত?",
                "options" to listOf("৮", "১৬", "৪", "১২"),
                "correctIndex" to 0,
                "explanation" to "xy = ((x+y)/2)^2 - ((x-y)/2)^2 = (3)^2 - (1)^2 = 9 - 1 = 8.",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            // General Knowledge (GK)
            mapOf(
                "subject" to "gk",
                "question" to "বাংলাদেশের মুক্তিযুদ্ধে অবদানের জন্য বীর প্রতীক খেতাবপ্রাপ্ত একমাত্র বিদেশী নাগরিক কে?",
                "options" to listOf("ডব্লিউ এ এস ওডারল্যান্ড", "ফাদার মারিনো রিগন", "এডওয়ার্ড কেনেডি", "জঁ কুয়ে"),
                "correctIndex" to 0,
                "explanation" to "ডব্লিউ এ এস ওডারল্যান্ড একমাত্র বিদেশী যিনি বীর প্রতীক খেতাব লাভ করেন।",
                "difficulty" to "medium",
                "bcsRelated" to true
            ),
            mapOf(
                "subject" to "gk",
                "question" to "মুজিবনগর সরকার কবে গঠিত হয়েছিল?",
                "options" to listOf("১০ এপ্রিল ১৯৭১", "১৭ এপ্রিল ১৯৭১", "২৬ মার্চ ১৯৭১", "৭ মার্চ ১৯৭১"),
                "correctIndex" to 0,
                "explanation" to "মুজিবনগর সরকার গঠিত হয় ১০ এপ্রিল ১৯৭১ এবং শপথ গ্রহণ করে ১৭ এপ্রিল ১৯৭১।",
                "difficulty" to "medium",
                "bcsRelated" to true
            )
        )

        var uploaded = 0
        for (questionMap in questions) {
            val questionId = questionMap["question"].toString().hashCode().toString()
            val finalQuestion = questionMap.toMutableMap()
            finalQuestion["id"] = questionId // Save ID as a field inside the document
            
            db.collection("questions").document(questionId)
                .set(finalQuestion)
                .addOnCompleteListener { 
                    uploaded++
                    if (uploaded == questions.size) {
                        seedContests(onComplete)
                    }
                }
        }
    }

    private fun seedContests(onComplete: (Boolean) -> Unit) {
        val db = Firebase.firestore
        
        // Get some real question IDs from the seeded list
        val questions = listOf(
            "চর্যাপদ কোন ছন্দে লেখা?",
            "কাজী নজরুল ইসলামের 'অগ্নি-বীণা' কাব্যগ্রন্থের প্রথম কবিতা কোনটি?"
        )
        val validIds = questions.map { it.hashCode().toString() }

        val contests = listOf(
            mapOf(
                "id" to "contest_1",
                "title" to "Special Weekly BCS Contest",
                "startTime" to com.google.firebase.Timestamp.now(),
                "entryCost" to 50,
                "prizePool" to 500,
                "questionIds" to validIds
            )
        )

        for (contest in contests) {
            val id = contest["id"] as String
            db.collection("contests").document(id).set(contest)
        }
        onComplete(true)
    }
}
