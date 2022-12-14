package AgentLoginPane;

import AgentMainScenePane.AgentMainScenePaneController;
import DTO.TeamNameColumn;
import DesktopUserInterface.MainScene.ErrorDialog;
import Engine.EngineManager;
import Utils.HttpClientUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import static AgentsServletsPaths.AgentServletsPaths.AGENT_LOGIN_SERVLET;
import static AgentsServletsPaths.AgentServletsPaths.REGISTER_TO_ALLY_SERVLET;

public class AgentLoginPaneController implements Closeable {
    private AgentMainScenePaneController agentMainScenePaneController;

    private static final String AGENT_MAIN_PAGE_FXML_RESOURCE_LOCATION = "/AgentMainScenePane/AgentMainScenePane.fxml";
    @FXML
    private ScrollPane agentLoginPane;
    private int numberOfAlliesToRegisterTo;
    private String agentName;
    private String chosenTeam;
    private Integer numberOfThreads;
    private Long tasksPulledEachTime;
    private EngineManager engineManager;
    private Timer timer;
    private OptionalTeamsRefresher optionalTeamsRefresher;

    @FXML
    private TextField setAgentNameTextField;

    @FXML
    private TableView<TeamNameColumn> teamNameTable;

    @FXML
    private TableColumn<TeamNameColumn, String> teamsToChooseCol;

    @FXML
    private Button registerToTeamButton;

    @FXML
    private Label chosenTeamLabel;

    @FXML
    private Slider numberOfThreadsBar;

    @FXML
    private Button numberOfThreadsButton;

    @FXML
    private TextField tasksPulledTextField;

    @FXML
    private Button tasksPulledButton;

    @FXML
    private Button loginButton;

    @FXML
    private Button quitButton;
    public AgentLoginPaneController() {
        this.agentName = "";
        this.chosenTeam = "";
        this.numberOfThreads = 0;
        this.tasksPulledEachTime = 0L;
        this.numberOfAlliesToRegisterTo = 0;
    }
    @FXML public void initialize(){
        initializeTeamsTable();
        startListRefresher();
    }
    private void initializeTeamsTable() {
        teamsToChooseCol.setCellValueFactory(new PropertyValueFactory<TeamNameColumn, String>("teamName"));
    }

