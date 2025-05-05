import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
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
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

public class MasterServer extends Thread{

Socket clientSocket;
ServerSocket serverSocket;
String address;
int portNo;
int utilPortNo;
private boolean running = true;
private ArrayList<ServerList> serverList;

MasterServer(int portNo){
    this.portNo = portNo;
}

public void connect(){
try{
    clientSocket = new Socket(address, 7000);
}catch(Exception e){
    System.out.printf("Master Failed to Connect to Utility Server\n");
    

}
} 

//activates after thread spawn
public void run(){
    try{
        InputStreamReader re = new InputStreamReader(clientSocket.getInputStream());
        BufferedReader br = new BufferedReader(re);
        //save server and client sockets for return transmission and so they won't be erased if new connection is made
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getLocalPort();
        //tracks servers being actively used for THIS task AND which array piece they are scanning
        HashMap<Integer, ServerList> m = new HashMap<Integer, ServerList>();
        //manage working and not working Util Servers
        healthCheck();
        //Receive File for processing
        String s = "";
        while(re.ready() == false){
            
        }
        s = br.readLine();
        System.out.printf("File Received from Client\n");
        System.out.println();
        //parse data and send to available utility servers
        Thread.sleep(4000);
        int total = parseFile(s, m);
        returnData(total, clientIp, 4999);
    } catch (Exception e){

    }
    

    

    
}

public void healthCheck(){
    // System.out.printf("Initiating HealthCheck\n");
    serverList = new ArrayList<ServerList>();
    MulticastSocket s = null;
    String message = "Hello";
    try{
        //Establish new multicast socket, address, and port #, then send information for health check
        s = new MulticastSocket();
        // System.out.printf("Sending Health Check Packet\n");
        for(int i =1; i < 2;i++){
            Socket check = new Socket();   
            check.connect(new InetSocketAddress("127.0.0." + i, 6000), 4000); 
            PrintWriter pw = new PrintWriter(check.getOutputStream(), true);
            pw.println(message);
            // System.out.println("127.0.0." + i);
            // System.out.printf("Starting wait\n");

            while(check.getInputStream().available() <= 0){
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(check.getInputStream()));
            String data = br.readLine();
            // System.out.println(data);
            processCheck(data);
            // System.out.printf("Response Packet Received\n");
            pw.close();
            check.close();
            
            
        }
        // System.out.printf("Wait ended\n");

        
       

        
    }catch(Exception e){
        System.out.println("Something went wrong");
    }
    finally{
        // System.out.printf("Heartbeat Check Complete\n");
    }
}

    //process return data from heartbeat and add responding servers to list
    public void processCheck(String p){        
        String[] tokens = p.split(" ");

        String ip = tokens[0];
        int port = Integer.parseInt(tokens[1]);
        String status = tokens[2];

        ServerList s = new ServerList(ip, port, status);

        serverList.add(s);
        return;
    }

// public String receiveFile(){

//     String s = "";

//     try{
//         InputStreamReader re = new InputStreamReader(clientSocket.getInputStream());
//         BufferedReader br = new BufferedReader(re);
//         while(re.ready() == false){
            
//         }
//         String s = br.readLine();
//         System.out.printf("File Received from Client\n");

//     }catch(Exception e){
//         System.out.printf("File Reception From Client Failed\n");
//     }

//     return s;
// }

public int parseFile(String s, HashMap<Integer, ServerList> m){
    System.out.printf("Beginning File Parsing\n");
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
        System.out.printf("Sending Data to Util Servers\n");
        sent = sendDataToUtil(sent, socket, tokens, units, m);
        try{

         //establishes listening socket to receive data return data from all servers
         //can be filtered by checking with hashmap 
        ServerSocket serve = new ServerSocket(6500);
        while(receiving == true){
        serve.setSoTimeout(30000);
        socket = serve.accept();
        System.out.printf("Data Return Connection Accepted\n");
        if(socket != null){
           int found = checkList(socket.getInetAddress().getHostAddress(), socket.getPort(), m);
                //if connection is on waiting list, read in data, increment total and number of array pieces received
                if(found != -1){
                System.out.printf("Receiving Sum From Server\n");
                InputStream i = socket.getInputStream();
                byte[] b = i.readAllBytes();

                String str = new String(b, StandardCharsets.UTF_8);
                total += Integer.parseInt(str);
                
                //remove server from hashmap to indicate failure to receive
                m.remove(found);

                received++;
                found = -1;
                System.out.printf("Sum added to Total %d\n", total);
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
            receiving = false;
            e.printStackTrace();
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
        if(serverList.get(i).getStatus().trim().equals("Available") && sent < units){
            try{
                System.out.printf("Sending data to server\n");
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
            }
            
            
        }
        
    }

    return sent;
}

//checks HashMap for whether or not current socket connection is on the list
public int checkList(String ip, int port, HashMap<Integer, ServerList> m){
    System.out.printf("Checking List of Expected Servers\n");
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
            }
            
        }
    }
}

}

//return data to client
public void returnData(int sum, String ip, int port){
    try{
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress("127.0.0.1", 4999), 4000);
        OutputStream o = sock.getOutputStream();

        PrintWriter pr = new PrintWriter(o, true);
        pr.println(sum);
        pr.close();

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
            MasterServer m = new MasterServer(5000);
            m.clientSocket = this.clientSocket;
            m.start();
        }
    }
}catch(Exception e){
    System.out.printf("Connection to Client Failed\n");
}
}


public static void main(String args[]){
    MasterServer m = new MasterServer(5000);

    //Establish pulsing heartbeat protocol to maintenance server list
    Timer timer = new Timer(true);
    TimerTask task = new TimerHelper(m);

    timer.schedule(task, 0, 15000);

    //wait for client connection
    m.await();
}

}