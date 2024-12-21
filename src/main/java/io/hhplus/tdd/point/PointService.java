package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointValidation pointValidation;

    private  final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    // ReentrantLock 생성자의 매개변수를 true 전달
    // => 가장 오래 기다린 쓰레드가 lock을 획득할 수 있게 공정(fair)하게 처리
    //private ReentrantLock lock = new ReentrantLock(true);

    private Long MAX_POINT = 1000L;

    public UserPoint getUserPoint(long userId){
        pointValidation.validateUserId(userId);
        UserPoint userPoint = userPointTable.selectById(userId);
        if(userPoint == null){
            throw new IllegalArgumentException("해당 사용자 포인트 정보를 찾을 수 없습니다.");
        }
        return userPoint;
    }

    public List<PointHistory> getPointHistories(long userId){
        pointValidation.validateUserId(userId);
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);
        if(pointHistories == null){
            throw new IllegalArgumentException("해당 사용자 포인트 내역을 찾을 수 없습니다.");
        }
        return pointHistories;
    }

    public UserPoint patchPointCharge(long userId, long addAmount,long fixTime){
        ReentrantLock lock = lockMap.computeIfAbsent(userId, id -> new ReentrantLock());
        // .lock() 메서드를 통해 락을 획득
        // .unlock()을 사용하여 하나의 스레드 실행 후엔 무조건 락을 해제하여 교착상태를 방지한다.
        // try-finally를 사용하여 예외가 발생해도 항상 unlock() 호출

        try{
            // 동일 사용자에 대해 동기화
            lock.lock();
            pointValidation.validateUserId(userId);
            pointValidation.validateAmount(addAmount);
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
        } finally {
            lock.unlock(); // 락 해제
            lockMap.remove(userId, lock); // 락 객체 정리
        }
    }

    public UserPoint patchPointUse(long userId, long reduceAmount, long fixedTime){
        ReentrantLock lock = lockMap.computeIfAbsent(userId, id -> new ReentrantLock());

        try{
            lock.lock();
            pointValidation.validateUserId(userId);
            pointValidation.validateAmount(reduceAmount);
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
        } finally {
            lock.unlock(); // 락 해제
            lockMap.remove(userId, lock); // 락 객체 정리
        }
    }
}
