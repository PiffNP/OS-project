package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.*;

/** The idea of the implementation is that we can make children identify their job,
 * and two of them will become "leader" and "subleader" to guide the rest to the other side.
 * The procedure is: leader and subleader goes to Molokai, leader back,
 * call one guy to goto the other side, that guy call subleader back, finishing a cycle.
 * 
 * After leader and subleader goes to Molokai, leader will wake up boat to decide 
 * if they should stop there.
 *
 * We use a Priority Schduler to ensure that once waked up, the boat thread will
 * immediately check ending condition before any other thread.
 * 
 * The commented print statements will give a hint about what's happening.
 */

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats***");
	begin(100, 100, b);

    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	boolean intStatus = Machine.interrupt().disable();
	Lib.assertTrue(ThreadedKernel.scheduler instanceof PriorityScheduler);
	PriorityScheduler scheduler=(PriorityScheduler)ThreadedKernel.scheduler;
	/* We set the boat's priority to be higher than default
	 * to make sure it check ending condition before any
	 * forked threads.
	 */
	scheduler.setPriority(KThread.currentThread(),2);
    Machine.interrupt().restore(intStatus);
	bg = b;
	// Instantiate global variables here
	lock=new Lock();
	boat_lock=new Lock();
	waitForEnd=new Condition(boat_lock);
	waitForTravel=new Condition(lock);
	leaderCanRun=new Condition(lock);
	subleaderCanRun=new Condition(lock);
	childId=0;			//used for identification of jobs of childs
	waitingNumber=0;	//used for possible race
	finished=0;			//used for boat to decide ending condition
	Runnable adult = new Runnable() {
    public void run() {
            AdultItinerary();
        }
    };
	Runnable child = new Runnable() {
    public void run() {
            ChildItinerary();
        }
    };
    for(int i=0;i<adults;i++){
        KThread t = new KThread(adult);
        t.setName("adult "+i+" thread");
        t.fork();
    }
	for(int i=0;i<children;i++){
        KThread t = new KThread(child);
        t.setName("child "+i+" thread");
        t.fork();
	}
	while(true){
		boat_lock.acquire();
		waitForEnd.sleep();
		if(finished==children+adults){
			boat_lock.release();
			lock.acquire();
			//System.out.println("finished");
			return;	
		}
		boat_lock.release();
	}
    }

    static void AdultItinerary()
    {
		bg.initializeAdult();
		lock.acquire();
		waitingNumber+=1;
		if(waitingNumber==1){
			leaderCanRun.wake();
		}
		waitForTravel.sleep();
		waitingNumber-=1;
		bg.AdultRowToMolokai();finished++;
		subleaderCanRun.wake();
		lock.release();
    }

    static void ChildItinerary()
    {
		bg.initializeChild(); 
		lock.acquire();
		int id=childId;childId+=1;
    	if(id==0){
    		//Leader
    		while(true){
    			//System.out.println("Leader to Molakai");
	    		bg.ChildRowToMolokai();finished++;
	    		subleaderCanRun.wake();
				leaderCanRun.sleep();
				//System.out.println("Leader wait for end");
				boat_lock.acquire();
				waitForEnd.wake();
				boat_lock.release();
				lock.release();
				KThread.yield();
				lock.acquire();
				//System.out.println("Leader go to oahu");
				bg.ChildRowToOahu();finished--;
				if(waitingNumber==0){
					//System.out.println("Leader wait for passenger");
					leaderCanRun.sleep();
				}
				waitForTravel.wake();
				//System.out.println("Leader call passenger");
				leaderCanRun.sleep();
    		}
    	}else if(id==1){
    		//SubLeader, must be called after leader
    		while(true){
    			//System.out.println("subleader to molokai");
    			bg.ChildRideToMolokai();finished++;
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();
    			//System.out.println("subleader to oahu,call leader");
    			bg.ChildRowToOahu();finished--;
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();
    		}
    	}
    	else{
    		//literally the same as adult
    		waitingNumber+=1;
    		if(waitingNumber==1){
				leaderCanRun.wake();
			}
    		waitForTravel.sleep();
    		waitingNumber-=1;
    		bg.ChildRowToMolokai();finished++;
    		subleaderCanRun.wake();
    		lock.release();
    	}
    }
	static Condition leaderCanRun;
	static Condition subleaderCanRun;
    static Condition waitForEnd;
    static Condition waitForTravel;
    static int waitingNumber;
    static int finished;
    static int childId;
    static Lock lock;
    static Lock boat_lock;
}
