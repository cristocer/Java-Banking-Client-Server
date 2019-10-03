package server;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.io.PrintWriter;

/*
* The class AccountManager deals with all the requests (commands) called by the Server class
* There are 2 semaphores,2 locks and 2 conditions on locks.
* Assumption 1: To protect against a race condition situation I used 2 locks between State and Transfer commands.
* SemaC is a hold lock for State while SemaB is a hold lock for Transfer.
* Assumption 2: I also used 2 locks and a Thread.sleep(500) to protect against a deadlock situation in the case 
* of infinitely calls of a Transfer or State command at the exact same time, which would not release the lock 
* between State and Transfer if there was only one (lock).
* However, in our case the maximum number of threads is known so this shouldn't be the case (Unless there is
* a way to simulate that).
* Assumption 3. Commands State and and Transfer can't run concurently because there is the risk
* that State will be executed in between a transfer operations.
* Assumption 4. Commands Rate and Convert (as advised in the lecture) will run concurently,
* based on the assumption that if a they are called at the exact same time, then Convert
* should still get executed with the rate before the rate change occurs.
* Every set of commands ,besides State and Transfer toghether, will run concurently.
*/
public class AccountManager{
    
    //data structure used for storring accounts
    private  ConcurrentHashMap m= new ConcurrentHashMap();
    //semaphore that works with lockB and condition b
    static private int semaB=0;
    //semaphore that works with lockC and condition c
    static private int semaC=0;
    private double rate=10.0;
    //if state was first then transfer has to wait
    static private ReentrantLock lockB=new ReentrantLock();
    //if transfer was first , then state has to wait
    static private ReentrantLock lockC=new ReentrantLock();
    final Condition b=lockB.newCondition();
    final Condition c=lockC.newCondition();
    AccountManager(){
    }
    /*
    * openAccount opens a new account
    * First protection from race condition: 
    * If 2 threads try to open an account with same number, at the same exact time,
    * the second one in the execution process gets an error (tested by putting to sleep both threads).
    * All threads can open accounts simultaneously.
    * key is the accountNumber
    */
    public void openAccount(int key,Account a,Socket socket,PrintWriter out)throws Exception{
        // Using the chosen data structure's methods I can protect from race condition of
        // putting values with the same key or ovveride current key values 
        if(!m.containsKey(key)){
            m.putIfAbsent(key, a);
            //In the extreme case that it doesn't see the key in time.
            if(m.get(key)!=a){
                throw new Exception ();
            }
            out.println("Opened account "+key);
        }else{
            //If the account already exists.
            throw new Exception ();
        } 
    }

    //stateRead shows the state of the accounts and rate
    public void stateRead(PrintWriter out) throws InterruptedException{        
        //The semaphore C treats the case when: 
        //If the Transfer command is being executed then State command has to wait.
        if(semaC==1){
            //The scope of this lock is not to acquire a lock but to solve a lock assumption.
            //If multiple State commands need to wait for Trnasfer to be done then they will be all dormant.
            //If they don't need to wait, then they will be executed concurently.
            lockC.lock();            
            while(semaC==1)
                //State waits until all the previous called transfers are done.
                c.awaitUninterruptibly();
            //If the lock was acquired it will get released automatically when the execution 
            //of State commands start, by getting signaled in the Transfer method.
            lockC.unlock();
        }  
        //The semaphore B treats the case when: 
        //If the turn for execution of the State command has come then Transfer commands have to wait.
        semaB=1;       
        List sortK=new ArrayList (m.keySet());
        Collections.sort(sortK);
        for(Object i:sortK){
             out.println(((Account)m.get(i)).toString());
        }  
        out.println("Rate"+' '+rate);
        semaB=0;
        lockB.lock();
        b.signalAll();
        lockB.unlock();      
        //Avoid deadlock by not letting the same client request an overview of everything constantly which may block 
        //the whole system(assuming AS Advised that the client has to wait for a response before querying again).
        Thread.sleep(500);     
    }  
    //changeRate changes the Rate of the system.
    
    public void changeRate(double rate,PrintWriter out)throws Exception{
        try{
            this.rate=rate;
            out.println("Rate changed");
        }catch(Exception e){
            throw new Exception ("Rate can't be 0");
        }                 
    }
    //Multiple threads can use convert at the same time,but when it comes to multiple threads
    //modyfing the same account this gets syncronized(one thread has to wait).
    //This is done in the Account.convert method called below.
    public void convert(int accNumber,double arian,double pres,PrintWriter out) throws InterruptedException{     
        try{
            //This will lock only the account called instead of the whole data structure RACE CONDITION
            ((Account)m.get(accNumber)).convert(arian,pres,rate);       
            out.println("Converted");       
        }catch(Exception e){
            out.println("Account doesn't exist!");
        }  
    }

    //transfer arain and pres form accNumber1 to accNumber2
    //The locks are applied on individual accounts by having all methods in the Account class synchronized, which protects against race condition.
    //If there are 2 transfer between Account1 - Account2 and Account2 - Account3 there will be no deadlock on Account2
    //because the locks are on individual accounts and they get released immediatly after the currency change was produced.
    public void transfer(int accNumber1,int accNumber2,double arian,double pres,PrintWriter out) throws InterruptedException{
        //The semaphore B treats the case when: 
        //If the State command is being executed then Transfer commands have to wait.
        if(semaB==1){
            //The scope of this lock is not to acquire a lock but to solve a lock assumption.
            //If multiple Transfer commands need to wait for State to be done then they will be all dormant.
            //If they don't need to wait, then they will be executed concurently.
            lockB.lock();
            while(semaB==1)
                //State waits until all the previous called transfers are done.
                b.awaitUninterruptibly();
            lockB.unlock();
            //If the lock was acquired it will get released automatically when the execution 
            //of Transfer commands start, by getting signaled in the State method.
        }
        //The semaphore C treats the case when: 
        //If the turn for execution of the Transfer command has come then State commands have to wait.
        //Blocks the posibility of a state to be called in the middle of a transfer
        semaC=1;
        if(!m.containsKey(accNumber1)||!m.containsKey(accNumber2)){
            out.println("At least one account doesn't exist!");
        }else{         
            //those will lock only the accounts called instead of the whole data structure 
            ((Account)m.get(accNumber1)).transfer(-arian,-pres);           
            ((Account)m.get(accNumber2)).transfer(arian,pres);  
        }            
        out.println("Transferred");
        semaC=0;
        lockC.lock();
        c.signalAll();
        lockC.unlock();
    }
    
}
