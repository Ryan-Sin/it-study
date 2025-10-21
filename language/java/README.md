# Java 동시성(Concurrency) 학습 프로젝트

Java에서 멀티스레드 환경의 동시성 문제를 단계별로 학습하고 해결 방법을 비교하는 프로젝트입니다.

## 학습 순서

```
1. basic           → 동시성 문제 재현 (Race Condition)
2. synchronization → synchronized 키워드로 해결
3. explicitlock    → ReentrantLock으로 명시적 제어
4. atomic          → Lock-Free 방식 (AtomicLong)
```

## 패키지 구조

```
src/main/java/org/example/
├── basic/                  # 락 없음 - Race Condition 재현
│   ├── Product.java
│   └── README.md
├── synchronization/        # synchronized 키워드
│   ├── Product.java
│   └── README.md
├── explicitlock/          # ReentrantLock
│   ├── Product.java
│   └── README.md
└── atomic/                # AtomicLong (Lock-Free)
    ├── Product.java
    └── README.md

src/test/java/org/example/
├── basic/
│   └── ProductConcurrencyTest.java
├── synchronization/
│   └── ProductConcurrencyTest.java
├── explicitlock/
│   └── ProductConcurrencyTest.java
└── atomic/
    └── ProductConcurrencyTest.java
```

## 빠른 시작

### 전체 테스트 실행
```bash
./gradlew test
```

### 특정 패키지만 테스트
```bash
./gradlew test --tests org.example.basic.*
./gradlew test --tests org.example.synchronization.*
./gradlew test --tests org.example.explicitlock.*
./gradlew test --tests org.example.atomic.*
```

## 학습 내용 요약

### 1. basic - 동시성 문제 재현
**문제**: Race Condition, Lost Update
**결과**: 1000번 증가 → 실제 342 (손실 658)

```java
public void increment(long quantity) {
    this.quantity += quantity;  // ❌ 원자적이지 않음
}
```

### 2. synchronization - synchronized
**해결**: 모니터 락으로 상호 배제
**결과**: 1000번 증가 → 정확히 1000

```java
public synchronized void increment(long quantity) {
    this.quantity += quantity;  // ✅ 한 번에 한 스레드만 실행
}
```

**장점**: 간단, 자동 락 해제
**단점**: 타임아웃/인터럽트 불가, 공정성 없음

### 3. explicitlock - ReentrantLock
**해결**: 명시적 락 제어로 더 세밀한 관리
**결과**: 1000번 증가 → 정확히 1000

```java
public void increment(long quantity) {
    lock.lock();
    try {
        this.quantity += quantity;  // ✅ 명시적 제어
    } finally {
        lock.unlock();  // 반드시 해제
    }
}
```

**장점**: 타임아웃, 인터럽트, 공정성 지원
**단점**: 수동 unlock 필요

### 4. atomic - Lock-Free
**해결**: CAS 알고리즘으로 락 없이 동기화
**결과**: 1000번 증가 → 정확히 1000

```java
private final AtomicLong quantity = new AtomicLong(0);

public void increment(long quantity) {
    this.quantity.addAndGet(quantity);  // ✅ 원자적 연산
}
```

**장점**: 가장 빠름, Dead Lock 없음
**단점**: 단순 연산만 가능, 경합 높으면 비효율적

## 성능 비교

| 방식 | 대기 방식 | 성능 (경합 낮음) | 성능 (경합 높음) | Dead Lock | 적용 범위 |
|------|----------|----------------|----------------|-----------|----------|
| 락 없음 | - | 매우 빠름 | 매우 빠름 | 없음 | ❌ 동시성 오류 |
| synchronized | BLOCKED | 보통 | 보통 | 가능 | 복잡한 로직 OK |
| ReentrantLock | BLOCKED | 보통 | 보통 | 가능 | 복잡한 로직 OK |
| AtomicLong | RUNNABLE | 빠름 | CPU 낭비 가능 | 없음 | 단순 연산만 |

## 선택 가이드

### 이렇게 선택하세요

```
단순 변수 증감, 설정
└─→ AtomicLong/AtomicInteger

여러 변수 + 복잡한 로직
├─→ 간단한 동기화 필요
│   └─→ synchronized
└─→ 타임아웃/인터럽트/공정성 필요
    └─→ ReentrantLock
```

## 핵심 개념

### Race Condition (경쟁 상태)
여러 스레드가 공유 자원에 동시 접근하여 예상치 못한 결과 발생

### Critical Section (임계 영역)
한 번에 하나의 스레드만 실행되어야 하는 코드 구간

### Mutual Exclusion (상호 배제)
임계 영역에 한 번에 하나의 스레드만 진입하도록 보장

### Atomicity (원자성)
작업이 중단 없이 완전히 수행되거나 전혀 수행되지 않음

### CAS (Compare-And-Swap)
Lock-Free 알고리즘의 핵심. 현재 값이 예상과 같을 때만 업데이트

## 실전 사용 예시

### E-Commerce 재고 관리
```java
// 단순 재고 수량만 관리 → AtomicLong
AtomicLong stock = new AtomicLong(100);
stock.decrementAndGet();

// 재고 + 주문 상태 + 로그 → synchronized 또는 ReentrantLock
synchronized(this) {
    this.stock -= quantity;
    this.orderStatus = "CONFIRMED";
    log.info("Order confirmed");
}
```

### 카운터/통계
```java
// 요청 수, 조회 수 등 → AtomicLong
AtomicLong requestCount = new AtomicLong(0);
requestCount.incrementAndGet();
```

### 복잡한 비즈니스 로직
```java
// 여러 상태 변경 + 검증 → synchronized 또는 ReentrantLock
synchronized(this) {
    if (isValid()) {
        updateA();
        updateB();
        updateC();
    }
}
```

## 참고 자료

- [Java Concurrency in Practice](https://jcip.net/)
- [java.util.concurrent 공식 문서](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html)
- [Brian Goetz - Java Concurrency](https://www.ibm.com/developerworks/java/library/j-jtp/)

## 학습 다음 단계

1. **volatile** 키워드 학습
2. **ReadWriteLock** (읽기/쓰기 분리)
3. **StampedLock** (낙관적 읽기)
4. **Semaphore, CountDownLatch, CyclicBarrier**
5. **ThreadPoolExecutor, ForkJoinPool**
6. **CompletableFuture** (비동기 프로그래밍)

---

> 각 패키지의 README.md에서 더 자세한 설명을 확인할 수 있습니다.
