import java.util.ArrayList;
import java.util.HashMap;

import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Richard and Ricardo on 5/3/17.
 */
public class BracketPane extends BorderPane {

    /**
     * Reference to the graphical representation of the nodes within the bracket.
     */
    private static ArrayList<BracketNode> nodes;
    /**
     * Maps the text "buttons" to it's respective grid-pane
     */
    private HashMap<StackPane, Pane> panes;
    /**
     * Reference to the current bracket.
     */
    private Bracket currentBracket;
    /**
     * Reference to the master bracket for simulation results (used for coloring predictions)
     */
    private Bracket comparisonBracket;

    //CLEANUP(Josh): Import existing TournamentInfo instead of re-creating it from disk every time
    private TournamentInfo teamInfo;
    /**
     * Reference to active subtree within current bracket.
     */
    private int displayedSubtree;
    /**
     * Keeps track of whether or not bracket has been finalized.
     */
    private boolean finalized;
    /**
     * Important logical simplification for allowing for code that is easier
     * to maintain.
     */
    private HashMap<BracketNode, Integer> bracketMap = new HashMap<>();
    /**
     * Reverse of the above;
     */
    private HashMap<Integer, BracketNode> nodeMap = new HashMap<>();

    /**
     * Clears the entries of a team future wins
     *
     * @param treeNum
     */
    private void clearAbove(int treeNum) {
        int nextTreeNum = (treeNum - 1) / 2;
        if (!nodeMap.get(nextTreeNum).getName().isEmpty()) {
            nodeMap.get(nextTreeNum).setName("");
            clearAbove(nextTreeNum);
        }
    }


    public void clear(){
        clearSubtree(displayedSubtree);
    }

