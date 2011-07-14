package packager.commands

import groovy.text.GStringTemplateEngine
import packager.commands.makedh.Context
import static java.util.Collections.unmodifiableList

final class MakeDh implements Command {

    @Override void execute() {
        tasks*.call()
    }

    private def copyRequiredUserFiles = {
        withRequiredFilesProvided {
            it.each {
                new File(target, it).text = new File(sources, it).text
            }
        }
    }

    private def copyOptionalUserFiles = {
        withOptionalFilesProvided {
            it.each {
                new File(target, it).text = new File(sources, it).text
            }
        }
    }

    private def withOptionalFilesProvided(Closure c) {
        def found = sources.list() as List
        c(found - requiredUserFiles)
    }

    private def withRequiredFilesProvided(Closure c) {
        def found = sources.list() as List
        def intersection = found.intersect(requiredUserFiles)
        if(intersection != requiredUserFiles) throw new RequiredConfigurationException(sources, requiredUserFiles - intersection)
        c(requiredUserFiles)
    }

    private List<String> getRequiredUserFiles() {
        return ['copyright', "${context.name}.install"]
    }

    private def generateControl = {
        generateFile('control')
    }

    private def generateDirs = {
        new File(target, 'dirs').text = context.dirs.join('\n')
    }

    private def generateRules = {
        new File(target, 'rules').text = '''#!/usr/bin/make -f
# -*- makefile -*-
# Sample debian/rules that uses debhelper.
# This file was originally written by Joey Hess and Craig Small.
# As a special exception, when this file is copied by dh-make into a
# dh-make output file, you may use that output file without restriction.
# This special exception was added by Craig Small in version 0.37 of dh-make.

# Uncomment this to turn on verbose mode.
#export DH_VERBOSE=1

%:
	dh  $@'''
    }

    private def generateChangelog = {
        generateFile('changelog')
    }

    private def generateFile(String file) {
        new File(target, file).text = engine.createTemplate(getClass().getResourceAsStream("/packager/debian/$file").newReader()).make(context as Map).toString()
    }

    private def generateSourceFormat = {
        def source = new File(target, 'source')
        source.mkdirs()
        new File(source, 'format').text = '3.0 (native)'
    }

    static Context context(Map args) {
        new Context(args.name, args.version, args.releaseNotes, args.author, args.email, args.homepage, args.time.toString('E, dd MMM yyyy HH:mm:ss Z'), args.description, args.dirs, args.depends)
    }

    static class RequiredConfigurationException extends RuntimeException {

        private final List<File> missingFiles

        RequiredConfigurationException(File sources, List<String> missingFiles) {
            super("Required files missing! ${missingFiles.collect([]) {new File(sources, it)}}".toString())
            this.missingFiles = missingFiles.collect([]) {new File(sources, it)}
        }

        List<File> getMissingFiles() {unmodifiableList(missingFiles)}
    }

    boolean equals(o) {
        if(this.is(o)) return true;
        if(getClass() != o.class) return false;

        MakeDh makeDh = (MakeDh) o;

        if(context != makeDh.context) return false;
        if(sources != makeDh.sources) return false;
        if(target != makeDh.target) return false;

        return true;
    }

    int hashCode() {
        int result;
        result = (sources != null ? sources.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "MakeDh{" +
                "sources=" + sources +
                ", target=" + target +
                ", context=" + context +
                '}';
    }

    private final File sources
    private final File target
    private final Context context
    private final GStringTemplateEngine engine = new GStringTemplateEngine()

    private def tasks = [copyRequiredUserFiles, copyOptionalUserFiles, generateSourceFormat, generateChangelog, generateControl, generateDirs, generateRules]

    MakeDh(File sources, File target, Context context) {
        this.sources = sources
        this.target = target
        this.context = context
    }
}