package task2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket ;
import java.net.InetAddress;
import java.net.SocketTimeoutException;


import java.io.*;
import java.net.*;
import java.util.*;

public class udpclient {
    public static void main(String[] args) throws IOException {
        System.out.println("���Ͷ�");
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(100); // ���ó�ʱʱ��Ϊ100ms

        BufferedReader bufr = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("�������������ip��ַ: ");
        String serverIp = bufr.readLine();
        System.out.print("������������Ķ˿ں�: ");
        int serverPort = Integer.parseInt(bufr.readLine());

        byte[] receiveBuf = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);

        List<Long> rttList = new ArrayList<>(); // �洢 RTT ʱ��

        // ģ����������
        byte[] synBuf = "SYN".getBytes();
        DatagramPacket synPacket = new DatagramPacket(synBuf, synBuf.length, InetAddress.getByName(serverIp), serverPort);
        clientSocket.send(synPacket);
        System.out.println("����: SYN");

        long startTime = System.nanoTime(); // ��¼��ʼʱ��

        clientSocket.receive(receivePacket);
        long synAckReceivedTime = System.nanoTime(); // ��¼ SYN-ACK ����ʱ��
        String synAckResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
        if ("SYN-ACK".equals(synAckResponse)) {
            System.out.println("���յ�: SYN-ACK");

            byte[] ackBuf = "ACK".getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length, InetAddress.getByName(serverIp), serverPort);
            clientSocket.send(ackPacket);
            System.out.println("����: ACK");
        } else {
            System.out.println("δ���յ���ȷ�� SYN-ACK������ʧ��");
            clientSocket.close();
            return;
        }

        // �������ݴ���׶�
        long startTimeTransmission = System.nanoTime(); // ��¼���ݴ��俪ʼʱ��
        int sentPackets = 0;
        int receivedPackets = 0;
        int lostPackets = 0;
        long maxRTT = Long.MIN_VALUE;
        long minRTT = Long.MAX_VALUE;
        long totalRTT = 0;

        for (int i = 0; i < 12; i++) {
            short seqNo = (short) (i + 1);  // ��Ϣ���
            byte ver = 1;  // �汾��
            String content = "Content for message " + (i + 1);  // ��Ϣ����
            byte[] contentBytes = content.getBytes();  // ������ת��Ϊ�ֽ�����

            byte[] buf = new byte[203]; // 2 bytes for seqNo, 1 byte for ver, 200 bytes for content
            buf[0] = (byte) (seqNo >> 8); // �� seqNo �ĸ�8λ���� buf[0]
            buf[1] = (byte) seqNo; // �� seqNo �ĵ�8λ���� buf[1]
            buf[2] = ver; // ���汾�Ŵ��� buf[2]
            System.arraycopy(contentBytes, 0, buf, 3, Math.min(contentBytes.length, 200)); // �������ֽ����鿽���� buf[3] ��ʼ��λ��

            DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(serverIp), serverPort);

            boolean packetReceived = false;
            boolean resend = false;
            int resendCount = 0;
            long rtt = 0;

            do {
                try {
                    long sendTime = System.nanoTime(); // ��¼����ʱ��
                    clientSocket.send(dp);
                    sentPackets++;
                    System.out.println("������Ϣ: Seq no: " + seqNo + ", Ver: " + ver + ", Content: " + content);

                    clientSocket.receive(receivePacket);
                    long receiveTime = System.nanoTime(); // ��¼����ʱ��
                    rtt = receiveTime - sendTime; // ���� RTT
                    rttList.add(rtt); // �� RTT �����б�
                    totalRTT += rtt; // �ۼ� RTT
                    maxRTT = Math.max(maxRTT, rtt);
                    minRTT = Math.min(minRTT, rtt);

                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("��������Ӧ: " + receivedMessage);
                    receivedPackets++;

                    packetReceived = true;
                } catch (SocketTimeoutException e) {
                    System.out.println("���� " + (i + 1) + " ��ʱ������Ӧ���ش���...");
                    lostPackets++;
                    resend = true;
                    resendCount++;
                    if (resendCount == 2) {
                        System.out.println("�ش�����ʧ�ܣ����������ش�");
                        break;
                    }
                }
            } while (!packetReceived && resend);

            if (!packetReceived) {
                continue; // ���û���յ���Ӧ�Ҳ���Ҫ�ش����������һ��ѭ��
            }

            // ���� RTT
            System.out.println("RTT for message " + (i + 1) + ": " + rtt / 1e6 + " ms");
        }

        long endTimeTransmission = System.nanoTime(); // ��¼���ݴ������ʱ��

        // ģ���Ĵλ���
        byte[] finBuf = "FIN".getBytes();
        DatagramPacket finPacket = new DatagramPacket(finBuf, finBuf.length, InetAddress.getByName(serverIp), serverPort);
        clientSocket.send(finPacket);
        System.out.println("����: FIN");

        clientSocket.receive(receivePacket);
        long finAckReceivedTime = System.nanoTime(); // ��¼ FIN-ACK ����ʱ��
        String finAckResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
        if ("FIN-ACK".equals(finAckResponse)) {
            System.out.println("���յ�: FIN-ACK");

            byte[] ackBuf = "ACK".getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length, InetAddress.getByName(serverIp), serverPort);
            clientSocket.send(ackPacket);
            System.out.println("����: ACK");
        } else {
            System.out.println("δ���յ���ȷ�� FIN-ACK���ر�ʧ��");
        }

        clientSocket.close();

        // ���������Ϣ
        long totalTime = endTimeTransmission - startTime;
        double lossRate = (double) lostPackets / sentPackets * 100;
        double avgRTT = (double) totalRTT / receivedPackets / 1e6; // ƽ�� RTT����λ����
        double stdDevRTT = calculateStdDev(rttList) / 1e6; // RTT ��׼���λ����
        long serverResponseTime = finAckReceivedTime - synAckReceivedTime; // Server ��Ӧʱ��

        // ��ӡ������Ϣ
        System.out.println("\n�����ܡ���Ϣ:");
        System.out.println("�� ���յ��� UDP packets ��Ŀ: " + receivedPackets);
        System.out.println("�� ������: " + String.format("%.2f", lossRate) + "%");
        System.out.println("�� ��� RTT: " + maxRTT / 1e6 + " ms");
        System.out.println("�� ��С RTT: " + minRTT / 1e6 + " ms");
        System.out.println("�� ƽ�� RTT: " + String.format("%.2f", avgRTT) + " ms");
        System.out.println("�� RTT �ı�׼��: " + String.format("%.2f", stdDevRTT) + " ms");
        System.out.println("�� Server ��������Ӧʱ��: " + serverResponseTime / 1e6 + " ms");
    }

    // �����׼��
    private static double calculateStdDev(List<Long> list) {
        double sum = 0.0;
        double mean;
        double num = 0.0;

        for (Long value : list) {
            sum += value;
        }
        mean = sum / list.size();

        for (Long value : list) {
            num += Math.pow(value - mean, 2);
        }

        return Math.sqrt(num / (list.size() - 1));
    }
}


