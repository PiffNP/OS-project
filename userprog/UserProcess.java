package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.PriorityQueue;
/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    	fileSystemUtils = new FileSystemUtils();
    	
    	// add stdin and stdout to file table
    	fileTable = new OpenFile[maxFile];//by requirement at most 16 file
    	fileTable[0] = UserKernel.console.openForReading();
    	fileSystemUtils.addFileRef(fileTable[0].getName());
    	fileTable[1] = UserKernel.console.openForWriting();
    	fileSystemUtils.addFileRef(fileTable[1].getName());
    	//assign process ID
    	processID = ProcessIdentity.getProcessID();
    	childs = new HashMap<Integer, UserProcess>();
    	childExits = new HashMap<Integer, Integer>();
    	ProcessIdentity.increaseAliveProcessNumber();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
    	if (!load(name, args))
    		return false;
    	Lib.assertTrue(thread == null);
    	thread = new UThread(this);
    	thread.setName(name);
    	System.out.println("start run");
    	thread.fork();
    	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
    	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
    	Lib.assertTrue(maxLength >= 0);

    	byte[] bytes = new byte[maxLength+1];

    	int bytesRead = readVirtualMemory(vaddr, bytes);

    	for (int length = 0; length < bytesRead; length++) {
    		if (bytes[length] == 0)
    			return new String(bytes, 0, length);
    	}

    	return null;
    }

	public Integer readVirtualMemoryInt(int vaddr) {
		byte[] bytes = new byte[4];
		int bytesRead = readVirtualMemory(vaddr, bytes);
		if(bytesRead != 4) return null;
		return new Integer(Lib.bytesToInt(bytes,0));
	}
	
	public boolean writeVirtualMemoryInt(int vaddr, int value) {
		byte[] bytes = Lib.bytesFromInt(value);
		int bytesWrite = writeVirtualMemory(vaddr, bytes);
		if(bytesWrite != 4) return false;
		return true;
	}
	
    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */

	 public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//System.out.println("read vm vaddr="+vaddr+" offset="+offset+" length="+length);
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
	
		byte[] memory = Machine.processor().getMemory();
		
		int lastVPN = -1;
		TranslationEntry entry = null;
		int amount = 0;
		for(int i = 0; i < length;i++){
			int currentVaddr = vaddr + i;
			int vpn = Processor.pageFromAddress(currentVaddr);
			int vpo = Processor.offsetFromAddress(currentVaddr);
			if(vpn != lastVPN){//switch physical page
				entry = translate(vpn);
				if(entry == null){
					//we cannot find such page
					//System.out.println("EOF, amount="+amount);
					return amount;
				}
				lastVPN = vpn;
			}
			data[offset + i] = memory[entry.ppn * pageSize + vpo];
			amount++;
		}
		//System.out.println("full length, amount="+amount);
		return amount;
    }
     /*
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	System.out.println("read virtual memory");
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);

	return amount;
    }
    
	*/
    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
    	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    /*
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);

	return amount;
    }
    */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	
		byte[] memory = Machine.processor().getMemory();
		
		int lastVPN = -1;
		TranslationEntry entry = null;
		int amount = 0;
		for(int i = 0; i < length; i++){
			int currentVaddr = vaddr + i;
			int vpn = Processor.pageFromAddress(currentVaddr);
			int vpo = Processor.offsetFromAddress(currentVaddr);
			if(vpn != lastVPN){//switch physical page
				entry = translate(vpn);
				if(entry == null || entry.readOnly){
					//we cannot find such page
					return amount;
				}
				lastVPN = vpn;
			}
			memory[entry.ppn * pageSize + vpo] = data[offset + i];
			amount += 1;
		}
		return amount;
    }
    
    /* given vpn, return ppn if found, or null if fail */
    private TranslationEntry translate(int vpn){
    	for(int i = 0; i < numPages; i++){
    		TranslationEntry entry = pageTable[i];
    		if(!entry.valid) continue;
    		if(entry.vpn == vpn){
    			//System.out.println("translate from vpn="+vpn+" to ppn="+entry.ppn);
    			return entry;
    		}
    	}
    	return null;
    }
    

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
    	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

    	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
    	if (executable == null) {
    		Lib.debug(dbgProcess, "\topen failed");
    		return false;
    	}

    	try {
    		coff = new Coff(executable);
    	}
    	catch (EOFException e) {
    		executable.close();
    		Lib.debug(dbgProcess, "\tcoff load failed");
    		return false;
    	}

    	// make sure the sections are contiguous and start at page 0
    	numPages = 0;
    	for (int s=0; s<coff.getNumSections(); s++) {
    		CoffSection section = coff.getSection(s);
    		if (section.getFirstVPN() != numPages) {
    			coff.close();
    			Lib.debug(dbgProcess, "\tfragmented executable");
    			return false;
    		}
    		numPages += section.getLength();
    	}

    	// make sure the argv array will fit in one page
    	byte[][] argv = new byte[args.length][];
    	int argsSize = 0;
    	for (int i=0; i<args.length; i++) {
    		argv[i] = args[i].getBytes();
    		// 4 bytes for argv[] pointer; then string plus one for null byte
    		argsSize += 4 + argv[i].length + 1;
    	}
    	if (argsSize > pageSize) {
    		coff.close();
    		Lib.debug(dbgProcess, "\targuments too long");
    		return false;
    	}

    	// program counter initially points at the program entry point
    	initialPC = coff.getEntryPoint();	

    	// next comes the stack; stack pointer initially points to top of it
    	numPages += stackPages;
    	initialSP = numPages*pageSize;

    	// and finally reserve 1 page for arguments
    	numPages++;
    	//System.out.println("in load numPages="+numPages);
    	if (!loadSections())
    		return false;

    	// store arguments in last page
    	int entryOffset = (numPages-1)*pageSize;
    	int stringOffset = entryOffset + args.length*4;

    	this.argc = args.length;
    	this.argv = entryOffset;

    	for (int i = 0; i < argv.length; i++) {
    		byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
    		Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
    		entryOffset += 4;
    		Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
    				argv[i].length);
    		stringOffset += argv[i].length;
    		Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
    		stringOffset += 1;
    	}

    	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    	if (numPages > Machine.processor().getNumPhysPages()) {
    		coff.close();
    		Lib.debug(dbgProcess, "\tinsufficient physical memory");
    		return false;
    	}
    	int k = 0;
    	Integer[] physicalPage = PhysicalMemoryUtils.getNewPhysicalPage(numPages);
    	if (physicalPage == null) {
    		coff.close();
    		Lib.debug(dbgProcess, "\tinsufficient physical memory");
    		return false;
    	}
    	pageTable = new TranslationEntry[numPages];
    	//System.out.println("nr_page="+numPages+" pageSize="+pageSize);
    	// load sections
    	for (int s = 0; s < coff.getNumSections(); s++) {
    		CoffSection section = coff.getSection(s);

    		for (int i = 0; i < section.getLength(); i++) {
    			int vpn = section.getFirstVPN() + i;
    			//get new physical page
    			Integer ppn = physicalPage[k];
    			//System.out.println("get ppn "+ppn.intValue());
    			if(ppn == null){
    				System.out.println("physical page fail");
    				return false;
    			}
    			pageTable[k] = new TranslationEntry(vpn, ppn.intValue(), true, section.isReadOnly(), false, false);
    			k += 1;
    			section.loadPage(i, ppn.intValue());
    		}
    	}
    	//load stack and argument
    	for(int i = 0; i < stackPages + 1; i++){
    		int vpn = numPages - stackPages - 1 + i;
    		Integer ppn = physicalPage[k];
    		//System.out.println("get ppn "+ppn.intValue());
    		if(ppn == null){
    			System.out.println("physical page fail");
    			return false;
    		}
    		pageTable[k] = new TranslationEntry(vpn, ppn.intValue(), true, false, false, false);
    		k += 1;
    	}
    	Lib.assertTrue(k == numPages);
    	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
		Integer[] entryPPN = new Integer[numPages];
    	for(int i = 0; i < numPages; i++){
    		entryPPN[i] = pageTable[i].ppn;
    	}
		PhysicalMemoryUtils.ReleasePhysicalPage(entryPPN);
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
    	Processor processor = Machine.processor();

    	// by default, everything's 0
    	for (int i=0; i<processor.numUserRegisters; i++)
    		processor.writeRegister(i, 0);

    	// initialize PC and SP according
    	processor.writeRegister(Processor.regPC, initialPC);
    	processor.writeRegister(Processor.regSP, initialSP);

    	// initialize the first two argument registers to argc and argv
    	processor.writeRegister(Processor.regA0, argc);
    	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    	if(processID != 0){
    		return 0;
    	}
    	Machine.halt();
	
    	Lib.assertNotReached("Machine.halt() did not halt machine!");
    	return 0;
    }

    /**
     * Handle creat() & open() system call
     * int creat(char *name);
     * int open(char *name);
     * */
	private int handleCreateOpen(int a0, boolean isCreate) {
		if(!fileSystemUtils.validVirtualAddress(a0)){
    		System.out.println("[create/open]invalid address");
			return -1;
		}
		String name = readVirtualMemoryString(a0, maxBufferSize);
		//System.out.println("name = " + name);
		if(name == null){
			System.out.println("[create/open]read name fail");
			return -1;
		}
		int fileDes;
		OpenFile file;
		if((fileDes = fileSystemUtils.getFileDes()) == -1){
    		System.out.println("[create/open]file table full");
			return -1;
		}
		if(fileSystemUtils.addFileRef(name) == -1){
			System.out.println("[create/open]fd fail");
			return -1;
		}
		if((file = ThreadedKernel.fileSystem.open(name, isCreate)) == null){
			System.out.println("[create/open]open fail");
			//remember to remove file ref here
			fileSystemUtils.removeFileRef(name);
			return -1;
		}
		fileTable[fileDes] = file;
		return fileDes;
	}
	
    /**
     * Handle read() system call
     * int read(int fd, char *buffer, int size);
     * */
	private int handleRead(int a0, int a1, int a2){
		if(!fileSystemUtils.validVirtualAddress(a1)){
    		System.out.println("[read]invalid address");
			return -1;
		}
		if(!fileSystemUtils.validDes(a0)){
			System.out.println("[read]invalid file id");
			return -1;
		}
		if(!fileSystemUtils.validBufferSize(a2)){
			System.out.println("[read]invalid count");
			return -1;
		}
		if(a2 > numPages * pageSize){
			System.out.println("[read] exceed limit");
			return -1;
		}
		//not sure whether need typecast. stdin & stdout ?
		//OpenFileWithPosition file = (OpenFileWithPosition)fileTable[a0];
		byte[] buffer = new byte[a2];
		//int read_res = file.read(buffer, 0, a2);
		int read_res = fileTable[a0].read(buffer, 0, a2);
		if(read_res < 0){
			System.out.println("[read]error reading file");
			return -1;
		}
		int write_res = writeVirtualMemory(a1, buffer, 0, read_res);
		if(write_res != read_res){
			System.out.println("[read]error writing memory");
			return -1;
		}
		return read_res;
	}
	/**
     * Handle write() system call
     * int write(int fd, char *buffer, int size);
     * */
	private int handleWrite(int a0, int a1, int a2){
		if(!fileSystemUtils.validVirtualAddress(a1)){
    		System.out.println("[write]invalid address");
			return -1;
		}
		if(!fileSystemUtils.validDes(a0)){
			System.out.println("[write]invalid file id");
			return -1;
		}
		if(!fileSystemUtils.validBufferSize(a2)){
			System.out.println("[write]invalid count");
			return -1;
		}
		//OpenFile file = fileTable[a0];
		if(a2 > numPages * pageSize){
			System.out.println("[write] exceed limit");
			return -1;
		}
			
		byte[] buffer = new byte[a2];
		int read_res = readVirtualMemory(a1, buffer);
		if(read_res != a2){
			System.out.println("[write]error reading memory");
			return -1;
		}
		
		int write_res = fileTable[a0].write(buffer, 0, a2);
		//need check whether it is correct
		if(write_res != read_res){
			System.out.println("[write]error writing file");
			return -1;
		}
		//int write_res = file.write(buffer,0,a2);
		return write_res;
	}
	
	/**
     * Handle close() system call
     * int close(int fd);
     * */
	private int handleClose(int a0){
		if(!fileSystemUtils.validDes(a0)){
			System.out.println("[close]invalid file id");
			return -1;
		}
		String name = fileTable[a0].getName();
		fileTable[a0].close();
		fileTable[a0] = null;
		return fileSystemUtils.removeFileRef(name);
	}
	
	/**
     * Handle unlink() system call
     * int unlink(char *name);
     * */
	private int handleUnlink(int a0){
		if(!fileSystemUtils.validVirtualAddress(a0)){
    		System.out.println("[unlink]invalid address");
			return -1;
		}
		String name = readVirtualMemoryString(a0, maxBufferSize);
		if(name == null){
			System.out.println("[unlink]read name fail");
			return -1;
		}
		return fileSystemUtils.markToDeleteFile(name);
	}
	
	/**
     * Handle unlink() system call
     * int exec(char *name, int argc, char **argv);
     * */
	private int handleExec(int a0, int a1, int a2){
		if(!fileSystemUtils.validVirtualAddress(a0)){
    		System.out.println("[exec]invalid address");
			return -1;
		}
		String name = readVirtualMemoryString(a0, maxBufferSize);
		if(name == null || !name.endsWith(".coff")){
			System.out.println("[exec]read name fail");
			return -1;
		}
		if(a1 < 0){
			System.out.println("[exec]argc<0");
			return -1;
		}
		String [] args = new String[a1];
		for(int i = 0; i < a1; i++){
			if(!fileSystemUtils.validVirtualAddress(a2 + 4 * i)){
	    		System.out.println("[exec]invalid address");
				return -1;
			}
			Integer arg_adr = readVirtualMemoryInt(a2 + 4 * i);
			if(arg_adr == null){
				System.out.println("[exec]read int fail");
				return -1;
			}
			if(!fileSystemUtils.validVirtualAddress(arg_adr.intValue())){
	    		System.out.println("[exec]invalid address");
				return -1;
			}
			String arg = readVirtualMemoryString(arg_adr.intValue(), maxBufferSize);
			args[i] = arg;
		}
		UserProcess process = UserProcess.newUserProcess();
		process.parent = this;
		boolean result = process.execute(name, args);
		if(result == true){
			this.childs.put(process.processID, process);
			return process.processID;
		} else {
			ProcessIdentity.decreaseAliveProcessNumber();
	    	fileSystemUtils.removeFileRef(UserKernel.console.openForReading().getName());
	    	fileSystemUtils.removeFileRef(UserKernel.console.openForWriting().getName());
			return -1;
		}
	}
	
	/**
     * Handle join() system call
     * join(int pid, int *status);
     * */
	private int handleJoin(int a0, int a1){
		if(!childs.containsKey(a0)){
			System.out.println("[join]no such child");
			return -1;
		}
		if(!childExits.containsKey(a0)){
			childs.get(a0).thread.join();
		}
		Lib.assertTrue(childExits.containsKey(a0));
		childs.remove(a0);
		int ret = 0;
		if(childExits.get(a0) != null){
			//normally exit
			writeVirtualMemoryInt(a1, childExits.get(a0).intValue());
			ret = 1;
		}
		childExits.remove(a0);
		return ret;
	}
	
	/**
     * Handle exit() system call
     * void exit(int status);
     * */
	private int handleExit(Integer a0){
		fileSystemUtils.cleanFileTable();
		unloadSections();//cleanup memory
		if(parent != null){
			Lib.assertTrue(!parent.childExits.containsKey(processID));
			parent.childExits.put(processID, a0);
		}
		for(UserProcess child : childs.values()){
			Lib.assertTrue(child.parent == this);
			child.parent = null;
		}
		ProcessIdentity.decreaseAliveProcessNumber();
		if(ProcessIdentity.noAliveProcess()){
			Kernel.kernel.terminate();
		}else{
			KThread.finish();
		}
		Lib.assertNotReached();
		return 0;
	}
	
    private static final int syscallHalt = 0;
    private static final int syscallExit = 1;
    private static final int syscallExec = 2;
    private static final int syscallJoin = 3;
    private static final int syscallCreate = 4;
    private static final int syscallOpen = 5;
    private static final int syscallRead = 6;
    private static final int syscallWrite = 7;
    private static final int syscallClose = 8;
    private static final int syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    	//System.out.println("system call "+syscall);
    	switch (syscall) {
    	case syscallHalt:
    		return handleHalt();
    		//EDIT HERE
    	case syscallCreate:
    		return handleCreateOpen(a0, true);
    	case syscallOpen:
    		return handleCreateOpen(a0, false);
    	case syscallRead:
    		return handleRead(a0, a1, a2);
    	case syscallWrite:
    		return handleWrite(a0, a1, a2);
    	case syscallClose:
    		return handleClose(a0);
    	case syscallUnlink:
    		return handleUnlink(a0);
    	case syscallExec:
    		return handleExec(a0, a1, a2);
    	case syscallJoin:
    		return handleJoin(a0, a1);
    	case syscallExit:
    		return handleExit(a0);
    	default:
    		Lib.debug(dbgProcess, "Unknown syscall " + syscall);
    		Lib.assertNotReached("Unknown system call!");
    	}
    	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
    	Processor processor = Machine.processor();

    	switch (cause) {
    	case Processor.exceptionSyscall:
    		int result = handleSyscall(processor.readRegister(Processor.regV0),
    				processor.readRegister(Processor.regA0),
    				processor.readRegister(Processor.regA1),
    				processor.readRegister(Processor.regA2),
    				processor.readRegister(Processor.regA3)
    				);
    		processor.writeRegister(Processor.regV0, result);
    		processor.advancePC();
    		break;				       

    	default:
    		Lib.debug(dbgProcess, "Unexpected exception: " +
    				Processor.exceptionNames[cause]);

    		handleExit(null);
    		
    		Lib.assertNotReached("Unexpected exception");
    	}
    }

    private static Lock ProcessIDLock = new Lock();
    protected int processID;
    private static int nextProcessID = 0;
    private static int aliveProcessNumber = 0;
    protected static class ProcessIdentity{
    	private static int getProcessID(){
    		ProcessIDLock.acquire();
    		int ret = nextProcessID++;
    		ProcessIDLock.release();
    		return ret;
    	}
    	private static boolean noAliveProcess(){
    		ProcessIDLock.acquire();
    		int ret = aliveProcessNumber;
    		ProcessIDLock.release();
    		return (ret == 0);
    	}
    	private static void increaseAliveProcessNumber(){
    		ProcessIDLock.acquire();
    		aliveProcessNumber++;
    		ProcessIDLock.release();
    	}
    	private static void decreaseAliveProcessNumber(){
    		ProcessIDLock.acquire();
    		Lib.assertTrue(aliveProcessNumber > 0);
    		aliveProcessNumber--;
    		ProcessIDLock.release();
    	}
    }
    
    protected FileSystemUtils fileSystemUtils;
    private static Lock FileLock = new Lock();
	/** The program's file descriptors */
    protected OpenFile[] fileTable;
    private static HashMap<String, Integer> fileRefCounter = new HashMap<String, Integer>();
    private static final int maxFile = 16;
    protected class FileSystemUtils{
    	int getFileDes(){
    		for(int i = 0; i < maxFile; i++){
    			if(fileTable[i] == null){
    				return i;
    			}
    		}
    		return -1;
    	}
    	
    	void cleanFileTable(){
    		for(int i = 0; i < maxFile; i++)
    			if(fileTable[i] != null)
    				handleClose(i);
    	}
    	
    	boolean validVirtualAddress(int addr) {
    		int vpn = Processor.pageFromAddress(addr);
    		return (vpn < numPages && vpn >= 0);
    	}
    	
    	boolean validDes(int des){
    		return (!(des < 0 || des >= maxFile || fileTable[des] == null));
    	}
    	
    	boolean validBufferSize(int count){
    		return (count > 0);
    	}
    	
    	int addFileRef(String name){
    		int ret = 0;
    		FileLock.acquire();
    		if(fileRefCounter.containsKey(name)){
    			int cnt;
    			/** less than 0 if marked to be deleted*/
    			if((cnt = fileRefCounter.get(name).intValue()) < 0)
    				ret = -1;
    			else
    				fileRefCounter.replace(name, cnt + 1);
    		} else
    			fileRefCounter.put(name, 1);
    		FileLock.release();
    		return ret;
    	}
    	
    	int removeFileRef(String name){
    		int ret = 0;
    		FileLock.acquire();
    		if(fileRefCounter.containsKey(name)){
    			int cnt;
    			boolean flag = false;
    			if((cnt = fileRefCounter.get(name).intValue()) > 0)
    				cnt--;
    			else{
    				cnt++;
    				flag = true;
    			}
    			if(cnt == 0){
    				fileRefCounter.remove(name);
    				if(flag){
    					if(!ThreadedKernel.fileSystem.remove(name))
    						ret = -1;
    				}
    			}
    			else
    				fileRefCounter.replace(name, cnt);
    		} else
    			ret = -1;
    		FileLock.release();
    		return ret;
    	}
    	
    	int markToDeleteFile(String name){
    		int ret = 0;
    		FileLock.acquire();
    		if(fileRefCounter.containsKey(name)){
    			int cnt;
    			if((cnt = fileRefCounter.get(name).intValue()) > 0){
    				fileRefCounter.replace(name, -cnt);
    			} else{
    				if(!ThreadedKernel.fileSystem.remove(name))
    					ret = -1;
    			}
    		} else{
				if(!ThreadedKernel.fileSystem.remove(name))
					ret = -1;
    		}
    		FileLock.release();
    		return ret;
    	}
    }
    
    private static Lock PMLock = new Lock();    
    private static final int physicalPageSize = Machine.processor().getNumPhysPages();
    private static PriorityQueue<Integer> physicalPageStatus;
    static {
    	physicalPageStatus = new PriorityQueue<Integer>();
        for(int i = 0; i < physicalPageSize; i++)
        	physicalPageStatus.add(new Integer(i));
    }  
    protected static class PhysicalMemoryUtils{
    	private static Integer getNewPhysicalPage(){
    		PMLock.acquire();
    		Integer ret = physicalPageStatus.poll();
    		PMLock.release();
    		return ret;
    	}
    	private static Integer[] getNewPhysicalPage(int num){
    		PMLock.acquire();
    		Integer[] ret = null;
    		if(physicalPageStatus.size() >= num){
    			ret = new Integer[num];
    			for(int i = 0; i < num; i++)
    				ret[i] = physicalPageStatus.poll();
    		}
    		PMLock.release();
    		return ret;
    	}
    	private static void ReleasePhysicalPage(int ppn){
    		PMLock.acquire();
    		Lib.assertTrue(0 <= ppn && ppn < physicalPageSize);
    		Lib.assertTrue(!physicalPageStatus.contains(ppn));
    		physicalPageStatus.add(new Integer(ppn));
    		PMLock.release();
    	}
    	private static void ReleasePhysicalPage(Integer[] ppn){
    		PMLock.acquire();
    		for(int i = 0; i < ppn.length; i++){
    			Lib.assertTrue(0 <= ppn[i] && ppn[i] < physicalPageSize);
    			Lib.assertTrue(!physicalPageStatus.contains(ppn[i]));
    			physicalPageStatus.add(new Integer(ppn[i]));
    		}
    		PMLock.release();
    	}
    }
    
	
	protected UThread thread;
	protected UserProcess parent;
	protected Map<Integer,UserProcess> childs;	
	protected Map<Integer,Integer> childExits;//if exit normally 0, abnormally -1

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    private static final int maxBufferSize = 256;
    
    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
