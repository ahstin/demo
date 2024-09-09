package org.example.demo;


import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import org.example.demo.utils.TCPReceiveUtil;
import org.example.demo.utils.TCPSendUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.peer.RobotPeer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.reflect.Method;

import sun.awt.ComponentFactory;

//import static org.example.demo.Main.loginController;

public class Client {

    public static String name = null;
    public static String uid = null;

    public static  String  avatarUrl="/touxiang.png";  //头像路径
    public static  int controlTimes = 0 ;  //操控/touxiang.png次数
    public static double goodRatingPercentage = 0.0;//好评率

    public static int friendNumb = 0;
    public static ArrayList<String>friendNames = new ArrayList<>();

    public static ArrayList<String> selectFriendName = new ArrayList<>();
    public static String sex = "无";
    public static String country = "中国";
    public static String province = "北京";
    public static String birthday = "2000-01-01";
    public static int age = 24;
    public static String signature = "摆烂";

    public static GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    public static Toolkit toolkit = Toolkit.getDefaultToolkit();
    public static RobotPeer robot = (toolkit instanceof ComponentFactory ? ((ComponentFactory)toolkit).createRobot(screen) : null);
    public static Method method = robot.getClass().getDeclaredMethod("getRGBPixels",int.class,int.class,int.class,int.class,int[].class);
    public static Dimension screenSize = toolkit.getScreenSize();
    public static int Width = 2*screenSize.width;
    public static int Height = 2*screenSize.height;
    public static int[] rgbs = new int[Width*Height];
    
    public static Socket client = null;
    public static Socket secondClient = null;

    private Thread recieveImgThread;
    private Thread sendImgThread;
    private Thread robotThread;

    public static Map<String, Stage> chatWindows = new HashMap<>();


    public void init() {
        sendImgThread = new Thread(() -> {
            TCPSendUtil sendUtil = new TCPSendUtil(Client.client);
            while (true) {
                try {
                    method.invoke(robot,0,0,Width,Height,rgbs);
                    byte[] imageBytes = toByteArr(rgbs);
                    sendUtil.sendImg(imageBytes);
                } catch (AWTException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

         recieveImgThread = new Thread(()->{
            TCPReceiveUtil receive = new TCPReceiveUtil(Client.client);
            while (true) {
                byte[] imageData = receive.receiveImg();
                ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
                try {
                    BufferedImage image = ImageIO.read(bais);
                    //loginController.updateImage(image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        robotThread = new Thread(()->{
            TCPReceiveUtil receiveUtil = new TCPReceiveUtil(Client.secondClient);
            try {
                Robot robot = new Robot();

                while (true) {
                    String[] order = receiveUtil.receiveUTF().split("#");
                    String type = order[0];
                    switch (type) {
                        case "mouseMoved":
                            int x = Integer.parseInt(order[1]);
                            int y = Integer.parseInt(order[2]);

                            int windowSizeWidth = Integer.parseInt(order[3]);
                            int windowSizeHeight = Integer.parseInt(order[4]);
                            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                            robot.mouseMove(x * screenSize.width / windowSizeWidth, y * screenSize.height / windowSizeHeight);

                            System.out.println("x: " + x + ", y: " + y);
                            break;

                        case "mousePressed":
                            int mouseButtonPress = Integer.parseInt(order[1]);
                            //robot.mousePress(mouseButtonPress);
                            System.out.println("mouseButtonPress: " + mouseButtonPress);
                            break;

                        case "mouseReleased":
                            int mouseButtonRelease = Integer.parseInt(order[1]);
                            //robot.mouseRelease(mouseButtonRelease);
                            System.out.println("mouseReleased: " + mouseButtonRelease);
                            break;

                        case "mouseDragged":
                            int dragX = Integer.parseInt(order[1]);
                            int dragY = Integer.parseInt(order[2]);
                            // robot.mouseMove(dragX, dragY);
                            System.out.println("dragX: " + dragX + ", dragY: " + dragY);
                            break;

                        case "mouseWheel":
                            int wheelAmount = Integer.parseInt(order[1]);
                            //robot.mouseWheel(wheelAmount);
                            System.out.println("wheelAmount: " + wheelAmount);
                            break;

                        case "keyPressed":
                            int keyCodePress = Integer.parseInt(order[1]);
                            //robot.keyPress(keyCodePress);
                            System.out.println("keyCodePress: " + keyCodePress);
                            break;

                        case "keyReleased":
                            int keyCodeRelease = Integer.parseInt(order[1]);
                            //robot.keyRelease(keyCodeRelease);
                            System.out.println("keyCodeRelease: " + keyCodeRelease);
                            break;

                        default:
                            System.out.println("Unknown event type: " + type);
                            break;
                    }
                }
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void startRemoteHash() {
        sendImgThread.start();
        recieveImgThread.start();
    }

    public void startRemoteControl(){
        sendImgThread.start();
        recieveImgThread.start();
        robotThread.start();
    }

    public void stopRemoteControl(){
        sendImgThread.interrupt();
        recieveImgThread.interrupt();
        robotThread.interrupt();
    }

    public void stopRemoteHash(){
        sendImgThread.interrupt();
        recieveImgThread.interrupt();
    }

    // 手动更新头像的方法
    public static void updateAvatar(ImageView imageView) {
        if (Client.avatarUrl != null && !Client.avatarUrl.isEmpty()) {
            File avatarFile = new File(Client.avatarUrl);
            if (avatarFile.exists()) {
                imageView.setImage(new Image(avatarFile.toURI().toString()));
                System.out.println("头像已更新: " + Client.avatarUrl);
            } else {
                System.out.println("头像文件不存在: " + Client.avatarUrl);
            }
        }
    }
    public static byte[] toByteArr(int[] i) {
        byte[] b = new byte[4*i.length];
        for(int j=0;j<i.length;j++) {
            b[4*j] = (byte) ((i[j] >>> 24) & 0xFF);
            b[4*j+1] = (byte) ((i[j] >>> 16) & 0xFF);
            b[4*j+2] = (byte) ((i[j] >>> 8) & 0xFF);
            b[4*j+3] = (byte) (i[j] & 0xFF);
        }
        return b;
    }
}


