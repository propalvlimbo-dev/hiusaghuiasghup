package ru.rooyzee.elytrixclient.client.bots.own;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Папка ботов: одна папка = один сервер, свой пул ников и свои настройки входа.
 * Сохраняется в .minecraft/elytrix/bot-folders.json.
 */
public class BotFolder {
    public String name = "Сервер";
    public String address = "";
    public boolean useProxy = true;
    public boolean autoReg = true;
    public boolean autoLogin = true;
    public String password = "elytrix123";
    public int connectCount = 2;
    public int delayMs = 500;
    public List<String> accounts = new ArrayList<>();
    public Set<String> banned = new HashSet<>();
    /** Физика/поведение ботов этого сервера. */
    public boolean antiAfk = true;
    public boolean rotation = true;
    public boolean swing = true;
    public boolean autoJump = true;
    public boolean captcha = true;

    /** Следующий ник, не забаненный и не равный keep. null, если свободных нет. */
    public synchronized String nextAccount(String keep) {
        for (String a : accounts) {
            if (!banned.contains(a) && !a.equals(keep)) {
                return a;
            }
        }
        return null;
    }

    /** Все незабаненные ники. */
    public synchronized List<String> freeAccounts() {
        List<String> r = new ArrayList<>();
        for (String a : accounts) {
            if (!banned.contains(a)) {
                r.add(a);
            }
        }
        return r;
    }

    public synchronized void addAccount(String nick) {
        if (nick != null && !nick.isBlank() && !accounts.contains(nick.trim())) {
            accounts.add(nick.trim());
        }
    }
}
