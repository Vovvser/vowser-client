package com.vowser.client.voice

/**
 * 확장된 네이버 기반 음성 명령어 세트
 * 4배 확장된 더미데이터에 맞춘 상세한 명령어들
 */
object ExpandedVoiceCommands {
    
    fun getExpandedCommandSet(): List<VoiceCommand> {
        return listOf(
            
            // ========== 네이버 웹툰 관련 확장 명령어 ==========
            VoiceCommand(
                id = "webtoon_popular",
                command = "인기 웹툰 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("인기", "웹툰", "보고싶다", "추천"),
                expectedPath = listOf("root", "naver_main", "naver_webtoon", "webtoon_popular"),
                description = "네이버 인기 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "웹툰 추천해줘", "재밌는 웹툰 찾아줘", "인기 만화 보여줘"
                )
            ),
            
            VoiceCommand(
                id = "webtoon_romance",
                command = "로맨스 웹툰 읽고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("로맨스", "웹툰", "사랑", "연애"),
                expectedPath = listOf("root", "naver_main", "naver_webtoon", "webtoon_romance"),
                description = "로맨스 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "사랑 이야기 웹툰 찾아줘", "연애 웹툰 보고 싶어"
                )
            ),
            
            VoiceCommand(
                id = "webtoon_action",
                command = "액션 웹툰 보려고",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("액션", "웹툰", "전투", "모험"),
                expectedPath = listOf("root", "naver_main", "naver_webtoon", "webtoon_action"),
                description = "액션 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "전투 웹툰 찾아줘", "모험 웹툰 보고 싶어"
                )
            ),
            
            VoiceCommand(
                id = "webtoon_comedy",
                command = "웃긴 웹툰 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("웃긴", "개그", "코미디", "웹툰"),
                expectedPath = listOf("root", "naver_main", "naver_webtoon", "webtoon_comedy"),
                description = "개그 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "개그 웹툰 찾아줘", "재밌는 만화 보여줘"
                )
            ),
            
            VoiceCommand(
                id = "webtoon_daily",
                command = "일상 웹툰 읽을래",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("일상", "웹툰", "생활", "에세이"),
                expectedPath = listOf("root", "naver_main", "naver_webtoon", "webtoon_daily"),
                description = "일상 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "생활 웹툰 찾아줘", "일상 만화 보고 싶어"
                )
            ),
            
            // ========== 네이버 뉴스 관련 확장 명령어 ==========
            VoiceCommand(
                id = "news_politics",
                command = "정치 뉴스 알려줘",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("정치", "뉴스", "국정", "정부"),
                expectedPath = listOf("root", "naver_main", "naver_news", "news_politics"),
                description = "정치 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "정치 소식 보여줘", "국정 뉴스 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "news_economy",
                command = "경제 뉴스 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("경제", "뉴스", "주식", "부동산"),
                expectedPath = listOf("root", "naver_main", "naver_news", "news_economy"),
                description = "경제 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "주식 소식 알려줘", "부동산 뉴스 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "news_entertainment",
                command = "연예 뉴스 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("연예", "뉴스", "연예인", "가수"),
                expectedPath = listOf("root", "naver_main", "naver_news", "news_entertainment"),
                description = "연예 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "연예인 소식 알려줘", "가수 뉴스 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "news_it",
                command = "IT 뉴스 확인하고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("IT", "기술", "뉴스", "인터넷"),
                expectedPath = listOf("root", "naver_main", "naver_news", "news_it"),
                description = "IT 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "기술 뉴스 보여줘", "인터넷 소식 알려줘"
                )
            ),
            
            // ========== 네이버 스포츠 관련 명령어 ==========
            VoiceCommand(
                id = "sports_baseball",
                command = "야구 뉴스 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("야구", "스포츠", "KBO", "메이저리그"),
                expectedPath = listOf("root", "naver_main", "naver_sports", "sports_baseball"),
                description = "야구 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "야구 소식 알려줘", "KBO 뉴스 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "sports_soccer",
                command = "축구 뉴스 확인할게",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("축구", "스포츠", "K리그", "월드컵"),
                expectedPath = listOf("root", "naver_main", "naver_sports", "sports_soccer"),
                description = "축구 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "축구 소식 보여줘", "K리그 뉴스 찾아줘"
                )
            ),
            
            // ========== 네이버 쇼핑 관련 명령어 ==========
            VoiceCommand(
                id = "shopping_beauty",
                command = "화장품 쇼핑하고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("화장품", "뷰티", "쇼핑", "스킨케어"),
                expectedPath = listOf("root", "naver_main", "naver_shopping", "shopping_beauty"),
                description = "뷰티 상품으로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "뷰티 제품 찾아줘", "스킨케어 쇼핑하고 싶어"
                )
            ),
            
            VoiceCommand(
                id = "shopping_fashion",
                command = "옷 쇼핑하러 가자",
                type = VoiceCommandType.NAVIGATION,
                targetKeywords = setOf("옷", "패션", "쇼핑", "의류"),
                expectedPath = listOf("root", "naver_main", "naver_shopping", "shopping_fashion"),
                description = "패션 상품으로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "의류 쇼핑하고 싶어", "패션 아이템 찾아줘"
                )
            ),
            
            // ========== 네이버 블로그 관련 명령어 ==========
            VoiceCommand(
                id = "blog_recipe",
                command = "요리 레시피 찾아줘",
                type = VoiceCommandType.SEARCH,
                targetKeywords = setOf("요리", "레시피", "음식", "블로그"),
                expectedPath = listOf("root", "naver_main", "naver_blog", "blog_recipe"),
                description = "요리 블로그로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "음식 만드는 법 알려줘", "요리법 검색해줘"
                )
            ),
            
            VoiceCommand(
                id = "blog_travel",
                command = "여행 정보 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("여행", "관광", "맛집", "블로그"),
                expectedPath = listOf("root", "naver_main", "naver_blog", "blog_travel"),
                description = "여행 블로그로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "여행지 추천해줘", "관광 정보 찾아줘"
                )
            ),
            
            // ========== 네이버 TV 관련 명령어 ==========
            VoiceCommand(
                id = "tv_drama",
                command = "드라마 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("드라마", "한드", "로맨스", "TV"),
                expectedPath = listOf("root", "naver_main", "naver_tv", "tv_drama"),
                description = "드라마 콘텐츠로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "한국 드라마 찾아줘", "로맨스 드라마 보여줘"
                )
            ),
            
            VoiceCommand(
                id = "tv_variety",
                command = "예능 프로그램 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("예능", "버라이어티", "웃긴", "TV"),
                expectedPath = listOf("root", "naver_main", "naver_tv", "tv_variety"),
                description = "예능 프로그램으로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "웃긴 프로그램 찾아줘", "버라이어티 쇼 보여줘"
                )
            ),
            
            // ========== 유튜브 음악 확장 명령어 ==========
            VoiceCommand(
                id = "music_kpop",
                command = "케이팝 듣고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("케이팝", "k-pop", "아이돌", "한국음악"),
                expectedPath = listOf("root", "youtube", "youtube_music", "music_kpop"),
                description = "K-POP 플레이리스트로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "아이돌 노래 틀어줘", "한국 가요 들려줘"
                )
            ),
            
            VoiceCommand(
                id = "music_ballad",
                command = "발라드 들려줘",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("발라드", "슬픈노래", "감성", "조용한음악"),
                expectedPath = listOf("root", "youtube", "youtube_music", "music_ballad"),
                description = "발라드 플레이리스트로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "슬픈 노래 틀어줘", "감성적인 음악 듣고 싶어"
                )
            ),
            
            VoiceCommand(
                id = "music_trot",
                command = "트로트 음악 듣고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("트로트", "트럿", "국악", "음악"),
                expectedPath = listOf("root", "youtube", "youtube_music", "music_trot"),
                description = "트로트 플레이리스트로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "트로트 노래 틀어줘", "국악 듣고 싶어"
                )
            ),
            
            // ========== 전자제품 쇼핑 명령어 ==========
            VoiceCommand(
                id = "phone_samsung",
                command = "갤럭시 폰 사고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("갤럭시", "삼성", "스마트폰", "핸드폰"),
                expectedPath = listOf("root", "coupang", "coupang_electronics", "phone_samsung"),
                description = "삼성 스마트폰으로 이동",
                difficulty = CommandDifficulty.HARD,
                alternativeCommands = listOf(
                    "삼성 휴대폰 찾아줘", "갤럭시 쇼핑하고 싶어"
                )
            ),
            
            VoiceCommand(
                id = "phone_apple",
                command = "아이폰 사고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("아이폰", "애플", "iPhone", "스마트폰"),
                expectedPath = listOf("root", "coupang", "coupang_electronics", "phone_apple"),
                description = "애플 아이폰으로 이동",
                difficulty = CommandDifficulty.HARD,
                alternativeCommands = listOf(
                    "애플 휴대폰 찾아줘", "iPhone 쇼핑하고 싶어"
                )
            ),
            
            // ========== 자연스러운 대화형 명령어 ==========
            VoiceCommand(
                id = "casual_news",
                command = "오늘 무슨 일 있었나?",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("오늘", "무슨일", "뉴스", "소식"),
                expectedPath = listOf("root", "naver_main", "naver_news"),
                description = "네이버 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "오늘 뉴스 알려줘", "무슨 소식 있어?"
                )
            ),
            
            VoiceCommand(
                id = "casual_entertainment",
                command = "심심한데 재밌는 거 없을까?",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("심심한데", "재밌는", "boring", "entertainment"),
                expectedPath = listOf("root", "naver_main", "naver_webtoon", "webtoon_popular"),
                description = "재미있는 콘텐츠 추천 (웹툰)",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "뭔가 볼 거 없을까?", "재미있는 거 추천해줘"
                )
            ),
            
            VoiceCommand(
                id = "casual_music",
                command = "음악이나 들을까",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("음악이나", "들을까", "music", "노래"),
                expectedPath = listOf("root", "youtube", "youtube_music"),
                description = "유튜브 뮤직으로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "노래 듣고 싶어", "음악 틀어줘"
                )
            ),
            
            VoiceCommand(
                id = "weather_check",
                command = "오늘 날씨 어때?",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("날씨", "어때", "오늘", "기온"),
                expectedPath = listOf("root", "naver_main", "naver_weather"),
                description = "네이버 날씨로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "날씨 확인해줘", "기온 알려줘"
                )
            ),
            
            // ========== 도움 요청 ==========
            VoiceCommand(
                id = "help_general",
                command = "뭐 할 수 있어?",
                type = VoiceCommandType.HELP,
                targetKeywords = setOf("도움", "help", "뭐", "할수있어", "기능"),
                expectedPath = listOf("root"),
                description = "도움말 표시",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "도움말 보여줘", "사용법 알려줘", "어떤 기능이 있어?"
                )
            )
        )
    }
    
    /**
     * 확장된 키워드 기반 명령어 매칭
     */
    fun findMatchingExpandedCommand(userInput: String): VoiceCommand? {
        val commands = getExpandedCommandSet()
        val inputLower = userInput.lowercase()
        
        // 정확한 명령어 매칭 우선
        commands.forEach { command ->
            if (command.command.contains(inputLower) || inputLower.contains(command.command.lowercase())) {
                return command
            }
            
            // 대체 명령어 매칭
            command.alternativeCommands.forEach { alternative ->
                if (alternative.lowercase().contains(inputLower) || inputLower.contains(alternative.lowercase())) {
                    return command
                }
            }
        }
        
        // 키워드 기반 매칭
        commands.forEach { command ->
            val matchedKeywords = command.targetKeywords.count { keyword ->
                inputLower.contains(keyword.lowercase())
            }
            
            // 키워드 매칭 개선
            if (matchedKeywords >= 1 && command.targetKeywords.size <= 2) {
                return command
            }
            if (matchedKeywords >= 2 && command.targetKeywords.size > 2) {
                return command
            }
        }
        
        return null
    }
}