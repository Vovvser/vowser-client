package com.vowser.client.data

/**
 * 실제 네이버 사용 패턴을 반영한 확장된 데이터 생성기
 * 시각 장애인 사용자의 일반적인 네이버 이용 시나리오를 포함
 */
object RealNaverDataGenerator {
    
    fun createExpandedNaverData(): WebNavigationGraph {
        val graph = WebNavigationGraph()
        
        // === ROOT 노드 (Depth 0) ===
        val rootNode = WebNode(
            id = "voice_start",
            name = "음성 명령 시작",
            type = WebNodeType.ROOT,
            description = "사용자의 음성 명령을 받는 시작점",
            keywords = setOf("안녕", "도움", "찾아줘", "알려줘", "보여줘", "해줘")
        )
        graph.addNode(rootNode)
        
        // === MAIN WEBSITE 노드 (Depth 1) ===
        val naverMain = WebNode(
            id = "naver_main",
            name = "네이버 메인",
            type = WebNodeType.WEBSITE,
            url = "https://www.naver.com",
            description = "네이버 메인 페이지 - 한국 최대 포털",
            keywords = setOf("네이버", "포털", "메인", "홈"),
            accessibility = AccessibilityInfo(
                hasKeyboardNavigation = true,
                hasScreenReaderSupport = true,
                hasVoiceControl = true,
                difficulty = AccessibilityDifficulty.EASY
            )
        )
        graph.addNode(naverMain)
        
        // === MAJOR CATEGORIES 노드들 (Depth 2) ===
        val majorCategories = listOf(
            WebNode(
                id = "naver_search",
                name = "네이버 검색",
                type = WebNodeType.CATEGORY,
                url = "https://search.naver.com",
                description = "통합 검색 서비스",
                keywords = setOf("검색", "찾기", "검색창", "서치"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_news",
                name = "네이버 뉴스",
                type = WebNodeType.CATEGORY,
                url = "https://news.naver.com",
                description = "뉴스 및 언론사 기사",
                keywords = setOf("뉴스", "기사", "소식", "언론", "신문"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_weather",
                name = "네이버 날씨",
                type = WebNodeType.CATEGORY,
                url = "https://weather.naver.com",
                description = "날씨 정보 및 기상 예보",
                keywords = setOf("날씨", "기온", "예보", "미세먼지", "비", "눈"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_webtoon",
                name = "네이버 웹툰",
                type = WebNodeType.CATEGORY,
                url = "https://comic.naver.com",
                description = "네이버 웹툰 서비스",
                keywords = setOf("웹툰", "만화", "코믹", "웹코믹", "만화책"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_shopping",
                name = "네이버 쇼핑",
                type = WebNodeType.CATEGORY,
                url = "https://shopping.naver.com",
                description = "네이버 쇼핑 서비스",
                keywords = setOf("쇼핑", "구매", "상품", "온라인", "판매"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_map",
                name = "네이버 지도",
                type = WebNodeType.CATEGORY,
                url = "https://map.naver.com",
                description = "지도 및 길찾기 서비스",
                keywords = setOf("지도", "길찾기", "위치", "장소", "주소", "길안내"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            ),
            WebNode(
                id = "naver_blog",
                name = "네이버 블로그",
                type = WebNodeType.CATEGORY,
                url = "https://blog.naver.com",
                description = "개인 블로그 서비스",
                keywords = setOf("블로그", "포스팅", "일기", "리뷰", "정보"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_cafe",
                name = "네이버 카페",
                type = WebNodeType.CATEGORY,
                url = "https://cafe.naver.com",
                description = "커뮤니티 카페 서비스",
                keywords = setOf("카페", "커뮤니티", "동호회", "모임", "게시판"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            )
        )
        
        majorCategories.forEach { graph.addNode(it) }
        
        // === DETAILED CONTENT 노드들 (Depth 3) ===
        
        // 뉴스 세부 카테고리
        val newsCategories = listOf(
            WebNode(
                id = "news_society",
                name = "사회 뉴스",
                type = WebNodeType.CONTENT,
                description = "사회 이슈 및 사건 뉴스",
                keywords = setOf("사회", "사건", "이슈", "시민"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_politics",
                name = "정치 뉴스",
                type = WebNodeType.CONTENT,
                description = "정치 관련 뉴스",
                keywords = setOf("정치", "국정", "정부", "국회", "선거"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_economy",
                name = "경제 뉴스",
                type = WebNodeType.CONTENT,
                description = "경제 및 증시 뉴스",
                keywords = setOf("경제", "주식", "증시", "금융", "돈"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_sports",
                name = "스포츠 뉴스",
                type = WebNodeType.CONTENT,
                description = "스포츠 경기 및 선수 뉴스",
                keywords = setOf("스포츠", "축구", "야구", "농구", "경기", "선수"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_entertainment",
                name = "연예 뉴스",
                type = WebNodeType.CONTENT,
                description = "연예인 및 문화 뉴스",
                keywords = setOf("연예", "연예인", "문화", "드라마", "영화", "음악"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            )
        )
        
        // 날씨 세부 정보
        val weatherDetails = listOf(
            WebNode(
                id = "weather_current",
                name = "현재 날씨",
                type = WebNodeType.CONTENT,
                description = "현재 기온과 날씨 상태",
                keywords = setOf("현재", "지금", "오늘", "기온", "온도"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "weather_forecast",
                name = "주간 예보",
                type = WebNodeType.CONTENT,
                description = "일주일 날씨 예보",
                keywords = setOf("예보", "주간", "일주일", "내일", "모레"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "weather_air",
                name = "미세먼지 정보",
                type = WebNodeType.CONTENT,
                description = "대기질 및 미세먼지 정보",
                keywords = setOf("미세먼지", "대기질", "공기", "황사"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            )
        )
        
        // 웹툰 세부 카테고리
        val webtoonCategories = listOf(
            WebNode(
                id = "webtoon_today",
                name = "오늘의 웹툰",
                type = WebNodeType.CONTENT,
                description = "요일별 연재 웹툰",
                keywords = setOf("오늘", "연재", "요일별", "업데이트"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "webtoon_romance",
                name = "로맨스 웹툰",
                type = WebNodeType.CONTENT,
                description = "로맨스 장르 웹툰",
                keywords = setOf("로맨스", "사랑", "연애", "로맨틱"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "webtoon_action",
                name = "액션 웹툰",
                type = WebNodeType.CONTENT,
                description = "액션 모험 웹툰",
                keywords = setOf("액션", "모험", "전투", "싸움"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "webtoon_comedy",
                name = "개그 웹툰",
                type = WebNodeType.CONTENT,
                description = "코미디 개그 웹툰",
                keywords = setOf("개그", "코미디", "웃긴", "재밌는"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            )
        )
        
        // 쇼핑 세부 카테고리
        val shoppingCategories = listOf(
            WebNode(
                id = "shopping_fashion",
                name = "패션/의류",
                type = WebNodeType.CONTENT,
                description = "의류 및 패션 아이템",
                keywords = setOf("옷", "의류", "패션", "입을거", "셔츠", "바지"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "shopping_beauty",
                name = "화장품/미용",
                type = WebNodeType.CONTENT,
                description = "화장품 및 뷰티 제품",
                keywords = setOf("화장품", "미용", "뷰티", "스킨케어", "샴푸"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "shopping_food",
                name = "식품/생필품",
                type = WebNodeType.CONTENT,
                description = "음식 및 생활필수품",
                keywords = setOf("식품", "음식", "생필품", "먹을거", "간식"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "shopping_digital",
                name = "디지털/가전",
                type = WebNodeType.CONTENT,
                description = "전자제품 및 디지털 기기",
                keywords = setOf("전자제품", "컴퓨터", "스마트폰", "핸드폰", "가전"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            )
        )
        
        // 검색 유형들
        val searchTypes = listOf(
            WebNode(
                id = "search_web",
                name = "통합 검색",
                type = WebNodeType.CONTENT,
                description = "일반 웹 검색",
                keywords = setOf("통합", "일반", "웹검색"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "search_blog",
                name = "블로그 검색",
                type = WebNodeType.CONTENT,
                description = "블로그 전용 검색",
                keywords = setOf("블로그", "후기", "리뷰", "경험"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "search_local",
                name = "지역 검색",
                type = WebNodeType.CONTENT,
                description = "지역 업체 및 장소 검색",
                keywords = setOf("지역", "맛집", "병원", "상가", "근처"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            )
        )
        
        // 지도 기능들
        val mapFeatures = listOf(
            WebNode(
                id = "map_directions",
                name = "길찾기",
                type = WebNodeType.CONTENT,
                description = "출발지에서 목적지까지 경로 안내",
                keywords = setOf("길찾기", "경로", "가는길", "어떻게가"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            ),
            WebNode(
                id = "map_restaurants",
                name = "맛집 찾기",
                type = WebNodeType.CONTENT,
                description = "주변 맛집 및 음식점 정보",
                keywords = setOf("맛집", "음식점", "레스토랑", "먹을곳"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            )
        )
        
        // 모든 콘텐츠 노드 추가
        (newsCategories + weatherDetails + webtoonCategories + 
         shoppingCategories + searchTypes + mapFeatures).forEach { graph.addNode(it) }
        
        // === RELATIONSHIPS 생성 ===
        
        // Root -> Naver Main
        graph.addRelationship(WebRelationship(
            id = "voice_to_naver",
            fromNodeId = "voice_start",
            toNodeId = "naver_main",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "음성 명령으로 네이버 접속",
            estimatedTime = 1,
            requiredClicks = 0
        ))
        
        // Naver Main -> Major Categories
        majorCategories.forEach { category ->
            graph.addRelationship(WebRelationship(
                id = "naver_to_${category.id}",
                fromNodeId = "naver_main",
                toNodeId = category.id,
                actionType = NavigationAction.CLICK_MENU,
                description = "${category.name} 메뉴로 이동",
                estimatedTime = when(category.accessibility?.difficulty) {
                    AccessibilityDifficulty.EASY -> 2
                    AccessibilityDifficulty.MEDIUM -> 3
                    AccessibilityDifficulty.HARD -> 5
                    else -> 3
                },
                requiredClicks = when(category.accessibility?.difficulty) {
                    AccessibilityDifficulty.EASY -> 1
                    AccessibilityDifficulty.MEDIUM -> 2
                    AccessibilityDifficulty.HARD -> 3
                    else -> 2
                }
            ))
        }
        
        // News -> News Categories
        newsCategories.forEach { news ->
            graph.addRelationship(WebRelationship(
                id = "news_to_${news.id}",
                fromNodeId = "naver_news",
                toNodeId = news.id,
                actionType = NavigationAction.SELECT,
                description = "${news.name} 섹션 선택",
                estimatedTime = 1,
                requiredClicks = 1
            ))
        }
        
        // Weather -> Weather Details
        weatherDetails.forEach { weather ->
            graph.addRelationship(WebRelationship(
                id = "weather_to_${weather.id}",
                fromNodeId = "naver_weather",
                toNodeId = weather.id,
                actionType = NavigationAction.SELECT,
                description = "${weather.name} 정보 확인",
                estimatedTime = 1,
                requiredClicks = 1
            ))
        }
        
        // Webtoon -> Webtoon Categories
        webtoonCategories.forEach { webtoon ->
            graph.addRelationship(WebRelationship(
                id = "webtoon_to_${webtoon.id}",
                fromNodeId = "naver_webtoon",
                toNodeId = webtoon.id,
                actionType = NavigationAction.SELECT,
                description = "${webtoon.name} 장르 선택",
                estimatedTime = 2,
                requiredClicks = 1
            ))
        }
        
        // Shopping -> Shopping Categories
        shoppingCategories.forEach { shopping ->
            graph.addRelationship(WebRelationship(
                id = "shopping_to_${shopping.id}",
                fromNodeId = "naver_shopping",
                toNodeId = shopping.id,
                actionType = NavigationAction.SELECT,
                description = "${shopping.name} 카테고리 선택",
                estimatedTime = 3,
                requiredClicks = 2
            ))
        }
        
        // Search -> Search Types
        searchTypes.forEach { searchType ->
            graph.addRelationship(WebRelationship(
                id = "search_to_${searchType.id}",
                fromNodeId = "naver_search",
                toNodeId = searchType.id,
                actionType = NavigationAction.SEARCH,
                description = "${searchType.name} 실행",
                estimatedTime = 2,
                requiredClicks = 1
            ))
        }
        
        // Map -> Map Features
        mapFeatures.forEach { mapFeature ->
            graph.addRelationship(WebRelationship(
                id = "map_to_${mapFeature.id}",
                fromNodeId = "naver_map",
                toNodeId = mapFeature.id,
                actionType = NavigationAction.SELECT,
                description = "${mapFeature.name} 기능 사용",
                estimatedTime = 4,
                requiredClicks = 3
            ))
        }
        
        // 시각 장애인 사용자를 위한 빠른 경로 추가
        // 자주 사용되는 기능들에 대한 직접 경로
        
        // 날씨 정보 빠른 접근
        graph.addRelationship(WebRelationship(
            id = "voice_to_weather_direct",
            fromNodeId = "voice_start",
            toNodeId = "weather_current",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "음성으로 날씨 바로 확인",
            estimatedTime = 2,
            requiredClicks = 0
        ))
        
        // 뉴스 빠른 접근
        graph.addRelationship(WebRelationship(
            id = "voice_to_news_direct",
            fromNodeId = "voice_start",
            toNodeId = "news_society",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "음성으로 뉴스 바로 확인",
            estimatedTime = 2,
            requiredClicks = 0
        ))
        
        // 웹툰 추천 빠른 접근
        graph.addRelationship(WebRelationship(
            id = "voice_to_webtoon_direct",
            fromNodeId = "voice_start",
            toNodeId = "webtoon_today",
            actionType = NavigationAction.VOICE_COMMAND,
            description = "음성으로 웹툰 추천 바로 확인",
            estimatedTime = 3,
            requiredClicks = 0
        ))
        
        return graph
    }
    
    /**
     * 음성 명령어와 목표 노드를 매핑하는 테스트 시나리오
     */
    fun getVoiceTestScenarios(): List<VoiceTestScenario> {
        return listOf(
            VoiceTestScenario(
                voiceCommand = "오늘 날씨는 어때?",
                keywords = listOf("날씨", "오늘"),
                targetNodeId = "weather_current",
                expectedPath = listOf("voice_start", "weather_current"),
                estimatedTime = 2,
                difficulty = AccessibilityDifficulty.EASY,
                description = "현재 날씨 정보 확인"
            ),
            VoiceTestScenario(
                voiceCommand = "만화 추천해줘",
                keywords = listOf("만화", "추천", "웹툰"),
                targetNodeId = "webtoon_today",
                expectedPath = listOf("voice_start", "webtoon_today"),
                estimatedTime = 3,
                difficulty = AccessibilityDifficulty.MEDIUM,
                description = "오늘 업데이트된 웹툰 추천"
            ),
            VoiceTestScenario(
                voiceCommand = "최근에 무슨 일이 있었어?",
                keywords = listOf("최근", "뉴스", "일"),
                targetNodeId = "news_society",
                expectedPath = listOf("voice_start", "news_society"),
                estimatedTime = 2,
                difficulty = AccessibilityDifficulty.EASY,
                description = "최근 사회 뉴스 확인"
            ),
            VoiceTestScenario(
                voiceCommand = "강남역 근처 맛집 찾아줘",
                keywords = listOf("강남역", "맛집", "근처"),
                targetNodeId = "map_restaurants",
                expectedPath = listOf("voice_start", "naver_main", "naver_map", "map_restaurants"),
                estimatedTime = 8,
                difficulty = AccessibilityDifficulty.HARD,
                description = "지역 맛집 검색"
            ),
            VoiceTestScenario(
                voiceCommand = "샴푸 어디서 싸게 살 수 있어?",
                keywords = listOf("샴푸", "쇼핑", "싸게"),
                targetNodeId = "shopping_beauty",
                expectedPath = listOf("voice_start", "naver_main", "naver_shopping", "shopping_beauty"),
                estimatedTime = 6,
                difficulty = AccessibilityDifficulty.MEDIUM,
                description = "화장품/미용 제품 쇼핑"
            ),
            VoiceTestScenario(
                voiceCommand = "집에서 병원까지 어떻게 가?",
                keywords = listOf("집", "병원", "어떻게", "길찾기"),
                targetNodeId = "map_directions",
                expectedPath = listOf("voice_start", "naver_main", "naver_map", "map_directions"),
                estimatedTime = 8,
                difficulty = AccessibilityDifficulty.HARD,
                description = "길찾기 서비스 이용"
            ),
            VoiceTestScenario(
                voiceCommand = "감기에 좋은 음식이 뭐야?",
                keywords = listOf("감기", "음식", "건강", "좋은"),
                targetNodeId = "search_blog",
                expectedPath = listOf("voice_start", "naver_main", "naver_search", "search_blog"),
                estimatedTime = 5,
                difficulty = AccessibilityDifficulty.EASY,
                description = "건강 정보 블로그 검색"
            ),
            VoiceTestScenario(
                voiceCommand = "어제 야구 경기 결과 알려줘",
                keywords = listOf("야구", "경기", "결과", "스포츠"),
                targetNodeId = "news_sports",
                expectedPath = listOf("voice_start", "naver_main", "naver_news", "news_sports"),
                estimatedTime = 4,
                difficulty = AccessibilityDifficulty.EASY,
                description = "스포츠 뉴스 확인"
            ),
            VoiceTestScenario(
                voiceCommand = "간단한 저녁 요리 알려줘",
                keywords = listOf("요리", "저녁", "간단", "레시피"),
                targetNodeId = "search_blog",
                expectedPath = listOf("voice_start", "naver_main", "naver_search", "search_blog"),
                estimatedTime = 5,
                difficulty = AccessibilityDifficulty.EASY,
                description = "요리 레시피 블로그 검색"
            ),
            VoiceTestScenario(
                voiceCommand = "비슷한 관심사 사람들과 이야기하고 싶어",
                keywords = listOf("관심사", "사람", "이야기", "커뮤니티"),
                targetNodeId = "naver_cafe",
                expectedPath = listOf("voice_start", "naver_main", "naver_cafe"),
                estimatedTime = 5,
                difficulty = AccessibilityDifficulty.MEDIUM,
                description = "커뮤니티 카페 접속"
            )
        )
    }
}

/**
 * 음성 테스트 시나리오 데이터 클래스
 */
data class VoiceTestScenario(
    val voiceCommand: String,
    val keywords: List<String>,
    val targetNodeId: String,
    val expectedPath: List<String>,
    val estimatedTime: Int,
    val difficulty: AccessibilityDifficulty,
    val description: String
)