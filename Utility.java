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


//This class needs added threading capabilities and locking as well as receiving broadcast messages

public class Utility extends Thread {
	static String status = null;
	private int portNo;
	private String sortType;
	private static final Lock entryLock = new ReentrantLock();
	private ServerSocket serverSocket;

	boolean running = true;

	Utility(int portNo, String sortType) {
		this.portNo = portNo;
		this.sortType = sortType;
	}

	@Override
	public void run() {
		status= "Available";
		
		try {
			serverSocket = new ServerSocket(portNo);
			System.out.printf("Server Started @ %d%n", portNo);
			
			listenToMulticast(status);

			while (running) {
				Socket clientSocket = serverSocket.accept();
				System.out.printf("Connection Successful%n");

				Thread t = new Thread(new ClientHandler(clientSocket, sortType));
				t.start();
			}

		} catch (Exception e) {
			System.out.printf("Failed to Connect\n");
			e.printStackTrace();
			
			// serverSocket.close();
			status = "Available";
		}
		
	}

	public static void insertionSort(int[] data) {
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

	public static void bubbleSort(int[] data) {
		int n = data.length;

		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < n - 1; j++) {

				int temp = data[j];
				data[j] = data[j + 1];
				data[j + 1] = temp;
			}
		}

	}

	public static void mergeSort(int[] data, int n) {

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
		status = "Not Available";
		int[] data = null;
		try {
			BufferedInputStream b = new BufferedInputStream(socket.getInputStream());
			byte[] bytes = b.readAllBytes();

			String str = new String(bytes, StandardCharsets.UTF_8);
			String[] tokens = str.split(" ");
			data = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				data[i] = Integer.parseInt(tokens[i]);
			}
			b.close();

		} catch (Exception e) {
			System.out.printf("Data Reception Failed%n");
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
		
		status = "Not Available";
		try {
			System.out.printf("Attempting connection%n");
			OutputStream o = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(o, true);
			int[] dataFirst = Arrays.copyOfRange(data, 0, Math.min(5, data.length));
			int[] dataLast = Arrays.copyOfRange(data, Math.max(0, data.length-5), data.length);
			int sum = 0;
			for(int i=0; i<Math.min(5, data.length); i++ ) {
				sum+= data[i];
			}
			for(int i=Math.max(0, data.length-5); i<data.length; i++) {
				sum+= data[i];
			}
			
			
			int[] dataArray = combine(dataFirst, dataLast);
			String dataFile = Arrays.toString(dataArray);
			System.out.printf("Sending data:%n%d%n", sum);
			System.out.printf("Numbers given: %n%d%n",Arrays.toString(dataArray));
			writer.print(sum);
			//writer.print(dataFile);

				System.out.printf("Success%n");
			writer.flush();
			System.out.printf("Data Sent%n");
		} catch (Exception e) {
			System.out.printf("Write Failed%n");
			e.printStackTrace();
		}
		status = "Available";
		

	}

	public static void sortData(int[] data, String sortType) {
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
			System.out.printf("Try between options: %n");
			System.out.printf("insertion, bubble or merge%n");
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
					System.out.printf("Listening for multicast messages %n");

					while (running) {
						socket.receive(packet);
						String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
						System.out.printf("Message Received: %n", message);

						// response
						
						String hostAddress = group.getHostAddress();
						String response= null;
					  response = hostAddress + " " + 6000 +" " +  status;
						byte[] responseByte = response.getBytes(StandardCharsets.UTF_8);
						DatagramPacket reply = new DatagramPacket(responseByte, responseByte.length,
								packet.getAddress(), packet.getPort());
						socket.send(reply);
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
			System.out.printf("Please enter port # and sorting type%n");
			System.out.printf("Sorts: Insertion, Bubble, or Merge%n");
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
		try {
			int data[] = Utility.receiveData(clientSocket);
			Utility.sortData(data, sortType);
			int result = data.length;
			String masterIP= clientSocket.getInetAddress().getHostAddress();
			int masterPort = 6500;
			try(Socket returnSocket = new Socket(masterIP, masterPort);
					OutputStream os =  returnSocket.getOutputStream()){
				os.write(Integer.toString(result).getBytes(StandardCharsets.UTF_8));
				os.flush();
			}
			
			clientSocket.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
