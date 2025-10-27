# Search Paths Summary

## daum.net
### 1. 다음 페이지로 이동

- Path ID: `d2277018ac11481a8db86cd3911528fa`
- Steps: 1 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://www.daum.net/
  - Selectors: body

## docs.oracle.com
### 1. 자바 공식문서 String 내용 보기

- Path ID: `ae03ee98f9716821e8e2eaf5c0065ba9`
- Steps: 6 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://docs.oracle.com/en/
  - Selectors: body
- [1] navigate | 페이지 이동
  - URL: https://docs.oracle.com/
  - Selectors: body
- [2] click | Oracle Help Center 클릭
  - URL: https://docs.oracle.com/en/
  - Selectors: #search-bar-input, .background-white.black-placeholder
- [3] input | 검색어 입력
  - URL: https://docs.oracle.com/en/
  - Selectors: #search-bar-input, .background-white.black-placeholder
- [4] input | 검색어 입력
  - URL: https://docs.oracle.com/en/
  - Selectors: #search-bar-input, .background-white.black-placeholder
- [5] navigate | 페이지 이동
  - URL: https://docs.oracle.com/search/?q=String
  - Selectors: body

### 2. 자바 공식문서에서 Integer 보기

- Path ID: `1d5e4d36c88736723a6b822f8283271e`
- Steps: 6 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://docs.oracle.com/en/
  - Selectors: body
- [1] click | Oracle Help Center 클릭
  - URL: https://docs.oracle.com/en/
  - Selectors: #search-bar-input, .background-white.black-placeholder
- [2] input | 검색어 입력
  - URL: https://docs.oracle.com/en/
  - Selectors: #search-bar-input, .background-white.black-placeholder
- [3] input | 검색어 입력
  - URL: https://docs.oracle.com/en/
  - Selectors: #search-bar-input, .background-white.black-placeholder
- [4] navigate | 페이지 이동
  - URL: https://docs.oracle.com/search/?q=Integer
  - Selectors: body
- [5] navigate | 페이지 이동
  - URL: https://docs.oracle.com/cd/G11131_01/rules_palette/Content/Transactions/Integer_field.htm
  - Selectors: body

## etk.srail.kr
### 1. SRT 승차권 예약하기

- Path ID: `d1f081c83dd506dff5081fcacfa93ec6`
- Steps: 12 | Weight: 37

Steps:
- [0] click | 메인 화면에서 '로그인' 링크 클릭
  - URL: https://etk.srail.kr/
  - Selectors: a:has-text('로그인')
- [1] click | 로그인 유형 중 '휴대전화번호' 라디오 버튼 선택
  - URL: https://etk.srail.kr/cmc/01/selectLoginForm.do?pageId=CTE0001
  - Selectors: #srchDvCd3
- [2] input | 로그인 정보 '휴대전화번호' 입력
  - URL: https://etk.srail.kr/cmc/01/selectLoginForm.do?pageId=CTE0001
  - Selectors: #srchDvNm03
- [3] input | 로그인 정보 '비밀번호' 입력
  - URL: https://etk.srail.kr/cmc/01/selectLoginForm.do?pageId=CTE0001
  - Selectors: #hmpgPwdCphd03
- [4] click | 정보 입력 후 '확인' 버튼 클릭하여 로그인
  - URL: https://etk.srail.kr/cmc/01/selectLoginForm.do?pageId=CTE0001
  - Selectors: input[value='확인'].loginSubmit:not([disabled])
- [5] navigate | 승차권 조회 페이지로 이동
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: -
- [6] input | 조회 정보 '출발역' 입력
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: #dptRsStnCdNm
- [7] input | 조회 정보 '도착역' 입력
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: #arvRsStnCdNm
- [8] select | 조회 정보 '출발일' 선택
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: #dptDt
- [9] select | 조회 정보 '출발시각' 선택
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: #dptTm
- [10] click | 조회 조건 입력 후 '조회하기' 버튼 클릭
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: input[value='조회하기']
- [11] click | 조회된 열차 목록에서 '예약하기' 버튼 클릭
  - URL: https://etk.srail.kr/hpg/hra/01/selectScheduleList.do
  - Selectors: #result-form > fieldset > div.tbl_wrap > table > tbody > tr > td:nth-child(7) > a.btn_burgundy_dark:has-text('예약하기'):first-of-type, #result-form > fieldset > div.tbl_wrap > table > tbody > tr > td:nth-child(6) > a.btn_burgundy_dark:has-text('예약하기'):first-of-type

