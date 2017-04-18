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
	
	public static void main(String[] args) {
		ArrayDeque<JobRequest> dynJobRequests = jobBuilder();
		ArrayDeque<JobRequest> fixJobRequests = new ArrayDeque<JobRequest>(dynJobRequests);
 		FixedMemory fix = new FixedMemory(MEMORY_SIZE, MEMORY_ADDRESS);
		DynamicMemory dyn = new DynamicMemory(MEMORY_SIZE, MEMORY_ADDRESS);
		
		simulateAllocation(fix, fixJobRequests, Algorithm.BEST_FIT);
		simulateAllocation(dyn, dynJobRequests, Algorithm.BEST_FIT);
		
//		fix = new FixedMemory(MEMORY_SIZE, MEMORY_ADDRESS);
//		dyn = new DynamicMemory(MEMORY_SIZE, MEMORY_ADDRESS);
//		simulateAllocation(fix, jobRequests, Algorithm.FIRST_FIT);
//		simulateAllocation(dyn, jobRequests, Algorithm.FIRST_FIT);
	}

	private static void simulateAllocation(DynamicMemory dyn, ArrayDeque<JobRequest> jobRequests, Algorithm algo) {
		System.out.println();
		System.out.println("DYNAMIC MEMORY SIMULATION");
		for (int i = 0; i < jobRequests.size(); i++) {
			dyn.addJob(algo, jobRequests.poll().job);
			if(ThreadLocalRandom.current().nextDouble() > .75){
				dyn.removeJob();
				dyn.printAll();
			}
			dyn.printAll();
		}
		while(!dyn.isEmpty()){
			dyn.removeJob();
			dyn.printAll();
		}
	}

	private static void simulateAllocation(FixedMemory fix, ArrayDeque<JobRequest> jobRequests, Algorithm algo) {
		System.out.println("FIXED MEMORY SIMULATION");
		for (int i = 0; i < jobRequests.size(); i++) {
			fix.addJob(algo, jobRequests.poll().job);
			if(ThreadLocalRandom.current().nextDouble() > .75){
				fix.removeJob();
				fix.printAll();
			}
			fix.printAll();
		}
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
