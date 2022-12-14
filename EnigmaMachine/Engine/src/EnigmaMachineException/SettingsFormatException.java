package EnigmaMachineException;

import EnigmaMachine.Settings.Sector;
import EnigmaMachine.Settings.SectorType;
import EnigmaMachine.Settings.SettingsFormat;

import java.util.ArrayList;
import java.util.List;

public class SettingsFormatException extends Exception{
    private final String EXCEPTION_IDENTATION = "       ";
    private Integer errorIndex;
    private final String startingMessage = "Error: Failed to initialize the settings because because the following settings have not been initialized:" + System.lineSeparator();
    private SettingsFormat settingsFormat;

    public SettingsFormatException(SettingsFormat settingsFormat){
        this.settingsFormat = settingsFormat;

    }

    @Override
    public String getMessage() {
        return System.lineSeparator()
                + startingMessage + getMissingSettings();
    }

    private String getMissingSettings() {
        List<String> missingSettings = new ArrayList<>();
        Boolean isSectorTypeFound;

        for (SectorType sectorType : SectorType.values()) {
            isSectorTypeFound = false;

            for(Sector settingSector : settingsFormat.getSettingsFormat()) {
                if(sectorType == settingSector.getType()) {
                    isSectorTypeFound = true;
                    break;
                }
            }

            if(!isSectorTypeFound) {
                missingSettings.add(sectorType.toString());
            }
        }

        return EXCEPTION_IDENTATION + String.join(",", missingSettings);
    }
}
