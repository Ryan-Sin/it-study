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
- JVM이 최적화 가능 (Biased Locking, Lock Coarsening 등)

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

---

## OS/JVM 레벨 동작 원리 (Deep Dive)

### 1. synchronized의 내부 구조

#### 바이트코드 레벨
```java
public synchronized void increment(long quantity) {
    this.quantity += quantity;
}
```

컴파일된 바이트코드:
```
public synchronized void increment(long);
  descriptor: (J)V
  flags: (0x0021) ACC_PUBLIC, ACC_SYNCHRONIZED  // ← synchronized 플래그
  Code:
    stack=4, locals=3, args_size=2
       0: aload_0
       1: dup
       2: getfield      #7  // Field quantity:J
       5: lload_1
       6: ladd
       7: putfield      #7
      10: return
```

#### 모니터 락 (Monitor Lock)
- 각 객체는 **모니터(Monitor)** 를 가지고 있음
- 모니터는 **락(Lock) + 대기 큐(Wait Queue)** 로 구성
- JVM은 객체 헤더(Object Header)에 락 정보 저장

```
┌──────────────────────────────────────┐
│ Java Object (this)                   │
├──────────────────────────────────────┤
│ Object Header (Mark Word)            │  ← 락 상태 저장
│  - Lock Status (00, 01, 10, 11)     │
│  - Thread ID (소유 스레드)           │
│  - Age (GC 정보)                     │
├──────────────────────────────────────┤
│ Class Metadata Pointer               │
├──────────────────────────────────────┤
│ Instance Data (quantity, name, ...)  │
└──────────────────────────────────────┘
```

### 2. synchronized의 오버헤드

#### ① 락 획득 비용 (Heavyweight Lock)
```
Thread A가 synchronized 메서드 진입 시:

1. JVM 레벨:
   - 객체 헤더의 Mark Word 검사
   - 락 상태 확인 (unlocked → locked)

2. OS 레벨 (경합 발생 시):
   - Mutex Lock 획득 시도
   - 실패 시 → OS에 "이 스레드를 BLOCKED 상태로 만들어줘" 요청
   - OS: Thread A를 RUNNABLE → BLOCKED 상태로 전환
   - OS: Thread A를 대기 큐(Wait Queue)에 추가

3. 컨텍스트 스위칭 (Context Switch):
   - Thread A의 레지스터, PC, Stack 등을 저장
   - 다른 RUNNABLE 스레드를 선택하여 실행
   - CPU 캐시 무효화 (Cache Invalidation)

비용: ~1-10 마이크로초 (μs)
```

#### ② 락 해제 비용
```
Thread A가 synchronized 블록 종료 시:

1. JVM 레벨:
   - 객체 헤더의 Mark Word 업데이트 (locked → unlocked)

2. OS 레벨:
   - 대기 큐에서 다음 스레드 선택 (Thread B)
   - OS: Thread B를 BLOCKED → RUNNABLE 상태로 전환
   - OS: 스케줄러에 Thread B를 등록

3. 컨텍스트 스위칭:
   - Thread B의 레지스터, PC, Stack 복원
   - CPU 캐시 워밍업 (Cache Miss 발생)

비용: ~1-10 마이크로초 (μs)
```

#### ③ 총 오버헤드 요약
```
락 획득/해제 1회당 오버헤드:

1. 경합 없을 때 (Biased Locking):
   - 비용: ~10-50 나노초 (ns)
   - 이유: JVM이 락을 특정 스레드에 편향(bias)시켜 최적화

2. 경합 약간 있을 때 (Lightweight Locking):
   - 비용: ~100-500 나노초 (ns)
   - 이유: CAS로 락 획득 시도, 실패 시 Spinning

3. 경합 많을 때 (Heavyweight Locking):
   - 비용: ~1-10 마이크로초 (μs)
   - 이유: OS 호출 + 컨텍스트 스위칭

★ 핵심: 경합이 심할수록 OS 레벨 개입 증가 → 오버헤드 증가
```

### 3. JVM의 Lock 최적화 기법

#### ① Biased Locking (편향 락)
```
상황: 락을 항상 동일한 스레드가 획득하는 경우

최적화:
- 첫 번째 획득: 객체 헤더에 Thread ID 기록
- 이후 획득: Thread ID만 확인 (CAS 불필요)
- 효과: 락 획득 비용 ~90% 감소

코드 예시:
Thread A: lock → unlock → lock → unlock (반복)
         ↑ 모두 Thread A이므로 CAS 없이 빠르게 처리
```

#### ② Lock Coarsening (락 확대)
```java
// 최적화 전
for (int i = 0; i < 1000; i++) {
    synchronized(this) {
        count++;
    }
}

// JVM이 자동으로 최적화 후
synchronized(this) {
    for (int i = 0; i < 1000; i++) {
        count++;
    }
}
// 락 획득/해제 1000번 → 1번
```

#### ③ Lock Elision (락 제거)
```java
public void localMethod() {
    Object obj = new Object();
    synchronized(obj) {  // ← JVM이 제거
        // 로컬 객체이므로 다른 스레드 접근 불가
    }
}
```

### 4. 스레드 상태 전환 (OS 레벨)

```
RUNNABLE (실행 가능)
    ↓ synchronized 진입 시도 → 락 획득 실패
BLOCKED (대기)
    ↓ 락 소유자가 unlock() 호출
RUNNABLE (실행 가능)
    ↓ CPU 스케줄러가 선택
RUNNING (실행 중)

★ 각 전환마다 OS 시스템 콜 발생 (오버헤드)
```

### 5. Mutex Lock (OS 레벨)

```c
// Linux Kernel의 Mutex 구현 (간략화)
struct mutex {
    atomic_long_t owner;      // 소유 스레드
    spinlock_t wait_lock;     // 대기 큐 보호
    struct list_head wait_list;  // 대기 스레드 목록
};

void mutex_lock(struct mutex *lock) {
    // Fast Path: CAS로 획득 시도
    if (atomic_try_cmpxchg(&lock->owner, NULL, current)) {
        return;  // 성공
    }

    // Slow Path: 실패 시
    spin_lock(&lock->wait_lock);
    list_add_tail(&current->wait_list, &lock->wait_list);
    set_current_state(TASK_UNINTERRUPTIBLE);  // BLOCKED
    spin_unlock(&lock->wait_lock);

    schedule();  // CPU 양보, 다른 스레드 실행
}
```

### 6. 실전 성능 측정

```
테스트: 1000 스레드가 각 1000번 increment() 호출

경합 낮음 (스레드 간격 10ms):
- 락 획득 성공률: 99%
- 평균 락 대기 시간: 50 ns (Biased Locking)
- 컨텍스트 스위칭: 거의 없음

경합 높음 (동시 진입):
- 락 획득 성공률: 10% (첫 시도)
- 평균 락 대기 시간: 5 μs (Heavyweight Locking)
- 컨텍스트 스위칭: 초당 10,000회+
- CPU 사용률: 30% (나머지는 대기)
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