## example.com
### 1. 경로 테스트

- Path ID: `52dab638cd7d18b7b6bf0ba02fdd5171`
- Steps: 1 | Weight: 1

Steps:
- [0] click | 테스트 버튼
  - URL: https://example.com/page1
  - Selectors: button#test, button.test-btn

## google.com
### 1. 기여보미

- Path ID: `7bf13ac98a722c84ecc83341e234829e`
- Steps: 1 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://www.google.com/?zx=1761448912237&no_sw_cr=1
  - Selectors: body

## gov.kr
### 1. 정부24 주민등록등본 발급

- Path ID: `961244506fceb6a125a19ac73975d129`
- Steps: 14 | Weight: 45

Steps:
- [0] click | 메인 화면에서 '주민등록등본(초본)' 바로가기 클릭
  - URL: https://www.gov.kr/
  - Selectors: a[title='주민등록등본(초본)'], a:has-text('주민등록등본(초본)'), a[href*='AA020InfoCappView'][href*='CappBizCD=13100000015']
- [1] click | 서비스 상세 정보 확인 후 '발급하기' 버튼 클릭
  - URL: https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=13100000015&HighCtgCD=A01010001&tp_seq=01&Mcode=10200
  - Selectors: #applyBtn, button:has-text('발급하기'), a:has-text('발급하기')
- [2] click | 로그인 안내 모달에서 '회원 신청하기' 버튼 클릭
  - URL: https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=13100000015&HighCtgCD=A01010001&tp_seq=01&Mcode=10200
  - Selectors: #memberApplyBtn, a:has-text('회원 신청하기'), a[href*='AA040OfferMainFrm'][href*='capp_biz_cd=13100000015']
- [3] click | 로그인 방식 중 '간편인증' 선택
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: button.login-type:has-text('간편인증'), button:has-text('간편인증')
- [4] click | 간편인증 방식 중 '카카오톡' 선택
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: label:has(p:has-text('카카오톡')), p:has-text('카카오톡'), img[alt*='카카오' i]
- [5] input | 간편인증 정보 '이름' 입력
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: #oacx_name
- [6] input | 간편인증 정보 '생년월일' 입력
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: #oacx_birth
- [7] input | 간편인증 정보 '핸드폰 뒷 번호' 입력
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: #oacx_phone2
- [8] click | 정보 입력 후 '전체동의' 체크 버튼 클릭
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: #totalAgree
- [9] click | '인증 요청' 버튼 클릭
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: #oacx-request-btn-pc
- [10] wait | 사용자 카카오톡 인증 완료 대기
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: -
- [11] click | 카카오톡 인증 후 '인증 완료' 버튼 클릭
  - URL: https://www.gov.kr/mw/AA040OfferMainFrm.do?capp_biz_cd=13100000015
  - Selectors: button:has-text('인증 완료')
- [12] click | 최종 신청 정보 확인 후 '신청하기' 버튼 클릭
  - URL: https://www.gov.kr/mw/AA020InfoCappViewApp.do
  - Selectors: button#btn_end:has-text('신청하기'), button:has-text('신청하기'), .btn_L#btn_end, #btn_end
- [13] click | 처리 완료된 내역의 '문서출력' 버튼 클릭
  - URL: https://www.gov.kr/mw/AA020InfoCappViewApp.do
  - Selectors: button:has-text('문서출력')

### 2. 정부24 장애인 등록

- Path ID: `8eca38def968b8d515815b103aebc504`
- Steps: 5 | Weight: 1

