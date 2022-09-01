# Voices of Wynn installer

This is the official repository for developing the installer for the Minecraft mod **Voices of Wynn**.

Check out the [main repository](https://github.com/Team-VoW/WynncraftVoiceProject) for more information about the mod.

If you want to see the source code of the website, check out the [website repository](https://github.com/Team-VoW/VoicesOfWynn-Website).

## How does it work

This application can be used both as an installer and as an updater.

The program connects to remote server hosting components of the mod. It gets a list of all source and resource files in the selected version of the mod and downloads  
them. When everything is downloaded, the program packs all the files into a `.jar` file, that can be loaded by Forge.

In case there is already any version of Voices of Wynn present on your device, the program will download only components that were changed in the selected version.  
this can enormously reduce the data transfer required for an update, as all the unchanged recordings don't need to be downloaded again.

## Storage

Installer of Voices of Wynn doesn't alter your registry in any way. However, it creates a small file in you `AppData` folder, which contains nothing but a path to the  
latest updated mod file. If you wish so, you can delete it after closing the application without any consequences.

## Requirements

Java 8 needs to be installed on your device to run the application.

If the application doesn't work for you and you can't update your Java,  
you can get Voices of Wynn by downloading the mod file directly from [our website](https://voicesofwynn.com).

## Contact

In case you encounter any problems with the installer, feel free to [submit an issue](https://github.com/Team-VoW/500mb_jar_installer/issues/new).
If you don't have an GitHub account and don't want to create one, you can also message us at [team@voicesofwynn.com](mailto:team@voicesofwynn.com).
