package memAllocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

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
	private HashMap<Long,Partition> freeList;
	private int lastAlotted;

	public FixedMemory(long size, long memAddress){
		totalSize = size;
		this.address = memAddress;
		partitions = new LinkedList<Partition>();
		waitJobs = new ArrayDeque<Job>();
		jobMap = new HashMap<Integer, Partition>();
		freeList = new HashMap<Long, Partition>();
		loadPartitions();
		lastAlotted = 0;
	}

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
				System.out.format(leftAlignFormat, "Partition " + (partitions.indexOf(p)+1));;
				System.out.format(leftAlignFormat, " has " + p.getFragmentation() + "k frag");
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
			jobMap.put(newJob.getId(), bestFitPart);
			bestFitPart.setCurrentJob(newJob);
			freeList.remove(bestFitPart.getMemAddress());
		}
	}

	private void addFirstFit(Job newJob){
		for(Long address : freeList.keySet()){
			Partition currPart = freeList.get(address);
			if(currPart.canFit(newJob)){
				currPart.setCurrentJob(newJob);
				jobMap.put(newJob.getId(), currPart);
				freeList.remove(currPart.getMemAddress());
				return;
			}
		}
		waitJobs.add(newJob);
	}

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

	private void addNextFit(Job newJob){
		for (int i = 0; i < partitions.size(); i++) {
			int pointer = (i + lastAlotted) % partitions.size();
			Partition currPart = partitions.get(pointer);
			if(currPart.isFree()){
				if(currPart.canFit(newJob)){
					currPart.setCurrentJob(newJob);
					jobMap.put(newJob.getId(), currPart);
					freeList.remove(currPart.getMemAddress());
					lastAlotted = pointer;
					return;
				}
			} 
		}
		waitJobs.add(newJob);
	}

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