Steps:
- [0] type | 통합검색
  - URL: https://www.gov.kr
  - Selectors: .main_search input[name='searchKeyword'], #main_search_input, .search_input
- [1] click | 장애인 등록 신청
  - URL: https://www.gov.kr/search?query=장애인등록
  - Selectors: .search_result .service_item[data-service='disability_registration'] a, a:contains('장애인 등록 신청'), .disability_registration_link
- [2] click | 온라인 신청
  - URL: https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=14600000117
  - Selectors: .service_info .btn_apply_online, button:contains('온라인 신청'), .online_apply
- [3] click | 공동인증서 로그인
  - URL: https://www.gov.kr/portal/ntnadmSrvc/main
  - Selectors: .login_area .btn_login[data-type='public_cert'], button:contains('공동인증서'), .cert_login
- [4] click | 다음단계
  - URL: https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=14600000117&loginType=cert
  - Selectors: .application_form .btn_next, button:contains('다음단계'), .next_step

## kawid.or.kr
### 1. 한국장애인복지시설협회 장애인 시설 검색

- Path ID: `4b0fab722c3e372bc5fbcd7a7fa46c91`
- Steps: 6 | Weight: 1

Steps:
- [0] click | 회원시설안내
  - URL: https://www.kawid.or.kr
  - Selectors: .gnb_menu_list a[href*='member'], a:contains('회원시설안내'), .facility_menu
- [1] click | 회원시설검색
  - URL: https://www.kawid.or.kr/sub/member/member01.php
  - Selectors: #my-button, button:contains('회원시설검색'), .facility_search_btn
- [2] select | 지역선택
  - URL: https://www.kawid.or.kr/sub/member/member01.php#search_modal
  - Selectors: .modal-body select[name='region'], #region_select, .region_dropdown
- [3] select | 시설유형
  - URL: https://www.kawid.or.kr/sub/member/member01.php?region=seoul
  - Selectors: .modal-body select[name='facility_type'], #facility_type_select, .type_dropdown
- [4] click | 검색
  - URL: https://www.kawid.or.kr/sub/member/member01.php?region=seoul&type=welfare_center
  - Selectors: .modal-body .btn_search, button:contains('검색'), .search_submit
- [5] click | 상세보기
  - URL: https://www.kawid.or.kr/sub/member/search_result.php?region=seoul&type=welfare_center
  - Selectors: .facility_list .facility_item:first-child .detail_btn, .result_item:first .more_info, .facility_detail

## knat.go.kr
### 1. 중앙보조기기센터 보조기기 검색

- Path ID: `9fe8ab94200f5f6f1cda29be631e3f38`
- Steps: 6 | Weight: 1

Steps:
- [0] click | 보조기기 DB
  - URL: https://www.knat.go.kr/knw/
  - Selectors: .main_menu a[href*='knat_DB'], a:contains('보조기기 DB'), .db_menu
- [1] select | 카테고리 선택
  - URL: https://www.knat.go.kr/knw/home/knat_DB/main.html
  - Selectors: .search_category select[name='category'], #category_select, .category_dropdown
- [2] click | 손 기능
  - URL: https://www.knat.go.kr/knw/home/knat_DB/main.html?category=body_function
  - Selectors: .subcategory_area input[type='checkbox'][value='hand_function'], input[name='hand_function'], .hand_function_check
- [3] click | 검색
  - URL: https://www.knat.go.kr/knw/home/knat_DB/main.html?category=body_function&subcategory=hand_function
  - Selectors: .search_area .btn_search, button:contains('검색'), .search_submit
- [4] click | 첫 번째 보조기기
  - URL: https://www.knat.go.kr/knw/home/knat_DB/search_result.html?category=body_function&subcategory=hand_function
  - Selectors: .device_list .device_item:first-child .detail_link, .result_item:first a, .device_detail
- [5] click | 지원안내
  - URL: https://www.knat.go.kr/knw/home/knat_DB/device_detail.html?id=12345
  - Selectors: .device_detail .support_info .btn_support_guide, button:contains('지원안내'), .support_guide

