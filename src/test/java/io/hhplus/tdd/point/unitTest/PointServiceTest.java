package io.hhplus.tdd.point.unitTest;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Mock
    UserPoint mockUserPoint;

    @Mock
    List<PointHistory> mockPointHistory;

    // 리팩토링 : 공통으로 쓰이는 기대값은 테스트 실행전에 생성
    @BeforeEach
    void init(){
        mockUserPoint = new UserPoint(1L, 700L, System.currentTimeMillis());
        mockPointHistory = List.of(
                new PointHistory(1L, 1L, 700L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, 1L, 500L, TransactionType.USE, System.currentTimeMillis())
        );
    }
    @Test
    @DisplayName("포인트 조회 테스트")
    // 테스트 목적 : userId로 포인트를 조회했을 때, 반환된 데이터가 기대값과 일치하는지 확인
    void getUserPointTest(){
        //given
        long userId = 1L;
        //Mock 개체 동작 정의
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        //when
        UserPoint userPoint = pointService.getUserPoint(userId);

        //then
        // 반환값 userPoint가 널이 아닌지 체크
        assertNotNull(userPoint);
        // 기대값과 반환값이 동일한지 체크
        assertEquals(mockUserPoint, userPoint);
        // 포인트 조회 호출이 1회 발생했는지 체크
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 조회 실패 테스트 - 해당 사용자 포인트 정보를 찾을 수 없음.")
    // 테스트 목적 : userId로 포인트를 조회했을 때 해당 사용자 포인트 정보가 없으면 ,IllegalArgumentException이 발생하는지 확인
    void getUserPointNotFoundUserFailTest(){
        //given
        long userId = 1L;
        when(userPointTable.selectById(userId)).thenReturn(null);

        //when
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.getUserPoint(userId));

        //then
        assertEquals("해당 사용자 포인트 정보를 찾을 수 없습니다.", exception.getMessage());
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 테스트")
    // 테스트 목적 : userId로 포인트 내역을 조회했을 때, 반환된 데이터가 기대값과 일치하는지 확인
    void getUserPointHistoryTest(){
        //given
        long userId = 1L;
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
        // 포인트 내역 조회 호출이 1회 발생했는지 체크
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 실패 테스트 - 해당 사용자 포인트 내역을 찾을 수 없음.")
    // 테스트 목적 : userId로 포인트를 조회했을 때 해당 사용자 포인트 내역이 없으면 ,IllegalArgumentException이 발생하는지 확인
    void getUserPointHistoryNotFoundUserFailTest(){
        //given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.getPointHistories(userId));

        //then
        assertEquals("해당 사용자 포인트 내역을 찾을 수 없습니다.", exception.getMessage());
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }
}
