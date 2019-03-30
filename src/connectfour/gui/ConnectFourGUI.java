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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.util.List;

/**
 * A JavaFX GUI for the networked Connect Four game.
 *
 * @author James Heloitis @ RIT CS
 * @author Sean Strout @ RIT CS
 * @author YOUR NAME HERE
 */
public class ConnectFourGUI extends Application implements Observer<ConnectFourBoard> {

    private ConnectFourBoard board;
    private ConnectFourNetworkClient client;
    private static final int COL = 7;
    private static final int ROW = 6;

    @Override
    public void init() throws ConnectFourException {
        try {
            // get the command line args
            List<String> args = getParameters().getRaw();

            // get host info and port from command line
            String host = args.get(0);
            int port = Integer.parseInt(args.get(1));
            board = new ConnectFourBoard();
            client = new ConnectFourNetworkClient(host, port, board);
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
        Image image = new Image(new FileInputStream("/Users/cameronriu/Documents/Spring 2019/Comp Sci 2/Labs/lab08-cmr6689/src/connectfour/gui/empty.png"));
        VBox vbox = new VBox();
        GridPane gridPane = new GridPane();

        for (int row = 0; row < COL; row++) {
            for (int col = 0; col < ROW; col++) {
                //Button button = new Button("", new ImageView(image));
                //vbox.getChildren().add(button);
                vbox.getChildren().add(new ImageView(image));
            }
            Button button = new Button("", vbox);
            button.setPadding(new Insets(1, 1, 1, 1));
            gridPane.addColumn(row, button);
            vbox = new VBox();
        }


        BorderPane borderPane = new BorderPane();
        borderPane.setPrefSize(456, 456);
        borderPane.setCenter(gridPane);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
        client.startListener();
        if (board.isMyTurn()) {
            int loc = GridPane.getColumnIndex(vbox.getChildren().get(MouseEvent.MOUSE_CLICKED));
            System.out.println(loc);
            client.sendMove(loc);
            client.moveMade(String.valueOf(loc));
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
        // TODO
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