## ko.wikipedia.org
### 1. 위키백과에서 오픈소스 검색하기

- Path ID: `8dd7e71230e232636371175f75406127`
- Steps: 8 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://ko.wikipedia.org/wiki/%EC%9C%84%ED%82%A4%EB%B0%B1%EA%B3%BC:%EB%8C%80%EB%AC%B8
  - Selectors: body
- [1] click | 위키백과, 우리 모두의 백과사전 클릭
  - URL: https://ko.wikipedia.org/wiki/%EC%9C%84%ED%82%A4%EB%B0%B1%EA%B3%BC:%EB%8C%80%EB%AC%B8
  - Selectors: #searchInput, .cdx-text-input__input.mw-searchInput
- [2] input | 검색어 입력
  - URL: https://ko.wikipedia.org/wiki/%EC%9C%84%ED%82%A4%EB%B0%B1%EA%B3%BC:%EB%8C%80%EB%AC%B8
  - Selectors: div > input.cdx-text-input__input, .cdx-text-input__input
- [3] input | 검색어 입력
  - URL: https://ko.wikipedia.org/wiki/%EC%9C%84%ED%82%A4%EB%B0%B1%EA%B3%BC:%EB%8C%80%EB%AC%B8
  - Selectors: div > input.cdx-text-input__input.cdx-text-input__input--has-value, .cdx-text-input__input.cdx-text-input__input--has-value
- [4] navigate | 페이지 이동
  - URL: https://ko.wikipedia.org/wiki/%EC%98%A4%ED%94%88_%EC%86%8C%EC%8A%A4
  - Selectors: body
- [5] navigate | 페이지 이동
  - URL: https://ko.wikipedia.org/wiki/%EC%98%A4%ED%94%88%EC%86%8C%EC%8A%A4
  - Selectors: body
- [6] navigate | 페이지 이동
  - URL: https://ko.wikipedia.org/wiki/%EC%98%A4%ED%94%88_%EC%86%8C%EC%8A%A4#%EC%98%A4%ED%94%88_%EC%86%8C%EC%8A%A4_%EB%9D%BC%EC%9D%B4%EC%84%A0%EC%8A%A4
  - Selectors: body
- [7] click | 오픈 소스 라이선스 클릭
  - URL: https://ko.wikipedia.org/wiki/%EC%98%A4%ED%94%88_%EC%86%8C%EC%8A%A4#%EC%98%A4%ED%94%88_%EC%86%8C%EC%8A%A4_%EB%9D%BC%EC%9D%B4%EC%84%A0%EC%8A%A4
  - Selectors: div > span

## namu.wiki
### 1. 나무위키 투명성 정보 보고서 들어가기

- Path ID: `f49eb37014896771fe05fffbe532fa50`
- Steps: 7 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%EB%8C%80%EB%AC%B8
  - Selectors: body
- [1] navigate | 페이지 이동
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%EA%B6%8C%EB%A6%AC%EC%B9%A8%ED%95%B4%20%EB%8F%84%EC%9B%80%EB%A7%90
  - Selectors: body
- [2] click | 권리침해 도움말 클릭
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%EA%B6%8C%EB%A6%AC%EC%B9%A8%ED%95%B4%20%EB%8F%84%EC%9B%80%EB%A7%90
  - Selectors: div > a.bJYj++4j, .bJYj++4j
- [3] navigate | 페이지 이동
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%ED%88%AC%EB%AA%85%EC%84%B1%20%EB%B3%B4%EA%B3%A0%EC%84%9C
  - Selectors: body
- [4] click | 나무위키:투명성 보고서 클릭
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%ED%88%AC%EB%AA%85%EC%84%B1%20%EB%B3%B4%EA%B3%A0%EC%84%9C
  - Selectors: div > a.bJYj++4j, .bJYj++4j
- [5] navigate | 페이지 이동
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%ED%88%AC%EB%AA%85%EC%84%B1%20%EB%B3%B4%EA%B3%A0%EC%84%9C/%EC%A0%80%EC%9E%91%EA%B6%8C
  - Selectors: body
