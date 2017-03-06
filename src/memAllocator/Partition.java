package memAllocator;

public class Partition {
	private long size;
	private boolean free;
	private long address;
	private Job job;

	public Partition(long size, long memAddress){
		this.setSize(size);
		this.setMemAddress(memAddress);
		setFree(true);
		setCurrentJob(null);
	}

	public long getFragmentation() {
		return size - job.getSize();
	}

	public Job getCurrentJob() {
		
		return job;
	}

	public void setCurrentJob(Job currentJob) {
		if(currentJob == null){
			setFree(true);
		}
		else{
			this.job = currentJob;
			setFree(false);
		}
	}

	public long getMemAddress() {
		return address;
	}

	public void setMemAddress(long memAddress) {
		this.address = memAddress;
	}

	public boolean isFree() {
		return free;
	}

	public void setFree(boolean free) {
		this.free = free;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String toString(){
		return "size: " + size + " free: " + free + " address: " + address;

	}

	/**
	 * @param newJob
	 * @return
	 */
	public boolean canFit(Job newJob) {
		if(newJob.getSize() <= size){
			return true;
		}
		return false;
	}

	public String getStatus() {
		if(free) return "Free";
		else return "Busy";
	}

}