    @FXML
    void onLoginButtonClicked(ActionEvent event) {
        if(setAgentNameTextField.getText() == null || setAgentNameTextField.getText().equals("") || chosenTeam.equals("") || numberOfThreads == 0 || tasksPulledEachTime == 0) {
            Platform.runLater(() -> {
                new ErrorDialog(new Exception("Please fill all the fields"), "Error");
            });
        } else {
            String finalUrl = HttpUrl.parse(AGENT_LOGIN_SERVLET).
                    newBuilder().
                    addQueryParameter("action", "setAgentName").
                    addQueryParameter("agentName", setAgentNameTextField.getText()).
                    build().
                    toString();
            HttpClientUtil.runAsync(finalUrl, new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Platform.runLater(() -> new ErrorDialog(e, "Error"));
                }

                @Override
                public void onResponse(@NotNull okhttp3.Call call, @NotNull okhttp3.Response response) throws IOException {
                    if(response.isSuccessful()) {
                        Platform.runLater(() -> {
                            agentName = setAgentNameTextField.getText();
                            setAgentNameTextField.setDisable(true);
                        });

                        registerToAlly();

                    } else {
                        Platform.runLater(() -> {
                            try {
                                setAgentNameTextField.setText("");
                                new ErrorDialog(new Exception(response.body().string()), "User name already exists");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    response.close();
                }
            });
        }
    }

    private void registerToAlly() {
        String finalUrl = HttpUrl.parse(REGISTER_TO_ALLY_SERVLET).
                newBuilder().
                addQueryParameter("action", "registerToAlly").
                addQueryParameter("agentName", agentName).
                addQueryParameter("chosenTeam", chosenTeam).
                addQueryParameter("numberOfThreads", numberOfThreads.toString()).
                addQueryParameter("tasksPulledEachTime", tasksPulledEachTime.toString()).
                build().
                toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Platform.runLater(() -> new ErrorDialog(e, "Error"));
            }

            @Override
            public void onResponse(@NotNull okhttp3.Call call, @NotNull okhttp3.Response response) throws IOException {
                if(response.isSuccessful()) {
                    Platform.runLater(() -> {
                        switchToMainScene();
                    });
                } else {
                    Platform.runLater(() -> new ErrorDialog(new Exception("Failed to register to ally"), "Error"));
                }
                response.close();
            }
        });
    }

    private void switchToMainScene() {
        Scene scene = agentLoginPane.getScene();
        URL url = getClass().getResource(AGENT_MAIN_PAGE_FXML_RESOURCE_LOCATION);
        FXMLLoader fxmlLoader = new FXMLLoader(url);
        try {
            Parent root = fxmlLoader.load();
            agentMainScenePaneController = fxmlLoader.getController();
            scene.setRoot(root);
            agentMainScenePaneController.setActive();
            agentMainScenePaneController.setAgentName(agentName);
            agentMainScenePaneController.setNumberOfThreads(numberOfThreads);
        } catch (IOException e) {
            new ErrorDialog(e, "Unable to load main ally scene");
        }
    }
    public void startListRefresher(){
        optionalTeamsRefresher = new OptionalTeamsRefresher(
                this::updateOptionalTeamsTable);
        timer = new Timer();
        timer.schedule(optionalTeamsRefresher, 0, 5000);
    }
    private void updateOptionalTeamsTable(List<TeamNameColumn> optionalTeams) {
        Platform.runLater(() -> {
            if(optionalTeams != null && optionalTeams.size() != numberOfAlliesToRegisterTo) {
                clearTable();
                numberOfAlliesToRegisterTo = optionalTeams.size();
                if (optionalTeams.size() > 0) {

                    ObservableList<TeamNameColumn> allTeams = teamNameTable.getItems();
                    for (TeamNameColumn team : optionalTeams) {
                        allTeams.add(new TeamNameColumn(team.getTeamName()));
                        teamNameTable.setItems(allTeams);
                    }

                }
            }
            if(optionalTeams == null){
                numberOfAlliesToRegisterTo = 0;
                clearTable();
            }
        });
    }

    private void clearTable() {
        teamNameTable.getItems().clear();
    }

    private List<String> getTeamsNames(List<TeamNameColumn> optionalTeams) {
        List<String> teams = new ArrayList<>();
        for(TeamNameColumn team : optionalTeams) {
            teams.add(team.getTeamName());
        }
        return teams;
    }

    @Override
    public void close() throws IOException {
        if(optionalTeamsRefresher != null) {
            optionalTeamsRefresher.cancel();
            timer.cancel();
        }

    }


    @FXML
    void onNumberOfThreadsButtonClicked(ActionEvent event) {
        numberOfThreads = (int) numberOfThreadsBar.getValue();

        numberOfThreadsBar.setDisable(true);
    }

    @FXML
    void onQuitButtonClicked(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    void onRegisterToTeamButtonClicked(ActionEvent event) {
        try {
            chosenTeam = getChosenTeam();
            chosenTeamLabel.setText(chosenTeam);
        }
        catch (Exception e){
            new ErrorDialog(e, "Failed to get chosen team");
        }
    }

    private String getChosenTeam() throws Exception {
        ObservableList<TeamNameColumn> chosenTeam = teamNameTable.getSelectionModel().getSelectedItems();
        if(chosenTeam.size() != 1) {
            throw new Exception("You must choose only one team!!");
        }
        else {
            return chosenTeam.get(0).getTeamName();
        }
    }

    @FXML
    void onTasksPulledButtonClicked(ActionEvent event) {
        try {
            tasksPulledEachTime = Long.parseLong(tasksPulledTextField.getText());

            if(tasksPulledEachTime <= 0) {
                tasksPulledEachTime = 0L;
                new ErrorDialog(new Exception("Tasks pulled each time must be positive"), "You inputted a negative number");
            }
            else {
                tasksPulledTextField.setDisable(true);
                tasksPulledButton.setDisable(true);
            }
        }
        catch (Exception e) {
            new ErrorDialog(new Exception("You must choose a positive number"), "You didn't input a number");
        }

    }

    public void setAgentMainScenePaneController(AgentMainScenePaneController agentMainScenePaneController) {
        this.agentMainScenePaneController = agentMainScenePaneController;
        Platform.runLater( ()-> {
                agentMainScenePaneController.setAgentName(agentName);
        });
    }
}
