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

    public UserPoint patchPointCharge(long userId, long addAmount,long fixTime){
        UserPoint userPoint = userPointTable.selectById(userId);
        if(userPoint == null){
            throw new IllegalArgumentException("해당 사용자 포인트 정보를 찾을 수 없습니다.");
        }

        long newPoint = userPoint.point() + addAmount;
        if(newPoint > MAX_POINT){
            throw new IllegalStateException("최대 잔고 초과로 포인트 충전에 실패하였습니다.");
        }

        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
        pointHistoryTable.insert(userId, addAmount, TransactionType.CHARGE, fixTime);
        return updatedPoint;
    }

    public UserPoint patchPointUse(long userId, long reduceAmount, long fixedTime){
        UserPoint userPoint = userPointTable.selectById(userId);
        if(userPoint == null){
            throw new IllegalArgumentException("해당 사용자 포인트 정보를 찾을 수 없습니다.");
        }

        if(userPoint.point() < reduceAmount){
            throw new IllegalStateException("잔고 포인트 부족으로 포인트 사용에 실패하였습니다.");
        }

        long newPoint = userPoint.point() - reduceAmount;

        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
        pointHistoryTable.insert(userId, reduceAmount, TransactionType.USE, fixedTime);
        return updatedPoint;
    }
}
