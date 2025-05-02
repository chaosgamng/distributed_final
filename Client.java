import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.ServerSocket;

public class Client{
Socket s;
private File f;
String address;
int portNo;
OutputStream o;


Client(String address, int portNo){
    this.address = address;
    this.portNo = portNo;
}

public void setFile(String path){
    System.out.printf("Setting file\n");
    f = new File(path);
}

public void connect(){
try{
    System.out.printf("Connecting...\n");
    s = new Socket(address, portNo);
}catch(Exception e){
    System.out.printf("Client Failed to Connect to Master Server\n");
    

}
} 

public void sendFile(){
    try{
        System.out.printf("Attempting file send\n");
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

    }catch(Exception e){
        e.printStackTrace();
    }

}

public void receive(){
    try{
        System.out.printf("Awaiting return data\n");
    ServerSocket server = new ServerSocket(7000);

    server.setSoTimeout(30000);
    Socket socket = server.accept();
    InputStream i = socket.getInputStream();

    byte[] bytes = i.readAllBytes();
    String string = bytes.toString();

    for(int j = 0; j < bytes.length; j++){
        System.out.print(bytes[j]);
    }
    socket.close();
    server.close();
    
    }catch(Exception e){
        e.printStackTrace();
        System.out.printf("Client failed to receive\n");
    }


}

public static void main(String args[]){
    String filePath = "";
    Client c = new Client("127.0.0.1", 5000);
    c.connect();
if(args.length > 0){
    filePath = args[0];
    c.setFile(filePath);
    c.sendFile();
    c.receive();
}


}
}