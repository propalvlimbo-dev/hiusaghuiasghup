//! Встроенный протокольный бот (offline-сервера), раскладки 26.2 (protocol 776).
//! Порт проверенной Java-версии: handshake → login → config → play, keepalive,
//! телепорты, автовоскрешение, чат/сваг/ходьба, авторег/автовход.
use crate::proto::*;
use flate2::read::ZlibDecoder;
use flate2::write::ZlibEncoder;
use flate2::Compression;
use std::io::{self, Read, Write};
use std::net::TcpStream;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::time::{SystemTime, UNIX_EPOCH};

// C2S
const L_START: i32 = 0;
const L_ACK: i32 = 3;
const CFG_CLIENT_INFO: i32 = 0;
const CFG_FINISH: i32 = 3;
const CFG_KEEPALIVE: i32 = 4;
const CFG_PONG: i32 = 5;
const CFG_KNOWN: i32 = 7;
const P_CONFIRM_TP: i32 = 0;
const P_CHAT: i32 = 9;
const P_CHUNK_BATCH: i32 = 11;
const P_CLIENT_CMD: i32 = 12;
const P_KEEPALIVE: i32 = 28;
const P_POS_ROT: i32 = 31;
const P_LOADED: i32 = 44;
const P_PONG: i32 = 45;
const P_SWING: i32 = 63;
// S2C
const SL_DISCONNECT: i32 = 0;
const SL_ENC: i32 = 1;
const SL_SUCCESS: i32 = 2;
const SL_COMPRESSION: i32 = 3;
const SC_DISCONNECT: i32 = 2;
const SC_FINISH: i32 = 3;
const SC_KEEPALIVE: i32 = 4;
const SC_PING: i32 = 5;
const SP_KEEPALIVE: i32 = 44;
const SP_PLAYER_POS: i32 = 72;
const SP_SET_HEALTH: i32 = 104;
const SP_DISCONNECT: i32 = 32;
const SP_CHUNK_START: i32 = 12;
const SP_COMBAT_KILL: i32 = 68;
const SP_PING: i32 = 61;

#[derive(Clone, serde::Deserialize, serde::Serialize)]
pub struct Settings {
    #[serde(default = "d_count")]
    pub count: i32,
    #[serde(default = "d_delay")]
    pub delay_ms: u64,
    #[serde(default = "d_timeout")]
    pub timeout_ms: u64,
    #[serde(default = "d_prefix")]
    pub prefix: String,
    #[serde(default)]
    pub spam: bool,
    #[serde(default = "d_spammsg")]
    pub spam_message: String,
    #[serde(default = "d_smin")]
    pub spam_delay_min: u64,
    #[serde(default = "d_smax")]
    pub spam_delay_max: u64,
    #[serde(default = "d_true")]
    pub rotation: bool,
    #[serde(default = "d_true")]
    pub swing: bool,
    #[serde(default = "d_true")]
    pub movement: bool,
    #[serde(default)]
    pub auto_reg: bool,
    #[serde(default)]
    pub auto_login: bool,
    #[serde(default = "d_pass")]
    pub password: String,
}
fn d_count() -> i32 { 10 }
fn d_delay() -> u64 { 200 }
fn d_timeout() -> u64 { 5000 }
fn d_prefix() -> String { "ElytrixBot_".into() }
fn d_spammsg() -> String { "Elytrix on top!".into() }
fn d_smin() -> u64 { 150 }
fn d_smax() -> u64 { 250 }
fn d_true() -> bool { true }
fn d_pass() -> String { "elytrix123".into() }

pub struct BotHandle {
    pub name: String,
    pub status: Arc<Mutex<String>>,
    pub alive: Arc<AtomicBool>,
    stop: Arc<AtomicBool>,
}

