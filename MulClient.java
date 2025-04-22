import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class MulClient{

public void receive(){
    try{

        //establish new multicast socket and set listening ip, address, and network interface "eth0" means ethernet
        MulticastSocket s = new MulticastSocket(6000);
        InetAddress a = InetAddress.getByName("224.0.0.22");
        InetSocketAddress address = new InetSocketAddress(a, 6000);
        NetworkInterface n = NetworkInterface.getByName("eth0");

        //join listening group to receive all messages to group
        s.joinGroup(address, n);

        //create empty datagram to receive incoming messages (buffer size does not have to be 1000)
        byte[] buffer = new byte[1000];
        DatagramPacket p = new DatagramPacket(buffer, 0, buffer.length);
        System.out.println("Awaiting packet");
        //wait to receive code (puts thread to sleep so beware if you want to put a timer)
        s.receive(p);

        System.out.println("Packet Received");

        //convert data received to byte array
        byte[] bytes = p.getData();

        //convert received bytes to string
        String str = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(str);
        
        //send return message (need i.p., port, and status (locked or available) in that order)
        String message = "Hello back";
        //sleep just in case to be sure master is ready (may not be necessary)
        Thread.sleep(2000);

        //create packet and send
        DatagramPacket packet  = new DatagramPacket(message.getBytes(), message.length(), p.getAddress(), p.getPort());
        System.out.println("Sending packet");
            s.send(packet);

        //close out socket    
        s.close();
    }catch(Exception e){
        e.printStackTrace();
    }
}

public static void main(String args[]){
    MulClient m = new MulClient();
    m.receive();
}


}
