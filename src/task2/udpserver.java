package task2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class udpserver {
    public static void main(String[] args) throws IOException {
        DatagramSocket serverSocket = new DatagramSocket(10000);
        byte[] receiveBuf = new byte[2048];
        Random random = new Random();
        double lossRate = 0.2; // 设丢包率
        

        //模拟连接建立
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            serverSocket.receive(receivePacket);

            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            if ("SYN".equals(received)) {
                System.out.println("接收到: SYN");

                byte[] synAckBuf = "SYN-ACK".getBytes();
                DatagramPacket synAckPacket = new DatagramPacket(synAckBuf, synAckBuf.length, clientAddress, clientPort);
                serverSocket.send(synAckPacket);
                System.out.println("发送: SYN-ACK");

                serverSocket.receive(receivePacket);
                String ackResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if ("ACK".equals(ackResponse)) {
                    System.out.println("接收到: ACK");
                    break;
                } else {
                    System.out.println("未接收到正确的 ACK，连接失败");
                }
            }
        }
        //数据传输
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            serverSocket.receive(receivePacket);

            byte[] data = receivePacket.getData();
            short seqNo = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF)); // 从字节数组中提取 seqNo
            byte ver = data[2]; // 提取版本号
            String content = new String(data, 3, 200).trim(); // 提取内容并去除空白字符

            System.out.println("收到消息: Seq no: " + seqNo + ", Ver: " + ver + ", Content: " + content);

            if (random.nextDouble() > lossRate) {
                String response = "Response to message with Seq no: " + seqNo;
                byte[] sendBuf = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, receivePacket.getAddress(), receivePacket.getPort());
                serverSocket.send(sendPacket);
                System.out.println("响应消息: " + response);
            } else {
                System.out.println("模拟丢包，未响应");
            }

            if (seqNo == 12) {
                break;
            }
        }
        //模拟断开连接

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            serverSocket.receive(receivePacket);

            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            if ("FIN".equals(received)) {
                System.out.println("接收到: FIN");

                byte[] finAckBuf = "FIN-ACK".getBytes();
                DatagramPacket finAckPacket = new DatagramPacket(finAckBuf, finAckBuf.length, clientAddress, clientPort);
                serverSocket.send(finAckPacket);
                System.out.println("发送: FIN-ACK");

                serverSocket.receive(receivePacket);
                String ackResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if ("ACK".equals(ackResponse)) {
                    System.out.println("接收到: ACK");
                    break;
                } else {
                    System.out.println("未接收到正确的 ACK，关闭失败");
                }
            }
        }

        serverSocket.close();
    }
}
