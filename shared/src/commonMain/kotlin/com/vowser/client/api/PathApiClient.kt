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
                val result = json.decodeFromString<PathSaveResponse>(response.bodyAsText())
                Napier.i("✅ Path saved successfully: ${result.data.result.stepsSaved} steps", tag = Tags.API)
                Result.success(result)
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
            step.htmlAttributes?.get("placeholder") ?: step.htmlAttributes?.get("aria-label")
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

        // 1. 원본 셀렉터
        step.selector?.let { selectors.add(it) }

        // 2. ID 기반
        attrs?.get("id")?.let { id ->
            if (id.isNotBlank()) selectors.add("#$id")
        }

        // 3. Name 기반
        attrs?.get("name")?.let { name ->
            if (name.isNotBlank()) {
                selectors.add("[name='$name']")
            }
        }

        // 4. data-testid 기반
        attrs?.get("data-testid")?.let { testId ->
            if (testId.isNotBlank()) {
                selectors.add("[data-testid='$testId']")
            }
        }

        // 5. aria-label 기반
        attrs?.get("aria-label")?.let { label ->
            if (label.isNotBlank()) {
                selectors.add("[aria-label='$label']")
            }
        }

        // 6. Class 기반
        attrs?.get("class")?.let { className ->
            if (className.isNotBlank()) {
                val classes = className.split(" ").filter { it.isNotBlank() }
                if (classes.isNotEmpty()) {
                    selectors.add(".${classes.joinToString(".")}")
                }
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
        else            -> "click"
    }

    /**
     * 텍스트 레이블 추출
     */
    private fun extractTextLabels(step: ContributionStep): List<String> {
        val labels = mutableListOf<String>()
        val attrs = step.htmlAttributes

        attrs?.get("text")?.let { if (it.isNotBlank()) labels.add(it.trim()) }
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
            "click" -> if (text.isNotBlank()) "$text 클릭" else "요소 클릭"
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