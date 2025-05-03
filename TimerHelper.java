import java.util.TimerTask;

public class TimerHelper extends TimerTask{

   private MasterServer m;
   public TimerHelper(MasterServer m){
        this.m = m;
    }

    public void run(){
        m.healthCheck();
    }


}