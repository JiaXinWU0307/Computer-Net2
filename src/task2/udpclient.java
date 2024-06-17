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
        System.out.println("发送端");
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(100); // 设置超时时间为100ms

        BufferedReader bufr = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("请输入服务器的ip地址: ");
        String serverIp = bufr.readLine();
        System.out.print("请输入服务器的端口号: ");
        int serverPort = Integer.parseInt(bufr.readLine());

        byte[] receiveBuf = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);

        List<Long> rttList = new ArrayList<>(); // 存储 RTT 时间

        // 模拟三次握手
        byte[] synBuf = "SYN".getBytes();
        DatagramPacket synPacket = new DatagramPacket(synBuf, synBuf.length, InetAddress.getByName(serverIp), serverPort);
        clientSocket.send(synPacket);
        System.out.println("发送: SYN");

        long startTime = System.nanoTime(); // 记录开始时间

        clientSocket.receive(receivePacket);
        long synAckReceivedTime = System.nanoTime(); // 记录 SYN-ACK 接收时间
        String synAckResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
        if ("SYN-ACK".equals(synAckResponse)) {
            System.out.println("接收到: SYN-ACK");

            byte[] ackBuf = "ACK".getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length, InetAddress.getByName(serverIp), serverPort);
            clientSocket.send(ackPacket);
            System.out.println("发送: ACK");
        } else {
            System.out.println("未接收到正确的 SYN-ACK，连接失败");
            clientSocket.close();
            return;
        }

        // 进入数据传输阶段
        long startTimeTransmission = System.nanoTime(); // 记录数据传输开始时间
        int sentPackets = 0;
        int receivedPackets = 0;
        int lostPackets = 0;
        long maxRTT = Long.MIN_VALUE;
        long minRTT = Long.MAX_VALUE;
        long totalRTT = 0;

        for (int i = 0; i < 12; i++) {
            short seqNo = (short) (i + 1);  // 消息序号
            byte ver = 1;  // 版本号
            String content = "Content for message " + (i + 1);  // 消息内容
            byte[] contentBytes = content.getBytes();  // 将内容转换为字节数组

            byte[] buf = new byte[203]; // 2 bytes for seqNo, 1 byte for ver, 200 bytes for content
            buf[0] = (byte) (seqNo >> 8); // 将 seqNo 的高8位存入 buf[0]
            buf[1] = (byte) seqNo; // 将 seqNo 的低8位存入 buf[1]
            buf[2] = ver; // 将版本号存入 buf[2]
            System.arraycopy(contentBytes, 0, buf, 3, Math.min(contentBytes.length, 200)); // 将内容字节数组拷贝到 buf[3] 开始的位置

            DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(serverIp), serverPort);

            boolean packetReceived = false;
            boolean resend = false;
            int resendCount = 0;
            long rtt = 0;

            do {
                try {
                    long sendTime = System.nanoTime(); // 记录发送时间
                    clientSocket.send(dp);
                    sentPackets++;
                    System.out.println("发送消息: Seq no: " + seqNo + ", Ver: " + ver + ", Content: " + content);

                    clientSocket.receive(receivePacket);
                    long receiveTime = System.nanoTime(); // 记录接收时间
                    rtt = receiveTime - sendTime; // 计算 RTT
                    rttList.add(rtt); // 将 RTT 加入列表
                    totalRTT += rtt; // 累计 RTT
                    maxRTT = Math.max(maxRTT, rtt);
                    minRTT = Math.min(minRTT, rtt);

                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("服务器响应: " + receivedMessage);
                    receivedPackets++;

                    packetReceived = true;
                } catch (SocketTimeoutException e) {
                    System.out.println("请求 " + (i + 1) + " 超时，无响应，重传中...");
                    lostPackets++;
                    resend = true;
                    resendCount++;
                    if (resendCount == 2) {
                        System.out.println("重传两次失败，放弃本次重传");
                        break;
                    }
                }
            } while (!packetReceived && resend);

            if (!packetReceived) {
                continue; // 如果没有收到响应且不需要重传，则继续下一次循环
            }

            // 处理 RTT
            System.out.println("RTT for message " + (i + 1) + ": " + rtt / 1e6 + " ms");
        }

        long endTimeTransmission = System.nanoTime(); // 记录数据传输结束时间

        // 模拟四次挥手
        byte[] finBuf = "FIN".getBytes();
        DatagramPacket finPacket = new DatagramPacket(finBuf, finBuf.length, InetAddress.getByName(serverIp), serverPort);
        clientSocket.send(finPacket);
        System.out.println("发送: FIN");

        clientSocket.receive(receivePacket);
        long finAckReceivedTime = System.nanoTime(); // 记录 FIN-ACK 接收时间
        String finAckResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
        if ("FIN-ACK".equals(finAckResponse)) {
            System.out.println("接收到: FIN-ACK");

            byte[] ackBuf = "ACK".getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length, InetAddress.getByName(serverIp), serverPort);
            clientSocket.send(ackPacket);
            System.out.println("发送: ACK");
        } else {
            System.out.println("未接收到正确的 FIN-ACK，关闭失败");
        }

        clientSocket.close();

        // 计算汇总信息
        long totalTime = endTimeTransmission - startTime;
        double lossRate = (double) lostPackets / sentPackets * 100;
        double avgRTT = (double) totalRTT / receivedPackets / 1e6; // 平均 RTT，单位毫秒
        double stdDevRTT = calculateStdDev(rttList) / 1e6; // RTT 标准差，单位毫秒
        long serverResponseTime = finAckReceivedTime - synAckReceivedTime; // Server 响应时间

        // 打印汇总信息
        System.out.println("\n【汇总】信息:");
        System.out.println("● 接收到的 UDP packets 数目: " + receivedPackets);
        System.out.println("● 丢包率: " + String.format("%.2f", lossRate) + "%");
        System.out.println("● 最大 RTT: " + maxRTT / 1e6 + " ms");
        System.out.println("● 最小 RTT: " + minRTT / 1e6 + " ms");
        System.out.println("● 平均 RTT: " + String.format("%.2f", avgRTT) + " ms");
        System.out.println("● RTT 的标准差: " + String.format("%.2f", stdDevRTT) + " ms");
        System.out.println("● Server 的整体响应时间: " + serverResponseTime / 1e6 + " ms");
    }

    // 计算标准差
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


