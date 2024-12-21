# 동시성 제어 방식 분석 보고서
## 동시성 제어 정의
동시성 제어는 여러 프로세스, 스레드가 동시에 동일한 자원(데이터, 파일 메모리 등)을 접근하거나 수정할 때 발생할 수 있는 문제를 방지하고, 데이터 무결성, 일관성, 안정성을 보장하는 기술이다.
## 동시성 제어할 레벨 선택
동시성을 제어하는 방법으로는 애플리케이션, 데이터베이스, 분산 데이터베이스 레벨에서 제어를 할 수 있다.
하지만 이번 과제는 분산 환경은 고려하지 않고, 데이터베이스는 인베모리 DB를 사용하고 구현체는 수정하지 않아야 하기 때문에 애플리케이션 레벨에서 동시성을 제어하도록 한다.
## 자바에서 동시성 제어하는 방식 후보
### 동시성 제어
+ synchronized : 한 번에 하나의 스레드만 임계영역에 접근하도록 한다. 락을 획득하지 못한 다른 스레드들은 대기 상태가 되어 전반적인 실행 시간이 길어져 성능이 저하된다는 단점있다. 
+ ReentrantLock : ReentrantLock은 synchronized 와 유사하지만 더 세밀한 제어가 가능하고,  lock()과 unlock() 메서드를 통해 잠금을 수동을 관리할 수 있다.
+ synchronized vs ReentrantLock 차이
  * synchronized

    * synchronized 블럭으로 동기화를 하면 자동적으로 lock이 잠기고 풀린다.
      (synchronized 블럭 내에서 예외가 발생해도 lock은 자동적으로 해제)
    * 그러나 같은 메소드 내에서만 lock을 걸 수 있다는 제약이 존재
    * 암묵적인 lock 방식
    * WAITING 상태인 스레드는 interrupt가 불가
      ```
      synchronized(lock) {
      // 임계영역
      }
      ```
  * ReentrantLock
    * synchronized와 달리 수동으로 lock을 잠그고 해제한다.
    * 명시적인 lock 방식
    * 암묵적인 락만으로 해결할 수 없는 복잡한 상황에서 사용
    * lockInterruptably() 함수를 통해 WAITING 상태의 스레드를 interrupt 할 수 있다.
       ```
      lock.lock();
      // 임계영역
      lock.unlock();
      ```
### 순서 보장
* ExecutorService : 스레드 풀을 관리하고 작업을 큐에 넣어서 동시성 문제를 해결한다. 여러 작업을 병렬로 실행할 수 있고, 작업이 완료되면 결과를 받을 수 있다.
* Concurrent Collections : 자바에서 동시성 문제를 처리할 수 있는 컬렉션 클래스를 제공하는데,
ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue 등이 있다.
---
선택 : 
세밀한 잠금 범위를 설정할 수 있는 ReentrantLock을 사용하여 교착 상태를 방지하고 , 큐를 사용하여 순서 보장하면서 병렬로 실행 가능한 ExecutorService를 사용해 보려고 한다.

