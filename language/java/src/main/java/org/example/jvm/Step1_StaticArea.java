package org.example.jvm;

/**
 * Step 1: Static 영역 (Method Area) 이해하기
 *
 * Static 영역에는:
 * 1. 클래스 메타데이터 (클래스 구조 정보)
 * 2. static 변수
 * 3. static 메서드 정보
 */
public class Step1_StaticArea {

    // Static 영역에 저장됨
    static int staticCount = 0;
    static String staticName = "Global";

    public static void main(String[] args) {
        System.out.println("=== Static 영역 테스트 ===\n");

        // 1. static 변수는 클래스 이름으로 접근
        System.out.println("초기 staticCount: " + Step1_StaticArea.staticCount);
        System.out.println("초기 staticName: " + Step1_StaticArea.staticName);

        // 2. static 변수 변경
        Step1_StaticArea.staticCount = 100;
        Step1_StaticArea.staticName = "Changed";

        System.out.println("\n변경 후:");
        System.out.println("staticCount: " + Step1_StaticArea.staticCount);
        System.out.println("staticName: " + Step1_StaticArea.staticName);

        // 3. static 메서드 호출
        printStaticInfo();

        System.out.println("\n--- 메모리 구조 ---");
        System.out.println("Static 영역 (Metaspace):");
        System.out.println("  ├─ Step1_StaticArea 클래스 메타데이터");
        System.out.println("  ├─ staticCount = " + staticCount);
        System.out.println("  ├─ staticName → (참조)");
        System.out.println("  └─ printStaticInfo() 메서드 정보");
        System.out.println("\nHeap 영역:");
        System.out.println("  └─ \"Changed\" 문자열 객체");
    }

    // Static 메서드 (Static 영역에 정보 저장)
    static void printStaticInfo() {
        System.out.println("\nstatic 메서드 실행:");
        System.out.println("  → static 메서드는 객체 생성 없이 호출 가능");
        System.out.println("  → 메서드 정보는 Static 영역에 저장");
    }
}
