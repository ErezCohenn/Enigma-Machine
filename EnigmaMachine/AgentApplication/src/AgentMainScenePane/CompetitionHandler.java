package AgentMainScenePane;

import AgentMainScenePane.Body.ContestAndTeamDataPane.ContestAndTeamDataPaneController;
import AgentMainScenePane.Body.ContestStatusRefresher;
import DTO.AgentCandidatesInformation;
import DTO.TaskToAgent;
import DesktopUserInterface.MainScene.ErrorDialog;
import Engine.AgentsManager.AgentTask;
import Engine.AgentsManager.AgentWorker;
import Engine.Dictionary;
import Engine.EngineManager;
import EnigmaMachine.EnigmaMachine;
import EnigmaMachine.Settings.Sector;
import EnigmaMachine.Settings.StartingRotorPositionSector;
import EnigmaMachineException.MachineNotExistsException;
import Utils.HttpClientUtil;
import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import static AgentsServletsPaths.AgentServletsPaths.BATTLE_CANDIDATES_SERVLET;
import static AgentsServletsPaths.AgentServletsPaths.TASKS_SERVLET;
import static Utils.Constants.ACTION;
import static Utils.Constants.GSON_INSTANCE;

public class CompetitionHandler extends Thread implements Closeable {
    private Timer timer;
    private ContestStatusRefresher contestStatusRefresher;
    private int numberOfCandidatesFound;
    List<AgentCandidatesInformation> agentCandidatesInformationList;
    private long tasksCompleted;
    long numberOfTasksPulled;
    private ThreadPoolExecutor tasksPool;
    private BlockingQueue<TaskToAgent> contestTasksQueue;
    private EngineManager engineManager;
    private AgentMainScenePaneController agentMainScenePaneController;
    private OkHttpClient client;
    private String agentName;
    private BlockingQueue<Runnable> tasksQueue;
    private boolean isContestActive;


    public CompetitionHandler(ThreadPoolExecutor tasksPool, EngineManager engineManager, BlockingQueue<TaskToAgent> contestTasksQueue, String agentName, OkHttpClient client,
                              AgentMainScenePaneController agentMainScenePaneController,
                              BlockingQueue<Runnable> tasksQueue) {
        this.tasksPool = tasksPool;
        this.engineManager = engineManager;
        this.contestTasksQueue = contestTasksQueue;
        this.agentName = agentName;
        this.client = client;
        this.agentMainScenePaneController = agentMainScenePaneController;
        this.tasksQueue = tasksQueue;
        this.numberOfTasksPulled = 0;
        this.tasksCompleted = 0;
        this.agentCandidatesInformationList = new ArrayList<>();
        this.numberOfCandidatesFound = 0;
    }


    @Override
    public void start() {
        isContestActive = true;
        startRefreshingContestStatus();
        while (isContestActive) {
            try {
                if (tasksQueue.isEmpty()) {
                    getTaskFromServer();
                    sendCandidateInformationToServer();
                    numberOfCandidatesFound += agentCandidatesInformationList.size();
                    updateTasksCompleted(tasksCompleted, numberOfCandidatesFound);
                    agentCandidatesInformationList.clear();
                }
            } catch ( IOException e) {
                e.printStackTrace();
            }
        }
        tasksPool.shutdown();
        System.out.println("Contest is over");
    }

    private boolean isContestActive(Boolean isContestActive) {
        this.isContestActive = isContestActive;
        return isContestActive;
    }

    private void startRefreshingContestStatus() {
        contestStatusRefresher = new ContestStatusRefresher(this::isContestActive);
        timer = new Timer();
        timer.schedule(contestStatusRefresher, 0, 500);
    }

    @Override
    public void close() throws IOException {
        if(contestStatusRefresher != null) {
            contestStatusRefresher.cancel();
            timer.cancel();
        }
    }
    private void updateTasksCompleted(long tasksCompleted, int numberOfCandidatesFound) {
        updateCandidatesFoundInServer(numberOfCandidatesFound);
        addTasksCompletedToAgentsServer(tasksCompleted);
        agentMainScenePaneController.updateTasksCompleted(tasksCompleted, numberOfCandidatesFound);
    }

