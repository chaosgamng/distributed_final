import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class CameronMaster{
    ServerSocket server;
    static ArrayList<workerThread> workers;
    static ArrayList<String> utilityWorkers;
    static boolean running = true;
    static ArrayList<clientHandler> clients = new ArrayList<>();
    static int clientNum = 0;


    public CameronMaster() throws Exception{
        this.server = new ServerSocket(8000);
        System.err.println("Server started on: " + this.server.getInetAddress().getHostAddress());
        workers = new ArrayList<>();
        utilityWorkers = new ArrayList<>();
    }

    public synchronized void heartbeat(){
        try{
            workers = new ArrayList<>();
            for(int i =0; i < 255;i++){
                workers.add(new workerThread("127.0.0."+(i+1), 7999, utilityWorkers));
            }
            for (int i = 0; i < workers.size(); i++) {
                workers.get(i).start();
            }
            for (int i = 0; i < workers.size(); i++) {
                workers.get(i).join();
            }
            for (int idx = 0; idx < workers.size(); idx++) {
                if(workers.get(idx).avaliable.equals("AVAILABLE")){
                    utilityWorkers.add(workers.get(idx).ip);
                }
                if(workers.get(idx).avaliable.equals("LOCKED") && utilityWorkers.contains(workers.get(idx).ip)){
                    utilityWorkers.remove(utilityWorkers.indexOf(workers.get(idx).ip));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        CameronMaster master = new CameronMaster();
        ReentrantLock lock = new ReentrantLock();
        ArrayList<clientHandler> cls = new ArrayList<>();
        try{
            while(running == true){
                lock.lock();
                try {
                    
                    Socket client = master.server.accept();
                    System.out.println("Connection made.");
                    new clientHandler(master, client).start();
                    System.out.println("started");
                } catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}

class clientHandler extends Thread{

    CameronMaster server;
    Socket client;
    int num = 0;

    public clientHandler(CameronMaster server, Socket client){
        this.server = server;
        this.client = client;
    }

    public synchronized void run(){
        System.out.println("Running client handler");
        try{
            InputStream inputStream = client.getInputStream();
            // FileOutputStream fileOutputStream = new FileOutputStream("received_file.txt");
            // BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            // byte[] buffer = new byte[4096];
            // int bytesRead;
            // while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
            //     bufferedOutputStream.write(buffer, 0, bytesRead);
            // }
            // bufferedOutputStream.close();
            // fileOutputStream.close();
            // inputStream.close();
            System.out.println("Received file from client");

            server.utilityWorkers = new ArrayList<>();
            server.heartbeat();
            Thread.sleep(1000);
            if(server.utilityWorkers.size() == 0){
                System.out.println("No workers available");
                return;
            } else {
                System.out.println(server.utilityWorkers.size() + " workers found");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter[] pws = new PrintWriter[server.utilityWorkers.size()];
            Socket[] sockets = new Socket[pws.length];
            for(int i =0; i < pws.length;i++){
                Socket check = new Socket();   
                check.connect(new InetSocketAddress(server.utilityWorkers.get(i), 7999), 4000); 
                sockets[i] = check;
                pws[i] = new PrintWriter(sockets[i].getOutputStream());
            }

            ArrayList<String> tokens = new ArrayList<>();
            System.out.println("Reading file from client and sending data to utility works");

            String line;
            while((line = br.readLine()) != null){
                String[] data = line.split(" ");
                for(String token: data){
                    tokens.add(token);
                }
            }

            int split = tokens.size() / pws.length;

            for(int i =0; i < pws.length;i++){
                String st = "";
                for(int x =(i * split); i < (i+1) * split; x++){
                    if(x >= tokens.size()){
                        break;
                    }
                    st += tokens.get(x) + " ";
                }
                pws[i].println(st);
                pws[i].flush();
            }

            System.out.println("Data sent waiting on return");

            int idx = 0;
            while(idx != pws.length){
                BufferedReader br1 = new BufferedReader(new InputStreamReader(sockets[idx].getInputStream()));
                String data;
                while ((data = br1.readLine()) == null) {
                }
                num += Integer.parseInt(data);
                idx++;
                br1.close();
            }

            System.out.println("The total of sum of all utility workers is " + num);

            pws = null;
            sockets = null;
            br.close();
            client.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }



}

class workerThread extends Thread{
    String ip;
    int port;
    String avaliable;
    public workerThread(String ip, int port, ArrayList<String> taken){
        this.ip = ip;
        this.port = port;
        avaliable = "LOCKED";
    }


    @Override
    public synchronized void run(){
        try {
            
       
                // System.out.println("Connecting to client and sending message");
            Socket check = new Socket();   
            check.connect(new InetSocketAddress(ip, port), 4000); 
            PrintWriter pw = new PrintWriter(check.getOutputStream(), true);
            pw.println("Hello");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(check.getInputStream()));
            String[] data = reader.readLine().split(",");
            if(data.length > 1){
                // System.out.println("Client acks");
                // System.out.println("Utility: " + data[0] + ":" + data[1] + " is " + data[2]);
                avaliable = data[2];
            }

            reader.close();
            pw.close();
            check.close();
        } catch (Exception e) {
            // System.out.println("Utility: " + ip + ":" + port + " " + " does not exist or is in use");  
        } 
    }

}