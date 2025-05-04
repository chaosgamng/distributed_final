import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.ServerSocket;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class Client{
Socket s;
private File f;
String address;
int portNo;
OutputStream o;

private static final Object fileLock = new Object();

	public static void log(String format, Object... args) {
		synchronized (fileLock) {
			try (PrintWriter out = new PrintWriter(new FileWriter("/logs/log.txt", true))) {
				out.printf("%s [%s] [Client]", java.time.LocalDateTime.now(), Thread.currentThread().getName());
				out.printf(format + "%n", args);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
Client(String address, int portNo){
    this.address = address;
    this.portNo = portNo;
    Client.log("Instance of Client was created with address:%s and port:%d", address,portNo);
}

public void setFile(String path){
    System.out.printf("Setting file\n");
    Client.log("Setting file path: %s", path);
    f = new File(path);
}

public void connect(){
try{
    System.out.printf("Connecting...\n");
    Client.log("Client is trying to connect with socket:%s and port:%d", address, portNo);
    s = new Socket(address, portNo);
}catch(Exception e){
    System.out.printf("Client Failed to Connect to Master Server\n");
    Client.log("Exception encountered in connect() %s", e.getMessage());
    

}
} 

public void sendFile(){
    try{
        System.out.printf("Attempting file send\n");
        Client.log("Attempting to send file to server %s", s.getInetAddress());
        
        o = s.getOutputStream();
        BufferedReader r = new BufferedReader(new FileReader(f));

        String str;

        while((str = r.readLine()) != null){
            str += " ";
            byte[] b  = str.getBytes();
            o.write(b);
        }

        o.flush();
        r.close();
        o.close();
        System.out.printf("File Sent \n");
        Client.log("File Sent");

    }catch(Exception e){
        e.printStackTrace();
        Client.log("Exception encountered in sendFile(): %s",e.getMessage());
    }

}

public void receive(){
    try{
        System.out.printf("Awaiting return data\n");
        Client.log("Awaiting return data...");
    ServerSocket server = new ServerSocket(7000);

    server.setSoTimeout(30000);
    Socket socket = server.accept();
    InputStream i = socket.getInputStream();

    byte[] bytes = i.readAllBytes();
    String string = new String(bytes, StandardCharsets.UTF_8);

    System.out.printf("The Final Result is: %s\n", string);
    Client.log("Final Result; %s", string);
    socket.close();
    server.close();
    
    }catch(Exception e){
        e.printStackTrace();
        System.out.printf("Client failed to receive\n");
        Client.log("Exception encountered in receive(): %s", e.getMessage());
    }


}

public static void main(String args[]){
    String filePath = "";
    Client c = new Client("127.0.0.1", 5000);
    Client.log("Instance of Client started with: %s ", Arrays.toString(args));
    c.connect();
if(args.length > 0){
    filePath = args[0];
    c.setFile(filePath);
    c.sendFile();
    c.receive();
}


}
}
