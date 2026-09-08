package com.example.core.ai.study

import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuizDifficulty

object StudyAIConfig {

  fun buildStudySystemInstruction(level: EducationLevel): String {
    val levelGuidance = when (level) {
      EducationLevel.JHS -> """
        - TARGET LEVEL: Junior High School (JHS) / BECE Preparation in Ghana.
        - TONE & STYLE: Clear, encouraging, step-by-step, using relatable analogies and everyday Ghanaian examples.
        - COMPLEXITY: Focus on foundational principles, clear definitions, simple calculations, and fundamental scientific/mathematical rules.
      """.trimIndent()
      EducationLevel.SHS -> """
        - TARGET LEVEL: Senior High School (SHS) / WASSCE Preparation in Ghana.
        - TONE & STYLE: Rigorous, academically structured, exam-oriented, and intellectually engaging.
        - COMPLEXITY: Use proper technical terminology (e.g. stoichiometry, calculus principles, constitutional mechanisms), structured derivations, and exam-focused key points.
      """.trimIndent()
      EducationLevel.GENERAL -> """
        - TARGET LEVEL: General Academic & Lifelong Learning.
        - TONE & STYLE: Adaptable, lucid, insightful, balancing deep intuition with practical application.
      """.trimIndent()
    }

    return """
      You are KASA Study Companion, an intelligent, empathetic, and culturally grounded academic tutor designed for students in Ghana and across Africa.

      $levelGuidance

      CORE PEDAGOGICAL PRINCIPLES:
      1. TEACH, DON'T JUST DUMP ANSWERS: Explain concepts step-by-step. Break difficult ideas into smaller, intuitive pieces.
      2. ANALOGIES & EXAMPLES: Connect abstract concepts to familiar Ghanaian real-world contexts (e.g. market trading, electricity distribution, solar intensity, local ecosystems, constitutional governance) where natural and effective.
      3. MATHEMATICAL & SCIENTIFIC RIGOR: For numerical and calculation problems, always show the complete mathematical working step-by-step with clear units, formulas, and variable definitions.
      4. HONESTY ON EXAMS: Practice questions generated are "AI-generated practice questions" designed for mastery. Never claim a generated question is an official WAEC or WASSCE past exam question unless verified.
      5. ACTIVE RECALL & RETENTION: Highlight key formulas, definitions, and common student misconceptions.
      6. EMPATHY & CLARITY: If a student asks to "make it simpler" or "explain again", provide a fresh, even more intuitive explanation without condescension.
    """.trimIndent()
  }

  fun buildLessonPrompt(subject: String, topic: String, level: EducationLevel): String {
    return """
      Create a structured, engaging lesson for a student studying:
      - Subject: $subject
      - Topic: $topic
      - Education Level: ${level.displayName} (${level.examContext})

      Respond strictly with a valid JSON object matching the following structure (do not include markdown code block markers or extraneous text outside the JSON):
      {
        "summary": "A concise 2-3 sentence overview of the core concept.",
        "detailedExplanation": "A thorough, step-by-step explanation of how and why this concept works, with clear paragraph breaks.",
        "examples": [
          "Example 1 with clear walk-through solution or real-world application",
          "Example 2 illustrating a common exam scenario or variation"
        ],
        "keyPoints": [
          "Essential definition or rule to remember",
          "Key formula or relationship",
          "Crucial principle"
        ],
        "examPointers": [
          "Common student mistake or trap to avoid",
          "What examiners specifically look for in this topic"
        ],
        "suggestedFollowUps": [
          "Explain step-by-step",
          "Give another example",
          "Make it simpler",
          "Why does this happen?"
        ]
      }
    """.trimIndent()
  }

  fun buildQuizPrompt(
    subject: String,
    topic: String,
    level: EducationLevel,
    difficulty: QuizDifficulty,
    questionCount: Int,
  ): String {
    val boundedCount = questionCount.coerceIn(3, 10)
    return """
      Generate exactly $boundedCount practice questions on:
      - Subject: $subject
      - Topic: $topic
      - Level: ${level.displayName}
      - Difficulty: ${difficulty.displayName} (${difficulty.description})

      GUIDELINES:
      - Make each question clear, unambiguous, and mathematically/scientifically sound.
      - For multiple-choice questions, provide exactly 4 options labeled A, B, C, D.
      - Provide a thorough, step-by-step explanation for the correct answer, including why incorrect options are wrong or common student calculation errors.
      - For calculations, show full formula and arithmetic working in the explanation.

      Respond strictly with a valid JSON object matching this schema (no markdown formatting outside JSON):
      {
        "questions": [
          {
            "questionNumber": 1,
            "questionText": "Question text here?",
            "questionType": "MULTIPLE_CHOICE",
            "options": [
              "A) Option one",
              "B) Option two",
              "C) Option three",
              "D) Option four"
            ],
            "correctAnswer": "B",
            "explanation": "Detailed explanation showing step-by-step reasoning or mathematical proof.",
            "hint": "A subtle conceptual hint that guides without giving away the final answer."
          }
        ]
      }
    """.trimIndent()
  }
}
