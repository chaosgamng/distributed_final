import java.util.TimerTask;

public class TimerHelper extends TimerTask{

    Master m;
    TimerHelper(Master m){
        this.m = m;
    }

    public void run(){
        m.healthCheck();
    }


}