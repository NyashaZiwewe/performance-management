package hr.performancemanagement.utils.wrappers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CredentialSettingsWrapper {

    private String mailFromName;
    private String mailFromEmail;
    private String mailHost;
    private String mailPort;
    private String mailUsername;
    private String mailPassword;
    private boolean mailPasswordConfigured;
}
