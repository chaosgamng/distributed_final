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

//This class needs added threading capabilities and locking as well as receiving broadcast messages

public class Utility extends Thread {
	private static final Logger LOGGER = Logger.getLogger(Utility.class.getName());
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
		String status = "Currently Running";
		
		try {
			serverSocket = new ServerSocket(portNo);
			System.out.println("Server Started @ " + portNo);
			
			listenToMulticast(status);

			while (running) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Connection Successful");

				Thread t = new Thread(new ClientHandler(clientSocket, sortType));
				t.start();
			}

		} catch (Exception e) {
			System.out.println("Failed to Connect");
			e.printStackTrace();
			LOGGER.severe("An error has occurred");
			// serverSocket.close();
			status = "Process Failed";
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
			System.out.println("Data Reception Failed");
			e.printStackTrace();
		}

		return data;
	}

	public synchronized static void sendData(Socket socket, int[] data) {

		try {
			System.out.println("Attempting connection");
			OutputStream o = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(o, true);
			for (int i : data) {
				writer.print(i + " ");

				System.out.println("Success");
			}
			writer.flush();
			System.out.println("Data Sent");
		} catch (Exception e) {
			System.out.println("Write Failed");
			e.printStackTrace();
		}

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
			System.out.println("Try between options: ");
			System.out.println("insertion, bubble or merge");
		}
	}

	private void listenToMulticast(String status) {
		Thread multicastThread = new Thread(new Runnable() {
			public void run() {

				try (MulticastSocket socket = new MulticastSocket(6000)) {
					InetAddress group = InetAddress.getByName("224.0.0.22");
					NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
					socket.joinGroup(new InetSocketAddress(group, 6000), netIf);

					byte[] buffer = new byte[1024];
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					System.out.println("Listening for multicast messages ");

					while (running) {
						socket.receive(packet);
						String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
						System.out.println("Message Received: " + message);

						// response
						
						String hostAddress = group.getHostAddress();
						String response= null;
					  response = hostAddress+ " " + 6000 + " " + status;
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
			System.out.println("Please enter port # and sorting type");
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
			Utility.sendData(clientSocket, data);
			clientSocket.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
