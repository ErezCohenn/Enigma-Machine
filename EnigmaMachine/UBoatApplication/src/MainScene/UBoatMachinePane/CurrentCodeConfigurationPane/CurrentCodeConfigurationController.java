package MainScene.UBoatMachinePane.CurrentCodeConfigurationPane;

import DesktopUserInterface.MainScene.Common.SkinType;
import MainScene.UBoatMachinePane.UBoatMachinePaneController;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class CurrentCodeConfigurationController {

    @FXML private VBox CodeCalibrationGrid;
    @FXML private TextArea currentCodeConfigurationTextArea;
    private UBoatMachinePaneController uBoatMachinePaneController;
    private Map<SkinType, String> skinPaths;

    @FXML public void initialize() {
        initializeSkins();
    }

    private void initializeSkins() {
        int skinIndex = 1;
        skinPaths = new HashMap<>();
        for(SkinType skin : SkinType.values()) {
            skinPaths.put(skin, "CurrentCodeConfigurationSkin" + skinIndex++ + ".css");
        }
    }
    public void clearTextArea() {
        currentCodeConfigurationTextArea.clear();
    }
    public void setCodeConfiguration(String currentMachineSettings) {
        currentCodeConfigurationTextArea.setText(currentMachineSettings);
    }

    public void currentCodeChanged(Object o, String currentCode) {
        currentCodeConfigurationTextArea.setText(currentCode);
    }

    public void setSkin(SkinType skinType) {
        CodeCalibrationGrid.getStylesheets().clear();
        CodeCalibrationGrid.getStylesheets().add(String.valueOf(getClass().getResource(skinPaths.get(skinType))));
    }

    public void setUBoatCompetitionPaneController(UBoatMachinePaneController uBoatMachinePaneController) {
        this.uBoatMachinePaneController = uBoatMachinePaneController;
    }

    public void setCurrentConfig() {

    }
}
