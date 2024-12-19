package io.hhplus.tdd.point.integrationTest;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PointConcurrencyControlTest {

    @Autowired
    private PointService pointService;
    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;


    // 충전할 포인트
    private long addAmount = 10L;
    // 사용할 포인트
    private long reduceAmount = 10L;
    // 초기 포인트 값
    private long initPoint;

    @Test
    @DisplayName("동시 포인트 충전 테스트 - 같은 유저 동시 충전, 동기")
    public void concurrentPointChargeTest_sameUserSynchronized() throws InterruptedException {
        //given
        long userId = 123L;
        int threads = 10;

        pointService.patchPointCharge(userId, 100L, System.currentTimeMillis());
        initPoint = pointService.getUserPoint(userId).point();

        // 스레드 수만큼 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        //when
        // 스레드 수만큼 동시 작업 실행
        for(int i=0; i < threads; i++){
            //동시 실행 작업 제출
            executorService.submit(() ->{
                pointService.patchPointCharge(userId, addAmount, System.currentTimeMillis());
            });
        }
        // 스레드 종료 요청
        executorService.shutdown();
        // 작업 완료까지 최대 1분 대기
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        //then
        // 최종 포인트 검증
        long finalPoint = pointService.getUserPoint(userId).point();
        // 예상 포인트와 동시에 포인트 충전을 10회 요청한 최종 포인트가 일치하는지 검증
        assertEquals(initPoint + (threads * addAmount), finalPoint);
    }

    @Test
    @DisplayName("동시 포인트 충전 테스트 - 다른 유저 동시 충전, 비동기")
    public void concurrentPointChargeTest_differentUserAsynchronous() throws InterruptedException {
        //given
        Long userId1 = 123L;
        Long userId2 = 456L;
        int threads = 10;

        pointService.patchPointCharge(userId1, 100L, System.currentTimeMillis());
        long initPoint_userId1 = pointService.getUserPoint(userId1).point();

        pointService.patchPointCharge(userId2, 100L, System.currentTimeMillis());
        long initPoint_userId2 = pointService.getUserPoint(userId2).point();

        // 스레드 수만큼 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        //when
        executorService.submit(() ->{
            pointService.patchPointCharge(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointCharge(userId2, addAmount, System.currentTimeMillis());
        });

        // 스레드 종료 요청
        executorService.shutdown();
        // 작업 완료까지 최대 1분 대기
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        //then
        // 최종 포인트 검증
        long finalPointUserId1 = pointService.getUserPoint(userId1).point();
        assertEquals(initPoint_userId1 + 5 * addAmount, finalPointUserId1);

        long finalPointUserId2 = pointService.getUserPoint(userId2).point();
        assertEquals(initPoint_userId2 + 5 * addAmount, finalPointUserId2);
    }

    @Test
    @DisplayName("동시 포인트 사용 테스트 - 같은 유저 동시 충전, 동기")
    public void concurrentPointUseTest_sameUserSynchronized() throws InterruptedException {
        //given
        long userId = 123L;
        int threads = 10;

        pointService.patchPointCharge(userId, 100L, System.currentTimeMillis());
        initPoint = pointService.getUserPoint(userId).point();

        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        //when
        for(int i=0; i < threads; i++){
            //동시 실행 작업 제출
            executorService.submit(() ->{
                pointService.patchPointUse(userId, reduceAmount, System.currentTimeMillis());
            });
        }
        // 스레드 종료 요청
        executorService.shutdown();
        // 작업 완료까지 최대 1분 대기
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        //then
        // 최종 포인트 검증
        long finalPoint = pointService.getUserPoint(userId).point();
        // 예상 포인트와 동시에 포인트 사용을 10회 요청한 최종 포인트가 일치하는지 검증
        assertEquals(initPoint - (threads * reduceAmount), finalPoint);
    }

    @Test
    @DisplayName("동시 포인트 사용 테스트 - 다른 유저 동시 충전, 비동기")
    public void concurrentPointUseTest_differentUserAsynchronous() throws InterruptedException {
        //given
        Long userId1 = 123L;
        Long userId2 = 456L;
        int threads = 10;

        pointService.patchPointCharge(userId1, 100L, System.currentTimeMillis());
        long initPoint_userId1 = pointService.getUserPoint(userId1).point();

        pointService.patchPointCharge(userId2, 100L, System.currentTimeMillis());
        long initPoint_userId2 = pointService.getUserPoint(userId2).point();

        // 스레드 수만큼 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        //when
        executorService.submit(() ->{
            pointService.patchPointUse(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId1, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId2, addAmount, System.currentTimeMillis());
        });
        executorService.submit(() ->{
            pointService.patchPointUse(userId2, addAmount, System.currentTimeMillis());
        });

        // 스레드 종료 요청
        executorService.shutdown();
        // 작업 완료까지 최대 1분 대기
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        //then
        // 최종 포인트 검증
        long finalPointUserId1 = pointService.getUserPoint(userId1).point();
        assertEquals(initPoint_userId1 - 5 * reduceAmount, finalPointUserId1);

        long finalPointUserId2 = pointService.getUserPoint(userId2).point();
        assertEquals(initPoint_userId2 - 5 * reduceAmount, finalPointUserId2);
    }
}
