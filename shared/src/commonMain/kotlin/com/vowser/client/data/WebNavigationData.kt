package com.vowser.client.data

/**
 * Neo4j와 유사한 웹 탐색 데이터 구조
 * Depth 3까지 지원하는 계층형 웹사이트 구조
 */

/**
 * 웹 노드의 타입을 정의
 */
enum class WebNodeType(val displayName: String, val depth: Int) {
    ROOT("시작", 0),           // 음성 명령 시작점
    WEBSITE("웹사이트", 1),     // naver.com, youtube.com 등
    CATEGORY("카테고리", 2),    // 웹툰, 뮤직, 쇼핑 등
    CONTENT("콘텐츠", 3)       // 구체적인 웹툰, 영상, 상품 등
}

/**
 * 웹 탐색 노드
 */
data class WebNode(
    val id: String,
    val name: String,
    val type: WebNodeType,
    val url: String? = null,
    val description: String? = null,
    val keywords: Set<String> = emptySet(), // 음성 인식을 위한 키워드
    val accessibility: AccessibilityInfo? = null
)

/**
 * 접근성 정보
 */
data class AccessibilityInfo(
    val hasKeyboardNavigation: Boolean = true,
    val hasScreenReaderSupport: Boolean = true,
    val hasVoiceControl: Boolean = false,
    val difficulty: AccessibilityDifficulty = AccessibilityDifficulty.MEDIUM
)

enum class AccessibilityDifficulty {
    EASY,    // 클릭 1-2번으로 접근 가능
    MEDIUM,  // 클릭 3-4번 필요
    HARD     // 복잡한 탐색 경로 필요
}

/**
 * 웹 탐색 관계 (엣지)
 */
data class WebRelationship(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val actionType: NavigationAction,
    val description: String,
    val estimatedTime: Int = 1, // 초 단위
    val requiredClicks: Int = 1,
    val isDirectPath: Boolean = true // 직접 경로인지 여부
)

/**
 * 탐색 액션 타입
 */
enum class NavigationAction(val displayName: String) {
    VOICE_COMMAND("음성 명령"),
    NAVIGATE("사이트 접속"),
    CLICK_MENU("메뉴 클릭"),
    SEARCH("검색"),
    SELECT("선택"),
    SCROLL("스크롤"),
    FILTER("필터링")
}

/**
 * 웹 탐색 그래프 (Neo4j 스타일)
 */
class WebNavigationGraph {
    private val nodes = mutableMapOf<String, WebNode>()
    private val relationships = mutableMapOf<String, WebRelationship>()
    
    fun addNode(node: WebNode) {
        nodes[node.id] = node
    }
    
    fun addRelationship(relationship: WebRelationship) {
        relationships[relationship.id] = relationship
    }
    
    fun getNode(id: String): WebNode? = nodes[id]
    
    fun getAllNodes(): List<WebNode> = nodes.values.toList()
    
    fun getAllRelationships(): List<WebRelationship> = relationships.values.toList()
    
    fun getNodesByType(type: WebNodeType): List<WebNode> {
        return nodes.values.filter { it.type == type }
    }
    
    fun getRelationshipsFrom(nodeId: String): List<WebRelationship> {
        return relationships.values.filter { it.fromNodeId == nodeId }
    }
    
    fun getRelationshipsTo(nodeId: String): List<WebRelationship> {
        return relationships.values.filter { it.toNodeId == nodeId }
    }
    
    fun findPath(fromId: String, toId: String): List<WebRelationship>? {
        // 간단한 BFS 경로 찾기
        val visited = mutableSetOf<String>()
        val queue = mutableListOf<Pair<String, List<WebRelationship>>>()
        
        queue.add(fromId to emptyList())
        visited.add(fromId)
        
        while (queue.isNotEmpty()) {
            val (currentId, path) = queue.removeFirst()
            
            if (currentId == toId) {
                return path
            }
            
            getRelationshipsFrom(currentId).forEach { rel ->
                if (rel.toNodeId !in visited) {
                    visited.add(rel.toNodeId)
                    queue.add(rel.toNodeId to path + rel)
                }
            }
        }
        
        return null
    }
}

/**
 * 더미 데이터 생성기
 */
object WebNavigationDataGenerator {
    
