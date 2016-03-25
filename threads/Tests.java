package nachos.threads;
import nachos.machine.*;

/** Various tests done here*/
public class Tests{	
	public static void test(int testType){		
		if(testType == KThreadJoinTest){
			class JoinTest implements Runnable {
				JoinTest(KThread thread) {
					this.thread = thread;
				}

				public void run() {
					if(thread == null){
						orderCheck(2, "running target");
						return;
					}else{
						orderCheck(1, "running join");
						try{
							thread.join();
							orderCheck(3, "end join");
						} catch (Error e){
							System.out.println("Error caught.");
							flag = false;
						} finally{
							unitTestEnd();
						}
					}
				}
				
				private KThread thread;
			}
			
		    unitTestInit(1, "basic two threads joining");
			KThread target = new KThread(new JoinTest(null)).setName("target");
		    KThread joiner = new KThread(new JoinTest(target)).setName("joiner");
		    joiner.fork();
			target.fork();
			unitTestStart();
		    unitTestCheck(true);
		    
		    unitTestInit(2, "self joining");
			final KThread selfJoin = new KThread().setName("selfJoin");
			selfJoin.setTarget(new Runnable() {
						public void run() {
							try{
								selfJoin.join();
							} catch (Error e){
								System.out.println("Error caught.");
								flag = false;
							} finally{
								unitTestEnd();
							}
						}
			});
			selfJoin.fork();
			unitTestStart();
			unitTestCheck(false);
			
		    unitTestInit(3, "call joining twice");
		    target = new KThread(new JoinTest(null)).setName("target");
		    joiner = new KThread(new JoinTest(target)).setName("joiner1");
		    KThread joiner2 = new KThread(new JoinTest(target)).setName("joiner2");
			joiner.fork();
			joiner2.fork();
			target.fork();
			unitTestStart();
			unitTestCheck(false);
		} else if(testType == PrioritySchedulerTest){
			PriorityScheduler.selfTest();
		} else if(testType == BoatTest){
		    Boat.selfTest();
		}
	}
	
	private static void unitTestInit(int id, String description){
		System.out.println("\nTest #" + id + ": " + description);
		lock = new Lock();
		condition = new Condition(lock);
		flag = true;
		counter = 0;
		Tests.id = id;
	}
	
	private static void unitTestStart(){
	    lock.acquire();
	    condition.sleep();
		lock.release();
	}
	
	private static void unitTestEnd(){
	    lock.acquire();
	    condition.wake();
		lock.release();
	}
	
	/** order index starts with 1*/
	private static void orderCheck(int order){
		if(++counter != order)
			flag = false;
		//System.out.println(counter + " " + order);
	}
	private static void orderCheck(int order, String message){
		orderCheck(order);
		Lib.debug(dbgThread, message);
	}
	
	private static void unitTestCheck(boolean flagObjState){
		System.out.println("Test #" + id + ' ' + (flag == flagObjState? "passed" : "failed") + ".\n");
	}

	private static int id;
	private static Lock lock;
	private static Condition condition;
	private static boolean flag;
	private static int counter;
	
	private static final char dbgThread = 'z';
	
	/** tests type */
	public static final int KThreadJoinTest = 0;
	public static final int PrioritySchedulerTest = KThreadJoinTest + 1;
	public static final int BoatTest = PrioritySchedulerTest + 1;
}

