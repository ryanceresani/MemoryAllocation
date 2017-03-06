package memAllocator;

import java.util.ArrayList;

public class MemoryTester {

	static final long MEMORY_SIZE = 200;
	static final long MEMORY_ADDRESS = 0;
	
	public static void main(String[] args) {
		ArrayList<Job> jobArray = jobBuilder();
		FixedMemory fix = new FixedMemory(MEMORY_SIZE, MEMORY_ADDRESS);
		DynamicMemory dyn = new DynamicMemory(MEMORY_SIZE, MEMORY_ADDRESS);
		
		simulateAllocation(fix, jobArray, Algorithm.BEST_FIT);
		simulateAllocation(dyn, jobArray, Algorithm.BEST_FIT);
		
	}

	private static void simulateAllocation(DynamicMemory dyn, ArrayList<Job> jobArray, Algorithm algo) {
		System.out.println();
		System.out.println("DYNAMIC MEMORY SIMULATION");
		dyn.printFreeList();
		dyn.addJob(algo, jobArray.get(0));
		dyn.printSnapShot();
		dyn.printFreeList();
		
		dyn.addJob(algo, jobArray.get(1));
		dyn.printSnapShot();
		dyn.addJob(algo, jobArray.get(2));
		dyn.printSnapShot();
		dyn.addJob(algo, jobArray.get(3));
		dyn.printSnapShot();
		dyn.printFreeList();
		dyn.removeJob(1);
		dyn.printFreeList();
		dyn.addJob(algo, jobArray.get(4));
		dyn.printSnapShot();
		dyn.addJob(algo, jobArray.get(5));
		dyn.printSnapShot();
		dyn.removeJob(4);
		dyn.printSnapShot();
		dyn.printWaitQueue();
		dyn.removeJob(3);
		dyn.printSnapShot();
		dyn.printWaitQueue();
		dyn.removeJob(2);
		dyn.printSnapShot();
		dyn.printWaitQueue();
	}

	private static void simulateAllocation(FixedMemory fix, ArrayList<Job> jobArray, Algorithm algo) {
		System.out.println("FIXED MEMORY SIMULATION");
		fix.addJob(algo, jobArray.get(0));
		fix.printSnapShot();
		fix.addJob(algo, jobArray.get(1));
		fix.printSnapShot();
		fix.addJob(algo, jobArray.get(2));
		fix.printSnapShot();
		fix.addJob(algo, jobArray.get(3));
		fix.printSnapShot();
		fix.printPartitions();
		fix.removeJob(1);
		fix.printPartitions();
		fix.addJob(algo, jobArray.get(4));
		fix.addJob(algo, jobArray.get(5));
		fix.removeJob(4);
		fix.printWaitQueue();
	}

	private static ArrayList<Job> jobBuilder(){
		System.out.println("DYNAMIC MEMORY SIMULATION");
		ArrayList<Job> jobArray = new ArrayList<Job>();
		jobArray.add(new Job(40));
		jobArray.add(new Job(10));
		jobArray.add(new Job(50));
		jobArray.add(new Job(25));
		jobArray.add(new Job(80));
		jobArray.add(new Job(15));
		jobArray.add(new Job(30));
		return jobArray;
	}
	public enum Algorithm {
		BEST_FIT, FIRST_FIT, WORST_FIT, NEXT_FIT
	}

}
