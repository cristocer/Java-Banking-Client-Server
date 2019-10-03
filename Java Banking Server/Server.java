package server;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {     

    /**
     * Runs the server. When a client connects, the server spawns a new thread to do
     * the servicing.
     */
    public static void main(String[] args) throws Exception {
        
        AccountManager accM=new AccountManager();
        try (ServerSocket listener = new ServerSocket(4242)) {
	    ExecutorService pool = Executors.newFixedThreadPool(1000);
            while (true) {
                pool.execute(new Talk(listener.accept(),accM));
            }
        }
    }
    
    private static class Talk implements Runnable {
        
        private Socket socket;
        private AccountManager accM;
        Talk(Socket socket, AccountManager accM) {
            this.socket = socket;
            this.accM=accM;
        }
        //Execution of the State command.
        //Additional errors are treated in the AccountManager method called below.
        private void state(PrintWriter out) throws InterruptedException{            
            accM.stateRead(out);
        }
        //Execution of the Open command.
        //Additional errors are treated in the AccountManager method called below.
        private void open(double a,PrintWriter out) throws InterruptedException{
            try{
                accM.openAccount((int)a, new Account((int)a),socket,out);
            }catch (Exception e){
                out.println("Account1 already exists. You can only open one with a different name!");
            }
        }
        //Execution of the Rate command.
        //Additional errors are treated in the AccountManager method called below.
        private void rate(double a,PrintWriter out){
            try{
                accM.changeRate(a,out);
            }catch (Exception e){
                out.println("Rate can't be 0!Please try again.");
            }
        }
        //Execution of the Convert command.
        //Additional errors are treated in the AccountManager method called below.
        private void convert(int a,double b1,double b2,PrintWriter out) throws InterruptedException{           
            accM.convert(a,b1,b2,out);
        }
        //Execution of the Transfer command.
        //Additional errors are treated in the AccountManager method called below.
        private void transfer(int a1,int a2,double b1,double b2,PrintWriter out) throws Exception{
            accM.transfer(a1,a2,b1,b2,out);
        }
        
        public void run() {
            System.out.println("Connected: " + socket);
            try {
                Scanner in = new Scanner(socket.getInputStream());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                //reading commands from clients
                while (in.hasNextLine()) {
		    String line = in.nextLine();
                    boolean check=true;
                    try{
                       //State command
                       if(line.equals("State")){
                           state(out);
                           check=false;
                        }else{                            
                            switch(line.substring(0, line.indexOf(' '))){
                                //Open command
                                case "Open":  open(Integer.parseInt(line.substring(5,line.length())),out);break;
                                //Rate command
                                case "Rate": rate(Double.parseDouble(line.substring(5,line.length())),out);break;
                                //Convert command
                                case "Convert": convert(Integer.parseInt(line.substring(8,line.indexOf(' ',8))),
                                       Double.parseDouble(line.substring(line.indexOf('(',0)+1,line.indexOf(',',0))),
                                       Double.parseDouble(line.substring(line.indexOf(',',0)+1,line.indexOf(')',0)))
                                       ,out);break;
                                //Transfer command
                                case "Transfer":transfer(Integer.parseInt(line.substring(9,line.indexOf(' ',9))),
                                       Integer.parseInt(line.substring(line.indexOf(' ',9)+1,line.lastIndexOf(' '))),
                                       Double.parseDouble(line.substring(line.indexOf('(',line.lastIndexOf(' '))+1,line.indexOf(',',0))),
                                       Double.parseDouble(line.substring(line.indexOf(',',0)+1,line.indexOf(')',0)))
                                       ,out);
                                break;
                                default: throw new Exception();
                            }
                        }
                    } catch(Exception e){
                        if(line.indexOf(' ')==-1&&check==true){//if there are no spaces besides in the case of State command output:
                            out.println("You maybe forgot a space,bracket or a comma. Please put another Input.");
                        }else{
                            //if there was an error in the command format
                            out.println("Inccorect command, values, or too many inputed spaces or commas,please try again.");
                        }                        
                    }                                
                }
            } catch (Exception e) {//errors with sockets
                System.out.println("Error:" + socket);
            } finally {
                try { socket.close(); } catch (IOException e) {}
                System.out.println("Closed: " + socket);
            }
        }
    }
}