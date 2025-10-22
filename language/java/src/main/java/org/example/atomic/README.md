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
| OS 호출 | 필요 (경합 시) | 불필요 (User-Space) |
| 일관성 보장 | 100% (대기하지만 처리) | 100% (데이터 무결성) |
| 가용성 보장 | 100% (모든 요청 처리) | 불완전 (재시도 제한 시) |

---

## CPU/하드웨어 레벨 동작 원리 (Deep Dive)

### 1. CAS의 하드웨어 구현

#### x86/x64: CMPXCHG 명령어
```assembly
; AtomicLong.compareAndSet(expected, newValue) 내부 동작

; pseudo-assembly (실제는 더 복잡)
CMPXCHG [memory_address], newValue
  ; 1. RAX 레지스터에 expected 값 로드
  ; 2. [memory_address]와 RAX 비교
  ; 3. 같으면: [memory_address] = newValue, ZF=1
  ; 4. 다르면: RAX = [memory_address], ZF=0
  ; 5. LOCK prefix: 다른 CPU 코어가 메모리 수정 불가

; 실제 JVM Native 코드
inline jint Atomic::cmpxchg(jint exchange_value,
                             volatile jint* dest,
                             jint compare_value) {
  __asm {
    mov edx, dest
    mov ecx, exchange_value
    mov eax, compare_value
    LOCK CMPXCHG dword ptr [edx], ecx  // ← 원자적 실행
  }
}
```

#### ARM: LDREX/STREX (Load-Linked/Store-Conditional)
```assembly
; ARM의 CAS 구현
retry:
  LDREX  r1, [r0]          ; Exclusive Load (메모리 주소 r0의 값을 r1로)
  CMP    r1, r2            ; 현재 값과 expected 비교
  BNE    fail              ; 다르면 실패
  STREX  r3, r4, [r0]      ; Exclusive Store (r4를 r0에 저장, 성공 여부는 r3)
  CMP    r3, #0
  BNE    retry             ; 실패 시 재시도
success:
  MOV    r0, #1
  B      done
fail:
  MOV    r0, #0
done:
```

### 2. LOCK Prefix와 메모리 배리어

#### LOCK Prefix의 역할
```
LOCK CMPXCHG [address], value

효과:
1. 메모리 버스 잠금 (Memory Bus Lock)
   - 다른 CPU 코어가 해당 메모리 주소에 접근 불가

2. 캐시 일관성 프로토콜 (Cache Coherence)
   - MESI Protocol (Modified, Exclusive, Shared, Invalid)
   - 모든 CPU 코어의 캐시 라인을 무효화

3. 메모리 배리어 (Memory Barrier)
   - Store-Load Barrier 자동 삽입
   - 이전 쓰기가 모두 완료된 후 실행
   - 이후 읽기는 CAS 완료 후 실행

비용: ~40-100 CPU 사이클 (캐시 동기화)
```

#### 캐시 라인 무효화 (Cache Invalidation)
```
시나리오: 4개 CPU 코어에서 동일한 변수에 CAS 수행

CPU 0: [Cache Line: value=100, State=Exclusive]
CPU 1: [Cache Line: value=100, State=Shared]
CPU 2: [Cache Line: value=100, State=Shared]
CPU 3: [Cache Line: value=100, State=Shared]

CPU 0이 CAS(100 → 99) 실행:
  1. LOCK CMPXCHG 실행
  2. 메모리 버스에 "Invalidate" 메시지 브로드캐스트
  3. CPU 1-3의 캐시 라인 → Invalid 상태로 전환
  4. CPU 0: value=99, State=Modified

CPU 1이 값을 읽으려고 시도:
  1. Cache Miss 발생 (Invalid 상태)
  2. 메모리 버스를 통해 CPU 0에 요청
  3. CPU 0: Modified 데이터를 메인 메모리에 Write-Back
  4. CPU 1: 메모리에서 값을 읽어옴
  5. CPU 0, 1: State=Shared

비용: Cache Miss ~200-300 CPU 사이클
```

### 3. AtomicLong의 오버헤드 분석

#### ① 단일 CAS 연산 비용
```
경합 없을 때 (Cache Hit):
  1. volatile 읽기: ~10 사이클
  2. LOCK CMPXCHG: ~40 사이클
  3. 총: ~50 사이클 = ~15-20 나노초 (@3GHz CPU)

경합 있을 때 (Cache Miss):
  1. volatile 읽기 (Cache Miss): ~200 사이클
  2. 캐시 일관성 대기: ~100 사이클
  3. LOCK CMPXCHG: ~40 사이클
  4. 캐시 무효화 브로드캐스트: ~100 사이클
  5. 총: ~440 사이클 = ~150 나노초
```

#### ② 재시도 비용 (높은 경합)
```
극한 경합 (500 threads, 동시 CAS):

Thread A:
  - CAS 시도 1회: 50 사이클 (성공)

Thread B:
  - CAS 시도 1회: 440 사이클 (실패, Cache Miss)
  - CAS 시도 2회: 440 사이클 (실패)
  - CAS 시도 3회: 440 사이클 (실패)
  - ... (100회 재시도)
  - 총: ~44,000 사이클 = ~15 마이크로초
  - 100회 초과 → IllegalStateException

★ 핵심: 경합이 심할수록 Cache Miss와 재시도가 급증
         → CPU 사이클 낭비 폭증
```

