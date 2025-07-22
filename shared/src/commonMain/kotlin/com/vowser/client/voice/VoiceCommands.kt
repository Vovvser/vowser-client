package com.vowser.client.voice

/**
 * 장애인 친화적 음성 명령어 시스템
 * 실제 접근성이 필요한 사용자들이 사용할 법한 자연스러운 명령어 패턴
 */

/**
 * 음성 명령어 타입
 */
enum class VoiceCommandType {
    NAVIGATION,     // "~로 이동해줘"
    SEARCH,        // "~찾아줘"
    REQUEST,       // "~보고 싶어"
    ACTION,        // "~해줘"
    HELP          // "도움말", "어떻게 해?"
}

/**
 * 음성 명령어 데이터 클래스
 */
data class VoiceCommand(
    val id: String,
    val command: String,           // 실제 음성 명령어
    val type: VoiceCommandType,
    val targetKeywords: Set<String>, // 매칭할 키워드들
    val expectedPath: List<String>,  // 예상되는 노드 경로 ID들
    val description: String,
    val difficulty: CommandDifficulty = CommandDifficulty.MEDIUM,
    val alternativeCommands: List<String> = emptyList() // 대체 표현들
)

enum class CommandDifficulty {
    EASY,    // 1-2단계로 바로 접근 가능
    MEDIUM,  // 2-3단계 탐색 필요
    HARD     // 복잡한 탐색 경로 필요
}

/**
 * 장애인 친화적 음성 명령어 세트
 */
object AccessibilityVoiceCommands {
    
    /**
     * 실제 장애인 사용자들이 사용할 법한 자연스러운 명령어들
     * - 간단하고 명확한 표현
     * - 다양한 표현 방식 지원
     * - 실제 사용 시나리오 기반
     */
    fun getCommandSet(): List<VoiceCommand> {
        return listOf(
            
            // ========== 웹툰 관련 명령어 ==========
            VoiceCommand(
                id = "webtoon_popular",
                command = "인기 웹툰 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("인기", "웹툰", "보고싶다", "추천"),
                expectedPath = listOf("root", "naver", "naver_webtoon", "webtoon_popular"),
                description = "네이버 인기 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "웹툰 추천해줘",
                    "재밌는 웹툰 찾아줘",
                    "인기 만화 보여줘",
                    "웹툰 베스트 보고 싶어"
                )
            ),
            
            VoiceCommand(
                id = "webtoon_new",
                command = "새로운 웹툰 찾아줘",
                type = VoiceCommandType.SEARCH,
                targetKeywords = setOf("새로운", "신작", "웹툰", "최신"),
                expectedPath = listOf("root", "naver", "naver_webtoon", "webtoon_new"),
                description = "신작 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "신작 웹툰 보여줘",
                    "최신 웹툰 알려줘",
                    "새로 나온 만화 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "webtoon_general",
                command = "웹툰 보러 가자",
                type = VoiceCommandType.NAVIGATION,
                targetKeywords = setOf("웹툰", "만화", "코믹"),
                expectedPath = listOf("root", "naver", "naver_webtoon"),
                description = "네이버 웹툰으로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "웹툰 사이트로 가줘",
                    "만화 보러 가고 싶어",
                    "네이버 웹툰 열어줘"
                )
            ),
            
