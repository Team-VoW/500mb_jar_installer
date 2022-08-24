use std::{fs, io};
use std::fs::read;
use std::path::Path;
use crc32fast::Hasher;

pub fn unarchive_archive<'a>(id: String, name: String, recommended_name: String, file_link: String) -> String {
    println!("started unjarring {}", file_link);

    let path = Path::new("./out").join(Path::new(&id));

    let mut dir_list = String::from("path,hash\n");

    let file = fs::File::open(&file_link).unwrap();

    let mut archive = zip::ZipArchive::new(file).unwrap();

    for i in 0..archive.len() {
        let mut file = archive.by_index(i).unwrap();

        if file.is_dir() {
            continue;
        }

        let out_path = match file.enclosed_name() {
            Some(path) => path.to_owned(),
            None => continue,
        };
        let out_path = path.join(out_path);

        if let Some(p) = out_path.parent() {
            if !p.exists() {
                fs::create_dir_all(&p).unwrap();
            }
        }

        let mut outfile = fs::File::create(&out_path).unwrap();
        io::copy(&mut file, &mut outfile).unwrap();

        let mut s = Hasher::new();
        s.update(&*read(out_path).expect("Error loading a file"));
        dir_list += &(file.enclosed_name().unwrap().to_str().unwrap().to_owned() + "," + &s.finalize().to_string() + "\n");
    }
    fs::write(path.join("files.csv"), &dir_list).unwrap();

    println!("Finished unjarring {}", file_link);

    return id + "," + &name + "," + &recommended_name + "\n";
}