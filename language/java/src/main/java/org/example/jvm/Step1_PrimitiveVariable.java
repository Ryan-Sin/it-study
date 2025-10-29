package org.example.jvm;

/**
 * 기초 1단계: int 변수는 어디에 저장될까?
 */
public class Step1_PrimitiveVariable {

    public static void main(String[] args) {
        // 이 코드를 실행하면 무슨 일이 일어날까?
        int number = 42;
        System.out.println("number = " + number);

        /**
         * 1. JWM이 main 메서드를 호출함
         *    -> Stack에 main  메서드용 "Frame" 생성
         * 2. int number = 42; 실행
         *    -> Stack의 main Frame 안에 "number"라는 공간 생성
         *    -> 그 공간에 숫자 42를 직접 저장
         * 3. System.out.println 실행
         *    -> Stack에서 number 값(42)을 읽어서 출력
         *
         *   💾 메모리 상태 그림
         *
         *   ┌─────────────────────────────┐
         *   │         Stack               │  ← main 스레드의 Stack
         *   ├─────────────────────────────┤
         *   │  main() Frame               │
         *   │  ┌───────────────────────┐  │
         *   │  │ number = 42           │  │  ← int는 값이 직접 저장됨
         *   │  └───────────────────────┘  │
         *   └─────────────────────────────┘
         *
         *   ┌─────────────────────────────┐
         *   │         Heap                │  ← 비어있음 (객체 생성 안 함)
         *   └─────────────────────────────┘
         */
    }
}