    fun createSampleData(): WebNavigationGraph {
        val graph = WebNavigationGraph()
        
        // === ROOT 노드 (Depth 0) ===
        val rootNode = WebNode(
            id = "root",
            name = "음성 명령 시작",
            type = WebNodeType.ROOT,
            description = "사용자의 음성 명령을 받는 시작점",
            keywords = setOf("시작", "명령", "도움", "찾아줘")
        )
        graph.addNode(rootNode)
        
        // === WEBSITE 노드들 (Depth 1) ===
        val websites = listOf(
            WebNode(
                id = "naver",
                name = "네이버",
                type = WebNodeType.WEBSITE,
                url = "https://www.naver.com",
                description = "한국 최대 포털 사이트",
                keywords = setOf("네이버", "포털", "검색"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "youtube",
                name = "유튜브",
                type = WebNodeType.WEBSITE,
                url = "https://www.youtube.com",
                description = "동영상 플랫폼",
                keywords = setOf("유튜브", "동영상", "영상", "비디오"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "coupang",
                name = "쿠팡",
                type = WebNodeType.WEBSITE,
                url = "https://www.coupang.com",
                description = "온라인 쇼핑몰",
                keywords = setOf("쿠팡", "쇼핑", "구매", "주문"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            )
        )
        
        websites.forEach { graph.addNode(it) }
        
        // === CATEGORY 노드들 (Depth 2) ===
        val categories = listOf(
            // 네이버 카테고리
            WebNode(
                id = "naver_webtoon",
                name = "네이버 웹툰",
                type = WebNodeType.CATEGORY,
                url = "https://comic.naver.com",
                description = "네이버 웹툰 서비스",
                keywords = setOf("웹툰", "만화", "코믹"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_news",
                name = "네이버 뉴스",
                type = WebNodeType.CATEGORY,
                url = "https://news.naver.com",
                description = "뉴스 및 기사",
                keywords = setOf("뉴스", "기사", "소식"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_shopping",
                name = "네이버 쇼핑",
                type = WebNodeType.CATEGORY,
                url = "https://shopping.naver.com",
                description = "네이버 쇼핑 서비스",
                keywords = setOf("쇼핑", "구매", "상품"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // 유튜브 카테고리
            WebNode(
                id = "youtube_music",
                name = "유튜브 뮤직",
                type = WebNodeType.CATEGORY,
                url = "https://music.youtube.com",
                description = "음악 스트리밍",
                keywords = setOf("음악", "뮤직", "노래", "플레이리스트"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "youtube_trending",
                name = "유튜브 인기",
                type = WebNodeType.CATEGORY,
                description = "인기 동영상",
                keywords = setOf("인기", "트렌드", "핫한", "많이본"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // 쿠팡 카테고리
            WebNode(
                id = "coupang_electronics",
                name = "쿠팡 전자제품",
                type = WebNodeType.CATEGORY,
                description = "전자제품 카테고리",
                keywords = setOf("전자제품", "컴퓨터", "폰", "스마트폰"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            )
        )
        
        categories.forEach { graph.addNode(it) }
        
        // === CONTENT 노드들 (Depth 3) ===
        val contents = listOf(
            // 웹툰 콘텐츠
            WebNode(
                id = "webtoon_popular",
                name = "인기 웹툰",
                type = WebNodeType.CONTENT,
                description = "현재 인기 있는 웹툰들",
                keywords = setOf("인기 웹툰", "베스트", "추천"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "webtoon_new",
                name = "신작 웹툰",
                type = WebNodeType.CONTENT,
                description = "새로 연재되는 웹툰들",
                keywords = setOf("신작", "새로운", "최신"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // 뉴스 콘텐츠
            WebNode(
                id = "news_politics",
                name = "정치 뉴스",
                type = WebNodeType.CONTENT,
                description = "정치 관련 뉴스",
                keywords = setOf("정치", "국정", "정부"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_sports",
                name = "스포츠 뉴스",
                type = WebNodeType.CONTENT,
                description = "스포츠 관련 뉴스",
                keywords = setOf("스포츠", "축구", "야구", "운동"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // 음악 콘텐츠
            WebNode(
                id = "music_kpop",
                name = "K-POP 플레이리스트",
                type = WebNodeType.CONTENT,
                description = "한국 팝 음악 모음",
                keywords = setOf("케이팝", "k-pop", "한국 음악", "아이돌"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "music_ballad",
                name = "발라드 플레이리스트",
                type = WebNodeType.CONTENT,
                description = "발라드 음악 모음",
                keywords = setOf("발라드", "슬픈 노래", "감성"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // 전자제품 콘텐츠
            WebNode(
                id = "phone_samsung",
                name = "삼성 스마트폰",
                type = WebNodeType.CONTENT,
                description = "삼성 갤럭시 시리즈",
                keywords = setOf("삼성", "갤럭시", "스마트폰", "핸드폰"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            )
        )
        
        contents.forEach { graph.addNode(it) }
        
        // === 관계(엣지) 생성 ===
        
        // Root -> Websites
        graph.addRelationship(WebRelationship(
            id = "root_to_naver",
            fromNodeId = "root",
            toNodeId = "naver",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "네이버로 이동",
            estimatedTime = 2,
            requiredClicks = 0
        ))
        
        graph.addRelationship(WebRelationship(
            id = "root_to_youtube",
            fromNodeId = "root",
            toNodeId = "youtube",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "유튜브로 이동",
            estimatedTime = 3,
            requiredClicks = 0
        ))
        
        graph.addRelationship(WebRelationship(
            id = "root_to_coupang",
            fromNodeId = "root",
            toNodeId = "coupang",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "쿠팡으로 이동",
            estimatedTime = 2,
            requiredClicks = 0
        ))
        
        // Websites -> Categories
        graph.addRelationship(WebRelationship(
            id = "naver_to_webtoon",
            fromNodeId = "naver",
            toNodeId = "naver_webtoon",
            actionType = NavigationAction.CLICK_MENU,
            description = "웹툰 메뉴 클릭",
            estimatedTime = 2,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "naver_to_news",
            fromNodeId = "naver",
            toNodeId = "naver_news",
            actionType = NavigationAction.CLICK_MENU,
            description = "뉴스 메뉴 클릭",
            estimatedTime = 2,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "naver_to_shopping",
            fromNodeId = "naver",
            toNodeId = "naver_shopping",
            actionType = NavigationAction.CLICK_MENU,
            description = "쇼핑 메뉴 클릭",
            estimatedTime = 3,
            requiredClicks = 2
        ))
        
        graph.addRelationship(WebRelationship(
            id = "youtube_to_music",
            fromNodeId = "youtube",
            toNodeId = "youtube_music",
            actionType = NavigationAction.CLICK_MENU,
            description = "뮤직 메뉴 클릭",
            estimatedTime = 3,
            requiredClicks = 2
        ))
        
        graph.addRelationship(WebRelationship(
            id = "youtube_to_trending",
            fromNodeId = "youtube",
            toNodeId = "youtube_trending",
            actionType = NavigationAction.CLICK_MENU,
            description = "인기 탭 클릭",
            estimatedTime = 2,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "coupang_to_electronics",
            fromNodeId = "coupang",
            toNodeId = "coupang_electronics",
            actionType = NavigationAction.CLICK_MENU,
            description = "전자제품 카테고리 선택",
            estimatedTime = 4,
            requiredClicks = 3
        ))
        
        // Categories -> Contents
        graph.addRelationship(WebRelationship(
            id = "webtoon_to_popular",
            fromNodeId = "naver_webtoon",
            toNodeId = "webtoon_popular",
            actionType = NavigationAction.SELECT,
            description = "인기 웹툰 선택",
            estimatedTime = 2,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "webtoon_to_new",
            fromNodeId = "naver_webtoon",
            toNodeId = "webtoon_new",
            actionType = NavigationAction.SELECT,
            description = "신작 웹툰 선택",
            estimatedTime = 2,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "news_to_politics",
            fromNodeId = "naver_news",
            toNodeId = "news_politics",
            actionType = NavigationAction.SELECT,
            description = "정치 뉴스 선택",
            estimatedTime = 1,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "news_to_sports",
            fromNodeId = "naver_news",
            toNodeId = "news_sports",
            actionType = NavigationAction.SELECT,
            description = "스포츠 뉴스 선택",
            estimatedTime = 1,
            requiredClicks = 1
        ))
        
        graph.addRelationship(WebRelationship(
            id = "music_to_kpop",
            fromNodeId = "youtube_music",
            toNodeId = "music_kpop",
            actionType = NavigationAction.SEARCH,
            description = "K-POP 검색",
            estimatedTime = 5,
            requiredClicks = 2
        ))
        
        graph.addRelationship(WebRelationship(
            id = "music_to_ballad",
            fromNodeId = "youtube_music",
            toNodeId = "music_ballad",
            actionType = NavigationAction.SEARCH,
            description = "발라드 검색",
            estimatedTime = 5,
            requiredClicks = 2
        ))
        
        graph.addRelationship(WebRelationship(
            id = "electronics_to_samsung",
            fromNodeId = "coupang_electronics",
            toNodeId = "phone_samsung",
            actionType = NavigationAction.FILTER,
            description = "삼성 브랜드 필터링",
            estimatedTime = 8,
            requiredClicks = 4
        ))
        
        return graph
    }
}