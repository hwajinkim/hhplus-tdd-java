package io.hhplus.tdd.point;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PointValidation {

    // 사용자 id 검증
    public void validateUserId(Long userId) {
        // 사용자 id가 널이거나 0보다 작거나 같을때(음수)
        if(userId == null || userId <= 0){
            throw new IllegalArgumentException("사용자 ID가 유효하지 않습니다.");
        }
    }

    // 포인트 검증
    public void validateAmount(Long amount){

        // 포인트가 널이거나 0보다 작거나 같을때(음수)
        if(amount == null || amount <= 0){
            throw new IllegalArgumentException("포인트 금액이 유효하지 않습니다.");
        }
    }


}
