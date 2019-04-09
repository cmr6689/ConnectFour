package connectfour.gui;

import connectfour.ConnectFourException;
import connectfour.client.ConnectFourBoard;
import connectfour.client.ConnectFourNetworkClient;
import connectfour.client.Observer;
import connectfour.server.ConnectFour;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javax.sound.sampled.spi.AudioFileReader;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * A JavaFX GUI for the networked Connect Four game.
 *
 * @author James Heloitis @ RIT CS
 * @author Sean Strout @ RIT CS
 * @author Cameron Riu
 */
public class ConnectFourGUI extends Application implements Observer<ConnectFourBoard>{

    private ConnectFourBoard board;
    private ConnectFourNetworkClient client;
    private static final int COL = 6;
    private static final int ROW = 5;
    private Button[][] buttons;
    private Label myTurn;
    private ImageView p1;
    private ImageView p2;
    private boolean clicked;
    private ConnectFourBoard.Move currentPiece;

    @Override
    public void init() throws ConnectFourException, FileNotFoundException {
        try {
            // get the command line args
            List<String> args = getParameters().getRaw();

            // get host info and port from command line
            String host = args.get(0);
            int port = Integer.parseInt(args.get(1));
            this.board = new ConnectFourBoard();
            this.board.addObserver(this);
            this.client = new ConnectFourNetworkClient(host, port, board);
            this.myTurn = new Label("");
            this.p1 = new ImageView(new Image(getClass().getResourceAsStream("p1black.png")));
            this.p2 = new ImageView(new Image(getClass().getResourceAsStream("p2red.png")));
            this.currentPiece = ConnectFourBoard.Move.PLAYER_ONE;
            this.clicked = false;
        } catch(NumberFormatException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Construct the layout for the game.
     *
     * @param stage container (window) in which to render the GUI
     * @throws Exception if there is a problem
     */
    public void start( Stage stage ) throws Exception {
        Image image = new Image(getClass().getResourceAsStream("empty.png"));
        GridPane gridPane = new GridPane();

        buttons = new Button[COL+1][ROW+1];

        for (int row = 0; row <= ROW; row++) {
            for (int col = 0; col <= COL; col++) {
                buttons[col][row] = new Button("image");
                buttons[col][row].setOnAction(clickedButton(col));
                gridPane.add(buttons[col][row], col, row);
            }
        }

        BorderPane borderPane = new BorderPane();
        borderPane.setPrefSize(456, 456);
        borderPane.setCenter(gridPane);

        myTurn.setAlignment(Pos.CENTER);
        borderPane.setBottom(myTurn);


        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
        client.startListener();



    }

    public EventHandler<ActionEvent> clickedButton(int col)  throws FileNotFoundException {
        return event -> {
            if (board.isMyTurn()) {
                client.sendMove(col);
                client.moveMade(String.valueOf(col));
                board.moveMade(col);
                clicked = true;
            }
        };
    }

    /**
     * GUI is closing, so close the network connection. Server will get the message.
     */
    @Override
    public void stop() {
        client.close();
    }

    /**
     * Do your GUI updates here.
     */
    private void refresh() {
        if (!board.isMyTurn()) {
            for (int row = 0; row <= ROW; row++) {
                for (int col = 0; col <= COL; col++) {
                    buttons[col][row].setDisable(true);
                }
            }
            myTurn.setText("NOT YOUR TURN");
            myTurn.setAlignment(Pos.CENTER);
            ConnectFourBoard.Status status = board.getStatus();
            switch (status) {
                case ERROR:
                    client.error("ERROR");
                    break;
            }
            this.currentPiece = currentPiece.opponent();
            clicked = false;
        } else {
            for (int row = 0; row <= ROW; row++) {
                for (int col = 0; col <= COL; col++) {
                    buttons[col][row].setDisable(false);
                }
            }
            myTurn.setText("YOUR TURN");
            myTurn.setAlignment(Pos.CENTER);
            System.out.println("CLICKED");
            for (int row = 0; row <= ROW; row++) {
                for (int col = 0; col <= COL; col++) {
                    if (board.getContents(row, col) == ConnectFourBoard.Move.PLAYER_ONE) {
                        buttons[col][row] = new Button("", p1);
                    }
                }
            }
            this.currentPiece = currentPiece.opponent();
            /*if (board.isMyTurn()) {
                if (board.isValidMove(col)) {
                    for (int i = ROW; i >= 0; i++) {
                        if (board.getContents(i, col) == ConnectFourBoard.Move.NONE) {
                            if (.equals(ConnectFourBoard.Move.PLAYER_ONE)) {
                                buttons[col][i].setGraphic(p1);
                            } else if (currentPiece.equals(ConnectFourBoard.Move.PLAYER_TWO)) {
                                buttons[col][i].setGraphic(p2);
                            }
                            client.sendMove(col);
                            board.moveMade(col);
                            this.currentPiece = this.currentPiece.opponent();
                            board.didMyTurn();
                            break;
                        }
                    }
                }
            }*/
        }
    }

    /**
     * Called by the model, client.ConnectFourBoard, whenever there is a state change
     * that needs to be updated by the GUI.
     *
     * @param connectFourBoard
     */
    @Override
    public void update(ConnectFourBoard connectFourBoard) {
        if ( Platform.isFxApplicationThread() ) {
            this.refresh();
        }
        else {
            Platform.runLater( () -> this.refresh() );
        }
    }

    /**
     * The main method expects the host and port.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ConnectFourGUI host port");
            System.exit(-1);
        } else {
            Application.launch(args);
        }
    }
}
