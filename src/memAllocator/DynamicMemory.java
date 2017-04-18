package memAllocator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import memAllocator.MemoryTester.Algorithm;

/**
 * @author Ryan Ceresani
 *
 */
public class DynamicMemory{

	private long totalSize;
	private TreeMap<Long, Partition> partitions;
	private ArrayDeque<Job> waitJobs;
	private long address;
	private HashMap<Integer, Partition> jobMap;
	public TreeMap<Long, Partition> freeList;
	private long lastAllocated;

	/**
	 * Dynamic Memory module constructor
	 * @param size
	 * @param memAddress
	 */
	public DynamicMemory(long size, long memAddress) {
		totalSize = size;
		this.address = memAddress;
		partitions = new TreeMap<Long, Partition>();
		waitJobs = new ArrayDeque<Job>();
		jobMap = new HashMap<Integer, Partition>();
		freeList = new TreeMap<Long, Partition>();
		initPartition(partitions);
		lastAllocated = 0;
	}

	/**
	 * Initializes the first partition (which is the whole memory)
	 * @param partitions
	 */
	private void initPartition(TreeMap<Long, Partition> partitions) {
		Partition start = new Partition(totalSize, address);
		this.partitions.put(address, start);
		freeList.put(start.getMemAddress(), start);
	}

	public void printAll(){
		printFreeList();
		printSnapShot();
		printWaitQueue();
		
	}
	
	public void printPartitions(){
		Iterator<Entry<Long, Partition>> it = partitions.entrySet().iterator();
		while(it.hasNext()){
			System.out.println(it.next());
		}	
		System.out.println(jobMap.toString());
	}

	public void printFreeList(){
		System.out.println();
		String leftAlignFormat = "| %-18d | %-13d |%n";
		System.out.println("+Dynamic Free List");
		System.out.format("+--------------------+---------------+%n");
		System.out.format("| Beggining Address  | Block Size    |%n");
		System.out.format("+--------------------+---------------+%n");
		Iterator<Entry<Long, Partition>> it = freeList.entrySet().iterator();
		while(it.hasNext()){
			Partition p = it.next().getValue();
			System.out.format(leftAlignFormat, p.getMemAddress(), p.getSize());
		}
		System.out.format("+--------------------+---------------+%n");
	}

	public void printSnapShot(){
		System.out.println();
		String leftAlignFormat = "| %-20s | %-5s %n";
		String endFormat = " %28s ";
		System.out.println("+Snapshot Dynamic Partition");
		System.out.format("+----------------------+%n");
		System.out.format("| Memory Stack         |%n");
		System.out.format("+----------------------+%n");
		Iterator<Entry<Long, Partition>> it = partitions.entrySet().iterator();
		while(it.hasNext()){
			Partition p = it.next().getValue();
			if(p.isFree()){
				long breaks = p.getSize()/10;
				System.out.format(leftAlignFormat, "", p.getMemAddress() + "k");
				for (int i = 0; i < breaks-1; i++) {
					System.out.format(leftAlignFormat, "","");
				}
				System.out.format("+----------------------+%n");
			}
			else{
				long jobBreaks = p.getCurrentJob().getSize()/10;
				System.out.format(leftAlignFormat, "Job " + p.getCurrentJob().getId() + " = " + p.getSize() + "k", p.getMemAddress() + "k");
				for (int i = 0; i < jobBreaks - 1; i++) {
					System.out.format(leftAlignFormat, "","");
				}
				System.out.format("+----------------------+%n");
			}
		}
		System.out.format(endFormat, totalSize + "k");
	}

	public void printWaitQueue(){
		if(waitJobs.isEmpty()){
			System.out.println();
			System.out.println("No jobs currently waiting.");
		} else { 
			System.out.println();
			String leftAlignFormat = "| %-20s |%n";
			System.out.println("+Job Waiting Queue");
			System.out.format("+----------------------+%n");
			System.out.format("| Job                  |%n");
			System.out.format("+----------------------+%n");
			for(Job j : waitJobs){
				System.out.format(leftAlignFormat, "Job " + j.getId() + " - " + (j.getSize()) + "k");
			}
			System.out.format("+----------------------+%n");
		}
	}

