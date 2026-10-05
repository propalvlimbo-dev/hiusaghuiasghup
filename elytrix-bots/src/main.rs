//! Elytrix bot daemon. Управляется по HTTP JSON с клиента:
//!   GET  /status  -> {running, alive, total, bots:[{name,status}]}
//!   POST /start   -> Settings{host,port,count,...}
//!   POST /stop    -> {}
mod bot;
mod proto;

use bot::{BotHandle, Settings};
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::Mutex;

struct World {
    host: String,
    port: u16,
    protocol: i32,
    bots: Vec<BotHandle>,
    running: bool,
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let mut port = 25570u16;
    let mut i = 1;
    while i < args.len() {
        if args[i] == "--port" {
            if let Some(v) = args.get(i + 1) { port = v.parse().unwrap_or(25570); }
            i += 2;
        } else { i += 1; }
    }
    let listener = TcpListener::bind(("127.0.0.1", port)).expect("bind");
    println!("[elytrix-bots] daemon на 127.0.0.1:{port}");
    let world = Mutex::new(World { host: String::new(), port: 0, protocol: 776, bots: vec![], running: false });
    for s in listener.incoming() {
        if let Ok(s) = s { handle(s, &world); }
    }
}

fn handle(mut s: TcpStream, world: &Mutex<World>) {
    s.set_read_timeout(Some(std::time::Duration::from_millis(2000))).ok();
    let mut buf = Vec::new();
    let mut tmp = [0u8; 4096];
    // читаем заголовки
    let header_end = loop {
        match s.read(&mut tmp) {
            Ok(0) => break None,
            Ok(n) => {
                buf.extend(&tmp[..n]);
                if let Some(p) = find_seq(&buf, b"\r\n\r\n") { break Some(p); }
            }
            Err(_) => break None,
        }
    };
    let Some(hend) = header_end else { return; };
    let head = String::from_utf8_lossy(&buf[..hend]).into_owned();
    let mut lines = head.lines();
    let req = lines.next().unwrap_or("");
    let mut parts = req.split_whitespace();
    let method = parts.next().unwrap_or("");
    let path = parts.next().unwrap_or("");
    let clen = head
        .lines()
        .find_map(|l| {
            let l = l.to_lowercase();
            l.strip_prefix("content-length:").map(|v| v.trim().parse::<usize>().unwrap_or(0))
        })
        .unwrap_or(0);
    let mut body = buf[hend + 4..].to_vec();
    while body.len() < clen {
        match s.read(&mut tmp) {
            Ok(0) => break,
            Ok(n) => body.extend(&tmp[..n]),
            Err(_) => break,
        }
    }

    let resp = route(method, path, &body, world);
    let out = format!(
        "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: {}\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n{}",
        resp.len(),
        resp
    );
    let _ = s.write_all(out.as_bytes());
    let _ = s.flush();
}

fn route(method: &str, path: &str, body: &[u8], world: &Mutex<World>) -> String {
    let mut w = world.lock().unwrap();
    match (method, path) {
        ("GET", "/status") => status_json(&w),
        ("POST", "/start") => {
            if w.running {
                return "{\"ok\":false,\"error\":\"already running\"}".into();
            }
            let mut s: Settings = serde_json::from_slice(body).unwrap_or_else(|_| Settings {
                count: 10, delay_ms: 200, timeout_ms: 5000, prefix: "ElytrixBot_".into(),
                spam: false, spam_message: "Elytrix on top!".into(), spam_delay_min: 150,
                spam_delay_max: 250, rotation: true, swing: true, movement: true,
                auto_reg: false, auto_login: false, password: "elytrix123".into(),
            });
            let cfg: serde_json::Value = serde_json::from_slice(body).unwrap_or_default();
            let host = cfg.get("host").and_then(|v| v.as_str()).unwrap_or("127.0.0.1").to_string();
            let port = cfg.get("port").and_then(|v| v.as_u64()).unwrap_or(25565) as u16;
            let proto = probe_protocol(&host, port).unwrap_or(776);
            println!("[elytrix-bots] старт {} ботов на {host}:{port} (protocol {proto})", s.count);
            w.host = host.clone(); w.port = port; w.protocol = proto;
            for i in 1..=s.count {
                let h = BotHandle::spawn(host.clone(), port, proto, s.clone(), i);
                w.bots.push(h);
                std::thread::sleep(std::time::Duration::from_millis(s.delay_ms));
            }
            w.running = true;
            format!("{{\"ok\":true,\"protocol\":{proto}}}")
        }
        ("POST", "/stop") => {
            for b in w.bots.iter() { b.stop(); }
            w.bots.clear();
            w.running = false;
            "{\"ok\":true}".into()
        }
        _ => "{\"ok\":false,\"error\":\"not found\"}".into(),
    }
}

fn status_json(w: &World) -> String {
    let alive = w.bots.iter().filter(|b| b.alive.load(std::sync::atomic::Ordering::SeqCst)).count();
    let mut bots = String::new();
    for (i, b) in w.bots.iter().enumerate() {
        if i > 0 { bots.push(','); }
        let st = b.status.lock().unwrap().clone();
        bots.push_str(&format!("{{\"name\":\"{}\",\"status\":\"{}\"}}", esc(&b.name), esc(&st)));
    }
    format!(
        "{{\"running\":{},\"alive\":{},\"total\":{},\"bots\":[{}]}}",
        w.running, alive, w.bots.len(), bots
    )
}

fn esc(s: &str) -> String {
    s.replace('\\', "\\\\").replace('"', "\\\"")
}

fn find_seq(h: &[u8], n: &[u8]) -> Option<usize> {
    h.windows(n.len()).position(|w| w == n)
}

/// Узнаёт протокол сервера через status-ping.
fn probe_protocol(host: &str, port: u16) -> Option<i32> {
    let mut s = TcpStream::connect((host.to_string(), port)).ok()?;
    s.set_read_timeout(Some(std::time::Duration::from_millis(2000))).ok();
    let mut hs = Vec::new();
    proto::write_varint(&mut hs, 0).ok()?;
    proto::write_varint(&mut hs, 776).ok()?;
    proto::write_str(&mut hs, host).ok()?;
    proto::write_u16(&mut hs, port).ok()?;
    proto::write_varint(&mut hs, 1).ok()?;
    send_raw(&mut s, &hs)?;
    send_raw(&mut s, &proto::varint_bytes(0))?;
    let len = proto::read_varint(&mut s).ok()? as usize;
    let mut data = vec![0u8; len];
    s.read_exact(&mut data).ok()?;
    let mut off = 0usize;
    let id = proto::read_varint_buf(&data, &mut off);
    if id != 0 { return None; }
    let json = proto::read_str(&data, &mut off);
    let v: serde_json::Value = serde_json::from_str(&json).ok()?;
    v.pointer("/version/protocol")?.as_i64().map(|p| p as i32)
}

fn send_raw(s: &mut TcpStream, payload: &[u8]) -> Option<()> {
    let mut out = Vec::new();
    proto::write_varint(&mut out, payload.len() as i32).ok()?;
    out.extend(payload);
    s.write_all(&out).ok()?;
    s.flush().ok()
}
