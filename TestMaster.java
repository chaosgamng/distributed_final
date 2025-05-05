import java.io.InputStream;
import java.net.Socket;
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
import java.util.Arrays;
import java.lang.Math;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class MasterServer extends Thread{

Socket clientSocket;
ServerSocket serverSocket;
String address;
int portNo;
int utilPortNo;
private boolean running = true;
private ArrayList<ServerList> serverList;
private static final Object fileLock = new Object();

public static void log(String format, Object ... args) {
	synchronized(fileLock) {
		try(PrintWriter out = new PrintWriter(new FileWriter("/home/monique/logs/log.txt", true))) {
		out.printf("%s [%s] [MasterServer]", java.time.LocalDateTime.now(), Thread.currentThread().getName());
		out.printf(format + "%n", args);
	}catch(IOException e) {
		e.printStackTrace();
	}
	}
	
	
}

MasterServer(int portNo){
    this.portNo = portNo;
    MasterServer.log("Instance of MasterServer was created with port %d", portNo);
}

public void connect(){
	MasterServer.log("Attempting to connnect to Utility Server");
try{
	
    clientSocket = new Socket(address, 7000);
    MasterServer.log("Client Socket started at %s:%d", address, 7000);
}catch(Exception e){
    System.out.printf("Master Failed to Connect to Utility Server\n");
   MasterServer.log("Exception encountered in connect():%s", e.getMessage()); 

}
} 

//activates after thread spawn
public void run(){
    //save server and client sockets for return transmission and so they won't be erased if new connection is made
    MasterServer.log("MasterServer thread currently running for client %s", clientSocket.getInetAddress());
	String clientIp = clientSocket.getInetAddress().getHostAddress();
    int clientPort = clientSocket.getLocalPort();
     //tracks servers being actively used for THIS task AND which array piece they are scanning
     HashMap<Integer, ServerList> m = new HashMap<Integer, ServerList>();
    //manage working and not working Util Servers
    healthCheck();
    MasterServer.log("Health check is being completed with %d servers", serverList.size());
    //Receive File for processing
    String s = receiveFile();
     //parse data and send to available utility servers
    int total = parseFile(s, m);
    MasterServer.log("File has been parsed and is ready to processed with a sum of: %d",total);
    
    returnData(total, clientIp, 7000);
    
}

public void healthCheck(List<ServerList> allServers){
 serverList = new ArrayList<>();
 for(serverList server: allServers){
    try(Socket socket = new socket()){
    SocketAddress addr = new InetSocketAddress(server.getIp(), server.getPort());
    socket.connect(addr, 2000);
    server.setStatus("Available");
    serverList.add(server);
    MasterServer.log("Server %s:%d is available", server.getIp(), server.getPort());
    }catch(IOException e){
            MasterServer.log("Server %s:%d is unavailable", server.getIp(), server.getPort());
    }

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

        System.out.printf("New Server Added\n");
    }

public String receiveFile(){

    String s = "";

    try{

        InputStream i = clientSocket.getInputStream();
        byte[] bytes = i.readAllBytes();
        s = new String(bytes, StandardCharsets.UTF_8);
        System.out.printf("File Received from Client\n");

    }catch(Exception e){
        System.out.printf("File Reception From Client Failed\n");
    }

    return s;
}

public int parseFile(String s, HashMap<Integer, ServerList> m){
    System.out.printf("Beginning File Parsing\n");
   
    String[] tokens = s.split(" ");
    int units = Math.ceilDiv(tokens.length, 10000000); 
    MasterServer.log("Beginning File Parsing with file, contains %d units", units);
    //instantiate socket for connecting to EACH individual server multiple sockets will not be used
    Socket socket = null;
    //keeps track of array pieces sent
    int sent = 0;
    int received = 0;
    //keeps track of array pieces received
    int total = 0;
    //value for controller receiving loop
    boolean receiving = true;
    while(received < units){
        if(serverList.size() > 0){
            //check if data has been previously and needs to be resent
        if(m.isEmpty() == false){
            resendData(m, tokens, socket);
        }
        //send as much data as possible to utility servers and return how many array pieces were sent
        System.out.printf("Sending Data to Util Servers\n");
        MasterServer.log("Sending Data to Utility Servers: Sent:%d Socket:%d Tokens: %d  Units:%d", sent, socket);
        sent = sendDataToUtil(sent, socket, tokens, units, m);
        try{

         //establishes listening socket to receive data return data from all servers
         //can be filtered by checking with hashmap 
        ServerSocket serve = new ServerSocket(6500);
        while(receiving == true){
        serve.setSoTimeout(30000);
        socket = serve.accept();
        System.out.printf("Data Return Connection Accepted\n");
        MasterServer.log("Data Return Connection Accepted");
        if(socket != null){
           int found = checkList(socket.getInetAddress().getHostAddress(), socket.getPort(), m);
                //if connection is on waiting list, read in data, increment total and number of array pieces received
                if(found != -1){
                System.out.printf("Receiving Sum From Server\n");
                MasterServer.log("Receiving Sum From Server");
                InputStream i = socket.getInputStream();
                byte[] b = i.readAllBytes();

                String str = new String(b, StandardCharsets.UTF_8);
                total += Integer.parseInt(str);
                MasterServer.log("Received Sum %d From Server %s", Integer.parseInt(str), socket.getInetAddress());
                //remove server from hashmap to indicate failure to receive
                m.remove(found);

                received++;
                found = -1;
                System.out.printf("Sum added to Total\n");
                MasterServer.log("Sum added to Total");
                socket.close();
                //check terminating condition
                if(received >= units){
                    receiving = false;
                }

                }
                else{
                    socket.close();
                }
            }

        }
        serve.close();
        }catch(Exception e){
            System.out.printf("Wait Timed Out\n");
            MasterServer.log("Wait Timed Out %s", e.getMessage());
            receiving = false;
            e.printStackTrace();
        }
        
        }
        else{
            try{
            sleep(5000);
            }catch(Exception e){
                System.out.printf("Sleep Failed\n");
                MasterServer.log("Sleep Failed %s", e.getMessage());
                
            }
        }
    }

    return total;
}

//send data to utility servers and send back how many parts of array have been sent
public int sendDataToUtil(int sent, Socket socket, String[] array, int units, HashMap<Integer, ServerList> m){
    //Check if first time sending data or dealing with failure to receive
    for(int i = 0; i < serverList.size(); i++){
        if(serverList.get(i).getStatus().trim().equals("Available") && sent < units){
            try{
                System.out.printf("Sending data to server\n");
                MasterServer.log("Sending data segment %d to Utility Server at %s:%d", sent, serverList.get(i).getIp(), serverList.get(i).getPort());
            socket = new Socket(serverList.get(i).getIp(), serverList.get(i).getPort());
            
            OutputStream o = socket.getOutputStream();

            int start = sent * 10000000;
            int finish = (sent + 1) * 10000000;

            if(finish > array.length){
                finish = array.length;
            }


            //send data to util server
            for(int j = start; j < finish; j++){
                String str = "" + array[j] + " ";
                byte[] b = str.getBytes();
                o.write(b);
            }

            //place util server in hashmap, with segment # as index value
            m.put(sent, serverList.get(i));

            //iterate to next segment and close current connection
            sent++;
            o.flush();
            o.close();
            }catch(Exception e){
                e.printStackTrace();
                System.out.printf("Failed to send data\n");
                MasterServer.log("Exception encountered in sendToUtil()", e.getMessage());
            }
            
            
        }
        
    }

    return sent;
}

//checks HashMap for whether or not current socket connection is on the list
public int checkList(String ip, int port, HashMap<Integer, ServerList> m){
    System.out.printf("Checking List of Expected Servers\n");
    MasterServer.log("Checking List of Expected Servers");
    int found = -1;

    for(int key : m.keySet()){
        if(m.get(key).getIp().equals(ip)){
            found = key;
        }

    }
    return found;
}

//resend data that wasn't received within the time period
public void resendData(HashMap<Integer, ServerList> m, String[] array, Socket socket){
    System.out.printf("Resending Data that Failed to Receive\n");
    
    for(int key: m.keySet()){
        for(int i = 0; i < serverList.size(); i++){
            if(serverList.get(i).getStatus() == "Available"){
                try{
                	MasterServer.log("Resending data segment %d to Utility Server at %s:%d", key,serverList.get(i).getIp(), serverList.get(i).getPort());
                    System.out.printf("Sending data to server\n");
                    socket = new Socket(serverList.get(i).getIp(), serverList.get(i).getPort());
                    OutputStream o = socket.getOutputStream();
        
                    int start = key * 10000000;
                    int finish = (key + 1) * 10000000;
        
                    if(finish > array.length){
                        finish = array.length;
                }
             //send data to new util server
            for(int j = start; j < finish; j++){
                byte[] b = array[j].getBytes();
                o.write(b);
            }

            //place updated util server in hashmap, with segment # as index value
            m.put(key, serverList.get(i));

            o.flush();
            o.close();
            }catch(Exception e){
                System.out.printf("Failed to send data\n");
                MasterServer.log("Exception encountered in resendData()", e.getMessage());
            }
            
        }
    }
}

}

//return data to client
public void returnData(int sum, String ip, int port){
    try{
        Socket s = new Socket(ip, port);

        OutputStream o = s.getOutputStream();

        String str = "" + sum;
        byte[] bytes = str.getBytes();

        o.write(bytes);
        o.flush();
        o.close();
        s.close();

        System.out.printf("Final Data Sent Successfully\n");
        MasterServer.log("Final data (sum: %d) sent successfully to client %s:%d",sum,ip,port);

    }catch(Exception e){
        System.out.printf("Final Data Send Failed\n");
        MasterServer.log("Exception encountered in returnData(): ", e.getMessage());
    }

}

//end thread for whatever needed reasons
public void endThread(){
    running = false;
    MasterServer.log("Thread has been ended");
}



//waits for client connection
public void await(){
    try{
        System.out.printf("Waiting for Client to Connect\n");
        MasterServer.log("Waiting for Client to Connect on port %d:", portNo );
    serverSocket = new ServerSocket(portNo);
    while(running){
        clientSocket = serverSocket.accept();
        System.out.printf("Connection to Client Successful\n");
        MasterServer.log("Client Connnected from %s:%d", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
        if(clientSocket.isConnected()){
            System.out.printf("Task Takeover Thread Spawned\n");
            MasterServer.log("Task Takeover Thread Spawned");
            MasterServer m = new MasterServer(5000);
            m.clientSocket = this.clientSocket;
            m.start();
        }
    }
}catch(Exception e){
    System.out.printf("Connection to Client Failed\n");
    MasterServer.log("Exception encountered in await()", e.getMessage());
}
}


public static void main(String args[]){
    MasterServer m = new MasterServer(5000);
    MasterServer.log("Thread created in Main() starting MasterServer and TimerHelper");

    //Establish pulsing heartbeat protocol to maintenance server list
    Timer timer = new Timer(true);
    TimerTask task = new TimerHelper(m);

    timer.schedule(task, 0, 15000);

    //wait for client connection
    m.await();
}

}
