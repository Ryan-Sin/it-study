package org.example.jvm;

/**
 * Step 2: Heap 영역 이해하기
 *
 * Heap 영역에는:
 * 1. new로 생성한 모든 객체
 * 2. 배열
 * 3. 문자열 객체
 */
public class Step2_HeapArea {

    // 인스턴스 변수 (객체 생성 시 Heap에 저장됨)
    String name;
    int age;

    public static void main(String[] args) {
        System.out.println("=== Heap 영역 테스트 ===\n");

        // 1. 객체 생성 → Heap에 저장
        System.out.println("[1] 객체 생성:");
        Step2_HeapArea person1 = new Step2_HeapArea();
        person1.name = "Kim";
        person1.age = 30;

        Step2_HeapArea person2 = new Step2_HeapArea();
        person2.name = "Lee";
        person2.age = 25;

        System.out.println("person1: " + person1.name + ", " + person1.age);
        System.out.println("person2: " + person2.name + ", " + person2.age);
        System.out.println("  → 두 객체는 Heap의 다른 위치에 저장됨");

        // 2. 배열 생성 → Heap에 저장
        System.out.println("\n[2] 배열 생성:");
        int[] numbers = {1, 2, 3, 4, 5};
        String[] names = {"Alice", "Bob", "Charlie"};

        System.out.println("numbers 배열: " + numbers);  // 주소 출력
        System.out.println("names 배열: " + names);      // 주소 출력
        System.out.println("  → 배열도 객체이므로 Heap에 저장됨");

        // 3. 문자열 생성 → Heap에 저장 (String Pool)
        System.out.println("\n[3] 문자열 생성:");
        String str1 = "Hello";           // String Pool (Heap)
        String str2 = "Hello";           // 같은 객체 재사용
        String str3 = new String("Hello"); // 새로운 객체 생성

        System.out.println("str1 == str2: " + (str1 == str2));  // true (같은 객체)
        System.out.println("str1 == str3: " + (str1 == str3));  // false (다른 객체)
        System.out.println("  → 리터럴은 String Pool 재사용, new는 새 객체 생성");

        // 메모리 구조 출력
        printMemoryStructure(person1, person2, numbers, names);
    }

    static void printMemoryStructure(Step2_HeapArea p1, Step2_HeapArea p2,
                                     int[] nums, String[] strs) {
        System.out.println("\n--- 메모리 구조 ---");
        System.out.println("Heap 영역:");
        System.out.println("  ├─ Step2_HeapArea 객체 1 (person1)");
        System.out.println("  │  ├─ name → \"Kim\" (Heap)");
        System.out.println("  │  └─ age = 30");
        System.out.println("  ├─ Step2_HeapArea 객체 2 (person2)");
        System.out.println("  │  ├─ name → \"Lee\" (Heap)");
        System.out.println("  │  └─ age = 25");
        System.out.println("  ├─ int[] 배열 {1, 2, 3, 4, 5}");
        System.out.println("  ├─ String[] 배열 {주소1, 주소2, 주소3}");
        System.out.println("  ├─ \"Kim\" 문자열 객체");
        System.out.println("  ├─ \"Lee\" 문자열 객체");
        System.out.println("  ├─ \"Alice\" 문자열 객체");
        System.out.println("  ├─ \"Bob\" 문자열 객체");
        System.out.println("  └─ \"Charlie\" 문자열 객체");
        System.out.println("\nStack 영역 (main 스레드):");
        System.out.println("  └─ main() Frame");
        System.out.println("     ├─ person1 → Heap의 객체1 주소");
        System.out.println("     ├─ person2 → Heap의 객체2 주소");
        System.out.println("     ├─ numbers → Heap의 배열 주소");
        System.out.println("     └─ names → Heap의 배열 주소");
    }
}
