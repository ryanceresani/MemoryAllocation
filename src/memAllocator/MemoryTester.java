package memAllocator;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

public class MemoryTester {

	static final long MEMORY_SIZE = 200;
	static final long MEMORY_ADDRESS = 0;
	
	//Maximum size of a randomly generated job
	static final int MAX_JOB_SIZE = 80;
	//Minimum size of a randomly generated job
	static final int MIN_JOB_SIZE = 10;
	//How long the sequence of add or remove jobs will be
	static final int SEQUENCE_LENGTH = 20;
	//Probability of removing a job every cycle
	static final double REMOVE_CHANCE = .25;
	
	public static void main(String[] args) {
		//Create random job request sequence
		ArrayDeque<JobRequest> dynJobRequests = jobBuilder();
		//Copy queue for use in fixed memory
		ArrayDeque<JobRequest> fixJobRequests = new ArrayDeque<JobRequest>(dynJobRequests);
 		FixedMemory fix = new FixedMemory(MEMORY_SIZE, MEMORY_ADDRESS);
		DynamicMemory dyn = new DynamicMemory(MEMORY_SIZE, MEMORY_ADDRESS);
		
		//Run the simulations with the given algorithm
		simulateAllocation(fix, fixJobRequests, Algorithm.BEST_FIT);
		simulateAllocation(dyn, dynJobRequests, Algorithm.BEST_FIT);
		
	}

	private static void simulateAllocation(DynamicMemory dyn, ArrayDeque<JobRequest> jobRequests, Algorithm algo) {
		System.out.println();
		System.out.println("DYNAMIC MEMORY SIMULATION");
		//continue while there is a any jobs left in request sequence
		while(!jobRequests.isEmpty()) {
			dyn.addJob(algo, jobRequests.poll().job);
			//Randomly remove a job with % chance
			if(ThreadLocalRandom.current().nextDouble() > (1-REMOVE_CHANCE)){
				dyn.removeJob();
				dyn.printAll();
			}
			dyn.printAll();
		}
		//Remove jobs until memory is empty
		while(!dyn.isEmpty()){
			dyn.removeJob();
			dyn.printAll();
		}
	}

	private static void simulateAllocation(FixedMemory fix, ArrayDeque<JobRequest> jobRequests, Algorithm algo) {
		System.out.println("FIXED MEMORY SIMULATION");
		//continue while there is a any jobs left in request sequence
		while(!jobRequests.isEmpty()) {
			fix.addJob(algo, jobRequests.poll().job);
			//Randomly remove a job with % chance
			if(ThreadLocalRandom.current().nextDouble() > (1-REMOVE_CHANCE)){
				fix.removeJob();
				fix.printAll();
			}
			fix.printAll();
		}
		//Remove jobs until memory is empty
		while(!fix.isEmpty()){
			fix.removeJob();
			fix.printAll();
		}
	}

	private static ArrayDeque<JobRequest> jobBuilder(){
		ArrayDeque<JobRequest> jobArray = new ArrayDeque<JobRequest>();
		for (int i = 0; i < SEQUENCE_LENGTH; i++) {
			jobArray.add(new JobRequest());
		}
		return jobArray;
	}
	
	private static class JobRequest 
	{
		Job job;
		private JobRequest(){
			int jobSize = ThreadLocalRandom.current().nextInt(MIN_JOB_SIZE, MAX_JOB_SIZE);
			job = new Job(jobSize);
		}
	}
	public enum Algorithm {
		BEST_FIT, FIRST_FIT, WORST_FIT, NEXT_FIT
	}

}
