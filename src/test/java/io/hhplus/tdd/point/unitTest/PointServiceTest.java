package io.hhplus.tdd.point.unitTest;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private PointValidation pointValidation;
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPoint mockUserPoint;

    @Mock
    private List<PointHistory> mockPointHistory;

    private long fixedTime;

    // 리팩토링 : 공통으로 쓰이는 기대값은 테스트 실행전에 생성
    @BeforeEach
    void init(){
        mockUserPoint = new UserPoint(1L, 700L, System.currentTimeMillis());
        mockPointHistory = List.of(
                new PointHistory(1L, 1L, 700L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, 1L, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        fixedTime = 1700000000000L;
    }
    @Test
    @DisplayName("포인트 조회 테스트")
        // 테스트 목적 : userId로 포인트를 조회했을 때, 반환된 데이터가 기대값과 일치하는지 확인
    void getUserPointTest(){
        //given
        long userId = 1L;
        //Mock 개체 동작 정의
        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        //when
        UserPoint userPoint = pointService.getUserPoint(userId);

        //then
        // 반환값 userPoint가 널이 아닌지 체크
        assertNotNull(userPoint);
        // 기대값과 반환값이 동일한지 체크
        assertEquals(mockUserPoint, userPoint);
        // validation 호출 확인
        verify(pointValidation, times(1)).validateUserId(userId);
        // 포인트 조회 호출이 1회 발생했는지 체크
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 조회 실패 테스트 - 해당 사용자 포인트 정보를 찾을 수 없음.")
        // 테스트 목적 : userId로 포인트를 조회했을 때 해당 사용자 포인트 정보가 없으면 ,IllegalArgumentException이 발생하는지 확인
    void getUserPointNotFoundUserFailTest(){
        //given
        long userId = 1L;

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        when(userPointTable.selectById(userId)).thenReturn(null);

        //when
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.getUserPoint(userId));

        //then
        assertEquals("해당 사용자 포인트 정보를 찾을 수 없습니다.", exception.getMessage());
        verify(pointValidation, times(1)).validateUserId(userId);
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 테스트")
        // 테스트 목적 : userId로 포인트 내역을 조회했을 때, 반환된 데이터가 기대값과 일치하는지 확인
    void getUserPointHistoryTest(){
        //given
        long userId = 1L;
        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockPointHistory);

        //when
        List<PointHistory> pointHistories = pointService.getPointHistories(userId);

        //then
        // 반환값이 널이 아닌지 체크
        assertNotNull(pointHistories);
        // 포인트 내역 크기가 2인지 체크
        assertEquals(2, pointHistories.size());
        // 리스트의 기대값과 반환값이 동일한지 체크
        assertEquals(mockPointHistory.get(0),pointHistories.get(0));
        assertEquals(mockPointHistory.get(1),pointHistories.get(1));
        verify(pointValidation, times(1)).validateUserId(userId);
        // 포인트 내역 조회 호출이 1회 발생했는지 체크
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 실패 테스트 - 해당 사용자 포인트 내역을 찾을 수 없음.")
        // 테스트 목적 : userId로 포인트를 조회했을 때 해당 사용자 포인트 내역이 없으면 ,IllegalArgumentException이 발생하는지 확인
    void getUserPointHistoryNotFoundUserFailTest(){
        //given
        long userId = 1L;
        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.getPointHistories(userId));

        //then
        assertEquals("해당 사용자 포인트 내역을 찾을 수 없습니다.", exception.getMessage());
        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 충전 테스트")
        // 테스트 목적 : 충전 요청 포인트만큼 포인트가 증가하고, 반환값이 기대값과 일치하는지 확인
    void patchPointChargeTest(){
        //given
        long userId = 1L;
        long addAmount = 300L;

        // 포인트 충전으로 업데이트 된 데이터 기대값
        UserPoint updateUserPoint = new UserPoint(userId, mockUserPoint.point()+addAmount, System.currentTimeMillis());
        // 포인트 히스토리에 충전 내역 저장된 기대값
        PointHistory mockPointHistory = new PointHistory(1L, userId, addAmount, TransactionType.CHARGE, fixedTime);

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        doNothing().when(pointValidation).validateAmount(addAmount);
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);
        when(userPointTable.insertOrUpdate(userId,updateUserPoint.point())).thenReturn(updateUserPoint);
        when(pointHistoryTable.insert(userId, addAmount, TransactionType.CHARGE, fixedTime)).thenReturn(mockPointHistory);

        //when
        UserPoint chargePoint = pointService.patchPointCharge(userId, addAmount, fixedTime);
        //then
        assertNotNull(chargePoint);
        assertEquals(updateUserPoint, chargePoint);

        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointValidation, times(1)).validateAmount(addAmount);
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, updateUserPoint.point());
        verify(pointHistoryTable, times(1)).insert( userId, addAmount, TransactionType.CHARGE, fixedTime);
    }

    @Test
    @DisplayName("포인트 충전 실패테스트 - 해당 사용자 포인트 정보 없음")
        // 테스트 목적 : userId로 포인트를 조회했을 때 해당 사용자 포인트 정보가 없으면 ,IllegalArgumentException이 발생하는지 확인
    void patchPointChargeNotFoundUserFailTest(){
        //given
        long userId = 1L;
        long addAmount = 300L;

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        doNothing().when(pointValidation).validateAmount(addAmount);

        when(userPointTable.selectById(userId)).thenReturn(null);
        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.patchPointCharge(userId, addAmount, fixedTime));

        // 잘못된 인수로 발생한 예외 메세지가 예상 메세지와 일치하는지 체크
        assertEquals("해당 사용자 포인트 정보를 찾을 수 없습니다.", exception.getMessage());

        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointValidation, times(1)).validateAmount(addAmount);

        verify(userPointTable, times(1)).selectById(userId);
        // 예외가 발생한 이후 포인트 충전 업데이트, 포인트 내역이 기록되지 않았는지 체크
        verify(userPointTable, never()).insertOrUpdate(userId, mockUserPoint.point()+addAmount);
        verify(pointHistoryTable, never()).insert(userId, addAmount, TransactionType.CHARGE, fixedTime);
    }
    @Test
    @DisplayName("포인트 충전 실패테스트 - 최대 잔고 초과")
        // 테스트 목적 : 충전 요청 포인트와 현재 포인트의 합이 최대 잔고 포인트를 초과할 때, IllegalStateException이 발생하는지 확인
    void patchPointChargeExceededBalanceFailTest(){
        //given
        // 충전할 포인트(400) + 현재 포인트(700)가 최대 잔고(1000)보다 크도록 설정
        long userId = 1L;
        long addAmount = 400L;

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        doNothing().when(pointValidation).validateAmount(addAmount);
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        //when & then
        Exception exception = assertThrows(IllegalStateException.class,
                ()-> pointService.patchPointCharge(userId, addAmount, fixedTime));

        // 최대 잔고 초과로 발생한 예외 메세지가 예상 메세지와 일치하는지 체크
        assertEquals("최대 잔고 초과로 포인트 충전에 실패하였습니다.", exception.getMessage());

        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointValidation, times(1)).validateAmount(addAmount);

        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, never()).insertOrUpdate(userId, mockUserPoint.point()+addAmount);
        verify(pointHistoryTable, never()).insert(userId, addAmount, TransactionType.CHARGE, fixedTime);
    }

    @Test
    @DisplayName("포인트 사용 테스트")
        // 테스트 목적 : 사용 요청 포인트만큼 포인트가 감소하고, 반환값이 기대값과 일치하는지 확인
    void patchPointUse(){
        //given
        long userId = 1L;
        long reduceAmount = 300L;

        // 포인트 사용으로 업데이트 된 데이터 기대값
        UserPoint updateUserPoint = new UserPoint(userId, mockUserPoint.point()-reduceAmount, System.currentTimeMillis());
        // 포인트 히스토리에 충전 내역 저장된 기대값
        PointHistory mockPointHistory = new PointHistory(1L, userId, reduceAmount, TransactionType.USE, fixedTime);

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        doNothing().when(pointValidation).validateAmount(reduceAmount);

        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);
        when(userPointTable.insertOrUpdate(userId, updateUserPoint.point())).thenReturn(updateUserPoint);
        when(pointHistoryTable.insert( userId, reduceAmount, TransactionType.USE, fixedTime)).thenReturn(mockPointHistory);

        //when
        UserPoint usePoint = pointService.patchPointUse(userId, reduceAmount, fixedTime);

        //then
        assertNotNull(usePoint);
        // 포인트 사용 기대값과 반환값 비교
        assertEquals(updateUserPoint.point(), usePoint.point());
        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointValidation, times(1)).validateAmount(reduceAmount);

        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, updateUserPoint.point());
        verify(pointHistoryTable, times(1)).insert(userId, reduceAmount, TransactionType.USE, fixedTime);
    }

    @Test
    @DisplayName("포인트 사용 실패 테스트 - 해당 사용자 포인트 정보 없음")
        // 테스트 목적 : userId로 포인트를 조회했을 때 해당 사용자 포인트 정보가 없으면 ,IllegalArgumentException이 발생하는지 확인
    void patchPointUseNotFoundUserFailTest(){
        //given
        long userId = 1L;
        long reduceAmount = 300L;

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        doNothing().when(pointValidation).validateAmount(reduceAmount);

        when(userPointTable.selectById(userId)).thenReturn(null);

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.patchPointUse(userId, reduceAmount, fixedTime));

        // 잘못된 인수로 발생한 예외 메세지가 예상 메세지와 일치하는지 체크
        assertEquals("해당 사용자 포인트 정보를 찾을 수 없습니다.", exception.getMessage());

        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointValidation, times(1)).validateAmount(reduceAmount);

        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, never()).insertOrUpdate(userId,reduceAmount);
        verify(pointHistoryTable, never()).insert(userId, reduceAmount, TransactionType.USE, fixedTime);
    }

    @Test
    @DisplayName("포인트 사용 실패 테스트 - 잔고 부족")
        // 테스트 목적 : 사용 요청 포인트가 현재 포인트보다 클 때, IllegalStateException이 발생하는지 확인
    void patchPointUseLackedBalanceFailTest(){
        //given
        // 사용 요청 포인트가 현재 포인트보다 크도록 설정
        long userId = 1L;
        long reduceAmount = 1000L;

        // validation 통과
        doNothing().when(pointValidation).validateUserId(userId);
        doNothing().when(pointValidation).validateAmount(reduceAmount);

        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        //when & then
        Exception exception = assertThrows(IllegalStateException.class,
                ()-> pointService.patchPointUse(userId, reduceAmount, fixedTime));

        // 잔고 부족으로 발생한 예외 메세지와 예상 메세지가 일치하는지 체크
        assertEquals("잔고 포인트 부족으로 포인트 사용에 실패하였습니다.", exception.getMessage());

        verify(pointValidation, times(1)).validateUserId(userId);
        verify(pointValidation, times(1)).validateAmount(reduceAmount);

        verify(userPointTable, times(1)).selectById(userId);
        // 잔고 부족 예외가 발생한 이후에 포인트 사용 업데이트와 포인트 내역 기록이 되지 않는지 체크
        verify(userPointTable, never()).insertOrUpdate(userId, mockUserPoint.point()-reduceAmount);
        verify(pointHistoryTable, never()).insert(userId, reduceAmount, TransactionType.USE, fixedTime);
    }
}