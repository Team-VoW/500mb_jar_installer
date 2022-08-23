use std::alloc::System;
use crc32fast::Hasher;
use std::collections::HashMap;
use std::fs;
use std::fs::read;
use std::io::Read;
use std::path::Path;
use std::time::SystemTime;

#[derive(Clone)]
pub struct File {
    pub(crate) contents: Vec<u8>,
    pub(crate) name: String,
    pub(crate) hash: u32,
}

pub struct Archive {
    pub name: String,
    pub recommended_file_name: String,
    pub hash: u32,
    pub files: HashMap<String, File>,
}

pub fn load_archive(name: String, recommended_name: String, file_link: String) -> Archive {
    println!("Loading {}!", file_link);

    let mut files = HashMap::new();

    let mut s = Hasher::new();
    s.update(read(&file_link).unwrap().as_slice());
    let hash = s.finalize();

    let mut s = Hasher::new();
    s.update(read(&file_link).unwrap().as_slice());
    let hash = s.finalize();

    let file = fs::File::open(&file_link).unwrap();

    let mut archive = zip::ZipArchive::new(file).unwrap();

    for i in 0..archive.len() {
        let start = SystemTime::now();
        let mut file = archive.by_index(i).unwrap();

        if file.is_dir() {
            continue;
        }

        let mut contents: Vec<u8> = Vec::with_capacity(file.size() as usize);
        file.read(contents.as_mut_slice())
            .expect("Error reading file from zip");

        files.insert(
            String::from(file.name()),
            File {
                name: file.name().parse().unwrap(),
                hash,
                contents,
            },
        );
    }

    println!("Loaded {} with {} files!", file_link, &files.len());
    return Archive {
        name,
        recommended_file_name: recommended_name,
        files,
        hash,
    };
}

// pub fn dir_to_files(path: &String, start_path: &Path) -> Vec<File> {
//     let mut files: Vec<File> = Vec::new();
//
//     for file in fs::read_dir(path).expect("Error opening the dir") {
//         let file = file.expect("Aaaa");
//
//         let str = &file
//             .path()
//             .into_os_string()
//             .into_string()
//             .expect("Error converting a string to a string");
//         if file.path().is_dir() {
//             files.append(&mut dir_to_files(str, start_path).to_vec());
//         } else {
//             if file.file_name().to_str().unwrap().contains(".DS_Store") {
//                 // gotta hate macOS
//                 continue;
//             }
//
//             let contents: Vec<u8> = read(str).expect("Error loading a file");
//             let mut s = Hasher::new();
//             s.update(&*contents);
//             files.push(File {
//                 contents,
//                 name: file
//                     .path()
//                     .as_path()
//                     .strip_prefix(start_path)
//                     .unwrap()
//                     .display()
//                     .to_string(),
//                 hash: s.finalize(),
//             });
//         }
//     }
//
//     return files;
// }
