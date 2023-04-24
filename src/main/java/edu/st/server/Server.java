package edu.st.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import edu.st.common.messages.Message;

public class Server {

    public static final int port = 8000;
    public List<Message> messageList = new ArrayList<Message>();

    public static void main(String[] args) {
        RouterThread routerThread = new RouterThread();
        routerThread.start();

        try (ServerSocket serversocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serversocket.accept();

                System.out.println("Socket Connected: " + socket);

                ServerThread serverThread = new ServerThread(socket);
                serverThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
