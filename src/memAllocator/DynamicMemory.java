package memAllocator;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
	 * @param newJob
	 */
	private void addBestFit(Job newJob){
		Long bestFit = null;
		Partition bestFitPart = null;
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
				long testFit = currPart.getSize() - newJob.getSize();
				if(bestFit == null || testFit == 0 || testFit < bestFit) {
					bestFitPart = currPart;
					bestFit = testFit;
				}
			}

		}
		if(bestFitPart == null){
			waitJobs.add(newJob);
		}
		else {
			allocate(bestFitPart, newJob);
			jobMap.put(newJob.getId(), bestFitPart);
			bestFitPart.setCurrentJob(newJob);
			freeList.remove(bestFitPart.getMemAddress());
		}
	}

	/**
	 * @param newJob
	 */
	private void addFirstFit(Job newJob){
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
				allocate(currPart, newJob);
				currPart.setCurrentJob(newJob);
				jobMap.put(newJob.getId(), currPart);
				freeList.remove(currPart.getMemAddress());
				return;
			}
		}
		waitJobs.add(newJob);
	}

	/**
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
	 * @param newJob
	 */
	private void addNextFit(Job newJob){
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
	 * @param id
	 */
	public void removeJob(int id){
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
	 * @param p
	 */
	private void deallocate(Partition p) {
		int freeNeighbors = 0;
		Partition higher = null;
		Partition lower = null;
		try {
			higher = partitions.higherEntry(p.getMemAddress()).getValue();
			if(higher.isFree()){
				freeNeighbors++;
			}
		} catch (NullPointerException e) {
		}

		try {
			lower = partitions.lowerEntry(p.getMemAddress()).getValue();
			if(lower.isFree()){
				freeNeighbors++;
			}	
		} catch (NullPointerException e) {	
		}

		if(freeNeighbors == 0){
			freeList.put(p.getMemAddress(), p);
		}

		else if(freeNeighbors == 2){
			partitions.remove(p.getMemAddress());
			partitions.remove(higher.getMemAddress());
			lower.setSize(lower.getSize() + p.getSize() + higher.getSize());
			freeList.remove(higher.getMemAddress());
		}

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
	 * 
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
				}
			}
		}
	}	
}

