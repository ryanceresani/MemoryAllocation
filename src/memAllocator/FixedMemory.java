package memAllocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import memAllocator.MemoryTester.Algorithm;

/**
 * @author Ryan Ceresani
 *
 */
public class FixedMemory{

	private static final String CONFIG_NAME = "FixedPartitionConfig";
	private long totalSize;
	private LinkedList<Partition> partitions;
	private ArrayDeque<Job> waitJobs;
	private long address;
	private HashMap<Integer, Partition> jobMap;
	private TreeMap<Long, Partition> freeList;
	private long lastAlotted;

	public FixedMemory(long size, long memAddress){
		totalSize = size;
		this.address = memAddress;
		partitions = new LinkedList<Partition>();
		waitJobs = new ArrayDeque<Job>();
		jobMap = new HashMap<Integer, Partition>();
		freeList = new TreeMap<Long, Partition>();
		loadPartitions();
		lastAlotted = 0;
	}

	/**
	 * Imports the partition sizes from a config file. 
	 * File will just be a text file with partition sizes.
	 */
	private void loadPartitions(){
		long currentMemAddress = address;
		try {
			File file = new File(CONFIG_NAME);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while((line = br.readLine()) != null){
				long size = Long.parseLong(line);
				Partition p = new Partition(size, currentMemAddress);
				partitions.add(p);
				freeList.put(p.getMemAddress(), p);
				currentMemAddress += size;
			} 
		} catch (IOException e) {
			// TODO: handle exception
			System.out.println("IO Exception: ");
			e.printStackTrace();
		}
	}

	public void printAll(){
		printPartitions();
		printSnapShot();
		printWaitQueue();
		
	}
	
	public void printPartitions(){
		String leftAlignFormat = "| %-15d | %-13d | %-8s | %-7s |%n";
		System.out.println("+Fixed Partition Status");
		System.out.format("+-----------------+---------------+----------+---------+%n");
		System.out.format("| Partition Size  | Mem Address   | Access  | Status |%n");
		System.out.format("+-----------------+---------------+----------+---------+%n");
		Iterator<Partition> it = partitions.iterator();
		while(it.hasNext()){
			Partition p = it.next();
			System.out.format(leftAlignFormat, p.getSize(), p.getMemAddress(), p.getCurrentJob(), p.getStatus());
		}
		System.out.format("+-----------------+---------------+----------+---------+%n");
	}

	public void printSnapShot(){
		System.out.println();
		String leftAlignFormat = "| %-20s |%n";
		System.out.println("+Snapshot Fixed Partition");
		System.out.format("+----------------------+%n");
		System.out.format("| Memory Stack         |%n");
		System.out.format("+----------------------+%n");
		Iterator<Partition> it = partitions.iterator();
		while(it.hasNext()){
			Partition p = it.next();
			if(p.isFree()){
				long breaks = p.getSize()/10;
				System.out.format(leftAlignFormat, "Partition " + (partitions.indexOf(p)+1) + " = " + p.getSize() + "k");
				System.out.format(leftAlignFormat, "Status: " + p.getStatus());
				for (int i = 0; i < breaks; i++) {
					System.out.format(leftAlignFormat, "","%n");
				}
				System.out.format("+----------------------+%n");
			}
			else{
				long jobBreaks = p.getCurrentJob().getSize()/10;
				long breaks = p.getSize()/10 - jobBreaks;
				System.out.format(leftAlignFormat, "Job " + p.getCurrentJob().getId() + " = " + p.getSize() + "k");
				for (int i = 0; i < jobBreaks; i++) {
					System.out.format(leftAlignFormat, "","%n");
				}
				System.out.format("+......................+%n");
				if(p.getFragmentation() > 0){
					System.out.format(leftAlignFormat, "Partition " + (partitions.indexOf(p)+1));;
					System.out.format(leftAlignFormat, " has " + p.getFragmentation() + "k frag");
				}
				for (int i = 0; i < breaks; i++) {
					System.out.format(leftAlignFormat, "","%n");
				}
				System.out.format("+----------------------+%n");
			}
		}

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
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
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
		Iterator<Map.Entry<Long,Partition>> iter = freeList.tailMap(lastAlotted).entrySet().iterator();
		while (iter.hasNext()){
			Map.Entry<Long, Partition> entry = iter.next();
			Partition currPart = entry.getValue();
			if(currPart.isFree()){
				if(currPart.canFit(newJob)){
					currPart.setCurrentJob(newJob);
					jobMap.put(newJob.getId(), currPart);
					freeList.remove(currPart.getMemAddress());
					return;
				}
			}
		}
		//If nothing found to the end, go from the beginning to the original space to complete full traversal
		iter = freeList.headMap(lastAlotted).entrySet().iterator();
		while (iter.hasNext()){
			Map.Entry<Long, Partition>entry = iter.next();
			Partition currPart = entry.getValue();
			if(currPart.isFree()){
				if(currPart.canFit(newJob)){
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
	 * Simulates a job completing
	 * Removes it from the active job map, sets the partitions job to null, 
	 * updates the free list, and then checks to see if anything in the Wait Queue can be assigned
	 * @param id
	 */
	public void removeJob(int id){
		try{
			Partition p = jobMap.get(id);
			jobMap.remove(id);
			p.setCurrentJob(null);
			freeList.put(p.getMemAddress(), p);
			checkWaitQueue();
		} catch (NullPointerException e) {
			System.out.println("Job with ID " + id + "  was not found.");
			System.out.println("No job was removed.");
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
				}
			}
		}

	}
}
