package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 3 children***");
	begin(100, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	// Instantiate global variables here
	lock=new Lock();
	canEnd=new Condition(lock);
	waitForEnd=new Condition(lock);
	waitForTravel=new Condition(lock);
	leaderCanRun=new Condition(lock);
	subleaderCanRun=new Condition(lock);
	childId=0;
	waitingNumber=0;
	finished=0;
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
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
        t.setName("adult thread");
        t.fork();
    }
	for(int i=0;i<children;i++){
        KThread t = new KThread(child);
        t.setName("child thread");
        t.fork();
	}
	while(true){
		lock.acquire();
		if(finished==children+adults){
			lock.release();
			return;	
		}
		System.out.println("boat call up leader");
		canEnd.wake();
		waitForEnd.sleep();
		lock.release();
	}
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
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
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
		lock.acquire();
		int id=childId;
    	childId+=1;
    	if(id==0){
    		//Leader
    		while(true){
    			System.out.println("Leader to Molakai");
	    		bg.ChildRowToMolokai();finished++;
	    		subleaderCanRun.wake();
				leaderCanRun.sleep();
				System.out.println("Leader wait for end");
				waitForEnd.wake();
				canEnd.sleep();//if waked up, still needs work
					System.out.println("Leader go to oahu");
				bg.ChildRowToOahu();finished--;
				if(waitingNumber==0){
					System.out.println("Leader wait for passenger");
					leaderCanRun.sleep();
				}
				waitForTravel.wake();
				System.out.println("Leader call passenger");
				leaderCanRun.sleep();
    		}
    	}else if(id==1){
    		//SubLeader, must be after leader
    		while(true){
    			System.out.println("subleader to molokai");
    			bg.ChildRideToMolokai();finished++;
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();//if waked up, go back
    			System.out.println("subleader to oahu,call leader");
    			bg.ChildRowToOahu();finished--;
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();
    		}
    	}
    	else{
    		System.out.println("passenger arrive");
    		waitingNumber+=1;
    		if(waitingNumber==1){
				leaderCanRun.wake();
			}
    		waitForTravel.sleep();
    		waitingNumber-=1;
    		System.out.println("passenger goes to Molokai");
    		bg.ChildRowToMolokai();finished++;
    		subleaderCanRun.wake();
    		lock.release();
    	}
    }
    static Condition canEnd;
	static Condition leaderCanRun;
	static Condition subleaderCanRun;
    static Condition waitForEnd;
    static Condition waitForTravel;
    static int waitingNumber;
    static int finished;
    static int childId;
    static Lock lock;
}
