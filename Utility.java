import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.io.BufferedReader;

//This class needs added threading capabilities and locking as well as receiving broadcast messages

public class Utility{
private int portNo;

private ServerSocket serverSocket;
private Socket clientSocket;
boolean running = true;

Utility(int portNo){
    this.portNo = portNo;
}

public void run(String sort){
    try{
        serverSocket = new ServerSocket(portNo);
        System.out.println("Server Started");

        while(running){
            clientSocket = serverSocket.accept();
            System.out.println("Connection Successful");
            int[] data = receiveData();
            
            if(sort == "insertion"){
                insertionSort(data);
            }
            else if(sort == "bubble"){
                bubbleSort(data);
            }
            else if(sort == "merge"){
                mergeSort(data);
            }

            sendData(data);
            clientSocket.close();
            running = false;
        }

        serverSocket.close();
    }catch(Exception e){
        System.out.println("Failed to Connect");
        //serverSocket.close();
    }
}

public void insertionSort(int[] data){
    for(int i = 1; i < data.length; i++){
        int key = data[i];
        int j = i - 1;

        while(j >= 0 && data[j] >= key){
            data[j + 1] = data[j];
            j = j - 1;
        }

        data[j + 1] = key;
    }


}

public void bubbleSort(int[] data){

}

public void mergeSort(int[] data){
    
}

public int[] receiveData(){
    int[] data = null;
    try{
        BufferedInputStream b = new BufferedInputStream(clientSocket.getInputStream());
        byte[] bytes = b.readAllBytes();

        String str = new String(bytes, StandardCharsets.UTF_8);
        String[] tokens = str.split(" ");
         data = new int[tokens.length];
        for(int i = 0; i < tokens.length; i++){
            data[i] = Integer.parseInt(tokens[i]);
        }
        b.close();

    }catch(Exception e){
        System.out.println("Data Reception Failed");
    }


    
    return data;
}

public void sendData(int[] data){
    try{
    System.out.println("Attempting connection");
    OutputStream o = clientSocket.getOutputStream();
    for(int i : data){
        o.write(i);
        
        System.out.println("Success");
    }
    o.flush();
}catch(Exception e){
    System.out.println("Write Failed");
    e.printStackTrace();
}



}

public static void main(String args[]){
    String sort = "";
    if(args.length > 0){
        sort = args[0];
        Utility u = new Utility();
        u.run(sort);
    }

   
}

}