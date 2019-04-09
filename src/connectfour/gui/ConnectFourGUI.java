package connectfour.gui;

import connectfour.ConnectFourException;
import connectfour.client.ConnectFourBoard;
import connectfour.client.ConnectFourNetworkClient;
import connectfour.client.Observer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
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
    private Label turnLabel;
    private Label movesLeft;
    private Image p1;
    private Image p2;

    @Override
    public void init() throws ConnectFourException {
        try {
            // get the command line args
            List<String> args = getParameters().getRaw();

            // get host info and port from command line
            String host = args.get(0);
            int port = Integer.parseInt(args.get(1));
            this.board = new ConnectFourBoard();
            this.board.addObserver(this);
            this.client = new ConnectFourNetworkClient(host, port, board);
            this.turnLabel = new Label("");
            this.movesLeft = new Label("");
            this.p1 = new Image(getClass().getResourceAsStream("p1black.png"));
            this.p2 = new Image(getClass().getResourceAsStream("p2red.png"));
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
                buttons[col][row] = new Button();
                buttons[col][row].setGraphic(new ImageView(image));
                buttons[col][row].setOnAction(clickedButton(col));
                gridPane.add(buttons[col][row], col, row);
            }
        }
        disableButtons();

        BorderPane borderPane = new BorderPane();
        borderPane.setPrefSize(456, 456);
        borderPane.setCenter(gridPane);

        turnLabel.setText("NOT YOUR TURN");
        movesLeft.setText("MOVES LEFT: " + board.getMovesLeft());
        BorderPane labels = new BorderPane();
        labels.setLeft(turnLabel);
        labels.setRight(movesLeft);
        borderPane.setBottom(labels);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.setTitle("Connect Four");
        stage.show();
        client.startListener();
    }

    private EventHandler<ActionEvent> clickedButton(int col) {
        return event -> {
            if (board.isMyTurn()) {
                if (board.isValidMove(col)) {
                    client.sendMove(col);
                }
            }
        };
    }

    private void disableButtons() {
        for (int row = 0; row <= ROW; row++) {
            for (int col = 0; col <= COL; col++) {
                buttons[col][row].setDisable(true);
            }
        }
    }

    private void enableButtons() {
        for (int row = 0; row <= ROW; row++) {
            for (int col = 0; col <= COL; col++) {
                buttons[col][row].setDisable(false);
            }
        }
    }

    private void checkBoard() {
        for (int row = 0; row <= ROW; row++) {
            for (int col = 0; col <= COL; col++) {
                if (board.getContents(row, col) == ConnectFourBoard.Move.PLAYER_ONE) {
                    buttons[col][row].setGraphic(new ImageView(p1));
                } else if (board.getContents(row, col) == ConnectFourBoard.Move.PLAYER_TWO) {
                    buttons[col][row].setGraphic(new ImageView(p2));
                }
            }
        }
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
            disableButtons();
            turnLabel.setText("NOT YOUR TURN");
            movesLeft.setText("MOVES LEFT: " + board.getMovesLeft());
            checkBoard();
            ConnectFourBoard.Status status = board.getStatus();
            switch (status) {
                case ERROR:
                    client.error("ERROR");
                    turnLabel.setText("ERROR");
                    break;
                case I_WON:
                    turnLabel.setText("YOU WON");
                    break;
                case I_LOST:
                    turnLabel.setText("YOU LOST");
                    break;
                case TIE:
                    turnLabel.setText("YOU TIED");
                    break;
            }
        } else {
            enableButtons();
            turnLabel.setText("YOUR TURN");
            movesLeft.setText("MOVES LEFT: " + board.getMovesLeft());
            checkBoard();
        }
    }

    /**
     * Called by the model, client.ConnectFourBoard, whenever there is a state change
     * that needs to be updated by the GUI.
     *
     * @param connectFourBoard board
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
