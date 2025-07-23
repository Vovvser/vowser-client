# Package: com.vowser.client.visualization

그래프 시각화 엔진과 레이아웃 알고리즘을 담당하는 패키지

## 주요 파일
- `GraphVisualizationEngine.kt` - 시각화 엔진 인터페이스
  - D3.js와 Kotlin Compose 구현을 쉽게 교체할 수 있도록 추상화
  - 다양한 레이아웃 타입 지원 (원형, 계층형, 힘-지향, 트리)

## 레이아웃 타입
- `CIRCULAR` - 원형 배치 (현재 기본 구현)
- `HIERARCHICAL` - 계층형 배치 (depth 기반)
- `FORCE_DIRECTED` - 힘-지향 배치 (D3.js 스타일)
- `TREE` - 트리 구조 배치

## 데이터 구조
- `GraphVisualizationData` - 시각화용 노드/엣지 컨테이너
- `GraphNode` - UI 표시용 노드 (위치, 크기, 색상 포함)
- `GraphEdge` - UI 표시용 엣지 (시작점, 끝점, 스타일)

## 확장성
향후 D3.js, Three.js 등 다른 시각화 라이브러리로 쉽게 교체 가능한 설계