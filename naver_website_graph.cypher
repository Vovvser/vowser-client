// Neo4j Cypher 스크립트 - 네이버 웹사이트 구조 그래프 데이터
// 실제 시각 장애인 사용자의 네이버 이용 패턴을 반영한 데이터

// === 메인 노드들 ===
CREATE (naver:Website {id: "naver", name: "네이버 메인", url: "https://www.naver.com", description: "네이버 메인 페이지"})
CREATE (search:Feature {id: "naver_search", name: "검색창", url: "https://search.naver.com", description: "통합검색", accessKey: "Ctrl+L"})
CREATE (news:Category {id: "naver_news", name: "뉴스", url: "https://news.naver.com", description: "네이버 뉴스 섹션"})
CREATE (weather:Service {id: "naver_weather", name: "날씨", url: "https://weather.naver.com", description: "날씨 정보"})
CREATE (webtoon:Service {id: "naver_webtoon", name: "웹툰", url: "https://comic.naver.com", description: "네이버 웹툰"})
CREATE (blog:Service {id: "naver_blog", name: "블로그", url: "https://blog.naver.com", description: "네이버 블로그"})
CREATE (cafe:Service {id: "naver_cafe", name: "카페", url: "https://cafe.naver.com", description: "네이버 카페"})
CREATE (shopping:Service {id: "naver_shopping", name: "쇼핑", url: "https://shopping.naver.com", description: "네이버 쇼핑"})
CREATE (map:Service {id: "naver_map", name: "지도", url: "https://map.naver.com", description: "네이버 지도", accessKey: "Alt+M"})

// === 뉴스 세부 카테고리 ===
CREATE (news_politics:NewsCategory {id: "news_politics", name: "정치", url: "https://news.naver.com/section/100", description: "정치 뉴스"})
CREATE (news_economy:NewsCategory {id: "news_economy", name: "경제", url: "https://news.naver.com/section/101", description: "경제 뉴스"})
CREATE (news_society:NewsCategory {id: "news_society", name: "사회", url: "https://news.naver.com/section/102", description: "사회 뉴스"})
CREATE (news_world:NewsCategory {id: "news_world", name: "세계", url: "https://news.naver.com/section/104", description: "국제 뉴스"})
CREATE (news_sports:NewsCategory {id: "news_sports", name: "스포츠", url: "https://sports.news.naver.com", description: "스포츠 뉴스"})
CREATE (news_entertainment:NewsCategory {id: "news_entertainment", name: "연예", url: "https://entertain.naver.com", description: "연예 뉴스"})

// === 웹툰 세부 카테고리 ===
CREATE (webtoon_today:WebtoonCategory {id: "webtoon_today", name: "오늘의 웹툰", url: "https://comic.naver.com/webtoon", description: "요일별 연재 웹툰"})
CREATE (webtoon_complete:WebtoonCategory {id: "webtoon_complete", name: "완결 웹툰", url: "https://comic.naver.com/webtoon/finish", description: "완결된 웹툰"})
CREATE (webtoon_challenge:WebtoonCategory {id: "webtoon_challenge", name: "도전만화", url: "https://comic.naver.com/challenge", description: "도전만화"})
CREATE (webtoon_genre_romance:WebtoonGenre {id: "webtoon_romance", name: "로맨스", url: "https://comic.naver.com/webtoon/genre/romance", description: "로맨스 장르"})
CREATE (webtoon_genre_action:WebtoonGenre {id: "webtoon_action", name: "액션", url: "https://comic.naver.com/webtoon/genre/action", description: "액션 장르"})
CREATE (webtoon_genre_comedy:WebtoonGenre {id: "webtoon_comedy", name: "코미디", url: "https://comic.naver.com/webtoon/genre/comedy", description: "코미디 장르"})

