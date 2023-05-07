package edu.st.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import edu.st.common.messages.Message;
import edu.st.common.messages.Packet;
import edu.st.common.messages.client.JoinGame;
import edu.st.common.messages.client.MakeMove;
import edu.st.common.messages.server.MoveMade;
import edu.st.common.models.Game;
import edu.st.common.models.GamePair;
import edu.st.common.models.Token;
import edu.st.common.serialize.SerializerFactory;
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
          println(game.getHostSocket(), moveMade, channel);
          println(game.getPlayerSocket(), moveMade, channel);
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

  private boolean isWinner(ArrayList<ArrayList<Token>> board) {
    Token t1 = null;
    Token t2 = null;
    Token t3 = null;
    for (ArrayList<Token> row : board) {
      t1 = row.get(0);
      t2 = row.get(1);
      t3 = row.get(2);

      if (checkTiles(t1, t2, t3)) {
        return true;
      }
    }

    for (int col = 0; col < board.size(); ++col) {
      t1 = board.get(0).get(col);
      t2 = board.get(1).get(col);
      t3 = board.get(2).get(col);

      if (checkTiles(t1, t2, t3)) {
        return true;
      }
    }

    t1 = board.get(0).get(0);
    t2 = board.get(1).get(1);
    t3 = board.get(2).get(2);
    if (checkTiles(t1, t2, t3)) {
      return true;
    }

    t1 = board.get(0).get(2);
    t2 = board.get(1).get(1);
    t3 = board.get(2).get(0);
    if (checkTiles(t1, t2, t3)) {
      return true;
    }

    return false;
  }

  private boolean isBoardFull(ArrayList<ArrayList<Token>> board) {
    for (ArrayList<Token> row : board) {
      if (row.contains(null)) {
        return false;
      }
    }
    return true;
  }

  private boolean checkTiles(Token t1, Token t2, Token t3) {
    if (t1 == null || t2 == null || t3 == null) {
      return false;
    }

    if (t1 == t2 && t2 == t3) {
      return true;
    }

    return false;
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
