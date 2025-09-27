package com.vowser.client.contribution

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags

@Serializable
data class ContributionStep(
    val url: String,
    val title: String,
    val action: String,
    val selector: String?,
    val htmlAttributes: Map<String, String>?,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class ContributionSession(
    val sessionId: String = uuid4().toString(),
    val task: String,
    val steps: MutableList<ContributionStep> = mutableListOf(),
    val startTime: Long = Clock.System.now().toEpochMilliseconds(),
    var isActive: Boolean = true
)

@Serializable
data class ContributionMessage(
    val type: String = ContributionConstants.MESSAGE_TYPE,
    val sessionId: String,
    val task: String,
    val steps: List<ContributionStep>,
    val isPartial: Boolean = false,
    val isComplete: Boolean = false,
    val totalSteps: Int = 0
)

enum class ContributionStatus {
    INACTIVE,
    RECORDING,
    SENDING,
    COMPLETED,
    ERROR
}

object ContributionDataValidator {
    
    // 허용되지 않는 문자 패턴 (XSS 방지)
    private val dangerousPatterns = listOf(
        "<script", "</script>", "javascript:", "vbscript:", "onload=", "onerror=",
        "alert(", "eval(", "document.cookie", "innerHTML", "outerHTML"
    )
    
    fun sanitizeString(input: String?, maxLength: Int = 1000): String {
        if (input == null) return ""
        
        var sanitized = input.trim()
            .take(maxLength) // 길이 제한
            .replace(Regex("[\\r\\n\\t]+"), " ") // 개행 문자 정리
            .replace(Regex("\\s+"), " ") // 중복 공백 정리
        
        // 위험한 패턴 제거
        dangerousPatterns.forEach { pattern ->
            sanitized = sanitized.replace(pattern, "", ignoreCase = true)
        }
        
        return sanitized
    }
    
    fun validateUrl(url: String): String? {
        val sanitizedUrl = sanitizeString(url, ContributionConstants.MAX_URL_LENGTH)
        return if (sanitizedUrl.isBlank() || 
                   (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://") && !sanitizedUrl.startsWith("file://") && !sanitizedUrl.startsWith("about:"))) {
            null
        } else {
            sanitizedUrl
        }
    }
    
    fun sanitizeContributionStep(step: ContributionStep): ContributionStep? {
        try {
            val sanitizedUrl = validateUrl(step.url) ?: return null
            val sanitizedTitle = sanitizeString(step.title, ContributionConstants.MAX_TITLE_LENGTH)
            val sanitizedAction = sanitizeString(step.action, ContributionConstants.MAX_ACTION_NAME_LENGTH)
            val sanitizedSelector = step.selector?.let { sanitizeString(it, ContributionConstants.MAX_SELECTOR_LENGTH) }
            
            // HTML 속성 수정
            val sanitizedAttributes = step.htmlAttributes?.let { attrs ->
                if (attrs.size > ContributionConstants.MAX_ATTRIBUTES_COUNT) {
                    attrs.toList().take(ContributionConstants.MAX_ATTRIBUTES_COUNT).toMap()
                } else attrs
            }?.mapValues { (_, value) ->
                sanitizeString(value, ContributionConstants.MAX_ATTRIBUTE_VALUE_LENGTH)
            }?.filterValues { it.isNotBlank() }
            
            // 유효성 검사
            if (sanitizedAction.isBlank()) return null
            if (sanitizedTitle.isBlank()) return null
            
            return step.copy(
                url = sanitizedUrl,
                title = sanitizedTitle,
                action = sanitizedAction,
                selector = sanitizedSelector,
                htmlAttributes = sanitizedAttributes
            )
        } catch (e: Exception) {
            VowserLogger.warn("Failed to sanitize contribution step: ${e.message}", Tags.BROWSER_AUTOMATION)
            return null
        }
    }
}