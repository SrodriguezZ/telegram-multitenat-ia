package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.application.response.AuthenticatedUser;

public interface CurrentUserProviderPort {
    AuthenticatedUser currentUser();
}
