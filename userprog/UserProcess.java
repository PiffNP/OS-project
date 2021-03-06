package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    // we need to use dynamic way
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
	
	// add stdin and stdout to file table
	fileTable=new OpenFile[maxFile];//by requirement at most 16 file
	fileTable[0]=UserKernel.console.openForReading();
	fileTable[1]=UserKernel.console.openForWriting();
	//assign process ID
	processID=getProcessID();
	childs=new HashMap<Integer,UserProcess>();
	childExits=new HashMap<Integer,Integer>();
	increaseAliveProcessNumber();
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
	Lib.assertTrue(thread==null);
	thread=new UThread(this);
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

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

	public Integer readVirtualMemoryInt(int vaddr) {
		byte[] bytes= new byte[4];
		int bytesRead=readVirtualMemory(vaddr,bytes);
		if(bytesRead!=4) return null;
		return new Integer(Lib.bytesToInt(bytes,0));
	}
	public boolean writeVirtualMemoryInt(int vaddr,int value) {
		byte[] bytes= Lib.bytesFromInt(value);
		int bytesWrite=writeVirtualMemory(vaddr,bytes);
		if(bytesWrite!=4) return false;
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
	//Below are modified version, to be tested later
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//System.out.println("read vm vaddr="+vaddr+" offset="+offset+" length="+length);
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	
		byte[] memory = Machine.processor().getMemory();
		
		int lastVPN=-1;
		TranslationEntry entry=null;
		int amount=0;
		for(int i=0;i<length;i++){
			int currentVaddr=vaddr+i;
			int vpn=getVPN(currentVaddr);
			int vpo=getVPO(currentVaddr);
			if(vpn!=lastVPN){//switch physical page
				entry=translate(vpn);
				if(entry==null){
					//we cannot find such page
					//System.out.println("EOF, amount="+amount);
					return amount;
				}
				lastVPN=vpn;
			}
			data[offset+i]=memory[entry.ppn*pageSize+vpo];
			amount+=1;
		}
		//System.out.println("full length, amount="+amount);
		return amount;
    }
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	
		byte[] memory = Machine.processor().getMemory();
		
		int lastVPN=-1;
		TranslationEntry entry=null;
		int amount=0;
		for(int i=0;i<length;i++){
			int currentVaddr=vaddr+i;
			int vpn=getVPN(currentVaddr);
			int vpo=getVPO(currentVaddr);
			if(vpn!=lastVPN){//switch physical page
				entry=translate(vpn);
				if(entry==null){
					//we cannot find such page
					return amount;
				}
				lastVPN=vpn;
			}
			memory[entry.ppn*pageSize+vpo]=data[offset+i];
			amount+=1;
		}
		return amount;
    }
    private int getVPN(int vaddr){
    	return vaddr/pageSize;
    }
    private int getVPO(int vaddr){
    	return vaddr%pageSize;
    }
    
    /* given vpn, return ppn if found, or null if fail */
    private TranslationEntry translate(int vpn){
    	for(int i=0;i<numPages;i++){
    		TranslationEntry entry=pageTable[i];
    		if(!entry.valid) continue;
    		if(entry.vpn==vpn){
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
	
	for (int i=0; i<argv.length; i++) {
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
	pageTable=new TranslationEntry[numPages];
	//System.out.println("nr_page="+numPages+" pageSize="+pageSize);
	// load sections
	int k=0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
			int vpn = section.getFirstVPN()+i;
			// for now, just assume virtual addresses=physical addresses
			
			//section.loadPage(i, vpn);
			//get new physical page
			Integer ppn=getNewPhysicalPage();
			//System.out.println("get ppn "+ppn.intValue());
			if(ppn==null){
				System.out.println("physical page fail");
			}
			pageTable[k]=new TranslationEntry(vpn,ppn.intValue(), true,false,false,false);
			k+=1;
			section.loadPage(i,ppn.intValue());
	    }
	}
	//load stack and argument
	for(int i=0;i<stackPages+1;i++){
		int vpn=numPages-stackPages-1+i;
		Lib.debug(dbgProcess, "\tinitializing stack/argument with vpn "+vpn);
		Integer ppn=getNewPhysicalPage();
		//System.out.println("get ppn "+ppn.intValue());
		if(ppn==null){
			System.out.println("physical page fail");
		}
		pageTable[k]=new TranslationEntry(vpn,ppn.intValue(), true,false,false,false);
		k+=1;
	}
	Lib.assertTrue(k==numPages);
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	for(int i=0;i<numPages;i++){
    		TranslationEntry entry=pageTable[i];
    		ReleasePhysicalPage(entry.ppn);
    	}
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
	if(processID!=0){
		return 0;
	}
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

	private int handleCreateOpen(int a0,boolean isCreate) {
		String name=readVirtualMemoryString(a0,maxBufferSize);
		if(name==null){
			System.out.println("[create/open]read name fail");
			return -1;
		}
		//System.out.println("name="+name);
		int ret=-1;
		for(int i=0;i<maxFile;i++){
			if(fileTable[i]==null){
				ret=i;
				break;
			}
		}
		if(ret==-1){
			System.out.println("[create/open]fileTable full");
			return -1;
		}
		OpenFile file=ThreadedKernel.fileSystem.open(name,isCreate);
		if(file==null){
			System.out.println("[create/open] open fail");
			return -1;
		}
		fileTable[ret]=file;
		return ret;
	}

	private int handleRead(int a0,int a1, int a2){
		if(a0<0 || a0>=maxFile || fileTable[a0]==null){
			System.out.println("[read]invalid file id");
			return -1;
		}
		if(a2<1){
			System.out.println("[read]invalid count");
			return -1;
		}
		OpenFileWithPosition file=(OpenFileWithPosition)fileTable[a0];
		byte[] buffer=new byte[a2];
		int read_res=file.read(buffer,0,a2);
		if(read_res<0){
			return -1;
		}
		int write_res=writeVirtualMemory(a1,buffer,0,read_res);
		if(write_res!=read_res){
			System.out.println("[read]error writing memory");
			return -1;
		}
		return read_res;
	}
	
	private int handleWrite(int a0,int a1, int a2){
		if(a0<0 || a0>=maxFile || fileTable[a0]==null){
			System.out.println("[write]invalid file id");
			return -1;
		}
		if(a2<1){
			System.out.println("[write]invalid count");
			return -1;
		}
		OpenFile file=fileTable[a0];
		byte[] buffer=new byte[a2];
		int read_res=readVirtualMemory(a1,buffer);
		if(read_res!=a2){
			System.out.println("[write]error reading memory");
			return -1;
		}
		int write_res=file.write(buffer,0,a2);
		return write_res;
	}
	
	private int handleClose(int a0){
		if(a0<0 || a0>maxFile ||fileTable[a0]==null){
			System.out.println("[close]invalid file id");
			return -1;
		}
		fileTable[a0].close();
		fileTable[a0]=null;
		return 0;
	}
	
	private int handleUnlink(int a0){
		String name=readVirtualMemoryString(a0,maxBufferSize);
		if(name==null){
			System.out.println("[unlink]read name fail");
			return -1;
		}
		boolean res=ThreadedKernel.fileSystem.remove(name);
		if(res==true){
			return 0;
		}else{
			return -1;//XXX:not quite sure
		}
	}
	
	private int handleExec(int a0,int a1,int a2){
		String name=readVirtualMemoryString(a0,maxBufferSize);
		if(name==null){
			System.out.println("[exec]read name fail");
			return -1;
		}
		if(a1<0){
			System.out.println("[exec]argc<0");
			return -1;
		}
		String [] args=new String[a1];
		for(int i=0;i<a1;i++){
			Integer arg_adr=readVirtualMemoryInt(a2+4*i);
			if(arg_adr==null){
				System.out.println("[exec]read int fail");
			}
			String arg=readVirtualMemoryString(arg_adr.intValue(),maxBufferSize);
			args[i]=arg;
		}
		UserProcess process=UserProcess.newUserProcess();
		process.parent=this;
		this.childs.put(process.processID,process);
		boolean result=process.execute(name,args);
		if(result==true){
			return process.processID;
		}else{
			return -1;
		}
	}
	
	private int handleJoin(int a0, int a1){
		if(!childs.containsKey(a0)){
			System.out.println("[join]no such child");
			return -1;
		}
		UserProcess child=childs.get(a0);
		child.thread.join();
		if(childExits.containsKey(a0)){
			//normally exit
			int ret=childExits.get(a0);
			writeVirtualMemoryInt(a1,ret);
			return 1;
		}else{
			// means it fails;
			int ret=-1;
			writeVirtualMemoryInt(a1,ret);
			return 0;
		}
		
	}
	
	private int handleExit(int a0){
		//System.out.println("begin exit");
		for(int i=0;i<maxFile;i++){
			OpenFile file=fileTable[i];
			if(file!=null){
				file.close();
			}
		}
		//System.out.println("unloadSections");
		unloadSections();//cleanup memory
		if(parent!=null){
			Lib.assertTrue(!parent.childExits.containsKey(processID));
			parent.childExits.put(processID,a0);
		}
		for(Map.Entry<Integer,UserProcess> entry:childs.entrySet()){
			assert(entry.getValue().parent==this);
			entry.getValue().parent=null;
		}
		//System.out.println("finish thread");
		decreaseAliveProcessNumber();
		//System.out.println("alive process="+aliveProcessNumber);
		if(noAliveProcess()){
			Kernel.kernel.terminate();
		}else{
			KThread.finish();//XXX:possible wrong implementation
		}
		return 0;
	}
	
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

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
		return handleCreateOpen(a0,true);
	case syscallOpen:
		return handleCreateOpen(a0,false);
	case syscallRead:
		return handleRead(a0,a1,a2);
	case syscallWrite:
		return handleWrite(a0,a1,a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	case syscallExec:
		return handleExec(a0,a1,a2);
	case syscallJoin:
		return handleJoin(a0,a1);
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
	    Lib.assertNotReached("Unexpected exception");
	}
    }


	private static synchronized int getProcessID(){
		int ret=nextProcessID;
		nextProcessID+=1;
		return ret;
	}
	private static synchronized boolean noAliveProcess(){
		return aliveProcessNumber==0;
	}
	private static synchronized void increaseAliveProcessNumber(){
		aliveProcessNumber+=1;
	}
	private static synchronized void decreaseAliveProcessNumber(){
		aliveProcessNumber-=1;
		Lib.assertTrue(aliveProcessNumber>=0);
	}
	
	private static synchronized Integer getNewPhysicalPage(){
		for(int i=0;i<physicalPageSize;i++){
			if(physicalPageStatus[i]==false){
				physicalPageStatus[i]=true;
				//System.out.println("allocate new physical page "+i);
				return new Integer(i);
			}
		}
		return null;
	}
	private static synchronized void ReleasePhysicalPage(int ppn){
		Lib.assertTrue(0<=ppn && ppn<physicalPageSize && physicalPageStatus[ppn]==true);
		physicalPageStatus[ppn]=false;
	}
	
	protected UThread thread;
	protected int processID;
	protected UserProcess parent;
	protected Map<Integer,UserProcess> childs;	
	protected Map<Integer,Integer> childExits;//if exit normally 0, abnormally -1
	/** The program's file descriptors */
	protected OpenFile[] fileTable;

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final int maxFile=16;
    private static final int maxBufferSize=256;
    private static int nextProcessID=0;
    private static int aliveProcessNumber=0;
    private static final int physicalPageSize=Machine.processor().getNumPhysPages();
    private static boolean[] physicalPageStatus=new boolean[physicalPageSize];
}
