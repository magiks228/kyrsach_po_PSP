package org.strah.utils;

public interface Authenticable {
    boolean authenticate(String login, String password);
}
