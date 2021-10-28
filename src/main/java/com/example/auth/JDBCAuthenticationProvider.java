package com.example.auth;

import com.example.auth.persistence.UserEntity;
import com.example.auth.persistence.UserRepository;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

public class JDBCAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCAuthenticationProvider.class);
    private final UserRepository users;

    public JDBCAuthenticationProvider(UserRepository users) {
        this.users = users;
    }

    @Override
    public Publisher<AuthenticationResponse> authenticate(
            @Nullable final HttpRequest<?> httpRequest,
            final AuthenticationRequest<?, ?> authenticationRequest) {
        return Flowable.create(flowableEmitter -> {
            final String identity = (String) authenticationRequest.getIdentity();
            LOG.debug("User {} tries to login...", identity);

            final Optional<UserEntity> maybeUser = users.findByEmail(identity);
            if (maybeUser.isPresent()) {
                LOG.debug("Found user: {}", maybeUser.get().getEmail());
                final String secret = (String) authenticationRequest.getSecret();
                if (maybeUser.get().getPassword().equals(secret)) {
                    // pass
                    LOG.debug("User logged in.");
                    flowableEmitter.onNext(new UserDetails(identity, new ArrayList<>()));
                    flowableEmitter.onComplete();
                    return;
                } else {
                    LOG.debug("Wrong username or password!");
                }
            } else {
                LOG.debug("No user found with email: {}", identity);
            }

            flowableEmitter.onError(new AuthenticationException(new AuthenticationFailed("Wrong username or password!")));
        }, BackpressureStrategy.ERROR);
    }
}
