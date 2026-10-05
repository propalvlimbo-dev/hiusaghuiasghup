package ru.rooyzee.elytrixclient.client.bots.own;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Прокси для ботов. Принимаются строки вида {@code ip:port} и {@code ip:port:user:pass}.
 * Подключение: сначала SOCKS5 (с логином/паролем, если заданы), если прокси
 * не SOCKS — пробуем HTTP CONNECT с Basic-авторизацией.
 */
public final class BotProxy {

    public final String host;
    public final int port;
    public final String user;
    public final String pass;

    public BotProxy(String host, int port, String user, String pass) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
    }

    /** null, если строка не распознана. */
    public static BotProxy parse(String line) {
        if (line == null) {
            return null;
        }
        String t = line.trim();
        if (t.isEmpty() || t.startsWith("#")) {
            return null;
        }
        String[] p = t.split(":");
        if (p.length < 2) {
            return null;
        }
        try {
            int port = Integer.parseInt(p[1].trim());
            String u = p.length > 2 && !p[2].trim().isEmpty() ? p[2].trim() : null;
            String pw = p.length > 3 ? p[3].trim() : null;
            return new BotProxy(p[0].trim(), port, u, pw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean hasAuth() {
        return user != null && !user.isEmpty();
    }

    public Socket connect(String targetHost, int targetPort, int timeoutMs) throws IOException {
        Socket s = new Socket(host, port);
        s.setSoTimeout(timeoutMs);
        try {
            socks5(s, targetHost, targetPort);
            s.setSoTimeout(30000);
            return s;
        } catch (IOException socksFail) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
        s = new Socket(host, port);
        s.setSoTimeout(timeoutMs);
        httpConnect(s, targetHost, targetPort);
        s.setSoTimeout(30000);
        return s;
    }

    private void socks5(Socket s, String th, int tp) throws IOException {
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();
        out.write(new byte[]{5, 1, (byte) (hasAuth() ? 2 : 0)}, 0, 3);
        out.flush();
        byte[] r = in.readNBytes(2);
        if (r.length < 2 || r[0] != 5) {
            throw new IOException("не SOCKS5");
        }
        if (r[1] == 2) {
            if (!hasAuth()) {
                throw new IOException("SOCKS5 требует пароль");
            }
            byte[] ub = user.getBytes(StandardCharsets.UTF_8);
            byte[] pb = (pass == null ? "" : pass).getBytes(StandardCharsets.UTF_8);
            byte[] m = new byte[3 + ub.length + pb.length];
            m[0] = 1;
            m[1] = (byte) ub.length;
            System.arraycopy(ub, 0, m, 2, ub.length);
            m[2 + ub.length] = (byte) pb.length;
            System.arraycopy(pb, 0, m, 3 + ub.length, pb.length);
            out.write(m);
            out.flush();
            byte[] ar = in.readNBytes(2);
            if (ar.length < 2 || ar[1] != 0) {
                throw new IOException("SOCKS5: логин/пароль не подошли");
            }
        } else if (r[1] != 0) {
            throw new IOException("SOCKS5: метод " + r[1]);
        }
        byte[] db = th.getBytes(StandardCharsets.UTF_8);
        byte[] req = new byte[7 + db.length];
        req[0] = 5;
        req[1] = 1;
        req[2] = 0;
        req[3] = 3;
        req[4] = (byte) db.length;
        System.arraycopy(db, 0, req, 5, db.length);
        req[5 + db.length] = (byte) ((tp >> 8) & 0xFF);
        req[6 + db.length] = (byte) (tp & 0xFF);
        out.write(req);
        out.flush();
        byte[] resp = in.readNBytes(10);
        if (resp.length < 2 || resp[1] != 0) {
            throw new IOException("SOCKS5: connect отказ (" + (resp.length > 1 ? resp[1] : "?") + ")");
        }
    }

    private void httpConnect(Socket s, String th, int tp) throws IOException {
        OutputStream out = s.getOutputStream();
        String auth = hasAuth()
                ? "Proxy-Authorization: Basic " + Base64.getEncoder()
                        .encodeToString((user + ":" + (pass == null ? "" : pass)).getBytes(StandardCharsets.UTF_8)) + "\r\n"
                : "";
        String req = "CONNECT " + th + ":" + tp + " HTTP/1.1\r\nHost: " + th + ":" + tp + "\r\n" + auth + "\r\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        InputStream in = s.getInputStream();
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && sb.length() < 2048) {
            sb.append((char) c);
            if (sb.length() >= 4 && "\r\n\r\n".contentEquals(sb.subSequence(sb.length() - 4, sb.length()))) {
                break;
            }
        }
        String head = sb.toString();
        if (!head.contains(" 200")) {
            throw new IOException("HTTP-прокси ответил: " + head.trim().replace('\n', ' '));
        }
    }

    @Override
    public String toString() {
        return host + ":" + port + (hasAuth() ? " (с логином)" : "");
    }
}
