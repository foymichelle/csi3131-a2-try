import java.util.Random;
import java.util.concurrent.Semaphore;

/***************************************************************************************/

//  Provide code for the methods in the classes Aeroplane and Airport, and in one place
//  in the main() method. Look for "your code here" comments.

/* the main class of assignment 2, launching the simulation */

public class Assignment2 {
  // Configuration
  final static int DESTINATIONS = 4;
  final static int AEROPLANES = 6;
  final static int PLANE_SIZE = 3;
  final static int PASSENGERS = 20;
  final static String[] destName = {"Toronto", "New York", "New Delhi", "Beijing"};

  public static void main(String args[]) {
    int i;
    Aeroplane[] sships = new Aeroplane[AEROPLANES];
    Passenger[] passengers = new Passenger[PASSENGERS];

    // create the airport
    Airport sp = new Airport();

    /* create aeroplanes and passengers*/
    for (i=0; i<AEROPLANES; i++) sships[i] = new Aeroplane(sp, i);
    for (i=0; i<PASSENGERS; i++) passengers[i] = new Passenger(sp, i);

    /* now launch them */
    for (i=0; i<AEROPLANES; i++) sships[i].start();
    for (i=0; i<PASSENGERS; i++) passengers[i].start();

    // let them enjoy for 20 seconds
    try { Thread.sleep(20000);} catch (InterruptedException e) { }

    /* now stop them */
    // note how we are using deferred cancellation
    for (i=0; i<AEROPLANES; i++) try {sships[i].interrupt();} catch (Exception e) { }
    for (i=0; i<PASSENGERS; i++) try {passengers[i].interrupt();} catch (Exception e) { }

    // Wait until everybody else is finished
    for (i=0; i<AEROPLANES; i++) try {sships[i].join();} catch (Exception e) { }
    for (i=0; i<PASSENGERS; i++) try {passengers[i].join();} catch (Exception e) { }

    // This should be the last thing done by this program:
    System.out.println("Simulation finished.");
  }
}

/* The class implementing a passenger. */
// This class is completely provided to you, you don't have to change
// anything, just have a look and understand what the passenger wants from
// the airport and from the aeroplanes
class Passenger extends Thread {
  private boolean enjoy;
  private int id;
  private Airport sp;

  // constructor
  public Passenger(Airport sp, int id) {
    this.sp = sp;
    this.id = id;
    enjoy = true;
  }

  // this is the passenger's thread
  public void run() {
    int stime;
    int dest;
    Aeroplane sh;

    while (enjoy) {
      try {
        // Wait and arrive to the port
        stime = (int) (700*Math.random());
        sleep(stime);
        // Choose the destination
        dest = (int) (((double) Assignment2.DESTINATIONS)*Math.random());
        System.out.println("Passenger " + id + " wants to go to " + Assignment2.destName[dest]);

        // come to the airport and board a aeroplane to my destination
        // (might wait if there is no such aeroplane ready)
        sh = sp.wait4Ship(dest);

        // Should be executed after the aeroplane is on the pad and taking passengers
        System.out.println("Passenger " + id + " has boarded aeroplane " + sh.id + ", destination: "+Assignment2.destName[dest]);
        // wait for launch
        sh.wait4launch();

        // Enjoy the ride

        // Should be executed after the aeroplane has launched.
        System.out.println("Passenger "+id+" enjoying the ride to "+Assignment2.destName[dest]+ ": Woohooooo!");

        // wait for landing
        sh.wait4landing();

        // Should be executed after the aeroplane has landed
        System.out.println("Passenger " + id + " leaving the aeroplane " + sh.id);

        // Leave the aeroplane
        sh.leave();
      } catch (InterruptedException e) {
        enjoy = false; // have been interrupted, probably by the main program, terminate
      }
    }
    System.out.println("Passenger "+id+" has finished its rides.");
  }
}

/* The class simulating an aeroplane */
// Now, here you will have to implement several methods
class Aeroplane extends Thread {
  public int id;
  private Airport sp;
  private boolean enjoy;
  // your code here (other local variables and semaphores)
  int passengers; // number of passengers on plane
  Semaphore alertLand; // to alert passengers that we have landed
  Semaphore alertLaunch; // to alert passengers that we have launched
  Semaphore planeReady;

  // constructor
  public Aeroplane(Airport sp, int id) {
    this.sp = sp;
    this.id = id;
    enjoy = true;
    // your code here (local variable and semaphore initializations)
    passengers = 0;
    alertLand = new Semaphore(0, true);
    alertLaunch = new Semaphore(0, true);
  }

  // the aeroplane thread executes this
  public void run() {
    int stime;
    int dest;

    while (enjoy) {
      try {
        // Wait until there an empty landing pad, then land
        dest = sp.wait4landing(this);
        System.out.println("Aeroplane " + id + " landing on pad " + dest);

        planeReady = sp.semPadReady[dest];
        // Tell the passengers that we have landed
        sp.semPadLeave[id].release();

        // Wait until all passengers leave
        try {
          planeReady.acquire(); // plane waits to be ready for passengers
        } catch (InterruptedException e) { break; }

        System.out.println("Aeroplane " + id + " boarding to "+Assignment2.destName[dest]+" now!");

        // the passengers can start to board now
        sp.boarding(dest); //

        // Wait until full of passengers

        // 4, 3, 2, 1, Launch!
        try {
          sp.semPadLaunch[id].acquire();
        } catch (InterruptedException e) { }
        sp.launch(dest);

        System.out.println("Aeroplane " + id + " launches towards "+Assignment2.destName[dest]+"!");

        // tell the passengers we have launched, so they can enjoy now ;-)
        for (int i=0; i<passengers; i++) {
          alertLaunch.release();
        }
        // Fly in the air
        stime = 500+(int) (1500*Math.random());
        sleep(stime);
      } catch (InterruptedException e) {
        enjoy = false; // have been interrupted, probably by the main program, terminate
      }
    }
    System.out.println("Aeroplane "+id+" has finished its flights.");
  }

