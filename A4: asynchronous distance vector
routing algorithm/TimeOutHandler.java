import java.util.TimerTask;
//========A time out handler for Router
public class TimeOutHandler implements Runnable{	
	Router master;
	public TimeOutHandler(Router master){
		this.master = master;	
	}
	
	public void run(){
		master.processTimeOut();		
	}
	
	
}
