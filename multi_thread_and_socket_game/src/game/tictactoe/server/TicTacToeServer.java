package game.tictactoe.server;
import java.net.ServerSocket;
import java.util.Scanner;

/**
 * A server for a network multi-player tic tac toe game.  
 * Modified and extended from the class presented in Deitel and Deitel "Java How to Program" book.
 * I made a bunch of enhancements and rewrote large sections of the code.
 * The main change is instead of passing *data* between the
 * client and server, I made a TTTP (tic tac toe protocol) which is totally
 * plain text, so you can test the game with Telnet (always a good idea.)
 * The strings that are sent in TTTP are:
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 8)    WELCOME <char>  (char in {X, O})
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 *
 * A second change is that it allows an unlimited number of pairs of
 * players to play.
 */
public class TicTacToeServer {
	static int PORT=0;
	
    public static void main(String[] args) throws Exception {
    	Scanner scan=new Scanner(System.in);
    	System.out.println("서버로 작동시킬 포트를 입력하세요.");
    	PORT=scan.nextInt();
    	scan.close();
    	
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("가위바위보 서버가 작동되었습니다.");
        try {
            while (true) {
                Game game = new Game();
                Game.Player xPlayer = game.new Player(serverSocket.accept(), 'X');
                Game.Player oPlayer = game.new Player(serverSocket.accept(), 'O');
                xPlayer.setOpponent(oPlayer);
                oPlayer.setOpponent(xPlayer);
                game.currentPlayer = xPlayer;
                xPlayer.start();
                oPlayer.start();
            }
        } finally {
            serverSocket.close();
        }
    }
}