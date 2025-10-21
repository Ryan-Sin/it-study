# atomic - AtomicLong (Lock-Free)

## 개요
`java.util.concurrent.atomic.AtomicLong`을 사용하여 **Lock-Free** 방식으로 동기화를 구현합니다.

## 핵심 개념

### Atomic 클래스
- 락을 사용하지 않고 동기화 달성 (Lock-Free)
- **CAS(Compare-And-Swap)** 알고리즘 사용
- CPU의 원자적 명령어 활용

### CAS (Compare-And-Swap)

```java
// 의사 코드
boolean compareAndSet(expected, newValue) {
    if (current == expected) {
        current = newValue;
        return true;
    }
    return false;
}
```

실제 동작:
1. 현재 값을 읽음
2. 새로운 값을 계산
3. 현재 값이 예상과 같으면 새 값으로 업데이트
4. 실패하면 재시도 (낙관적 락 패턴)

## 코드 예시

### 증가 연산
```java
private final AtomicLong quantity = new AtomicLong(0);

public void increment(long value) {
    quantity.addAndGet(value);  // 원자적 증가
}
```

### 감소 연산 (재시도 패턴)
```java
public boolean decrement(long value) {
    while (true) {
        long current = quantity.get();
        if (current < value) {
            return false;  // 재고 부족
        }
        long next = current - value;
        if (quantity.compareAndSet(current, next)) {
            return true;  // 성공
        }
        // CAS 실패 시 재시도
    }
}
```

## Lock vs Lock-Free

| 특성 | Lock (synchronized, ReentrantLock) | Lock-Free (Atomic) |
|------|-----------------------------------|-------------------|
| 대기 방식 | Blocking (스레드 대기) | Spinning (계속 시도) |
| 컨텍스트 스위칭 | 발생 (BLOCKED 상태) | 없음 (RUNNABLE 상태) |
| 성능 (경합 낮음) | 빠름 | 매우 빠름 |
| 성능 (경합 높음) | 보통 | CPU 낭비 가능 |
| Dead Lock | 가능 | 불가능 |
| 우선순위 역전 | 가능 | 불가능 |
| 적용 범위 | 복잡한 로직 가능 | 단순 연산만 가능 |

## 주요 메서드

### AtomicLong
```java
long get()                        // 현재 값 조회
void set(long newValue)           // 값 설정
long getAndSet(long newValue)     // 값 설정 후 이전 값 반환

long incrementAndGet()            // ++i
long getAndIncrement()            // i++
long decrementAndGet()            // --i
long getAndDecrement()            // i--

long addAndGet(long delta)        // i += delta, return i
long getAndAdd(long delta)        // temp = i, i += delta, return temp

boolean compareAndSet(expect, update)  // CAS 연산
```

## 언제 사용하는가?

### Atomic 사용 (추천)
- 단일 변수의 단순 연산 (증가, 감소, 설정)
- 경합이 낮거나 중간 수준
- Dead Lock 방지가 중요한 경우
- 응답 시간이 중요한 경우

### Lock 사용 (추천)
- 여러 변수를 함께 보호해야 하는 경우
- 복잡한 비즈니스 로직
- 경합이 매우 심한 경우 (Spinning 비효율적)
- 공정성이 필요한 경우

## Atomic 클래스 종류

```java
AtomicInteger      // int
AtomicLong         // long
AtomicBoolean      // boolean
AtomicReference<V> // 객체 참조

// 배열
AtomicIntegerArray
AtomicLongArray
AtomicReferenceArray<E>

// 필드 업데이터 (리플렉션 기반)
AtomicIntegerFieldUpdater
AtomicLongFieldUpdater
AtomicReferenceFieldUpdater
```

## 성능 비교

```
단순 증가 연산 (1000 스레드, 각 10000번 증가)

synchronized:     ~250ms
ReentrantLock:    ~240ms
AtomicLong:       ~150ms  ← 가장 빠름

복잡한 로직 (if-else 분기, 여러 변수)

synchronized:     ~300ms  ← 적합
ReentrantLock:    ~290ms
AtomicLong:       구현 불가능 또는 매우 복잡
```

## 실행 방법

```bash
./gradlew test --tests org.example.atomic.ProductConcurrencyTest
```

## 예상 결과

```
[AtomicLong ✅] 예상: 1000, 실제: 1000
```

## ABA 문제

CAS의 잠재적 문제점:

```
Thread 1: read A → (sleep) → CAS(A, B)
Thread 2: A → B → A (다시 A로 변경)
Thread 1: CAS 성공 (하지만 중간에 변경됨을 모름)
```

해결책: `AtomicStampedReference` (버전 번호 포함)

## 이전 단계
← [explicitlock](../explicitlock/README.md): ReentrantLock
← [synchronization](../synchronization/README.md): synchronized 키워드
← [basic](../basic/README.md): 동시성 문제 재현
