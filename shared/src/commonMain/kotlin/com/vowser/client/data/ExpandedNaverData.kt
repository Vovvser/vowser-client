package com.vowser.client.data

/**
 * 네이버 기준 확장된 웹 탐색 데이터
 * 실제 네이버 웹사이트 구조를 기반으로 한 상세한 사이트맵
 */
object ExpandedNaverDataGenerator {
    
    fun createExpandedNaverData(): WebNavigationGraph {
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
                id = "naver_main",
                name = "네이버 메인",
                type = WebNodeType.WEBSITE,
                url = "https://www.naver.com",
                description = "네이버 포털 메인 페이지",
                keywords = setOf("네이버", "포털", "메인", "홈"),
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
            ),
            WebNode(
                id = "kakao",
                name = "카카오",
                type = WebNodeType.WEBSITE,
                url = "https://www.kakao.com",
                description = "카카오 서비스 포털",
                keywords = setOf("카카오", "메신저", "톡"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            )
        )
        
        websites.forEach { graph.addNode(it) }
        
        // === CATEGORY 노드들 (Depth 2) - 네이버 중심 대폭 확장 ===
        val categories = listOf(
            // === 네이버 주요 서비스 카테고리 ===
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
            WebNode(
                id = "naver_blog",
                name = "네이버 블로그",
                type = WebNodeType.CATEGORY,
                url = "https://blog.naver.com",
                description = "개인 블로그 서비스",
                keywords = setOf("블로그", "일기", "포스팅"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_cafe",
                name = "네이버 카페",
                type = WebNodeType.CATEGORY,
                url = "https://cafe.naver.com",
                description = "커뮤니티 카페 서비스",
                keywords = setOf("카페", "커뮤니티", "모임"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_map",
                name = "네이버 지도",
                type = WebNodeType.CATEGORY,
                url = "https://map.naver.com",
                description = "지도 및 길찾기 서비스",
                keywords = setOf("지도", "길찾기", "위치", "네비"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_mail",
                name = "네이버 메일",
                type = WebNodeType.CATEGORY,
                url = "https://mail.naver.com",
                description = "이메일 서비스",
                keywords = setOf("메일", "이메일", "편지"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_tv",
                name = "네이버 TV",
                type = WebNodeType.CATEGORY,
                url = "https://tv.naver.com",
                description = "동영상 스트리밍 서비스",
                keywords = setOf("TV", "동영상", "방송", "드라마"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_pay",
                name = "네이버페이",
                type = WebNodeType.CATEGORY,
                url = "https://pay.naver.com",
                description = "간편 결제 서비스",
                keywords = setOf("페이", "결제", "포인트", "적립"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "naver_search",
                name = "네이버 검색",
                type = WebNodeType.CATEGORY,
                description = "통합 검색 서비스",
                keywords = setOf("검색", "찾기", "서치"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_weather",
                name = "네이버 날씨",
                type = WebNodeType.CATEGORY,
                description = "날씨 정보 서비스",
                keywords = setOf("날씨", "기온", "비", "맑음"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "naver_sports",
                name = "네이버 스포츠",
                type = WebNodeType.CATEGORY,
                url = "https://sports.naver.com",
                description = "스포츠 뉴스 및 정보",
                keywords = setOf("스포츠", "축구", "야구", "농구"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // === 유튜브 카테고리 ===
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
            WebNode(
                id = "youtube_gaming",
                name = "유튜브 게이밍",
                type = WebNodeType.CATEGORY,
                description = "게임 관련 영상",
                keywords = setOf("게임", "게이밍", "플레이"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // === 쿠팡 카테고리 ===
            WebNode(
                id = "coupang_electronics",
                name = "쿠팡 전자제품",
                type = WebNodeType.CATEGORY,
                description = "전자제품 카테고리",
                keywords = setOf("전자제품", "컴퓨터", "폰", "스마트폰"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "coupang_fashion",
                name = "쿠팡 패션",
                type = WebNodeType.CATEGORY,
                description = "의류 및 패션 아이템",
                keywords = setOf("패션", "옷", "의류", "신발"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // === 카카오 카테고리 ===
            WebNode(
                id = "kakaotalk",
                name = "카카오톡",
                type = WebNodeType.CATEGORY,
                description = "메신저 서비스",
                keywords = setOf("카카오톡", "메신저", "채팅"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            )
        )
        
        categories.forEach { graph.addNode(it) }
        
        // === CONTENT 노드들 (Depth 3) - 대폭 확장 ===
        val contents = listOf(
            // === 네이버 웹툰 콘텐츠 ===
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
            WebNode(
                id = "webtoon_romance",
                name = "로맨스 웹툰",
                type = WebNodeType.CONTENT,
                description = "로맨스 장르 웹툰",
                keywords = setOf("로맨스", "사랑", "연애"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "webtoon_action",
                name = "액션 웹툰",
                type = WebNodeType.CONTENT,
                description = "액션 장르 웹툰",
                keywords = setOf("액션", "전투", "모험"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "webtoon_comedy",
                name = "개그 웹툰",
                type = WebNodeType.CONTENT,
                description = "코미디 장르 웹툰",
                keywords = setOf("개그", "코미디", "웃긴"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "webtoon_daily",
                name = "일상 웹툰",
                type = WebNodeType.CONTENT,
                description = "일상 소재 웹툰",
                keywords = setOf("일상", "생활", "에세이"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // === 네이버 뉴스 콘텐츠 ===
            WebNode(
                id = "news_politics",
                name = "정치 뉴스",
                type = WebNodeType.CONTENT,
                description = "정치 관련 뉴스",
                keywords = setOf("정치", "국정", "정부"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_economy",
                name = "경제 뉴스",
                type = WebNodeType.CONTENT,
                description = "경제 관련 뉴스",
                keywords = setOf("경제", "주식", "부동산"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_society",
                name = "사회 뉴스",
                type = WebNodeType.CONTENT,
                description = "사회 이슈 뉴스",
                keywords = setOf("사회", "이슈", "사건"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_world",
                name = "해외 뉴스",
                type = WebNodeType.CONTENT,
                description = "해외 소식",
                keywords = setOf("해외", "국제", "세계"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_it",
                name = "IT 뉴스",
                type = WebNodeType.CONTENT,
                description = "IT 기술 뉴스",
                keywords = setOf("IT", "기술", "인터넷"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "news_entertainment",
                name = "연예 뉴스",
                type = WebNodeType.CONTENT,
                description = "연예계 소식",
                keywords = setOf("연예", "연예인", "가수"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // === 네이버 스포츠 콘텐츠 ===
            WebNode(
                id = "sports_baseball",
                name = "야구 뉴스",
                type = WebNodeType.CONTENT,
                description = "야구 관련 뉴스",
                keywords = setOf("야구", "KBO", "메이저리그"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "sports_soccer",
                name = "축구 뉴스",
                type = WebNodeType.CONTENT,
                description = "축구 관련 뉴스",
                keywords = setOf("축구", "K리그", "월드컵"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "sports_basketball",
                name = "농구 뉴스",
                type = WebNodeType.CONTENT,
                description = "농구 관련 뉴스",
                keywords = setOf("농구", "KBL", "NBA"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "sports_volleyball",
                name = "배구 뉴스",
                type = WebNodeType.CONTENT,
                description = "배구 관련 뉴스",
                keywords = setOf("배구", "V리그"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // === 네이버 쇼핑 콘텐츠 ===
            WebNode(
                id = "shopping_fashion",
                name = "패션 상품",
                type = WebNodeType.CONTENT,
                description = "패션 아이템",
                keywords = setOf("패션", "옷", "신발"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "shopping_beauty",
                name = "뷰티 상품",
                type = WebNodeType.CONTENT,
                description = "화장품 및 뷰티 제품",
                keywords = setOf("뷰티", "화장품", "스킨케어"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "shopping_home",
                name = "홈/리빙 상품",
                type = WebNodeType.CONTENT,
                description = "생활용품 및 가구",
                keywords = setOf("홈", "가구", "생활용품"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "shopping_food",
                name = "식품 상품",
                type = WebNodeType.CONTENT,
                description = "식품 및 건강식품",
                keywords = setOf("식품", "음식", "건강식품"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // === 네이버 블로그 콘텐츠 ===
            WebNode(
                id = "blog_recipe",
                name = "요리 블로그",
                type = WebNodeType.CONTENT,
                description = "요리 레시피 블로그",
                keywords = setOf("요리", "레시피", "음식"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "blog_travel",
                name = "여행 블로그",
                type = WebNodeType.CONTENT,
                description = "여행 후기 및 정보",
                keywords = setOf("여행", "관광", "맛집"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "blog_parenting",
                name = "육아 블로그",
                type = WebNodeType.CONTENT,
                description = "육아 정보 및 경험담",
                keywords = setOf("육아", "아이", "교육"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            WebNode(
                id = "blog_lifestyle",
                name = "라이프스타일 블로그",
                type = WebNodeType.CONTENT,
                description = "일상 및 라이프스타일",
                keywords = setOf("라이프스타일", "일상", "취미"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.EASY)
            ),
            
            // === 네이버 카페 콘텐츠 ===
            WebNode(
                id = "cafe_hobby",
                name = "취미 카페",
                type = WebNodeType.CONTENT,
                description = "취미 관련 카페",
                keywords = setOf("취미", "동호회", "모임"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "cafe_local",
                name = "지역 카페",
                type = WebNodeType.CONTENT,
                description = "지역별 정보 카페",
                keywords = setOf("지역", "동네", "정보"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "cafe_study",
                name = "스터디 카페",
                type = WebNodeType.CONTENT,
                description = "학습 관련 카페",
                keywords = setOf("스터디", "공부", "학습"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // === 네이버 TV 콘텐츠 ===
            WebNode(
                id = "tv_drama",
                name = "드라마",
                type = WebNodeType.CONTENT,
                description = "한국 드라마 콘텐츠",
                keywords = setOf("드라마", "한드", "로맨스"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "tv_variety",
                name = "예능 프로그램",
                type = WebNodeType.CONTENT,
                description = "예능 및 버라이어티 쇼",
                keywords = setOf("예능", "버라이어티", "웃긴"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "tv_music",
                name = "음악 프로그램",
                type = WebNodeType.CONTENT,
                description = "음악 방송 프로그램",
                keywords = setOf("음악방송", "가요", "콘서트"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // === 유튜브 음악 콘텐츠 ===
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
            WebNode(
                id = "music_pop",
                name = "팝 플레이리스트",
                type = WebNodeType.CONTENT,
                description = "팝송 음악 모음",
                keywords = setOf("팝", "팝송", "해외음악"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            WebNode(
                id = "music_trot",
                name = "트로트 플레이리스트",
                type = WebNodeType.CONTENT,
                description = "트로트 음악 모음",
                keywords = setOf("트로트", "트럿", "국악"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.MEDIUM)
            ),
            
            // === 전자제품 콘텐츠 ===
            WebNode(
                id = "phone_samsung",
                name = "삼성 스마트폰",
                type = WebNodeType.CONTENT,
                description = "삼성 갤럭시 시리즈",
                keywords = setOf("삼성", "갤럭시", "스마트폰", "핸드폰"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            ),
            WebNode(
                id = "phone_apple",
                name = "애플 아이폰",
                type = WebNodeType.CONTENT,
                description = "애플 아이폰 시리즈",
                keywords = setOf("애플", "아이폰", "iPhone"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            ),
            WebNode(
                id = "laptop_samsung",
                name = "삼성 노트북",
                type = WebNodeType.CONTENT,
                description = "삼성 갤럭시북 시리즈",
                keywords = setOf("삼성", "갤럭시북", "노트북"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            ),
            WebNode(
                id = "laptop_lg",
                name = "LG 노트북",
                type = WebNodeType.CONTENT,
                description = "LG 그램 시리즈",
                keywords = setOf("LG", "그램", "노트북"),
                accessibility = AccessibilityInfo(difficulty = AccessibilityDifficulty.HARD)
            )
        )
        
        contents.forEach { graph.addNode(it) }
        
        // === 관계(엣지) 생성 ===
        
        // === Root -> Websites ===
        listOf(
            "root" to "naver_main",
            "root" to "youtube", 
            "root" to "coupang",
            "root" to "kakao"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.VOICE_COMMAND,
                description = "${graph.getNode(to)?.name}으로 이동",
                estimatedTime = 2,
                requiredClicks = 0
            ))
        }
        
        // === Naver Main -> Categories ===
        listOf(
            "naver_main" to "naver_webtoon",
            "naver_main" to "naver_news",
            "naver_main" to "naver_shopping",
            "naver_main" to "naver_blog",
            "naver_main" to "naver_cafe",
            "naver_main" to "naver_map",
            "naver_main" to "naver_mail",
            "naver_main" to "naver_tv",
            "naver_main" to "naver_pay",
            "naver_main" to "naver_search",
            "naver_main" to "naver_weather",
            "naver_main" to "naver_sports"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.CLICK_MENU,
                description = "${graph.getNode(to)?.name} 메뉴 클릭",
                estimatedTime = 2,
                requiredClicks = 1
            ))
        }
        
        // === YouTube -> Categories ===
        listOf(
            "youtube" to "youtube_music",
            "youtube" to "youtube_trending",
            "youtube" to "youtube_gaming"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.CLICK_MENU,
                description = "${graph.getNode(to)?.name} 탭 클릭",
                estimatedTime = 2,
                requiredClicks = 1
            ))
        }
        
        // === Coupang -> Categories ===
        listOf(
            "coupang" to "coupang_electronics",
            "coupang" to "coupang_fashion"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.CLICK_MENU,
                description = "${graph.getNode(to)?.name} 카테고리 선택",
                estimatedTime = 3,
                requiredClicks = 2
            ))
        }
        
        // === Kakao -> Categories ===
        graph.addRelationship(WebRelationship(
            id = "kakao_to_kakaotalk",
            fromNodeId = "kakao",
            toNodeId = "kakaotalk",
            actionType = NavigationAction.CLICK_MENU,
            description = "카카오톡 서비스 선택",
            estimatedTime = 2,
            requiredClicks = 1
        ))
        
        // === Categories -> Contents ===
        
        // 네이버 웹툰 -> 웹툰 콘텐츠
        listOf(
            "naver_webtoon" to "webtoon_popular",
            "naver_webtoon" to "webtoon_new",
            "naver_webtoon" to "webtoon_romance",
            "naver_webtoon" to "webtoon_action",
            "naver_webtoon" to "webtoon_comedy",
            "naver_webtoon" to "webtoon_daily"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SELECT,
                description = "${graph.getNode(to)?.name} 선택",
                estimatedTime = 1,
                requiredClicks = 1
            ))
        }
        
        // 네이버 뉴스 -> 뉴스 콘텐츠
        listOf(
            "naver_news" to "news_politics",
            "naver_news" to "news_economy",
            "naver_news" to "news_society",
            "naver_news" to "news_world",
            "naver_news" to "news_it",
            "naver_news" to "news_entertainment"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SELECT,
                description = "${graph.getNode(to)?.name} 선택",
                estimatedTime = 1,
                requiredClicks = 1
            ))
        }
        
        // 네이버 스포츠 -> 스포츠 콘텐츠
        listOf(
            "naver_sports" to "sports_baseball",
            "naver_sports" to "sports_soccer",
            "naver_sports" to "sports_basketball", 
            "naver_sports" to "sports_volleyball"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SELECT,
                description = "${graph.getNode(to)?.name} 선택",
                estimatedTime = 1,
                requiredClicks = 1
            ))
        }
        
        // 네이버 쇼핑 -> 쇼핑 콘텐츠
        listOf(
            "naver_shopping" to "shopping_fashion",
            "naver_shopping" to "shopping_beauty",
            "naver_shopping" to "shopping_home",
            "naver_shopping" to "shopping_food"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SELECT,
                description = "${graph.getNode(to)?.name} 카테고리 선택",
                estimatedTime = 2,
                requiredClicks = 1
            ))
        }
        
        // 네이버 블로그 -> 블로그 콘텐츠
        listOf(
            "naver_blog" to "blog_recipe",
            "naver_blog" to "blog_travel",
            "naver_blog" to "blog_parenting",
            "naver_blog" to "blog_lifestyle"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SEARCH,
                description = "${graph.getNode(to)?.name} 검색",
                estimatedTime = 3,
                requiredClicks = 2
            ))
        }
        
        // 네이버 카페 -> 카페 콘텐츠
        listOf(
            "naver_cafe" to "cafe_hobby",
            "naver_cafe" to "cafe_local",
            "naver_cafe" to "cafe_study"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SEARCH,
                description = "${graph.getNode(to)?.name} 찾기",
                estimatedTime = 4,
                requiredClicks = 3
            ))
        }
        
        // 네이버 TV -> TV 콘텐츠
        listOf(
            "naver_tv" to "tv_drama",
            "naver_tv" to "tv_variety",
            "naver_tv" to "tv_music"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SELECT,
                description = "${graph.getNode(to)?.name} 선택",
                estimatedTime = 2,
                requiredClicks = 1
            ))
        }
        
        // 유튜브 뮤직 -> 음악 콘텐츠
        listOf(
            "youtube_music" to "music_kpop",
            "youtube_music" to "music_ballad",
            "youtube_music" to "music_pop",
            "youtube_music" to "music_trot"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.SEARCH,
                description = "${graph.getNode(to)?.name} 검색",
                estimatedTime = 3,
                requiredClicks = 2
            ))
        }
        
        // 쿠팡 전자제품 -> 전자제품 콘텐츠
        listOf(
            "coupang_electronics" to "phone_samsung",
            "coupang_electronics" to "phone_apple",
            "coupang_electronics" to "laptop_samsung",
            "coupang_electronics" to "laptop_lg"
        ).forEach { (from, to) ->
            graph.addRelationship(WebRelationship(
                id = "${from}_to_${to}",
                fromNodeId = from,
                toNodeId = to,
                actionType = NavigationAction.FILTER,
                description = "${graph.getNode(to)?.name} 필터링",
                estimatedTime = 5,
                requiredClicks = 3
            ))
        }
        
        return graph
    }
}