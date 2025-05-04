import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

//This class needs added threading capabilities and locking as well as receiving broadcast messages

public class Utility extends Thread {

	private int portNo;
	private String sortType;
	private static final Lock entryLock = new ReentrantLock();
	private ServerSocket serverSocket;
	static String status = null;
	private static final Object fileLock = new Object();

	boolean running = true;

	public static void log(String format, Object ... args) {
		synchronized(fileLock) {
			try(PrintWriter out = new PrintWriter(new FileWriter("/logs/shared-log.txt", true))){
				out.printf("%s [%s] [Utility] ", java.time.LocalDateTime.now(), Thread.currentThread().getName());
				out.printf(format + "%n", args);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	Utility(int portNo, String sortType) {
		this.portNo = portNo;
		this.sortType = sortType;
		Utility.log("Instance of Utility was created with port %d and sort %s", portNo, sortType);
	}

	@Override
	public void run() {
		status = "Available";
		Utility.log("Utility thread made on port %d with sort '%s'", portNo, sortType);
		
		try {
			serverSocket = new ServerSocket(portNo);
			Utility.log("Server socket opened on port %d", portNo);
			System.out.printf("Server Started @ %d\n", portNo);
			
			listenToMulticast(status);

			while (running) {
				Utility.log("Awaiting MasterServer Connection...");
				System.out.printf("Awaiting Master Server Connection...\n");
				Socket clientSocket = serverSocket.accept();
				System.out.printf("Connection Successful\n");
				Utility.log("Connection Sucessful");

				Thread t = new Thread(new ClientHandler(clientSocket, sortType));
				t.start();
			}

		} catch (Exception e) {
			Utility.log("Exception encountered in run(): %s", e.getMessage());
			System.out.printf("Failed to Connect\n");
			e.printStackTrace();
			// serverSocket.close();
			status = "Process Failed";
		}
		
	}

	public static void insertionSort(int[] data) {
		Utility.log("Insertion sort started with array: %s", Arrays.toString(data));
		System.out.printf("Beginning Insertion Sort\n");
		for (int i = 1; i < data.length; i++) {
			int key = data[i];
			int j = i - 1;

			while (j >= 0 && data[j] >= key) {
				data[j + 1] = data[j];
				j = j - 1;
			}

			data[j + 1] = key;
		}

		System.out.printf("Insertion Sort Complete\n");
		Utility.log("Insertion sort completed");

	}

	public static void bubbleSort(int[] data) {
		Utility.log("Bubble sort started with array: %s", Arrays.toString(data));
		System.out.printf("Beginning Bubble Sort\n");
		int n = data.length;

		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < n - 1; j++) {

				int temp = data[j];
				data[j] = data[j + 1];
				data[j + 1] = temp;
			}
		}

		System.out.printf("Bubble Sort Complete\n");
		Utility.log("Bubble sort completed");

	}

	public static void mergeSort(int[] data, int n) {
		System.out.printf("Beginning Merge Sort\n");
		Utility.log("Merge sort started with array: %s", Arrays.toString(data));

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

		System.out.printf("Merge Sort Completed\n");
		Utility.log("Merge sort completed");
	}

	public static void merge(int data[], int[] L, int[] R, int l, int r) {
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

	public synchronized static int[]  receiveData(Socket socket) throws IOException {
		status = "Unavailable";
		int[] data = null;
		
		
		try {
			System.out.printf("Util Server Receiving Data\n");
			Utility.log("Receiving data from socket: %s", socket.getInetAddress());
			
			BufferedInputStream b = new BufferedInputStream(socket.getInputStream());
			byte[] bytes = b.readAllBytes();

			String str = new String(bytes, StandardCharsets.UTF_8);
			String[] tokens = str.split(" ");
			data = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				if(!tokens[i].equals("")){
				data[i] = Integer.parseInt(tokens[i]);
				}
			}
			b.close();
			
			System.out.printf("Data Received\n");
			Utility.log("Data Received: %s", Arrays.toString(data));

		} catch (Exception e) {
			System.out.printf("Data Reception Failed\n");
			Utility.log("Exception encountered in receiveData(): %s", e.getMessage());
			e.printStackTrace();
		}

		return data;
	}
	
	public static int[] combine(int[] first, int [] last) {
		int length1 = first.length;
		int length2 = last.length;
		int [] combined = new int[length1 + length2];
		
		System.arraycopy(first, 0, combined, 0, length1);
		System.arraycopy(last, 0, combined, 0, length2);
		return combined;
	}
	public synchronized static void sendData(Socket socket, int[] data) {
		Utility.log("Getting ready to send data to MasterServer");
		status = "Unavailable";
		try {
			//System.out.printf("Attempting to Send Data to Master Server\n");
			
			
			OutputStream o = socket.getOutputStream();
			int[] dataFirst = Arrays.copyOfRange(data, 0, Math.min(5, data.length));
			int[] dataLast = Arrays.copyOfRange(data, Math.max(0, data.length-5), data.length);
			int sum = 0;
			for(int i=0; i<Math.min(5, data.length); i++ ) {
				sum+= data[i];
			}
			for(int i=Math.max(0, data.length-5); i<data.length; i++) {
				sum+= data[i];
			}
			
			String output = "" + sum;
			int[] dataArray = combine(dataFirst, dataLast);
			String dataFile = Arrays.toString(dataArray);
			System.out.printf("Sending data to MasterServer: %s\n", output);
			Utility.log("Sending data: %s", output);
			//System.out.println("Numbers given: " + dataArray);
			

			byte[] bytes = output.getBytes();

			o.write(bytes);
			o.flush();
			o.close();

			System.out.printf("Data Successfully Sent\n");
			Utility.log("Data Successfully Sent: %s (sum: %s)", Arrays.toString(data), output);
		} catch (Exception e) {
			System.out.printf("Write Failed\n");
			Utility.log("Exception encountered in sendData(): %s", e.getMessage());
			e.printStackTrace();
		}

		status = "Available";

	}

	public static void sortData(int[] data, String sortType) {
		Utility.log("Sorting data using method: %s", sortType);
		switch (sortType.toLowerCase()) {
		case "insertion":
			insertionSort(data);
			break;
		case "bubble":
			bubbleSort(data);
			break;
		case "merge":
			mergeSort(data, data.length);
			break;
		default:
			System.out.printf("Try between options: \n");
			System.out.printf("insertion, bubble or merge\n");
		}
	}

	private void listenToMulticast(String status) {
		Thread multicastThread = new Thread(new Runnable() {
			public void run() {
				try (MulticastSocket socket = new MulticastSocket(6000)) {
					InetAddress group = InetAddress.getByName("224.0.0.22");
					NetworkInterface netIf = NetworkInterface.getByName("eth0");
					socket.joinGroup(new InetSocketAddress(group, 6000), netIf);

					byte[] buffer = new byte[1024];
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					System.out.printf("Listening for multicast messages \n");
					Utility.log("Listening for multicast messages on 224.0.0.22:6000");

					InetAddress ad = null;

					while (running) {
						socket.receive(packet);
						String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
						System.out.printf("Message Received: %s\n",message);
						Utility.log("Message Recieved: %s", message);

						// response
						Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
						while(n.hasMoreElements()){
							NetworkInterface net = n.nextElement();

							if(net.getName().equals("eth0")){
								Enumeration<InetAddress> a = net.getInetAddresses();

								while(a.hasMoreElements()){
									ad = a.nextElement();
								}

								
							}
						}
						String hostAddress = ad.getHostAddress();
						System.out.println(hostAddress);
						String response= null;
					  response = hostAddress + " " + portNo + " " +  status;
						byte[] responseByte = response.getBytes(StandardCharsets.UTF_8);
						DatagramPacket reply = new DatagramPacket(responseByte, responseByte.length,
								packet.getAddress(), packet.getPort());
						socket.send(reply);
						Utility.log("Responded with message: %s", response);
					}
					socket.leaveGroup(new InetSocketAddress(group, 6000), netIf);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		multicastThread.start();

	}

	public static void main(String args[]) {

		if (args.length > 0) {
			int port = Integer.parseInt(args[0]);
			String sort = args[1];
			Utility u = new Utility(port, sort);
			u.start();
			
		} else {
			System.out.printf("Please enter port # and sorting type\n");
		}

	}

}

class ClientHandler implements Runnable {
	private Socket clientSocket;
	private String sortType;

	public ClientHandler(Socket clientSocket, String sortType) {
		this.clientSocket = clientSocket;
		this.sortType = sortType;
	}

	@Override
	public void run() {
		Utility.log("ClientHandler started for socket: %s", clientSocket.getInetAddress().getHostAddress());
		try {
			int data[] = Utility.receiveData(clientSocket);
			Utility.sortData(data, sortType);
			Utility.log("SOrting data for client %s:", clientSocket.getInetAddress());
			//int result = data.length;
			String masterIP= clientSocket.getInetAddress().getHostAddress();
			int masterPort = 6500;
			try{
				System.out.printf("Attempting to Connect to Master Server\n");
				Utility.log("Attempting to connect to MasterServer at %s %d",masterIP, masterPort);
				Socket returnSocket = new Socket(masterIP, masterPort);
					Utility.sendData(returnSocket, data);
					clientSocket.close();
			}catch(Exception e){
				System.out.printf("Connection to Master Server failed\n");
				Utility.log("Exception encountered in ClientHandler run() attempting to connect to MasterServer",e.getMessage());
				e.printStackTrace();
			}
			
		} catch (Exception ex) {
			Utility.log("Exception encounter in ClientHandler run()", ex.getMessage());
			ex.printStackTrace();
		}
	}

}
