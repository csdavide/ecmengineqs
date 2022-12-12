package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.entities.User;
import it.doqui.index.ecmengineqs.business.exceptions.UnauthorizedException;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.business.repositories.UserRepository;
import it.doqui.index.ecmengineqs.utils.MD4;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;

@ApplicationScoped
@Slf4j
public class UserService {

    @Inject
    UserContextManager userContextManager;

    @Inject
    UserRepository userRepository;

    public void authenticate() {
        String tenant = userContextManager.getContext().getTenant();
        String username = userContextManager.getContext().getUsername();
        String password = userContextManager.getContext().getPassword();
        userRepository
            .find("tenant = ?1 and username = ?2", tenant, username)
            .firstResultOptional()
            .filter(u -> verifyPassword(u, password))
            .orElseThrow(() -> new UnauthorizedException());
    }

    private boolean verifyPassword(User u, String password) {
        try {
            byte[] pwdBytes = StringUtils.stripToEmpty(password).getBytes("UTF-16LE");
            MD4 md4 = new MD4();
            byte[] encPwd = md4.digest(pwdBytes);
            String encodedPassword = encodeHexString(encPwd);
            String userPassword = (String) u.getData().get("usr:password");
            return StringUtils.equals(encodedPassword, userPassword);
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to verify password for user {}: {}", u.getUsername(), e.getMessage());
            throw new UnauthorizedException();
        }
    }

    private String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    private String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits).toLowerCase();
    }
}