    private void addTasksCompletedToAgentsServer(long tasksCompleted) {
        String finalUrl = HttpUrl.parse(BATTLE_CANDIDATES_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "UpdateTasksCompleted")
                .addQueryParameter("tasksCompleted", GSON_INSTANCE.toJson(tasksCompleted))
                .build().toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200) {
                    response.close();
                }
                response.close();
            }
        });
    }

    private void updateCandidatesFoundInServer(int numberOfCandidatesFound) {
        String finalUrl = HttpUrl.parse(BATTLE_CANDIDATES_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "updateCandidatesFound")
                .addQueryParameter("agentName", agentName)
                .addQueryParameter("numberOfCandidatesFound", GSON_INSTANCE.toJson(numberOfCandidatesFound))
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                new ErrorDialog(e, "Error while trying to update candidates found");
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.code() != 200) {
                    new ErrorDialog(new Exception(response.body().string()), "Error while trying to update candidates found");
                }
                response.close();
            }
        });
    }
    private void sendCandidateInformationToServer() {
        Gson gson = new Gson();
        if(agentCandidatesInformationList.size() > 0) {
            String json = gson.toJson(agentCandidatesInformationList);
            String finalUrl = HttpUrl.parse(BATTLE_CANDIDATES_SERVLET)
                    .newBuilder()
                    .addQueryParameter(ACTION, "sendCandidates")
                    .addQueryParameter("candidatesList", json)
                    .build().toString();
            HttpClientUtil.runAsync(finalUrl, new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    new ErrorDialog(new Exception("Failed to send candidates to server"), "Failed to send candidates to server");
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.code() != 200) {
                        String responseBody = response.body().string();
                        new ErrorDialog(new Exception("Failed to send candidates to server: " + responseBody), "Failed to send candidates to server");
                    }
                    response.close();
                }
            });
        }
    }

    private void getTaskFromServer() throws IOException {

        String finalUrl = HttpUrl.parse(TASKS_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "getTasksInterval")
                .addQueryParameter("agentName", agentName)
                .build()
                .toString();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(finalUrl)
                .build();

        Response response = client.newCall(request).execute();
        if (response.code() == 200) {
            Gson gson = new Gson();
            String responseString = response.body().string();
            List<TaskToAgent> tasksToAgent = Arrays.asList(gson.fromJson(responseString, TaskToAgent[].class));
            numberOfTasksPulled += tasksToAgent.size();

            CountDownLatch countDownLatch = new CountDownLatch(tasksToAgent.size());
            updateTasksPulled(numberOfTasksPulled);

            try {
                for (int i = 0; i < tasksToAgent.size(); i++) {
                    AgentTask agentTask = getAgentTaskFromTaskToAgent(tasksToAgent.get(i));
                    AgentWorker agent = new AgentWorker(agentTask,agentName,numberOfTasksPulled,agentCandidatesInformationList,countDownLatch);
                    tasksPool.execute(agent);
                }
                countDownLatch.await();
                tasksCompleted += tasksToAgent.size();
                //System.out.println("All tasks completed " + countDownLatch.getCount() );

            } catch (CloneNotSupportedException | MachineNotExistsException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            tasksPool.shutdown();
        }
    }
    private void updateTasksPulled(long numberOfTasksPulled) {
        updateTasksPulledInServer(numberOfTasksPulled);
        agentMainScenePaneController.updateTasksPulled(numberOfTasksPulled);
    }
    private void updateTasksPulledInServer(long numberOfTasksPulled) {
        String finalUrl = HttpUrl.parse(BATTLE_CANDIDATES_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "updateTasksPulled")
                .addQueryParameter("agentName", agentName)
                .addQueryParameter("numberOfTasksPulled", GSON_INSTANCE.toJson(numberOfTasksPulled))
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                new ErrorDialog(e, "Error while trying to update tasks pulled");
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.code() != 200) {
                    new ErrorDialog(new Exception(response.body().string()), "Error while trying to update tasks pulled");
                }
                response.close();
            }
        });

    }
    private AgentTask getAgentTaskFromTaskToAgent(TaskToAgent taskToAgent) throws CloneNotSupportedException, MachineNotExistsException {
        int taskSize = (int)taskToAgent.getTaskSize();
        EnigmaMachine enigmaMachine = new EnigmaMachine(engineManager.getEnigmaMachine().cloneRotors(), engineManager.getCurrentEnigmaMachine().cloneReflectors()
                , engineManager.getEnigmaMachine().cloneKeyboard(), engineManager.getEnigmaMachine().getNumOfActiveRotors());
        List<Integer> notchPositions = taskToAgent.getSectorsCodeAsJson().getNotchPositions();

        List<Sector> sectors = taskToAgent.getSectorsCodeAsJson().getSectors();

        char[]  startingRotorPositionSectorElements = taskToAgent.getSectorsCodeAsJson().getStartingRotorPositionElements();
        //startingRotorPositionSector.setCurrentNotchPositions(notchPositions);
        List<Character> startingRotorPosition = new ArrayList<>();
        for (char startingRotorPositionSectorElement : startingRotorPositionSectorElements) {
            startingRotorPosition.add(startingRotorPositionSectorElement);
        }
        StartingRotorPositionSector startingRotorPositionSector = new StartingRotorPositionSector(startingRotorPosition);
        sectors.get(0).setSectorInTheMachine(enigmaMachine);
        startingRotorPositionSector.setSectorInTheMachine(enigmaMachine);
        sectors.get(2).setSectorInTheMachine(enigmaMachine);
        sectors.get(3).setSectorInTheMachine(enigmaMachine);
        String encryptedMessage = taskToAgent.getEncryptedMessage();
        Dictionary dictionary = engineManager.getDictionaryObject();

        return new AgentTask(taskSize, startingRotorPositionSector, enigmaMachine, encryptedMessage, dictionary,agentName);

    }
}