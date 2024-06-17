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
        double lossRate = 0.2; // �趪����
        

        //ģ�����ӽ���
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            serverSocket.receive(receivePacket);

            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            if ("SYN".equals(received)) {
                System.out.println("���յ�: SYN");

                byte[] synAckBuf = "SYN-ACK".getBytes();
                DatagramPacket synAckPacket = new DatagramPacket(synAckBuf, synAckBuf.length, clientAddress, clientPort);
                serverSocket.send(synAckPacket);
                System.out.println("����: SYN-ACK");

                serverSocket.receive(receivePacket);
                String ackResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if ("ACK".equals(ackResponse)) {
                    System.out.println("���յ�: ACK");
                    break;
                } else {
                    System.out.println("δ���յ���ȷ�� ACK������ʧ��");
                }
            }
        }
        //���ݴ���
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            serverSocket.receive(receivePacket);

            byte[] data = receivePacket.getData();
            short seqNo = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF)); // ���ֽ���������ȡ seqNo
            byte ver = data[2]; // ��ȡ�汾��
            String content = new String(data, 3, 200).trim(); // ��ȡ���ݲ�ȥ���հ��ַ�

            System.out.println("�յ���Ϣ: Seq no: " + seqNo + ", Ver: " + ver + ", Content: " + content);

            if (random.nextDouble() > lossRate) {
                String response = "Response to message with Seq no: " + seqNo;
                byte[] sendBuf = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, receivePacket.getAddress(), receivePacket.getPort());
                serverSocket.send(sendPacket);
                System.out.println("��Ӧ��Ϣ: " + response);
            } else {
                System.out.println("ģ�ⶪ����δ��Ӧ");
            }

            if (seqNo == 12) {
                break;
            }
        }
        //ģ��Ͽ�����

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            serverSocket.receive(receivePacket);

            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            if ("FIN".equals(received)) {
                System.out.println("���յ�: FIN");

                byte[] finAckBuf = "FIN-ACK".getBytes();
                DatagramPacket finAckPacket = new DatagramPacket(finAckBuf, finAckBuf.length, clientAddress, clientPort);
                serverSocket.send(finAckPacket);
                System.out.println("����: FIN-ACK");

                serverSocket.receive(receivePacket);
                String ackResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if ("ACK".equals(ackResponse)) {
                    System.out.println("���յ�: ACK");
                    break;
                } else {
                    System.out.println("δ���յ���ȷ�� ACK���ر�ʧ��");
                }
            }
        }

        serverSocket.close();
    }
}
