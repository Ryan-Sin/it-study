# basic - 동시성 문제 재현

## 개요
락(Lock)을 사용하지 않았을 때 발생하는 **Race Condition**과 **Lost Update** 문제를 재현합니다.

## 핵심 개념

### Race Condition (경쟁 상태)
여러 스레드가 동시에 공유 자원에 접근하여 예상치 못한 결과가 발생하는 상황

### Lost Update (손실된 갱신)
```
Thread 1: read(100) → +1 → write(101)
Thread 2: read(100) → +1 → write(101)
결과: 101 (기대값: 102)
```

## 코드 예시

```java
public void increment(long quantity) {
    this.quantity += quantity;  // 원자적이지 않은 연산!
}
```

이 연산은 실제로 3단계로 나뉩니다:
1. **Read**: 메모리에서 값 읽기
2. **Modify**: 값 증가
3. **Write**: 메모리에 다시 쓰기

## 실행 방법

```bash
./gradlew test --tests org.example.basic.ProductConcurrencyTest
```

## 예상 결과

```
[락 ❌] 예상: 1000, 실제: 342, 손실: 658
```

## 왜 1000개 스레드를 사용하는가?

1. **타임슬라이스 vs 연산 속도**
   - OS 스케줄러의 타임슬라이스: 약 5~10ms
   - quantity++ 연산 시간: 약 10ns
   - 소수의 스레드는 순차 실행되어 문제가 잘 드러나지 않음

2. **컨텍스트 스위칭 강제**
   - 스레드 수 >> CPU 코어 수
   - OS가 강제로 컨텍스트 스위칭을 자주 수행
   - Read-Modify-Write 중간에 끼어들 확률 증가

3. **Thread.sleep(100)의 역할**
   - 모든 스레드가 동시에 값을 읽은 후 대기
   - 100ms 후 거의 동시에 깨어나 동일한 값으로 덮어씀
   - Lost Update 확실히 재현

## 다음 단계
→ [synchronization](../synchronization/README.md): `synchronized` 키워드로 문제 해결
