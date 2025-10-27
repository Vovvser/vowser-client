package com.vowser.client.api

import com.vowser.client.api.dto.*
import com.vowser.client.contribution.ContributionStep
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * REST API 클라이언트 - 경로 저장/검색 관리
 */
class PathApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
    }

    /**
     * 경로 저장 (POST /api/v1/paths)
     */
    suspend fun savePath(
        sessionId: String,
        taskIntent: String,
        domain: String,
        steps: List<ContributionStep>
    ): Result<PathSaveResponse> {
        return try {
            // ContributionStep → PathStepSubmission 변환
            val pathSteps = steps.map { step ->
                convertToPathStepSubmission(step, domain)
            }

            val submission = PathSubmission(
                sessionId = sessionId,
                taskIntent = taskIntent,
                domain = domain,
                steps = pathSteps
            )

            val response: HttpResponse = httpClient.post("$baseUrl/api/v1/paths") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(submission))
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val result = json.decodeFromString<PathSaveResponse>(bodyText)
                if (result.status.equals("success", ignoreCase = true)) {
                    val stepsSaved = result.data?.result?.stepsSaved ?: 0
                    Napier.i("✅ Path saved successfully: $stepsSaved steps", tag = Tags.API)
                    Result.success(result)
                } else {
                    val message = result.error?.message ?: "Unknown server error"
                    Napier.e("❌ Failed to save path: $message", tag = Tags.API)
                    Result.failure(Exception(message))
                }
            } else {
                val errorBody = response.bodyAsText()
                Napier.e("❌ Failed to save path: ${response.status} - $errorBody", tag = Tags.API)
                Result.failure(Exception("Failed to save path: ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.e("❌ Error saving path: ${e.message}", e, tag = Tags.API)
            Result.failure(e)
        }
    }

    /**
     * 경로 검색 (GET /api/v1/paths/search)
     */
    suspend fun searchPaths(
        query: String,
        limit: Int = 3,
        domain: String? = null
    ): Result<PathSearchResponse> {
        return try {
            val url = buildString {
                append("$baseUrl/api/v1/paths/search")
                append("?query=${query.encodeURLParameter()}")
                append("&limit=$limit")
                domain?.let { append("&domain=${it.encodeURLParameter()}") }
            }

            val response: HttpResponse = httpClient.get(url)

            if (response.status.isSuccess()) {
                val result = json.decodeFromString<PathSearchResponse>(response.bodyAsText())
                Napier.i("✅ Found ${result.data.totalMatched} paths in ${result.data.performance.searchTime}ms", tag = Tags.API)
                Result.success(result)
            } else {
                val errorBody = response.bodyAsText()
                Napier.e("❌ Failed to search paths: ${response.status} - $errorBody", tag = Tags.API)
                Result.failure(Exception("Failed to search paths: ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.e("❌ Error searching paths: ${e.message}", e, tag = Tags.API)
            Result.failure(e)
        }
    }

    /**
     * ContributionStep을 PathStepSubmission으로 변환
     */
    private fun convertToPathStepSubmission(
        step: ContributionStep,
        domain: String
    ): PathStepSubmission {
        // 셀렉터 생성 (단일 → 다중 fallback)
        val selectors = generateSelectors(step)

        // 액션 타입 매핑
        val action = mapActionType(step.action)

        // 텍스트 레이블 추출
        val textLabels = extractTextLabels(step)

        // Description 생성
        val description = generateDescription(step)

        // Input 타입 감지
        val isInput = action == "input"
        val inputType = if (isInput) detectInputType(step) else null
        val inputPlaceholder = if (isInput) {
            // 실행 재현성을 높이기 위해, 입력된 텍스트를 우선 저장 (민감/장문은 제외)
            val typed = step.htmlAttributes?.get("text")?.trim()
            val safeTyped = if (!typed.isNullOrEmpty() && typed.length <= 20 && (inputType == "search" || inputType == "text")) typed else null
            safeTyped ?: (step.htmlAttributes?.get("placeholder") ?: step.htmlAttributes?.get("aria-label"))
        } else null

        return PathStepSubmission(
            url = step.url,
            domain = domain,
            action = action,
            selectors = selectors,
            description = description,
            textLabels = textLabels,
            isInput = isInput,
            shouldWait = false,
            inputType = inputType,
            inputPlaceholder = inputPlaceholder,
            waitMessage = null
        )
    }

    /**
     * 다중 셀렉터 생성 (fallback용)
     */
    private fun generateSelectors(step: ContributionStep): List<String> {
        val selectors = mutableListOf<String>()
        val attrs = step.htmlAttributes
        val action = step.action.lowercase()
        val detectedInputType = detectInputType(step)
        val tagName = attrs?.get("tag")?.lowercase()
        val isAnchor = tagName == "a"
        val isButton = tagName == "button"

        fun esc(value: String): String = value.replace("'", "\\'")
        fun cssIdent(value: String): String {
            if (value.isEmpty()) return value
            val sb = StringBuilder()
            value.forEachIndexed { idx, ch ->
                val valid = ch.isLetterOrDigit() || ch == '-' || ch == '_'
                if (!valid || (idx == 0 && ch.isDigit())) sb.append('\\').append(ch) else sb.append(ch)
            }
            return sb.toString()
        }

        // 1. (원본 셀렉터는 가장 마지막에 추가하여 너무 범용적인 선택이 우선되지 않도록 한다)

        // 2. ID 기반
        attrs?.get("id")?.let { id ->
            if (id.isNotBlank()) selectors.add("#${cssIdent(id)}")
        }

        // 2-1. (select 특화 셀렉터 생성은 제외)

        // 3. Anchor/URL 기반 (가장 안정적이므로 상위 우선순위)
        attrs?.get("href")?.let { href ->
            if (href.isNotBlank()) {
                selectors.add("a[href='${esc(href)}']")
            }
        }

        // 4. Name 기반
        attrs?.get("name")?.let { name ->
            if (name.isNotBlank()) {
                selectors.add("[name='${esc(name)}']")
            }
        }

        // 5. data-testid 기반
        attrs?.get("data-testid")?.let { testId ->
            if (testId.isNotBlank()) {
                selectors.add("[data-testid='${esc(testId)}']")
            }
        }
        // 5-1. data-nclicks 기반(네이버 계열)
        attrs?.get("data-nclicks")?.let { v ->
            if (v.isNotBlank()) selectors.add("[data-nclicks='${esc(v)}']")
        }

        // 6. aria-label 기반
        attrs?.get("aria-label")?.let { label ->
            if (label.isNotBlank()) {
                selectors.add("[aria-label='${esc(label)}']")
            }
        }

        // 7. 태그 특화 셀렉터
        if (tagName == "input") {
            attrs?.get("placeholder")?.let { ph ->
                if (ph.isNotBlank()) selectors.add("input[placeholder='${esc(ph)}']")
            }
            // 민감값 노출 방지: value 기반 셀렉터는 검색/텍스트형이고 값이 짧을 때만
            attrs?.get("value")?.let { v ->
                val safe = v.trim()
                val isShort = safe.length in 1..20
                val isNonSensitive = detectedInputType == "search" || detectedInputType == "text"
                if (safe.isNotEmpty() && isShort && isNonSensitive) {
                    selectors.add("input[value='${esc(safe)}']")
                }
            }
            attrs?.get("type")?.let { t ->
                if (t.isNotBlank()) selectors.add("input[type='${esc(t)}']")
            }
        } else if (isButton) {
            selectors.add("button")
            attrs?.get("type")?.let { t ->
                if (t.isNotBlank()) selectors.add("button[type='${esc(t)}']")
            }
        }

        // 8. 텍스트 기반 (Playwright 확장 :has-text) — 클릭에서는 높은 우선순위
        val text = attrs?.get("text")?.trim()?.take(100)
        if (!text.isNullOrBlank() && action == "click") {
            val safe = esc(text)
            selectors.add("a:has-text('$safe')")
            selectors.add("button:has-text('$safe')")
            selectors.add("[role='button']:has-text('$safe')")
            selectors.add("*:has-text('$safe')")
        }

        // 9. Class 기반 (낮은 우선순위)
        attrs?.get("class")?.let { className ->
            if (className.isNotBlank()) {
                val classes = className.split(" ").filter { it.isNotBlank() }
                if (classes.isNotEmpty()) {
                    val escClasses = classes.map { cssIdent(it) }
                    selectors.add(".${escClasses.joinToString(".")}")
                    // 토큰 매칭 보조 셀렉터(첫 토큰)
                    selectors.add("[class~='${esc(classes.first())}']")
                    // 텍스트와 조합한 앵커 클래스 우선 후보
                    if (!text.isNullOrBlank() && action == "click") {
                        val safeText = esc(text)
                        if (isAnchor) selectors.add("a.${escClasses.joinToString(".")}:has-text('$safeText')")
                        if (isButton) selectors.add("button.${escClasses.joinToString(".")}:has-text('$safeText')")
                    }
                }
            }
        }

        // 10. 원본 셀렉터(최후순위) — 클릭은 제외하여 과도하게 범용적인 구조 셀렉터 사용을 피함
        if (action != "click") {
            step.selector?.let { rawSel ->
                if (rawSel.isNotBlank()) selectors.add(rawSel)
            }
        }

        // 중복 제거 및 유효한 셀렉터만 반환
        return selectors.distinct().filter { it.isNotBlank() }
            .ifEmpty { listOf(step.selector ?: "body") }
    }

    /**
     * 액션 타입 매핑
     */
    private fun mapActionType(action: String): String = when (action.lowercase()) {
        "type", "input" -> "input"
        "wait"          -> "wait"
        "click"         -> "click"
        "navigate"      -> "navigate"
        "new_tab"       -> "new_tab"
        "select"        -> "select"
        else            -> "click"
    }

    /**
     * 텍스트 레이블 추출
     */
    private fun extractTextLabels(step: ContributionStep): List<String> {
        val labels = mutableListOf<String>()
        val attrs = step.htmlAttributes

        // 입력값은 민감도와 길이에 따라 제한(검색 입력은 허용)
        val action = step.action.lowercase()
        val detectedType = detectInputType(step)
        val rawText = attrs?.get("text")?.trim()
        val allowText = when {
            action == "type" -> (detectedType == "search" && (rawText?.length ?: 0) in 1..20)
            else -> true
        }
        if (allowText && !rawText.isNullOrBlank()) labels.add(rawText)
        attrs?.get("aria-label")?.let { if (it.isNotBlank()) labels.add(it.trim()) }
        attrs?.get("placeholder")?.let { if (it.isNotBlank()) labels.add(it.trim()) }
        attrs?.get("alt")?.let { if (it.isNotBlank()) labels.add(it.trim()) }
        attrs?.get("title")?.let { if (it.isNotBlank()) labels.add(it.trim()) }

        if (step.title.isNotBlank()) {
            labels.add(step.title.trim())
        }

        return labels.distinct().take(5)
    }

    /**
     * Description 생성
     * !!!! 이건 사용자가 직접 입력한다고 가정하고, 나중에 UI에서 편집 가능하도록 변경 !!!!
     */
    private fun generateDescription(step: ContributionStep): String {
        val action = step.action
        val text = step.htmlAttributes?.get("text")?.trim()
            ?: step.htmlAttributes?.get("aria-label")?.trim()
            ?: step.title.trim()

        return when (action.lowercase()) {
            "click" -> {
                val tag = step.htmlAttributes?.get("tag")?.lowercase()
                val inputType = step.htmlAttributes?.get("type")?.lowercase()
                when {
                    tag == "input" || tag == "textarea" -> "입력창 클릭"
                    inputType == "search" -> "검색창 클릭"
                    text.isNotBlank() -> "$text 클릭"
                    else -> "요소 클릭"
                }
            }
            "type", "input" -> {
                val inputType = detectInputType(step)
                val typeLabel = when (inputType) {
                    "email" -> "이메일"
                    "password" -> "비밀번호"
                    "search" -> "검색어"
                    "id" -> "아이디"
                    else -> "텍스트"
                }
                "$typeLabel 입력"
            }
            "navigate" -> "페이지 이동"
            "new_tab" -> "새 탭 열기"
            else -> "$action 실행"
        }
    }

    /**
     * Input 타입 감지
     */
    private fun detectInputType(step: ContributionStep): String {
        val attrs = step.htmlAttributes ?: return "text"

        val type = attrs["type"]?.lowercase()
        val name = attrs["name"]?.lowercase()
        val placeholder = attrs["placeholder"]?.lowercase()
        val ariaLabel = attrs["aria-label"]?.lowercase()

        return when {
            type == "password" -> "password"
            type == "email" -> "email"
            name?.contains("email") == true -> "email"
            name?.contains("password") == true || name?.contains("pwd") == true -> "password"
            name?.contains("id") == true || name?.contains("username") == true -> "id"
            placeholder?.contains("검색") == true || placeholder?.contains("search") == true -> "search"
            ariaLabel?.contains("검색") == true || ariaLabel?.contains("search") == true -> "search"
            else -> "text"
        }
    }
}
