package com.voicesofwynn.installer;

public abstract class InstallerOut {
    public abstract void outState(String str, int progress, int left);
    public abstract void corruptJar();
}
