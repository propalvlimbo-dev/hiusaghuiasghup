//! Каркас протокола Minecraft: VarInt, кадры, сжатие. Раскладки пакетов 26.2 — в bot.rs.
use std::io::{self, Read, Write};

pub fn read_varint<R: Read>(r: &mut R) -> io::Result<i32> {
    let mut value = 0i32;
    let mut pos = 0;
    loop {
        let mut b = [0u8; 1];
        r.read_exact(&mut b)?;
        value |= ((b[0] & 0x7F) as i32) << pos;
        if b[0] & 0x80 == 0 {
            return Ok(value);
        }
        pos += 7;
        if pos >= 32 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "VarInt too big"));
        }
    }
}

pub fn read_varint_buf(data: &[u8], off: &mut usize) -> i32 {
    let mut value = 0i32;
    let mut pos = 0;
    loop {
        let b = data[*off];
        *off += 1;
        value |= ((b & 0x7F) as i32) << pos;
        if b & 0x80 == 0 {
            return value;
        }
        pos += 7;
        assert!(pos < 32, "VarInt too big");
    }
}

pub fn write_varint<W: Write>(w: &mut W, mut v: i32) -> io::Result<()> {
    loop {
        if (v & !0x7F) != 0 {
            w.write_all(&[((v & 0x7F) | 0x80) as u8])?;
            v = ((v as u32) >> 7) as i32;
        } else {
            w.write_all(&[v as u8])?;
            return Ok(());
        }
    }
}

pub fn varint_bytes(mut v: i32) -> Vec<u8> {
    let mut out = Vec::new();
    loop {
        if (v & !0x7F) != 0 {
            out.push(((v & 0x7F) | 0x80) as u8);
            v = ((v as u32) >> 7) as i32;
        } else {
            out.push(v as u8);
            return out;
        }
    }
}

pub fn write_str<W: Write>(w: &mut W, s: &str) -> io::Result<()> {
    let b = s.as_bytes();
    write_varint(w, b.len() as i32)?;
    w.write_all(b)
}

pub fn write_u16<W: Write>(w: &mut W, v: u16) -> io::Result<()> {
    w.write_all(&v.to_be_bytes())
}

pub fn write_i32<W: Write>(w: &mut W, v: i32) -> io::Result<()> {
    w.write_all(&v.to_be_bytes())
}

pub fn write_i64<W: Write>(w: &mut W, v: i64) -> io::Result<()> {
    w.write_all(&v.to_be_bytes())
}

pub fn write_f32<W: Write>(w: &mut W, v: f32) -> io::Result<()> {
    w.write_all(&v.to_be_bytes())
}

pub fn write_f64<W: Write>(w: &mut W, v: f64) -> io::Result<()> {
    w.write_all(&v.to_be_bytes())
}

pub fn read_i32(data: &[u8], off: &mut usize) -> i32 {
    let v = i32::from_be_bytes(data[*off..*off + 4].try_into().unwrap());
    *off += 4;
    v
}

pub fn read_i64(data: &[u8], off: &mut usize) -> i64 {
    let v = i64::from_be_bytes(data[*off..*off + 8].try_into().unwrap());
    *off += 8;
    v
}

pub fn read_f32(data: &[u8], off: &mut usize) -> f32 {
    let v = f32::from_be_bytes(data[*off..*off + 4].try_into().unwrap());
    *off += 4;
    v
}

pub fn read_f64(data: &[u8], off: &mut usize) -> f64 {
    let v = f64::from_be_bytes(data[*off..*off + 8].try_into().unwrap());
    *off += 8;
    v
}

pub fn read_str(data: &[u8], off: &mut usize) -> String {
    let len = read_varint_buf(data, off) as usize;
    let s = String::from_utf8_lossy(&data[*off..*off + len]).into_owned();
    *off += len;
    s
}

/// Оффлайн-uuid как у ваниллы: md5("OfflinePlayer:" + name) в формате UUID.
pub fn offline_uuid(name: &str) -> (i64, i64) {
    use md5::{Digest, Md5};
    let mut h = Md5::new();
    h.update(format!("OfflinePlayer:{name}").as_bytes());
    let d = h.finalize();
    let mut ms = [0u8; 8];
    ms.copy_from_slice(&d[..8]);
    let mut ls = [0u8; 8];
    ls.copy_from_slice(&d[8..]);
    // version/variant bits как у UUID v3
    ms[6] = (ms[6] & 0x0F) | 0x30;
    ls[0] = (ls[0] & 0x3F) | 0x80;
    (i64::from_be_bytes(ms), i64::from_be_bytes(ls))
}
