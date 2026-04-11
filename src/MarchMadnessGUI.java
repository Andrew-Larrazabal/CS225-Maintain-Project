//package marchmadness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *  MarchMadnessGUI
 * 
 * this class contains the buttons the user interacts
 * with and controls the actions of other objects 
 *
 * @author Grant Osborn
 */
public class MarchMadnessGUI extends Application {
    
    
    //all the gui ellements
    private BorderPane root;
    private ToolBar toolBar;
    private ToolBar btoolBar;
    private Button simulate;
    private Button login;
    private Button scoreBoardButton;
    private Button viewBracketButton;
    private Button clearButton;
    private Button resetButton;
    private Button finalizeButton;
    
    //allows you to navigate back to division selection screen
    private Button back;
  
    
    private  Bracket startingBracket;
    //reference to currently logged in bracket
    private Bracket selectedBracket;
    private Bracket simResultBracket;    //Tracks whether simulation has occurred to show feedback
    private boolean simulationHasOccurred = false;
    
    private ArrayList<Bracket> playerBrackets;
    private HashMap<String, Bracket> playerMap;

    

    private ScoreBoardTable scoreBoard;
    private TableView table;
    private BracketPane bracketPane;
    private GridPane loginP;
    private TournamentInfo teamInfo;

    private ProgressMeter progressMeter; // Bandana: progess meter showing the completness of your bracket
    
    
    @Override
    public void start(Stage primaryStage) {
        //try to load all the files, if there is an error display it
        try{
            teamInfo=new TournamentInfo();
            startingBracket= new Bracket(teamInfo.loadStartingBracket());
            //CLEANUP(Josh): Clone loaded bracket instead of reloading from disk
            simResultBracket=new Bracket(startingBracket);
        } catch (IOException ex) {
            showError(new Exception("Can't find "+ex.getMessage(),ex),true);
        }
        //deserialize stored brackets
        playerBrackets = loadBrackets();
        
        playerMap = new HashMap<>();
        addAllToMap();
        


        //the main layout container
        root = new BorderPane();
        scoreBoard= new ScoreBoardTable();
        table=scoreBoard.start();
        loginP=createLogin();
        progressMeter = new ProgressMeter(); // bandana :initialize progress meter for tracking bracket completion
        CreateToolBars();
        
        //display login screen
        login();
        

        setActions();
        root.setTop(toolBar);   
        root.setBottom(btoolBar);
        Scene scene = new Scene(root);
        primaryStage.setMaximized(true);

        primaryStage.setTitle("March Madness Bracket Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    
    
    /**
     * simulates the tournament  
     * simulation happens only once and
     * after the simulation no more users can login
     */
    // Pranshu worked on this: enable scoreboard/view bracket after simulation and keep sim results for feedback
    private void simulate(){
        //cant login and restart prog after simulate
        login.setDisable(true);
        simulate.setDisable(true);
        
       scoreBoardButton.setDisable(false);
       viewBracketButton.setDisable(false);
       
       teamInfo.simulate(simResultBracket);
       for(Bracket b:playerBrackets){
           scoreBoard.addPlayer(b,b.scoreBracket(simResultBracket));
       }
       
       simulationHasOccurred = true;
       System.out.println("DEBUG ========== SIMULATION COMPLETE - simulationHasOccurred = true ==========");
        
        displayPane(table);
    }
    
    /**
     * Displays the login screen
     * 
     */
       private void login(){
        if (selectedBracket != null) {
            seralizeBracket(selectedBracket);
        }

        selectedBracket = null;
        bracketPane = null;

        loginP = createLogin();
        progressMeter.reset();

        login.setText("Login");
        login.setDisable(true);
        simulate.setDisable(true);
        scoreBoardButton.setDisable(true);
        viewBracketButton.setDisable(true);
        btoolBar.setDisable(true);

        displayPane(loginP);
    }
    
     /**
     * Displays the score board
     * 
     */
    private void scoreBoard(){
        displayPane(table);
    }
    
     /**
      * Displays Simulated Bracket (reference only, no feedback)
      * 
      */
    // Pranshu worked on this: render player's bracket with sim results comparison so feedback colors appear
    private void viewBracket(){
       // Show player's bracket (selectedBracket) with simulation results for comparison feedback
       bracketPane=new BracketPane(selectedBracket, teamInfo, simulationHasOccurred ? simResultBracket : null, clearButton, progressMeter);
       GridPane full = bracketPane.getFullPane();
       full.setAlignment(Pos.CENTER);
       full.setDisable(true);
       displayPane(new ScrollPane(full)); 
    }
    
    /**
     * allows user to choose bracket
     * 
     */
    // Pranshu worked on this: pass simulation comparison to bracket views after simulation
        private void chooseBracket(){
        login.setDisable(false);
        login.setText("Logout");
        btoolBar.setDisable(false);
        Bracket comparison = simulationHasOccurred ? simResultBracket : null;
        System.out.println("DEBUG chooseBracket called - simulationHasOccurred=" + simulationHasOccurred + ", comparison is null: " + (comparison == null));
        bracketPane=new BracketPane(selectedBracket, teamInfo, comparison, clearButton,progressMeter);
        displayPane(bracketPane);
        progressMeter.update(selectedBracket);// Bandana: updates progress meter when the user is filling the bracket
    }
    /**
     * resets current selected sub tree
     * for final4 reset Ro2 and winner
     */
    // Pranshu worked on this: rebuild cleared bracket with sim comparison if available
    private void clear(){
      bracketPane.clear();

      Bracket comparison = simulationHasOccurred ? simResultBracket : null;
      bracketPane=new BracketPane(selectedBracket, teamInfo, comparison, clearButton,progressMeter);
      displayPane(bracketPane);
      progressMeter.update(selectedBracket); //bandana: update the progress meter to reflect cleared picks.
        

    }
    
    /**
     * resets entire bracket
     */
    // Pranshu worked on this: preserve simulated comparison when resetting the bracket view
    private void reset(){
        if(confirmReset()){
            //horrible hack to reset
            selectedBracket=new Bracket(startingBracket);
            Bracket comparison = simulationHasOccurred ? simResultBracket : null;
            bracketPane=new BracketPane(selectedBracket, teamInfo, comparison, clearButton,progressMeter);
            displayPane(bracketPane);
            progressMeter.reset(); // bandana : resets the progressmeter when bracket is reset.
        }

    }
    
    private void finalizeBracket(){
        // Edited by: Jasper Carr
        if (bracketPane.isComplete()) {
            bracketPane.setFinalized(true);
            bracketPane.setDisable(true);

            // keep bottom toolbar disabled until simulation finishes
            btoolBar.setDisable(true);

            simulate.setDisable(false);
            login.setDisable(false);

            seralizeBracket(selectedBracket);
            //go back to bracket section selection screen
            // bracketPane=new BracketPane(selectedBracket);
            displayPane(bracketPane);
        
       }
       //bracketPane=new BracketPane(selectedBracket);
    }
    
    
    /**
     * displays element in the center of the screen
     * 
     * @param p must use a subclass of Pane for layout. 
     * to be properly center aligned in  the parent node
     */
    private void displayPane(Node p){
        root.setCenter(p);
        BorderPane.setAlignment(p,Pos.CENTER);
    }
    
    /**
     * Creates toolBar and buttons.
     * adds buttons to the toolbar and saves global references to them
     */
    private void CreateToolBars(){
        toolBar  = new ToolBar();
        btoolBar  = new ToolBar();
        login=new Button("Login");
        simulate=new Button("Simulate");
        scoreBoardButton=new Button("ScoreBoard");
        viewBracketButton= new Button("View Simulated Bracket");
        clearButton=new Button("Clear");
        resetButton=new Button("Reset");
        finalizeButton=new Button("Finalize");
        toolBar.getItems().addAll(
                createSpacer(),
                login,
                simulate,
                scoreBoardButton,
                viewBracketButton,
                createSpacer()
        );
        btoolBar.getItems().addAll(
                createSpacer(),
                clearButton,
                resetButton,
                finalizeButton,
                back=new Button("Choose Division"),
                progressMeter, //bandana : Display progres meter.
                createSpacer()
                
        );

    }
    
   /**
    * sets the actions for each button
    */
    private void setActions(){
        login.setOnAction(e->login());
        simulate.setOnAction(e->simulate());
        scoreBoardButton.setOnAction(e->scoreBoard());
        viewBracketButton.setOnAction(e->viewBracket());
        clearButton.setOnAction(e->clear());
        resetButton.setOnAction(e->reset());
        finalizeButton.setOnAction(e->finalizeBracket());
        back.setOnAction(e->{
            Bracket comparison = simulationHasOccurred ? simResultBracket : null;
            bracketPane=new BracketPane(selectedBracket, teamInfo, comparison, clearButton,progressMeter);
            displayPane(bracketPane);
        });
    }
    
    /**
     * Creates a spacer for centering buttons in a ToolBar
     */
    private Pane createSpacer(){
        Pane spacer = new Pane();
        HBox.setHgrow(
                spacer,
                Priority.SOMETIMES
        );
        return spacer;
    }
    
    
    private GridPane createLogin(){
        
        
        /*
        LoginPane
        Sergio and Joao
         */

        GridPane loginPane = new GridPane();
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setPadding(new Insets(5, 5, 5, 5));

        Text welcomeMessage = new Text("March Madness Login Welcome");
        loginPane.add(welcomeMessage, 0, 0, 2, 1);

        // Tristan add instructions
        Text instructions = new Text(
                "Instructions:\n" +
                        "- Enter your username and password.\n" +
                        "- If your username exists, you will log into your saved bracket.\n" +
                        "- If not, a new account and bracket will be created.\n" +
                        "- The program will only save if you have completely filled out everything.\n" +
                        "- Complete your bracket and finalize it before simulation.\n" +
                        "- Once ready then click the simulation button to view results.\n" +
                        "- TIP: Right click / Hover over teams to see information"
        );
        instructions.setWrappingWidth(400); // keeps it readable

        loginPane.add(instructions, 1, 1,2,1);

        Label userName = new Label("User Name: ");
        loginPane.add(userName, 1, 2);

        TextField enterUser = new TextField();
        loginPane.add(enterUser, 2, 2);

        Label password = new Label("Password: ");
        loginPane.add(password, 1, 3);

        PasswordField passwordField = new PasswordField();
        loginPane.add(passwordField, 2, 3);

        Button signButton = new Button("Sign in");
        loginPane.add(signButton, 2, 5);
        signButton.setDefaultButton(true);//added by matt 5/7, lets you use sign in button by pressing enter

        Label message = new Label();
        loginPane.add(message, 2, 6);

        signButton.setOnAction(event -> {

            // the name user enter
            String name = enterUser.getText();
            // the password user enter
            String playerPass = passwordField.getText();


            //Tristan added not allowed to enter empty or blank field boxes
            if (name.isBlank() || playerPass.isBlank()) {
                infoAlert("You cannot enter an empty name or password!");
            } else if (playerMap.get(name) != null) {
                //check password of user
                 
                Bracket tmpBracket = this.playerMap.get(name);
               
                String password1 = tmpBracket.getPassword();

                if (Objects.equals(password1, playerPass)) {
                    // load bracket
                    selectedBracket=playerMap.get(name);
                    chooseBracket();
                }else{
                   infoAlert("The password you have entered is incorrect!");
                }

            } else {
                //check for empty fields
                if(!name.equals("")&&!playerPass.equals("")){
                    //create new bracket
                    Bracket tmpPlayerBracket = new Bracket(startingBracket, name);
                    playerBrackets.add(tmpPlayerBracket);
                    tmpPlayerBracket.setPassword(playerPass);

                    playerMap.put(name, tmpPlayerBracket);
                    selectedBracket = tmpPlayerBracket;
                    //alert user that an account has been created
                    infoAlert("No user with the Username \""  + name + "\" exists. A new account has been created.");
                    chooseBracket();
                }
            }
        });
        
        return loginPane;
    }
    
    /**
     * addAllToMap
     * adds all the brackets to the map for login
     */
    private void addAllToMap(){
        for(Bracket b:playerBrackets){
            playerMap.put(b.getPlayerName(), b);   
        }
    }
    
    /**
     * The Exception handler
     * Displays a error message to the user
     * and if the error is bad enough closes the program
     * @param fatal true if the program should exit. false otherwise
     */
    private void showError(Exception e,boolean fatal){
        String msg=e.getMessage();
        if(fatal){
            msg=msg+" \n\nthe program will now close";
            //e.printStackTrace();
        }
        Alert alert = new Alert(AlertType.ERROR,msg);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(420);   
        alert.setTitle("Error");
        alert.setHeaderText("something went wrong");
        alert.showAndWait();
        if(fatal){ 
            System.exit(666);
        }   
    }
    
    /**
     * alerts user to the result of their actions in the login pane 
     * @param msg the message to be displayed to the user
     */
    private void infoAlert(String msg){
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("March Madness Bracket Simulator");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    /**
     * Prompts the user to confirm that they want
     * to clear all predictions from their bracket
     * @return true if the yes button clicked, false otherwise
     */
    private boolean confirmReset(){
        Alert alert = new Alert(AlertType.CONFIRMATION, 
                "Are you sure you want to reset the ENTIRE bracket?", 
                ButtonType.YES,  ButtonType.CANCEL);
        alert.setTitle("March Madness Bracket Simulator");
        alert.setHeaderText(null);
        alert.showAndWait();
        return alert.getResult()==ButtonType.YES;
    }
    
    
    /**
     * Tayon Watson 5/5
     * seralizedBracket
     * @param B The bracket the is going to be seralized
     */
    private void seralizeBracket(Bracket B){
        FileOutputStream outStream = null;
        ObjectOutputStream out = null;
    try 
    {
      outStream = new FileOutputStream(B.getPlayerName()+".ser");
      out = new ObjectOutputStream(outStream);
      out.writeObject(B);
      out.close();
    } 
    catch(IOException e)
    {
      // Grant osborn 5/6 hopefully this never happens 
      showError(new Exception("Error saving bracket \n"+e.getMessage(),e),false);
    }
    }
    /**
     * Tayon Watson 5/5
     * deseralizedBracket
     * @param filename of the seralized bracket file
     * @return deserialized bracket 
     */
    private Bracket deseralizeBracket(String filename){
        Bracket bracket = null;
        FileInputStream inStream = null;
        ObjectInputStream in = null;
    try 
    {
        inStream = new FileInputStream(filename);
        in = new ObjectInputStream(inStream);
        bracket = (Bracket) in.readObject();
        in.close();
    }catch (IOException | ClassNotFoundException e) {
      // Grant osborn 5/6 hopefully this never happens either
      showError(new Exception("Error loading bracket \n"+e.getMessage(),e),false);
    } 
    return bracket;
    }
    
      /**
     * Tayon Watson 5/5
     * deseralizedBracket
     * @return deserialized bracket
     */
    private ArrayList<Bracket> loadBrackets()
    {   
         /* old code which caused nullpointerexception
        ArrayList<Bracket> list=new ArrayList<Bracket>();
        File dir = new File(".");
        for (final File fileEntry : dir.listFiles()){
	@@ -533,6 +626,37 @@ private ArrayList<Bracket> loadBrackets()
            }
        }
        return list;
         */

        // Edited by: Jasper Carr
        ArrayList<Bracket> list = new ArrayList<>();
        File dir = new File(".");

        // Check if directory is valid
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Directory not found: " + dir.getAbsolutePath());
            return list;
        }

        File[] files = dir.listFiles();

        // Prevent NullPointerException
        if (files == null) {
            System.out.println("Could not read files in directory.");
            return list;
        }

        for (final File fileEntry : files) {
            String fileName = fileEntry.getName();

            // extension check
            if (fileName.endsWith(".ser")) {
                list.add(deseralizeBracket(fileName));
            }
        }

        return list;
    }
       
}
