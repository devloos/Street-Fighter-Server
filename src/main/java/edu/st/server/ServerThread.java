package edu.st.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import edu.st.common.Util;
import edu.st.common.messages.Message;
import edu.st.common.messages.Packet;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerThread extends Thread {

    private Socket socket = null;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            while (!this.socket.isClosed()) {
                String message = input.readLine();

                if (message == null) {
                    return;
                }

                Packet<Message> packet = Util.deserialize(message);

                if (packet == null) {
                    return;
                }

                Message actualMessage = packet.getMessage();

                // if message invovles current game
                if (actualMessage.getType().contains("MakeMove")) {
                    GameController.addJob(new Pair<Socket, Packet<Message>>(socket, packet));
                } else {
                    RouterThread.addJob(new Pair<Socket, Packet<Message>>(socket, packet));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