- [6] click | 나무위키:투명성 보고서/저작권 클릭
  - URL: https://namu.wiki/w/%EB%82%98%EB%AC%B4%EC%9C%84%ED%82%A4:%ED%88%AC%EB%AA%85%EC%84%B1%20%EB%B3%B4%EA%B3%A0%EC%84%9C/%EC%A0%80%EC%9E%91%EA%B6%8C
  - Selectors: div > a.bJYj++4j, .bJYj++4j

### 2. 나무위키 123123 문서

- Path ID: `f1c4875d371385a50e044bd03c214fa7`
- Steps: 1 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://namu.wiki/w/123123
  - Selectors: body

## naver.com
### 1. 네이버 주식 에서 삼성 보기

- Path ID: `5f35163c9b291f6bc976ab015c8cb24c`
- Steps: 1 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://www.naver.com/
  - Selectors: body

### 2. 네이버 미세먼지 날씨

- Path ID: `545d364b01ba9471061ab8dc67408f74`
- Steps: 5 | Weight: 1

Steps:
- [0] click | 지역선택
  - URL: https://weather.naver.com
  - Selectors: .region_select .btn_region, .location_btn, .area_select
- [1] click | 부산
  - URL: https://weather.naver.com/region/list
  - Selectors: .region_list .region_item[data-region='busan'] a, a:contains('부산'), .busan_region
- [2] click | 미세먼지
  - URL: https://weather.naver.com/today/09440111
  - Selectors: .content_tabmenu .tab_item[data-tab='air'] a, a:contains('미세먼지'), .air_quality_tab
- [3] click | 주간
  - URL: https://weather.naver.com/air/09440111
  - Selectors: .air_chart_area .btn_chart_period[data-period='week'], button:contains('주간'), .weekly_chart
- [4] click | 지역비교
  - URL: https://weather.naver.com/air/09440111?period=week
  - Selectors: .compare_area .btn_compare, button:contains('지역비교'), .region_compare

### 3. 네이버 증권 주식 호가 검색

- Path ID: `2a0b5d6b66a245c2f55d88448f922bfa`
- Steps: 5 | Weight: 1

Steps:
- [0] type | 종목 검색
  - URL: https://finance.naver.com
  - Selectors: .stock_search #stock_items, .search_stock input, #stock_search
- [1] click | 일봉
  - URL: https://finance.naver.com/item/main.naver?code=005930
  - Selectors: .tab_con1 .subtab_chart li:nth-child(2) a, a:contains('일봉'), .chart_tab .daily
- [2] click | 호가
  - URL: https://finance.naver.com/item/main.naver?code=005930#chart_area
  - Selectors: .subtab_sise li:nth-child(2) a, a:contains('호가'), .bid_ask_tab
- [3] click | 기관
  - URL: https://finance.naver.com/item/sise.naver?code=005930
  - Selectors: .tab_con .subtab li:nth-child(3) a, a:contains('기관'), .institutional_tab
- [4] click | 날짜선택
  - URL: https://finance.naver.com/item/frgn.naver?code=005930
  - Selectors: .date_sise .cal_img, .date_picker, .calendar_btn

### 4. 네이버 강남역 인기 카페 검색

- Path ID: `48ef341b5d62edfe6d952fd7376c8cd4`
- Steps: 4 | Weight: 1

Steps:
- [0] click | 검색창
  - URL: https://map.naver.com/v5
  - Selectors: input.input_search, #search_input, .search_input_box input
- [1] click | 첫 번째 카페
  - URL: https://map.naver.com/v5/search/강남역%20카페
  - Selectors: .place_list .PlaceItem:first-child .place_bluelink, .search_item:first .place_name, .place_item:first a
- [2] click | 정보
  - URL: https://map.naver.com/v5/entry/place/1234567890
  - Selectors: .place_fixed_maintab .tab_menu a[data-tab='info'], a:contains('정보'), .info_tab
- [3] click | 길찾기
  - URL: https://map.naver.com/v5/entry/place/1234567890?tab=info
  - Selectors: .place_section_content .btn_direction, .direction_btn, button:contains('길찾기')

