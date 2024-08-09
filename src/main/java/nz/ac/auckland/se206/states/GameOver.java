package nz.ac.auckland.se206.states;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;

/**
 * The GameOver state of the game. Handles interactions after the game has ended, informing the
 * player that the game is over and no further actions can be taken.
 */
public class GameOver implements GameState {

  private MediaPlayer mediaPlayer;

  /**
   * Constructs a new GameOver state with the given game state context.
   *
   * @param context the context of the game state
   */
  public GameOver(GameStateContext context) {}

  /**
   * Handles the event when a rectangle is clicked. Informs the player that the game is over and
   * provides the profession of the clicked character if applicable.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    // If sound is playing, turn off the sound
    if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
      mediaPlayer.stop();
    }

    if (rectangleId.equals("caseRec") || rectangleId.equals("pictureRec")) {
      return;
    }
    try {
      playMedia("/sounds/already.mp3");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * Handles the event when the guess button is clicked. Informs the player that the game is over
   * and no further guesses can be made.
   *
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleGuessClick() throws IOException {
    // If sound is playing, turn off the sound
    if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
      mediaPlayer.stop();
    }

    try {
      playMedia("/sounds/already.mp3");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  private void playMedia(String filePath) throws URISyntaxException {
    Media introSound = new Media(App.class.getResource(filePath).toURI().toString());
    mediaPlayer = new MediaPlayer(introSound);
    mediaPlayer.play();
  }
}
