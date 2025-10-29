# 📦 JVM 메모리 구조

## 🎯 전통적인 3대 영역

JVM 메모리는 크게 **3가지 영역**으로 나뉩니다:

1. **Static 영역** (Method Area, Class Area)
2. **Heap 영역**
3. **Stack 영역**

---

## 1. Static 영역 (Method Area / Class Area)

### 📌 개념
- 클래스 정보와 static 변수가 저장되는 영역
- 모든 스레드가 **공유**

### 🏗️ 구현 방식 (Java 버전별)

**Java 7 이전: PermGen (Permanent Generation)**
- 클래스 메타데이터 + static 변수 저장
- **문제점**: 고정 크기 → `OutOfMemoryError: PermGen space` 자주 발생
- 설정: `-XX:PermSize=128m -XX:MaxPermSize=256m`

**Java 8 이후: Metaspace**
- PermGen 제거, Metaspace로 교체
- Native 메모리 사용 → **동적 확장** 가능
- 설정: `-XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m`

### 💾 뭐가 저장되나요?

1. **클래스 메타데이터**
   - 클래스 구조 정보 (필드, 메서드 이름, 타입 등)
   - 메서드의 바이트코드
   - 상수 풀 (Constant Pool)

2. **Static 변수**
   - `static` 키워드로 선언한 클래스 변수
   - 클래스 로딩 시 한 번만 생성되고, 프로그램 종료 시까지 유지

3. **Runtime Constant Pool**
   - 문자열 리터럴, 숫자 상수 등

### 📝 예시

```java
public class MyClass {
    // Static 영역에 저장
    static int count = 0;
    static String name = "Hello";

    static void printCount() {
        System.out.println(count);
    }
}
```

**메모리 상태:**
```
Static 영역 (Metaspace)
├─ MyClass 클래스 메타데이터
├─ count (static 변수) → 참조는 여기, 값 0은 여기 저장
├─ name (static 변수) → 참조는 여기, "Hello" 객체는 Heap
└─ printCount() 메서드 바이트코드
```

---

## 2. Heap 영역

### 📌 개념
- `new` 키워드로 생성한 모든 **객체**가 저장되는 영역
- 모든 스레드가 **공유**
- **GC (Garbage Collection)**의 대상

### 💾 뭐가 저장되나요?

1. **모든 객체 인스턴스**
   - `new`로 생성한 객체
   - 객체의 인스턴스 변수 (필드)

2. **배열**
   - `int[]`, `String[]` 등 모든 배열

3. **문자열 객체**
   - `"Hello"` 같은 문자열 리터럴 (String Pool에 저장)
   - `new String("Hello")` 같은 명시적 객체

### 🏗️ Heap 내부 구조 (GC 관점)

```
Heap
├─ Young Generation (젊은 세대)
│  ├─ Eden Space          ← 새로 생성된 객체
│  ├─ Survivor 0 (S0)     ← Minor GC 후 살아남은 객체
│  └─ Survivor 1 (S1)     ← Minor GC 후 살아남은 객체
└─ Old Generation (늙은 세대)
   └─ Tenured Space       ← 오래 살아남은 객체
```

### 📝 예시

```java
public class Person {
    String name;  // 인스턴스 변수
    int age;

    public static void main(String[] args) {
        Person p = new Person();  // Heap에 객체 생성
        p.name = "Kim";           // Heap의 객체 안에 저장
        p.age = 30;

        int[] arr = {1, 2, 3};    // Heap에 배열 생성
    }
}
```

**메모리 상태:**
```
Heap
├─ Person 객체
│  ├─ name → "Kim" (String 객체도 Heap)
│  └─ age → 30
└─ int[] 배열 {1, 2, 3}

Stack (main 스레드)
├─ p → Heap의 Person 객체 주소
└─ arr → Heap의 배열 주소
```

### ⚙️ 설정
- `-Xms512m`: 초기 Heap 크기
- `-Xmx2g`: 최대 Heap 크기

---

## 3. Stack 영역

### 📌 개념
- 메서드 호출과 지역 변수를 저장하는 영역
- **각 스레드마다 독립적**으로 생성
- **LIFO (Last In First Out)** 구조

### 💾 뭐가 저장되나요?

1. **메서드 호출 정보 (Stack Frame)**
   - 메서드가 호출될 때마다 Frame 생성
   - 메서드 종료 시 Frame 제거

2. **지역 변수 (Local Variables)**
   - 메서드 안에서 선언한 변수
   - Primitive 타입: **값 자체** 저장
   - Reference 타입: **주소(참조)만** 저장, 실제 객체는 Heap

3. **Operand Stack**
   - 연산 중간 결과 저장

4. **Frame Data**
   - 메서드 복귀 주소, 예외 처리 정보

### 🏗️ Stack Frame 구조

```
Stack (Thread 1)
│
├─ main() Frame
│  ├─ Local Variables
│  │  ├─ int x = 10       ← 값 10이 직접 저장
│  │  └─ Person p         ← Heap의 주소만 저장
│  ├─ Operand Stack
│  └─ Frame Data
│
└─ methodA() Frame
   ├─ Local Variables
   ├─ Operand Stack
   └─ Frame Data
```

### 📝 예시

```java
public class StackExample {
    public static void main(String[] args) {
        int x = 10;              // Stack: 값 10 직접 저장
        String str = "Hello";    // Stack: 주소만, "Hello"는 Heap

        methodA(x);              // Stack에 methodA Frame 생성
    }

    static void methodA(int param) {
        int y = 20;              // Stack: methodA Frame 안에 저장
        System.out.println(param + y);
    }  // methodA 종료 → Frame 제거
}
```