impl BotHandle {
    pub fn spawn(host: String, port: u16, protocol: i32, s: Settings, idx: i32) -> BotHandle {
        let name = format!("{}{idx}", s.prefix);
        let status = Arc::new(Mutex::new("подключение".to_string()));
        let alive = Arc::new(AtomicBool::new(true));
        let stop = Arc::new(AtomicBool::new(false));
        let (h2, a2, s2) = (host, port, s);
        let (st, al, stp) = (status.clone(), alive.clone(), stop.clone());
        let nm = name.clone();
        std::thread::Builder::new()
            .name(format!("elytrix-bot-{idx}"))
            .spawn(move || run(nm, h2, p2(port), protocol, s2, st, al, stp))
            .ok();
        BotHandle { name, status, alive, stop }
    }
    pub fn stop(&self) {
        self.stop.store(true, Ordering::SeqCst);
    }
}
fn p2(p: u16) -> u16 { p }

fn now_ms() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_millis() as u64).unwrap_or(0)
}

struct Conn {
    rd: TcpStream,
    wr: Arc<Mutex<TcpStream>>,
    compression: bool,
    threshold: i32,
}

impl Conn {
    fn send(&self, id: i32, body: &[u8]) -> io::Result<()> {
        let mut payload = Vec::new();
        payload.extend(varint_bytes(id));
        payload.extend(body);
        let w = self.wr.lock().unwrap();
        let mut w = w;
        if self.compression {
            if self.threshold > 0 && payload.len() as i32 >= self.threshold {
                let mut enc = ZlibEncoder::new(Vec::new(), Compression::default());
                enc.write_all(&payload)?;
                let z = enc.finish()?;
                let mut frame = Vec::new();
                frame.extend(varint_bytes(payload.len() as i32));
                frame.extend(z);
                let mut out = Vec::new();
                out.extend(varint_bytes(frame.len() as i32));
                out.extend(frame);
                w.write_all(&out)?;
            } else {
                let mut frame = Vec::new();
                frame.extend(varint_bytes(0));
                frame.extend(payload);
                let mut out = Vec::new();
                out.extend(varint_bytes(frame.len() as i32));
                out.extend(frame);
                w.write_all(&out)?;
            }
        } else {
            let mut out = Vec::new();
            out.extend(varint_bytes(payload.len() as i32));
            out.extend(payload);
            w.write_all(&out)?;
        }
        w.flush()
    }

    fn read_frame(&mut self) -> io::Result<(i32, Vec<u8>)> {
        let len = read_varint(&mut self.rd)? as usize;
        let mut data = vec![0u8; len];
        self.rd.read_exact(&mut data)?;
        let mut off = 0usize;
        if self.compression {
            let data_len = read_varint_buf(&data, &mut off);
            if data_len > 0 {
                let mut dec = ZlibDecoder::new(&data[off..]);
                let mut out = Vec::new();
                dec.read_to_end(&mut out)?;
                data = out;
                off = 0;
            }
        }
        let id = read_varint_buf(&data, &mut off);
        Ok((id, data[off..].to_vec()))
    }
}

#[allow(clippy::too_many_arguments)]
fn run(
    name: String,
    host: String,
    port: u16,
    protocol: i32,
    s: Settings,
    status: Arc<Mutex<String>>,
    alive: Arc<AtomicBool>,
    stop: Arc<AtomicBool>,
) {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        inner(&name, &host, port, protocol, &s, &status, &stop)
    }));
    let msg = match result {
        Ok(Ok(())) => "остановлен".to_string(),
        Ok(Err(e)) => format!("ошибка: {e}"),
        Err(_) => "паника".to_string(),
    };
    *status.lock().unwrap() = msg.clone();
    alive.store(false, Ordering::SeqCst);
    let _ = stop;
}

