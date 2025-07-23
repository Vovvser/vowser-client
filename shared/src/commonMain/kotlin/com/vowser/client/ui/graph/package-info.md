# Package: com.vowser.client.ui.graph

네트워크 그래프 시각화를 담당하는 UI 컴포넌트 패키지

## 주요 파일
- `ModernNetworkGraph.kt` - 메인 그래프 컴포넌트 (2025 트렌드 반영)
- `GraphCanvas.kt` - 커스텀 Canvas 기반 그래프 렌더링
- `GraphControls.kt` - 그래프 제어 UI (줌, 팬, 리셋)
- `GraphPhysics.kt` - 물리 기반 노드 배치 알고리즘
- `NetworkGraphComponent.kt` - 그래프 래퍼 컴포넌트
- `AnimatedNavigationGraph.kt` - 애니메이션 효과
- `NodeInfoPanel.kt` - 노드 상세 정보 패널
- `GraphHeader.kt` - 그래프 헤더 UI
- `GraphLegend.kt` - 범례 및 설명
- `GraphLoading.kt` - 로딩 상태 처리

## 주요 기능
- **인터랙티브 그래프**: 노드 클릭/롱클릭, 드래그 지원
- **글래스모피즘**: 반투명 유리 효과
- **펄스 애니메이션**: 활성 노드 강조
- **경로 하이라이팅**: 음성 명령 결과 시각화
- **멀티터치**: 확대/축소, 패닝 제스처