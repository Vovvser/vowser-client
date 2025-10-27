package com.vowser.client.api

import com.vowser.client.api.dto.PathStepDetail
import com.vowser.client.model.MemberResponse
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags

/**
 * 사용자 정보를 자동으로 입력 필드에 매칭하는 유틸리티
 */
object UserInputMatcher {

    /**
     * PathStep의 textLabels를 분석하여 자동으로 입력할 값을 결정
     * @param step 입력이 필요한 경로 단계
     * @param userInfo 사용자 정보
     * @return 자동으로 채울 값, 없으면 null
     */
    fun getAutoFillValue(step: PathStepDetail, userInfo: MemberResponse): String? {
        if (step.isInput == false) {
            return null
        }

        val labels = step.textLabels?.joinToString(" ") { it.lowercase() } ?: ""
        val selectors = step.selectors

        // 1. 이름 필드 매칭
        val nameKeywords = listOf("이름", "name", "성명", "full name", "fullname")
        val nameMatchInLabels = labels.containsOneOf(nameKeywords)
        val nameMatchInSelectors = matchesBySelector(selectors, nameKeywords)
        Napier.d("[Name Match] in labels: $nameMatchInLabels, in selectors: $nameMatchInSelectors", tag = Tags.BROWSER_AUTOMATION)
        if (nameMatchInLabels || nameMatchInSelectors) {
            Napier.i("✓ Auto-fill: name → ${userInfo.name}", tag = Tags.BROWSER_AUTOMATION)
            Napier.d("--- Auto-fill-Debugging --- END", tag = Tags.BROWSER_AUTOMATION)
            return userInfo.name
        }

        // 2. 생년월일 필드 매칭
        val birthdate = userInfo.birthdate
        val birthKeywords = listOf("생년월일", "생일", "birth", "birthday", "date of birth", "dob")
        if (!birthdate.isNullOrBlank()) {
            val birthMatchInLabels = labels.containsOneOf(birthKeywords)
            val birthMatchInSelectors = matchesBySelector(selectors, birthKeywords)
            Napier.d("[Birth Match] in labels: $birthMatchInLabels, in selectors: $birthMatchInSelectors", tag = Tags.BROWSER_AUTOMATION)
            if (birthMatchInLabels || birthMatchInSelectors) {
                val formattedBirthdate = formatBirthDate(birthdate)
                Napier.i("✓ Auto-fill: birthdate → $formattedBirthdate (original: $birthdate)", tag = Tags.BROWSER_AUTOMATION)
                Napier.d("--- Auto-fill-Debugging --- END", tag = Tags.BROWSER_AUTOMATION)
                return formattedBirthdate
            }
        }

        // 3. 전화번호 필드 매칭
        val phoneNumber = userInfo.phoneNumber
        val phoneKeywords = listOf("전화", "휴대폰", "핸드폰", "연락처", "phone", "mobile", "tel", "contact")
        if (!phoneNumber.isNullOrBlank()) {
            val phoneMatchInLabels = labels.containsOneOf(phoneKeywords)
            val phoneMatchInSelectors = matchesBySelector(selectors, phoneKeywords)
            Napier.d("[Phone Match] in labels: $phoneMatchInLabels, in selectors: $phoneMatchInSelectors", tag = Tags.BROWSER_AUTOMATION)
            if (phoneMatchInLabels || phoneMatchInSelectors) {
                val combinedHints = labels + " " + selectors.joinToString(" ")
                val extractedPhone = extractPhoneNumber(combinedHints, phoneNumber)
                if (extractedPhone != null) {
                    Napier.i("✓ Auto-fill: phone → $extractedPhone (original: $phoneNumber)", tag = Tags.BROWSER_AUTOMATION)
                    Napier.d("--- Auto-fill-Debugging --- END", tag = Tags.BROWSER_AUTOMATION)
                    return extractedPhone
                }
            }
        }

        return null
    }

    private fun String.containsOneOf(keywords: List<String>): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }

    private fun matchesBySelector(selectors: List<String>, keywords: List<String>): Boolean {
        return selectors.any { selector ->
            // e.g., #user_name, [name="user_name"], [placeholder~="name"]
            val selectorLower = selector.lowercase()
            keywords.any { keyword ->
                selectorLower.contains(keyword)
            }
        }
    }

    /**
     * 전화번호 파싱 - 레이블에 따라 다른 형식 추출
     * @param labels 입력 필드의 텍스트 레이블
     * @param phoneNumber 전체 전화번호 (예: "010-1234-5678" 또는 "01012345678")
     * @return 추출된 전화번호 (레이블에 맞는 형식)
     */
    private fun extractPhoneNumber(labels: String?, phoneNumber: String): String? {
        // 하이픈 제거
        val digitsOnly = phoneNumber.replace("-", "").replace(" ", "")

        // 전화번호 형식 검증 (10~11자리)
        if (digitsOnly.length !in 10..11) {
            Napier.w("Invalid phone number format: $phoneNumber", tag = Tags.BROWSER_AUTOMATION)
            return null
        }

        return when {
            // "뒤 4자리" 또는 "뒷 번호"
            labels?.contains("뒤") == true || labels?.contains("뒷") == true || labels?.contains("last") == true -> {
                digitsOnly.takeLast(8)
            }
            // "중간 번호"
            labels?.contains("중간") == true || labels?.contains("middle") == true -> {
                when (digitsOnly.length) {
                    10 -> digitsOnly.substring(3, 6) // 010-123-4567 → 123
                    11 -> digitsOnly.substring(3, 7) // 010-1234-5678 → 1234
                    else -> null
                }
            }
            // "첫 번호" 또는 "앞 번호"
            labels?.contains("첫") == true || labels?.contains("앞") == true || labels?.contains("first") == true -> {
                digitsOnly.take(3) // 010
            }
            // 전체 번호
            else -> {
                // 하이픈 포함 여부 판단
                if (labels?.contains("하이픈") == true || labels?.contains("hyphen") == true || labels?.contains("-") == true) {
                    formatPhoneWithHyphen(digitsOnly)
                } else {
                    digitsOnly
                }
            }
        }
    }

    /**
     * 전화번호에 하이픈 추가 (010-1234-5678 형식)
     */
    private fun formatPhoneWithHyphen(digitsOnly: String): String {
        return when (digitsOnly.length) {
            10 -> "${digitsOnly.substring(0, 3)}-${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
            11 -> "${digitsOnly.substring(0, 3)}-${digitsOnly.substring(3, 7)}-${digitsOnly.substring(7)}"
            else -> digitsOnly
        }
    }

    /**
     * 생년월일을 8자리 숫자 형식으로 변환 (YYYYMMDD)
     * 예: "1990-01-01" → "19900101"
     */
    private fun formatBirthDate(birthdate: String): String {
        return birthdate.replace("-", "").replace(".", "").replace("/", "").replace(" ", "")
    }
}