package com.example.data.model.study

enum class EducationLevel(
  val id: String,
  val displayName: String,
  val shortName: String,
  val examContext: String,
  val description: String,
) {
  JHS(
    id = "jhs",
    displayName = "Junior High School (JHS)",
    shortName = "JHS",
    examContext = "BECE Context",
    description = "Foundational concepts and clear step-by-step explanations.",
  ),
  SHS(
    id = "shs",
    displayName = "Senior High School (SHS)",
    shortName = "SHS",
    examContext = "WASSCE Context",
    description = "In-depth academic concepts, standard terminology, and analytical problem solving.",
  ),
  GENERAL(
    id = "general",
    displayName = "General Learning",
    shortName = "General",
    examContext = "Open Academic",
    description = "Flexible explanations tailored to personal curiosity and continuous education.",
  );

  companion object {
    fun fromId(id: String): EducationLevel {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SHS
    }
  }
}

enum class SubjectCategory {
  CORE,
  ELECTIVE,
}

data class StudySubject(
  val id: String,
  val name: String,
  val category: SubjectCategory,
  val description: String,
  val iconName: String,
  val sampleTopics: List<String>,
)

object StudySubjectCatalog {
  val CORE_SUBJECTS = listOf(
    StudySubject(
      id = "core_math",
      name = "Core Mathematics",
      category = SubjectCategory.CORE,
      description = "Algebra, trigonometry, geometry, statistics, and practical computations.",
      iconName = "Calculate",
      sampleTopics = listOf("Simultaneous Equations", "Quadratic Functions", "Probability", "Circle Theorems", "Statistics & Standard Deviation"),
    ),
    StudySubject(
      id = "english",
      name = "English Language",
      category = SubjectCategory.CORE,
      description = "Grammar, comprehension, summary writing, essay structures, and vocabulary.",
      iconName = "MenuBook",
      sampleTopics = listOf("Summary Writing Techniques", "Subject-Verb Agreement", "Idioms & Phrasal Verbs", "Formal Letter Writing", "Clauses and Phrases"),
    ),
    StudySubject(
      id = "integrated_science",
      name = "Integrated Science",
      category = SubjectCategory.CORE,
      description = "Foundations of biology, chemistry, physics, and agricultural science.",
      iconName = "Science",
      sampleTopics = listOf("Photosynthesis", "Atomic Structure & Chemical Bonding", "Newton's Laws of Motion", "Ecosystems and Food Webs", "Acids, Bases & Salts"),
    ),
    StudySubject(
      id = "social_studies",
      name = "Social Studies",
      category = SubjectCategory.CORE,
      description = "Ghanaian culture, governance, development, economy, and environmental stewardship.",
      iconName = "Public",
      sampleTopics = listOf("Ghanaian Constitution & Governance", "Environmental Degradation & Solutions", "Culture and National Identity", "Human Resource Development", "Sustainable Resource Management"),
    ),
  )

  val ELECTIVE_SUBJECTS = listOf(
    StudySubject(
      id = "elective_math",
      name = "Elective Mathematics",
      category = SubjectCategory.ELECTIVE,
      description = "Calculus, coordinate geometry, vectors, matrices, mechanics, and advanced algebra.",
      iconName = "Functions",
      sampleTopics = listOf("Differentiation & Rates of Change", "Integration by Substitution", "Vectors in 2D & 3D", "Matrices & Determinants", "Polynomials & Remainder Theorem"),
    ),
    StudySubject(
      id = "physics",
      name = "Physics",
      category = SubjectCategory.ELECTIVE,
      description = "Mechanics, thermodynamics, wave optics, electricity, electromagnetism, and atomic physics.",
      iconName = "ElectricBolt",
      sampleTopics = listOf("Ohm's Law & Circuit Analysis", "Projectile Motion", "Electromagnetic Induction", "Wave Properties & Sound", "Radioactivity & Half-Life"),
    ),
    StudySubject(
      id = "chemistry",
      name = "Chemistry",
      category = SubjectCategory.ELECTIVE,
      description = "Organic chemistry, stoichiometry, thermodynamics, electrochemistry, and kinetics.",
      iconName = "Biotech",
      sampleTopics = listOf("Mole Concept & Stoichiometry", "Organic Functional Groups", "Redox Reactions & Electrolysis", "Chemical Equilibrium", "Periodic Table Trends"),
    ),
    StudySubject(
      id = "biology",
      name = "Biology",
      category = SubjectCategory.ELECTIVE,
      description = "Cellular biology, genetics, ecology, physiology, reproduction, and evolution.",
      iconName = "Eco",
      sampleTopics = listOf("Mendelian Genetics & Inheritance", "Cell Division (Mitosis & Meiosis)", "Human Circulatory System", "Respiration & Energy Production", "Osmosis & Diffusion Mechanisms"),
    ),
    StudySubject(
      id = "economics",
      name = "Economics",
      category = SubjectCategory.ELECTIVE,
      description = "Microeconomics, macroeconomics, fiscal policy, international trade, and market structures.",
      iconName = "TrendingUp",
      sampleTopics = listOf("Price Elasticity of Demand", "Inflation & Monetary Policy", "National Income Accounting", "Perfect Competition vs Monopoly", "Balance of Payments"),
    ),
    StudySubject(
      id = "geography",
      name = "Geography",
      category = SubjectCategory.ELECTIVE,
      description = "Physical geography, map work, climatology, regional geography, and human settlements.",
      iconName = "Map",
      sampleTopics = listOf("Topographic Map Reading", "Plate Tectonics & Earthquakes", "Weathering and Landforms", "Climate Zones of Africa", "Urbanization Trends in West Africa"),
    ),
    StudySubject(
      id = "government",
      name = "Government",
      category = SubjectCategory.ELECTIVE,
      description = "Political theory, constitutional development in Ghana, West African political systems.",
      iconName = "AccountBalance",
      sampleTopics = listOf("Separation of Powers & Checks", "Rule of Law and Human Rights", "Decentralization & Local Governance", "Public Administration", "International Organizations (AU, ECOWAS)"),
    ),
    StudySubject(
      id = "ict",
      name = "ICT / Computing",
      category = SubjectCategory.ELECTIVE,
      description = "Computer architecture, programming logic, networking, database concepts, and internet safety.",
      iconName = "Computer",
      sampleTopics = listOf("Algorithm Design & Flowcharts", "Database Relationships & SQL", "Computer Networking Basics", "Cybersecurity & Data Privacy", "Logic Gates & Boolean Algebra"),
    ),
  )

  val ALL_SUBJECTS = CORE_SUBJECTS + ELECTIVE_SUBJECTS

  fun findSubject(idOrName: String): StudySubject? {
    return ALL_SUBJECTS.firstOrNull { 
      it.id.equals(idOrName, ignoreCase = true) || it.name.equals(idOrName, ignoreCase = true) 
    }
  }
}