**실행 순서별 메모리 상태:**

```
[1] main 시작
Stack (Thread 1)
└─ main() Frame
   ├─ x = 10
   └─ str → Heap의 "Hello"

[2] methodA 호출
Stack (Thread 1)
├─ main() Frame
│  ├─ x = 10
│  └─ str → Heap
└─ methodA() Frame
   ├─ param = 10 (복사된 값)
   └─ y = 20

[3] methodA 종료
Stack (Thread 1)
└─ main() Frame
   ├─ x = 10
   └─ str → Heap
   (methodA Frame 제거됨)
```

### ⚙️ 설정
- `-Xss1m`: 스레드당 Stack 크기

### ⚠️ StackOverflowError
- 재귀 호출이 너무 깊을 때 발생
- Stack 크기를 초과하면 에러

```java
void infiniteRecursion() {
    infiniteRecursion();  // Stack Frame이 계속 쌓임
}  // StackOverflowError 발생!
```

---

## 🖼️ 전체 메모리 구조

```
┌───────────────────────────────────────────────────────────┐
│                    JVM Process                            │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Static 영역 (Metaspace in Java 8+)                  │  │
│  │  - 모든 스레드 공유                                     │  │
│  │  - 클래스 메타데이터                                    │  │
│  │  - static 변수                                       │  │
│  │  - Runtime Constant Pool                            │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Heap 영역                                           │  │
│  │  - 모든 스레드 공유                                     │  │
│  │  - GC 대상                                           │  │
│  │                                                     │  │
│  │  ┌────────────────────────────────────────────────┐ │  │
│  │  │ Young Generation                               │ │  │
│  │  │  ├─ Eden                                       │ │  │
│  │  │  ├─ Survivor 0                                 │ │  │
│  │  │  └─ Survivor 1                                 │ │  │
│  │  └────────────────────────────────────────────────┘ │  │
│  │                                                     │  │
│  │  ┌────────────────────────────────────────────────┐ │  │
│  │  │ Old Generation                                 │ │  │
│  │  └────────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ Stack (Thread 1) │  │ Stack (Thread 2) │  ...          │
│  │ - 스레드별 독립      │  │ - 스레드별 독립     │               │
│  │                  │  │                  │               │
│  │ ┌──────────────┐ │  │ ┌──────────────┐ │               │
│  │ │ main() Frame │ │  │ │ run() Frame  │ │               │
│  │ └──────────────┘ │  │ └──────────────┘ │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

## 🔑 핵심 차이점 요약

| 영역 | 공유 여부 | 저장 대상 | GC 대상 | 생명주기 | JVM 옵션 |
|------|----------|-----------|---------|---------|----------|
| **Static** | 모든 스레드 공유 | 클래스 정보, static 변수 | △ (Class Unloading 시) | 클래스 로딩~언로딩 | `-XX:MetaspaceSize` |
| **Heap** | 모든 스레드 공유 | 객체, 배열 | ✅ | 객체 생성~GC | `-Xms`, `-Xmx` |
| **Stack** | 스레드별 독립 | 지역 변수, 메서드 호출 | ❌ | 메서드 호출~종료 | `-Xss` |

---

## 💡 실전 예제

```java
public class MemoryExample {
    // Static 영역
    static int staticCount = 0;
    static String staticName = "Global";

    // 인스턴스 변수 (객체 생성 시 Heap에 저장)
    int instanceCount;
    String instanceName;

    public static void main(String[] args) {
        // Stack: x, y, z (값 직접 저장)
        int x = 10;
        int y = 20;
        int z = x + y;

        // Stack: obj1, obj2 (주소만 저장)
        // Heap: MemoryExample 객체 2개
        MemoryExample obj1 = new MemoryExample();
        MemoryExample obj2 = new MemoryExample();

        obj1.instanceCount = 1;  // Heap의 obj1 객체 안에 저장
        obj2.instanceCount = 2;  // Heap의 obj2 객체 안에 저장

        staticCount = 100;       // Static 영역에 저장 (모든 객체 공유)

        methodA();               // Stack에 Frame 추가
    }

    static void methodA() {
        // Stack: methodA Frame 안에 local 저장
        int local = 5;
        System.out.println(local);
    }  // Frame 제거
}
```

**메모리 상태:**
```
Static 영역 (Metaspace)
├─ MemoryExample 클래스 메타데이터
├─ staticCount = 100
└─ staticName → "Global" (Heap)

Heap
├─ MemoryExample 객체 (obj1)
│  ├─ instanceCount = 1
│  └─ instanceName = null
├─ MemoryExample 객체 (obj2)
│  ├─ instanceCount = 2
│  └─ instanceName = null
└─ "Global" 문자열 객체

Stack (main 스레드)
└─ main() Frame
   ├─ x = 10
   ├─ y = 20
   ├─ z = 30
   ├─ obj1 → Heap의 첫 번째 객체 주소
   └─ obj2 → Heap의 두 번째 객체 주소
```

---

## 📚 용어 정리

| 용어 | 다른 이름 | 의미 |
|------|----------|------|
| **Static 영역** | Method Area, Class Area, Metaspace (Java 8+), PermGen (Java 7 이전) | 클래스 정보 + static 변수 저장 영역 |
| **Heap 영역** | - | 객체 저장 영역 |
| **Stack 영역** | Call Stack, Execution Stack | 메서드 호출 + 지역 변수 저장 영역 |
