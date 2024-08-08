package nz.ac.auckland.se206.states;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.speech.TextToSpeech;

/**
 * The Guessing state of the game. Handles the logic for when the player is making a guess about the
 * profession of the characters in the game.
 */
public class Guessing implements GameState {

  private final GameStateContext context;
  private MediaPlayer mediaPlayer;

  /**
   * Constructs a new Guessing state with the given game state context.
   *
   * @param context the context of the game state
   */
  public Guessing(GameStateContext context) {
    this.context = context;
  }

  /**
   * Handles the event when a rectangle is clicked. Checks if the clicked rectangle is a customer
   * and updates the game state accordingly.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    if (rectangleId.equals("pictureRec") || rectangleId.equals("caseRec")) {
      try {
        playMedia("/sounds/suspects.mp3");
      } catch (URISyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return;
    }

    // String clickedProfession = context.getProfession(rectangleId);
    if (rectangleId.equals(context.getRectIdToGuess())) {
      TextToSpeech.speak(
          "Correct! You won! This is the criminal! Thank you for solving the case Dr. Watson.");
    } else {
      TextToSpeech.speak(
          "You lost! This was an innocent person. I wish Mr. Holmes was here to solve the case.");
    }
    context.setState(context.getGameOverState());
  }

  /**
   * Handles the event when the guess button is clicked. Since the player has already guessed, it
   * notifies the player.
   *
   * @throws IOException if there is an I/O error
   */
  @Override
  public void handleGuessClick() throws IOException {
    try {
      playMedia("/sounds/already.mp3");
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void playMedia(String filePath) throws URISyntaxException {
    Media introSound = new Media(App.class.getResource(filePath).toURI().toString());
    mediaPlayer = new MediaPlayer(introSound);
    mediaPlayer.play();
  }
}
