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
						//System.out.println("hhh");
						return;
					}else{
						//System.out.println("lol");
						orderCheck(1, "running join");
						thread.join();
						//System.out.println("XD");
						orderCheck(3, "end join");
					    unitTestEnd();
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
								throw e;
							} finally{
								unitTestEnd();
							}
						}
			});
			try{
				joiner.fork();
				unitTestStart();
			} catch (Error e){
				System.out.println("Error caught.");
				flag = false;
			}
			unitTestCheck(false);		    
		} else if(testType == PriorutySchedulerTest){

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
		System.out.println(message);
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
	public static final int PriorutySchedulerTest = KThreadJoinTest + 1;
}

