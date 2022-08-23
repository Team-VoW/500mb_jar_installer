use std::cmp::min;
use std::io::{Read, Write};
use std::net::TcpStream;
use std::sync::mpsc::Sender;
use std::thread::sleep;
use std::time::Duration;

const WAIT_BETWEEN_DATA_PACKETS: Duration = Duration::from_millis(2);
const DATA_PACKET_SIZE: usize = 32768;

pub struct Connection {
    stream: TcpStream,
    pub alive: bool,
}

impl Connection {
    pub fn new(stream: TcpStream) -> Connection {
        stream
            .set_read_timeout(Option::from(Duration::from_secs(20)))
            .unwrap_or(());
        Connection {
            stream,
            alive: true,
        }
    }

    pub fn set_time_out(&self, duration: Duration) {
        self.stream
            .set_read_timeout(Option::from(duration))
            .unwrap_or(());
    }

    pub fn get_value(&mut self) -> PacketValue {
        let mut buf = [0; 1];
        match self.stream.read(&mut buf) {
            Ok(_) => match buf[0] {
                0 => {
                    let mut buf = [0; 4];
                    match self.stream.read(&mut buf) {
                        Ok(_) => {
                            let len = i32::from_be_bytes(buf);
                            let mut buf = vec![0; len as usize];
                            match self.stream.read(buf.as_mut_slice()) {
                                Ok(a) => {
                                    return PacketValue::String(
                                        String::from_utf8(buf.to_vec()).unwrap_or_else(|_| {
                                            self.alive = false;
                                            String::from("")
                                        }),
                                    );
                                }
                                Err(er) => self.alive = false,
                            }
                        }
                        Err(er) => self.alive = false,
                    }
                }
                _ => self.alive = false,
            },
            Err(er) => self.alive = false,
        }
        PacketValue::BrokenValue
    }

    pub fn send_bytes(&mut self, bytes: &[u8]) {
        match self.stream.write(bytes) {
            Ok(_) => {}
            Err(_) => {
                self.alive = false;
            }
        }
    }
}

#[derive(Debug)]
pub enum PacketValue<'a> {
    String(String),
    Integer(i32),
    ByteArray(&'a Vec<u8>),
    BrokenValue,
}

pub struct Packet<'a> {
    pub contents: Vec<PacketValue<'a>>,
}

impl Packet<'_> {
    pub fn new<'a>() -> Packet<'a> {
        Packet {
            contents: Vec::new(),
        }
    }

    pub fn from(contents: Vec<PacketValue>) -> Packet {
        Packet { contents }
    }

    pub fn send(&self, mut connection: &mut Connection) {
        let mut bytes = Vec::new();
        for value in &self.contents {
            match value {
                PacketValue::String(val) => {
                    bytes.push(0);
                    bytes.append(&mut (val.len() as i32).to_be_bytes().to_vec());
                    bytes.append(&mut String::from(val).into_bytes());
                }
                PacketValue::Integer(int) => {
                    bytes.push(1);
                    bytes.append(&mut int.to_be_bytes().to_vec());
                }
                PacketValue::ByteArray(ar) => {
                    bytes.push(2);
                    bytes.append(&mut (ar.len() as i32).to_be_bytes().to_vec());
                    connection.send_bytes(bytes.as_slice());
                    bytes = Vec::new();

                    let mut current: usize = 0;
                    let size = ar.len();
                    while current < size {
                        let to = min(size, current + DATA_PACKET_SIZE);

                        connection.send_bytes(&ar.as_slice()[current..to]);
                        current = to;
                        sleep(WAIT_BETWEEN_DATA_PACKETS);
                    }
                }
                _ => {}
            }
        }
        if bytes.len() > 0 {
            connection.send_bytes(bytes.as_slice());
        }
    }
}
