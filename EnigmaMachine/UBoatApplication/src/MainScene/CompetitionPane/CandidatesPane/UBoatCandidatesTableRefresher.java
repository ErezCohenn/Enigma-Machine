package MainScene.CompetitionPane.CandidatesPane;

import DTO.AllyCandidatesTable;
import DTO.ContestWinnerInformation;
import Utils.HttpClientUtil;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

import static Utils.Constants.ACTION;
import static Utils.Constants.BATTLE_CANDIDATES_SERVLET;

public class UBoatCandidatesTableRefresher extends TimerTask {
    private Consumer<List<AllyCandidatesTable>> updateCandidatesTable;

    private Consumer<ContestWinnerInformation> updateWinnerInformation;

    public UBoatCandidatesTableRefresher(Consumer<List<AllyCandidatesTable>> updateCandidatesTable, Consumer<ContestWinnerInformation> updateWinnerInformation) {
        this.updateCandidatesTable = updateCandidatesTable;
        this.updateWinnerInformation = updateWinnerInformation;
    }


    @Override
    public void run() {
        //uBoatCandidatesPaneController.refreshCandidatesTable();
        checkIfWordWasFound();
        String finalUrl = HttpUrl
                .parse(BATTLE_CANDIDATES_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "getAlliesCandidates")
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200) {
                    String jsonAllyCandidatesTable = response.body().string();
                    Gson gson = new Gson();
                    List<AllyCandidatesTable> allyCandidatesTables = Arrays.asList(gson.fromJson(jsonAllyCandidatesTable, AllyCandidatesTable[].class));
                    updateCandidatesTable.accept(allyCandidatesTables);
                }
                response.close();
            }
        });
    }

    private void checkIfWordWasFound() {
        String finalUrl = HttpUrl
                .parse(BATTLE_CANDIDATES_SERVLET)
                .newBuilder()
                .addQueryParameter(ACTION, "lookForWinner")
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200) {
                    String jsonContestWinnerInformation = response.body().string();
                    Gson gson = new Gson();
                    ContestWinnerInformation contestWinnerInformation = gson.fromJson(jsonContestWinnerInformation, ContestWinnerInformation.class);
                    updateWinnerInformation.accept(contestWinnerInformation);
                }
                response.close();
            }
        });
    }
}