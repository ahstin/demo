package org.example.demo;

import org.example.demo.utils.CloseUtil;
import org.example.demo.utils.DbUtil;
import org.example.demo.utils.TCPReceiveUtil;
import org.example.demo.utils.TCPSendUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * 创建服务器
 * 1、指定端口 使用SeverSocket创建服务器
 * 2、阻塞式等待连接 accept
 * 3、操作：输入输出流操作
 * 4、释放资源
 */

public class RemoteControlServer {

    private static CopyOnWriteArrayList<Client> all = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("-----Server-----");

        try (// 1、指定端口 使用SeverSocket创建服务器
             ServerSocket server = new ServerSocket(6666);
             ServerSocket serverSocket = new ServerSocket(8888);) {
            // 2、阻塞式等待连接 accept
            while (true) {
                Socket client = server.accept();
                Socket socket = serverSocket.accept();
                Client c = new Client(client, socket);
                all.add(c); // 管理所有的成员
                c.run();
                if (c.client.isClosed())
                    c.release();
            }
        }
        // 一般 server不会停止
    }

    static class Client {
        private final Socket client;
        private final Socket secondClient;

        private int uid;

        private boolean isRunning = false;
        private final TCPSendUtil send;
        private final TCPReceiveUtil receive;
        private final TCPSendUtil send2;
        private final TCPReceiveUtil receive2;

        public Client(Socket client, Socket secondClient) {
            this.client = client;
            this.secondClient = secondClient;
            isRunning = true;
            this.send = new TCPSendUtil(client);
            this.receive = new TCPReceiveUtil(client);
            this.send2 = new TCPSendUtil(secondClient);
            this.receive2 = new TCPReceiveUtil(secondClient);

            System.out.println("一个客户建立了链接");
        }

        public void run() {
            new Thread(() -> {
                for (int i = 0; i < 2; i++) {
                    String order = receive.receiveUTF();
                    if (order != null) {
                        System.out.println("接收到的命令" + order);
                        String[] request = order.split(" ");
                        if (request[0].equals(("REMOTECONTROLSTART"))) {
                            selectClient(DbUtil.getID(request[1])).send2.sendUTF("REMOTECONTROLSTART#" + DbUtil.getUserName(uid));
                        }

                        if (request[0].equals(("REJECTREMOTECONTROL"))) {
                            selectClient(DbUtil.getID(request[1])).send2.sendUTF("REMOTECONTROLEND#" + DbUtil.getUserName(uid));
                        }
                        if (request[0].equals("ACCEPTREMOTECONTROL")) {
                            selectClient(DbUtil.getID(request[1])).send2.sendUTF("ACCEPTREMOTECONTROL#" + DbUtil.getUserName(uid));
                        }
                    }
                }
            }).start();
            // 远程投屏
            new Thread(() -> {
                while (isRunning) {
                    String order = receive2.receiveUTF();
                    if (order != null)
                        send2.sendUTF(order);
                }
            }).start();
            // 远程控制
            new Thread(() -> {
                while (isRunning) {
                    byte[] image = receive.receiveImg();
                    if (image != null)
                        send.sendImg(image);
                    // 这个根据自己写的部分按照需要写
                }
            }).start();
        }

        public void stop(){
            isRunning = false;
        }

        public void restart(){
            isRunning = true;
        }

        public void release() {
            isRunning = false;
            CloseUtil.close(client, send, receive, send2, receive2, secondClient);
            all.remove(this);
        }

        public Socket getTargetClient(int targetUid) {
            for (Client c : all) {
                if (c.uid == targetUid)
                    return c.client;
            }
            return null;
        }

        private Client selectClient(int userID) {
            System.out.println(all);
            for (Client c: all) {
                if (userID == c.uid) {
                    return c;
                }
            }
            return null;
        }
    }
}

