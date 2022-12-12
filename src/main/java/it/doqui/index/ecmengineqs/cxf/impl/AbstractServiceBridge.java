package it.doqui.index.ecmengineqs.cxf.impl;

import it.doqui.index.ecmengine.mtom.dto.MtomOperationContext;
import it.doqui.index.ecmengine.mtom.exception.InvalidCredentialsException;
import it.doqui.index.ecmengine.mtom.exception.InvalidParameterException;
import it.doqui.index.ecmengineqs.business.exceptions.UnauthorizedException;
import it.doqui.index.ecmengineqs.business.services.UserService;
import it.doqui.index.ecmengineqs.foundation.UserContext;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;

import javax.inject.Inject;
import java.util.Objects;

public abstract class AbstractServiceBridge {

    @Inject
    UserContextManager userContextManager;

    @Inject
    UserService userService;

    protected void handleContext(MtomOperationContext context) {
        try {
            Objects.requireNonNull(context, "Operation content must not be null");
            Objects.requireNonNull(context.getUsername(), "Username must not be null");
            Objects.requireNonNull(context.getPassword(), "Password must not be null");
            Objects.requireNonNull(context.getRepository(), "Repository must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        String[] u = context.getUsername().split("@", 2);
        userContextManager.setContext(UserContext.builder()
            .repository(context.getRepository())
            .username(u[0])
            .tenant(u.length > 1 ? u[1]: null)
            .password(context.getPassword())
            .build());

        try {
            userService.authenticate();
        } catch (UnauthorizedException e) {
            throw new InvalidCredentialsException();
        }

    }

}