  // service functions to passengers

  // called by the passengers leaving the aeroplane
  public void leave() throws InterruptedException  {
    // your code here
    try {
      sp.semPadLeave[id].acquire();
    } catch (InterruptedException e) { }

    passengers--;

    if (passengers == 0) {
      // allow new passengers to board
      planeReady.release();
    }
    else {
      sp.semPadLeave[id].release(); // allow more passengers to leave
    }
  }

  // called by the passengers sitting in the aeroplane, to wait
  // until the launch
  public void wait4launch() throws InterruptedException {
    // your code here
    try {
      alertLaunch.acquire();
    } catch (InterruptedException e) { }
  }

  // called by the bored passengers sitting in the aeroplane, to wait
  // until landing
  public void wait4landing() throws InterruptedException {
    // your code here
    try {
      sp.semAeroLand[id].acquire();
    } catch (InterruptedException e) { }
  }
}

/* The class implementing the Airport. */
/* This might be convenient place to put lots of the synchronization code into */
class Airport {
  Aeroplane[] pads;
  // what is sitting on a given pad

  // your code here (other local variables and semaphores)
  Semaphore[] semPadLand = new Semaphore[Assignment2.DESTINATIONS]; // for planes to request to land on pad
  Semaphore[] semPadReady = new Semaphore[Assignment2.DESTINATIONS]; // for planes on pad ready to accept passengers
  Semaphore[] semPadLaunch = new Semaphore[Assignment2.AEROPLANES]; // for planes to request to launch from pad
  Semaphore[] semPadBoard = new Semaphore[Assignment2.AEROPLANES]; // for passengers to request to board plane
  Semaphore[] semPadLeave = new Semaphore[Assignment2.AEROPLANES]; // for passengers to request to leave plane
  Semaphore[] semAeroLand = new Semaphore[Assignment2.AEROPLANES]; // for passengers to know when plane landed

  // constructor
  public Airport() {
    int i;
    pads = new Aeroplane[Assignment2.DESTINATIONS];

    // pads[] is an array containing the aeroplanes sitting on corresponding pads
    // Value null means the pad is empty
    for(i=0; i<Assignment2.DESTINATIONS; i++) {
      pads[i] = null;
      semPadLand[i] = new Semaphore(1, true); // initially all pads are empty/available
      semPadReady[i] = new Semaphore(0, true);

    }
    // your code here (local variable and semaphore initializations)
    for (i=0; i<Assignment2.AEROPLANES; i++) {
      semPadBoard[i] = new Semaphore(0, true);
      semPadLeave[i] = new Semaphore(0, true);
      semAeroLand[i] = new Semaphore(0, true);
      semPadLaunch[i] = new Semaphore(0, true);
    }
  }

  // called by a passenger wanting to go to the given destination
  // returns the aeroplane he/she boarded
  // Careful here, as the pad might be empty at this moment
  public Aeroplane wait4Ship(int dest) throws InterruptedException {
    // your code here
    Aeroplane plane = pads[dest];

    // check if there is a plane on pad to destination
    while (plane == null) {
      if (pads[dest] != null) {
        plane = pads[dest];
      }
    }

    // System.out.println("=============================");
    //
    // for (int i=0; i<pads.length; i++) {
    //   if (pads[i] != null) {
    //     System.out.println("pads["+i+"] = plane "+pads[i].id);
    //   }
    //   else {
    //     System.out.println("pads["+i+"] = empty");
    //   }
    // }
    //
    // System.out.println("=============================");

    // try to board plane
    try {
      semPadBoard[plane.id].acquire();
    } catch (InterruptedException e) { }

    plane.passengers++; // increase # passengers

    if (plane.passengers < Assignment2.PLANE_SIZE) {
      semPadBoard[plane.id].release(); // allow more passengers to board
    }
    else if (plane.passengers == Assignment2.PLANE_SIZE) {
      semPadLaunch[plane.id].release(); // we are ready to launch
    }

    return plane;
  }

  // called by an aeroplane to tell the airport that it is accepting passengers now to destination dest
  public void boarding(int dest) {
    // your code here
    Aeroplane plane = pads[dest];
    semPadBoard[plane.id].release();
  }

  // called by an aeroplane returning from a trip
  // Returns the number of the empty pad where to land (might wait
  // until there is an empty pad).
  // Try to rotate the pads so that no destination is starved
  public int wait4landing(Aeroplane sh)  throws InterruptedException  {
    // your code here
    boolean found = false;
    int pad = -1;

    while (!found) {
      int i = (int) ((Assignment2.DESTINATIONS)*Math.random());

      if (pads[i] == null) { // check empty pad
        found = true;
        pad = i;
        try {
          semPadLand[pad].acquire(); // try to land on pad
        } catch (InterruptedException e) { }
        pads[i] = sh;
        break;
      }
    }

    if (sh.passengers == 0) {
      semPadReady[pad].release(); // if plane arrives empty it can accept passengers right away
    }
    else {
      semPadLeave[sh.id].release(); // passengers can now leave
    }

    return pad;
  }

  // called by an aeroplane when it launches, to inform the
  // airport that the pad has been emptied
  public void launch(int dest) {
    // your code here
    semPadLand[dest].release(); // pad is now empty
    pads[dest] = null;
  }
}
