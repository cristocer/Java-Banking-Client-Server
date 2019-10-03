package server;
/* 
* Account class is an instance of an account that a user may have.
* Besides the constructor and the ovveriden toString method it has 
* a convert and transfer synchronized methods.
* The keyword synchronized here means that a lock is applied on the account that
* needs to suffer a transformation, by changing arian and pres values.
* An account can't be modified by two threads at the same time which avoids race
* condition by bit letting threads "battle"(race) for a lock on a method in this class
* which very likely alter the data.
*/
public class Account {
    
    private int accNumber;//account number
    private double arian,pres;//currency
    static private int i=0;
    Account(int accNumber){
        this.accNumber=accNumber;
        this.arian=0.0;
        this.pres=0.0;
    }
    public synchronized void convert(double arian,double pres,double rate)throws InterruptedException {
        this.arian=this.arian - arian + pres/ rate;
        this.pres=this.pres-pres +arian*rate;                     
    }
    public synchronized void transfer(double arian,double pres) throws InterruptedException{
        this.arian+=arian ;
        this.pres+=pres ;                   
    }    
    @Override
    public String toString(){
        return String.valueOf(accNumber)+':'+" Arian "+
                String.valueOf(arian)+','+" Pres "+String.valueOf(pres);
    }  
}
