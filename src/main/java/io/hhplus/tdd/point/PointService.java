package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private Long MAX_POINT = 1000L;

    public UserPoint getUserPoint(long userId){
        UserPoint userPoint = userPointTable.selectById(userId);
        if(userPoint == null){
            throw new IllegalArgumentException("해당 사용자 포인트 정보를 찾을 수 없습니다.");
        }
        return userPoint;
    }

    public List<PointHistory> getPointHistories(long userId){
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);
        if(pointHistories == null){
            throw new IllegalArgumentException("해당 사용자 포인트 내역을 찾을 수 없습니다.");
        }
        return pointHistories;
    }
}
