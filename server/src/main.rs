extern crate core;

pub mod utils {
    pub mod file_loading;
    pub mod socket_communication;
}

use crate::utils::file_loading::{load_archive, Archive, File};
use crate::utils::socket_communication::{Connection, Packet, PacketValue};
use crc32fast::Hasher;
use std::alloc::System;
use std::cmp::{max, min};
use std::collections::hash_map::DefaultHasher;
use std::collections::HashMap;
use std::fs::read;
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};
use std::path::Path;
use std::thread::sleep;
use std::time::Duration;
use std::{env, fs, thread};
use yaml_rust::{Yaml, YamlLoader};

const MINIMUM_KB_PER_SECOND: u64 = 50;

fn handle_client(mut connection: Connection) {

    let req = match connection.get_value() {
        PacketValue::String(a) => {a}
        _ => {
            return;
        }
    };

    if req == "list" {
        let mut current_packet = Packet::new();
        unsafe {
            for arch in &FILES {
                current_packet.contents.push(PacketValue::String(
                     String::from(&arch.name) + "~=/" + &arch.recommended_file_name + "~=/" + &arch.hash.to_string(),
                ));
            }
            current_packet.contents.push(PacketValue::String(
                String::from("+end*")
            ));
        }
        current_packet.send(&mut connection);
        return;
    }

    let mut archive = None;
    unsafe {
        for x in &FILES {
            if x.name == req {
                archive = Some(x);
            }
        }
    }

    let archive = match archive {
        None => {
            return;
        }
        Some(a) => a,
    };

    let files = &archive.files;

    let mut i = 60;
    let mut current_packet = Packet::new();
    for file in files {
        i -= 1;
        current_packet.contents.push(PacketValue::String(
            String::from(file.0) + "~/~" + &file.1.hash.to_string(),
        ));
        if i <= 0 {
            current_packet.send(&mut connection);
            current_packet = Packet::new();
            i = 60;
        }
    }
    current_packet.send(&mut connection);
    Packet::from(vec![PacketValue::String(String::from("~end~"))]).send(&mut connection);

    while connection.alive {
        let request = connection.get_value();
        let request = match request {
            PacketValue::String(a) => a,
            _ => {
                return;
            }
        };
        if !connection.alive {
            return;
        }

        let answer = files.get(&request);
        let answer = match answer {
            None => {
                connection.alive = false;
                return;
            }
            Some(a) => a,
        };

        connection.set_time_out(Duration::from_millis(
            answer.contents.len() as u64 / MINIMUM_KB_PER_SECOND,
        ));

        Packet::from(vec![PacketValue::ByteArray(&answer.contents)]).send(&mut connection);
    }
}

static mut FILES: Vec<Archive> = Vec::new();

fn main() {
    println!("Hello, world!");

    let args: Vec<String> = env::args().collect();

    let config = YamlLoader::load_from_str(
        &fs::read_to_string(args.get(1).expect("You specified non existing config file"))
            .expect("Something went wrong opening yml file"),
    )
    .expect("Bad yml?");

    let config = &config[0];

    for x in config["files"].as_hash().unwrap().iter() {
        let name = x.0.as_str().unwrap();
        unsafe {
            FILES.push(load_archive(
                String::from(config["files"][name]["name"].as_str().unwrap()),
                String::from(config["files"][name]["recommended-name"].as_str().unwrap()),
                String::from(config["files"][name]["path"].as_str().unwrap()),
            ));
        }
    }

    let ip = config["ip"].as_str().unwrap();

    let listener = TcpListener::bind(ip).unwrap();

    let to_wait = Duration::from_millis(50);

    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                thread::spawn(|| {
                    println!("User connected");
                    handle_client(Connection::new(stream));
                    println!("User disconnected");
                });
            }
            Err(e) => {
                println!("Unable to connect: {}", e);
            }
        }
        sleep(to_wait);
    }
}
