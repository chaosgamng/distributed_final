import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

//This class needs added threading capabilities and locking as well as receiving broadcast messages

public class CameronUtility extends Thread{
    private ServerSocket serverSocket;
    private Socket clientSocket;
    boolean running = true;
    String avaliable;
    public static String sort;

    CameronUtility(){
        this.avaliable = "AVAILABLE";
    }

    public synchronized void run(){
        try{
            
            while(running){
                serverSocket = new ServerSocket(7999);
                System.out.println("Server Started and is " + this.avaliable);
                    if( avaliable.equals("AVAILABLE") ){
                        clientSocket = serverSocket.accept();
                        System.out.println("Connection Successful");
                        receiveData();
                        System.out.println("Data collected and message sent back");
                        clientSocket.close();
                        avaliable = "AVAILABLE";
                    }
                serverSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public synchronized static void insertionSort(int[] data) {
        System.out.println("Insertion sort");
		for (int i = 1; i < data.length; i++) {
			int key = data[i];
			int j = i - 1;

			while (j >= 0 && data[j] >= key) {
				data[j + 1] = data[j];
				j = j - 1;
			}

			data[j + 1] = key;
		}

	}

	public synchronized static void bubbleSort(int[] data) {
        System.out.println("Bubble sort");
		int n = data.length;

		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < n - 1; j++) {

				int temp = data[j];
				data[j] = data[j + 1];
				data[j + 1] = temp;
			}
		}

	}

	public synchronized static void mergeSort(int[] data, int n) {
		if (n < 2) {
			return;
		}
		int m = n / 2;
		int L[] = new int[m];
		int R[] = new int[n - m];

		for (int i = 0; i < m; i++) {
			L[i] = data[i];
		}
		for (int i = m; i < n; i++) {
			R[i - m] = data[i];
		}
		mergeSort(L, m);
		mergeSort(R, n - m);

		merge(data, L, R, m, n - m);

	}

	public synchronized static void merge(int data[], int[] L, int[] R, int l, int r) {
		int i = 0;
		int j = 0;
		int k = 0;

		while (i < l && j < r) {
			if (L[i] <= R[j]) {
				data[k++] = L[i++];
			} else {
				data[k++] = R[j++];
			}

		}
		while (i < l) {
			data[k++] = L[i++];
		}
		while (j < r) {
			data[k++] = R[j++];
		}

	}

    public synchronized void receiveData(){
        int[] data = null;
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String str = br.readLine();
            if(str.equals("Hello")){
                System.out.println("Acking");
                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
                pw.println(clientSocket.getInetAddress().getHostAddress() + "," + clientSocket.getLocalPort() + "," + this.avaliable);
                pw.close();
            }else{
                System.out.println("Starting collection of data from master");
                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
                avaliable = "LOCKED";
                Thread.sleep(15);
                System.out.println("working");
                ArrayList<Integer> list = new ArrayList<>();
                String[] tempd = str.split(" ");
                for(String token: tempd){
                    list.add(Integer.valueOf(token));
                }
                data = new int[list.size()];
                for(int i =0; i < list.size();i++){
                    data[i] = list.get(i);
                }

                if(data != null && data.length > 1){
                    if(sort.equals("insertion")){
                        insertionSort(data);
                    } else if (sort.equals("bubble")){
                        bubbleSort(data);
                    } else if (sort.equals("merge")){
                        System.out.println("Merge sort");
                        mergeSort(data, data.length-1);
                    } else {
                        System.out.println("Wrong type of sort selected on start up");
                    }
                }
                int temp = 0;
                for(int i =0; i < 5;i++){
                    temp += data[i];
                } 
                for(int i =data.length-5; i < data.length;i++){
                    temp += data[i];
                }    

                System.out.println("Worker thread found " + temp);
                
                pw.println(temp);
                pw.flush();
                pw.close();
            }
            br.close();

        }catch(Exception e){
            System.out.println("Data Reception Failed");
        }
    }


    public static void main(String args[]){
        CameronUtility u = new CameronUtility();
        // sort = args[0];
        sort = "insertion";
        u.start();
    
    }

}