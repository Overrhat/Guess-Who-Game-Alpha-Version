package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.speech.TextToSpeech;

public class CrimeroomController {

  @FXML private Label timerLabel;
  @FXML private Rectangle oldManRec;
  @FXML private Rectangle manRec;
  @FXML private Rectangle womanRec;
  @FXML private Rectangle pictureRec;
  @FXML private Rectangle caseRec;
  @FXML private Button guessButton;
  @FXML private Button sendButton;
  @FXML private TextArea chatText;
  @FXML private TextField inputText;

  // Timer variables
  private volatile boolean running = true;
  private volatile boolean paused = false;
  private volatile boolean timesUp = false;
  private final int totalSeconds = 123; // 2 minutes and 3 seconds for loading at first
  private volatile long remainingSeconds = totalSeconds; // Track remaining time
  private Thread timerThread;

  // Media player for intro sound
  private MediaPlayer mediaPlayer;

  // Initialize the context
  private static GameStateContext context = new GameStateContext();

  // Variables for chat
  private String profession;
  private ChatCompletionRequest chatCompletionRequest;

  public void initialize() throws URISyntaxException, ApiProxyException {
    // Play the intro media
    playMedia("/sounds/intro.mp3");

    // Start the timer
    startTimer();
  }

  private void startTimer() {
    // Create a new daemon thread for the timer
    timerThread =
        new Thread(
            () -> {
              Instant endTime = Instant.now().plus(Duration.ofSeconds(totalSeconds));
              while (running) {
                if (!paused) {
                  endTime = Instant.now().plus(Duration.ofSeconds(remainingSeconds));
                  while (!paused && running && Instant.now().isBefore(endTime)) {
                    long secondsLeft = Duration.between(Instant.now(), endTime).getSeconds();
                    remainingSeconds = secondsLeft; // Update remaining time
                    Platform.runLater(() -> updateTimerLabel((int) remainingSeconds));

                    // Sleep for 1 second between updates
                    try {
                      Thread.sleep(1000);
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                    }
                  }

                  // If the loop ends and time is up
                  if (running && !paused && !timesUp) {
                    Platform.runLater(
                        () -> {
                          timesUp = true;
                          forceGuess();
                        });
                  }
                }

                // Sleep briefly to avoid busy-waiting
                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            });

    // Set the thread as a daemon so it exits with the application
    timerThread.setDaemon(true);
    timerThread.start();
  }

  private void updateTimerLabel(int seconds) {
    int minutes = seconds / 60;
    int secs = seconds % 60;
    timerLabel.setText(String.format("%02d:%02d", minutes, secs));
  }

  // Method to force the player to make a guess
  private void forceGuess() {
    // Say time is up
    try {
      playMedia("/sounds/guessIn10.mp3");
    } catch (URISyntaxException e) {
      // Handle exception
      e.printStackTrace();
    }

    context.setState(context.getGuessingState());

    Timer timer = new Timer();
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            // Check if the state is already GameOverState
            if (context.getGameState() == context.getGameOverState()) {
              return; // Stop the function immediately
            }

            try {
              playMedia("/sounds/timesUp.mp3");
            } catch (URISyntaxException e) {
              // Handle exception
              e.printStackTrace();
            }
            context.setState(context.getGameOverState());
          }
        },
        15000 // 15 seconds delay as there is 5 seconds talking
        );
  }

  // Method to pause the timer
  public void pauseTimer() {
    this.paused = true;
  }

  // Method to resume the timer
  public void resumeTimer() {
    if (this.paused) {
      this.paused = false;
      startTimer(); // Restart the timer thread to continue from where it paused
    }
  }

  // Method to handle the click event

  /**
   * Handles mouse clicks on rectangles representing people in the room.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void handleRectangleClick(MouseEvent event) throws IOException {
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    context.handleRectangleClick(event, clickedRectangle.getId());
  }

  /**
   * Handles the guess button click event.
   *
   * @param event the action event triggered by clicking the guess button
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void handleGuessClick(ActionEvent event) throws IOException {
    pauseTimer();
    context.handleGuessClick();
  }

  // Chat controller methods

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    return PromptEngineering.getPrompt(this.profession);
  }

  /**
   * Sets the profession for the chat context and initializes the ChatCompletionRequest.
   *
   * @param profession the profession to set
   */
  public void setProfession(String profession) {
    this.profession = profession;
    switch (this.profession) {
      case "oldMan":
        chatText.appendText("******Now chatting with Old Man******" + "\n\n");
        break;
      case "man":
        chatText.appendText("******Now chatting with Young Man******" + "\n\n");
        break;
      case "woman":
        chatText.appendText("******Now chatting with Woman******" + "\n\n");
        break;
    }
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setMaxTokens(100);
      runGpt(new ChatMessage("system", getSystemPrompt()));
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    if (msg.getRole().equals("user")) {
      chatText.appendText("Dr. Watson (You) : " + msg.getContent() + "\n\n");
    } else {
      switch (this.profession) {
        case "oldMan":
          chatText.appendText("Edgar Thompson (Old Man) : " + msg.getContent() + "\n\n");
          return;
        case "man":
          chatText.appendText("Alex Carter (Young Man) : " + msg.getContent() + "\n\n");
          return;
        case "woman":
          chatText.appendText("Lena Stone (Woman) : " + msg.getContent() + "\n\n");
          return;
        default:
          chatText.appendText(msg.getRole() + ": " + msg.getContent() + "\n\n");
      }
    }
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    chatCompletionRequest.addMessage(msg);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      TextToSpeech.speak(result.getChatMessage().getContent(), profession);
      chatCompletionRequest.addMessage(result.getChatMessage());
      appendChatMessage(result.getChatMessage());
      return result.getChatMessage();
    } catch (ApiProxyException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    if (context.getGameState() == context.getGameOverState()
        || timesUp
        || context.getGameState() == context.getGuessingState()) {
      return;
    }

    String message = inputText.getText().trim();
    // System.out.println("User message: " + message); // Debug
    if (message.isEmpty()) {
      return;
    }
    inputText.clear();
    ChatMessage msg = new ChatMessage("user", message);
    appendChatMessage(msg);
    runGpt(msg);
  }

  // Method to play media from a given path
  private void playMedia(String filePath) throws URISyntaxException {
    Media introSound = new Media(App.class.getResource(filePath).toURI().toString());
    mediaPlayer = new MediaPlayer(introSound);
    mediaPlayer.play();
  }
}
