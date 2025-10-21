# synchronization - synchronized 키워드

## 개요
Java의 `synchronized` 키워드를 사용하여 메서드 레벨에서 동기화를 구현합니다.

## 핵심 개념

### synchronized 키워드
- **모니터 락(Monitor Lock)** 을 사용한 상호 배제(Mutual Exclusion)
- 한 번에 하나의 스레드만 메서드 실행 가능
- 객체의 intrinsic lock(고유 락) 사용

### 동작 원리

```java
public synchronized void increment(long quantity) {
    this.quantity += quantity;
}
```

컴파일 시 실제로는:
```java
public void increment(long quantity) {
    synchronized(this) {  // 객체의 모니터 락 획득
        this.quantity += quantity;
    }  // 자동으로 락 해제
}
```

## 장점
- 사용이 간단하고 직관적
- 자동으로 락 해제 (예외 발생 시에도)
- JVM이 최적화 가능

## 단점
- **메서드 전체**가 임계 영역이 됨 (불필요하게 넓을 수 있음)
- 락을 획득하지 못한 스레드는 **BLOCKED 상태**로 대기
- 타임아웃이나 인터럽트 불가능
- 공정성(fairness) 보장 안 됨

## 성능 특성

```
스레드 대기 상태: BLOCKED
락 해제 방식: 자동 (메서드 종료 시)
재진입: 가능 (Reentrant)
공정성: 없음
```

## 실행 방법

```bash
./gradlew test --tests org.example.synchronization.ProductConcurrencyTest
```

## 예상 결과

```
[synchronized ✅] 예상: 1000, 실제: 1000
```

## synchronized vs 블록 동기화

### 메서드 레벨 (현재 방식)
```java
public synchronized void increment(long quantity) {
    this.quantity += quantity;
}
```

### 블록 레벨 (더 세밀한 제어)
```java
public void increment(long quantity) {
    synchronized(this) {
        this.quantity += quantity;
    }
}
```

## 이전/다음 단계
← [basic](../basic/README.md): 동시성 문제 재현
→ [explicitlock](../explicitlock/README.md): ReentrantLock으로 더 세밀한 제어