    /**
     * Handles clicked events for BracketNode objects
     */
    private EventHandler<MouseEvent> clicked = mouseEvent -> {
        //conditional added by matt 5/7 to differentiate between left and right mouse click
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            BracketNode n = (BracketNode) mouseEvent.getSource();
            int treeNum = bracketMap.get(n);
            int nextTreeNum = (treeNum - 1) / 2;
            if (!nodeMap.get(nextTreeNum).getName().equals(n.getName())) {
                currentBracket.removeAbove((nextTreeNum));
                clearAbove(treeNum);
                nodeMap.get((bracketMap.get(n) - 1) / 2).setName(n.getName());
                currentBracket.moveTeamUp(treeNum);
            }
        }
    };
    /**
     * Handles mouseEntered events for BracketNode objects
     */
    private EventHandler<MouseEvent> enter = mouseEvent -> {
        BracketNode tmp = (BracketNode) mouseEvent.getSource();
        tmp.setStyle("-fx-background-color: lightcyan;");
        tmp.setEffect(new InnerShadow(10, Color.LIGHTCYAN));
    };

    /**
     * Handles mouseExited events for BracketNode objects
     */
    private EventHandler<MouseEvent> exit = mouseEvent -> {
        BracketNode tmp = (BracketNode) mouseEvent.getSource();
        tmp.setStyle(null);
        tmp.setEffect(null);

    };

    public GridPane getFullPane() {
        return fullPane;
    }

    private GridPane center;
    private GridPane fullPane;


    private Button clearButton;

    /**
     * TODO: Reduce. reuse, recycle!
     * Initializes the properties needed to construct a bracket.
     */
    public BracketPane(Bracket currentBracket, TournamentInfo teamInfo, Bracket comparisonBracket, Button clearButton) {
        System.out.println("DEBUG BracketPane constructor - comparisonBracket is null: " + (comparisonBracket == null));
        this.clearButton = clearButton;
        displayedSubtree=0;
        this.currentBracket = currentBracket;
        this.comparisonBracket = comparisonBracket;
        //CLEANUP(Josh): Import existing TournamentInfo instead of re-creating it from disk every time
        this.teamInfo = teamInfo;

        bracketMap = new HashMap<>();
        nodeMap = new HashMap<>();
        panes = new HashMap<>();
        nodes = new ArrayList<>();
        ArrayList<DivisionPane> divisionPanes = new ArrayList<>();

        center = new GridPane();

        ArrayList<StackPane> buttons = new ArrayList<>();
        buttons.add(customButton("EAST"));
        buttons.add(customButton("WEST"));
        buttons.add(customButton("MIDWEST"));
        buttons.add(customButton("SOUTH"));
        buttons.add(customButton("FULL"));

        for (int m = 0; m < buttons.size() - 1; m++) {
            divisionPanes.add(new DivisionPane(3 + m, comparisonBracket));
            panes.put(buttons.get(m), divisionPanes.get(m));
        }
        Pane finalPane = createFinalFour();
        //buttons.add(customButton("FINAL"));
        //panes.put(buttons.get(5), finalPane);
        fullPane = new GridPane();
        GridPane gp1 = new GridPane();
        gp1.add(divisionPanes.get(0), 0, 0);
        gp1.add(divisionPanes.get(1), 0, 1);
        GridPane gp2 = new GridPane();
        gp2.add(divisionPanes.get(2), 0, 0);
        gp2.add(divisionPanes.get(3), 0, 1);
        gp2.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        fullPane.add(gp1, 0, 0);
        fullPane.add(finalPane, 1, 0, 1, 2);
        fullPane.add(gp2, 2, 0);
        fullPane.setAlignment(Pos.CENTER);
        panes.put(buttons.get((buttons.size() - 1)), fullPane);
        finalPane.toBack();

        // Initializes the button grid
        GridPane buttonGrid = new GridPane();
        for (int i = 0; i < buttons.size(); i++)
            buttonGrid.add(buttons.get(i), 0, i);
        buttonGrid.setAlignment(Pos.CENTER);

        // set default center to the button grid
        this.setCenter(buttonGrid);

        for (StackPane t : buttons) {
            t.setOnMouseEntered(mouseEvent -> {
                t.setStyle("-fx-background-color: lightblue;");
                t.setEffect(new InnerShadow(10, Color.LIGHTCYAN));
            });
            t.setOnMouseExited(mouseEvent -> {
                t.setStyle("-fx-background-color: orange;");
                t.setEffect(null);
            });
            t.setOnMouseClicked(mouseEvent -> {
                setCenter(null);
                /**
                 * @update Grant & Tyler
                 * 			panes are added as ScrollPanes to retain center alignment when moving through full-view and region-view
                 */
                center.add(new ScrollPane(panes.get(t)), 0, 0);
                center.setAlignment(Pos.CENTER);
                setCenter(center);
                //Grant 5/7 this is for clearing the tree it kind of works
                int index = buttons.indexOf(t);
                if (index == 4) { // FULL
                    displayedSubtree = 0;
                } else {
                    displayedSubtree = index + 3;
                }
                clearButton.setDisable(displayedSubtree == 0);
            });
        }

    }

    /**
     * Helpful method to retrieve our magical numbers
     *
     * @param root the root node (3,4,5,6)
     * @param pos  the position in the tree (8 (16) , 4 (8) , 2 (4) , 1 (2))
     * @return The list representing the valid values.
     */
    public ArrayList<Integer> helper(int root, int pos) {
        ArrayList<Integer> positions = new ArrayList<>();
        int base = 0;
        int tmp = (root * 2) + 1;
        if (pos == 8) base = 3;
        else if (pos == 4) base = 2;
        else if (pos == 2) base = 1;
        for (int i = 0; i < base; i++) tmp = (tmp * 2) + 1;
        for (int j = 0; j < pos * 2; j++) positions.add(tmp + j);
        return positions; //                while ((tmp = ((location * 2) + 1)) <= 127) ;
    }

    /**
     * Checks if a prediction at the given index is correct
     * @param index The bracket index to check
     * @return true if prediction matches the simulated result, false otherwise
     */
    private boolean isPredictionCorrect(int index) {
        if (comparisonBracket == null) return false;
        String predicted = currentBracket.getBracket().get(index);
        String actual = comparisonBracket.getBracket().get(index);
        return !predicted.isEmpty() && predicted.equals(actual);
    }

    /**
     * Returns the number of points awarded for a correct prediction at this bracket position
     * @param index The bracket index
     * @return points for correct prediction at this round
     */
    private int pointsForIndex(int index) {
        if (index == 0) return 32;  // Finals
        if (index < 3) return 16;   // Semifinals
        if (index < 7) return 8;    // Quarterfinals
        if (index < 15) return 4;   // Sweet 16
        if (index < 31) return 2;   // Round of 32
        if (index < 63) return 1;   // Round of 64
        return 0;
    }

    /**
     * Sets the current bracket to,
     *
     * @param target The bracket to replace currentBracket
     */
    public void setBracket(Bracket target) {
        currentBracket = target;
    }

    /**
     * Clears the sub tree from,
     *
     * @param position The position to clear after
     */
    public void clearSubtree(int position) {
        currentBracket.resetSubtree(position);
        // FEATURE(Cesar): Clear only the displayed division without returning to division selector.
        ArrayList<Integer> affectedIndices = new ArrayList<>();

        affectedIndices.add(position);
        affectedIndices.addAll(helper(position, 8));
        affectedIndices.addAll(helper(position, 4));
        affectedIndices.addAll(helper(position, 2));
        affectedIndices.addAll(helper(position, 1));

        for (int index : affectedIndices) {
            if (nodeMap.containsKey(index)) {
                nodeMap.get(index).setName(currentBracket.getBracket().get(index));
            }
        }
    }

    /**
     * Resets the bracket-display
     */
    public void resetBracket() {
        currentBracket.resetSubtree(0);
    }

    /**
     * Checks if currently viewing the full bracket
     */
    public boolean isFullView() {
        return displayedSubtree == 0;
    }

    /**
     * Requests a message from current bracket to tell if the bracket
     * has been completed.
     *
     * @return True if completed, false otherwise.
     */
    public boolean isComplete() {
        return currentBracket.isComplete();
    }

    /**
     * @return true if the current-bracket is complete and the value
     * of finalized is also true.
     */
    public boolean isFinalized() {
        return currentBracket.isComplete() && finalized;
    }

    /**
     * @param isFinalized The value to set finalized to.
     */
    public void setFinalized(boolean isFinalized) {
        finalized = isFinalized && currentBracket.isComplete();
    }

    /**
     * Returns a custom "Button" with specified
     *
     * @param name The name of the button
     * @return pane The stack-pane "button"
     */
    private StackPane customButton(String name) {
        StackPane pane = new StackPane();
        Rectangle r = new Rectangle(100, 50, Color.TRANSPARENT);
        Text t = new Text(name);
        t.setTextAlignment(TextAlignment.CENTER);
        pane.getChildren().addAll(r, t);
        pane.setStyle("-fx-background-color: orange;");
        return pane;
    }

    // Pranshu worked on this: widen final four/championship nodes and preserve color feedback on these nodes
    public Pane createFinalFour() {
        Pane finalPane = new Pane();
        //CLEANUP(Josh): Use a loop
        int[] xPos = {162, 75, 250};
        int[] yPos = {300, 400, 400};

        for (int i = 0; i < xPos.length; i++) {
            String teamName = currentBracket.getBracket().get(i);
            BracketNode nodeFinal = new BracketNode(teamName, xPos[i], yPos[i], 200, 30, false);
            finalPane.getChildren().add(nodeFinal);
            bracketMap.put(nodeFinal, i);
            nodeMap.put(i, nodeFinal);
            
            // Update display with scores and colors if comparison bracket exists
            if (comparisonBracket != null) {
                nodeFinal.setNameWithScore(teamName, i);
            }
            
            nodeFinal.setOnMouseClicked(clicked);
            nodeFinal.setOnMouseDragEntered(enter);
            nodeFinal.setOnMouseDragExited(exit);
            String existingStyle = nodeFinal.getStyle();
            nodeFinal.setStyle((existingStyle == null ? "" : existingStyle) + "-fx-border-color: darkblue;");
        }
        finalPane.setMinWidth(Region.USE_COMPUTED_SIZE);

        return finalPane;
    }

    /**
     * Creates the graphical representation of a subtree.
     * Note, this is a vague model. TODO: MAKE MODULAR
     */
    //CLEANUP(Josh): Give a more clear name
    private class DivisionPane extends Pane {
        //BUGFIX(Josh): Increase width to prevent longer team names from overflowing box
        //Pranshu worked on this: widen bracket nodes to better fit long names
        //TODO: Calculate needed width based on longest team name, instead of hardcoding?
        private static final int NODE_WIDTH = 200;
        private static final int INITIAL_MATCHES = 8;
        private static final int PADDING = 25;
        private int location;
        private Bracket comparisonBracketRef;

        public DivisionPane(int location, Bracket comparisonBracketRef) {
            this.location = location;
            this.comparisonBracketRef = comparisonBracketRef;
            System.out.println("DEBUG DivisionPane created - comparisonBracketRef is null: " + (comparisonBracketRef == null));
            //CLEANUP(Josh): Use while loop, calculate parameters algorithmically instead of hardcoding
            int matchCount = INITIAL_MATCHES;
            int startX = PADDING;
            int startY = PADDING;
            int yDiff = 25;

            while (matchCount > 0) {
                createBracketColumn(startX, startY, NODE_WIDTH, yDiff, matchCount, yDiff * 2);

                startY += yDiff / 2;
                yDiff *= 2;
                startX += NODE_WIDTH;
                matchCount /= 2;
            }
            createBracketColumn(startX, startY, NODE_WIDTH, yDiff, matchCount, 0);

            for (BracketNode n : nodes) {
                n.setOnMouseClicked(clicked);
                n.setOnMouseEntered(enter);
                n.setOnMouseExited(exit);
            }
        }

        //CLEANUP(Josh): Gave method & parameters more descriptive names, added @params to Javadoc
        /**
         * The secret sauce... well not really,
         * Creates 3 lines in appropriate location unless it is the last line.
         * Adds these lines and "BracketNodes" to the Pane of this inner class
         * @param startX        Initial x position of first BracketNode
         * @param startY        Initial y position of first BracketNode
         * @param nodeWidth     Width of each BracketNode
         * @param yDiff         Spacing between pairs of BracketNodes in a match
         * @param matchCount    Amount of matches in this column (0 to create single BracketNode)
         * @param yIncrement    Spacing between each match (0 to create single BracketNode)
         */
        private void createBracketColumn(int startX, int startY, int nodeWidth, int yDiff, int matchCount, int yIncrement) {
            int y = startY;
            if (matchCount == 0 && yIncrement == 0) {
                BracketNode last = new BracketNode("", startX, y - 20, nodeWidth, 20, false);
                nodes.add(last);
                getChildren().addAll(new Line(startX, startY, startX + nodeWidth, startY), last);
                String teamName = currentBracket.getBracket().get(location);
                if (comparisonBracketRef != null) {
                    last.setNameWithScore(teamName, location);
                } else {
                    last.setName(teamName);
                }
                bracketMap.put(last, location);
                nodeMap.put(location, last);
            } else {
                ArrayList<BracketNode> aNodeList = new ArrayList<>();
                for (int i = 0; i < matchCount; i++) {
                    Point2D tl = new Point2D(startX, y);
                    Point2D tr = new Point2D(startX + nodeWidth, y);
                    Point2D bl = new Point2D(startX, y + yDiff);
                    Point2D br = new Point2D(startX + nodeWidth, y + yDiff);
                    BracketNode nTop = new BracketNode("", startX, y - 20, nodeWidth, 20, matchCount == INITIAL_MATCHES);
                    aNodeList.add(nTop);
                    nodes.add(nTop);
                    BracketNode nBottom = new BracketNode("", startX, y + (yDiff - 20), nodeWidth, 20, matchCount == INITIAL_MATCHES);
                    aNodeList.add(nBottom);
                    nodes.add(nBottom);
                    Line top = new Line(tl.getX(), tl.getY(), tr.getX(), tr.getY());
                    Line bottom = new Line(bl.getX(), bl.getY(), br.getX(), br.getY());
                    Line right = new Line(tr.getX(), tr.getY(), br.getX(), br.getY());
                    getChildren().addAll(top, bottom, right, nTop, nBottom);
                    y += yIncrement;
                }
                ArrayList<Integer> tmpHelp = helper(location, matchCount);
                for (int j = 0; j < aNodeList.size(); j++) {
                    int bracketIndex = tmpHelp.get(j);
                    String teamName = currentBracket.getBracket().get(bracketIndex);
                    if (comparisonBracketRef != null) {
                        aNodeList.get(j).setNameWithScore(teamName, bracketIndex);
                    } else {
                        aNodeList.get(j).setName(teamName);
                    }
                    bracketMap.put(aNodeList.get(j), bracketIndex);
                    nodeMap.put(bracketIndex, aNodeList.get(j));
                }
            }

        }
    }

    /**
     * The BracketNode model for the Graphical display of the "Bracket"
     */
    private class BracketNode extends HBox {
        private String teamName;
        private Label name;

        /**
         * Creates a BracketNode with,
         *
         * @param teamName The name if any
         * @param x        The starting x location
         * @param y        The starting y location
         * @param rX       The width of the rectangle to fill pane
         * @param rY       The height of the rectangle
         */
        // Pranshu worked on this: improve node label wrapping and tooltip support for long team labels
        public BracketNode(String teamName, int x, int y, int rX, int rY, boolean isFirstMatch) {
            this.setLayoutX(x);
            this.setLayoutY(y);
            this.setMinSize(rX, rY);
            this.setPrefSize(rX, rY);
            this.setMaxSize(rX, rY);
            this.setAlignment(Pos.CENTER_LEFT);
            this.setSpacing(6);
            this.setPadding(new javafx.geometry.Insets(0, 5, 0, 5));
            this.teamName = teamName;

            //FEATURE(Josh): Replace right-click with info button for displaying team data
            Node infoButton = createInfoButton(rY);
            infoButton.setVisible(isFirstMatch);
            getChildren().add(infoButton);

            name = new Label(teamName);
            name.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
            name.setWrapText(true);
            name.setTextAlignment(TextAlignment.LEFT);
            name.setPrefWidth(Math.max(rX - 50, 120));
            name.setMaxWidth(Math.max(rX - 50, 120));
            name.setTooltip(new Tooltip(teamName));
            getChildren().addAll(name);
        }

        private Node createInfoButton(double nodeHeight) {
            Circle buttonCircle = new Circle(nodeHeight / 2.0 - 2.0);
            Label buttonLabel = new Label("i");
            StackPane infoButton = new StackPane(buttonCircle, buttonLabel);

            infoButton.getStyleClass().add("info-button");
            buttonCircle.getStyleClass().add("circle");
            infoButton.getStylesheets().add(getClass().getResource("infoButton.css").toString());

            infoButton.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    return;
                }
                // Code moved from BracketPane.clicked
                String text = "";
                int treeNum = bracketMap.get(this);
                String teamName = currentBracket.getBracket().get(treeNum);
                Team t = teamInfo.getTeam(teamName);

                //by Tyler - added the last two pieces of info to the pop up window
                text += "Team: " + teamName + " | Ranking: " + t.getRanking() + "\nMascot: " + t.getNickname() +
                    "\nInfo: " + t.getInfo() + "\nAverage Offensive PPG: " + t.getOffensePPG() + "\nAverage Defensive PPG: " + t.getDefensePPG();
                //create a popup with the team info
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.CLOSE);
                alert.setTitle("March Madness Bracket Simulator");
                alert.setHeaderText(null);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.showAndWait();

                // Prevent the MouseClicked event for the parent BracketNode from firing
                event.consume();
            });
            return infoButton;
        }

        /**
         * @return teamName The teams name.
         */
        public String getName() {
            return teamName;
        }

        /**
         * @param teamName The name to assign to the node.
         *///Tristan added showign ranking next to names
        public void setName(String teamName) {
            System.out.println("DEBUG setName called (NOT setNameWithScore): " + teamName);
            this.teamName = teamName;
            if (this.teamName.isBlank()) {
                name.setText(teamName);
            } else {
                Team t = teamInfo.getTeam(teamName);
                name.setText(teamName + " R:" + t.getRanking());
            }
    }
        }

        /**
         * Sets the name and updates display with score and prediction feedback
         * @param teamName The team name
         * @param bracketIndex The index in the bracket (used for coloring and scoring)
         */
        // Pranshu worked on this: show actual winner on wrong picks and add hover tooltip feedback
        public void setNameWithScore(String teamName, int bracketIndex) {
            this.teamName = teamName;
            
            System.out.println("DEBUG setNameWithScore called: team=" + teamName + ", index=" + bracketIndex);
            System.out.println("  comparisonBracket is null: " + (comparisonBracket == null));
            
            // Only set display text if teamName is not empty
            if (!teamName.isEmpty()) {
                // DEBUG: Force text to show scores
                int score = (comparisonBracket != null) ? comparisonBracket.getTeamScore(bracketIndex) : currentBracket.getTeamScore(bracketIndex);
                String displayText = teamName + " (" + score + ")";
                
                System.out.println("  displayText=" + displayText);
                
                // Apply coloring only if there's a meaningful comparison
                Tooltip tooltip = new Tooltip();
                if (comparisonBracket != null) {
                    if (isPredictionCorrect(bracketIndex)) {
                        // Correct prediction: green on the HBox background
                        displayText += " +" + pointsForIndex(bracketIndex);
                        this.setStyle("-fx-background-color: #90EE90; -fx-padding: 2;");
                        tooltip.setText("Correct pick. +" + pointsForIndex(bracketIndex) + " points.");
                    } else if (!currentBracket.getBracket().get(bracketIndex).isEmpty()) {
                        // Incorrect prediction: red on the HBox background
                        this.setStyle("-fx-background-color: #FFB6C6; -fx-padding: 2;");
                        tooltip.setText("Your pick: " + teamName);
                    }
                }
                
                name.setText(displayText);
                Tooltip.install(this, tooltip);
            } else {
                name.setText("");
                this.setStyle(null);
                Tooltip.uninstall(this, null);
            }
        }
    }
}
