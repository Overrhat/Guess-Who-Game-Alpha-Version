package nz.ac.auckland.se206.states;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;

/**
 * The GameStarted state of the game. Handles the initial interactions when the game starts,
 * allowing the player to chat with characters and prepare to make a guess.
 */
public class GameStarted implements GameState {

  private final GameStateContext context;
  private MediaPlayer mediaPlayer;

  /**
   * Constructs a new GameStarted state with the given game state context.
   *
   * @param context the context of the game state
   */
  public GameStarted(GameStateContext context) {
    this.context = context;
  }

  /**
   * Handles the event when a rectangle is clicked. Depending on the clicked rectangle, it either
   * provides an introduction or transitions to the chat view.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    switch (rectangleId) {
      case "oldManRec":
        App.openChat("oldMan");
        return;
      case "manRec":
        App.openChat("man");
        return;
      case "womanRec":
        App.openChat("woman");
        return;
      case "pictureRec":
        // Play the guess media
        String picturePath = "/sounds/picture.mp3";
        Media pictureSound = null;
        try {
          pictureSound = new Media(App.class.getResource(picturePath).toURI().toString());
        } catch (URISyntaxException e) {
          e.printStackTrace();
        }
        mediaPlayer = new MediaPlayer(pictureSound);
        mediaPlayer.play();
        return;
      case "caseRec":
        // Play the guess media
        String casePath = "/sounds/caseFound.mp3";
        Media caseSound = null;
        try {
          caseSound = new Media(App.class.getResource(casePath).toURI().toString());
        } catch (URISyntaxException e) {
          e.printStackTrace();
        }
        mediaPlayer = new MediaPlayer(caseSound);
        mediaPlayer.play();
        return;
    }
  }

  /**
   * Handles the event when the guess button is clicked. Prompts the player to make a guess and
   * transitions to the guessing state.
   *
   * @throws IOException if there is an I/O error
   * @throws URISyntaxException
   */
  @Override
  public void handleGuessClick() throws IOException {
    // Play the guess media
    String guessPath = "/sounds/guess.mp3";
    Media guessSound = null;
    try {
      guessSound = new Media(App.class.getResource(guessPath).toURI().toString());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    mediaPlayer = new MediaPlayer(guessSound);
    mediaPlayer.play();

    context.setState(context.getGuessingState());
  }
}
