package memAllocator;

import java.util.concurrent.atomic.AtomicInteger;

public class Job {
	private int size;
	private int id;
	static AtomicInteger nextId = new AtomicInteger();

	public Job(int size){
		this.id = nextId.incrementAndGet();
		this.size = size;
	}
	public long getSize() {
		// TODO Auto-generated method stub
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