// === 쇼핑 세부 카테고리 ===
CREATE (shopping_fashion:ShoppingCategory {id: "shopping_fashion", name: "패션의류", url: "https://shopping.naver.com/category/50000000", description: "의류/패션"})
CREATE (shopping_beauty:ShoppingCategory {id: "shopping_beauty", name: "화장품/미용", url: "https://shopping.naver.com/category/50000001", description: "뷰티"})
CREATE (shopping_digital:ShoppingCategory {id: "shopping_digital", name: "디지털/가전", url: "https://shopping.naver.com/category/50000002", description: "전자제품"})
CREATE (shopping_food:ShoppingCategory {id: "shopping_food", name: "식품", url: "https://shopping.naver.com/category/50000003", description: "식품/생필품"})
CREATE (shopping_living:ShoppingCategory {id: "shopping_living", name: "생활용품", url: "https://shopping.naver.com/category/50000004", description: "생활/건강"})

// === 검색 관련 노드 ===
CREATE (search_image:SearchType {id: "search_image", name: "이미지 검색", url: "https://search.naver.com/search.naver?where=image", description: "이미지 검색"})
CREATE (search_video:SearchType {id: "search_video", name: "동영상 검색", url: "https://search.naver.com/search.naver?where=nexearch", description: "동영상 검색"})
CREATE (search_blog:SearchType {id: "search_blog", name: "블로그 검색", url: "https://search.naver.com/search.naver?where=blog", description: "블로그 검색"})
CREATE (search_cafe:SearchType {id: "search_cafe", name: "카페 검색", url: "https://search.naver.com/search.naver?where=article", description: "카페 검색"})
CREATE (search_local:SearchType {id: "search_local", name: "지역 검색", url: "https://search.naver.com/search.naver?where=local", description: "지역정보 검색"})

// === 날씨 세부 정보 ===
CREATE (weather_current:WeatherInfo {id: "weather_current", name: "현재 날씨", description: "현재 기온 및 날씨 상태"})
CREATE (weather_forecast:WeatherInfo {id: "weather_forecast", name: "주간 예보", description: "일주일 날씨 예보"})
CREATE (weather_air:WeatherInfo {id: "weather_air", name: "미세먼지", description: "대기질 정보"})

// === 관계 정의 (RELATIONSHIPS) ===

// 메인 페이지에서 각 서비스로의 연결
CREATE (naver)-[:CONTAINS]->(search)
CREATE (naver)-[:CONTAINS]->(news)
CREATE (naver)-[:CONTAINS]->(weather)
CREATE (naver)-[:CONTAINS]->(webtoon)
CREATE (naver)-[:CONTAINS]->(blog)
CREATE (naver)-[:CONTAINS]->(cafe)
CREATE (naver)-[:CONTAINS]->(shopping)
CREATE (naver)-[:CONTAINS]->(map)

// 뉴스 카테고리 연결
CREATE (news)-[:HAS_CATEGORY]->(news_politics)
CREATE (news)-[:HAS_CATEGORY]->(news_economy)
CREATE (news)-[:HAS_CATEGORY]->(news_society)
CREATE (news)-[:HAS_CATEGORY]->(news_world)
CREATE (news)-[:HAS_CATEGORY]->(news_sports)
CREATE (news)-[:HAS_CATEGORY]->(news_entertainment)

// 웹툰 카테고리 연결
CREATE (webtoon)-[:HAS_SECTION]->(webtoon_today)
CREATE (webtoon)-[:HAS_SECTION]->(webtoon_complete)
CREATE (webtoon)-[:HAS_SECTION]->(webtoon_challenge)
CREATE (webtoon)-[:HAS_GENRE]->(webtoon_genre_romance)
CREATE (webtoon)-[:HAS_GENRE]->(webtoon_genre_action)
CREATE (webtoon)-[:HAS_GENRE]->(webtoon_genre_comedy)