fn inner(
    name: &str,
    host: &str,
    port: u16,
    protocol: i32,
    s: &Settings,
    status: &Arc<Mutex<String>>,
    stop: &AtomicBool,
) -> io::Result<()> {
    let sock = TcpStream::connect((host.to_string(), port))?;
    sock.set_read_timeout(Some(std::time::Duration::from_millis(300)))?;
    let wr = sock.try_clone()?;
    let mut c = Conn { rd: sock, wr: Arc::new(Mutex::new(wr)), compression: false, threshold: 0 };

    // handshake -> login
    let mut b = Vec::new();
    write_varint(&mut b, protocol)?;
    write_str(&mut b, host)?;
    write_u16(&mut b, port)?;
    write_varint(&mut b, 2)?;
    c.send(0, &b)?;
    let (ms, ls) = offline_uuid(name);
    let mut b = Vec::new();
    write_str(&mut b, name)?;
    write_i64(&mut b, ms)?;
    write_i64(&mut b, ls)?;
    c.send(L_START, &b)?;

    let mut state = "login";
    let mut x = 0f64; let mut y = 0f64; let mut z = 0f64;
    let mut yaw = 0f32; let mut pitch = 0f32;
    let mut have_pos = false;
    let mut tick_at = 0u64;
    let mut last_read = now_ms();
    let mut next_chat = now_ms() + 1500;
    let mut next_swing = 0u64;
    let mut next_turn = 0u64;
    let mut reg_at: i64 = -1; let mut login_at: i64 = -1;
    let mut reg_sent = false; let mut login_sent = false;
    let mut walk_x = 0f64; let mut walk_z = 0f64;
    let mut walking = false;
    let mut next_walk = 0u64;

    loop {
        if stop.load(Ordering::SeqCst) {
            return Ok(());
        }
        match c.read_frame() {
            Ok((id, data)) => {
                last_read = now_ms();
                let keep = handle(&mut c, state, id, &data, status, &mut x, &mut y, &mut z, &mut yaw, &mut pitch, &mut have_pos, &mut reg_at, &mut login_at, s)?;
                if !keep { return Ok(()); }
                if state == "login" && id == SL_SUCCESS { state = "config"; }
                else if state == "config" && id == SC_FINISH { state = "play"; }
            }
            Err(e) => {
                let k = e.kind();
                if k == io::ErrorKind::WouldBlock || k == io::ErrorKind::TimedOut {
                    let now = now_ms();
                    if now - last_read > s.timeout_ms {
                        *status.lock().unwrap() = "таймаут".into();
                        return Ok(());
                    }
                    if state == "play" && now >= tick_at {
                        tick_at = now + 50;
                        tick(&c, s, now, &mut x, &mut y, &mut z, &mut yaw, &mut pitch, have_pos, &mut next_chat, &mut next_swing, &mut next_turn, &mut reg_at, &mut login_at, &mut reg_sent, &mut login_sent, &mut walk_x, &mut walk_z, &mut walking, &mut next_walk);
                    }
                } else {
                    return Err(e);
                }
            }
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn handle(
    c: &mut Conn,
    state: &str,
    id: i32,
    data: &[u8],
    status: &Arc<Mutex<String>>,
    x: &mut f64, y: &mut f64, z: &mut f64,
    yaw: &mut f32, pitch: &mut f32,
    have_pos: &mut bool,
    reg_at: &mut i64, login_at: &mut i64,
    s: &Settings,
) -> io::Result<bool> {
    let mut off = 0usize;
    match state {
        "login" => match id {
            SL_COMPRESSION => { c.threshold = read_varint_buf(data, &mut off); c.compression = true; }
            SL_ENC => { *status.lock().unwrap() = "кик: сервер в online-mode".into(); return Ok(false); }
            SL_DISCONNECT => { *status.lock().unwrap() = format!("кик: {}", component(data)); return Ok(false); }
            SL_SUCCESS => {
                c.send(L_ACK, &[])?;
                let mut b = Vec::new();
                write_str(&mut b, "ru_ru")?;
                b.push(8);
                write_varint(&mut b, 0)?;
                b.push(1);
                b.push(127);
                write_varint(&mut b, 1)?;
                b.push(0);
                b.push(1);
                write_varint(&mut b, 0)?; // particle status (есть в 26.2)
                c.send(CFG_CLIENT_INFO, &b)?;
                c.send(CFG_KNOWN, &varint_bytes(0))?;
            }
            _ => {}
        },
        "config" => match id {
            SC_KEEPALIVE => { let v = read_i64(data, &mut off); let mut b = Vec::new(); write_i64(&mut b, v)?; c.send(CFG_KEEPALIVE, &b)?; }
            SC_PING => { let v = read_i32(data, &mut off); let mut b = Vec::new(); write_i32(&mut b, v)?; c.send(CFG_PONG, &b)?; }
            SC_DISCONNECT => { *status.lock().unwrap() = format!("кик: {}", component(data)); return Ok(false); }
            SC_FINISH => {
                c.send(CFG_FINISH, &[])?;
                c.send(P_LOADED, &[])?;
                let now = now_ms() as i64;
                if s.auto_reg { *reg_at = now + 1000; }
                if s.auto_login || s.auto_reg { *login_at = now + if s.auto_reg { 2500 } else { 1000 }; }
            }
            _ => {}
        },
        "play" => match id {
            SP_KEEPALIVE => { let v = read_i64(data, &mut off); let mut b = Vec::new(); write_i64(&mut b, v)?; c.send(P_KEEPALIVE, &b)?; }
            SP_PING => { let v = read_i32(data, &mut off); let mut b = Vec::new(); write_i32(&mut b, v)?; c.send(P_PONG, &b)?; }
            SP_PLAYER_POS => {
                let tp = read_varint_buf(data, &mut off);
                let nx = read_f64(data, &mut off);
                let ny = read_f64(data, &mut off);
                let nz = read_f64(data, &mut off);
                read_f64(data, &mut off); read_f64(data, &mut off); read_f64(data, &mut off);
                let nyaw = read_f32(data, &mut off);
                let npitch = read_f32(data, &mut off);
                let rel = read_i32(data, &mut off);
                *x = if rel & 1 != 0 { nx + *x } else { nx };
                *y = if rel & 2 != 0 { ny + *y } else { ny };
                *z = if rel & 4 != 0 { nz + *z } else { nz };
                *yaw = if rel & 8 != 0 { nyaw + *yaw } else { nyaw };
                *pitch = if rel & 16 != 0 { npitch + *pitch } else { npitch };
                *have_pos = true;
                c.send(P_CONFIRM_TP, &varint_bytes(tp))?;
                send_pos_rot(c, *x, *y, *z, *yaw, *pitch)?;
            }
            SP_SET_HEALTH => { let hp = read_f32(data, &mut off); if hp <= 0.0 { c.send(P_CLIENT_CMD, &varint_bytes(0))?; } }
            SP_COMBAT_KILL => { c.send(P_CLIENT_CMD, &varint_bytes(0))?; }
            SP_CHUNK_START => { let mut b = Vec::new(); write_f32(&mut b, 10.0)?; c.send(P_CHUNK_BATCH, &b)?; }
            SP_DISCONNECT => { *status.lock().unwrap() = format!("кик: {}", component(data)); return Ok(false); }
            _ => {}
        },
        _ => {}
    }
    Ok(true)
}

fn send_pos_rot(c: &Conn, x: f64, y: f64, z: f64, yaw: f32, pitch: f32) -> io::Result<()> {
    let mut b = Vec::new();
    write_f64(&mut b, x)?; write_f64(&mut b, y)?; write_f64(&mut b, z)?;
    write_f32(&mut b, yaw)?; write_f32(&mut b, pitch)?;
    b.push(1);
    c.send(P_POS_ROT, &b)
}

fn send_chat(c: &Conn, msg: &str, now: u64) -> io::Result<()> {
    let mut b = Vec::new();
    write_str(&mut b, msg)?;
    write_i64(&mut b, now as i64)?;
    write_i64(&mut b, 0)?;
    b.push(0);
    write_varint(&mut b, 0)?;
    b.extend([0u8; 3]);
    b.push(0);
    c.send(P_CHAT, &b)
}

#[allow(clippy::too_many_arguments)]
fn tick(
    c: &Conn, s: &Settings, now: u64,
    x: &mut f64, y: &mut f64, z: &mut f64, yaw: &mut f32, pitch: &mut f32,
    have_pos: bool,
    next_chat: &mut u64, next_swing: &mut u64, next_turn: &mut u64,
    reg_at: &mut i64, login_at: &mut i64, reg_sent: &mut bool, login_sent: &mut bool,
    walk_x: &mut f64, walk_z: &mut f64, walking: &mut bool, next_walk: &mut u64,
) {
    let r = rand01();
    if *reg_at > 0 && !*reg_sent && (now as i64) >= *reg_at {
        *reg_sent = true;
        let _ = send_chat(c, &format!("/register {} {}", s.password, s.password), now);
    }
    if *login_at > 0 && !*login_sent && (now as i64) >= *login_at {
        *login_sent = true;
        let _ = send_chat(c, &format!("/login {}", s.password), now);
    }
    if s.movement {
        if now >= *next_walk {
            *next_walk = now + 2000 + (r * 4000.0) as u64;
            *walking = r > 0.5;
            let a = r * std::f64::consts::TAU;
            *walk_x = a.sin();
            *walk_z = -a.cos();
        }
        if *walking {
            *x += *walk_x * 0.09;
            *z += *walk_z * 0.09;
            *yaw = ((*walk_x).atan2(-*walk_z).to_degrees()) as f32;
        }
    } else if s.rotation && now >= *next_turn {
        *next_turn = now + 1500 + (r * 3000.0) as u64;
        *yaw = (r * 360.0) as f32;
        *pitch = -20.0 + (r * 60.0) as f32;
    }
    if have_pos { let _ = send_pos_rot(c, *x, *y, *z, *yaw, *pitch); }
    if s.swing && now >= *next_swing {
        *next_swing = now + 1500 + (r * 2500.0) as u64;
        let _ = c.send(P_SWING, &varint_bytes(0));
    }
    if s.spam && now >= *next_chat {
        let min = s.spam_delay_min.max(1);
        let span = s.spam_delay_max.saturating_sub(min).max(1);
        *next_chat = now + min + (r * span as f64) as u64;
        let _ = send_chat(c, &s.spam_message, now);
    }
}

fn rand01() -> f64 {
    use std::time::SystemTime;
    let n = SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.subsec_nanos()).unwrap_or(0);
    (n as f64 % 1000.0) / 1000.0
}

fn component(data: &[u8]) -> String {
    let mut off = 0usize;
    if data.is_empty() { return "(пусто)".into(); }
    let t = data[off]; off += 1;
    nbt_walk(data, &mut off, t, 0).unwrap_or_else(|| "(не читается)".into())
}

fn nbt_walk(d: &[u8], off: &mut usize, t: u8, depth: i32) -> Option<String> {
    if depth > 8 || *off >= d.len() { return None; }
    match t {
        1 => { *off += 1; None }
        2 => { *off += 2; None }
        3 => { *off += 4; None }
        4 => { *off += 8; None }
        5 => { *off += 4; None }
        6 => { *off += 8; None }
        7 => { let n = read_i32(d, off) as usize; *off += n; None }
        8 => {
            let n = u16::from_be_bytes(d[*off..*off + 2].try_into().unwrap()) as usize;
            *off += 2;
            let s = String::from_utf8_lossy(&d[*off..*off + n]).into_owned();
            *off += n;
            Some(s)
        }
        9 => {
            let et = d[*off]; *off += 1;
            let n = read_i32(d, off) as usize;
            for _ in 0..n { if let Some(s) = nbt_walk(d, off, et, depth + 1) { return Some(s); } }
            None
        }
        10 => loop {
            let et = d[*off]; *off += 1;
            if et == 0 { return None; }
            let nl = u16::from_be_bytes(d[*off..*off + 2].try_into().unwrap()) as usize;
            *off += 2 + nl;
            if let Some(s) = nbt_walk(d, off, et, depth + 1) { return Some(s); }
        },
        11 => { let n = read_i32(d, off) as usize; *off += 4 * n; None }
        12 => { let n = read_i32(d, off) as usize; *off += 8 * n; None }
        _ => None,
    }
}
