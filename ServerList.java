import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;


public class ServerList{
    private String ip;
    private int port;
    private String status;
    private int id;

    private static final Object fileLock = new Object();

	public static void log(String format, Object... args) {
		synchronized (fileLock) {
			try (PrintWriter out = new PrintWriter(new FileWriter("/logs/log.txt", true))) {
				out.printf("%s [%s] [ServerList]", java.time.LocalDateTime.now(), Thread.currentThread().getName());
				out.printf(format + "%n", args);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
    ServerList(String ip, int port, String status){
        this.ip = ip;
        this.port = port;
        this.status = status;
        ServerList.log("Instance of ServerList was created with ip:%s, port:%d, and status:%s", ip,port,status);
    }


    public String getStatus(){
        return this.status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public String getIp(){
        return this.ip;
    }

    public void setIp(String ip){
        this.ip = ip;
    }

    public int getPort(){
        return this.port;
    }

    public void setPort(int port){
        this.port = port;
    }

    public int getId(){
        return this.id;
    }

    public void setId(int id){
        this.id = id;
    }
}
