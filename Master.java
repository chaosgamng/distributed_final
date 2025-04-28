import java.io.InputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedInputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.lang.Math;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class Master extends Thread{

Socket clientSocket;
ServerSocket serverSocket;
String address;
int portNo;
int utilPortNo;
private boolean running = true;
private ArrayList<ServerList> serverList;

Master(int portNo){
    this.portNo = portNo;
}

public void connect(){
try{
    clientSocket = new Socket(address, portNo);
}catch(Exception e){
    System.out.printf("Master Failed to Connect to Utility Server / %s", LocalDateTime.now());
    

}
} 

//activates after thread spawn
public void run(){
    //save server and client sockets for return transmission and so they won't be erased if new connection is made
    Socket cSocket = this.clientSocket;
    ServerSocket sSocket = this.serverSocket;
    //manage working and not working Util Servers
    healthCheck();
    //Receive File for processing
    String s = receiveFile();
     //parse data and send to available utility servers
    int total = parseFile(s);

    returnData(total, cSocket);
    try{
    cSocket.close();
    }catch(Exception e){
        e.printStackTrace();
    }
}

public void healthCheck(){
    System.out.printf("Initiating HealthCheck %s\n", LocalDateTime.now());
    serverList = new ArrayList<ServerList>();
    MulticastSocket s = null;
    String message = "Hello";
    try{
        //Establish new multicast socket, address, and port #, then send information for health check
        s = new MulticastSocket();
        InetAddress address = InetAddress.getByName("10.0.2.15");
        InetSocketAddress a = new InetSocketAddress(address, 6000);   
        DatagramPacket packet  = new DatagramPacket(message.getBytes(), message.length(), a);
        System.out.println("Sending packet");
            s.send(packet);


        //Set up to receive return info (should be util i.p., port #, and status)    
        byte[] buffer = new byte[1000];
        DatagramPacket p = new DatagramPacket(buffer, 0, buffer.length);

        System.out.printf("Starting wait %s\n", LocalDateTime.now());
        while(s.isClosed() == false){
        s.setSoTimeout(5000);
        s.receive(p);
        processCheck(p);
        System.out.printf("Packet Received %s\n", LocalDateTime.now());
        }

        s.close();

        System.out.printf("Wait ended %s\n", LocalDateTime.now());
    }catch(Exception e){
        
    }
    finally{
        System.out.printf("Heartbeat Check Complete %s\n", LocalDateTime.now());
    }
}

    //process return data from heartbeat and add responding servers to list
    public void processCheck(DatagramPacket p){
        byte[] bytes = p.getData();

        String str = new String(bytes, StandardCharsets.UTF_8);
        
        String[] tokens = str.split(" ");

        String ip = tokens[0];
        int port = Integer.parseInt(tokens[1]);
        String status = tokens[2];

        ServerList s = new ServerList(ip, port, status);

        serverList.add(s);
    }

public String receiveFile(){

    String s = "";

    try{

        InputStream i = clientSocket.getInputStream();
        byte[] bytes = i.readAllBytes();
        s = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("File Received");

    }catch(Exception e){
        e.printStackTrace();
    }

    return s;
}

public int parseFile(String s){
    String[] tokens = s.split(" ");
    int units = Math.ceilDiv(tokens.length, 10000000);

    //instantiate socket for connecting to EACH individual server multiple sockets will not be used
    Socket socket = null;
    int sent = 0;
    int received = 0;
    int total = 0;
    while(received < units){
        if(serverList.size() > 0){
        sent = sendDataToUtil(sent, socket, tokens, units);
        try{
        ServerSocket serve = new ServerSocket(6500);
        serve.setSoTimeout(5000);
        socket = serve.accept();
        if(socket != null){
                BufferedInputStream i = new BufferedInputStream(socket.getInputStream());

                byte[] b = i.readAllBytes();

                
                received++;
        }
        }catch(Exception e){
            System.out.printf("Failed to Receive Data %s\n", LocalDateTime.now());
        }
        
        }
        else{
            try{
            sleep(5000);
            }catch(Exception e){
                System.out.printf("Sleep Failed %s\n", LocalDateTime.now());
            }
        }
    }

    return total;
}

//send data to utility servers and send back how many parts of array have been sent
public int sendDataToUtil(int sent, Socket socket, String[] array, int units){
    for(int i = 0; i < serverList.size(); i++){
        if(serverList.get(i).getStatus() == "Available" && sent < units){
            try{
                System.out.printf("Sending data to server %d %s\n", i, LocalDateTime.now());
            socket = new Socket(serverList.get(i).getIp(), serverList.get(i).getPort());
            OutputStream o = socket.getOutputStream();

            int start = sent * 10000000;
            int finish = (sent + 1) * 10000000;

            if(finish > array.length){
                finish = array.length;
            }

            for(int j = start; j < finish; j++){
                byte[] b = array[j].getBytes();
                o.write(b);
            }
            sent++;
            o.close();
            }catch(Exception e){
                System.out.printf("Failed to send data %s\n", LocalDateTime.now());
            }
            
        }
        
    }

    return sent;
}

//return data to client
public void returnData(int sum, Socket socket){

}

//end thread for whatever needed reasons
public void endThread(){
    running = false;
}



//waits for client connection
public void await(){
    try{
        System.out.println("Waiting for Client");
    serverSocket = new ServerSocket(portNo);
    while(running){
        clientSocket = serverSocket.accept();
        System.out.println("Connection Successful");
        if(clientSocket.isConnected()){
            System.out.println("Thread Spawned");
            Master m = new Master(5000);
            m.clientSocket = this.clientSocket;
            m.start();
        }
    }
}catch(Exception e){
    System.out.println("Connection Failed");
}
}


public static void main(String args[]){
    Master m = new Master(5000);

    //Establish pulsing heartbeat protocol to maintenance server list
    Timer timer = new Timer(true);
    TimerTask task = new TimerHelper(m);

    timer.schedule(task, 0, 15000);

    //wait for client connection
    m.await();
}

}