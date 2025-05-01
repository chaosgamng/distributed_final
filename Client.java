import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.io.OutputStream;
import java.io.FileReader;


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
    f = new File(path);
}

public void connect(){
try{
    s = new Socket(address, portNo);
}catch(Exception e){
    System.out.printf("Master Failed to Connect to Utility Server\n");
    

}
} 

public void sendFile(){
    try{
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

    }catch(Exception e){
        e.printStackTrace();
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
}


}
}