### 5. 네이버 로맨스 웹툰 추천

- Path ID: `05fcb6a1b779efe00368e45de37f4ba7`
- Steps: 4 | Weight: 1

Steps:
- [0] click | 로맨스
  - URL: https://comic.naver.com/index
  - Selectors: a[href*='genre=romance'], a:contains('로맨스'), .genre_romance
- [1] click | 별점순
  - URL: https://comic.naver.com/webtoon/weekday?genre=romance
  - Selectors: .sorting_area .btn_sort[data-sort='rating'], a:contains('별점순'), .sort_rating
- [2] click | 첫 번째 웹툰
  - URL: https://comic.naver.com/webtoon/weekday?genre=romance&sort=rating
  - Selectors: .ContentList .item:first-child .thumb_area a, .webtoon_item:first a, .thumb_link
- [3] click | 최신 에피소드
  - URL: https://comic.naver.com/webtoon/list?titleId=12345
  - Selectors: .EpisodeListInfo__episode_area .EpisodeListInfo__episode:first-child a, .episode_item:first a, .latest_episode a

### 6. 네이버 중국어 사전 검색

- Path ID: `72158d9a296f865a960bf0a7012ddd16`
- Steps: 4 | Weight: 1

Steps:
- [0] type | 검색창
  - URL: https://dict.naver.com
  - Selectors: #ac_input, .input_search, .search_input
- [1] click | 첫 번째 검색 결과
  - URL: https://dict.naver.com/search.dict?dicQuery=beautiful&query=beautiful&target=dic&ie=utf8&query_utf=&isOnlyViewEE=
  - Selectors: .dic_search_result .mean_list:first-child, .search_result:first, .dic_result:first
- [2] click | 중국어
  - URL: https://dict.naver.com/search.dict?dicQuery=beautiful&query=beautiful&target=dic&ie=utf8
  - Selectors: .language_select .btn_select[data-lang='ch'], button:contains('중국어'), .lang_ch
- [3] click | 발음듣기
  - URL: https://dict.naver.com/search.dict?dicQuery=beautiful&query=beautiful&target=dic&ie=utf8&lang=ch
  - Selectors: .pronunciation .btn_listen, .sound_btn, button:contains('발음듣기')

### 7. 치지직에서 인기클립 가기

- Path ID: `11fb0feb301471791bf347b63de5428b`
- Steps: 4 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://www.naver.com/
  - Selectors: body
- [1] new_tab | 새 탭 열기
  - URL: https://chzzk.naver.com/?tracking_code=home_recommend
  - Selectors: body
- [2] click | NAVER 클릭
  - URL: https://www.naver.com/
  - Selectors: a > span.service_icon, .service_icon
- [3] click | 인기 클립 클릭
  - URL: https://chzzk.naver.com/clips
  - Selectors: li > a.navigation_bar_menu__ud4mU, .navigation_bar_menu__ud4mU

### 8. 네이버 정치 뉴스

- Path ID: `1321e9696d1dc1d339dac115d1dcd807`
- Steps: 2 | Weight: 1

Steps:
- [0] click | 뉴스
  - URL: https://news.naver.com/main/main.naver
  - Selectors: a[href*='sid1=100'], a:contains('정치'), .category_item[data-cid='100']
- [1] click | 첫 번째 정치 기사
  - URL: https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=100
  - Selectors: .list_body .type06_headline dt a, .headline_area a, .news_tit

### 9. 네이버 날씨

- Path ID: `0fad870b4d6322aed1d66b725a376427`
- Steps: 1 | Weight: 50

Steps:
- [0] click | 날씨
  - URL: https://www.naver.com
  - Selectors: a[href*='weather.naver.com'], a:contains('날씨'), .service_weather

### 10. 네이버 사전

- Path ID: `94a180448371c12476a79321842c114d`
- Steps: 1 | Weight: 1

Steps:
- [0] click | 사전
  - URL: https://www.naver.com
  - Selectors: a[href*='dict.naver.com'], a:contains('사전'), .service_dict

