package ru.rooyzee.elytrixclient.client.bots.own;

/** Параметры запуска встроенных ботов (зеркалят карточку поведения). */
public class OwnBotSettings {
    public int count = 1;
    public int delayMs = 200;
    public int timeoutMs = 5000;
    public String prefix = "ElytrixBot_";
    public boolean spam;
    public String spamMessage = "Elytrix on top!";
    /** 0 = стоит, 1 = за мной (следует за игроком клиента), 2 = гулять. */
    public int mode = 0;
    public boolean autoJump = true;
    public boolean useProxy;
    public boolean rejoin;
    public int rejoinDelayMs = 5000;
    public int spamDelayMin = 150;
    public int spamDelayMax = 250;
    public boolean rotation = true;
    public boolean swing = true;
    public boolean movement = true;
    public boolean captcha = true;
    public boolean antiAfk = true;
    public boolean autoReg;
    public boolean autoLogin;
    public String password = "elytrix123";
}
