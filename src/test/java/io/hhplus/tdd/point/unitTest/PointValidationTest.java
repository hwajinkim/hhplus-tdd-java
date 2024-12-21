package io.hhplus.tdd.point.unitTest;

import io.hhplus.tdd.point.PointValidation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class PointValidationTest {

    private final PointValidation pointValidation = new PointValidation();

    @Test
    @DisplayName("사용자 id 유효성 테스트")
    void validateUserIdTest(){
        // given
        Long userId = 1L;

        //when & then
        // 예외 발생하지 않는지 확인
        assertDoesNotThrow(()-> pointValidation.validateUserId(userId));
    }

    @Test
    @DisplayName("사용자 id 유효성 실패테스트 - null값")
    void validateUserIdFailTest_nullId(){
        //given
        Long userId = null;

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointValidation.validateUserId(userId));

        assertEquals("사용자 ID가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("사용자 id 유효성 실패테스트 - 음수값")
    void validateUserIdFailTest_negativeId(){
        //given
        Long userId = -1L;

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointValidation.validateUserId(userId));

        assertEquals("사용자 ID가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("사용자 id 유효성 실패테스트 - 0값")
    void validateUserIdFailTest_zeroId(){
        //given
        Long userId = 0L;

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointValidation.validateUserId(userId));

        assertEquals("사용자 ID가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 유효성 테스트")
    void validateAmountTest(){
        // given
        Long amount = 100L;

        //when & then
        // 예외 발생하지 않는지 확인
        assertDoesNotThrow(()-> pointValidation.validateAmount(amount));
    }

    @Test
    @DisplayName("사용자 id 유효성 실패테스트 - null값")
    void validateAmountFailTest_nullAmount(){
        //given
        Long amount = null;

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointValidation.validateAmount(amount));

        assertEquals("포인트 금액이 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 유효성 실패테스트 - 음수값")
    void validateAmountFailTest_negativePoint(){
        //given
        Long amount = -1L;

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointValidation.validateAmount(amount));

        assertEquals("포인트 금액이 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 유효성 실패테스트 - 0값")
    void validateAmountFailTest_zeroPoint(){
        //given
        Long amount = 0L;

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointValidation.validateAmount(amount));

        assertEquals("포인트 금액이 유효하지 않습니다.", exception.getMessage());
    }

}
