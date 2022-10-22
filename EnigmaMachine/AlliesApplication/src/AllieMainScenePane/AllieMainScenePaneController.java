package AllieMainScenePane;

import AllieMainScenePane.Body.ContestTabPane.ContestTabPaneController;
import AllieMainScenePane.Body.DashBoardTabPane.ContestData.IllegibleContestAmountChosenException;
import DTO.OnLineContestsTable;
import AllieMainScenePane.Body.DashBoardTabPane.DashboardTabPaneController;
import DesktopUserInterface.MainScene.ErrorDialog;
import Utils.HttpClientUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static AlliesServletsPaths.AlliesServletsPaths.*;
import static Constants.ServletConstants.USER_NAME;
import static UBoatServletsPaths.UBoatsServletsPaths.U_BOAT_LOGOUT_SERVLET;
import static Utils.Constants.*;
import static Utils.Constants.TASK_SIZE;

public class AllieMainScenePaneController {
    @FXML private Label allieHeaderLabel;
    @FXML private AnchorPane dashboardTabPane;
    @FXML private DashboardTabPaneController dashboardTabPaneController;
    @FXML private AnchorPane contestsTabPane;
    @FXML private ContestTabPaneController contestsTabPaneController;
    @FXML private Button registerToBattleButton;
    @FXML private Button readyToContestButton;
    @FXML private Button logOutButton;
    @FXML private Button setTaskSizeButton;
    @FXML private TextField taskSizeTextField;

    public void initialize() {
        if(dashboardTabPaneController != null) {
            dashboardTabPaneController.setAllieMainScenePaneController(this);
        }
        if(contestsTabPaneController != null) {
            contestsTabPaneController.setAllieMainScenePaneController(this);
        }
        setAllyName();
    }

    private void setAllyName() {
        String finalUrl = HttpUrl.parse(GET_USER_NAME_SERVLET).
                newBuilder().
                addQueryParameter("action", "getAllyName").
                build().
                toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new ErrorDialog(new Exception("Failed to get user name"), "Failed to get user name");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String userName = response.body().string();
                allieHeaderLabel.setText("Ally - " + userName);
            }
        });
    }

    public void setActive() {
        dashboardTabPaneController.setActive();
        contestsTabPaneController.setActive();

    }

    @FXML public void onDashboardButtonClicked(ActionEvent actionEvent) {
        dashboardTabPane.setVisible(true);
        contestsTabPane.setVisible(false);

    }

    @FXML public void onContestButtonClicked(ActionEvent actionEvent) {
        dashboardTabPane.setVisible(false);
        contestsTabPane.setVisible(true);
    }
    public void onRegisterToBattleButtonClicked(ActionEvent actionEvent) {
        try {
            String chosenContestName = dashboardTabPaneController.getSelectedContest();
            String finalUrl = HttpUrl.parse(REGISTER_TO_BATTLE_SERVLET)
                    .newBuilder()
                    .addQueryParameter(USER_NAME, chosenContestName)
                    .build()
                    .toString();
            HttpClientUtil.runAsync(finalUrl, new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    new ErrorDialog(e, "Error");
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.code() == 200) {
                        String body = response.body().string();
                        OnLineContestsTable chosenContest = onLineContestFromJson(body);
                        setChosenContest(chosenContest);
                        Platform.runLater(() -> {
                            registerToBattleButton.setDisable(true);
                            setTaskSizeButton.setDisable(false);
                        });
                    } else {
                        new ErrorDialog(new Exception("Error registering to battle"), "Error");
                    }
                }
            });
        }

        catch(IllegibleContestAmountChosenException ex) {
            new ErrorDialog(ex, "Error: Failed to register to contest");
        }
    }
    public static OnLineContestsTable onLineContestFromJson(String jsonChosenContest) {
        JsonObject jsonObject = JsonParser.parseString(jsonChosenContest).getAsJsonObject();
        String battleName = jsonObject.get("battleName").getAsString();
        String boatName = jsonObject.get("boatName").getAsString();
        String contestStatus = jsonObject.get("contestStatus").getAsString();
        String difficulty = jsonObject.get("difficulty").getAsString();
        String teamsRegisteredAndNeeded = jsonObject.get("teamsRegisteredAndNeeded").getAsString();
        return  new OnLineContestsTable(battleName, boatName, contestStatus, difficulty, teamsRegisteredAndNeeded);
    }

    public String getSelectedContest() throws IllegibleContestAmountChosenException {
        return dashboardTabPaneController.getSelectedContest();
    }
    public void onSetTaskSizeButtonClicked(ActionEvent actionEvent) {
        if(taskSizeTextField.getText().isEmpty()) {
            return;
        }
        try {
            long taskSize = Long.parseLong(taskSizeTextField.getText());
            if(taskSize < 1) {
                new ErrorDialog(new Exception("Task size must be positive number"),"Error");
            }
            else{
                setAllyTaskSize(taskSize);
            }
        } catch (NumberFormatException e) {
            new ErrorDialog(new Exception("Task size must be a number"), "Error");
            taskSizeTextField.setText("");
        }
    }

    private void setAllyTaskSize(long taskSize) {
        String finalUrl = HttpUrl.parse(ALLIES_OPS_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, SET_TASK_SIZE)
                .addQueryParameter(TASK_SIZE, String.valueOf(taskSize))
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new ErrorDialog(e, "Error");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200) {
                    Platform.runLater(() -> {
                        setTaskSizeButton.setDisable(true);
                        //readyToContestButton.setDisable(true);
                    });

                }
                else{
                    new ErrorDialog(new Exception("Error setting task size"), "Error");
                    taskSizeTextField.setText("");
                }
            }
        });
    }

    public void close() {
        Platform.exit();
    }

    public void setChosenContest(OnLineContestsTable chosenContest) {
        contestsTabPaneController.setChosenContest(chosenContest);
    }

    public void onReadyToContestButtonClicked(ActionEvent actionEvent) {
        String finalUrl = HttpUrl.parse(READY_MANAGER_SERVLET)
                .newBuilder()
                .addQueryParameter(TYPE, "Allies")
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new ErrorDialog(e, "Error");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200) {
                    contestsTabPaneController.startRefresh();
                    Platform.runLater(() -> {
                        readyToContestButton.setDisable(true);
                    });
                    //String body = response.body().string();
                }
                else{
                    new ErrorDialog(new Exception("Error setting Ready"), "Error");
                }
                response.close();
            }
        });

    }

    public void onLogOutButtonClicked(ActionEvent actionEvent) {
        String finalUrl = HttpUrl.parse(U_BOAT_LOGOUT_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "alliesLogout")
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new ErrorDialog(e, "Error");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200) {
                    new ErrorDialog(new Exception("Logged out successfully"), "Logged out");
                    dashboardTabPaneController.close();
                    contestsTabPaneController.closeActivity();
                }
                else{
                    new ErrorDialog(new Exception("Error logging out"), "Error");
                }
            }
        });
    }
}
