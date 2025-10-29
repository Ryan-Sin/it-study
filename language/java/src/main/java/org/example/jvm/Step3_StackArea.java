package org.example.jvm;

/**
 * Step 3: Stack 영역 이해하기
 *
 * Stack 영역에는:
 * 1. 메서드 호출 정보 (Stack Frame)
 * 2. 지역 변수
 *   - Primitive 타입: 값 자체
 *   - Reference 타입: 주소만 (실제 객체는 Heap)
 */
public class Step3_StackArea {

    public static void main(String[] args) {
        System.out.println("=== Stack 영역 테스트 ===\n");

        // 1. Primitive 타입 → Stack에 값 직접 저장
        System.out.println("[1] Primitive 타입 (Stack에 값 저장):");
        int x = 10;
        double y = 3.14;
        boolean flag = true;

        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("flag = " + flag);
        System.out.println("  → Stack의 main() Frame에 값이 직접 저장됨");

        // 2. Reference 타입 → Stack에 주소만, 객체는 Heap
        System.out.println("\n[2] Reference 타입 (Stack에 주소만):");
        String str = "Hello";
        int[] arr = {1, 2, 3};

        System.out.println("str = " + str + " (주소: " + System.identityHashCode(str) + ")");
        System.out.println("arr = " + arr);
        System.out.println("  → Stack에는 주소만, 실제 데이터는 Heap");

        // 3. 메서드 호출 → Stack Frame 추가
        System.out.println("\n[3] 메서드 호출 (Stack Frame 생성):");
        System.out.println("main에서 methodA 호출 전");
        methodA(100);
        System.out.println("main으로 복귀 (methodA Frame 제거됨)");

        // 메모리 구조 출력
        printMemoryStructure();
    }

    static void methodA(int param) {
        System.out.println("  → methodA 호출됨 (Stack Frame 생성)");
        System.out.println("  → param = " + param + " (Stack의 methodA Frame에 저장)");

        int localVar = 200;
        System.out.println("  → localVar = " + localVar + " (Stack의 methodA Frame에 저장)");

        methodB();

        System.out.println("  → methodA 종료 준비");
    }

    static void methodB() {
        System.out.println("    → methodB 호출됨 (Stack Frame 생성)");

        String localStr = "Local";
        System.out.println("    → localStr = " + localStr);
        System.out.println("      (Stack에는 주소, \"Local\"은 Heap)");

        System.out.println("    → methodB 종료 (Frame 제거)");
    }

    static void printMemoryStructure() {
        System.out.println("\n--- 메모리 구조 (메서드 호출 시점) ---");
        System.out.println("\n[main 실행 중]");
        System.out.println("Stack (Thread 1):");
        System.out.println("  └─ main() Frame");
        System.out.println("     ├─ x = 10");
        System.out.println("     ├─ y = 3.14");
        System.out.println("     ├─ flag = true");
        System.out.println("     ├─ str → Heap의 \"Hello\"");
        System.out.println("     └─ arr → Heap의 배열");

        System.out.println("\n[main → methodA 호출]");
        System.out.println("Stack (Thread 1):");
        System.out.println("  ├─ main() Frame");
        System.out.println("  │  ├─ x = 10");
        System.out.println("  │  └─ ...");
        System.out.println("  └─ methodA() Frame  ← 추가됨");
        System.out.println("     ├─ param = 100");
        System.out.println("     └─ localVar = 200");

        System.out.println("\n[main → methodA → methodB 호출]");
        System.out.println("Stack (Thread 1):");
        System.out.println("  ├─ main() Frame");
        System.out.println("  ├─ methodA() Frame");
        System.out.println("  └─ methodB() Frame  ← 추가됨");
        System.out.println("     └─ localStr → Heap의 \"Local\"");

        System.out.println("\n[methodB 종료 → Frame 제거]");
        System.out.println("Stack (Thread 1):");
        System.out.println("  ├─ main() Frame");
        System.out.println("  └─ methodA() Frame");
        System.out.println("     (methodB Frame 제거됨)");
    }
}
