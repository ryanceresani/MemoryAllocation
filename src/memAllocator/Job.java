package memAllocator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ryan Ceresani
 *
 *Inner job class to keep track of Job information for memory allocator
 */
public class Job {
	private int size;
	private int id;
	static AtomicInteger nextId = new AtomicInteger();

	public Job(int size){
		this.id = nextId.incrementAndGet();
		this.size = size;
	}
	public long getSize() {
		return size;
	}
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "Job " + id;
	}
}