	/**
	 * Determines which allocation algorith to use based on algorithm enum
	 * @param algorithmID
	 * @param newJob
	 */
	public void addJob(Algorithm algorithmID, Job newJob){
		System.out.println("\n***ADD JOB " + newJob.getId());
		if(algorithmID == Algorithm.BEST_FIT){
			addBestFit(newJob);
		}
		else if(algorithmID == Algorithm.FIRST_FIT){
			addFirstFit(newJob);
		}
		else if(algorithmID == Algorithm.WORST_FIT){
			addWorstFit(newJob);
		}
		else if(algorithmID == Algorithm.NEXT_FIT){
			addNextFit(newJob);
		}
	}

	/**
	 * BestFit Memory Allocation for Dynamic Memory
	 * Finds closest matchin place to the incoming job (if any) and assigns it there.
	 * @param newJob
	 */
	private void addBestFit(Job newJob){
		Long bestFit = null;
		Partition bestFitPart = null;
		//Iterate through list of free spaces
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
				long testFit = currPart.getSize() - newJob.getSize();
				//Check if there is no fit yet, if the fit is perfect, or its smaller than current best fit
				if(bestFit == null || testFit == 0 || testFit < bestFit) {
					bestFitPart = currPart;
					bestFit = testFit;
				}
			}

		}
		//If no fit was found, job goes to wait queue
		if(bestFitPart == null){
			waitJobs.add(newJob);
		}
		//Fit was found
		else {
			//Designate a partition exactly the size of the job and add a blank partition immediately afterwads in the blank space
			allocate(bestFitPart, newJob);
			//Map job to partition it is in for easy access later
			jobMap.put(newJob.getId(), bestFitPart);
			//Set the partitions current job
			bestFitPart.setCurrentJob(newJob);
			//remove the location from the free list
			freeList.remove(bestFitPart.getMemAddress());
		}
	}

	/**
	 * First Fit Memory Allocation for Dynamic Memory
	 * Finds first matching place for the incoming job (if any) and assigns it there.
	 * @param newJob
	 */
	private void addFirstFit(Job newJob){
		//Iterate through list of free spaces
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
				//Designate a partition exactly the size of the job and add a blank partition immediately afterwads in the blank space
				allocate(currPart, newJob);
				//Set the partitions current job
				currPart.setCurrentJob(newJob);
				//Map job to partition it is in for easy access later
				jobMap.put(newJob.getId(), currPart);
				//remove the location from the free list
				freeList.remove(currPart.getMemAddress());
				//Match found, we can immediately exit the method
				return;
			}
		}
		waitJobs.add(newJob);
	}

	/**
	 * Same as Best Fit but looks for one that leaves most fragmentation
	 * @param newJob
	 */
	private void addWorstFit(Job newJob){
		Long worstFit = null;
		Partition worstFitPart = null;
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
				long testFit = currPart.getSize() - newJob.getSize();
				if(worstFit == null || testFit == 0 || testFit > worstFit) {
					worstFitPart = currPart;
					worstFit = testFit;
				}
			}
		}
		if(worstFitPart == null){
			waitJobs.add(newJob);
		}
		else {
			allocate(worstFitPart, newJob);
			jobMap.put(newJob.getId(), worstFitPart);
			worstFitPart.setCurrentJob(newJob);
			freeList.remove(worstFitPart.getMemAddress());
		}
	}

	/**
	 * Same as First Fit but starts at the most recently allocated partition and works around
	 * @param newJob
	 */
	private void addNextFit(Job newJob){
		//Iterate to the end of the Free Space list, starting at the desired location.
		Iterator<Map.Entry<Long,Partition>> iter = freeList.tailMap(lastAllocated).entrySet().iterator();
		while (iter.hasNext()){
			Map.Entry<Long, Partition> entry = iter.next();
			Partition currPart = entry.getValue();
			if(currPart.isFree()){
				if(currPart.canFit(newJob)){
					allocate(currPart, newJob);
					currPart.setCurrentJob(newJob);
					jobMap.put(newJob.getId(), currPart);
					freeList.remove(currPart.getMemAddress());
					return;
				}
			}
		}
		//If nothing found to the end, go from the beginning to the original space to complete full traversal
		iter = freeList.headMap(lastAllocated).entrySet().iterator();
		while (iter.hasNext()){
			Map.Entry<Long, Partition>entry = iter.next();
			Partition currPart = entry.getValue();
			if(currPart.isFree()){
				if(currPart.canFit(newJob)){
					allocate(currPart, newJob);
					currPart.setCurrentJob(newJob);
					jobMap.put(newJob.getId(), currPart);
					freeList.remove(currPart.getMemAddress());
					return;
				}
			}
		}
		waitJobs.add(newJob);
	}	

	/**
	 * Splits the partition into two
	 * One partition contains the job
	 * The other contains remaining free space
	 * @param bestFitPart
	 * @param newJob
	 */
	private void allocate(Partition bestFitPart, Job newJob) {
		long newMemLoc = bestFitPart.getMemAddress() + newJob.getSize();
		Partition newPart = new Partition(bestFitPart.getSize() - newJob.getSize(), newMemLoc);
		bestFitPart.setSize(newJob.getSize());
		partitions.put(newMemLoc, newPart);
		freeList.put(newMemLoc, newPart);
	}


	/**
	 * Simulates a job completing
	 * Removes it from the active job map, sets the partitions job to null, 
	 * deallocates the memory, updates the free list,
	 *  and then checks to see if anything in the Wait Queue can be assigned
	 * @param id
	 */
	public void removeJob(){
		System.out.println("\n***REMOVE JOB");
		List<Integer> keysAsArray = new ArrayList<Integer>(jobMap.keySet());
		int rand = ThreadLocalRandom.current().nextInt(0, keysAsArray.size());
		int id = keysAsArray.get(rand);
		try {
			Partition p = jobMap.get(id);
			jobMap.remove(id);
			p.setCurrentJob(null);
			deallocate(p);
			checkWaitQueue();
		} catch (NullPointerException e) {
			System.out.println("Job with ID " + id + "  was not found. No job was removed.");
		}
	}

	/**
	 * Follows standard dynamic deallocation for Isolated, join two, and join three.
	 * Checks if the neighbors are free and then does the proper joining based on that
	 * @param p
	 */
	private void deallocate(Partition p) {
		int freeNeighbors = 0;
		Partition higher = null;
		Partition lower = null;
		//If the partition immediately after this one is free
		try {
			higher = partitions.higherEntry(p.getMemAddress()).getValue();
			if(higher.isFree()){
				freeNeighbors++;
			}
			//If this is last entry in list, we move on
		} catch (NullPointerException e) {
		}
		//If the partition immediately before this one is free
		try {
			lower = partitions.lowerEntry(p.getMemAddress()).getValue();
			if(lower.isFree()){
				freeNeighbors++;
			}
			//If this is first entry in list, we move on
		} catch (NullPointerException e) {	
		}

		//Isolated memory, just gets added to free list on its own
		if(freeNeighbors == 0){
			freeList.put(p.getMemAddress(), p);
		}

		//Joins all three empty partitions together
		else if(freeNeighbors == 2){
			partitions.remove(p.getMemAddress());
			partitions.remove(higher.getMemAddress());
			lower.setSize(lower.getSize() + p.getSize() + higher.getSize());
			freeList.remove(higher.getMemAddress());
		}

		//checks which of the two adjacent memory slots is open and joins with that
		else{
			if(!higher.equals(null) && higher.isFree()){
				partitions.remove(higher.getMemAddress());
				freeList.remove(higher.getMemAddress());
				p.setSize(p.getSize() + higher.getSize());
				freeList.put(p.getMemAddress(), p);
			}
			else{
				partitions.remove(p.getMemAddress());
				lower.setSize(p.getSize() + lower.getSize());
			}
		}
	}

	/**
	 * called after memory is deallocated
	 * Since the memory state has changed, it iterates through the waiting jobs to see if any can now fit.
	 * If they can it adds them to the spot using First Fit
	 */
	private void checkWaitQueue() {
		Iterator<Job> it = waitJobs.iterator();
		while(it.hasNext()){
			Job j = it.next();
			for (Long address : freeList.keySet()) {
				Partition currPart = freeList.get(address);
				if(currPart.canFit(j)){
					addFirstFit(j);
					it.remove();
					return;
				}
			}
		}
	}	
}

