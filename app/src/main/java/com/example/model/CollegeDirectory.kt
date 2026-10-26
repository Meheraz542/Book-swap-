package com.example.model

object CollegeDirectory {
    const val FENI_POLYTECHNIC = "Feni Polytechnic Institute"
    const val FENI_COMPUTER_INSTITUTE = "Feni Computer Institute"

    val ALL_COLLEGES = listOf(
        FENI_POLYTECHNIC,
        FENI_COMPUTER_INSTITUTE
    )

    /**
     * Returns the technologies offered by a specific polytechnic / institute.
     * Specific rules:
     * 1. Feni Polytechnic Institute has 6 technologies:
     *    - Civil Technology (Civil)
     *    - Electrical Technology (Electrical)
     *    - Mechanical Technology (Mechanical)
     *    - Power Technology (Power)
     *    - Computer Technology (Computer / CST)
     *    - Architecture and Interior Design Technology (AIDT)
     * 2. Feni Computer Institute has 2 departments:
     *    - Computer Science & Technology (CSE / CST)
     *    - Telecommunication Technology (Telecom)
     */
    fun getTechnologiesForCollege(college: String): List<Technology> {
        val normalized = college.trim().lowercase()
        return when {
            normalized.contains("feni computer") -> listOf(
                Technology.CSE,
                Technology.TELECOM
            )
            normalized.contains("feni polytechnic") -> listOf(
                Technology.CIVIL,
                Technology.ELECTRICAL,
                Technology.MECHANICAL,
                Technology.POWER,
                Technology.CSE,
                Technology.AIDT
            )
            else -> listOf(
                Technology.CIVIL,
                Technology.ELECTRICAL,
                Technology.MECHANICAL,
                Technology.POWER,
                Technology.CSE,
                Technology.AIDT
            )
        }
    }

    fun matchesCollege(collegeA: String, collegeB: String): Boolean {
        val a = collegeA.trim().lowercase()
        val b = collegeB.trim().lowercase()
        if (a == b) return true
        if (a.contains("feni computer") && b.contains("feni computer")) return true
        if (a.contains("feni polytechnic") && b.contains("feni polytechnic")) return true
        if (a.contains("computer") && b.contains("computer")) return true
        if (a.contains("polytechnic") && b.contains("polytechnic")) return true
        return false
    }
}
