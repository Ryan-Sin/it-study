package org.example.jvm;

/**
 * Step 4: Static + Heap + Stack 종합 예제
 *
 * 3가지 메모리 영역을 한 번에 확인
 */
public class Step4_Complete {

    // Static 영역에 저장
    static int staticCount = 0;
    static String staticName = "Global";

    // 인스턴스 변수 (객체 생성 시 Heap에 저장)
    int instanceCount;
    String instanceName;

    public static void main(String[] args) {
        System.out.println("=== JVM 메모리 구조 종합 예제 ===\n");

        // Stack: Primitive 지역 변수
        int x = 10;
        int y = 20;
        int z = x + y;

        System.out.println("[1] Stack 영역:");
        System.out.println("  x = " + x + " (Stack에 값 저장)");
        System.out.println("  y = " + y + " (Stack에 값 저장)");
        System.out.println("  z = " + z + " (Stack에 값 저장)");

        // Heap: 객체 생성
        Step4_Complete obj1 = new Step4_Complete();
        Step4_Complete obj2 = new Step4_Complete();

        obj1.instanceCount = 1;
        obj1.instanceName = "Object1";

        obj2.instanceCount = 2;
        obj2.instanceName = "Object2";

        System.out.println("\n[2] Heap 영역:");
        System.out.println("  obj1: count=" + obj1.instanceCount + ", name=" + obj1.instanceName);
        System.out.println("  obj2: count=" + obj2.instanceCount + ", name=" + obj2.instanceName);
        System.out.println("  → 각 객체는 Heap의 독립적인 공간에 저장");

        // Static: 클래스 변수
        staticCount = 100;
        staticName = "Changed";

        System.out.println("\n[3] Static 영역:");
        System.out.println("  staticCount = " + staticCount + " (모든 객체 공유)");
        System.out.println("  staticName = " + staticName + " (모든 객체 공유)");
        System.out.println("  obj1.staticCount = " + obj1.staticCount);
        System.out.println("  obj2.staticCount = " + obj2.staticCount);
        System.out.println("  → static 변수는 객체와 무관하게 공유됨");

        // Stack: 메서드 호출
        System.out.println("\n[4] 메서드 호출 (Stack Frame):");
        int result = calculate(x, y);
        System.out.println("  계산 결과: " + result);

        // 전체 메모리 구조 출력
        printCompleteMemoryStructure();
    }

    static int calculate(int a, int b) {
        System.out.println("  → calculate() 호출 (Stack Frame 생성)");
        int sum = a + b;  // Stack의 calculate Frame에 저장
        int multiply = a * b;  // Stack의 calculate Frame에 저장

        System.out.println("    sum = " + sum);
        System.out.println("    multiply = " + multiply);
        System.out.println("  → calculate() 종료 (Frame 제거 준비)");

        return sum;
    }

    static void printCompleteMemoryStructure() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("전체 메모리 구조");
        System.out.println("=".repeat(60));

        System.out.println("\n┌─ Static 영역 (Metaspace) ─────────────────────┐");
        System.out.println("│ Step4_Complete 클래스 정보                    │");
        System.out.println("│  ├─ staticCount = 100                        │");
        System.out.println("│  ├─ staticName → \"Changed\" (Heap)           │");
        System.out.println("│  ├─ main() 메서드 정보                       │");
        System.out.println("│  └─ calculate() 메서드 정보                  │");
        System.out.println("└───────────────────────────────────────────────┘");

        System.out.println("\n┌─ Heap 영역 ───────────────────────────────────┐");
        System.out.println("│ Step4_Complete 객체 1                         │");
        System.out.println("│  ├─ instanceCount = 1                         │");
        System.out.println("│  └─ instanceName → \"Object1\" (Heap)         │");
        System.out.println("│                                               │");
        System.out.println("│ Step4_Complete 객체 2                         │");
        System.out.println("│  ├─ instanceCount = 2                         │");
        System.out.println("│  └─ instanceName → \"Object2\" (Heap)         │");
        System.out.println("│                                               │");
        System.out.println("│ 문자열 객체들                                  │");
        System.out.println("│  ├─ \"Global\"                                 │");
        System.out.println("│  ├─ \"Changed\"                                │");
        System.out.println("│  ├─ \"Object1\"                                │");
        System.out.println("│  └─ \"Object2\"                                │");
        System.out.println("└───────────────────────────────────────────────┘");

        System.out.println("\n┌─ Stack 영역 (main 스레드) ────────────────────┐");
        System.out.println("│ main() Frame                                  │");
        System.out.println("│  ├─ x = 10                                    │");
        System.out.println("│  ├─ y = 20                                    │");
        System.out.println("│  ├─ z = 30                                    │");
        System.out.println("│  ├─ obj1 → Heap의 객체1 주소                  │");
        System.out.println("│  ├─ obj2 → Heap의 객체2 주소                  │");
        System.out.println("│  └─ result = 30                               │");
        System.out.println("└───────────────────────────────────────────────┘");

        System.out.println("\n🔑 핵심 포인트:");
        System.out.println("  1. Primitive (int, double) → Stack에 값 저장");
        System.out.println("  2. 객체 (new) → Heap에 저장, Stack엔 주소만");
        System.out.println("  3. static 변수 → Static 영역, 모든 객체 공유");
        System.out.println("  4. 메서드 호출 → Stack에 Frame 생성/제거");
    }
}