## 동시성 제어 과정
```
// 10개의 스레드
private int threads = 10;
// 충전할 포인트
private long addAmount = 10L;
// 초기 포인트 값
private long initPoint;

@BeforeEach
public void setUp(){
	// 기본 포인트 100L 충전
	...
	// 초기 포인트 값
	...
}

@Test
@DisplayName("동시 포인트 충전 테스트 - 10L 포인트 10회 동시 충전")
public void concurrentPointChargeTest() throws InterruptedException {
    //given
    ExecutorService executorService = Executors.newFixedThreadPool(threads);

    //when
    for(int i=0; i < threads; i++){
        executorService.submit(() ->{
            pointService.patchPointCharge(userId, addAmount, System.currentTimeMillis());
        });
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    //then
    long finalPoint = pointService.getUserPoint(userId).point();
    // 예상 포인트와 동시에 포인트 충전을 10회 요청한 최종 포인트가 일치하는지 검증
    assertEquals(initPoint + (threads * addAmount), finalPoint);
}
```
스레드 풀을 10개로 생성하고 스레드 수 만큼 동시 작업을 실행하면 
동시성 제어가 안 되어 예상포인트와 최종 포인트 값이 차이가 나는 것을 확인할 수 있다.
<img width="1212" alt="스크린샷 2024-12-19 오후 9 22 42" src="https://github.com/user-attachments/assets/b266af48-04bd-4f8c-97a3-bb0ae01e3d47" />
이를 해결하기 위해서는 임계영역에 접근하기 전에 lock을 걸어주어 한 번에 한 스레드만 접근할 수 있도록 해야한다.
```
private ReentrantLock lock = new ReentrantLock(true);

public UserPoint patchPointCharge(long userId, long addAmount,long fixTime){
    lock.lock();
    try{
			// 포인트 충전 로직 ...        
    } finally {
        lock.unlock();
    }
}
```
ReentrantLock을 사용하여 동시성을 제어한 결과 
<img width="1006" alt="스크린샷 2024-12-19 오후 10 00 15" src="https://github.com/user-attachments/assets/3643c85a-14fb-4fb9-aad6-719e7ed920bb" />
예상값과 최종값이 일치하여 테스트를 성공하는 것을 확인할 수 있다.
그런데 스레드 10개로 돌렸을 때 6초 정도 소요되어 효율성이 떨어지는 것을 확인할 수 있다.
이에 대한 성능을 개선하기 위한 리팩토링을 진행해야한다.

## 최적화된 동시성 고민
* 서로 다른 유저인 경우
  * 대기가 발생하지 않아야 한다.
  * 효율성을 위해서 각각 동시에 실행되어야 한다. 이 경우  데이터 정합성이 깨지진 않는다.
* 같은 유저인 경우
  * 데이터를 읽는 시점, 쓰는 시점에 따라 데이터 정합성의 문제가 발생 가능하다.
  * 이 경우 동시성 제어가 필요하다.

** 사용자 ID에 따라 동시성 제어를 하느냐, 하지 않느냐 구별해주기 위해서 ConcurrentHashMap을 사용하여 처리한다.

### 리팩토링 구현 방식

사용자 ID에 따라 lock을 적절하게 걸어주기 위해서는 ConcurrentHashMap을 사용하여 사용자별로 락 개체를 관리하게끔 하였다.

```
    public UserPoint patchPointCharge(long userId, long addAmount,long fixTime){
	ReentrantLock lock = lockMap.computeIfAbsent(userId, id -> new ReentrantLock());

        try{
            // 동일 사용자에 대해 동기화
            lock.lock();
            // 충전 로직 ...
        } finally {
            lock.unlock(); // 락 해제
            lockMap.remove(userId, lock); // 락 객체 정리
        }
    }
```
* ConcurrentHashMap
	* 사용자별로 고유한 키(`userId`)를 맵에 저장하여 `ReentrantLock` 객체를 관리
	* computeIfAbsent` 메서드를 사용해 필요할 때만 락을 생성
* ReentrantLock
	* 동일한 사용자 키에 대해 동기화 처리를 보장
* 비동기 처리
	* 다른 사용자는 각자의 락을 사용하기 때문에 서로 영향을 받지 않고 병렬 처리 가능

### 최적화 후 스레드 처리 속도 비교

* 동일한 사용자가 충전 요청을 10번 했을때 - 6 sec 343ms 발생
<img width="1558" alt="스크린샷 2024-12-20 오전 2 19 53" src="https://github.com/user-attachments/assets/08aa1bf8-927a-414f-b21b-31a7b5c31d16" />

* 다른 사용자가 충전 요청을 10번 했을때 - 5 sec  429ms 발생
<img width="1591" alt="스크린샷 2024-12-20 오전 2 20 22" src="https://github.com/user-attachments/assets/66573a71-047c-4ba8-94b7-bf808e708a64" />
