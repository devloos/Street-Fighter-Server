package edu.st.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

import edu.st.common.Util;
import edu.st.common.messages.Message;
import edu.st.common.messages.Packet;
import edu.st.common.messages.client.JoinGame;
import edu.st.common.messages.client.MakeMove;
import edu.st.common.messages.server.MoveMade;
import edu.st.common.models.Game;
import edu.st.common.models.GamePair;
import javafx.util.Pair;

public class GameController extends Thread {
  private static volatile ArrayList<Pair<Socket, Packet<Message>>> jobs = new ArrayList<>();
  private static volatile ArrayList<Game> currentGames = new ArrayList<>();

  @Override
  public void run() {
    while (true) {
      if (jobs.isEmpty()) {
        continue;
      }

      // Socket socket = jobs.get(0).getKey();
      Packet<Message> packet = jobs.get(0).getValue();
      Message message = packet.getMessage();
      String channel = packet.getChannel();

      if (message.getType().contains("MakeMove")) {
        MakeMove msg = (MakeMove) message;
        Game game = currentGames.stream().filter(el -> el.getGameId().toString().equals(channel)).findFirst()
            .orElse(null);

        if (game != null) {
          Integer row = msg.getRow();
          Integer col = msg.getCol();

          game.updateBoard(row, col);

          // do lots of validation

          MoveMade moveMade = new MoveMade(row, col);
          Util.println(game.getHostSocket(), moveMade, channel);
          Util.println(game.getPlayerSocket(), moveMade, channel);
        } else {
          System.out.println("GAME NOT FOUND IN GAME CONTROLLER");
        }
      }

      deleteJob();
    }
  }

  public static synchronized void addJob(Pair<Socket, Packet<Message>> pair) {
    jobs.add(pair);
  }

  public static synchronized Game findGame(JoinGame joinGame) {
    Optional<Game> option = currentGames
        .stream()
        .filter(el -> el.getGameId().equals(joinGame.getGameId()))
        .findFirst();

    if (option.isPresent()) {
      return option.get();
    }

    return null;
  }

  public static synchronized void addGame(Game game) {
    currentGames.add(game);
  }

  public static synchronized ArrayList<GamePair> getGames() {
    ArrayList<GamePair> games = new ArrayList<>();

    for (Game game : currentGames) {
      games.add(new GamePair(game.getGameId(), game.getHostname()));
    }

    return games;
  }

  private static synchronized void deleteJob() {
    jobs.remove(0);
  }
}
