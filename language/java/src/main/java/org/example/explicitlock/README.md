# explicitlock - ReentrantLock

## 개요
`java.util.concurrent.locks.ReentrantLock`을 사용하여 명시적으로 락을 제어합니다.

## 핵심 개념

### ReentrantLock
- `synchronized`보다 더 유연한 락 메커니즘
- 명시적으로 `lock()`과 `unlock()` 호출
- 타임아웃, 인터럽트, 공정성 등 고급 기능 제공

### 코드 패턴

```java
private final Lock lock = new ReentrantLock();

public void increment(long quantity) {
    lock.lock();
    try {
        this.quantity += quantity;
    } finally {
        lock.unlock();  // 반드시 finally에서 해제!
    }
}
```

## synchronized vs ReentrantLock

| 특성 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 사용 편의성 | 간단 | 명시적 lock/unlock 필요 |
| 자동 락 해제 | O | X (finally 필수) |
| 타임아웃 | X | O (`tryLock(timeout)`) |
| 인터럽트 | X | O (`lockInterruptibly()`) |
| 공정성 | X | O (`new ReentrantLock(true)`) |
| 조건 변수 | X | O (`Condition`) |
| 성능 | 비슷 | 비슷 (경합 심할 때 약간 유리) |

## 주요 메서드

### 기본 락
```java
lock.lock();       // 락 획득 (대기)
lock.unlock();     // 락 해제
```

### 타임아웃
```java
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // 작업
    } finally {
        lock.unlock();
    }
} else {
    // 락 획득 실패 처리
}
```

### 인터럽트 가능
```java
try {
    lock.lockInterruptibly();
    try {
        // 작업
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 공정성
```java
// 대기 시간이 긴 스레드에게 우선권 부여
Lock lock = new ReentrantLock(true);
```

## 언제 사용하는가?

### synchronized 사용
- 간단한 동기화
- 메서드 전체 보호
- 자동 락 해제가 중요한 경우

### ReentrantLock 사용
- 타임아웃이 필요한 경우
- 인터럽트 처리가 필요한 경우
- 공정성이 중요한 경우
- 여러 조건 변수가 필요한 경우
- 락 상태 확인이 필요한 경우 (`isLocked()`, `getQueueLength()` 등)

## 실행 방법

```bash
./gradlew test --tests org.example.explicitlock.ProductConcurrencyTest
```

## 예상 결과

```
[ReentrantLock ✅] 예상: 1000, 실제: 1000
```

## 주의사항

### ❌ 잘못된 사용
```java
lock.lock();
this.quantity += quantity;
lock.unlock();  // 예외 발생 시 unlock 안 됨!
```

### ✅ 올바른 사용
```java
lock.lock();
try {
    this.quantity += quantity;
} finally {
    lock.unlock();  // 반드시 실행됨
}
```

## 이전/다음 단계
← [synchronization](../synchronization/README.md): synchronized 키워드
→ [atomic](../atomic/README.md): Lock-Free 방식의 동기화
