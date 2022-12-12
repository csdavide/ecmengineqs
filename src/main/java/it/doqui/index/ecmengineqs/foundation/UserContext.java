package it.doqui.index.ecmengineqs.foundation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@Getter
@Builder
@ToString
public class UserContext {
    private String repository;
    private String tenant;
    private String username;
    private String password;
    private final String operationId = UUID.randomUUID().toString();

    public String getAuthority() {
        return String.format("%s@%s", username, tenant);
    }

    public boolean isAdmin() {
        return StringUtils.equals(username, "admin");
    }

    public static String authority(String username, String tenant) {
        if (StringUtils.isBlank(tenant)) {
            return username;
        }

        return String.format("%s@%s", username, tenant);
    }

    public String getTenant() {
        if (StringUtils.isBlank(tenant)) {
            return "";
        }

        return tenant;
    }

}
