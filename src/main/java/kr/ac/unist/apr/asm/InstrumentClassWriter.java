package kr.ac.unist.apr.asm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.ClassWriter;

public class InstrumentClassWriter extends ClassWriter{
    private File directory;
    public InstrumentClassWriter(String directory,int flags) {
        super(flags);
        this.directory=new File(directory);
    }

    @Override
    protected ClassLoader getClassLoader() {
        try {
            return URLClassLoader.newInstance(new URL[]{directory.toURI().toURL()});
        } catch (MalformedURLException e) {
            // Return default class loader
            return super.getClassLoader();
        }
    }
}