// 쇼핑 카테고리 연결
CREATE (shopping)-[:HAS_CATEGORY]->(shopping_fashion)
CREATE (shopping)-[:HAS_CATEGORY]->(shopping_beauty)
CREATE (shopping)-[:HAS_CATEGORY]->(shopping_digital)
CREATE (shopping)-[:HAS_CATEGORY]->(shopping_food)
CREATE (shopping)-[:HAS_CATEGORY]->(shopping_living)

// 검색 타입 연결
CREATE (search)-[:SEARCH_TYPE]->(search_image)
CREATE (search)-[:SEARCH_TYPE]->(search_video)
CREATE (search)-[:SEARCH_TYPE]->(search_blog)
CREATE (search)-[:SEARCH_TYPE]->(search_cafe)
CREATE (search)-[:SEARCH_TYPE]->(search_local)

// 날씨 정보 연결
CREATE (weather)-[:PROVIDES]->(weather_current)
CREATE (weather)-[:PROVIDES]->(weather_forecast)
CREATE (weather)-[:PROVIDES]->(weather_air)

// 접근성 관련 연결 (시각 장애인 자주 사용하는 경로)
CREATE (search)-[:FREQUENT_PATH {usage_frequency: 95, avg_clicks: 1}]->(search_blog)
CREATE (search)-[:FREQUENT_PATH {usage_frequency: 85, avg_clicks: 1}]->(search_local)
CREATE (naver)-[:FREQUENT_PATH {usage_frequency: 90, avg_clicks: 2}]->(weather)
CREATE (naver)-[:FREQUENT_PATH {usage_frequency: 80, avg_clicks: 2}]->(news)
CREATE (news)-[:FREQUENT_PATH {usage_frequency: 70, avg_clicks: 3}]->(news_society)
CREATE (webtoon)-[:FREQUENT_PATH {usage_frequency: 65, avg_clicks: 3}]->(webtoon_today)

// 상호 참조 관계 (크로스 서비스)
CREATE (news_entertainment)-[:RELATES_TO]->(webtoon)
CREATE (shopping_food)-[:RELATES_TO]->(search_local)
CREATE (weather)-[:RELATES_TO]->(map)
CREATE (blog)-[:CROSS_REFERENCE]->(cafe)

// === 사용성 통계 메타데이터 ===
// 각 노드에 접근성 관련 속성 추가
SET naver.accessibility_score = 95, naver.avg_load_time = 1.2, naver.keyboard_shortcuts = "Tab, Enter"
SET search.accessibility_score = 98, search.avg_load_time = 0.8, search.keyboard_shortcuts = "Ctrl+L, Tab"
SET news.accessibility_score = 85, news.avg_load_time = 1.5, news.keyboard_shortcuts = "Tab, Arrow Keys"
SET weather.accessibility_score = 92, weather.avg_load_time = 1.1, weather.keyboard_shortcuts = "Tab, Enter"
SET webtoon.accessibility_score = 75, webtoon.avg_load_time = 2.0, webtoon.keyboard_shortcuts = "Tab, Space"
SET shopping.accessibility_score = 80, shopping.avg_load_time = 1.8, shopping.keyboard_shortcuts = "Tab, Enter, Arrow"
SET map.accessibility_score = 70, map.avg_load_time = 2.5, map.keyboard_shortcuts = "Alt+M, Arrow Keys"

// === 음성 명령 매핑 ===
SET search.voice_commands = ["검색", "찾기", "검색창으로"]
SET weather.voice_commands = ["날씨", "오늘 날씨", "날씨 보여줘"]
SET news.voice_commands = ["뉴스", "뉴스 보기", "최신 뉴스"]
SET webtoon.voice_commands = ["웹툰", "만화", "웹툰 추천", "만화 추천"]
SET shopping.voice_commands = ["쇼핑", "구매", "상품 찾기"]
SET map.voice_commands = ["지도", "길찾기", "위치 검색"]
SET blog.voice_commands = ["블로그", "블로그 검색"]
SET cafe.voice_commands = ["카페", "커뮤니티"]

RETURN "네이버 웹사이트 그래프 데이터 생성 완료" as result;