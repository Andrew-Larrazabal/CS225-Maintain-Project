/**
 * Displays how complete the user's bracket is.
 * Helps players know what is unfinished before pressing Finalize.
 *
 * @author Bandana
 */

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

// shows how much of the bracket is done
public class ProgressMeter extends HBox {

    private ProgressBar progressBar;
    private Label statusLabel;

    public ProgressMeter() {
        progressBar = new ProgressBar(0);
        statusLabel = new Label("Progress: 0 / 63 picks (0%)");

        // add label + bar to layout
        getChildren().addAll(statusLabel, progressBar);
        setSpacing(10);
    }

    // updates progress based on current bracket
    public void update(Bracket bracket) {
        int filled = 0;

        // count how many picks are filled (ignore empty ones)
        for (int i = 0; i < 63; i++) {
            if (bracket.getBracket().get(i) != null && !bracket.getBracket().get(i).equals("")) {
                filled++;
            }
        }

        int percent = (int) ((double) filled / 63 * 100);

        // update the progress bar
        progressBar.setProgress((double) filled / 63);

        // update the text
        if (filled == 63) {
            statusLabel.setText("Progress: 63 / 63 picks (100%) - Ready to Finalize!");
        } else {
            statusLabel.setText("Progress: " + filled + " / 63 picks (" + percent + "%)");
        }
    }

    // reset everything back to 0
    public void reset() {
        progressBar.setProgress(0);
        statusLabel.setText("Progress: 0 / 63 picks (0%)");
    }
}