            // ========== 음악 관련 명령어 ==========
            VoiceCommand(
                id = "music_kpop",
                command = "케이팝 듣고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("케이팝", "k-pop", "아이돌", "한국음악"),
                expectedPath = listOf("root", "youtube", "youtube_music", "music_kpop"),
                description = "K-POP 플레이리스트로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "아이돌 노래 틀어줘",
                    "한국 가요 들려줘",
                    "K팝 음악 찾아줘"
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
                    "슬픈 노래 틀어줘",
                    "감성적인 음악 듣고 싶어",
                    "조용한 노래 들려줘"
                )
            ),
            
            VoiceCommand(
                id = "music_general",
                command = "음악 듣고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("음악", "노래", "뮤직"),
                expectedPath = listOf("root", "youtube", "youtube_music"),
                description = "유튜브 뮤직으로 이동",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "노래 틀어줘",
                    "뮤직 앱 열어줘",
                    "음악 사이트로 가자"
                )
            ),
            
            // ========== 뉴스 관련 명령어 ==========
            VoiceCommand(
                id = "news_politics",
                command = "정치 뉴스 알려줘",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("정치", "뉴스", "국정", "정부"),
                expectedPath = listOf("root", "naver", "naver_news", "news_politics"),
                description = "정치 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "정치 소식 보여줘",
                    "국정 뉴스 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "news_sports",
                command = "스포츠 뉴스 보고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("스포츠", "운동", "축구", "야구"),
                expectedPath = listOf("root", "naver", "naver_news", "news_sports"),
                description = "스포츠 뉴스로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "축구 소식 알려줘",
                    "운동 뉴스 찾아줘",
                    "야구 결과 보여줘"
                )
            ),
            
            // ========== 쇼핑 관련 명령어 ==========
            VoiceCommand(
                id = "shopping_phone",
                command = "스마트폰 사고 싶어",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("스마트폰", "핸드폰", "폰", "갤럭시"),
                expectedPath = listOf("root", "coupang", "coupang_electronics", "phone_samsung"),
                description = "스마트폰 쇼핑으로 이동",
                difficulty = CommandDifficulty.HARD,
                alternativeCommands = listOf(
                    "휴대폰 보러 가자",
                    "핸드폰 쇼핑하고 싶어",
                    "갤럭시 찾아줘"
                )
            ),
            
            VoiceCommand(
                id = "shopping_general",
                command = "쇼핑하러 가자",
                type = VoiceCommandType.NAVIGATION,
                targetKeywords = setOf("쇼핑", "구매", "사고싶다"),
                expectedPath = listOf("root", "coupang"),
                description = "쿠팡 쇼핑몰로 이동",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "온라인 쇼핑 하고 싶어",
                    "뭔가 사고 싶어",
                    "쿠팡 열어줘"
                )
            ),
            
            // ========== 일반적인 도움 요청 ==========
            VoiceCommand(
                id = "help_general",
                command = "뭐 할 수 있어?",
                type = VoiceCommandType.HELP,
                targetKeywords = setOf("도움", "help", "뭐", "할수있어", "기능"),
                expectedPath = listOf("root"),
                description = "도움말 표시",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "도움말 보여줘",
                    "사용법 알려줘",
                    "어떤 기능이 있어?",
                    "뭐 할 수 있나요?"
                )
            ),
            
            VoiceCommand(
                id = "help_navigation",
                command = "어디로 갈 수 있어?",
                type = VoiceCommandType.HELP,
                targetKeywords = setOf("어디", "갈수있어", "이동", "사이트"),
                expectedPath = listOf("root"),
                description = "이용 가능한 사이트 안내",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "어떤 사이트 갈 수 있어?",
                    "뭐 볼 수 있나요?"
                )
            ),
            
            // ========== 자연스러운 대화형 명령어 ==========
            VoiceCommand(
                id = "casual_bored",
                command = "심심해, 뭔가 재밌는 거 없을까?",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("심심해", "재밌는", "boring", "entertainment"),
                expectedPath = listOf("root", "naver", "naver_webtoon", "webtoon_popular"),
                description = "재미있는 콘텐츠 추천 (웹툰)",
                difficulty = CommandDifficulty.EASY,
                alternativeCommands = listOf(
                    "재미있는 거 추천해줘",
                    "뭔가 볼 거 없을까?",
                    "심심한데 도와줘"
                )
            ),
            
            VoiceCommand(
                id = "casual_relax",
                command = "힘들었던 하루, 음악이나 들을까",
                type = VoiceCommandType.REQUEST,
                targetKeywords = setOf("힘들었던", "하루", "음악", "relax"),
                expectedPath = listOf("root", "youtube", "youtube_music", "music_ballad"),
                description = "휴식용 음악 추천 (발라드)",
                difficulty = CommandDifficulty.MEDIUM,
                alternativeCommands = listOf(
                    "마음 편한 음악 들려줘",
                    "힐링 음악 찾아줘"
                )
            )
        )
    }
    
    /**
     * 키워드 기반 명령어 매칭
     */
    fun findMatchingCommand(userInput: String): VoiceCommand? {
        val commands = getCommandSet()
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
            
            // 키워드가 일정 비율 이상 매칭되면 반환
            if (matchedKeywords >= 1 && command.targetKeywords.size <= 3) {
                return command
            }
            if (matchedKeywords >= 2 && command.targetKeywords.size > 3) {
                return command
            }
        }
        
        return null
    }
    
    /**
     * 사용 가능한 명령어 목록 반환 (도움말용)
     */
    fun getCommandExamples(): List<String> {
        return getCommandSet().map { it.command }.take(10)
    }
}