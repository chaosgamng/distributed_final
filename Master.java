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
import java.lang.Math;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

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
    System.out.printf("Master Failed to Connect to Utility Server\n");
    

}
} 

//activates after thread spawn
public void run(){
    //save server and client sockets for return transmission and so they won't be erased if new connection is made
    String clientIp = clientSocket.getInetAddress().getHostAddress();
    int clientPort = serverSocket.getLocalPort();
     //tracks servers being actively used for THIS task AND which array piece they are scanning
     HashMap<Integer, ServerList> m = new HashMap<Integer, ServerList>();
    //manage working and not working Util Servers
    healthCheck();
    //Receive File for processing
    String s = receiveFile();
     //parse data and send to available utility servers
    int total = parseFile(s, m);

    

    returnData(total, clientIp, clientPort);
    
}

public void healthCheck(){
    System.out.printf("Initiating HealthCheck\n");
    serverList = new ArrayList<ServerList>();
    MulticastSocket s = null;
    String message = "Hello";
    try{
        //Establish new multicast socket, address, and port #, then send information for health check
        s = new MulticastSocket();
        InetAddress address = InetAddress.getByName("10.0.2.15");
        InetSocketAddress a = new InetSocketAddress(address, 6000);   
        DatagramPacket packet  = new DatagramPacket(message.getBytes(), message.length(), a);
        System.out.printf("Sending Health Check Packet");
            s.send(packet);


        //Set up to receive return info (should be util i.p., port #, and status)    
        byte[] buffer = new byte[1000];
        DatagramPacket p = new DatagramPacket(buffer, 0, buffer.length);

        System.out.printf("Starting wait\n");
        while(s.isClosed() == false){
        s.setSoTimeout(5000);
        s.receive(p);
        processCheck(p);
        System.out.printf("Response Packet Received\n");
        }

        s.close();

        System.out.printf("Wait ended\n");
    }catch(Exception e){
        
    }
    finally{
        System.out.printf("Heartbeat Check Complete\n");
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
        System.out.printf("File Received from Client\n");

    }catch(Exception e){
        System.out.printf("File Reception From Client Failed\n");
    }

    return s;
}

public int parseFile(String s, HashMap<Integer, ServerList> m){
    String[] tokens = s.split(" ");
    int units = Math.ceilDiv(tokens.length, 10000000);

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
        sent = sendDataToUtil(sent, socket, tokens, units, m);
        try{

         //establishes listening socket to receive data return data from all servers
         //can be filtered by checking with hashmap 
        ServerSocket serve = new ServerSocket(6500);
        while(receiving == true){
        serve.setSoTimeout(5000);
        socket = serve.accept();
        if(socket != null){
           int found = checkList(socket.getInetAddress().getHostAddress(), socket.getPort(), m);

                //if connection is on waiting list, read in data, increment total and number of array pieces received
                if(found != -1){
                BufferedInputStream i = new BufferedInputStream(socket.getInputStream());

                byte[] b = i.readAllBytes();

                String str = b.toString();

                total += Integer.parseInt(str);
                
                //remove server from hashmap to indicate failure to receive
                m.remove(found);

                received++;
                }
                else{
                    socket.close();
                }
            }

        }
        }catch(Exception e){
            System.out.printf("Wait Timed Out\n");
            receiving = false;
        }
        
        }
        else{
            try{
            sleep(5000);
            }catch(Exception e){
                System.out.printf("Sleep Failed\n");
            }
        }
    }

    return total;
}

//send data to utility servers and send back how many parts of array have been sent
public int sendDataToUtil(int sent, Socket socket, String[] array, int units, HashMap<Integer, ServerList> m){
    //Check if first time sending data or dealing with failure to receive
    for(int i = 0; i < serverList.size(); i++){
        if(serverList.get(i).getStatus() == "Available" && sent < units){
            try{
                System.out.printf("Sending data to server");
            socket = new Socket(serverList.get(i).getIp(), serverList.get(i).getPort());
            OutputStream o = socket.getOutputStream();

            int start = sent * 10000000;
            int finish = (sent + 1) * 10000000;

            if(finish > array.length){
                finish = array.length;
            }


            //send data to util server
            for(int j = start; j < finish; j++){
                byte[] b = array[j].getBytes();
                o.write(b);
            }

            //place util server in hashmap, with segment # as index value
            m.put(sent, serverList.get(i));

            //iterate to next segment and close current connection
            sent++;
            o.flush();
            o.close();
            }catch(Exception e){
                System.out.printf("Failed to send data\n");
            }
            
            
        }
        
    }

    return sent;
}

//checks HashMap for whether or not current socket connection is on the list
public int checkList(String ip, int port, HashMap<Integer, ServerList> m){
    int found = -1;

    for(int key : m.keySet()){
        if(m.get(key).getIp() == ip && m.get(key).getPort() == port){
            found = key;
        }

    }

    return found;
}

//resend data that wasn't received within the time period
public void resendData(HashMap<Integer, ServerList> m, String[] array, Socket socket){
    System.out.printf("Resending Data that Failed to Receive");
    for(int key: m.keySet()){
        for(int i = 0; i < serverList.size(); i++){
            if(serverList.get(i).getStatus() == "Available"){
                try{
                    System.out.printf("Sending data to server");
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

        o.write(sum);
        o.flush();
        o.close();
        s.close();

        System.out.printf("Final Data Sent Successfully\n");
    }catch(Exception e){
        System.out.printf("Final Data Send Failed\n");
    }

}

//end thread for whatever needed reasons
public void endThread(){
    running = false;
}



//waits for client connection
public void await(){
    try{
        System.out.printf("Waiting for Client to Connect\n");
    serverSocket = new ServerSocket(portNo);
    while(running){
        clientSocket = serverSocket.accept();
        System.out.printf("Connection to Client Successful\n");
        if(clientSocket.isConnected()){
            System.out.printf("Task Takeover Thread Spawned\n");
            Master m = new Master(5000);
            m.clientSocket = this.clientSocket;
            m.start();
        }
    }
}catch(Exception e){
    System.out.printf("Connection to Client Failed\n");
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
