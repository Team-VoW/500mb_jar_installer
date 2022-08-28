extern crate core;

use std::{env, fs};
use std::fs::metadata;
use yaml_rust::YamlLoader;
use crate::utils::file_loading::unarchive_archive;

pub mod utils {
    pub mod file_loading;
}

fn main() {
    println!("Hello, world!");

    let args: Vec<String> = env::args().collect();

    let config = YamlLoader::load_from_str(
        &fs::read_to_string(args.get(1).expect("You specified non existent config file"))
            .expect("Something went wrong opening yml file"),
    )
    .expect("Bad yml?");

    let config = &config[0];

    for x in fs::read_dir("./out").unwrap() {
        let x = x.unwrap();
        if !x.file_name().into_string().unwrap().to_lowercase().contains(".git") {
            if metadata(x.path()).unwrap().is_dir() {
                fs::remove_dir_all(x.path()).expect("Something went wrong and we failed to delete old dir");
            } else {
                fs::remove_file(x.path()).expect("Something went wrong deleting a file");
            }
        }
    }

    let mut files = String::from("id,name,rname\n");
    for x in config.as_hash().unwrap().iter() {
        let name = x.0.as_str().unwrap();
        {
            files += &unarchive_archive(
                String::from(name),
                String::from(config[name]["name"].as_str().unwrap()),
                String::from(config[name]["recommended-name"].as_str().unwrap()),
                String::from(config[name]["path"].as_str().unwrap()),
            );
        }
    }
    fs::write("./out/versions.csv", files).unwrap();
}
