package org.strah.model.users;

/** Единый перечень ролей в системе */
public enum Role {
    ADMIN("Администратор"),
    STAFF("Сотрудник"),
    CLIENT("Клиент");

    private final String title;
    Role(String t) { this.title = t; }

    public String getTitle() { return title; }

    /** Позволяет сконвертировать строку из БД → Enum */
    public static Role fromTitle(String t) {
        for (Role r : values())
            if (r.title.equalsIgnoreCase(t)) return r;
        throw new IllegalArgumentException("Unknown role: " + t);
    }
}