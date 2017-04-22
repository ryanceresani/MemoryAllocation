package memAllocator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

public class MemoryTester {

	static final long MEMORY_SIZE = 200;
	static final long MEMORY_ADDRESS = 0;

	//Maximum size of a randomly generated job
	static int MAX_JOB_SIZE;
	//Minimum size of a randomly generated job
	static int MIN_JOB_SIZE;
	//How long the sequence of add or remove jobs will be
	static int SEQUENCE_LENGTH;
	//Probability of removing a job every cycle
	static double REMOVE_CHANCE;

	public static void main(String[] args) throws IOException {

		PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
		System.setOut(out);

		Algorithm algo = algoParser(args[2]);
		//Create random job request sequence
		ArrayDeque<JobRequest> dynJobRequests = jobBuilder(args[0]);
		//Copy queue for use in fixed memory
		ArrayDeque<JobRequest> fixJobRequests = new ArrayDeque<JobRequest>(dynJobRequests);
		FixedMemory fix = new FixedMemory(MEMORY_SIZE, MEMORY_ADDRESS, args[1]);
		DynamicMemory dyn = new DynamicMemory(MEMORY_SIZE, MEMORY_ADDRESS);

		//Run the simulations with the given algorithm
		simulateAllocation(fix, fixJobRequests, algo);
		simulateAllocation(dyn, dynJobRequests, algo);

	}

	private static Algorithm algoParser(String string) {
		Algorithm algo = Algorithm.valueOf(string);
		return algo;
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

	private static ArrayDeque<JobRequest> jobBuilder(String fileName) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		String input = in.readLine();
		String[] tokens = input.split(" ");
		in.close();

		SEQUENCE_LENGTH = Integer.parseInt(tokens[0]);
		MIN_JOB_SIZE = Integer.parseInt(tokens[1]);
		MAX_JOB_SIZE = Integer.parseInt(tokens[2]);
		REMOVE_CHANCE = (Integer.parseInt(tokens[3]) / 100);

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
		BEST_FIT("BEST"), FIRST_FIT("FIRST"), WORST_FIT("WORST"), NEXT_FIT("NEXT");

		private String text;

		Algorithm(String text) {
			this.text = text.toUpperCase();
		}
	}
}

