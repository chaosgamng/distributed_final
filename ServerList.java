public class ServerList{
    private String ip;
    private int port;
    private String status;
    private int id;


    ServerList(String ip, int port, String status){
        this.ip = ip;
        this.port = port;
        this.status = status;
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