### 11. 네이버 웹툰

- Path ID: `cafd7672d3aa84e096dad98979664fb4`
- Steps: 1 | Weight: 1

Steps:
- [0] click | 웹툰
  - URL: https://www.naver.com
  - Selectors: a[href*='comic.naver.com'], a:contains('웹툰'), .service_item[data-clk='webtoon']

### 12. 네이버 증권

- Path ID: `c7a23b6c2aa79975316f239084755d57`
- Steps: 1 | Weight: 1

Steps:
- [0] click | 증권
  - URL: https://www.naver.com
  - Selectors: a[href*='finance.naver.com'], a:contains('증권'), .service_finance

### 13. 네이버 지도

- Path ID: `3678315953cd421d263c5290ffa1d040`
- Steps: 1 | Weight: 1

Steps:
- [0] click | 지도
  - URL: https://www.naver.com
  - Selectors: a[href*='map.naver.com'], a:contains('지도'), .service_map

## osscontest.kr
### 1. 오픈소스 개발자대회 정보마당 들어가기

- Path ID: `cb0ff47a450342bb92b82c3abafb0a85`
- Steps: 6 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://osscontest.kr/
  - Selectors: body
- [1] navigate | 페이지 이동
  - URL: https://osscontest.kr/
  - Selectors: body
- [2] click | 대회 소개 클릭
  - URL: https://osscontest.kr/
  - Selectors: div > div.css-1qcsd9z, .css-1qcsd9z
- [3] new_tab | 새 탭 열기
  - URL: https://www.oss.kr/dev_competition
  - Selectors: body
- [4] click | 공개SW 개발자대회 소개, 대회 운영 프로세스, 대회 운영 조직 클릭
  - URL: https://www.oss.kr/dev_competition
  - Selectors: #oss_search
- [5] input | 검색어 입력
  - URL: https://www.oss.kr/dev_competition
  - Selectors: #oss_search

## wikipedia.org
### 1. wikipedia 이동

- Path ID: `3909efe5d0038eb1727a6d5f9d63ee4b`
- Steps: 1 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://www.wikipedia.org/
  - Selectors: body

## yahoo.com
### 1. 야후에서 트럼프 검색하기

- Path ID: `41fb0cfddd3787bc695b8a6805faa4be`
- Steps: 8 | Weight: 1

Steps:
- [0] navigate | 페이지 이동
  - URL: https://www.yahoo.com/
  - Selectors: body
- [1] navigate | 페이지 이동
  - URL: https://finance.yahoo.com/
  - Selectors: body
- [2] click | Yahoo Finance - Stock Market Live, Quotes, Business & Finance News 클릭
  - URL: https://finance.yahoo.com/
  - Selectors: #ybar-sbq, ._yb_d7nywr._yb_owcm9n._yb_ikgg1k.finsrch-inpt.modules-module_inputTopRounded__fSiBN
- [3] input | 검색어 입력
  - URL: https://finance.yahoo.com/
  - Selectors: #ybar-sbq, ._yb_d7nywr._yb_ikgg1k.finsrch-inpt.modules-module_inputTopRounded__fSiBN
- [4] input | 검색어 입력
  - URL: https://finance.yahoo.com/
  - Selectors: #ybar-sbq, ._yb_d7nywr._yb_ikgg1k.finsrch-inpt.modules-module_inputTopRounded__fSiBN
- [5] click | Yahoo Finance - Stock Market Live, Quotes, Business & Finance News 클릭
  - URL: https://finance.yahoo.com/
  - Selectors: #ybar-search, .rapid-noclick-resp._yb_xm1rg8.finsrch-btn
- [6] input | 검색어 입력
  - URL: https://finance.yahoo.com/
  - Selectors: #ybar-sbq, ._yb_d7nywr._yb_ikgg1k.finsrch-inpt.modules-module_inputTopRounded__fSiBN
- [7] navigate | 페이지 이동
  - URL: https://finance.yahoo.com/quote/DJT/?err=1
  - Selectors: body
