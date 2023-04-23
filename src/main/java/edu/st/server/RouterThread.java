package edu.st.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import edu.st.common.serialize.SerializerFactory;
import edu.st.common.test.*;
import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Server Thread processing each connected client
 */
@NoArgsConstructor
@AllArgsConstructor
public class RouterThread extends Thread {

  private static volatile ArrayList<Pair<Socket, Packet<Message>>> jobs = new ArrayList<>();
  private HashMap<String, ArrayList<Socket>> map = new HashMap<String, ArrayList<Socket>>();

  @Override
  public void run() {
    while (true) {
      if (jobs.isEmpty()) {
        continue;
      }

      Socket socket = jobs.get(0).getKey();
      Packet<Message> packet = jobs.get(0).getValue();
      Message message = packet.getMessage();
      String channel = packet.getChannel();

      if (message.getType().contains("Subscribe")) {
        Subscribe subscribe = (Subscribe) message;
        for (String c : subscribe.getChannels()) {
          ArrayList<Socket> list = getSockets(c);

          for (Socket s : list) {
            println(s, new Message(c + " User: " + subscribe.getUsername() + " has joined channel! (Server)"), channel);
          }

          list.add(socket);
          println(socket, new Received(true, c + " You have been subscribed! (Server)"), channel);
        }

        deleteJob();
        continue;
      }

      ArrayList<Socket> list = getSockets(channel);

      for (Socket s : list) {
        if (s != socket) {
          println(s, message, channel);
        }
      }

      deleteJob();
    }
  }

  public static synchronized void addJob(Pair<Socket, Packet<Message>> pair) {
    jobs.add(pair);
  }

  private static synchronized void deleteJob() {
    jobs.remove(0);
  }

  private ArrayList<Socket> getSockets(String channel) {
    if (map.get(channel) == null) {
      map.put(channel, new ArrayList<Socket>());
    }
    return map.get(channel);
  }

  private void println(Socket socket, Message message, String channel) {
    Packet<Message> packet = new Packet<Message>(message, channel);
    try {
      PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
      output.println(SerializerFactory.getSerializer().serialize(packet));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
