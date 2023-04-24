package edu.st.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import edu.st.common.messages.Message;
import edu.st.common.messages.Packet;
import edu.st.common.serialize.*;
import javafx.util.Pair;
import lombok.Getter;

@Getter
public class ServerThread extends Thread {

    private Socket socket = null;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            while (!this.socket.isClosed()) {
                String message = input.readLine();

                if (message == null) {
                    return;
                }

                Packet<Message> packet = SerializerFactory.getSerializer().deserialize(message);

                if (packet == null) {
                    return;
                }

                RouterThread.addJob(new Pair<Socket, Packet<Message>>(socket, packet));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
