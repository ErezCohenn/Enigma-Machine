package Utils;

import com.google.gson.Gson;

public class Constants {
    public final static String BASE_DOMAIN = "localhost";
    private final static String BASE_URL = "http://" + BASE_DOMAIN + ":8080";
    private final static String CONTEXT_PATH = "/WEB_EnigmaEngine_Web_exploded";

    public final static String FULL_SERVER_PATH = BASE_URL + CONTEXT_PATH;
    public final static Gson GSON_INSTANCE = new Gson();
    public static final long REFRESH_RATE = 2000;
    public static final String ACTION = "action";
    public static final String DISPLAY_SPECIFICATIONS = "displaySpecifications";
    public static final String GET_USER_NAME_SERVLET = FULL_SERVER_PATH + "/users/getUserName";
    public static final String TASK_SIZE = "taskSize";
    public static final String SET_TASK_SIZE = "setTaskSize";
    public static final String PROCESS_WORD_SERVLET = FULL_SERVER_PATH + "/machine/ProcessWord";
    public static final String GET_CURRENT_CONTEST_DATA = "getCurrentContestData";
    public static final String READY_MANAGER_SERVLET = FULL_SERVER_PATH + "/CompetitionServlet/ReadyManager";
    public static final String TYPE = "type";
    public static final String BATTLE_CANDIDATES_SERVLET = FULL_SERVER_PATH + "/BattleCandidates";

    public static final String TASKS_SERVLET = FULL_SERVER_PATH + "/Tasks";

    public static final String LOGOUT_SERVLET = FULL_SERVER_PATH + "/users/logout";
    public static final String CHAT_USERS_LIST = FULL_SERVER_PATH + "/Chat/UsersListServlet";
    public final static String JHON_DOE = "<Anonymous>";
    //TODO : change path from here down
    public final static String CHAT_ROOM_FXML_RESOURCE_LOCATION = "/chat/client/component/chatroom/chat-room-main.fxml";
    public final static String SEND_CHAT_LINE = FULL_SERVER_PATH + "/pages/chatroom/sendChat";
    public final static String CHAT_LINE_FORMATTING = "%tH:%tM:%tS | %.10s: %s%n";
    public final static String CHAT_LINES_LIST = FULL_SERVER_PATH + "/chat";
}
