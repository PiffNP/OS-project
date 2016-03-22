package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

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
		if(finished+2==children+adults){
			lock.release();
			return;	
		}
		leaderCanRun.wake();
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
		bg.AdultRowToMolokai();
		subleaderCanRun.wake();
		finished+=1;
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
	    		bg.ChildRowToMolokai();
				leaderCanRun.sleep();
				waitForEnd.wake();
				leaderCanRun.sleep();//if waked up, still needs work
				bg.ChildRowToOahu();
				if(waitingNumber==0){
					leaderCanRun.sleep();
				}
				waitForTravel.wake();
				leaderCanRun.sleep();
    		}
    	}else if(id==1){
    		//SubLeader, must be after leader
    		while(true){
    			bg.ChildRideToMolokai();
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();//if waked up, go back
    			bg.ChildRowToOahu();
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();
    		}
    	}
    	else{
    		waitingNumber+=1;
    		if(waitingNumber==1){
				leaderCanRun.wake();
			}
    		waitForTravel.sleep();
    		waitingNumber-=1;
    		bg.ChildRowToMolokai();
    		subleaderCanRun.wake();
    		finished+=1;
    		lock.release();
    	}
    }
	static Condition leaderCanRun;
	static Condition subleaderCanRun;
    static Condition waitForEnd;
    static Condition waitForTravel;
    static int waitingNumber;
    static int finished;
    static int childId;package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

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
		if(finished+2==children+adults){
			lock.release();
			return;	
		}
		leaderCanRun.wake();
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
		bg.AdultRowToMolokai();
		subleaderCanRun.wake();
		finished+=1;
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
	    		bg.ChildRowToMolokai();
				leaderCanRun.sleep();
				waitForEnd.wake();
				leaderCanRun.sleep();//if waked up, still needs work
				bg.ChildRowToOahu();
				if(waitingNumber==0){
					leaderCanRun.sleep();
				}
				waitForTravel.wake();
				leaderCanRun.sleep();
    		}
    	}else if(id==1){
    		//SubLeader, must be after leader
    		while(true){
    			bg.ChildRideToMolokai();
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();//if waked up, go back
    			bg.ChildRowToOahu();
    			leaderCanRun.wake();
    			subleaderCanRun.sleep();
    		}
    	}
    	else{
    		waitingNumber+=1;
    		if(waitingNumber==1){
				leaderCanRun.wake();
			}
    		waitForTravel.sleep();
    		waitingNumber-=1;
    		bg.ChildRowToMolokai();
    		subleaderCanRun.wake();
    		finished+=1;
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
}

    static Lock lock;
}
