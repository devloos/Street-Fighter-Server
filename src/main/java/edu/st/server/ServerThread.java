package edu.st.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import edu.st.common.test.*;
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

            while (true) {
                String message = input.readLine();

                if (message == null) {
                    continue;
                }

                Packet<Message> packet = SerializerFactory.getSerializer().deserialize(message);

                if (packet == null) {
                    continue;
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