### 4. False Sharing 문제

#### 캐시 라인 크기
```
현대 CPU의 캐시 라인: 64 바이트

만약 두 변수가 같은 캐시 라인에 있으면:
┌────────────────────────────────────────┐
│ Cache Line (64 bytes)                  │
├────────────────────┬───────────────────┤
│ AtomicLong count1  │ AtomicLong count2 │  ← 둘 다 8바이트
│ (Thread A 사용)    │ (Thread B 사용)   │
└────────────────────┴───────────────────┘

문제:
  Thread A가 count1을 CAS → 전체 캐시 라인 무효화
  → Thread B의 count2도 Cache Miss 발생
  → 서로 영향을 주지 않는 변수인데도 성능 저하

해결책: Padding으로 캐시 라인 분리
```

#### @Contended 어노테이션
```java
@jdk.internal.vm.annotation.Contended
static final class PaddedAtomicLong extends AtomicLong {
    // JVM이 자동으로 128바이트 패딩 추가
    // → 다른 변수와 캐시 라인 공유 방지
}
```

### 5. Lock vs CAS 오버헤드 비교 (CPU 사이클)

```
단일 연산 비용:

synchronized (경합 없음, Biased Locking):
  - 비용: ~30 사이클
  - 이유: Thread ID 확인만

synchronized (경합 있음):
  - 비용: ~5,000-30,000 사이클
  - 이유: OS 시스템 콜 + 컨텍스트 스위칭

ReentrantLock (경합 없음):
  - 비용: ~50 사이클
  - 이유: CAS 1회

ReentrantLock (경합 있음, Spinning):
  - 비용: ~500-1,000 사이클
  - 이유: Spinning 100회

AtomicLong (경합 없음):
  - 비용: ~50 사이클
  - 이유: CAS 1회

AtomicLong (경합 높음):
  - 비용: ~50,000 사이클 (100회 재시도)
  - 이유: CAS 재시도 + Cache Miss

★ 결론: 경합이 심할 때 AtomicLong이 가장 비효율적
```

### 6. 메모리 순서 보장 (Memory Ordering)

#### volatile의 의미
```java
private final AtomicLong quantity;  // 내부적으로 volatile

// 컴파일러와 CPU가 보장해야 하는 것:
quantity.set(100);     // Store
x = quantity.get();    // Load

보장:
1. Store-Load 순서 보장 (Store가 먼저 완료)
2. 다른 스레드에서 즉시 가시성 보장
3. 명령어 재배치(Reordering) 금지

구현: Memory Fence (메모리 펜스)
  - x86: MFENCE (Store-Load Barrier)
  - ARM: DMB (Data Memory Barrier)

비용: ~20-40 사이클
```

### 7. 실전 성능 측정 (하드웨어 관점)

```
테스트: 500 threads, 500,000 재고 감소

AtomicLong:
  - 총 CPU 사이클: ~714억 사이클 (238ms × 3GHz)
  - CAS 성공: 499,971회 × 50 사이클 = ~2,500만 사이클
  - CAS 실패 + 재시도: ~711.5억 사이클 (99.6%)
  - Cache Miss 추정: ~500만 회
  - CPU 사용률: 95% (대부분 Spinning)

synchronized:
  - 총 CPU 사이클: ~171억 사이클 (57ms × 3GHz)
  - 락 획득/해제: 500,000회 × 5,000 사이클 = ~25억 사이클
  - OS 대기 시간: ~146억 사이클 (CPU는 대기)
  - CPU 사용률: 35% (나머지는 BLOCKED)

ReentrantLock:
  - 총 CPU 사이클: ~120억 사이클 (40ms × 3GHz)
  - 락 획득/해제: 500,000회 × 1,000 사이클 = ~5억 사이클
  - Spinning: ~50억 사이클
  - OS 대기: ~65억 사이클
  - CPU 사용률: 55%

★ 핵심: AtomicLong은 CPU를 100% 사용하지만,
        대부분 재시도로 낭비됨 (비생산적 사용)
```

### 8. 동시성 vs 일관성 (재정리)

#### 동시성 (Thread Safety) - 100% 보장
```java
// 재고가 절대 음수가 되지 않음
public boolean decrement(long amount) {
    while (true) {
        long current = quantity.get();
        if (current < amount) {
            return false;  // 재고 부족
        }
        long next = current - amount;
        if (quantity.compareAndSet(current, next)) {
            return true;  // 성공
        }
        // 실패 → 재시도
    }
}

결과:
- 데이터 무결성: ✅ 보장 (음수 재고 불가능)
- Race Condition: ✅ 방지 (CAS로 원자성 보장)
```

#### 일관성 (Consistency/Availability) - 재시도 제한 시 불완전
```java
// 현재 구현: 100회 재시도 제한
public boolean decrement(long amount) {
    for (int i = 0; i < 100; i++) {
        // CAS 시도
    }
    throw new IllegalStateException("재시도 초과");
}

결과:
- 성공률: 99.99% (29건 실패)
- 실패 이유: CAS 재시도 100회 초과 (재고는 충분)
- 비즈니스 영향: 사용자가 "구매 실패" 경험

해결책:
1. 무제한 재시도 (while true) → 100% 보장
2. ReentrantLock 사용 → 100% 보장 + 빠름
```

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
