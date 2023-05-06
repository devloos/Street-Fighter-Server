package edu.st.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import edu.st.common.messages.Message;
import edu.st.common.messages.Packet;
import edu.st.common.messages.Received;
import edu.st.common.messages.Subscribe;
import edu.st.common.messages.client.CreateGame;
import edu.st.common.messages.client.JoinGame;
import edu.st.common.messages.server.GameJoined;
import edu.st.common.messages.server.GameList;
import edu.st.common.messages.server.PlayerJoined;
import edu.st.common.models.Game;
import edu.st.common.serialize.SerializerFactory;
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
        for (String channelToSub : subscribe.getChannels()) {
          getSockets(channelToSub).add(socket);

          if (channelToSub.equals("/gamelist")) {
            GameList gamelist = new GameList(GameController.getGames());
            for (Socket s : getSockets("/gamelist")) {
              println(s, gamelist, "/gamelist");
            }
          }
        }
      }

      if (message.getType().contains("CreateGame")) {
        CreateGame createGame = (CreateGame) message;
        Game game = new Game(UUID.randomUUID(), createGame.getHostname(), socket, null, null);

        GameController.addGame(game);

        GameList gamelist = new GameList(GameController.getGames());
        for (Socket s : getSockets("/gamelist")) {
          println(s, gamelist, "/gamelist");
        }

        ArrayList<Socket> list = new ArrayList<>();
        list.add(socket);
        map.put(game.getGameId().toString(), list);
      }

      if (message.getType().contains("JoinGame")) {
        JoinGame joinGame = (JoinGame) message;

        Game game = GameController.findGame(joinGame);

        if (game == null) {
          System.out.println("ERROR GAME NOT FOUND");
          continue;
        }

        String gameId = game.getGameId().toString();

        game.setPlayername(joinGame.getUsername());
        game.setPlayerSocket(socket);
        ArrayList<Socket> list = getSockets(gameId);
        list.add(socket);

        PlayerJoined playerJoined = new PlayerJoined(joinGame.getUsername());
        println(list.get(0), playerJoined, gameId);

        GameJoined gameJoined = new GameJoined(game.getHostname());
        println(list.get(1), gameJoined, gameId);

        // temporary
        // we send a Game Started to both players
        // actually send them to avatar selection
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
